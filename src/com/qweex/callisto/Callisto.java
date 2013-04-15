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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import android.content.*;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.qweex.callisto.listeners.OnCompletionListenerWithContext;
import com.qweex.callisto.listeners.OnErrorListenerWithContext;
import com.qweex.callisto.listeners.OnPreparedListenerWithContext;
import com.qweex.callisto.podcast.*;
import com.qweex.callisto.podcast.Queue;
import com.qweex.callisto.receivers.AudioJackReceiver;
import com.qweex.utils.ImgTxtButton;
import com.qweex.utils.QweexUtils;
import com.qweex.utils.XBMCStyleListViewMenu;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.qweex.callisto.bonuses.Bacon;
import com.qweex.callisto.donate.Donate;
import com.qweex.callisto.irc.IRCChat;
import com.qweex.callisto.widgets.CallistoWidget;
import com.qweex.utils.UnfinishedParseException;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.LinearLayout.LayoutParams;


//Task Tags: todo clean feature fixme wtf idea

//CLEAN: Rename IDs in layout XML
//CLEAN: Strings.xml

/** This class is the main activity, but also houses all of the static variables needed for the app.
 Things like the MediaPlayer, the Notifications, and the player Listeners are all here.
 @author MrQweex
 */
public class Callisto extends Activity
{
    //-----Static members-----
    // These are used across the multiple activities
    /** The media players are used across the app to control playing either live or podcast'ed episodes. */
    public static MediaPlayer mplayer = null, live_player;
    /** Used to connect to the SQLlite database. For more information, see the DatabaseConnector class.*/
    public static DatabaseConnector databaseConnector;
    /** Notifications are used in various activities */
    public static Notification notification_download, notification_playing, notification_chat, notification_alarm;
    /** The path on the SD card to store downloaded episodes. Customizable via the settings dialog. */
    public static String storage_path;
    /** The number of downloads that has been queued up. */
    public static int downloading_count = 0;
    /** The current download number. */
    public static int current_download = 1;
    /** One of the various date formats used across various Activities. The usage for most should be self-documenting from the name. */
    public static final SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss"),
            sdfRawSimple1 = new SimpleDateFormat("yyyyMMdd"),
            sdfRawSimple2 = new SimpleDateFormat("HHmmss"),
            sdfTime = new SimpleDateFormat("hh:mm aa"),
            sdfSource = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
    public static SimpleDateFormat sdfDestination;
    public static final SimpleDateFormat sdfFile = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat sdfHuman = new SimpleDateFormat("MMM d");
    public static final SimpleDateFormat sdfHumanLong = new SimpleDateFormat("MMM d, yyyy");
    /** Shared Drawable resources for updating the player control graphics. */
    public static Drawable playDrawable, pauseDrawable;
    /** An instance of the PlayerInfo class, to keep track of the info of a track when updating the player controls across the activities. */
    public static PlayerInfo playerInfo;
    /** Simply the shared resources of the project, for things like strings, colors, drawables, etc. */
    public static Resources RESOURCES;
    /** The value of a dip, to be used programatically when updating a view. */
    public static float DP;
    /** One of the Views for the IRC client. Having them static lets them be updated at any time, and not be destroyed when you leave the IRC screen. */
    public static TextView chatView, logView;
    /** If you should be a "weirdo" that likes dates in a logically correct manner. */
    public static boolean europeanDates;

    //------Local variables-----
    //TODO: wtf
    static TextView timeView;
    //TODO: wtf
    static int current;
    //TODO: wtf
    static ProgressBar timeProgress;
    //TODO: wtf
    private Timer timeTimer = null;
    /** Titles for the tabletMenu */
    String[] tabletMenu = new String[] {"Play", "Live", "Plan", "Chat", "Contact", "Donate"};
    /** Ids used for onclicklisteners and the tablet launching activities */
    int[] buttonIds = new int[] {R.id.listen, R.id.live, R.id.plan, R.id.chat, R.id.contact, R.id.donate};

    // Menu ID for this activity
    private static final int STOP_ID=Menu.FIRST+1;
    private static final int SETTINGS_ID=STOP_ID+1;
    private static final int MORE_ID=SETTINGS_ID+1;
    private static final int RELEASE_ID=MORE_ID+1;
    private static final int BACON_ID=RELEASE_ID+1;
    private static final int QUIT_ID=BACON_ID+1,
            CHRISROLL_ID=QUIT_ID+1;
    private static final int SAVE_POSITION_EVERY = 40,
            CHECK_LIVE_EVERY = 400;	//Cycles, not necessarily seconds

    public static OnCompletionListenerWithContext trackCompleted;
    public static OnErrorListenerWithContext trackCompletedBug;
    public static OnPreparedListenerWithContext mplayerPrepared;
    public static final int NOTIFICATION_ID = 1337;
    private static NotificationManager mNotificationManager;
    public static SharedPreferences alarmPrefs;
    public final static String PREF_FILE = "alarms";

    /** The status of the live player; true if it is playing, false if it is paused or not in use. */
    public static boolean live_isPlaying = false;
    /** The Version of Callisto. Set to -1 if it cannot be determined. */
    public static int appVersion = -1;
    /** Contains whether or not the activity has been launched via the widget. **/
    public static boolean is_widget;
    /** The Dialog for displaying what is new in this version **/
    private Dialog news;


    //Live stuff
    /** Locks the wifi when downloading or streaming **/
    public static WifiLock Live_wifiLock;
    //TODO: wtf
    private static Dialog errorDialog, liveDg;
    /** The url that is used to report statistics -completely anonymous- to the developer when the live fails. **/
    private final static String errorReportURL = "http://software.qweex.com/error_report.php";
    //TODO: wtf
    private static LIVE_FetchInfo LIVE_update = null;

    //TODO: wtf
    private static TimerRunnable clock;


    /** Called when the activity is first created. Sets up the view for the main screen and additionally initiates many of the static variables for the app.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
    //@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //**********************Do the app creation stuff - The stuff that is done because the app is initializing for the first time**********************************//
        //Get the main app settings (static variables)
        RESOURCES = getResources();
        europeanDates = android.text.format.DateFormat.getDateFormatOrder(this)[0]!='M';
        sdfDestination = new SimpleDateFormat(europeanDates ? "dd/MM/yyyy" : "MM/dd/yyyy");
        DP = RESOURCES.getDisplayMetrics().density;
        Callisto.storage_path = PreferenceManager.getDefaultSharedPreferences(this).getString("storage_path", "callisto");
        try {
            Callisto.appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {}
        //This is the most reliable way I've found to determine if it is landscape
        boolean isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();

        //Initialize some static variables
        mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        alarmPrefs = getApplicationContext().getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        Callisto.playDrawable = RESOURCES.getDrawable(R.drawable.ic_action_playback_play);
        Callisto.pauseDrawable = RESOURCES.getDrawable(R.drawable.ic_action_playback_pause);
        Callisto.databaseConnector = new DatabaseConnector(Callisto.this);
        Callisto.databaseConnector.open();
        if(Callisto.playerInfo==null)
            Callisto.playerInfo = new PlayerInfo(Callisto.this);
        else
            Callisto.playerInfo.update(Callisto.this);

        //Create the views for the the IRC
        chatView = new TextView(this);
        chatView.setGravity(Gravity.BOTTOM);
        String irc_scrollback=PreferenceManager.getDefaultSharedPreferences(this).getString("irc_max_scrollback", "500");
        chatView.setMaxLines(Integer.parseInt(irc_scrollback));
        logView = new TextView(this);
        logView.setGravity(Gravity.BOTTOM);
        logView.setMaxLines(Integer.parseInt(irc_scrollback));

        //Creates the dialog for live error
        errorDialog = new Dialog(this);
        TextView t = new TextView(this);
        t.setText("An error occurred. This may be a one time thing, or your device does not support the stream. You can try going to JBlive.info to see if it's just this app.");
        errorDialog.setContentView(t);
        errorDialog.setTitle("By the beard of Zeus!");

        //Create the wifi lock
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(Live_wifiLock==null || !Live_wifiLock.isHeld())
            Live_wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "Callisto_live");

        //Create the dialog for live selection
        liveDg = new AlertDialog.Builder(this)
                .setTitle("Switch from playlist to live?")
                .setView(((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.live_select, null))
                .create();

        //Create the phone state listener.
        // i.e., that which is able to pause when a phone call is received
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    if(Callisto.live_isPlaying
                            || !Callisto.playerInfo.isPaused)
                    {
                        playPause(Callisto.this, null);
                    }
                    //Incoming call: Pause music
                } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                    //Not in call: Play music
                } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    if(Callisto.live_isPlaying
                            || !Callisto.playerInfo.isPaused)
                    {
                        playPause(Callisto.this, null);
                    }
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        //Create the audiojack receiver
        CallistoService.audioJackReceiver = new AudioJackReceiver();
        CallistoService.audioJackReceiver.contextForPreferences = this;
        startService(new Intent(this, CallistoService.class));

        //Sets the player error and completion errors
        trackCompleted = new OnCompletionListenerWithContext();
        trackCompletedBug = new OnErrorListenerWithContext();
        mplayerPrepared = new OnPreparedListenerWithContext()
        {
            @Override
            public void onPrepared(MediaPlayer arg0) {

                Log.i("*:mplayer:onPrepared", "Prepared, seeking to " + Callisto.playerInfo.position);
                Callisto.mplayer.seekTo(Callisto.playerInfo.position);
                Callisto.playerInfo.length = Callisto.mplayer.getDuration()/1000;
                Callisto.databaseConnector.putLength(Callisto.playerInfo.title, Callisto.mplayer.getDuration());

                Log.i("*:mplayer:onPrepared", "Prepared, length is " + Callisto.playerInfo.length);
                try {
                    ImageButton ib = ((ImageButton)((Activity)c).findViewById(R.id.playPause));
                    ib.setImageDrawable(Callisto.pauseDrawable);
                } catch(NullPointerException e) {
                    Log.w("*:mplayer:onPrepared", "Could not find the button");
                } //Case for when ib is not found
                catch(ClassCastException e) {} //Case for when it's the widget
                Log.i("*:mplayer:onPrepared", (startPlaying ? "" : "NOT ") + "Starting to play: " + Callisto.playerInfo.title);
                if(!startPlaying)
                {
                    Callisto.playerInfo.update(c);
                    return;
                }
                Log.i("*:mplayer:onPrepared", "HERP");
                Callisto.mplayer.start();
                Callisto.playerInfo.isPaused = false;
                Callisto.playerInfo.update(c);

                if(pd!=null)
                {
                    pd.setOnDismissListener(null);
                    pd.dismiss();
                }
                pd=null;
            }
        };

        //Update shows
        SharedPreferences pf = PreferenceManager.getDefaultSharedPreferences(this);
        int lastVersion = pf.getInt("appVersion", 0);

        //Check for pending downloads. If there are any.....um, do something.
        if(!pf.getString(DownloadList.ACTIVE, "").equals("") && !pf.getString(DownloadList.ACTIVE, "").equals("|"))
        {
            //TODO: Display message
            //Callisto.downloading_count = pf.getString("ActiveDownloads", "").length() - pf.getString("ActiveDownloads", "").replaceAll("\\|", "").length() - 1;
            //new DownloadTask(Callisto.this).execute();
        }

        //Check to see if there is any external storage to write to
        if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            Toast.makeText(this, "There is currently no external storage to write to.", Toast.LENGTH_SHORT).show();

        //Display the release notes if recently updated
        if((Callisto.appVersion>lastVersion))
        {
            showUpdateNews();
            SharedPreferences.Editor editor = pf.edit();
            editor.putInt("appVersion", Callisto.appVersion);
            editor.commit();
        }

        //**********************Do the activity creation stuff - The stuff that is specific to the Callisto mainscreen activity**********************//
        //Set the content view
        boolean isTablet= QweexUtils.isTabletDevice(this);
        isTablet |= PreferenceManager.getDefaultSharedPreferences(this).getBoolean("new_mainscreen", false);
        if(isTablet)
            initTablet();
        else
            initPhone();

    }

    /** Initiating the activity for a Phone formfactor device OR for any device running 2.1 or earlier. (I dunno if Eclair tablets exist but if they do, they shouldn't.)
     * Note that everything in here should be specific to THIS activity. Everything concerning the app in general should
     * be in the OnCreate.
     */
    void initPhone()
    {
        setContentView(R.layout.main);
        //This loop sets the onClickListeners and adjusts the button settings if the view is landscape
        ImgTxtButton temp;
        for(int i=0; i<buttonIds.length; i++)
        {
            temp = (ImgTxtButton)findViewById(buttonIds[i]);
            temp.setOnClickListener(startAct);
        }

        //Set the player on click listeners; this is usually done by the PlayerInfo object, when switching activities.
        findViewById(R.id.playPause).setOnClickListener(Callisto.playPauseListener);
        findViewById(R.id.playlist).setOnClickListener(Callisto.playlist);
        findViewById(R.id.seek).setOnClickListener(Callisto.seekDialog);
        findViewById(R.id.next).setOnClickListener(Callisto.next);
        findViewById(R.id.previous).setOnClickListener(Callisto.previous);

        findViewById(R.id.live).setOnClickListener(LIVE_PlayButton);
    }

    /** Initiating the activity for a Tablet device. */
    void initTablet()
    {
        setContentView(R.layout.main_tablet);

        ((View)findViewById(R.id.listView).getParent()).setBackgroundResource(R.drawable.tabback);
        XBMCStyleListViewMenu slvm = (XBMCStyleListViewMenu) this.findViewById(R.id.listView);
        slvm.setSelectedSize(100);
        slvm.setData(Arrays.asList(tabletMenu));
        slvm.setOnMainItemClickListener(new XBMCStyleListViewMenu.OnMainItemClickListener()
        {
            @Override
            public void onMainItemClick(View v, int position) {
                View dummy = new View(Callisto.this);
                dummy.setId(buttonIds[position]);
                if(buttonIds[position]==R.id.live)
                    LIVE_PlayButton.onClick(dummy);
                else
                    startAct.onClick(dummy);
            }
        });

        //Set the player on click listeners; this is usually done by the PlayerInfo object, when switching activities.
        findViewById(R.id.playPause).setOnClickListener(Callisto.playPauseListener);
        findViewById(R.id.playlist).setOnClickListener(Callisto.playlist);
        findViewById(R.id.seek).setOnClickListener(Callisto.seekDialog);
        findViewById(R.id.next).setOnClickListener(Callisto.next);
        findViewById(R.id.previous).setOnClickListener(Callisto.previous);
    }

    /** Called when the activity is going to be destroyed. */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(news!=null)
            news.dismiss();
        Log.v("Callisto:onDestroy", "Destroying main activity");
        if(errorDialog !=null)
            errorDialog.dismiss();
        if(LIVE_PreparedListener.pd!=null)
            LIVE_PreparedListener.pd.dismiss();
    }

    /** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
    @Override
    public void onResume()
    {
        super.onResume();
        if(LIVE_PreparedListener.pd!=null)
            LIVE_PreparedListener.pd.show();
        Log.v("Callisto:onResume", "Resuming main activity");
        if(CallistoService.audioJackReceiver!=null)
            CallistoService.audioJackReceiver.contextForPreferences = Callisto.this;
        if(Callisto.playerInfo!=null)
            Callisto.playerInfo.update(Callisto.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, STOP_ID, 0, RESOURCES.getString(R.string.stop)).setIcon(R.drawable.ic_action_playback_stop);
        menu.add(0, SETTINGS_ID, 0, RESOURCES.getString(R.string.settings)).setIcon(R.drawable.ic_action_settings);
        SubMenu theSubMenu = menu.addSubMenu(0, MORE_ID, 0, RESOURCES.getString(R.string.more)).setIcon(R.drawable.ic_action_more);
        theSubMenu.add(0, RELEASE_ID, 0, RESOURCES.getString(R.string.release_notes)).setIcon(R.drawable.ic_action_info);

        //Stuffs for the donations stuffs
        if(QuickPrefsActivity.packageExists(QuickPrefsActivity.DONATION_APP,this))
        {
            String baconString = "Get Bacon";
            File target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + "extras");
            if(target.exists())
                baconString = RESOURCES.getString(R.string.bacon);
            theSubMenu.add(0, BACON_ID, 0, baconString).setIcon(R.drawable.bacon).setEnabled(QuickPrefsActivity.packageExists(QuickPrefsActivity.DONATION_APP, this));
            theSubMenu.add(0, CHRISROLL_ID, 0, "Chrisrolled!").setEnabled(QuickPrefsActivity.packageExists(QuickPrefsActivity.DONATION_APP, this));
        }

        menu.add(0, QUIT_ID, 0, RESOURCES.getString(R.string.quit)).setIcon(R.drawable.ic_action_io);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case STOP_ID:
                Callisto.stop(this);
                return true;
            case SETTINGS_ID:
                startActivity(new Intent(this, QuickPrefsActivity.class));
                return true;
            case RELEASE_ID:
                showUpdateNews();
                return true;
            case BACON_ID:
                File target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + "extras");
                if(!target.exists())
                {
                    new downloadExtras().execute((Void[])null);
                    return true;
                }
                Intent i = new Intent(Callisto.this, Bacon.class);
                startActivity(i);
                return true;
            case CHRISROLL_ID:
                Uri uri = Uri.parse("http://www.youtube.com/watch?v=98E2hfxF8oE");
                uri = Uri.parse("vnd.youtube:"  + uri.getQueryParameter("v"));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            case QUIT_ID:
                finish();
                mNotificationManager.cancelAll();
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Creates the layout for various activities, adding the Player Controls.
     *  It essentially takes whatever "mainView" is and wraps it and the Controls in a vertical LinearLayout.
     *  @param c The current context in which to build.
     *  @param mainView The main view of the Activity to be wrapped above the player controls
     */
    public static void build_layout(Context c, View mainView)
    {
        Log.v("*:build_layout", "Building the layout");

        // Inflate the controls and the empty view
        View controls = ((LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.controls, null, false);
        TextView empty = new TextView(c);
        empty.setId(android.R.id.empty);
        empty.setBackgroundColor(c.getResources().getColor(R.color.backClr));
        empty.setTextColor(c.getResources().getColor(R.color.txtClr));

        //Create what will be the total layout
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams   //main params
                (LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1f);
        LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams   //child params
                (LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f);
        LinearLayout.LayoutParams xParams = new LinearLayout.LayoutParams   //main params
                (LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 0f);

        //Build a horizontal view for holding the mainView and the empty view
        LinearLayout horl = new LinearLayout(c);
        horl.setOrientation(LinearLayout.HORIZONTAL);
        horl.addView(mainView, xParams);
        horl.addView(empty, cParams);
        horl.setBackgroundColor(RESOURCES.getColor(R.color.backClr));

        //Add the views to the total layout
        layout.addView(horl, mParams);
        layout.addView(controls, cParams);
        ((Activity)c).setContentView(layout);

        //Set the control listeners
        controls.findViewById(R.id.playPause).setOnClickListener(Callisto.playPauseListener);
        controls.findViewById(R.id.playlist).setOnClickListener(Callisto.playlist);
        controls.findViewById(R.id.seek).setOnClickListener(Callisto.seekDialog);
        controls.findViewById(R.id.next).setOnClickListener(Callisto.next);
        controls.findViewById(R.id.previous).setOnClickListener(Callisto.previous);
        Log.v("*:build_layout", "Finished building the layout");
    }


    //*********************************** Things for updating the player info ***********************************//

    /** Essentially a struct with a few functions to handle the player, particularly updating it when switching activities. */
    public class PlayerInfo
    {
        /** A reference to the MediaPlayer currently in use; either the main one or the live one. **/
        public MediaPlayer player;
        /** Data of the currently playing track **/
        public String title, show, date;
        /** Track information of the currently playing track, measured in seconds **/
        public int position = 0, length = 0;
        /** Holds whether or not the player is paused. **/
        public boolean isPaused = true;

        /** Constructor for the PlayerInfo class. Good to call when first creating the player controls, to set then to something.
         *  @param c The context for the current Activity.
         */
        public PlayerInfo(Context c)
        {
            TextView titleView = (TextView) ((Activity)c).findViewById(R.id.titleBar);
            Log.v("PlayerInfo()", "Initializing PlayerInfo, queue size=" +  Callisto.databaseConnector.queueCount());
            if(titleView!=null)
                titleView.setText(RESOURCES.getString(R.string.queue_size) + ": " + Callisto.databaseConnector.queueCount());
        }

        /** Updates the player controls, like the title and times. Used excessively when changing Activities.
         * @param c The context for the current Activity.
         */
        public void update(Context c)
        {
            //Update the context for the receiver, unrelated
            CallistoService.audioJackReceiver.contextForPreferences = c;

            //If it's a widget there is no need to update the controls.
            if(is_widget)
            {
                is_widget = false;
                return;
            }

            Callisto.trackCompleted.setContext(c);

            //Retrieve the length & position, depending on if it is video or audio
            if(VideoActivity.videoView!=null)
            {
                length = VideoActivity.videoView.getDuration()/1000;
                position = VideoActivity.videoView.getCurrentPosition()/1000;
            }
            else if(Callisto.mplayer!=null)
            {
                length = Callisto.mplayer.getDuration()/1000;
                position = Callisto.mplayer.getCurrentPosition()/1000;
            }

            Log.v("*:update", "Update - Title: " + title);

            //titleView
            TextView titleView = (TextView) ((Activity)c).findViewById(R.id.titleBar);
            if(titleView==null)
                Log.w("Callisto:update", "Could not find view: " + "titleView");
            else
            if(title==null && Callisto.live_player==null)
                titleView.setText("Playlist size: " + Callisto.databaseConnector.queueCount());
            else if(Callisto.live_player==null)
                titleView.setText(title + " - " + show);
            else
                titleView.setText(title + " - JB Radio");

            //timeView
            Callisto.timeView = (TextView) ((Activity)c).findViewById(R.id.timeAt);
            if(Callisto.timeView==null)
                Log.w("Callisto:update", "Could not find view: " + "TimeView");
            else if(Callisto.live_player!=null)
            {
                timeView.setText("Next ");
                timeView.setEnabled(false);
            }
            else
            {
                timeView.setText(formatTimeFromSeconds(title==null ? 0 : position));
                timeView.setEnabled(true);
            }

            //lengthView
            TextView lengthView = (TextView) ((Activity)c).findViewById(R.id.length);
            if(lengthView==null)
                Log.w("Callisto:update", "Could not find view: " + "lengthView");
            else if(Callisto.live_player!=null)
            {
                lengthView.setText(show);
                lengthView.setEnabled(false);
            }
            else
            {
                lengthView.setText(formatTimeFromSeconds(title==null ? 0 : length));
                lengthView.setEnabled(true);
            }

            //timeProgress
            Callisto.timeProgress = (ProgressBar) ((Activity)c).findViewById(R.id.timeProgress);
            if(Callisto.timeProgress==null)
                Log.w("Callisto:update", "Could not find view: " + "timeProgress");
            else if(Callisto.live_player!=null)
            {
                timeProgress.setMax(1);
                timeProgress.setProgress(0);
                timeProgress.setEnabled(false);
            }
            else
            {
                timeProgress.setMax(length);
                timeProgress.setProgress(title==null ? 0 : position);
                timeProgress.setEnabled(true);
            }


            ImageButton play = (ImageButton) ((Activity)c).findViewById(R.id.playPause);
            if(play==null)
                Log.w("Callisto:update", "Could not find view: " + "playPause");
            else if(Callisto.live_player!=null)
            {
                if(Callisto.live_isPlaying)
                    play.setImageDrawable(Callisto.pauseDrawable);
                else
                    play.setImageDrawable(Callisto.playDrawable);
            } else
            {
                if(Callisto.playerInfo.isPaused)
                    play.setImageDrawable(Callisto.playDrawable);
                else
                    play.setImageDrawable(Callisto.pauseDrawable);
            }

            //Disables views if it is live
            if(((Activity)c).findViewById(R.id.seek)!=null)
                ((Activity)c).findViewById(R.id.seek).setEnabled(Callisto.live_player==null);
            if(((Activity)c).findViewById(R.id.previous)!=null)
                ((Activity)c).findViewById(R.id.previous).setEnabled(Callisto.live_player==null);
            if(((Activity)c).findViewById(R.id.next)!=null)
                ((Activity)c).findViewById(R.id.next).setEnabled(Callisto.live_player==null);

            if(timeTimer==null)
            {
                Log.i("Callisto:PlayerInfo:update","Starting timer");
                timeTimer = new Timer();
                timeTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        TimerMethod();
                    }
                }, 0, 250);
            }
        }

        /** Clears all internal values to their respective blank values (i.e. null or 0). */
        public void clear()
        {
            title = show = date = null;
            position = length = 0;
            isPaused = true;
        }
    }

    /** A simple menthod to run TimerRunnable in the UI Thread to allow it to update Views. */
    private void TimerMethod()
    {
        if(clock==null)
            clock = new TimerRunnable();
        TimerHandler.post(clock);
//		Callisto.this.runOnUiThread(TimerRunnable);
    }

    private Handler TimerHandler = new Handler();

    /** A runnable to be used in conjunction with the update() function. Updates the player time every set amount of time. */
    class TimerRunnable implements Runnable
            //Runnable TimerRunnable = new Runnable()
    {
        public int i=0;
        public void run()
        {
            i++;
            if(Callisto.live_player!=null && Callisto.live_isPlaying)
            {
                if(i==Callisto.CHECK_LIVE_EVERY)
                {
                    LIVE_update = new LIVE_FetchInfo();
                    Callisto.LIVE_update.execute((Void [])null);
                    i=0;
                }
                return;
            }
            if((Callisto.mplayer==null || !Callisto.mplayer.isPlaying()) &&
                    (VideoActivity.videoView==null || !VideoActivity.videoView.isPlaying()))
            {
                i=0;
                return;
            }
            try {
                if(VideoActivity.videoView!=null)
                    Callisto.playerInfo.position = VideoActivity.videoView.getCurrentPosition();
                else
                    Callisto.playerInfo.position = Callisto.mplayer.getCurrentPosition();
                current = Callisto.playerInfo.position/1000;
                timeProgress.setProgress(current);
                timeView.setText(formatTimeFromSeconds(current));
                Log.i("Callisto:TimerMethod", "Timer mon " + Callisto.playerInfo.position);

                Log.i("currentprogress", Queue.currentProgress + " !" + (Queue.currentProgress==null?"NULL":"NOTNULL"));
                if(Queue.currentProgress!=null)
                {
                    Log.i("currentprogress", current + "/" + Callisto.playerInfo.length);
                    double xy = (current*100.0) / Callisto.playerInfo.length;
                    Log.i("currentprogress", xy + " !");
                    Queue.currentProgress.setProgress((int)(Double.isNaN(xy) ? 0 : xy));
                }


                if(i==Callisto.SAVE_POSITION_EVERY)
                {
                    i=0;
                    try {
                        Log.v("Callisto:TimerMethod", "Updating position: " + Callisto.playerInfo.position);
                        Cursor queue = Callisto.databaseConnector.currentQueueItem();
                        queue.moveToFirst();
                        Long identity = queue.getLong(queue.getColumnIndex("identity"));
                        Callisto.databaseConnector.updatePosition(identity, Callisto.playerInfo.position);
                    } catch(NullPointerException e)
                    {
                        Log.e("*:TimerRunnable", "NullPointerException when trying to update timer!");
                    }
                }

            } catch(Exception e)
            {
                System.out.println("HERP!!!");
            }
        }
    };

    static public Handler updateHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(Callisto.LIVE_PreparedListener.c!=null)
                playerInfo.update(Callisto.LIVE_PreparedListener.c);
        }
    };

    //*********************************** Listeners and methods for controlling the players ***********************************//

    /** Listener for the Next (">") button. Proceeds to the next track, if there is one. */
    public static OnClickListener next = new OnClickListener()
    {
        @Override public void onClick(View v)
        {
            changeToTrack(v.getContext(), 1, !Callisto.playerInfo.isPaused);
        }
    };
    /** Listener for the Previous ("<") button. Goes back to the previous track, if there is one. */
    public static OnClickListener previous = new OnClickListener()
    {
        @Override public void onClick(View v)
        {
            changeToTrack(v.getContext(), -1, !Callisto.playerInfo.isPaused);
        }
    };

    /** Changes position in queue to another song and optionally starts playing it.
     * @param c The context of the current activity.
     * @param previousOrNext >0 if it should play the next track, <0 for the previous, and 0 for the current
     * @param sp true if the player should start playing when it changes tracks, false otherwise
     */
    public static void changeToTrack(Context c, int previousOrNext, boolean sp)
    {
        Cursor queue = Callisto.databaseConnector.advanceQueue(previousOrNext);

        //If there are no items in the queue, stop the player
        if(queue==null || queue.getCount()==0)
        {
            Log.v("*:changeToTrack", "Queue is empty. Pausing.");
            ImageButton x = (ImageButton) ((Activity)c).findViewById(R.id.playPause);
            if(x!=null)
                x.setImageDrawable(Callisto.pauseDrawable);
            mNotificationManager.cancel(NOTIFICATION_ID);
            Callisto.stop(c);
            return;
        }

        Log.v("*:changeToTrack", "Queue Size: " + queue.getCount());
        queue.moveToFirst();
        //The queue merely stores the identity (_id) of the entrie's position in the main SQL
        //After obtaining it, we can get all the information about it
        Long id = queue.getLong(queue.getColumnIndex("_id"));
        Long identity = queue.getLong(queue.getColumnIndex("identity"));
        boolean isStreaming = queue.getInt(queue.getColumnIndex("streaming"))>0;
        boolean isVideo = queue.getInt(queue.getColumnIndex("video"))>0;
        Cursor theTargetTrack = Callisto.databaseConnector.getOneEpisode(identity);
        theTargetTrack.moveToFirst();

        //Retrieve all of the playerInfo about the new track
        String media_location;
        Callisto.playerInfo.title = theTargetTrack.getString(theTargetTrack.getColumnIndex("title"));
        Callisto.playerInfo.position = theTargetTrack.getInt(theTargetTrack.getColumnIndex("position"));
        Callisto.playerInfo.date = theTargetTrack.getString(theTargetTrack.getColumnIndex("date"));
        Callisto.playerInfo.show = theTargetTrack.getString(theTargetTrack.getColumnIndex("show"));
        Log.i("*:changeToTrack", "Loading info: " + Callisto.playerInfo.title);
        //Retrieve the location of the new track
        if(isStreaming)
        {
            media_location = theTargetTrack.getString(theTargetTrack.getColumnIndex(isVideo?"vidlink":"mp3link"));
        }
        else
        {
            //Get the date from the info retrieved from SQL. If it is malformed we have a SERIOUS problem and must halt.
            try {
                Callisto.playerInfo.date = Callisto.sdfFile.format(Callisto.sdfRaw.parse(playerInfo.date));
            } catch (ParseException e) {
                Log.e("*changeToTrack:ParseException", "Error parsing a date from the SQLite db:");
                Log.e("*changeToTrack:ParseException", playerInfo.date);
                Log.e("*changeToTrack:ParseException", "(This should never happen).");
                e.printStackTrace();
                Toast.makeText(c, RESOURCES.getString(R.string.queue_error), Toast.LENGTH_SHORT).show();
                Callisto.databaseConnector.deleteQueueItem(id);
                return;
            }

            //Here we actually get the path
            File target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + Callisto.playerInfo.show);
            target = new File(target,Callisto.playerInfo.date + "__" + Callisto.playerInfo.title +
                    EpisodeDesc.getExtension(theTargetTrack.getString(theTargetTrack.getColumnIndex(isVideo ? "vidlink" : "mp3link"))));
            //If it doesn't exist, we must halt.
            if(!target.exists())
            {
                if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
                {
                    new AlertDialog.Builder(c)
                            .setTitle("No SD Card")
                            .setMessage("There is currently no external storage to write to.")
                            .setNegativeButton("Ok",null)
                            .create().show();
                    return;
                }
                Log.e("*:changeToTrack", "File not found: " + target.getPath());
                Toast.makeText(c, RESOURCES.getString(R.string.queue_error), Toast.LENGTH_SHORT).show();;
                Callisto.databaseConnector.deleteQueueItem(id);
                return;
            }
            //Here we FINALLY get the path.
            media_location = target.getPath();
        }

        //Create the notification for this new track
        //TODO: must we create the intents and whatnot every time?
        Intent notificationIntent = new Intent(c, EpisodeDesc.class);
        notificationIntent.putExtra("id", identity);
        PendingIntent contentIntent = PendingIntent.getActivity(c, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Callisto.notification_playing = new Notification(R.drawable.callisto, null, System.currentTimeMillis());
        Callisto.notification_playing.flags = Notification.FLAG_ONGOING_EVENT;
        Callisto.notification_playing.setLatestEventInfo(c,  Callisto.playerInfo.title,  Callisto.playerInfo.show, contentIntent);
        mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_playing);


        //Here is where we FINALLY actually play the track.
        if(isVideo)
        {
            Log.i("*:changeToTrack", "New track is a video, creating intent");
            Intent intent= new Intent(c, VideoActivity.class);
            intent.putExtra("uri", media_location);
            intent.putExtra("seek", Callisto.playerInfo.position);
            Callisto.playerInfo.update(c);
            c.startActivity(intent);
            return;
        }
        try {
            Log.i("*:changeToTrack", "New track is an audio, doing MediaPlayer things");
            if(Callisto.mplayer==null)
                Callisto.mplayer = new MediaPlayer(); //This could be a problem
            Callisto.mplayer.reset();
            Callisto.mplayerPrepared.setContext(c);
            Callisto.mplayer.setDataSource(media_location);
            Log.i("*:changeToTrack", "Setting source: " + media_location);
            Callisto.mplayer.setOnCompletionListener(Callisto.trackCompleted);
            Callisto.mplayer.setOnErrorListener(Callisto.trackCompletedBug);
            mplayerPrepared.startPlaying = sp;
            Callisto.mplayer.setOnPreparedListener(mplayerPrepared);
            Log.i("*:changeToTrack", "Preparing..." + sp);
            //Streaming requires a dialog
            if(isStreaming)
            {
                mplayerPrepared.pd = ProgressDialog.show(c, Callisto.RESOURCES.getString(R.string.loading), Callisto.RESOURCES.getString(R.string.loading_msg), true, false);
                mplayerPrepared.pd.setCancelable(true);
                mplayerPrepared.pd.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Callisto.mplayer.stop();
                        dialog.cancel();
                    }
                });
            }
            Callisto.mplayer.prepareAsync();
            //Picks up at the mplayerPrepared listener
        } catch (IllegalArgumentException e) {
            Log.e("*changeToTrack:IllegalArgumentException", "Error attempting to set the data path for MediaPlayer:");
            Log.e("*changeToTrack:IllegalArgumentException", media_location);
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e("*changeToTrack:IllegalStateException", "Error in State for MediaPlayer:");
        } catch (IOException e) {
            Log.e("*changeToTrack:IOException", "IO is another of Jupiter's moons. Did you know that?");
            e.printStackTrace();
        }

        //Update the widgets
        CallistoWidget.updateAllWidgets(c);
    }


    /** Plays or pauses the currently playing track; does not adjust what is the current track **/
    //TODO: More comments
    public static void playPause(Context c, View v)
    {
        //-----Live-----
        String live_url = "";
        try {
            live_url = PreferenceManager.getDefaultSharedPreferences(c).getString("live_url", "http://jbradio.out.airtime.pro:8000/jbradio_b");
        } catch(NullPointerException e) {}
        if(Callisto.live_player!=null)
        {
            if(Callisto.live_isPlaying)
            {
                Callisto.live_player.stop();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(Callisto.playDrawable);
            }
            else
            {
                //1. liveInit
                //2. setOnPreparedListener
                //3. setDataSource
                //4. livePrepare
                try {
                    LIVE_Init();
                    Callisto.live_player.setOnPreparedListener(LIVE_PreparedListener);
                    LIVE_PreparedListener.setContext(c);
                    Callisto.live_player.setDataSource(live_url);
                    LIVE_Prepare(null);
                    if(v!=null)
                        ((ImageButton)v).setImageDrawable(Callisto.pauseDrawable);
                } catch(Exception e){}
            }
            Callisto.live_isPlaying = !Callisto.live_isPlaying;
            CallistoWidget.updateAllWidgets(c);
            return;
        }

        //-----Local-----
        if(databaseConnector==null || databaseConnector.queueCount()==0)
            return;
        if(Callisto.mplayer==null)
        {
            Log.d("*:playPause","PlayPause initiated");
            Callisto.mplayer = new MediaPlayer();
            Callisto.mplayer.setOnCompletionListener(Callisto.trackCompleted);
            Callisto.mplayer.setOnErrorListener(Callisto.trackCompletedBug);
            Callisto.changeToTrack(c, 0, true);
        }
        else
        {
            Log.d("*:playPause","PlayPause is " + (Callisto.playerInfo.isPaused ? "" : "NOT") + "paused");
            if(Callisto.playerInfo.isPaused)
            {
                Callisto.mplayer.start();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(Callisto.pauseDrawable);
            }
            else
            {
                Callisto.mplayer.pause();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(Callisto.playDrawable);
            }
            Callisto.playerInfo.isPaused = !Callisto.playerInfo.isPaused;
        }
        CallistoWidget.updateAllWidgets(c);
    }


    /** Listener for play/pause button; calls playPause(), the function */
    public static OnClickListener playPauseListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Callisto.playPause(v.getContext(), v);
            CallistoService.audioJackReceiver.wasPausedByThisReceiver = false;
        }
    };

    /** Stops the player. Clears the notifications, releases resources, etc. */
    public static void stop(Context c)
    {
        if(Callisto.playerInfo!=null)
            Callisto.playerInfo.clear();
        if(Callisto.mplayer!=null) {
            Callisto.mplayer.reset();
            Callisto.mplayer = null;
        }
        if(Callisto.live_player!=null) {
            Callisto.live_player.reset();
            Callisto.live_player = null;
        }
        playerInfo.title = null;
        playerInfo.update(c);
        mNotificationManager.cancel(NOTIFICATION_ID);
        CallistoWidget.updateAllWidgets(c);
    }

    //*********************************** On Click Listeners ***********************************//

    /** Listener for the playlist button; displays the queue. */
    public static OnClickListener playlist = new OnClickListener()
    {
        View psychV;
        @Override
        public void onClick(View v)
        {
            if(live_player!=null)
            {
                psychV = v;
                Dialog dg = new AlertDialog.Builder(v.getContext())
                        .setTitle("Switch from live back to playlist?")
                        .setPositiveButton("Yup", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                live_player.reset();
                                live_player = null;
                                if(Live_wifiLock!=null && !Live_wifiLock.isHeld())
                                    Live_wifiLock.release();
                                playerInfo.update(psychV.getContext());
                                mNotificationManager.cancel(NOTIFICATION_ID);
                            }
                        })
                        .setNegativeButton("Nope", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
                dg.show();
            }
            else {
                Intent newIntent = new Intent(v.getContext(), Queue.class);
                v.getContext().startActivity(newIntent);
            }
        }
    };

    /** Listener to start the different activities for the main buttons on the home screen.*/
    OnClickListener startAct = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Intent newIntent;
            switch(v.getId())
            {
                case R.id.plan:
                    newIntent = new Intent(Callisto.this, CalendarActivity.class);
                    break;
                case R.id.chat:
                    newIntent = new Intent(Callisto.this, IRCChat.class);
                    break;
                case R.id.contact:
                    newIntent = new Intent(Callisto.this, ContactForm.class);
                    break;
                case R.id.donate:
                    newIntent = new Intent(Callisto.this, Donate.class);
                    break;
                case R.id.live:
                    //Not used
                default:
                    newIntent = new Intent(Callisto.this, AllShows.class);
                    break;
            }
            startActivity(newIntent);
        }
    };

    /** Listener for the seek button; displays a dialog that allows the user to seek to a point in the episode. */
    public static OnClickListener seekDialog = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if(Callisto.mplayer==null)
                return;

            SeekBar sb = new SeekBar(v.getContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext()).setView(sb);
            final AlertDialog alertDialog = builder.create();

            alertDialog.setTitle(RESOURCES.getString(R.string.seek_title));
            alertDialog.setMessage(formatTimeFromSeconds(Callisto.mplayer.getCurrentPosition()/1000) + "/" + formatTimeFromSeconds(Callisto.playerInfo.length));
            sb.setMax(Callisto.playerInfo.length);
            sb.setProgress(Callisto.mplayer.getCurrentPosition()/1000);

            alertDialog.setButton(Callisto.RESOURCES.getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                }
            });//*/
            alertDialog.show();
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                    alertDialog.setMessage(formatTimeFromSeconds(progress) + "/" + formatTimeFromSeconds(Callisto.playerInfo.length));
                }
                @Override
                public void onStartTrackingTouch(SeekBar arg0) {}
                @Override

                public void onStopTrackingTouch(SeekBar arg0) {
                    Callisto.mplayer.seekTo(arg0.getProgress()*1000);
                    Callisto.clock.i=Callisto.SAVE_POSITION_EVERY;
                }
            });
        }
    };

    OnClickListener launchVideo = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            liveDg.dismiss();
            String live_video = PreferenceManager.getDefaultSharedPreferences(v.getContext()).getString("video_url", "rtsp://videocdn-us.geocdn.scaleengine.net/jblive/live/jblive.stream");
            Intent intent= new Intent(v.getContext(), VideoActivity.class);
            intent.putExtra("uri", "");
            Callisto.playerInfo.update(v.getContext());
            v.getContext().startActivity(intent);
            return;
        }
    };

    OnClickListener launchAudio = new OnClickListener()
    {
        @Override
        public void onClick(View v) {
            liveDg.dismiss();
            if(Callisto.live_player == null || !Callisto.live_isPlaying)
            {
                Log.d("LiveStream:playButton", "Live player does not exist, creating it.");
                if(Callisto.mplayer!=null)
                    Callisto.mplayer.reset();
                Callisto.mplayer=null;
                LIVE_Init();
                Log.d("LiveStream:playButton", "Would you like a falafel with that?");
                Callisto.live_player.setOnPreparedListener(LIVE_PreparedListener);
                Log.d("LiveStream:playButton", "Would you like a falafel with that?");
                LIVE_PreparedListener.setContext(v.getContext());
                Log.d("LiveStream:playButton", "Would you like a falafel with that?");
                String live_url = PreferenceManager.getDefaultSharedPreferences(v.getContext()).getString("live_url", "http://jbradio.out.airtime.pro:8000/jbradio_b");
                Log.d("LiveStream:playButton", "Alright so getting url");
                try {
                    Callisto.live_player.setDataSource(live_url);
                    if(!Live_wifiLock.isHeld())
                        Live_wifiLock.acquire();
                    LIVE_Prepare(Callisto.this);
                } catch (Exception e) {
                    //errorDialog.show();
                    e.printStackTrace();
                    LIVE_SendErrorReport("EXCEPTION");
                }
            }
            else
            {
                Log.d("LiveStream:playButton", "Live player does exist.");
                if(Callisto.live_isPlaying)
                {
                    Log.d("LiveStream:playButton", "Pausing.");
                    Callisto.live_player.pause();
                }
                else
                {
                    if(!Live_wifiLock.isHeld())
                        Live_wifiLock.acquire();
                    Log.d("LiveStream:playButton", "Playing.");
                    Callisto.live_player.start();
                }
                Callisto.live_isPlaying = !Callisto.live_isPlaying;
            }
            Log.d("LiveStream:playButton", "Done");
        }
    };


    //*********************************** Update a show ***********************************//


    /** Downloads and resizes a show's logo image.
     * @throws IOException
     * @throws NullPointerException
     */
    public static class downloadImage extends AsyncTask<String, Void, Void>
    {
        /** Do the thing stuff
         * @param s The values, split up into img_url and show. img_url is the image to download, show is the name of the show (to calculate the path)
         */
        @Override
        protected Void doInBackground(String... s)
        {
            //public static void downloadImage(String img_url, String show) throws IOException, NullPointerException
            //{
            try {
                String img_url = s[0], show = s[1];
                System.out.println(img_url);
                if(img_url==null)
                    throw(new NullPointerException());
                File f = new File(Environment.getExternalStorageDirectory() + File.separator +
                        storage_path + File.separator +
                        show + EpisodeDesc.getExtension(img_url));
                System.out.println(f.getAbsolutePath());
                if(f.exists())
                    return null;
                (new File(Environment.getExternalStorageDirectory() + File.separator +
                        storage_path)).mkdirs();
                URL url = new URL (img_url);
                InputStream input = url.openStream();
                try {
                    OutputStream output = new FileOutputStream (f.getPath());
                    try {
                        byte[] buffer = new byte[5 * 1024];
                        int bytesRead = 0;
                        while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                            output.write(buffer, 0, bytesRead);
                        }
                    } finally {
                        output.close();
                    }
                } finally {
                    input.close();
                }
                //Resize the image
                Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
                Bitmap scale  = Bitmap.createScaledBitmap(bitmap, (int)(60*DP), (int)(60*DP), true);
                OutputStream fOut = new FileOutputStream(f);
                scale.compress(Bitmap.CompressFormat.JPEG, 85, fOut);//*/
            } catch(Exception e) {
                Log.v("*:updateShow", "Failed to download image");
            }
            return null;
        }
    }



    /** Updates a show by checking to see if there are any new episodes available.
     *
     * @param currentShow The number of the current show in relation to the AllShows.SHOW_LIST array
     * @param showSettings The associated SharedPreferences with that show
     * @return A Message object with arg1 being 1 if the show found new episodes, 0 otherwise.
     */
    public static Message updateShow(int currentShow, SharedPreferences showSettings)
    {
        Log.i("*:updateShow", "Beginning update");
        String epDate = null, epTitle = null, epDesc = null, epLink = null;
        String lastChecked = showSettings.getString("last_checked", null);

        String newLastChecked = null;
        try
        {
            //Prepare the parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            XmlPullParserFactory factory2 = XmlPullParserFactory.newInstance();
            factory2.setNamespaceAware(true);
            XmlPullParser xpp_vid = factory2.newPullParser();
            //URL url = new URL(isVideo ? AllShows.SHOW_LIST_VIDEO[currentShow] : AllShows.SHOW_LIST_AUDIO[currentShow]);
            URL url = new URL(AllShows.SHOW_LIST_AUDIO[currentShow]);
            URL url2 = new URL(AllShows.SHOW_LIST_VIDEO[currentShow]);
            InputStream input = url.openConnection().getInputStream();
            InputStream input2 = url2.openConnection().getInputStream();
            xpp.setInput(input, null);
            xpp_vid.setInput(input2, null);

            Log.v("*:updateShow", "Parser is prepared");
            int eventType = xpp.getEventType();
            int eventType2 = xpp_vid.getEventType();

            while(!("title".equals(xpp.getName()) && eventType == XmlPullParser.END_TAG))
            {
                eventType = xpp.next();
                eventType2 = xpp_vid.next();
            }
            eventType = xpp.next();
            eventType2 = xpp_vid.next();

            String imgurl = null, imgurl2 = null;
            //Download the image
            while(!("title".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG) && !("thumbnail".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
            {
                //Handles if there is an <image> tag
                if("image".equals(xpp.getName()))
                {
                    if(xpp.getAttributeCount()>0)
                    {
                        imgurl = xpp.getAttributeValue(null, "href");
                        eventType = xpp.next();
                        eventType = xpp.next();
                        eventType2 = xpp_vid.next();
                        eventType2 = xpp_vid.next();
                    }
                    else
                    {
                        eventType = xpp.next();
                        eventType2 = xpp_vid.next();
                        while(!(("image".equals(xpp.getName()) || ("url".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))))
                        {
                            eventType = xpp.next();
                            eventType2 = xpp_vid.next();
                        }
                        if(!("image".equals(xpp.getName())))
                        {
                            eventType = xpp.next();
                            eventType2 = xpp_vid.next();
                            imgurl = xpp.getText();
                            while(!("image".equals(xpp.getName())))
                            {
                                eventType = xpp.next();
                                eventType2 = xpp_vid.next();
                            }
                        }
                    }
                }

                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                if(eventType==XmlPullParser.END_DOCUMENT)
                    throw(new UnfinishedParseException("Thumbnail"));
            }
            //Handles if no <image> tag was found, falls back to <media:thumbnail>
            if(("thumbnail".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
                imgurl2 = xpp.getAttributeValue(null, xpp.getAttributeName(0));

            if(imgurl2!=null)
                imgurl = imgurl2;
            if(imgurl!=null)
            {
                if(imgurl.startsWith("http://linuxactionshow.com") || imgurl.startsWith("www.linuxactionshow.com") || imgurl.startsWith("http://www.linuxactionshow.com"))
                    imgurl = "http://www.jupiterbroadcasting.com/images/LASBadge-Audio144.jpg";
                new downloadImage().execute(imgurl, AllShows.SHOW_LIST[currentShow]);
                //downloadImage(imgurl, AllShows.SHOW_LIST[currentShow]);
                Log.v("*:updateShow", "Parser is downloading image for " + AllShows.SHOW_LIST[currentShow] + ":" + imgurl);
            }

            //Find the first <item> tag
            while(!("item".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
            {
                eventType = xpp.next();
                if(eventType==XmlPullParser.END_DOCUMENT)
                    throw(new UnfinishedParseException("Item"));
            }

            //Get episodes
            while(eventType!=XmlPullParser.END_DOCUMENT)
            {
                //Title
                while(!("title".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
                {
                    eventType = xpp.next();
                    if(eventType==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Title"));
                }
                while(!("title".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG))
                {
                    eventType2 = xpp_vid.next();
                    if(eventType2==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Title"));
                }

                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                epTitle = xpp.getText();
                if(epTitle==null)
                    throw(new UnfinishedParseException("Title"));
                if(epTitle.indexOf("|")>0)
                    epTitle = epTitle.substring(0, epTitle.indexOf("|")).trim();
                Log.d("*:updateShow", "Title: " + epTitle);

                //Link
                while(!("link".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
                {
                    eventType = xpp.next();
                    if(eventType==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Link"));
                }
                while(!("link".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG))
                {
                    eventType2 = xpp_vid.next();
                    if(eventType==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Link"));
                }

                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                epLink = xpp.getText();
                if(epLink==null)
                    throw(new UnfinishedParseException("Link"));
                Log.d("*:updateShow", "Link: " + epLink);

                //Description
                while(!("description".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
                {
                    eventType = xpp.next();
                    if(eventType==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Description"));
                }
                while(!("description".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG))
                {
                    eventType2 = xpp_vid.next();
                    if(eventType2==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Description"));
                }

                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                epDesc = xpp.getText();
                if(epDesc==null)
                    throw(new UnfinishedParseException("Description"));
                Log.d("*:updateShow", "Desc: " + epDesc);

                //Date
                while(!("pubDate".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
                {
                    eventType = xpp.next();
                    if(eventType==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Date"));
                }
                while(!("pubDate".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG))
                {
                    eventType2 = xpp_vid.next();
                    if(eventType2==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Date"));
                }

                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                epDate = xpp.getText();
                Log.d("*:updateShow", "Date: " + epDate);
                Log.e("*:updateShow", "Date: " + xpp_vid.getText());


                if(epDate==null)
                    throw(new UnfinishedParseException("Date"));
                if(lastChecked!=null && !Callisto.sdfSource.parse(epDate).after(Callisto.sdfSource.parse(lastChecked)))
                    break;
                if(newLastChecked==null)
                    newLastChecked = epDate;

                //Media link and size
                while(!("enclosure".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
                {
                    eventType = xpp.next();
                    if(eventType==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Media"));
                }

                while(!("enclosure".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG))
                {
                    eventType2 = xpp_vid.next();
                    if(eventType2==XmlPullParser.END_DOCUMENT)
                        throw(new UnfinishedParseException("Media"));
                }


                String epAudioLink = xpp.getAttributeValue(xpp.getNamespace(),"url"),
                        epVideoLink = xpp_vid.getAttributeValue(xpp.getNamespace(),"url");
                if(epAudioLink==null)
                    throw(new UnfinishedParseException("AudioLink"));

                String temp = xpp.getAttributeValue(xpp.getNamespace(),"length");
                if(temp==null)
                    throw(new UnfinishedParseException("MediaSize"));
                String temp2 = xpp_vid.getAttributeValue(xpp_vid.getNamespace(),"length");
                long epAudioSize = Long.parseLong(temp);
                Log.e("*:updateShow", "ASDSDSADS: " + temp2);
                Log.d("*:updateShow", "A Link: " + epAudioLink);
                Log.d("*:updateShow", "A Size: " + epAudioSize);
                long epVideoSize = Long.parseLong(temp2);

                Log.d("*:updateShow", "V Link: " + epVideoLink);
                Log.d("*:updateShow", "V Size: " + epVideoSize);

                epDate = Callisto.sdfRaw.format(Callisto.sdfSource.parse(epDate));
                //if(!Callisto.databaseConnector.updateMedia(AllShows.SHOW_LIST[currentShow], epTitle,
                //isVideo, epMediaLink, epMediaSize))
                Callisto.databaseConnector.insertEpisode(AllShows.SHOW_LIST[currentShow], epTitle, epDate, epDesc, epLink, epAudioLink, epAudioSize, epVideoLink, epVideoSize);
                Log.v("*:updateShow", "Inserting episode: " + epTitle);
            }

        } catch (XmlPullParserException e) {
            Log.e("*:update:XmlPullParserException", "Parser error");
            //TODO EXCEPTION: XmlPullParserException
            e.printStackTrace();
        } catch (MalformedURLException e) {
            Log.e("*:update:MalformedURLException", "Malformed URL? That should never happen.");
            e.printStackTrace();
        } catch (UnknownHostException e)
        {
            Log.e("*:update:UnknownHostException", "Unable to initiate a connection");
            return null;
        }  catch (IOException e) {
            //FIXME: EXCEPTION: IOException
            Log.e("*:update:IOException", "IO is a moon");
            e.printStackTrace();
        } catch (ParseException e) {
            //FIXME: EXCEPTION: ParseException
            Log.e("*:update:ParseException", "Date Parser error: |" + epDate + "|");
        } catch (UnfinishedParseException e) {
            Log.w("*:update:UnfinishedParseException",e.toString());
        }


        Message m = new Message();
        if(newLastChecked==null)
        {
            Log.v("*:updateShow", "Not updating lastChecked: " + newLastChecked);
            m.arg1=0;
        }
        else
        {
            Log.v("*:updateShow", "Updating lastChecked for:" + AllShows.SHOW_LIST[currentShow] + "| " + newLastChecked);
            SharedPreferences.Editor editor = showSettings.edit();
            editor.putString("last_checked", newLastChecked);
            editor.commit();
            m.arg1=1;
        }
        Log.i("*:updateShow", "Finished update");
        return m;
    }


    //*********************************** Live Stuff ***********************************//
    //TODO: rename this shit?

    /** Initiates the live player. Can be called across activities. */
    static public void LIVE_Init()
    {
        Log.d("LiveStream:liveInit", "Initiating the live player.");
        Callisto.live_player = new MediaPlayer();
        Log.d("LiveStream:liveInit", "Initiating the live player.");
        Callisto.live_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        Log.d("LiveStream:liveInit", "Initiating the live player.");
        Callisto.live_player.setOnErrorListener(new OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if(LIVE_PreparedListener.pd!=null)
                    LIVE_PreparedListener.pd.hide();
                String whatWhat="";
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                        whatWhat = "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK";
                        break;
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        whatWhat = "MEDIA_ERROR_SERVER_DIED";
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                        whatWhat = "MEDIA_ERROR_UNKNOWN";
                        break;
                    default:
                        whatWhat = "???";
                        return true;
                }
                try{
                    if(errorDialog !=null)
                        errorDialog.show();
                }catch(Exception e){}

                System.out.println(whatWhat);
                LIVE_SendErrorReport(whatWhat);
                return true;
            }
        });
        Log.d("LiveStream:liveInit", "Initiating the live player.");
    }


    /** Method to prepare the live player; shows a dialog and then sets it up to be transfered to livePreparedListenerOther. */
    static public void LIVE_Prepare(Context c)
    {
        Log.d("LiveStream:LIVE_Prepare", "Preparing the live player.");
        if(c!=null)
        {
            LIVE_PreparedListener.pd = BaconDialog(c, "Buffering...", null);

            /*
            final AnimationDrawable d = (AnimationDrawable) ((ProgressBar) LIVE_PreparedListener.baconPDialog.getWindow().findViewById(android.R.id.progress)).getIndeterminateDrawable();
            ((View)LIVE_PreparedListener.baconPDialog.getWindow().findViewById(android.R.id.progress)).post(new Runnable() {
                @Override
                public void run() {
                    d.start();
                }
            });
            //*/
            LIVE_PreparedListener.pd.setOnDismissListener(new OnDismissListener()
            {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    LIVE_PreparedListener.pd.cancel();
                    LIVE_PreparedListener.pd = null;
                }

            });
            LIVE_PreparedListener.pd.setCancelable(true);
        }
        Callisto.live_player.prepareAsync();
    }


    /** Listener for the live player in only the LiveStream activity. Starts it playing or displays an error message. */
    static OnPreparedListenerWithContext LIVE_PreparedListener = new OnPreparedListenerWithContext()
    {
        @Override
        public void onPrepared(MediaPlayer arg0) {

            Log.e("LLLLLLLL:", "PREPARED!");
            if(pd!=null)
            {
                if(!pd.isShowing())
                    return;
                pd.hide();
            }
            //*/
            try {
                Callisto.live_player.start();
                LIVE_update = new LIVE_FetchInfo();
                Callisto.LIVE_update.execute((Void [])null);
                Callisto.live_isPlaying = true;
            }
            catch(Exception e)
            {
                errorDialog.show();
                e.printStackTrace();
            }
        }
    };

    /** Listener for the Live button. */
    private OnClickListener LIVE_PlayButton = new OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            Log.d("LiveStream:playButton", "Clicked play button");
            if(Callisto.mplayer!=null)
            {
                Dialog dg = new AlertDialog.Builder(v.getContext())
                        .setTitle("Switch from playlist to live?")
                        .setPositiveButton("Yup", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                mNotificationManager.cancel(NOTIFICATION_ID);
                                mplayer.reset();
                                mplayer = null;
                                v.performClick();
                            }
                        })
                        .setNegativeButton("Nope", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
                dg.show();
                return;
            }
            liveDg.show();
            liveDg.getWindow().findViewById(R.id.audio).setOnClickListener(launchAudio);
            liveDg.getWindow().findViewById(R.id.video).setOnClickListener(launchVideo);
        }
    };

    /** Sends an error report to the folks at Qweex. COMPLETELY anonymous. The only information that is sent is the version of Callisto and the version of Android. */
    public static void LIVE_SendErrorReport(String msg)
    {
        String errorReport = errorReportURL + "?id=Callisto&v=" + Callisto.appVersion + "&err=" + android.os.Build.VERSION.RELEASE + "_" + msg;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        HttpGet httpGet = new HttpGet(errorReport);
        try {
            httpClient.execute(httpGet, localContext);
        }catch(Exception e){}
    }


    //*********************************** Misc stuffs ***********************************//

    //TODO: Remove this shit
    public class downloadExtras extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... c)
        {
            final String FILELOC = "http://www.qweex.com/qweexware/callisto/extras/";
            String[] files = {"baconlove.gif", "bryan.mp3", "gangnam.mid", "gangnam.gif"};
            File f = new File(Environment.getExternalStorageDirectory() + File.separator +
                    storage_path + File.separator +
                    "extras");
            f.mkdir();
            for(int i=0; i<files.length; i++)
            {
                f = new File(Environment.getExternalStorageDirectory() + File.separator +
                        storage_path + File.separator +
                        "extras" + File.separator + files[i]);
                if(f.exists())
                    continue;
                try {
                    URL url = new URL (FILELOC + files[i]);
                    InputStream input = url.openStream();
                    try {
                        OutputStream output = new FileOutputStream (f.getPath());
                        try {
                            byte[] buffer = new byte[5 * 1024];
                            int bytesRead = 0;
                            while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                                output.write(buffer, 0, bytesRead);
                            }
                        } finally {
                            output.close();
                        }
                    } finally {
                        input.close();
                    }
                } catch(Exception e) {e.printStackTrace();}
            }
            return null;
        }
    };

    /** Builds & shows the update news for the current version */
    public void showUpdateNews()
    {
        //Build and Footer
        TextView newsHeader = new TextView(this);
        newsHeader.setPadding(5, 5, 5, 5);
        TextView newsFooter = new TextView(this);
        newsFooter.setPadding(5, 5, 5, 5);

        //Create the listview & data for it
        final android.widget.ExpandableListView elv = new android.widget.ExpandableListView(this);
        java.util.List<java.util.Map<String, String>> groupData = new ArrayList<java.util.Map<String, String>>();
        java.util.List<java.util.List<java.util.Map<String, String>>> childData = new ArrayList<java.util.List<java.util.Map<String, String>>>();

        //Read the file; halt if there's an error
        java.io.BufferedReader bufReader;
        try {
            bufReader = new java.io.BufferedReader(new java.io.InputStreamReader(getAssets().open("UpdateNotes")));
        } catch(Exception e) { return; }

        //Read the file line by line & process it
        String line=null;
        int place = 111;    //The placing that the current item is; numbers are a stupid way to do it. Stop judging me.
        try {
            while( (line=bufReader.readLine()) != null )
            {
                switch(place)
                {
                    case 111:   //The starting state; Does not require a new line for the first line of the header
                    case 1:     //The header
                        if("".equals(line))
                        {
                            place--;
                            continue;
                        }
                        newsHeader.setText(newsHeader.getText() + (place==111?"":"\n") + line);
                        if(place==111) place=1;
                        break;
                    case -1:    //The footer
                        newsFooter.setText(newsFooter.getText() + "\n" + line);
                        break;
                    case 0:     //An item with expandable subpoints
                        if("".equals(line))
                        {
                            place--;
                            continue;
                        }
                        java.util.Map<String, String> curGroupMap = new java.util.HashMap<String, String>();
                        java.util.List<java.util.Map<String, String>> children = new ArrayList<java.util.Map<String, String>>();
                        groupData.add(curGroupMap);

                        int x = line.indexOf("--",1);
                        int y = line.indexOf("--", x+2);;
                        curGroupMap.put("TITLE", line.substring(0, x).trim());
                        while(true)
                        {
                            if(y==-1)
                                y = line.length();
                            java.util.Map<String, String> curChildMap = new java.util.HashMap<String, String>();
                            children.add(curChildMap);
                            curChildMap.put("DESCRIPTION", line.substring(x+2, y).trim());
                            if(y==line.length())
                                break;
                            x = line.indexOf("--", x+2);
                            y = line.indexOf("--", x+2);
                        }
                        childData.add(children);
                        break;
                }
            }
        } catch(Exception e) {}

        //Yeaaah create the adapter! Yeah!
        android.widget.SimpleExpandableListAdapter mAdapter = new SimpleExpandableListAdapterWithLinkify(
                this,
                groupData,
                R.layout.news_listitem1,
                new String[] { "TITLE" },
                new int[] { android.R.id.text1 },
                childData,
                R.layout.news_listitem2,
                new String[] { "DESCRIPTION" },
                new int[] { android.R.id.text1 }
        );
        elv.addHeaderView(newsHeader);
        elv.addFooterView(newsFooter);
        elv.setAdapter(mAdapter);
        elv.setGroupIndicator(null);
        news = new Dialog(this);
        //Set the title with the version number if possible
        try {
            news.setTitle("Version " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch(Exception e) {
            news.setTitle("Update notes");
        }
        //Show it!
        news.addContentView(elv, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        news.show();
    }

    class SimpleExpandableListAdapterWithLinkify extends SimpleExpandableListAdapter
    {

        public SimpleExpandableListAdapterWithLinkify(Context context, List<? extends Map<String, ?>> groupData, int groupLayout, String[] groupFrom, int[] groupTo, List<? extends List<? extends Map<String, ?>>> childData, int childLayout, String[] childFrom, int[] childTo) {
            super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);
        }
        public View getChildView (int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
        {
            convertView = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
            Linkify.addLinks((TextView)convertView, Linkify.ALL);
            return convertView;
        }
    }

    static public ProgressDialog BaconDialog(Context c, String title, String message)
    {
        if(message==null)
            message=Callisto.RESOURCES.getString(R.string.loading_msg);
        ProgressDialog pDialog = new ProgressDialog(c);
        pDialog.setCancelable(false);
        pDialog.setTitle(title);
        pDialog.setMessage(message);
        pDialog.setIcon(R.drawable.ic_action_gear);
        pDialog.show();

        //((View)LIVE_PreparedListener.baconPDialog.getWindow().findViewById(android.R.id.progress).getParent().getParent().getParent().getParent().getParent()).setBackgroundDrawable(RESOURCES.getDrawable(R.drawable.bacon_anim));
        ((View)(pDialog.getWindow().findViewById(android.R.id.progress)).getParent()).setBackgroundColor(0xFFFFFFFF);
        ((TextView)pDialog.getWindow().findViewById(android.R.id.message)).setTextColor(0xff000000);
        ((TextView)pDialog.getWindow().findViewById(android.R.id.message)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, (float) 17.0);
        ((View) pDialog.getWindow().findViewById(android.R.id.message)).setPadding(15, 5, 5, 5);
        ((ProgressBar) pDialog.getWindow().findViewById(android.R.id.progress)).setLayoutParams(new LayoutParams(96, 96));
        ((ProgressBar) pDialog.getWindow().findViewById(android.R.id.progress)).setIndeterminateDrawable(RESOURCES.getDrawable(R.drawable.bacon_anim));
        return pDialog;
    }

    /** Converts a time in seconds to a human readable format.
     *  @param seconds The raw number of seconds to format.
     *  @return A string formatted in either HH:mm:ss or mm:ss, depending on how many hours are calculated.
     */
    public static String formatTimeFromSeconds(int seconds)
    {
        int minutes = seconds / 60;
        seconds %= 60;
        if(minutes>=60)
        {
            int hours = minutes / 60;
            minutes %= 60;
            return ((Integer.toString(hours) + ":" +
                    (minutes<10?"0":"") + Integer.toString(minutes) + ":" +
                    (seconds<10?"0":"") + Integer.toString(seconds)));
        }
        else
            return ((Integer.toString(minutes) + ":" +
                    (seconds<10?"0":"") + Integer.toString(seconds)));
    }
}
