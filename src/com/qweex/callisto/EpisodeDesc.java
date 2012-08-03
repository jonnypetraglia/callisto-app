package com.qweex.callisto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
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

//FIXME: Player controls sometimes get bumped offscreen
//FEATURE: Resume Download
//FEATURE: Ellipsize TItle

public class EpisodeDesc extends Activity
{
	//-----Local variables-----
	private String media_link = "";
	private String title = "";
	private String description = "";
	private long media_size = 0;
	private String date = "";
	private String show = "";
	private File file_location;
	private Button streamButton, downloadButton;
	private long id = 0;
	private InputStream is;
	private FileOutputStream outStream = null;
	private byte[] buff = null;
	private boolean isLandscape;
	
	//-----Static Variables-----
	public static final DecimalFormat twoDec = new DecimalFormat("0.00");
	private static final String[] SUFFIXES = new String[] {"", "K", "M", "G", "T"};

	
	@Override
	public void onCreate(Bundle savedInstanceState) 
    {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
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
		media_size = c.getLong(c.getColumnIndex(AllShows.IS_VIDEO ? "vidsize" : "mp3size"));
		media_link = c.getString(c.getColumnIndex(AllShows.IS_VIDEO ? "vidlink" : "mp3link"));
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
		((TextView)this.findViewById(R.id.size)).setText(formatBytes(media_size));
		
		//Convert the date AGAIN into one that is used for the file.
        SimpleDateFormat sdfDestination = new SimpleDateFormat("yyyy-MM-dd");
        try {
			date = sdfDestination.format(Callisto.sdfRaw.parse(date));
		} catch (ParseException e) {
			Log.e("ShowList:ShowListAdapter:ParseException", "Error parsing a date that has already been parsed:");
			Log.e("ShowList:ShowListAdapter:ParseException", date);
			Log.e("ShowList:ShowListAdapter:ParseException", "(This should SERIOUSLY never happen).");
		}
        
		file_location = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + show);
		file_location = new File(file_location, date + "__" + title + getExtension(media_link));
		
		
		streamButton = ((Button)this.findViewById(R.id.stream));
		downloadButton = ((Button)this.findViewById(R.id.download));
		
		if(isLandscape)
		{
			LinearLayout ll = (LinearLayout) this.findViewById(R.id.mainLin);
			LinearLayout bb = (LinearLayout) this.findViewById(R.id.buttons);
			ll.removeView(bb);
			LinearLayout hh = (LinearLayout) this.findViewById(R.id.headLin);
			bb.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL));
			hh.addView(bb);
		}
	    
	    CheckBox rb = ((CheckBox)findViewById(R.id.newImg));
        rb.setChecked(is_new);
        rb.setOnCheckedChangeListener(toggleNew);
    }
	
	private String getExtension(String filename)
	{
		return filename.substring(filename.lastIndexOf("."));
	}

	@Override
	public void onResume()
	{
		super.onResume();
		Log.v("EpisodeDesc:onResume", "Resuming main activity");
		Callisto.playerInfo.update(EpisodeDesc.this);
		determineButtons();
	}
	
	//Listener for when the episode's "New" status is toggled
	private OnCheckedChangeListener toggleNew = new OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			Callisto.databaseConnector.markNew(id, isChecked);
		}
	};
	
	
	//Listener for when the "play" button is pressed
	private OnClickListener launchPlay = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 Callisto.databaseConnector.appendToQueue(id, false);
			 if(Callisto.databaseConnector.queueCount()==1)
			 {
				 Callisto.playTrack(v.getContext(), 1, true);
			 }
			 ((Button)v).setText(Callisto.RESOURCES.getString(R.string.enqueued));
			 ((Button)v).setEnabled(false);
		  }
    };

    //Listener for when the "delete" button is pressed
    private OnClickListener launchDelete = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
		 	file_location.delete();
		 	Callisto.databaseConnector.deleteQueueItem(id);
			streamButton.setText(Callisto.RESOURCES.getString(R.string.stream));
			streamButton.setOnClickListener(launchStream);
			downloadButton.setText(Callisto.RESOURCES.getString(R.string.download));
			downloadButton.setOnClickListener(launchDownload);
		  }
    };
	
    //Listener for when the "stream" button is pressed
    private OnClickListener launchStream = new OnClickListener() 
    {
		@Override
		  public void onClick(View v) 
		  {
			Log.i("EpisodeDesc:launchStream","Beginning streaming: " + media_link);
				EpisodeDesc.this.setProgressBarIndeterminateVisibility(true);
				if(Callisto.mplayer!=null && Callisto.mplayer.isPlaying())
  			  		Callisto.mplayer.stop();
				 Callisto.databaseConnector.appendToQueue(id, true);
				 if(Callisto.databaseConnector.queueCount()==1)
				 {
					 try {
					 Callisto.mplayer = new MediaPlayer();
					 Callisto.mplayer.setDataSource(media_link);
					 
					 Callisto.mplayer.setOnCompletionListener(Callisto.nextTrack);
					 Callisto.mplayer.prepareAsync();
					} catch (IllegalArgumentException e) {
						// TODO EXCEPTION
						e.printStackTrace();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				 }
				 ((Button)v).setText(Callisto.RESOURCES.getString(R.string.enqueued));
				 ((Button)v).setEnabled(false);
		  }
    };
    
    //Listener for when the "download" button is pressed
    private OnClickListener launchDownload = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			//http://www.androidsnippets.com/download-an-http-file-to-sdcard-with-progress-notification
			 	Callisto.downloading_count++;
			 	Callisto.download_queue.add(EpisodeDesc.this.id);
			 	Log.i("EpisodeDesc:launchDownload", "Adding download: " + media_link);
			 	
			 	if(Callisto.download_queue.size()==1)
					new DownloadTask().execute(media_link);
			 	determineButtons();
		}
    };
    
    // Starts downloading a file outside the UI thread
    private class DownloadTask extends AsyncTask<String, Object, Boolean> 
    {
    	
    	private String Title, Date, Link, Show;
    	private long TotalSize;
    	private File Target;
		private final int NOTIFICATION_ID = 3696;
		private final int TIMEOUT_CONNECTION = 5000;
		private final int TIMEOUT_SOCKET = 30000;
		private NotificationManager mNotificationManager;
		private PendingIntent contentIntent;
		
		@Override
		protected void onPreExecute()
		{
    		Log.i("EpisodeDesc:DownloadTask", "Beginning downloads");
			Intent notificationIntent = new Intent(EpisodeDesc.this, DownloadList.class);
			contentIntent = PendingIntent.getActivity(EpisodeDesc.this, 0, notificationIntent, 0);
			Callisto.notification_download = new Notification(R.drawable.callisto, Callisto.RESOURCES.getString(R.string.beginning_download), System.currentTimeMillis());
			Callisto.notification_download.flags = Notification.FLAG_ONGOING_EVENT;
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Callisto.notification_download.setLatestEventInfo(getApplicationContext(), Callisto.RESOURCES.getString(R.string.downloading) + " " + Callisto.current_download + " " +  Callisto.RESOURCES.getString(R.string.of) + " " + Callisto.downloading_count + ": 0%", Show + ": " + Title, contentIntent);
		}
		
		
    	@Override
    	protected Boolean doInBackground(String... params)
    	{
    		
			Cursor current;
			while(!Callisto.download_queue.isEmpty())
			{
	   			try
				{
    		   		long id = Callisto.download_queue.get(0);
					current = Callisto.databaseConnector.getOneEpisode(id);
					current.moveToFirst();
					Link = current.getString(current.getColumnIndex(AllShows.IS_VIDEO ? "vidlink" : "mp3link"));
					Title = current.getString(current.getColumnIndex("title"));
					Date = current.getString(current.getColumnIndex("date"));
					Show = current.getString(current.getColumnIndex("show"));
					Log.i("EpisodeDesc:DownloadTask", "Starting download: " + Link);
					
					URL url = new URL(Link);
					URLConnection ucon = url.openConnection();
					ucon.setReadTimeout(TIMEOUT_CONNECTION);
					ucon.setConnectTimeout(TIMEOUT_SOCKET);
					
					Date = Callisto.sdfFile.format(Callisto.sdfRaw.parse(Date));
		           
		
					is = ucon.getInputStream();
					BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
					Target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + Show);
					Target.mkdirs();
					if(title.indexOf("|")>0)
						Title = Title.substring(0, title.indexOf("|"));
					Title=Title.trim();
					Target = new File(Target, Date + "__" + Title + getExtension(Link));
					
					
					outStream = new FileOutputStream(Target);
					buff = new byte[5 * 1024];
					TotalSize = ucon.getContentLength();
					int len;
					long downloadedSize = Target.length();
					long perc = 0;
					
					//Here is where the actual downloading happens
					while ((len = inStream.read(buff)) != -1)
					{
						outStream.write(buff,0,len);
			            downloadedSize += len;
				       	perc = downloadedSize*100;
				       	perc /= TotalSize;
				       	System.out.println(perc + "%");
				       	Callisto.notification_download.setLatestEventInfo(getApplicationContext(), Callisto.RESOURCES.getString(R.string.downloading) + " " + Callisto.current_download + " " + Callisto.RESOURCES.getString(R.string.of) + " " + Callisto.downloading_count + ": " + perc + "%", Show + ": " + Title, contentIntent);
				       	mNotificationManager.notify(NOTIFICATION_ID, Callisto.notification_download);
					}
					Callisto.current_download++;
		           
		           
					outStream.flush();
					outStream.close();
					inStream.close();
					Log.i("EpisodeDesc:DownloadTask", "Successfully downloaded to : " + Target.getPath());
					Callisto.download_queue.remove(0);
		       } catch (IOException e) {
		    	   Log.e("EpisodeDesc:DownloadTask:IOException", "IO is a moon");
		    	   e.printStackTrace();
		       } catch (ParseException e) {
					Log.e("EpisodeDesc:DownloadTask:ParseException", "Error parsing a date from the SQLite db: ");
					Log.e("EpisodeDesc:DownloadTask:ParseException", Date);
					Log.e("EpisodeDesc:DownloadTask:ParseException", "(This should never happen).");
					e.printStackTrace();
		       }
			}
			/*//IDEA: Change intent upon download completion?
			notificationIntent = new Intent(null, Callisto.class);
			contentIntent = PendingIntent.getActivity(EpisodeDesc.this, 0, notificationIntent, 0);
			*/
		       
			Log.i("EpisodeDesc:DownloadTask", "Finished Downloading");
       		mNotificationManager.cancel(NOTIFICATION_ID);
		    Callisto.notification_download = new Notification(R.drawable.callisto, "Finished downloading " + Callisto.downloading_count + " files", NOTIFICATION_ID);
		    Callisto.notification_download.setLatestEventInfo(getApplicationContext(), "Finished downloading " + Callisto.downloading_count + " files", null, contentIntent);
		    Callisto.notification_download.flags = Notification.FLAG_AUTO_CANCEL;
		   	mNotificationManager.notify(NOTIFICATION_ID, Callisto.notification_download);
		    Callisto.current_download=1;
		    Callisto.downloading_count=0;
		    return true;
       }
       
       @Override
       protected void onPostExecute(Boolean result)
       {
    	   if(result)
    	   {
				streamButton.setText(Callisto.RESOURCES.getString(R.string.play));
				streamButton.setOnClickListener(launchPlay);
				downloadButton.setText(Callisto.RESOURCES.getString(R.string.delete));
				downloadButton.setOnClickListener(launchDelete);
    	   } else
    	   {
	   			streamButton.setText(Callisto.RESOURCES.getString(R.string.stream));
	   			streamButton.setOnClickListener(launchStream);
	   			downloadButton.setText(Callisto.RESOURCES.getString(R.string.download));
	   			downloadButton.setOnClickListener(launchDownload);
    	   }
       }
    }
    
    private void determineButtons()
    {
    	if(Callisto.download_queue.contains(id))
    	{
    		streamButton.setText(Callisto.RESOURCES.getString(R.string.downloading));
    		streamButton.setEnabled(false);
			downloadButton.setText(Callisto.RESOURCES.getString(R.string.cancel));
			//FEATURE: Cancel Download
			//downloadButton.setOnClickListener(launchCancel);
    	}
    	else if(file_location.exists())
		{
    		if(file_location.length()!=media_size)
    		{
        		streamButton.setText(Callisto.RESOURCES.getString(R.string.resume));
        		downloadButton.setOnClickListener(launchDownload);
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
    
    
    public static String formatBytes(long input)
    {
		  double temp = input;
		  int i;
		  for(i=0; temp>5000; i++)
			  temp/=1024;
		  return (EpisodeDesc.twoDec.format(temp) + " " + EpisodeDesc.SUFFIXES[i] + "B");
    }
}
