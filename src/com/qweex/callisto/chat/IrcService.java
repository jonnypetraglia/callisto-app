package com.qweex.callisto.chat;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.PrefCache;
import com.qweex.callisto.R;
import com.qweex.utils.ResCache;
import com.sorcix.sirc.*;

import java.io.IOException;

public class IrcService extends IntentService {

    String TAG = "Callisto:chat_tab:IrcService";

    public static IrcService instance;
    public static ChatFragment chatFragment;

    private IrcConnection ircConnection;

    final int NOTIFY_ONGOING_ID = 1337, NOTIFY_MENTION_ID = 9001;

    /** Notification */
    NotificationManager notificationManager;
    int mentionCount;

    public IrcService() {
        super("Callisto/IRC");
        Log.v(TAG, "Constructor");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(NOTIFY_ONGOING_ID);
        notificationManager.cancel(NOTIFY_MENTION_ID);
        instance = null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent");

        instance = this;

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


            Log.i(TAG, "Notification is going to be built");
            // Ongoing notification
            Intent notificationIntent = new Intent(this, MasterActivity.class);
            notificationIntent.putExtra("fragment", MasterActivity.CHAT_ID);
            PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(), 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Log.i(TAG, "Notification is being built..");
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_action_dialog)
                    .setContentIntent(contentIntent)
                    .setOngoing(true);
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFY_ONGOING_ID, notificationBuilder.build());
            Log.i(TAG, "Notification has been shown");

        } catch (IOException e) {
            chatFragment.handleError(ircConnection, e);
        } catch (NickNameException e) {
            chatFragment.handleError(ircConnection, e);
        } catch (PasswordException e) {
            chatFragment.handleError(ircConnection, e);
        }
    }

    public void handleMention(IrcMessage msg, String tabTag) {

        NotificationCompat.Builder mentionNotifyBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_action_dialog);

        mentionCount++;

        // Load the settings for the notification from the preferences.
        int defaults = 0;
        if(PrefCache.bool("irc_vibrate", null, true) && (mentionCount==1 || PrefCache.bool("irc_vibrate_all", null, false)))
            defaults |= Notification.DEFAULT_VIBRATE;
        if(PrefCache.bool("irc_sound", null, true) && (mentionCount==1 || PrefCache.bool("irc_sound_all", null, false)))
            defaults |= Notification.DEFAULT_SOUND;
        mentionNotifyBuilder.setDefaults(defaults);


        // Set title and Text
        mentionNotifyBuilder.setContentTitle(msg.getTitle() + ": " + msg.getMessage());
        String text;
        if(mentionCount>1)
            text = ResCache.str(R.string.notify_new_mentions, (mentionCount-1) + "");
        else
            text = ResCache.str(R.string.notify_new_mention);
        mentionNotifyBuilder.setContentText(text);

        // Set intent info
        Intent notificationIntent = new Intent(this, MasterActivity.class);
        notificationIntent.putExtra("fragment", MasterActivity.CHAT_ID);
        notificationIntent.putExtra("tabTag", tabTag);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mentionNotifyBuilder.setContentIntent(contentIntent);


        notificationManager.notify(NOTIFY_MENTION_ID, mentionNotifyBuilder.build());
    }
}
