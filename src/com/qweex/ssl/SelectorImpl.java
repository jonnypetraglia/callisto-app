/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qweex.ssl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

//https://android.googlesource.com/platform/libcore/+/9b510df35b57946d843ffc34cf23fdcfc84c5220/luni/src/main/java/java/nio/SelectorImpl.java

//http://my.fit.edu/~vkepuska/ece5570/adt-bundle-windows-x86_64/sdk/sources/android-16/java/nio/SelectorImpl.java

public class SelectorImpl extends AbstractSelector
{
    private HashSet<SelectionKey> keys;
    private Set<SelectionKey> selectedKeys,
            publicKeys,
            publicSelectedKeys;
    short POLLIN = 0x0001, POLLOUT = 0x0004;        //TODO
    int OP_ACCEPT = SelectionKey.OP_ACCEPT,
        OP_CONNECT = SelectionKey.OP_CONNECT,
        OP_READ = SelectionKey.OP_READ,
        OP_WRITE = SelectionKey.OP_WRITE;

    /**
     * Used to synchronize when a key's interest ops change.
     */
    final Object keysLock = new Object();

    public static SelectorImpl open() throws IOException
    {
        return new SelectorImpl(SelectorProvider.provider());
    }

    protected SelectorImpl(SelectorProvider sp) throws IOException
    {
        super(sp);
        keys = new HashSet<SelectionKey>();
        selectedKeys = new HashSet<SelectionKey>();
        publicKeys = keys;
        publicSelectedKeys = selectedKeys;

        try {
        //TODO
        /*FileDescriptor[] pipeFds = Libcore.os.pipe();
        wakeupIn = pipeFds[0];
        wakeupOut = pipeFds[1];
        IoUtils.setBlocking(wakeupIn, false);
        */
        pollFds.add(new StructPollfd());
        //setPollFd(0, wakeupIn, POLLIN, null);
        }catch(Exception e)
        {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public Set<SelectionKey> keys() {
        if(!isOpen())
            throw new ClosedSelectorException();
        return publicKeys;
    }

    @Override
    public Set<SelectionKey> selectedKeys()
    {
        if(isOpen())
            throw new ClosedSelectorException();
        return publicSelectedKeys;
    }

    @Override
    public int selectNow() throws IOException {
        return selectedInternal(0);
    }

    @Override
    public int select() throws IOException {
        return select(0);
    }

    @Override
    public int select(long timeout) throws IOException {
        if(timeout<0)
            throw new IllegalArgumentException("Negative timeout");
        return selectedInternal(timeout);
    }

    //https://android.googlesource.com/platform/libcore/+/master/luni/src/main/java/libcore/io/BlockGuardOs.java
    private int selectedInternal(long timeout) throws IOException
    {
        doCancel();
        boolean isBlock = (timeout != 0);
        preparePollFds();
        int rc = 1;
        try {
            if(isBlock)
                begin();
            try {
                //TODO
                //rc = LibCore.os.poll(pollFds.toArray(), (int) timeout);
            } catch(Exception errException)
            //catch(ErrnoException errnoException)
            {
                throw new IOException(errException.getMessage());
                //errnoException.rethrowAsIOException();
            }
        } finally {
            if(isBlock)
                end();
        }

        int readyCount = (rc > 0) ? processPollFds() : 0;
        readyCount -=doCancel();
        return readyCount;
    }

    private int doCancel()
    {
        int deselected = 0;

        Set<SelectionKey> cancelledKeys = cancelledKeys();
        synchronized (cancelledKeys) {
            if (cancelledKeys.size() > 0) {
                for (SelectionKey currentKey : cancelledKeys) {
                    keys.remove(currentKey);
                    deregister((AbstractSelectionKey) currentKey);
                    if (selectedKeys.remove(currentKey)) {
                        deselected++;
                    }
                }
                cancelledKeys.clear();
            }
        }
        return deselected;
    }

    private final FileDescriptor wakeupIn = null;
    private final FileDescriptor wakeupOut = null;

    @Override
    public Selector wakeup() {
        try {
            //TODO
            //Libcore.os.write(wakeupOut, new byte[] { 1 }, 0, 1);
        } catch (Exception ignored) {
            //(ErrnoException ignored) {
        }
        return this;
    }

    private int processPollFds() throws IOException {
        if (pollFds.get(0).revents == POLLIN) {
            // Read bytes from the wakeup pipe until the pipe is empty.
            byte[] buffer = new byte[8];
            //TODO
            //while (IoBridge.read(wakeupIn, buffer, 0, 1) > 0) {
            //}
        }

        int readyKeyCount = 0;
        for (int i = 1; i < pollFds.size(); ++i) {
            StructPollfd pollFd = pollFds.get(i);
            if (pollFd.revents == 0) {
                continue;
            }
            if (pollFd.fd == null) {
                break;
            }

            SelectionKeyImpl key = (SelectionKeyImpl) pollFd.userData;

            pollFd.fd = null;
            pollFd.userData = null;

            int ops = key.interestOps();
            int selectedOp = 0;
            if ((pollFd.revents & POLLIN) != 0) {
                selectedOp = ops & (OP_ACCEPT | OP_READ);
            } else if ((pollFd.revents & POLLOUT) != 0) {
                if (key.isConnected()) {
                    selectedOp = ops & OP_WRITE;
                } else {
                    selectedOp = ops & OP_CONNECT;
                }
            }

            if (selectedOp != 0) {
                boolean wasSelected = selectedKeys.contains(key);
                if (wasSelected && key.readyOps() != selectedOp) {
                    key.setReadyOps(key.readyOps() | selectedOp);
                    ++readyKeyCount;
                } else if (!wasSelected) {
                    key.setReadyOps(selectedOp);
                    selectedKeys.add(key);
                    ++readyKeyCount;
                }
            }
        }
        return readyKeyCount;
    }

    private void preparePollFds()
    {
        int i = 1;
        for(SelectionKey key : keys)
        {
            int interestOps = key.interestOps();

            short eventMask = 0;
            if (((OP_ACCEPT | java.nio.channels.SelectionKey.OP_READ) & interestOps) != 0) {
                eventMask |= POLLIN;
            }
            if (((java.nio.channels.SelectionKey.OP_CONNECT | java.nio.channels.SelectionKey.OP_WRITE) & interestOps) != 0) {
                eventMask |= POLLOUT;
            }

            if (eventMask != 0) {
                setPollFd(i++, ((FileDescriptorChannel) key.channel()).getFD(), eventMask, key);
            }
        }
    }

    private void ensurePollFdsCapacity() {
        // We need one slot for each element of mutableKeys, plus one for the wakeup pipe.
        while (pollFds.size() < keys.size() + 1) {
            pollFds.add(new StructPollfd());
        }
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel channel,
                                    int operations, Object attachment) {
        if (!provider().equals(channel.provider())) {
            throw new IllegalSelectorException();
        }
        synchronized (this) {
            synchronized (keys) {
                SelectionKeyImpl selectionKey = new SelectionKeyImpl(channel, operations,
                        attachment, this);
                keys.add(selectionKey);
                ensurePollFdsCapacity();
                return selectionKey;
            }
        }
    }

    @Override
    protected void implCloseSelector() throws IOException {
        wakeup();
        synchronized (this) {
            synchronized (keys) {
                synchronized (selectedKeys) {
                    //TODO
                    //IoUtils.close(wakeupIn);
                    //IoUtils.close(wakeupOut);
                    doCancel();
                    for (SelectionKey sk : keys) {
                        deregister((AbstractSelectionKey) sk);
                    }
                }
            }
        }
    }


    private final ArrayList<StructPollfd> pollFds = new ArrayList<StructPollfd>(); //StructPollfd.class, 8);
    private void setPollFd(int i, FileDescriptor fd, int events, Object object) {
        StructPollfd pollFd = pollFds.get(i);
        pollFd.fd = fd;
        pollFd.events = (short) events;
        pollFd.userData = object;
    }
}