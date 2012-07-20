package com.qweex.callisto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/*
 * Title
 * Description
 * mp3 link
 * mp3 file size?
 * video link
 * video file size?
 */

public class EpisodeDesc extends Activity implements OnPreparedListener
{
	public String media_link = "";
	String title = "";
	String date = "";
	String show = "";
	int mediaFileLengthInMilliseconds = 0;
	public File target;
	Button streamButton, downloadButton;
	
	FileOutputStream outStream = null;
	InputStream is;
	byte[] buff = null;
	int totalSize = 0;
	
	long id = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
    {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		
		View info = ((LayoutInflater)this.getSystemService(this.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.episode, null, false);
		Callisto.build_layout(this, info);
		
		
		Callisto.databaseConnector.open();
		id = getIntent().getExtras().getLong("id", 0);
		if(id==0)
			return;
		Cursor c = Callisto.databaseConnector.getOneEpisode(id);
		
		c.moveToFirst();
		boolean is_new = c.getInt(c.getColumnIndex("new"))>0;
		title = c.getString(c.getColumnIndex("title"));
		date = c.getString(c.getColumnIndex("date"));
		String description = c.getString(c.getColumnIndex("description"));
		String media_size = c.getString(c.getColumnIndex(AllShows.IS_VIDEO ? "vidsize" : "mp3size"));
		show = c.getString(c.getColumnIndex("show"));
		Callisto.databaseConnector.close();
		
		
		this.setTitle(title);
		description = android.text.Html.fromHtml(description).toString();
		
	    description = description.replace(String.valueOf((char)0xFFFC),"").trim();
		
		((TextView)this.findViewById(R.id.title)).setText(title);
		((TextView)this.findViewById(R.id.description)).setText(description);
		try {
			((TextView)this.findViewById(R.id.date)).setText(Callisto.sdfDestination.format(Callisto.sdfRaw.parse(date)));
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		((TextView)this.findViewById(R.id.size)).setText(media_size);

        SimpleDateFormat sdfDestination = new SimpleDateFormat("yyyy-MM-dd");
        
        try {
			date = sdfDestination.format(Callisto.sdfRaw.parse(date));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + show);
	    target = new File(target,date + "__" + title + ".mp3");
		
	    streamButton = ((Button)this.findViewById(R.id.stream));
	    downloadButton = ((Button)this.findViewById(R.id.download));
	    
	    CheckBox rb = ((CheckBox)findViewById(R.id.newImg));
        rb.setChecked(is_new);
        rb.setOnCheckedChangeListener(toggleNew);
	    
		if(target.exists())
		{
			streamButton.setText(Callisto.databaseConnector.queueCount() == 0 ? "Play" :"Enqueue");
			streamButton.setOnClickListener(launchPlay);
			downloadButton.setText("Delete");
			downloadButton.setOnClickListener(launchDelete);
		}
		else
		{
			streamButton.setText("Stream");
			streamButton.setOnClickListener(launchStream);
			downloadButton.setText("Download");
			downloadButton.setOnClickListener(launchDownload);
		}
    }
	
	public OnCheckedChangeListener toggleNew = new OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			Callisto.databaseConnector.markNew(
					id
					, isChecked);
		}
	};
	
	OnClickListener launchPlay = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 Callisto.databaseConnector.appendToQueue(id);
			 if(Callisto.databaseConnector.queueCount()==1)
				 Callisto.playNext(EpisodeDesc.this);
			 ((TextView)v).setText("Enqueued");
		  }
    };
    
	OnClickListener launchDelete = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 target.delete();
				streamButton.setText("Stream");
				streamButton.setOnClickListener(launchStream);
				downloadButton.setText("Download");
				downloadButton.setOnClickListener(launchDownload);
		  }
    };
	
	OnClickListener launchStream = new OnClickListener() 
    {
		@Override
		  public void onClick(View v) 
		  {
				EpisodeDesc.this.setProgressBarIndeterminateVisibility(true);
				if(Callisto.player!=null && Callisto.player.isPlaying())
  			  		Callisto.player.stop();
				try {
					Callisto.player = new MediaPlayer();
					Callisto.player.setDataSource(media_link);
					Callisto.player.setOnPreparedListener(EpisodeDesc.this); 
					Callisto.player.prepareAsync();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
    };
	OnClickListener launchDownload = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			//http://www.androidsnippets.com/download-an-http-file-to-sdcard-with-progress-notification
			 	Callisto.downloading_count++;
			 	Callisto.download_queue.add(EpisodeDesc.this.id);
			 	System.out.println("dqueue = " + Callisto.download_queue.size());
			 	
			 	if(Callisto.download_queue.size()==1)
					try {
						(new UpdateProgressTask().execute(media_link)).get();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		}
    };
    // performs database query outside GUI thread
    private class UpdateProgressTask extends AsyncTask<String, Object, Boolean> 
    {
    	
    	String Title, Date, Link, Show;
       @Override
       protected Boolean doInBackground(String... params)
       {
			int TIMEOUT_CONNECTION = 5000;//5sec
			int TIMEOUT_SOCKET = 30000;//30sec
			Callisto.notification = new Notification(R.drawable.callisto, "Beginning download", System.currentTimeMillis());
			Callisto.notification.flags = Notification.FLAG_ONGOING_EVENT;
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	    Cursor current;
	       try
	       {
	    	   while(!Callisto.download_queue.isEmpty())
	    	   {
	    		   		Callisto.databaseConnector.open();
	    		   		long id = Callisto.download_queue.get(0);
						current = Callisto.databaseConnector.getOneEpisode(id);
						current.moveToFirst();
						
						Link = current.getString(current.getColumnIndex("mp3link"));
						
						Title = current.getString(current.getColumnIndex("title"));
	
						Date = current.getString(current.getColumnIndex("date"));
						
						Show = current.getString(current.getColumnIndex("show"));
						Callisto.databaseConnector.close();
						
						
			           URL url = new URL(Link);
			
			           //Open a connection to that URL.
			           URLConnection ucon = url.openConnection();
			
			           //this timeout affects how long it takes for the app to realize there's a connection problem
			           ucon.setReadTimeout(TIMEOUT_CONNECTION);
			           ucon.setConnectTimeout(TIMEOUT_SOCKET);
			
			           
			           SimpleDateFormat sdfDestination = new SimpleDateFormat("yyyy-MM-dd");
			           
			           Date = sdfDestination.format(Callisto.sdfRaw.parse(Date));
			           
			
			           //Define InputStreams to read from the URLConnection.
			           // uses 3KB download buffer
			           is = ucon.getInputStream();
			           BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
						File target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + Show);
						target.mkdirs();
						if(title.indexOf("|")>0)
							Title = Title.substring(0, title.indexOf("|"));
						Title=Title.trim();
						target = new File(target,Date + "__" + Title + ".mp3");
			           outStream = new FileOutputStream(target);
			           buff = new byte[5 * 1024];
			
			
			           totalSize = ucon.getContentLength();
			    	   int len;
			    	   int downloadedSize = 0;
			    	   long perc = 0;
			
			    	   
			    	   //Here is where the actual downloading happens
			           while ((len = inStream.read(buff)) != -1)
			           {
						outStream.write(buff,0,len);
			            downloadedSize += len;
				       	perc = downloadedSize*100;
				       	perc /= totalSize;
				       	Intent notificationIntent = new Intent(EpisodeDesc.this, EpisodeDesc.class);
				   		PendingIntent contentIntent = PendingIntent.getActivity(EpisodeDesc.this, 0, notificationIntent, 0);
				       	Callisto.notification.setLatestEventInfo(getApplicationContext(), "Downloading file " + Callisto.current_download + " of " + Callisto.downloading_count + ": " + perc + "%", Show + ": " + Title, contentIntent);
				       	mNotificationManager.notify(1, Callisto.notification);
			           }
			           Callisto.current_download++;
			           
			           
			           //clean up
			           outStream.flush();
			           outStream.close();
			           inStream.close();
			           
			           Callisto.download_queue.remove(0);
	    	   }
		       } catch (IOException e) {
		    	   //TODO Auto-generated catch block
		    	   e.printStackTrace();
		    	   return false;
		       } catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
		       }
		       Intent notificationIntent = new Intent(EpisodeDesc.this, EpisodeDesc.class);
		       PendingIntent contentIntent = PendingIntent.getActivity(EpisodeDesc.this, 0, notificationIntent, 0);
		       Callisto.notification.setLatestEventInfo(getApplicationContext(), "Callisto", "Finished downloading " + Callisto.downloading_count + " files", contentIntent); //DEBUG
		   		mNotificationManager.notify(1, Callisto.notification);
		   		Callisto.notification.flags = Notification.FLAG_AUTO_CANCEL;
		        Callisto.current_download=1;
		        Callisto.downloading_count=0;
	       return true;
       }
       
       @Override
       protected void onPostExecute(Boolean result)
       {
    	   if(result)
    	   {
				streamButton.setText("Play");
				streamButton.setOnClickListener(launchPlay);
				downloadButton.setText("Delete");
				downloadButton.setOnClickListener(launchDelete);
    	   } else
    	   {
	   			streamButton.setText("Stream");
	   			streamButton.setOnClickListener(launchStream);
	   			downloadButton.setText("Download");
	   			downloadButton.setOnClickListener(launchDownload);
    	   }
       }
    }
    
    @Override
    public void onPrepared(MediaPlayer mp) {  
    	mediaFileLengthInMilliseconds = Callisto.player.getDuration();
		System.out.println("Length: " + mediaFileLengthInMilliseconds/1000);
		Callisto.player.start();
		EpisodeDesc.this.setProgressBarIndeterminateVisibility(false);
    }
}
