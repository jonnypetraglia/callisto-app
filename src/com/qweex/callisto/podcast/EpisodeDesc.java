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
package com.qweex.callisto.podcast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import android.content.SharedPreferences;
import android.view.*;
import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

//This activity is for displaying specific information about an episode

/** An activity for showing the specific info about a particular episode.
 * @author MrQweex */
public class EpisodeDesc extends Activity
{
	//-----Local variables-----
	private static final int STOP_ID=Menu.FIRST+1;
	private static final int SHARE_ID=STOP_ID + 1;
	private String mp3_link = "", vid_link = "";
	private String title = "";
	private String description = "";
	private String link = "";
	private long mp3_size = 0, vid_size = 0;
	private String date = "";
	private String show = "";
	private File file_location_audio, file_location_video;
	private Button streamButton, downloadButton;
	private long id = 0;
	private byte[] buff = null;
	private boolean isLandscape;
    private TextView audioSize, videoSize, audioTab, videoTab;
    private boolean vidSelected = false;
	
	//-----Static Variables-----
	public static final DecimalFormat twoDec = new DecimalFormat("0.00");
	private static final String[] SUFFIXES = new String[] {"", "K", "M", "G", "T"};

	/** Called when the activity is first created. Sets up the view.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) 
    {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		EpisodeDesc.this.setProgressBarIndeterminateVisibility(false);
		
		Log.v("EpisodeDesc:OnCreate", "Launching Activity");
		View info = ((LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.episode, null, false);
		Callisto.build_layout(this, info);
		
		isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
		
		Bundle b = getIntent().getExtras();
		if(b==null)
		{
			Log.e("EpisodeDesc:OnCreate", "Bundle is null. No extra could be retrieved.");
			Toast.makeText(EpisodeDesc.this, Callisto.RESOURCES.getString(R.string.song_error), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		id = b.getLong("id", 0);
		Cursor c = Callisto.databaseConnector.getOneEpisode(id);
		if(id==0 || c.getCount()==0)
		{
			Log.e("EpisodeDesc:OnCreate", "Id is invalid/blank");
			Toast.makeText(EpisodeDesc.this, Callisto.RESOURCES.getString(R.string.song_error), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		c.moveToFirst();
		boolean is_new = c.getInt(c.getColumnIndex("new"))>0;
		title = c.getString(c.getColumnIndex("title"));
		date = c.getString(c.getColumnIndex("date"));
		description = c.getString(c.getColumnIndex("description"));
		link = c.getString(c.getColumnIndex("link"));
        mp3_size = c.getLong(c.getColumnIndex("mp3size"));
        mp3_link = c.getString(c.getColumnIndex("mp3link"));
        vid_size = c.getLong(c.getColumnIndex("vidsize"));
        vid_link = c.getString(c.getColumnIndex("vidlink"));
		show = c.getString(c.getColumnIndex("show"));
		
		setTitle(title);
		description = android.text.Html.fromHtml(description).toString();
	    description = description.replace(String.valueOf((char)0xFFFC),"").trim();		//Relace [obj] in the description
		
		((TextView)this.findViewById(R.id.title)).setText(title);
		((TextView)this.findViewById(R.id.description)).setText(description);
		try {
			((TextView)this.findViewById(R.id.date)).setText(Callisto.sdfDestination.format(Callisto.sdfRaw.parse(date)));
		} catch (ParseException e1) {
			Log.e("EpisodeDesc:ShowListAdapter:ParseException", "Error parsing a date from the SQLite db: ");
			Log.e("EpisodeDesc:ShowListAdapter:ParseException", date);
			Log.e("EpisodeDesc:ShowListAdapter:ParseException", "(This should never happen).");
			e1.printStackTrace();
		}
		((TextView)this.findViewById(R.id.audio_size)).setText(formatBytes(mp3_size));
        ((TextView)this.findViewById(R.id.video_size)).setText(formatBytes(vid_size));
		//Convert the date AGAIN into one that is used for the file.
        SimpleDateFormat sdfDestination = new SimpleDateFormat("yyyy-MM-dd");
        try {
			date = sdfDestination.format(Callisto.sdfRaw.parse(date));
		} catch (ParseException e) {
			Log.e("ShowList:ShowListAdapter:ParseException", "Error parsing a date that has already been parsed:");
			Log.e("ShowList:ShowListAdapter:ParseException", date);
			Log.e("ShowList:ShowListAdapter:ParseException", "(This should SERIOUSLY never happen).");
		}
		file_location_audio = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + show);
		file_location_audio = new File(file_location_audio, date + "__" + DownloadList.makeFileFriendly(title) + getExtension(mp3_link));
        System.out.println(vid_link==null?"Fail":"nope");
        if(vid_link!=null)
        {
            file_location_video = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + show);
            file_location_video = new File(file_location_video, date + "__" + DownloadList.makeFileFriendly(title) + getExtension(vid_link));
        }
		
		streamButton = ((Button)this.findViewById(R.id.stream));
		streamButton.setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
		downloadButton = ((Button)this.findViewById(R.id.download));
		downloadButton.setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
		
		if(isLandscape)
		{
			LinearLayout ll = (LinearLayout) this.findViewById(R.id.mainLin);
			LinearLayout bb = (LinearLayout) this.findViewById(R.id.buttons);
            if(bb.getParent()!=null)
                ((LinearLayout)bb.getParent()).removeView(bb);
			ll.removeView(bb);
			LinearLayout hh = (LinearLayout) this.findViewById(R.id.headLin);
			bb.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, Gravity.CENTER_VERTICAL));
            if(bb.getParent()!=null)
                ((ViewGroup)bb.getParent()).removeView(bb);
			hh.addView(bb);
		}
	    
		Callisto.nextTrack.setRunnable(new Runnable(){
			public void run() {
				determineButtons(false);
			}
		});
		
		WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(DownloadList.Download_wifiLock==null)
		    DownloadList.Download_wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "Callisto_download");
		
	    CheckBox rb = ((CheckBox)findViewById(R.id.newImg));
        rb.setChecked(is_new);
        rb.setOnCheckedChangeListener(toggleNew);


        //Set up the tabs
        audioSize = (TextView) findViewById(R.id.audio_size);
        videoSize = (TextView) findViewById(R.id.video_size);
        audioTab = (TextView) findViewById(R.id.audio_tab);
        videoTab = (TextView) findViewById(R.id.video_tab);
        audioTab.setOnClickListener(changeTab);
        videoTab.setOnClickListener(changeTab);
    }
	
	/** Finds the file extension of a path.
	 * @param filename The filename to examine
	 * @return The extension of the input file, including the ".".
	 */
	public static String getExtension(String filename)
	{
		return filename.substring(filename.lastIndexOf("."));
	}
	
	/** Called when it is time to create the menu.
	 * @param menu Um, the menu
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	menu.add(0, STOP_ID, 0, Callisto.RESOURCES.getString(R.string.stop)).setIcon(R.drawable.ic_action_playback_stop);
    	menu.add(0, SHARE_ID, 0, Callisto.RESOURCES.getString(R.string.share)).setIcon(R.drawable.ic_action_share);
        return true;
    }
    
    /** Called when an item in the menu is pressed.
	 * @param item The menu item ID that was pressed
	 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
 
        switch (item.getItemId())
        {
        case STOP_ID:
        	Callisto.stop(this);
        	return true;
        case SHARE_ID:
            Intent i=new Intent(android.content.Intent.ACTION_SEND);

            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Thought I'd share this with you!");
            i.putExtra(Intent.EXTRA_TEXT, "Check out this awesome episode of " + show + "!\n\n" + link);

            startActivity(Intent.createChooser(i, Callisto.RESOURCES.getString(R.string.share)));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

	/** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
	@Override
	public void onResume()
	{
		super.onResume();
		Log.v("EpisodeDesc:onResume", "Resuming main activity");
        EpisodeDesc.this.setProgressBarIndeterminateVisibility(false);
		Callisto.playerInfo.update(EpisodeDesc.this);
		determineButtons(false);
	}
	
	/** Listener for when the episode's "New" status is toggled. */
	private OnCheckedChangeListener toggleNew = new OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			System.out.println("ED "+ id + " " + isChecked);
			Callisto.databaseConnector.markNew(id, isChecked);
		}
	};
	
	
	/** Listener for when the "play" button is pressed. */
	public OnClickListener launchPlay = new OnClickListener()
    {
		 @Override
		  public void onClick(View v) 
		  {
			 Callisto.databaseConnector.appendToQueue(id, false, vidSelected);
			 if(Callisto.databaseConnector.queueCount()==1)
			 {
				 Callisto.playTrack(v.getContext(), 1, true);
			 }
			 ((Button)v).setText(Callisto.RESOURCES.getString(R.string.enqueued));
			 ((Button)v).setEnabled(false);
			 Callisto.playerInfo.update(v.getContext());
		  }
    };

    /** Listener for when the "delete" button is pressed. */
    public OnClickListener launchDelete = new OnClickListener()
    {
		 @Override
		  public void onClick(View v) 
		  {
            if(vidSelected)
		 	    file_location_video.delete();
            else
                file_location_audio.delete();
		 	int tempId = -1;
		 	Cursor c = Callisto.databaseConnector.currentQueue();
		 	if(c.getCount()!=0)
		 	{
		 		c.moveToFirst();
		 		tempId = c.getInt(c.getColumnIndex("_id"));
		 	}
		 	if(id==tempId)
		 	{
		 		Callisto.playTrack(v.getContext(), 1, !Callisto.playerInfo.isPaused);
		 		Callisto.databaseConnector.advanceQueue(1);
				 boolean isPlaying = (Callisto.mplayer!=null && !Callisto.mplayer.isPlaying());
				 Callisto.playTrack(v.getContext(), 1, !Callisto.playerInfo.isPaused);
				 if(isPlaying)
				 {
				    Callisto.playerInfo.isPaused = true;
				 	Callisto.mplayer.pause();
				 }
				 Callisto.databaseConnector.advanceQueue(1);
		 	}
		 	Callisto.databaseConnector.deleteQueueItem(id);
			streamButton.setText(Callisto.RESOURCES.getString(R.string.stream));
			streamButton.setOnClickListener(launchStream);
			downloadButton.setText(Callisto.RESOURCES.getString(R.string.download));
			downloadButton.setOnClickListener(launchDownload);
			Callisto.playerInfo.update(v.getContext());
		  }
    };
	
    /** Listener for when the "stream" button is pressed. */
    public OnClickListener launchStream = new OnClickListener()
    {
		@Override
		  public void onClick(View v) 
		  {
			Log.i("EpisodeDesc:launchStream","Beginning streaming: " + (vidSelected ? vid_link : mp3_link));
				 Callisto.databaseConnector.appendToQueue(id, true, vidSelected);
				 if(Callisto.databaseConnector.queueCount()==1)
				 {
					 Callisto.playTrack(v.getContext(), 1, true);
				 }
				 ((Button)v).setText(Callisto.RESOURCES.getString(R.string.enqueued));
				 ((Button)v).setEnabled(false);
		  }
    };
    
    /** Listener for when the "download" button is pressed. */
    public OnClickListener launchDownload = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            //http://www.androidsnippets.com/download-an-http-file-to-sdcard-with-progress-notification
            Callisto.downloading_count++;

            SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(v.getContext()).edit();
            String aDownloads = PreferenceManager.getDefaultSharedPreferences(v.getContext()).getString("ActiveDownloads", "");
            aDownloads = "";
            if(aDownloads.equals(""))
                aDownloads = "|";
            aDownloads = aDownloads.concat(Long.toString(EpisodeDesc.this.id * (vidSelected?-1:1)) + "|");
            Log.i("EpisodeDesc:launchDownload", "Updated download list: " + aDownloads);
            e.putString("ActiveDownloads", aDownloads);
            e.commit();

            //Callisto.download_queue.add(EpisodeDesc.this.id * (vidSelected?-1:1));
            Log.i("EpisodeDesc:launchDownload", "Adding download: " + (vidSelected ? vid_link : mp3_link));

            if(!DownloadList.Download_wifiLock.isHeld())
            DownloadList.Download_wifiLock.acquire();
            if(!DownloadTask.running)
            {
                Log.i("EpisodeDesc:launchDownload", "Executing downloads");
                new DownloadTask(EpisodeDesc.this).execute(vidSelected ? vid_link : mp3_link);
            }
            determineButtons(false);
        }
    };
    
    /** Listener for when the "download" button is pressed. */
    private OnClickListener launchCancel = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
              SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(v.getContext()).edit();
              String aDownloads = PreferenceManager.getDefaultSharedPreferences(v.getContext()).getString("ActiveDownloads", "");
              aDownloads = aDownloads.replace("|" + Long.toString(id) + "|", "|");
              if(aDownloads.equals("|"))
                  aDownloads="";
              e.putString("ActiveDownloads", aDownloads);
              e.commit();
			  determineButtons(true);
		  }
    };

    private OnClickListener changeTab = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if(view==audioTab)
            {
                if(!vidSelected)
                    return;
                videoTab.setBackgroundColor(0xff999999);
                audioTab.setBackgroundColor(0xffcccccc);
                findViewById(R.id.audio_size).setVisibility(View.VISIBLE);
                findViewById(R.id.video_size).setVisibility(View.GONE);
                vidSelected=false;
            }
            else
            {
                if(vidSelected)
                    return;
                videoTab.setBackgroundColor(0xffcccccc);
                audioTab.setBackgroundColor(0xff999999);
                findViewById(R.id.audio_size).setVisibility(View.GONE);
                findViewById(R.id.video_size).setVisibility(View.VISIBLE);
                vidSelected=true;
            }
            determineButtons(false);
        }
    };

    /** Determines the buttons' text and listeners depending on the status of whether the episode has been downloaded already.
     * @param forceNotThere Set to True to force the function to believe that the file for the episode is not there */
    private void determineButtons(boolean forceNotThere)
    {
        File curr = (vidSelected ? file_location_video : file_location_audio);
        long curr_size = (vidSelected ? vid_size : mp3_size);
    	if(PreferenceManager.getDefaultSharedPreferences(EpisodeDesc.this).getString("ActiveDownloads", "").contains("|" + Long.toString(id) + "|") && !forceNotThere)
    	{
    		streamButton.setText(Callisto.RESOURCES.getString(R.string.downloading));
    		streamButton.setEnabled(false);
			downloadButton.setText(Callisto.RESOURCES.getString(R.string.cancel));
			downloadButton.setOnClickListener(launchCancel);
    	}
    	else if(curr.exists() && !forceNotThere)
		{
    		if(curr.length()!=curr_size)
    		{
        		streamButton.setText(Callisto.RESOURCES.getString(R.string.resume));
        		streamButton.setOnClickListener(launchDownload);
    		} else if(Callisto.databaseConnector.isInQueue(id))
        	{
        		streamButton.setText(Callisto.RESOURCES.getString(R.string.enqueued));
        		streamButton.setEnabled(false);
        	}
        	else
        	{
	    		streamButton.setEnabled(true);
				streamButton.setText(Callisto.RESOURCES.getString(R.string.play));
				streamButton.setOnClickListener(launchPlay);
        	}
			downloadButton.setText(Callisto.RESOURCES.getString(R.string.delete));
			downloadButton.setOnClickListener(launchDelete);
		}
		else
		{
			streamButton.setEnabled(true);
			streamButton.setText(Callisto.RESOURCES.getString(R.string.stream));
			streamButton.setOnClickListener(launchStream);
			downloadButton.setText(Callisto.RESOURCES.getString(R.string.download));
			downloadButton.setOnClickListener(launchDownload);
		}
    }
    
    /** Formats a number given in Bytes into a human readable format.
     * @param input The input number to examine in bytes
     * @return A human formatted string, rounded to two decimal places
     */
    public static String formatBytes(long input)
    {
		  double temp = input;
		  int i;
		  for(i=0; temp>5000; i++)
			  temp/=1024;
		  return (EpisodeDesc.twoDec.format(temp) + " " + EpisodeDesc.SUFFIXES[i] + "B");
    }
    

}
