/*
Copyright (C) 2012 Qweex
This file is a part of Callisto.

Callisto is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

Callisto is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Callisto; If not, see <http://www.gnu.org/licenses/>.
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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.qweex.callisto.irc.IRCChat;
import com.qweex.callisto.podcast.AllShows;
import com.qweex.callisto.podcast.EpisodeDesc;
import com.qweex.callisto.podcast.Queue;
import com.qweex.utils.UnfinishedParseException;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;



//Task Tags: todo clean feature fixme wtf idea

//CLEAN: Rename IDs in layout XML
//CLEAN: Strings.xml

/** This is the main activity/class for the app.
    It contains the main screen, and also some static elements
    that are globally used across multiple activities
    @author MrQweex
 */
public class Callisto extends Activity {

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
		/** An ArrayList containing a list of IDs of what to download. Note that this is NOT saved when the app is shut down. */
	public static ArrayList<Long> download_queue = new ArrayList<Long>();
		/** One of the various date formats used across various Activities. The usage for most should be self-documenting from the name. */
	public static final SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss"),
										 sdfRawSimple1 = new SimpleDateFormat("yyyyMMdd"),
								 		 sdfRawSimple2 = new SimpleDateFormat("HHmmss"),
								 		 sdfTime = new SimpleDateFormat("hh:mm aa"),
								 		 sdfSource = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z"),
								 		 sdfDestination = new SimpleDateFormat("MM/dd/yyyy"),
								 		 sdfFile = new SimpleDateFormat("yyyy-MM-dd"),
								 		 sdfHuman = new SimpleDateFormat("MMM d"),
								 		 sdfHumanLong = new SimpleDateFormat("MMM d, yyyy");
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
	
	//------Local variables-----
	static TextView timeView;
	static int current;
	static ProgressBar timeProgress;
	private static final int QUIT_ID=Menu.FIRST+1;
	private static final int SETTINGS_ID=QUIT_ID+1;
	private static final int STOP_ID=SETTINGS_ID+1;
	private static final int SAVE_POSITION_EVERY = 40;	//Cycles, not necessarily seconds
	private Timer timeTimer = null;
	
	public static oOnCompletionListener nextTrack;
	public static oOnErrorListener nextTrackBug;
	public static oOnPreparedListener okNowPlay;
	public static final int NOTIFICATION_ID = 1337;
	private static NotificationManager mNotificationManager;
	public static SharedPreferences alarmPrefs;
	public final static String PREF_FILE = "alarms";
	
		/** The status of the live player; true if it is playing, false if it is paused or not in use. */
	public static boolean live_isPlaying = false;
		/** The Version of Callisto. Set to -1 if it cannot be determined. */
	public static int appVersion = -1;
	protected static boolean is_widget;
	
	/** Called when the activity is first created. Sets up the view for the main screen and additionally initiates many of the static variables for the app.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		RESOURCES = getResources();
		DP = RESOURCES.getDisplayMetrics().density;
		mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Callisto.storage_path = PreferenceManager.getDefaultSharedPreferences(this).getString("storage_path", "callisto");
		try {
			Callisto.appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode; } catch (NameNotFoundException e) {}
		
		//This is the most reliable way I've found to determine if it is landscape
		boolean isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
		alarmPrefs = getApplicationContext().getSharedPreferences(PREF_FILE, MODE_PRIVATE);
		
		setContentView(R.layout.main);
		findViewById(R.id.playPause).setOnClickListener(Callisto.playPauseListener);
		findViewById(R.id.playlist).setOnClickListener(Callisto.playlist);
		findViewById(R.id.seek).setOnClickListener(Callisto.seekDialog);
		findViewById(R.id.next).setOnClickListener(Callisto.next);
		findViewById(R.id.previous).setOnClickListener(Callisto.previous);
		
		
		//This loop sets the onClickListeners and adjusts the button settings if the view is landscape
		int [] buttons = {R.id.listen, R.id.live, R.id.plan, R.id.chat, R.id.contact, R.id.donate};
		int [] graphics = {R.drawable.ic_menu_play_clip, R.drawable.ic_menu_view, R.drawable.ic_menu_today, R.drawable.ic_menu_allfriends, R.drawable.ic_menu_send, R.drawable.ic_menu_emoticons};
		
		Button temp;
		for(int i=0; i<buttons.length; i++)
		{
			temp = (Button)findViewById(buttons[i]);
			temp.setOnClickListener(startAct);
			
			
			if(isLandscape)
			{
				ViewGroup.LayoutParams tr = temp.getLayoutParams();
				((MarginLayoutParams) tr).setMargins(0, 0, 0, 0);
				temp.setPadding((int) (10*DP), 0, 0, 0);
				temp.setLayoutParams(tr);
				temp.setCompoundDrawablesWithIntrinsicBounds(graphics[i], 0, 0, 0);
			}
			//*/
		}
		
		//Initialization of (some of the) static variables
		Callisto.playDrawable = RESOURCES.getDrawable(R.drawable.ic_media_play);
		Callisto.pauseDrawable = RESOURCES.getDrawable(R.drawable.ic_media_pause);
		Callisto.databaseConnector = new DatabaseConnector(Callisto.this);
		Callisto.databaseConnector.open();
		if(Callisto.playerInfo==null)
			Callisto.playerInfo = new PlayerInfo(Callisto.this);
		else
			Callisto.playerInfo.update(Callisto.this);
		
		
		nextTrack = new oOnCompletionListener();
		nextTrackBug = new oOnErrorListener();
	    okNowPlay = new oOnPreparedListener();
	    
	    	//Create the views for the the IRC
	    chatView = new TextView(this);
	    chatView.setGravity(Gravity.BOTTOM);
	    String i=PreferenceManager.getDefaultSharedPreferences(this).getString("irc_max_scrollback", "500");
	    chatView.setMaxLines(Integer.parseInt(i));
	    logView = new TextView(this);
	    logView.setGravity(Gravity.BOTTOM);
	    logView.setMaxLines(Integer.parseInt(i));
	    /*
	    ScrollView.LayoutParams ll = new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT);
	    ll.setMargins(0, 0, 0, (int)(50*Callisto.DP));
	    chatView.setPadding(0, 0, 0, (int)(50*Callisto.DP));
	    chatView.setLayoutParams(ll);
	    //*/
	}
	
	/** Called when the activity is going to be destroyed. Currently unused. */
	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}
	
	/** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
	@Override
	public void onResume()
	{
		super.onResume();
		Log.v("Callisto:onResume", "Resuming main activity");
		Callisto.playerInfo.update(Callisto.this);
	}
	
	/** Creates the layout for various activities, adding the Player Controls.
	 *  It essentially takes whatever "mainView" is and wraps it and the Controls in a vertical LinearLayout.
	 *  @param c The current context in which to build.
	 *  @param mainView The main view of the Activity to be wrapped above the player controls
	 */
    public static void build_layout(Context c, View mainView)
    {
    	Log.v("*:build_layout", "Building the layout");
		View controls = ((LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.controls, null, false);
		TextView empty = new TextView(c);
		empty.setId(android.R.id.empty);
		empty.setBackgroundColor(c.getResources().getColor(R.color.backClr));
		empty.setTextColor(c.getResources().getColor(R.color.txtClr));
		
		
		LinearLayout layout = new LinearLayout(c);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams
				(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f);
		LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams
				(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f);
		layout.addView(mainView, mParams);
		layout.addView(empty, mParams);
		layout.addView(controls, cParams);
		((Activity)c).setContentView(layout);
		((Activity)c).findViewById(R.id.playPause).setOnClickListener(Callisto.playPauseListener);
		((Activity)c).findViewById(R.id.playlist).setOnClickListener(Callisto.playlist);
		((Activity)c).findViewById(R.id.seek).setOnClickListener(Callisto.seekDialog);
		((Activity)c).findViewById(R.id.next).setOnClickListener(Callisto.next);
		((Activity)c).findViewById(R.id.previous).setOnClickListener(Callisto.previous);
		Log.v("*:build_layout", "Finished building the layout");
    }
    
    /** Listener for the Next (">") button. Proceeds to the next track, if there is one. */
    public static OnClickListener next = new OnClickListener()
    {
    	@Override public void onClick(View v)
    	{
    		playTrack(v.getContext(), 1, !Callisto.playerInfo.isPaused);
		}
	};
	/** Listener for the Previous ("<") button. Goes back to the previosu track, if there is one. */
    public static OnClickListener previous = new OnClickListener()
    {
    	@Override public void onClick(View v)
    	{
    		playTrack(v.getContext(), -1, !Callisto.playerInfo.isPaused);
		}
	};
    
	/** This method plays the next song in the queue, if there is one.
	 * @param c The context of the current activity.
	 * @param previousOrNext >0 if it should play the next track, <0 for the previous, and 0 for the current
	 * @param startPlaying true if the player should start playing when it changes tracks, false otherwise
	 */
    public static void playTrack(Context c, int previousOrNext, boolean startPlaying)
    {    	
		Cursor queue = Callisto.databaseConnector.advanceQueue(previousOrNext);
    	//If there are no items in the queue, stop the player
    	if(queue==null || queue.getCount()==0)
    	{
    		Log.v("*:playTrack", "Queue is empty. Pausing.");
	    	ImageButton x = (ImageButton) ((Activity)c).findViewById(R.id.playPause);
	    	if(x!=null)
	    		x.setImageDrawable(Callisto.pauseDrawable);
	    	mNotificationManager.cancel(NOTIFICATION_ID);
	    	Callisto.stop(c);
    		return;
    	}
    	
    	Log.v("*:playTrack", "Queue Size: " + queue.getCount());
    	queue.moveToFirst();
    		//The queue merely stores the identity (_id) of the entrie's position in the main SQL
    		//After obtaining it, we can get all the information about it
    	Long id = queue.getLong(queue.getColumnIndex("_id"));
    	Long identity = queue.getLong(queue.getColumnIndex("identity"));
    	boolean isStreaming = queue.getInt(queue.getColumnIndex("streaming"))>0;
        Cursor db = Callisto.databaseConnector.getOneEpisode(identity);
	    db.moveToFirst();
	    
	    String media_location;
	    Callisto.playerInfo.title = db.getString(db.getColumnIndex("title"));
	    Callisto.playerInfo.position = db.getInt(db.getColumnIndex("position"));
	    System.out.println("Position=" + Callisto.playerInfo.position);
	    Callisto.playerInfo.date = db.getString(db.getColumnIndex("date"));
	    Callisto.playerInfo.show = db.getString(db.getColumnIndex("show"));
	    Log.i("*:playTrack", "Loading info: " + Callisto.playerInfo.title);
	    if(isStreaming)
	    {
	    	media_location = db.getString(db.getColumnIndex("mp3link"));
	    }
	    else
	    {
		    try {
	        	Callisto.playerInfo.date = Callisto.sdfFile.format(Callisto.sdfRaw.parse(playerInfo.date));
			} catch (ParseException e) {
				Log.e("*playTrack:ParseException", "Error parsing a date from the SQLite db:");
				Log.e("*playTrack:ParseException", playerInfo.date);
				Log.e("*playTrack:ParseException", "(This should never happen).");
				e.printStackTrace();
				Toast.makeText(c, RESOURCES.getString(R.string.queue_error), Toast.LENGTH_SHORT).show();
				Callisto.databaseConnector.deleteQueueItem(id);
				return;
			}
		    
	        File target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + Callisto.playerInfo.show);
	        target = new File(target,Callisto.playerInfo.date + "__" + Callisto.playerInfo.title + ".mp3");
	        if(!target.exists())
	        {
	        	Log.e("*:playTrack", "File not found: " + target.getPath());
	        	Toast.makeText(c, RESOURCES.getString(R.string.queue_error), Toast.LENGTH_SHORT).show();;
	        	Callisto.databaseConnector.deleteQueueItem(id);
				return;
	        }
	        media_location = target.getPath();
	    }
	    
		Intent notificationIntent = new Intent(c, EpisodeDesc.class);
		notificationIntent.putExtra("id", identity);
		PendingIntent contentIntent = PendingIntent.getActivity(c, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    	Callisto.notification_playing = new Notification(R.drawable.callisto, Callisto.RESOURCES.getString(R.string.playing), System.currentTimeMillis());
		Callisto.notification_playing.flags = Notification.FLAG_ONGOING_EVENT;
       	Callisto.notification_playing.setLatestEventInfo(c,  Callisto.playerInfo.title,  Callisto.playerInfo.show, contentIntent);
       	
       	mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_playing);
	    
       	
		try {
			if(Callisto.mplayer==null)
				Callisto.mplayer = new MediaPlayer(); //This could be a problem
			Callisto.mplayer.reset();
			Callisto.okNowPlay.setContext(c);
			Callisto.mplayer.setDataSource(media_location);
			Callisto.mplayer.setOnCompletionListener(Callisto.nextTrack);
			Callisto.mplayer.setOnErrorListener(Callisto.nextTrackBug);
			if(!startPlaying)
				return;
			Callisto.mplayer.setOnPreparedListener(okNowPlay);
			Log.i("*:playTrack", "Preparing...");
			if(isStreaming)
				okNowPlay.pd = ProgressDialog.show(c, Callisto.RESOURCES.getString(R.string.loading), Callisto.RESOURCES.getString(R.string.loading_msg), true, false);
			Callisto.mplayer.prepareAsync();
			//FIXME: EXCEPTIONS
		} catch (IllegalArgumentException e) {
			Log.e("*playTrack:IllegalArgumentException", "Error attempting to set the data path for MediaPlayer:");
			Log.e("*playTrack:IllegalArgumentException", media_location);
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Log.e("*playTrack:IllegalStateException", "Error in State for MediaPlayer:");
		} catch (IOException e) {
			Log.e("*playTrack:IOException", "IO is another of Jupiter's moons. Did you know that?");
			e.printStackTrace();
		}
		
		WidgetHandler.updateAllWidgets(c);
    }
    
    
	/** Essentially a struct with a few functions to handle the player, particularly updating it when switching activities. */ 
    public class PlayerInfo
    {
    	public MediaPlayer player;
    	public String title, show, date;
    	public int position = 0, length = 0;
    	public boolean isPaused = true;
    	
    	/** Constructor for the PlayerInfo class. Good to call when first creating the player controls, to set then to something.
    	 *  @param c The context for the current Activity. 
    	 */
    	public PlayerInfo(Context c)
    	{
    		TextView titleView = (TextView) ((Activity)c).findViewById(R.id.titleBar);
    		Log.v("PlayerInfo()", "Initializing PlayerInfo, queue size=" +  Callisto.databaseConnector.queueCount());
			titleView.setText(RESOURCES.getString(R.string.queue_size) + ": " + Callisto.databaseConnector.queueCount());
    	}
    	
    	/** Updates the player controls, like the title and times. Used excessively when changing Activities.
    	 * @param c The context for the current Activity.
    	 */
    	public void update(Context c)
    	{
    		if(is_widget)
    		{
    			is_widget = false;
    			return;
    		}
    		Callisto.nextTrack.setContext(c);
    		if(Callisto.mplayer!=null)
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
    	    	if(Callisto.live_player!=null)
        	    	titleView.setText(LiveStream.liveTitle + " - JB Radio");
    	    	else if(title==null)
        	    	titleView.setText("Playlist size: " + Callisto.databaseConnector.queueCount());
        	    else
        	    	titleView.setText(title + " - " + show);
    	    
    	    //timeView
	    	Callisto.timeView = (TextView) ((Activity)c).findViewById(R.id.timeAt);
	    	if(Callisto.timeView==null)
    	    	Log.w("Callisto:update", "Could not find view: " + "TimeView");
    	    else if(Callisto.live_player!=null)
    	    {
    	    	timeView.setText("--");
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
    	    	lengthView.setText("--");
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
		Callisto.this.runOnUiThread(TimerRunnable);
	}
	
	/** A runnable to be used in conjunction with the update() function. Updates the player time every set amount of time. */
	Runnable TimerRunnable = new Runnable()
	{
		int i=0;
		public void run()
		{
			if(Callisto.mplayer==null || !Callisto.mplayer.isPlaying())
				return;
			i++;
			Callisto.playerInfo.position = Callisto.mplayer.getCurrentPosition();
			current = Callisto.playerInfo.position/1000;
			timeProgress.setProgress(current);
			timeView.setText(formatTimeFromSeconds(current));
			if(i==Callisto.SAVE_POSITION_EVERY)
			{
				try {
				Log.v("Callisto:TimerMethod", "Updating position: " + Callisto.playerInfo.position);
		    	Cursor queue = Callisto.databaseConnector.currentQueue();
		    	queue.moveToFirst();
		    	Long identity = queue.getLong(queue.getColumnIndex("identity"));
				Callisto.databaseConnector.updatePosition(identity, Callisto.mplayer.getCurrentPosition());
				i=0;
				} catch(NullPointerException e)
				{
					Log.e("*:TimerRunnable", "NullPointerException when trying to update timer!");
				}
			}
		}
	};
	
	public static void playPause(Context c, View v)
	{			
		String live_url = PreferenceManager.getDefaultSharedPreferences(c).getString("live_url", "http://jbradio.out.airtime.pro:8000/jbradio_b");
		if(Callisto.live_player!=null)
		{
			if(Callisto.live_isPlaying)
			{
				Callisto.live_player.pause();
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
					LiveStream.liveInit();
					Callisto.live_player.setOnPreparedListener(LiveStream.livePreparedListenerOther);
					Callisto.live_player.setDataSource(live_url);
					LiveStream.livePrepare(v.getContext());
					if(v!=null)
						((ImageButton)v).setImageDrawable(Callisto.pauseDrawable);
				} catch(Exception e){}
			}
			Callisto.live_isPlaying = !Callisto.live_isPlaying;
			return;
		}
		if(databaseConnector.queueCount()==0)
			return;
		
		if(Callisto.mplayer==null)
		{
			Log.d("*:playPause","PlayPause initiated");
			Callisto.mplayer = new MediaPlayer();
			Callisto.mplayer.setOnCompletionListener(Callisto.nextTrack);
			Callisto.mplayer.setOnErrorListener(Callisto.nextTrackBug);
			Callisto.playTrack(c, 0, true);
		}
		else
		{
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
		WidgetHandler.updateAllWidgets(c);
	}
	
    
    /** Listener for play/pause button; calls playPause(), the function */
	public static OnClickListener playPauseListener = new OnClickListener() 
    {
		@Override
		  public void onClick(View v) 
		  {
			Callisto.playPause(v.getContext(), v);
		  }
    };
    
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
						playerInfo.update(psychV.getContext());
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
			case R.id.live:
				newIntent = new Intent(Callisto.this, LiveStream.class);
				break;
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
				}
	        });
		  }
    };
    
    
    /** Downloads and resizes a show's logo image.
     * @param img_url The URL for the image to download.
     * @param show The name of the show. (The path is calculated from this.)
     * @throws IOException
     * @throws NullPointerException
     */
    public static void downloadImage(String img_url, String show) throws IOException, NullPointerException
    {
    	if(img_url==null)
    		throw(new NullPointerException());
    	File f = new File(Environment.getExternalStorageDirectory() + File.separator + 
    				      storage_path + File.separator +
    				      show + EpisodeDesc.getExtension(img_url));
    	if(f.exists())
    		return;
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
    }

    
    
	/** Updates a show by checking to see if there are any new episodes available.
	 * 
	 * @param currentShow The number of the current show in relation to the AllShows.SHOW_LIST array
	 * @param showSettings The associated SharedPreferences with that show
	 * @param isVideo true to check the video feed, false to check the audio.
	 * @return A Message object with arg1 being 1 if the show found new episodes, 0 otherwise.
	 */
	public static Message updateShow(int currentShow, SharedPreferences showSettings, boolean isVideo)
	{
		  Log.i("*:updateShow", "Beginning update");
		  String epDate = null, epTitle = null, epDesc = null;
		  String lastChecked = showSettings.getString("last_checked", null);
		  
		  String newLastChecked = null;
	   	  try
  	  {
	   		  //Prepare the parser
  		  XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
  		  factory.setNamespaceAware(true);
  		  XmlPullParser xpp = factory.newPullParser();
  		  URL url = new URL(isVideo ? AllShows.SHOW_LIST_VIDEO[currentShow] : AllShows.SHOW_LIST_AUDIO[currentShow]);
  		  InputStream input = url.openConnection().getInputStream();
  		  xpp.setInput(input, null);
  		  
  		  Log.v("*:updateShow", "Parser is prepared");
  		  int eventType = xpp.getEventType();
  		
  		  while(!("title".equals(xpp.getName()) && eventType == XmlPullParser.END_TAG))
  		  {
  			  eventType = xpp.next();
  		  }
  		  eventType = xpp.next();
  		  
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
  				}
  				else
  				{
  					eventType = xpp.next();
	  				while(!(("image".equals(xpp.getName()) || ("url".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))))
	  					eventType = xpp.next();
	  				if(!("image".equals(xpp.getName())))
	                {
	                      eventType = xpp.next();
	                      imgurl = xpp.getText();
	                      while(!("image".equals(xpp.getName())))
	                    	  eventType = xpp.next();
	                }
  				}
  			  }

			  eventType = xpp.next();
			  if(eventType==XmlPullParser.END_DOCUMENT)
				  throw(new UnfinishedParseException("Thumbnail"));
		  }
  		  //Handles if no <image> tag was found, falls back to <media:thumbnail>
  		  if(("thumbnail".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
  			  imgurl2 = xpp.getAttributeValue(null, xpp.getAttributeName(0));
  		  
  		  if(imgurl2!=null)
  			  imgurl = imgurl2;
  		  
  		  try {
  			  if(imgurl!=null)
  				  downloadImage(imgurl, AllShows.SHOW_LIST[currentShow]);
  			  Log.v("*:updateShow", "Parser has downloaded image for " + AllShows.SHOW_LIST[currentShow]);
  		  } catch(Exception e) {
  			Log.v("*:updateShow", "Failed to download image");
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
				  eventType = xpp.next();
				  epTitle = xpp.getText();
				  System.out.println("B: " + xpp.getText());
				  if(epTitle==null)
					  throw(new UnfinishedParseException("Title"));
				  if(epTitle.indexOf("|")>0)
						epTitle = epTitle.substring(0, epTitle.indexOf("|")).trim();
				  Log.d("*:updateShow", "Title: " + epTitle);
				  
				  //Description
				  while(!("description".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
				  {
					  eventType = xpp.next();
					  if(eventType==XmlPullParser.END_DOCUMENT)
						  throw(new UnfinishedParseException("Description"));
				  }
				  eventType = xpp.next();
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
				  eventType = xpp.next();
				  epDate = xpp.getText();
				  Log.d("*:updateShow", "Date: " + epDate);
				  
				  
				  
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
				  
				  String epMediaLink = xpp.getAttributeValue(xpp.getNamespace(),"url");
				  if(epMediaLink==null)
					  throw(new UnfinishedParseException("MediaLink"));
				  
				  String temp = xpp.getAttributeValue(xpp.getNamespace(),"length");
				  if(temp==null)
					  throw(new UnfinishedParseException("MediaSize"));
				  long epMediaSize = Long.parseLong(temp);
				  
				  Log.d("*:updateShow", "Link: " + epMediaLink);
				  Log.d("*:updateShow", "Size: " + epMediaSize);
				  
				  
				  epDate = Callisto.sdfRaw.format(Callisto.sdfSource.parse(epDate));
				  //if(!Callisto.databaseConnector.updateMedia(AllShows.SHOW_LIST[currentShow], epTitle,
						  								//isVideo, epMediaLink, epMediaSize))
					  Callisto.databaseConnector.insertEpisode(AllShows.SHOW_LIST[currentShow], epTitle, epDate, epDesc, epMediaLink, epMediaSize, isVideo);
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

    
    
    //Everything below this line is either vastly incomplete or for debugging
    //-------------------------
    
    
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	menu.add(0, QUIT_ID, 0, RESOURCES.getString(R.string.quit)).setIcon(R.drawable.ic_menu_close_clear_cancel);
    	menu.add(0, SETTINGS_ID, 0, RESOURCES.getString(R.string.settings)).setIcon(R.drawable.ic_menu_preferences);
    	menu.add(0, STOP_ID, 0, RESOURCES.getString(R.string.stop)).setIcon(R.drawable.stop);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
 
        switch (item.getItemId())
        {
        case QUIT_ID:
        	finish();	//FEATURE: Completely quit
            return true;
        case SETTINGS_ID:
        	startActivity(new Intent(this, QuickPrefsActivity.class));
        	return true;
        case STOP_ID:
        	stop(this);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
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
    	WidgetHandler.updateAllWidgets(c);
    }
    
    public class oOnPreparedListener implements OnPreparedListener
    {
    	Context c;
    	public ProgressDialog pd = null;

    	public void setContext(Context c)
    	{
    		this.c = c;
    	}

		@Override
		public void onPrepared(MediaPlayer arg0) {
			Log.i("*:mplayer:onPrepared", "Prepared, seeking to " + Callisto.playerInfo.position);
			Callisto.mplayer.seekTo(Callisto.playerInfo.position);
			Callisto.playerInfo.length = Callisto.mplayer.getDuration()/1000;
			if(pd!=null)
				pd.cancel();
			ImageButton ib = ((ImageButton)((Activity)c).findViewById(R.id.playPause));
			if(ib!=null)
				ib.setImageDrawable(Callisto.pauseDrawable);
	    	Log.i("*:playTrack", "Starting to play: " + Callisto.playerInfo.title);
			Callisto.mplayer.start();
			Callisto.playerInfo.isPaused = false;
			Callisto.playerInfo.update(c);
			pd=null;
		}
    	
    }
    
    public class oOnCompletionListener implements OnCompletionListener
    {
    	Context c;

    	public void setContext(Context c)
    	{
    		this.c = c;
    	}
    	
		@Override
		public void onCompletion(MediaPlayer mp) {
			Log.i("*:mplayer:onCompletion", "Playing next track");
			boolean del = PreferenceManager.getDefaultSharedPreferences(this.c).getBoolean("completion_delete", false);
			if(del)
			{
		        File target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + Callisto.playerInfo.show);
		        target = new File(target,Callisto.playerInfo.date + "__" + Callisto.playerInfo.title + ".mp3");
		        target.delete();
			}
			Cursor c = Callisto.databaseConnector.currentQueue();
			c.moveToFirst();
			long id = c.getLong(c.getColumnIndex("identity"));
			Callisto.databaseConnector.updatePosition(id, 0);
			
			Callisto.playTrack(this.c, 1, true);
		}
    	
    }
    
    public class oOnErrorListener implements OnErrorListener
    {
    	Context c;

    	public void setContext(Context c)
    	{
    		this.c = c;
    	}
    	

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			System.out.println("Next Track Bug");
			//Callisto.playTrack(this.c);
			return true;
		}
    	
    }
}
