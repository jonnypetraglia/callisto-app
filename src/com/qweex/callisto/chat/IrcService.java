package com.qweex.callisto.chat;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.sorcix.sirc.*;

import java.io.IOException;

public class IrcService extends IntentService {

    String TAG = "Callisto:chat:IrcService";

    public static IrcService instance;

    private IrcConnection ircConnection;
    private IrcServiceAdapter ircConnectionAdapter;
    private Channel jupiterbroadcasting, jupiterdev, jupitergaming, jupiterops, qweex;
    private User nickServ;

    public IrcService() {
        super("Callisto/IRC");
        Log.v(TAG, "Constructor");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent");

        String nickname = intent.getStringExtra("nickname"),
               username = intent.getStringExtra("username"),
               real_name = intent.getStringExtra("real_name"),
               password = intent.getStringExtra("password");

        ircConnectionAdapter = new IrcServiceAdapter();

        ircConnection = new IrcConnection("irc.geekshed.net");

        ircConnection.setNick(nickname);
        if(username!=null && username.length()>0) {
            if(real_name!=null && real_name.length()>0)
                ircConnection.setUsername(username);
            else
                ircConnection.setUsername(username, real_name);
        }

        ircConnection.addMessageListener(ircConnectionAdapter);
        ircConnection.addModeListener(ircConnectionAdapter);
        ircConnection.addServerListener(ircConnectionAdapter);

        try {
            ircConnection.connect();
            nickServ = ircConnection.createUser("nickserv");
            if(password!=null && password.length()>1)
                nickServ.sendMessage("identify " + password);
            jupiterbroadcasting = ircConnection.createChannel("#jupiterbroadcasting");
            jupitergaming = ircConnection.createChannel("#jupitergaming");
            jupiterdev = ircConnection.createChannel("#jupiterdev");
            jupiterops = ircConnection.createChannel("#jupiterops");
            qweex = ircConnection.createChannel("#qweex");
            Log.i(TAG, " connected? " + ircConnection.isConnected());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NickNameException e) {
            e.printStackTrace();
        } catch (PasswordException e) {
            e.printStackTrace();
        }
    }
}
