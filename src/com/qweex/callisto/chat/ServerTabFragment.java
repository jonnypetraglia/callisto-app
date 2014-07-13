package com.qweex.callisto.chat;


import android.util.Log;
import com.qweex.callisto.MasterActivity;
import com.sorcix.sirc.IrcConnection;

public class ServerTabFragment extends TabFragment{
    String TAG = super.TAG + ":Server";

    IrcConnection server = null;

    public ServerTabFragment(MasterActivity master, IrcConnection server) {
        super(master);
        Log.v(TAG, "Creating Server Fragment");
        this.server = server;
    }

    @Override
    void send(String msg) {
        server.sendRaw(msg);
    }
}
