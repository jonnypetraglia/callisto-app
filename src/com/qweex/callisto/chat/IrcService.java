package com.qweex.callisto.chat;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.sorcix.sirc.*;

import java.io.IOException;

public class IrcService extends IntentService {

    String TAG = "Callisto:chat_tab:IrcService";

    public static IrcService instance;
    public static ChatFragment chatFragment;

    private IrcConnection ircConnection;

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

        Bundle extras = intent.getExtras();

        // Retrieve data from intent. This is basically the "constructor"
        String nickname = extras.getString("nickname"),
               username = intent.getStringExtra("username"),
               real_name = intent.getStringExtra("real_name"),
               password = intent.getStringExtra("password");
        String[] channel_names = intent.getStringArrayExtra("channels");


        // Set up the connection
        ircConnection = new IrcConnection("irc.geekshed.net");


        ircConnection.setNick(nickname);
        if(username!=null && username.length()>0) {
            if(real_name!=null && real_name.length()>0)
                ircConnection.setUsername(username);
            else
                ircConnection.setUsername(username, real_name);
        }

        ircConnection.addMessageListener(chatFragment.ircConnectionAdapter);
        ircConnection.addModeListener(chatFragment.ircConnectionAdapter);
        ircConnection.addServerListener(chatFragment.ircConnectionAdapter);

        try {
            chatFragment.createTab(ircConnection);
            Log.i(TAG, "Connecting to server...");
            ircConnection.connect();

            if(password!=null && password.length()>1)
                ircConnection.createUser("nickserv").sendMessage("identify " + password);
            // Join the channels
            for(String channel_name : channel_names) {
                Channel channel = ircConnection.createChannel(channel_name);
                chatFragment.createTab(ircConnection, channel);
                Log.i(TAG, "Joining  " + channel);
                channel.join();
            }

            Log.i(TAG, "--connected? " + ircConnection.isConnected());

        } catch (IOException e) {
            chatFragment.handleError(ircConnection, e);
        } catch (NickNameException e) {
            chatFragment.handleError(ircConnection, e);
        } catch (PasswordException e) {
            chatFragment.handleError(ircConnection, e);
        }
    }

    public IrcConnection getConnection() {
        return ircConnection;
    }
}
