package com.qweex.callisto;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Notification;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;

/* Features to add:
 *  -"listened to" status
 *  -Queue
 * 	-Download mp3s
 *   -Download queue window
 *  -Calendar
 *  -Chat
 *  -Contact POST
 *  -Combine pause and play 
 *  ~New icon to replace star
 *  
 *  X Contact Form Spinner
 *  ? In-app Donate
 *  ? Separate section for radio
 */


public class Callisto extends Activity {

	public static MediaPlayer player = new MediaPlayer();
	public static DatabaseConnector databaseConnector;
	
	public static Notification notification;
	public final String DONATION_APP_ID = "jupiter.broadcasting.live.tv";
	public static String storage_path = "callisto";
	public static int downloading_count = 0;
	public static int current_download = 1;
	public static ArrayList<Long> download_queue = new ArrayList<Long>();
	
	public static final SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss");
	public static final SimpleDateFormat sdfSource = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
	public static final SimpleDateFormat sdfDestination = new SimpleDateFormat("MM/dd/yyyy");
	
	static TextView timeView;
	static int current;
	static ProgressBar timeProgress;
	public static Drawable playDrawable, pauseDrawable;
	
	Button listen, watch, plan, chat, contact, donate;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		View mainMenu = ((LayoutInflater)this.getSystemService(this.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.main, null, false);
		Callisto.build_layout(this, mainMenu);
		
		
		listen = (Button)findViewById(R.id.listen);
		listen.setOnClickListener(startAct);
		watch = (Button)findViewById(R.id.watch);
		watch.setOnClickListener(startAct);
		plan = (Button)findViewById(R.id.plan);
		plan.setOnClickListener(startAct);
		chat = (Button)findViewById(R.id.chat);
		chat.setOnClickListener(startAct);
		contact = (Button)findViewById(R.id.contact);
		contact.setOnClickListener(startAct);
		donate = (Button)findViewById(R.id.donate);
		donate.setOnClickListener(startAct);
		
		playDrawable = getResources().getDrawable(R.drawable.ic_media_play);
		pauseDrawable = getResources().getDrawable(R.drawable.ic_media_pause);
		
		databaseConnector = new DatabaseConnector(Callisto.this);
	}
	
    public static void build_layout(Context c, View mainView)
    {
		View controls = ((LayoutInflater)c.getSystemService(c.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.controls, null, false);
		
		LinearLayout layout = new LinearLayout(c);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams
				(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f);
		LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams
				(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0f);
		layout.addView(mainView, mParams);
		layout.addView(controls, cParams);
		((Activity)c).setContentView(layout);
		((Activity)c).findViewById(R.id.playPause).setOnClickListener(Callisto.playPause);
		((Activity)c).findViewById(R.id.playlist).setOnClickListener(Callisto.playlist);
    }
    
    public static void playNext(Context c)
    {    	
    	Callisto.databaseConnector.open();
    	Cursor queue = Callisto.databaseConnector.getQueue();
    	queue.moveToFirst();
    	Long identity = queue.getLong(queue.getColumnIndex("identity"));
    	
        Cursor db = Callisto.databaseConnector.getOneEpisode(identity);
	    db.moveToFirst();
	    String title = db.getString(db.getColumnIndex("title"));
	    
	    String date = db.getString(db.getColumnIndex("date"));
	    String show = db.getString(db.getColumnIndex("show"));
	    int posi = db.getColumnIndex("position");
	    System.out.println(posi);
	    int positionInSeconds = db.getInt(posi);
	    int lengthInSeconds;
	    Callisto.databaseConnector.close();
	    System.out.println("Starting to play: " + title);
    	
	    TextView titleView = (TextView) ((Activity)c).findViewById(R.id.titleBar);
	    timeView = (TextView) ((Activity)c).findViewById(R.id.timeAt);
	    TextView lengthView = (TextView) ((Activity)c).findViewById(R.id.length);
	    timeProgress = (ProgressBar) ((Activity)c).findViewById(R.id.timeProgress);
        SimpleDateFormat sdfDestination = new SimpleDateFormat("yyyy-MM-dd");
        
        try {
			date = sdfDestination.format(Callisto.sdfRaw.parse(date));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
    	File target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + show);
	    target = new File(target,date + "__" + title + ".mp3");
	    
		try {
			Callisto.player.setDataSource(target.getPath());
			Callisto.player.prepare();
	    	lengthInSeconds = Callisto.player.getDuration()/1000;
			titleView.setText(title + " - " + show);
			Callisto.player.seekTo(positionInSeconds*1000);
		    timeView.setText(formatTimeFromSeconds(positionInSeconds));
		    timeProgress.setMax(lengthInSeconds);
	    	lengthView.setText(formatTimeFromSeconds(lengthInSeconds));
	    	View III = ((Activity)c).findViewById(R.id.playPause);
	    	((Button)III).setCompoundDrawablesWithIntrinsicBounds(Callisto.pauseDrawable, null, null, null);
			Callisto.player.start();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    playThread.start();
    }
    
    public static Thread playThread = new Thread() {

        @Override
        public void run() {

        while(true)
        {
          while(Callisto.player.isPlaying())
          {
        	  current = Callisto.player.getCurrentPosition()/1000;
        	  mHandler.sendMessage(new Message());
          }
        }
        }
    };
    
	static Handler mHandler = new Handler()
	{
        @Override
        public void handleMessage(Message msg)
        {
        	timeProgress.setProgress(current);
        	timeView.setText(formatTimeFromSeconds(current));
		}
	};

	public static OnClickListener playPause = new OnClickListener() 
    {
		@Override
		  public void onClick(View v) 
		  {
			if(Callisto.player==null)
				return;
			try {
				if(!Callisto.player.isPlaying())
				{
					try {
						Callisto.player.start();
						((Button)v).setCompoundDrawablesWithIntrinsicBounds(Callisto.this.pauseDrawable, null, null, null);
					}
					catch(IllegalStateException e)
					{
						System.out.println("Play Next");
						Callisto.playNext(v.getContext());
					}
				}
				else
				{
					((Button)v).setCompoundDrawablesWithIntrinsicBounds(Callisto.this.playDrawable, null, null, null);
					Callisto.player.pause();
				}
			} catch(IllegalStateException e)
			{
			}
		  }
    };
	
    private static String formatTimeFromSeconds(int seconds)
    {
  	  int minutes = seconds / 60;
  	  seconds %= 60;
  	  if(minutes>60)
  	  {
  		  int hours = minutes / 60;
  		  minutes %= 60;
  		  return ((hours<10?"0":"") + Integer.toString(hours) + ":" + 
  				    (minutes<10?"0":"") + Integer.toString(minutes) + ":" + 
  				    (seconds<10?"0":"") + Integer.toString(seconds));
  	  }
  	  else
  		  return ((minutes<10?"0":"") + Integer.toString(minutes) + ":" + 
  				    (seconds<10?"0":"") + Integer.toString(seconds));
    }
    

    public static OnClickListener playlist = new OnClickListener() 
    {
		@Override
		  public void onClick(View v) 
		  {
			Intent newIntent = new Intent(v.getContext(), NowPlaying.class);
			v.getContext().startActivity(newIntent);
		  }
    };
    
    OnClickListener startAct = new OnClickListener()
    {
		@Override
		  public void onClick(View v) 
		  {
			Intent newIntent;
			if(v==donate)
			{
				newIntent = new Intent(Intent.ACTION_VIEW);
				
				
				try {
					newIntent.setData(Uri.parse("market://details?id=" + DONATION_APP_ID));
					startActivity(newIntent);
				} catch(ActivityNotFoundException e)
				{
					newIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + DONATION_APP_ID));
					startActivity(newIntent);
				}
			}
			else {
				   newIntent = new Intent(Callisto.this,
					(v==listen || v==watch) ? AllShows.class : (
					v==plan ? CalendarScreen.class : (
					//v==chat ? IRCChat.class : (
					v==contact ? ContactForm.class : (
					//)))));
							  AllShows.class))));
				   newIntent.putExtra("screen", (v==watch?true:false));
				   startActivity(newIntent);
			}
		  }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.layout.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
 
        switch (item.getItemId())
        {
        case R.id.menu_quit:
        	finish();
            //Toast.makeText(AndroidMenusActivity.this, "Preferences is Selected", Toast.LENGTH_SHORT).show();
            return true;
 
        default:
            return super.onOptionsItemSelected(item);
        }
    }    
}
