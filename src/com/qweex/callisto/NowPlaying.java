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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

//FEATURE: All of it

public class NowPlaying extends Activity
{
	private final String infoURL = "http://jbradio.airtime.pro/api/live-info";
	Matcher m = null;
    String currentShow = "Unknown", nextShow = "Unknown";
    TextView current, next;
    ImageButton playPause;
    String live_url;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nowplaying);
		Callisto.live_player = new MediaPlayer();
		
		current = (TextView) findViewById(R.id.current);
		next = (TextView) findViewById(R.id.next);
		playPause = (ImageButton) findViewById(R.id.playPause);
		playPause.setOnClickListener(play);
	}
	
	public void update()
    {
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpContext localContext = new BasicHttpContext();
	    HttpGet httpGet = new HttpGet(infoURL);
	    HttpResponse response;
	    try
		{
			response = httpClient.execute(httpGet, localContext);
		    BufferedReader reader = new BufferedReader(
			        new InputStreamReader(
			          response.getEntity().getContent()
			        )
			      );
		    
		    String line = null, result = "";
		    while ((line = reader.readLine()) != null){
		      result += line + "\n";
		    }
		    
		    m = (Pattern.compile(".*?\"currentShow\".*?"
		    					+ "\"name\":\"(.*?)\""
		    					+ ".*"
								+ "\"name\":\"(.*?)\""
		    					+ ".*?")
		    					).matcher(result);
		    if(m.find())
		    	currentShow = m.group(1);
		    if(m.groupCount()>1)
		    	nextShow = m.group(2);
	    	
		    current.setText(currentShow);
		    next.setText(nextShow);
		    
		} catch (ClientProtocolException e) {
			// TODO EXCEPTION
			e.printStackTrace();
		} catch (IOException e) {
			// TODO EXCEPTION
			e.printStackTrace();
		}
	}
	
	OnClickListener play = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			System.out.println("BUTTS play");
			Callisto.live_player.setOnPreparedListener(new OnPreparedListener()
			{
				@Override
				public void onPrepared(MediaPlayer arg0) {
					System.out.println("BUTTS prepared");
					update();
					System.out.println("BUTTS updated");
					//playPause.setOnClickListener(pause);
					Callisto.live_player.start();
					Callisto.playerInfo.isPaused = false;
				}
			});
			try {
				live_url = "http://www.jblive.am";
				Callisto.live_player.setDataSource(live_url);
				//Show waiting dialog
				Callisto.live_player.prepareAsync();
			} catch (IllegalArgumentException e) {
				// TODO EXCEPTION
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
	
	@Override
	public void onResume()
	{
		super.onResume();
		live_url = PreferenceManager.getDefaultSharedPreferences(NowPlaying.this).getString("live_url", "jblive.am");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Callisto.live_player.reset();
		Callisto.live_player = null;
	}
}
