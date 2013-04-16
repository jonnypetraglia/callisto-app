/*
 * Copyright (C) 2012-2013 Qweex
 * This file is a part of Callisto.
 *
 * Callisto is free software; it is released under the
 * Open Software License v3.0 without warranty. The OSL is an OSI approved,
 * copyleft license, meaning you are free to redistribute
 * the source code under the terms of the OSL.
 *
 * You should have received a copy of the Open Software License
 * along with Callisto; If not, see <http://rosenlaw.com/OSL3.0-explained.htm>
 * or check OSI's website at <http://opensource.org/licenses/OSL-3.0>.
 */

package com.qweex.utils;

import android.util.Log;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SSLSocketChannel extends SocketChannel implements ByteChannel, ScatteringByteChannel, GatheringByteChannel
{
    SocketChannel socketChannel;
    SSLEngine engine;
    String hostname;
    int port;
    /**
     * Constructs a new {@code SocketChannel}.
     *
     * @param selectorProvider an instance of SelectorProvider.
     */
    public SSLSocketChannel(SelectorProvider selectorProvider) throws IOException
    {
        super(selectorProvider);
    }

    public static SSLSocketChannel open() throws IOException
    {
        SSLSocketChannel s = new SSLSocketChannel(SelectorProvider.provider());
        s.socketChannel = SocketChannel.open();
        return s;
    }

    @Override
    public Socket socket() {
        return socketChannel.socket();
    }

    @Override
    public boolean isConnected() {
        return socketChannel.isConnected();
    }

    @Override
    public boolean isConnectionPending() {
        return socketChannel.isConnectionPending();
    }


    //http://code.google.com/p/hazelcast/source/browse/trunk/hazelcast/src/main/java/com/hazelcast/nio/SSLSocketChannelWrapper.java?r=2346&spec=svn2346
    //http://rmiproxy.com/ScalableSSL/javadoc/scalablessl/SSLSocketChannel.html
    //http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/samples/sslengine/SSLEngineSimpleDemo.java
    //http://publib.boulder.ibm.com/infocenter/java7sdk/v7r0/index.jsp?topic=%2Fcom.ibm.java.security.component.doc%2Fsecurity-component%2Fjsse2Docs%2Fssltlsdata.html
    //http://svn.apache.org/repos/asf/tomcat/trunk/java/org/apache/tomcat/util/net/SecureNioChannel.java

    private SSLEngine sslEngine;
    private ByteBuffer appSendBuffer;
    private ByteBuffer appRecvBuffer;
    private ByteBuffer cipherSendBuffer;
    private ByteBuffer cipherRecvBuffer;
    private SSLEngineResult.HandshakeStatus hStatus;
    private int remaingUnwraps;
    private SocketChannel sChannel;
    @Override
    public boolean connect(SocketAddress address) throws IOException
    {
        String aHostname = ((InetSocketAddress)address).getHostName();
        int aPort = ((InetSocketAddress)address).getPort();
            try
            {
                sslEngine = getInitializedSSLContext().createSSLEngine(aHostname, aPort);
                sslEngine.setNeedClientAuth(false);
                sslEngine.setUseClientMode(true);
                sslEngine.beginHandshake();
                hStatus = sslEngine.getHandshakeStatus();

                appSendBuffer = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
                cipherSendBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
                cipherRecvBuffer = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
                appRecvBuffer = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());

                return (sChannel = SocketChannel.open()).connect(new InetSocketAddress(aHostname, aPort));
            }
            catch (Exception aExc)
            {
                Log.e("", aExc + "!");
            }

            return false;
    }


    public String read() throws IOException
    {
        doAnyPendingHandshake();

        tryReadAndUnwrap();

        byte[] _bytes = new byte[appRecvBuffer.flip().limit()];
        appRecvBuffer.get(_bytes);
        appRecvBuffer.clear();

        return new String(_bytes);
    }

    public int write(String aMessage) throws IOException
    {
        doAnyPendingHandshake();

        appSendBuffer.clear();
        appSendBuffer.put(aMessage.getBytes()).flip();

        return wrapAndWrite();
    }

    private synchronized void doAnyPendingHandshake() throws IOException
    {
        while (processHandshake())
        {

        }
    }

    private synchronized boolean processHandshake() throws IOException
    {
        Log.d(":", Thread.currentThread().getName() + " " + hStatus);
        switch (hStatus)
        {
            case NEED_WRAP:
                wrapAndWrite();
                break;
            case NEED_UNWRAP:
                tryReadAndUnwrap();
                break;
            case NEED_TASK:
                executeTasks();
                break;
            case NOT_HANDSHAKING:
            case FINISHED:
                return false;
        }

        return true;
    }

    private void tryReadAndUnwrap() throws IOException, SSLException
    {
        SSLEngineResult _hRes;
        if (!sslEngine.isInboundDone())
        {
            if (remaingUnwraps == 0)
            {
                cipherRecvBuffer.clear();
                int _readCount = sChannel.read(cipherRecvBuffer);
                remaingUnwraps += _readCount;
                Log.d(":","Reading: " + _readCount);
                cipherRecvBuffer.flip();
            }

            _hRes = sslEngine.unwrap(cipherRecvBuffer, appRecvBuffer);
            hStatus = _hRes.getHandshakeStatus();
            remaingUnwraps -= _hRes.bytesConsumed();
        }
    }

    private int wrapAndWrite() throws SSLException, IOException
    {
        SSLEngineResult _hRes;
        if (cipherSendBuffer.position() != 0)
        {
            cipherSendBuffer.compact();
        }
        _hRes = sslEngine.wrap(appSendBuffer, cipherSendBuffer);
        hStatus = _hRes.getHandshakeStatus();
        cipherSendBuffer.flip();
        return sChannel.write(cipherSendBuffer);
    }

    private void executeTasks()
    {
        Runnable _r = null;
        while ((_r = sslEngine.getDelegatedTask()) != null)
        {
            new Thread(_r).start();
        }

        hStatus = sslEngine.getHandshakeStatus();
    }
    private SSLContext getInitializedSSLContext() throws NoSuchAlgorithmException, KeyManagementException
    {
        SSLContext _sslCtx = SSLContext.getInstance("SSL");
        _sslCtx.init(null, new TrustManager[] { new X509TrustManager()
        {
            public java.security.cert.X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
            {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
            {
            }
        } }, new SecureRandom());

        return _sslCtx;
    }



























    public boolean connect2(SocketAddress address) throws IOException
    {
        Log.i("Handy new class", "connect!");
        hostname = ((InetSocketAddress)address).getHostName();
        port = ((InetSocketAddress)address).getPort();

        boolean result = false;
        try {
        SSLContext sslCtx = SSLContext.getInstance("TLS");
        sslCtx.init(null, new X509TrustManager[]{new NaiveTrustManager()}, null);
        engine = sslCtx.createSSLEngine(hostname, port);
        engine.setUseClientMode(true);
        result = socketChannel.connect(address);

            Log.i("Handy new class", "spinning");
            // Complete connection
            while (!socketChannel.finishConnect())
            {
                // do something until connect completed
            }

            Log.i("Handy new class", "preparing for handshake");
            SSLSession session = engine.getSession();
            dataOut = ByteBuffer.allocate(session.getApplicationBufferSize());
            netOut = ByteBuffer.allocate(session.getPacketBufferSize());
            dataIn = ByteBuffer.allocate(session.getApplicationBufferSize());
            netIn = ByteBuffer.allocate(session.getPacketBufferSize());

            // Do initial handshake
            myHandshake();
        }catch(NoSuchAlgorithmException nsae)
        { nsae.printStackTrace();}
        catch(KeyManagementException kme)
        { kme.printStackTrace();}
        catch(IOException ioe)
        { ioe.printStackTrace();}
        catch(Exception e)
        { e.printStackTrace();}

        return result;  //To change body of implemented methods use File | Settings | File Templates.
    }

    ByteBuffer peerNetData_sT0c;
    ByteBuffer peerAppData;
    ByteBuffer myNetData_clientOut;
    ByteBuffer myAppData;
    ByteBuffer dataOut, netOut, dataIn, netIn;
    //clear()   == Clears this buffer. The position is set to zero, the limit is set to the capacity, and the mark is discarded.
    //flip()    == Flips this buffer. The limit is set to the current position and then the position is set to zero. If the mark is defined then it is discarded.
    //compact() == Compacts this buffer  (optional operation).    The bytes between the buffer's current position and its limit, if any, are copied to the beginning of the buffer.

    SSLEngineResult engineResult;


    protected SSLEngineResult.HandshakeStatus tasks() {
        Runnable r = null;
        while ( (r = engine.getDelegatedTask()) != null) {
            r.run();
        }
        return engine.getHandshakeStatus();
    }

    void myHandshake() throws Exception
    {
        // Begin handshake
        engine.beginHandshake();
        SSLEngineResult result;
        SSLEngineResult.HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
        Log.i("Handshake", "Starting");
        while(handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED)
        {
            switch(handshakeStatus)
            {
                case NEED_TASK:
                    while(handshakeStatus== SSLEngineResult.HandshakeStatus.NEED_TASK)
                        handshakeStatus = tasks();
                    break;
                case NEED_WRAP:
                    netOut.clear();
                    result = engine.wrap(dataOut, netOut);
                    netOut.flip();
                    handshakeStatus = result.getHandshakeStatus();
                    if(netOut.remaining()>0)
                        socketChannel.write(netOut);
                    Log.i("Handshake", "NEED_WRAP: writing");
                    if(result.getStatus() == SSLEngineResult.Status.OK)
                    {
                        if(result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK)
                        {
                            Log.i("Handshake", "NEED_UNWRAP: tasking");
                            handshakeStatus = tasks();
                        }
                    }
                    else if(result.getStatus() != SSLEngineResult.Status.OK)
                        throw new IOException("Unexpected status:" + result.getStatus() + " during handshake WRAP.");
                    //break;  //???????
                case NEED_UNWRAP:
                    Log.i("Handshake", "NEED_UNWRAP: spinning...");
                    if(netIn.position()==netIn.limit())
                        netIn.clear();
                    while (socketChannel.read(netIn) < 0) Thread.sleep(20);
                    do {
                        netIn.flip();
                        Log.i("Handshake", "NEED_UNWRAP: unwrapping...");
                        result = engine.unwrap(netIn, dataIn);
                        Log.i("Handshake", "NEED_UNWRAP: compacting...");
                        netIn.compact();
                        handshakeStatus = result.getHandshakeStatus();
                        if(result.getStatus() == SSLEngineResult.Status.OK && result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK)
                        {
                            Log.i("Handshake", "NEED_UNWRAP: tasking");
                            handshakeStatus = tasks();
                        }
                    } while(result.getStatus()== SSLEngineResult.Status.OK && handshakeStatus == SSLEngineResult.HandshakeStatus.NEED_UNWRAP);
                    break;
                case NOT_HANDSHAKING:
                default:
                    throw new IOException("NOT_HANDSHAKING when handshaking");
            }
            Log.i("Handshake", "After switch status is " + handshakeStatus);
        }
        Log.i("Handshake", "DONE");

    }


    public void IBMwrite(String thing) throws SSLException, IOException
    {
        myAppData.put("thing".getBytes());
        myAppData.flip();

        while (myAppData.hasRemaining())
        {
            // Generate SSL/TLS encoded data (handshake or application data)
            engineResult = engine.wrap(myAppData, myNetData_clientOut);

            // Process status of call
            if(engineResult.getStatus() == SSLEngineResult.Status.OK)
            {
                myAppData.compact();
                // Send SSL/TLS encoded data to peer

                while(myNetData_clientOut.hasRemaining())
                {
                    int num = socketChannel.write(myNetData_clientOut);
                    if (num == -1) {
                        // handle closed channel
                    } else if (num == 0) {

                        // no bytes written; try again later
                    }
                }
            }
            else if (engineResult.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW)
            {
                //TODO
            }
            else if (engineResult.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW)
            {
                //TODO
            }
            else if (engineResult.getStatus() == SSLEngineResult.Status.CLOSED)
            {

            }
        }
    }

    public void IBMRead() throws IOException
    {
        // Read SSL/TLS encoded data from peer
        int num = socketChannel.read(peerNetData_sT0c);

        if (num == -1) {
            // Handle closed channel
        } else if (num == 0) {

            // No bytes read; try again ...
        } else {
            // Process incoming data

            peerNetData_sT0c.flip();
            engineResult = engine.unwrap(peerNetData_sT0c, peerAppData);


            if (engineResult.getStatus() == SSLEngineResult.Status.OK)
            {
                peerNetData_sT0c.compact();

                if (peerAppData.hasRemaining()) {
                    // Use peerAppData
                }
            }

            // Handle other status: BUFFER_OVERFLOW, BUFFER_UNDERFLOW, CLOSED
        }
    }

    @Override
    public boolean finishConnect() throws IOException {
        return socketChannel.finishConnect();
    }

    @Override
    public int read(ByteBuffer target) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long read(ByteBuffer[] targets, int offset, int length) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int write(ByteBuffer source) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long write(ByteBuffer[] sources, int offset, int length) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        //TODO
    }

    @Override
    protected void implConfigureBlocking(boolean blocking) throws IOException {
        //TODO
    }


    public SocketAddress getRemoteAddress()
    {
        return socketChannel.socket().getRemoteSocketAddress();
    }

    public SocketAddress getLocalAddress()
    {
        return socketChannel.socket().getLocalSocketAddress();
    }

    public SocketChannel shutdownOutput()
    {
        try {
        socket().shutdownOutput();
        }catch(Exception e)
        {
        }
        return socketChannel;
    }

    public SocketChannel shutdownInput()
    {
        try {
            socket().shutdownInput();
        }catch(Exception e)
        {
        }
        return socketChannel;
    }

    public <T> SocketChannel setOption(SocketOption<T> name, T value)
    {
        return null;
    }

    public <T> T getOption(SocketOption<T> name) throws IOException
    {
        return null;
    }

    public java.util.Set<SocketOption<?>> supportedOptions()
    {
        return null;
    }

    public SocketChannel bind(SocketAddress sa)
    {
        try {
            socketChannel.socket().bind(sa);
        }catch(Exception e){}
        return socketChannel;
    }


    //<Qweex>
    public class NaiveTrustManager implements X509TrustManager
    {
        /**
         * Check client trusted
         *
         * @throws java.security.cert.CertificateException if not trusted
         */
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            // No Exception == Trust
        }

        /**
         * Check server trusted
         *
         * @throws CertificateException if not trusted
         */
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            // No Exception == Trust
        }

        /**
         * Get accepted issuers
         */
        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return new X509Certificate[0];
        }
    }
    //</Qweex>
}
        /*
        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        char[] passphrase = "passphrase".toCharArray();

        ks.load(new FileInputStream(keyStoreFile), passphrase);
        ts.load(new FileInputStream(trustStoreFile), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);
        //*/

        /*
        SSLContext sslCtx = SSLContext.getInstance("TLS");
        //sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        sslCtx.init(null, null, null);

        sslEngine = sslCtx.createSSLEngine(session.getRequestedConnection().getHostName(), session.getRequestedConnection().getPort());

        /*
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new X509TrustManager[] { new NaiveTrustManager() }, null);
        SSLSocketFactory factory = context.getSocketFactory();
        SSLSocket ssocket = (SSLSocket) factory.createSocket(session.getRequestedConnection().getHostName(), session.getRequestedConnection().getPort());
        ssocket.startHandshake();
        Socket _socket = ssocket;
        //https://github.com/pocmo/Yaaic/blob/master/application/src/org/jibble/pircbot/PircBot.java
        */