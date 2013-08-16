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
package com.qweex.callisto;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.qweex.callisto.podcast.EpisodeDesc;
import com.qweex.utils.ArrayListWithMaximum;
import com.qweex.callisto.irc.IRCChat;
import com.qweex.callisto.listeners.*;
import com.qweex.callisto.receivers.AudioJackReceiver;
import com.qweex.callisto.widgets.CallistoWidget;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Literally a giant "blob" to hold all the static things in the app, like date formats that are used frequently, preferences, shared resources, etc.
 */
public class StaticBlob
{
    /** One of the various date formats used across various Activities. The usage for most should be self-documenting from the name. */
    public static final SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final SimpleDateFormat sdfRawSimple1 = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat sdfRawSimple2 = new SimpleDateFormat("HHmmss");
    public static final SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm aa");
    public static final SimpleDateFormat sdfSource = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
    public static final SimpleDateFormat sdfFile = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat sdfHuman = new SimpleDateFormat("MMM d");
    public static final SimpleDateFormat sdfHumanLong = new SimpleDateFormat("MMM d, yyyy");
    //TODO: wtf?
    public static final int NOTIFICATION_ID = 1337;
    //TODO: wtf?
    public final static String PREF_FILE = "alarms";
    /** The media players are used across the app to control playing either live or podcast'ed episodes. */
    public static MediaPlayer mplayer = null;
    /** Used to connect to the SQLlite database. For more information, see the DatabaseConnector class.*/
    public static DatabaseConnector databaseConnector;
    /** Notifications are used in various activities */
    public static Notification notification_playing;
    public static Notification notification_chat;
    public static Notification notification_alarm;
    /** The path on the SD card to store downloaded episodes. Customizable via the settings dialog. */
    public static String storage_path;
    /** The current download number. */
    public static int current_download = 1;
    public static SimpleDateFormat sdfDestination;
    /** Shared Drawable resources for updating the player control graphics. */
    public static Drawable playDrawable;
    public static Drawable pauseDrawable;
    /** An instance of the PlayerInfo class, to keep track of the info of a track when updating the player controls across the activities. */
    public static PlayerInfo playerInfo;
    public static String[] SHOW_LIST_VIDEO;
    public static String[] SHOW_LIST_AUDIO;
    /** A static array containing corresponding info for the shows.
     *  A sub-heading will start with a space in the name list and have a value of null in the feeds.
     */
    public static String[] SHOW_LIST;
    /** The value of a dip, to be used programatically when updating a view. */
    public static float DP;
    /** If you should be a "weirdo" that likes dates in a logically correct manner. */
    public static boolean europeanDates;
    //TODO: wtf?
    public static OnCompletionListenerWithContext trackCompleted;
    public static OnErrorListenerWithContext trackCompletedBug;
    public static OnPreparedListenerWithContext mplayerPrepared;
    /** Used for notifications */
    public static NotificationManager mNotificationManager;
    //TODO: wtf?
    public static SharedPreferences alarmPrefs;
    /** The status of the live player; true if it is playing, false if it is paused or not in use. */
    public static boolean live_isPlaying = false;
    /** The Version of Callisto. Set to -1 if it cannot be determined. */
    public static int appVersion = -1;
    /** Contains whether or not the activity has been launched via the widget. **/
    public static boolean is_widget;
    /** Locks the wifi when downloading or streaming **/
    public static WifiManager.WifiLock Live_wifiLock;
    //TODO: wtf
    public static Dialog errorDialog;
    //TODO: wtf
    public static Dialog liveDg;
    //TODO: wtf
    public static TextView timeView;
    //TODO: wtf
    public static ProgressBar timeProgress;

    public static OnAudioFocusChangeListenerImpl audioFocus;

    public static PhoneStateListenerImpl phoneStateListener;

    /** Makes a string friendly to the filesystem by replacing offending characters with underscores
     * @param input The input. DUH. GOD. DO I HAVE TO TELL YOU EVERYTHING.
     * */
    public static String makeFileFriendly(String input)
    {
        return input.replaceAll("[\\?]", "_"); //[\\?:;\*"<>\|]
    }

    public enum PauseCause { PhoneCall, FocusChange, AudioJack, User};
    public static PauseCause pauseCause;

    public static ArrayListWithMaximum<IRCChat.IrcMessage> ircChat, ircLog;

    public static TelephonyManager teleMgr;

    public static void formatAlertDialogButtons(AlertDialog d)
    {
        d.getButton(Dialog.BUTTON_POSITIVE).setBackgroundResource(R.drawable.blue_button);
        d.getButton(Dialog.BUTTON_POSITIVE).setPadding((int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP));
        d.getButton(Dialog.BUTTON_NEGATIVE).setBackgroundResource(R.drawable.blue_button);
        d.getButton(Dialog.BUTTON_NEGATIVE).setPadding((int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP));
        ((View)d.getButton(Dialog.BUTTON_POSITIVE).getParent()).setBackgroundResource(R.color.backClr);
        View msg = ((ViewGroup) ((ViewGroup)(d.getButton(Dialog.BUTTON_POSITIVE).getParent().getParent().getParent())).getChildAt(1)).getChildAt(0);
        msg.setBackgroundResource(R.color.backClr);
        if(((TextView)((android.widget.ScrollView)msg).getChildAt(0))!=null)
            ((TextView)((android.widget.ScrollView)msg).getChildAt(0)).setTextColor(d.getContext().getResources().getColor(R.color.txtClr));
        //ViewGroup x = (ViewGroup) ((ViewGroup) ((ViewGroup)(d.getButton(Dialog.BUTTON_POSITIVE).getParent().getParent().getParent())).getChildAt(0)).getChildAt(0);
        //x.setBackgroundResource(R.color.backClr);
    }

    public static void init(final Context c)
    {
        String TAG = StaticBlob.TAG();
        if(StaticBlob.playerInfo!=null)
            return;
        //Get the main app settings (static variables)
        StaticBlob.SHOW_LIST_VIDEO = c.getResources().getStringArray(R.array.shows_video);
        StaticBlob.SHOW_LIST_AUDIO = c.getResources().getStringArray(R.array.shows_audio);
        StaticBlob.SHOW_LIST = c.getResources().getStringArray(R.array.shows);
        StaticBlob.europeanDates = android.text.format.DateFormat.getDateFormatOrder(c)[0]!='M';
        StaticBlob.sdfDestination = new SimpleDateFormat(StaticBlob.europeanDates ? "dd/MM/yyyy" : "MM/dd/yyyy");
        StaticBlob.DP = c.getResources().getDisplayMetrics().density;
        //Read the storage dir
        StaticBlob.storage_path = PreferenceManager.getDefaultSharedPreferences(c).getString("storage_path", "callisto");
        if(!StaticBlob.storage_path.startsWith("/"))
            StaticBlob.storage_path = Environment.getExternalStorageDirectory().toString() + File.separator + StaticBlob.storage_path;
        new File(StaticBlob.storage_path).mkdirs();

        Log.i(TAG, "Path is: " + StaticBlob.storage_path + ", " + new File(StaticBlob.storage_path).exists());


        try {
            StaticBlob.appVersion = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {}

        //Initialize some static variables
        StaticBlob.mNotificationManager =  (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        StaticBlob.alarmPrefs = c.getApplicationContext().getSharedPreferences(StaticBlob.PREF_FILE, c.MODE_PRIVATE);
        StaticBlob.playDrawable = c.getResources().getDrawable(R.drawable.ic_action_playback_play);
        StaticBlob.pauseDrawable = c.getResources().getDrawable(R.drawable.ic_action_playback_pause);
        StaticBlob.databaseConnector = new DatabaseConnector(c);
        StaticBlob.databaseConnector.open();
        if(StaticBlob.playerInfo==null)
            StaticBlob.playerInfo = new PlayerInfo(c);
        else
            StaticBlob.playerInfo.update(c);

        //Create the stuff for the IRC
        int irc_scrollback=500;
        try {
            irc_scrollback = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(c).getString("irc_max_scrollback", "500"));
        } catch(Exception e){}
        ircChat = new ArrayListWithMaximum<IRCChat.IrcMessage>();
        ircLog = new ArrayListWithMaximum<IRCChat.IrcMessage>();
        ircChat.setMaximumCapacity(irc_scrollback);
        ircLog.setMaximumCapacity(irc_scrollback);

        //Creates the dialog for live error
        StaticBlob.errorDialog = new Dialog(c);
        TextView t = new TextView(c);
        t.setText(R.string.live_error);
        StaticBlob.errorDialog.setContentView(t);
        StaticBlob.errorDialog.setTitle(R.string.live_error_title);

        //Create the wifi lock
        WifiManager wm = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        if(StaticBlob.Live_wifiLock==null || !StaticBlob.Live_wifiLock.isHeld())
            StaticBlob.Live_wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "Callisto_live");

        //Create the dialog for live selection
        StaticBlob.liveDg = new AlertDialog.Builder(c)
                .setTitle(R.string.switch_to_live)
                .setView(((LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.live_select, null))
                .create();

        //Create the phone state listener.
        // i.e., that which is able to pause when a phone call is received
        pauseCause = PauseCause.User;

        phoneStateListener = new PhoneStateListenerImpl(c);
        teleMgr = (TelephonyManager) c.getSystemService(c.TELEPHONY_SERVICE);
        if(teleMgr != null) {
            teleMgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        //Create the audiojack receiver
        CallistoService.audioJackReceiver = new AudioJackReceiver();
        CallistoService.audioJackReceiver.contextForPreferences = c;
        c.startService(new Intent(c, CallistoService.class));


        //Sets the player error and completion errors
        StaticBlob.trackCompleted = new OnCompletionListenerWithContext();
        StaticBlob.trackCompletedBug = new OnErrorListenerWithContext();
        StaticBlob.mplayerPrepared = new OnPreparedListenerWithContext()
        {
            @Override
            public void onPrepared(MediaPlayer arg0) {
                String TAG = StaticBlob.TAG();

                Log.i(TAG, "Prepared, seeking to " + StaticBlob.playerInfo.position);
                StaticBlob.mplayer.seekTo(StaticBlob.playerInfo.position);
                StaticBlob.playerInfo.length = StaticBlob.mplayer.getDuration()/1000;
                StaticBlob.databaseConnector.putLength(StaticBlob.playerInfo.title, StaticBlob.mplayer.getDuration());

                Log.i(TAG, "Prepared, length is " + StaticBlob.playerInfo.length);
                try {
                    ImageButton ib = ((ImageButton)((Activity)c).findViewById(R.id.playPause));
                    ib.setImageDrawable(StaticBlob.pauseDrawable);
                } catch(NullPointerException e) {
                    Log.w(TAG, "Could not find the button");
                } //Case for when ib is not found
                catch(ClassCastException e) {} //Case for when it's the widget
                Log.i(TAG, (startPlaying ? "" : "NOT ") + "Starting to play: " + StaticBlob.playerInfo.title);
                if(!startPlaying)
                {
                    StaticBlob.playerInfo.update(c);
                    return;
                }

                if(audioFocus==null && android.os.Build.VERSION.SDK_INT >= 11)
                    audioFocus = new OnAudioFocusChangeListenerImpl(c);

                StaticBlob.mplayer.start();
                StaticBlob.playerInfo.isPaused = false;
                StaticBlob.playerInfo.update(c);

                if(pd!=null)
                {
                    pd.setOnCancelListener(null);
                    pd.dismiss();
                }
                pd=null;
                //Update the widgets
                CallistoWidget.updateAllWidgets(c);
            }
        };
    }

    public static String TAG()
    {
        StackTraceElement stacktrace = Thread.currentThread().getStackTrace()[3];
        return
        "CALLISTO:" +
        stacktrace.getClassName().substring("com.qweex.callisto".length()+1) + ":" + stacktrace.getMethodName()
                + "::" + stacktrace.getLineNumber();
    }

    public static void deleteItem(Context con, long id, boolean video)      //id is from EPISODES
    {
        String TAG = TAG();
        //1. Get track info & create path
        Cursor c = StaticBlob.databaseConnector.getOneEpisode(id);
        if(c.getCount()==0)
        {
            Log.e(TAG, "Trying to delete an item that does not exist in the db");
            return;
        }
        c.moveToFirst();
        String show = c.getString(c.getColumnIndex("show"));
        String date = c.getString(c.getColumnIndex("date"));
        SimpleDateFormat sdfDestination = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = sdfDestination.format(StaticBlob.sdfRaw.parse(date));
        } catch (ParseException e) {
            Log.e(TAG+":ParseException", "Error parsing a date that has already been parsed:");
            Log.e(TAG+":ParseException", date);
            Log.e(TAG+":ParseException", "(This should SERIOUSLY never happen).");
        }
        String title = c.getString(c.getColumnIndex("title"));
        String ext = EpisodeDesc.getExtension(c.getString(c.getColumnIndex(video?"vidlink":"mp3link")));

        File target = new File(StaticBlob.storage_path + File.separator + show);
        target = new File(target, date + "__" + makeFileFriendly(title) + ext);

        //2. Delete file
        if(target.exists())
            target.delete();
        //3. Remove from Queue
        Cursor q = StaticBlob.databaseConnector.findInQueue(id,video);
        if(q.getCount()>0)
        {
            long idq;
            q.moveToFirst();
            do {
                idq = q.getLong(q.getColumnIndex("_id"));
                //4. If it was the current item in the queue AND player was playing, move to next
                if(q.getInt(q.getColumnIndex("current"))>0)
                {
                    PlayerControls.changeToTrack(con, 1, StaticBlob.mplayer!=null && !StaticBlob.playerInfo.isPaused);
                }
                StaticBlob.databaseConnector.deleteQueueItem(idq);
            } while(q.moveToNext());
        }
    }


}
