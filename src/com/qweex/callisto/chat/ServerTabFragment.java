package com.qweex.callisto.chat;


import com.qweex.callisto.MasterActivity;
import com.sorcix.sirc.IrcConnection;

public class ServerTabFragment extends TabFragment{
    IrcConnection server = null;

    public ServerTabFragment(MasterActivity master, IrcConnection server) {
        super(master);
        this.server = server;
    }

    @Override
    void send(String msg) {
        server.sendRaw(msg);
    }
}
