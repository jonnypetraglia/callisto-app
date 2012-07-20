/*
 * Copyright (C) 2012 Qweex
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qweex.callisto;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class AllShows extends Activity {
	
	public static boolean IS_VIDEO = false;
	public static View current_view = null;
	
	public static final String[] SHOW_LIST = new String[]
			{
			" Active Shows",
				"Coder Radio",
				"Linux Action Show",
				"SciByte",
				"TechSnap",
				"Unfilter",
				"FauxShow",
			" Inactive Shows ",
				"STOked",
				"TORKed",
				"Jupiter@Nite",
				"In Depth Look",
				"MMOrgue",
				"Jupiter Files",
				"Beer is Tasty",
			" Live",
				"Jupiter Radio"
			};
	public static final String[] SHOW_LIST_AUDIO = new String[]
			{
				null,
				"http://feeds.feedburner.com/coderradiomp3",
				"http://feeds2.feedburner.com/TheLinuxActionShow",
				"http://feeds.feedburner.com/scibyteaudio",
				"http://feeds.feedburner.com/techsnapmp3",
				"http://www.jupiterbroadcasting.com/feeds/unfilterMP3.xml",
				"http://www.jupiterbroadcasting.com/feeds/FauxShowMP3.xml",
				null,
				"http://feeds.feedburner.com/TorkedMp3",
				"http://feeds.feedburner.com/stoked",
				"http://feeds.feedburner.com/jupiternitemp3",
				"http://www.jupiterbroadcasting.com/feeds/indepthlookmp3.xml",
				"http://feeds.feedburner.com/MMOrgueMP3",
				"http://feeds.feedburner.com/ldf-mp3",
				"",
				null,
				"http://jblive.am"
			};
	public static final String[] SHOW_LIST_VIDEO = new String[]
			{
				null,
				"http://feeds.feedburner.com/coderradiovideo",
				"http://feeds.feedburner.com/linuxactionshowipodvid",
				"http://feeds.feedburner.com/scibytemobile",
				"http://feeds.feedburner.com/techsnapmobile",
				"http://www.jupiterbroadcasting.com/feeds/unfilterMob.xml",
				"http://www.jupiterbroadcasting.com/feeds/FauxShowMobile.xml",
				null,
				"http://feeds.feedburner.com/TorkedMobile",
				"http://feeds.feedburner.com/stokedipod",
				"http://feeds.feedburner.com/jupiterniteivid",
				"http://www.jupiterbroadcasting.com/feeds/indepthlookmob.xml",
				"http://feeds.feedburner.com/MMOrgueMobile",
				"http://feeds.feedburner.com/ldf-video",
				"http://feeds2.feedburner.com/BeerIsTasty",
				null,
				"http://jblive.fm"
			};
	
	private ListView mainListView;
	private AllShowsAdapter listAdapter ;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		IS_VIDEO=getIntent().getExtras().getBoolean("screen");
		if(IS_VIDEO)
			this.setTitle("All Video");
		else
			this.setTitle("All Audio");
		
		mainListView = new ListView(this);
		Callisto.build_layout(this, mainListView);
		
		
		mainListView.setOnItemClickListener(selectShow);
		listAdapter = new AllShowsAdapter(this, R.layout.main_row, SHOW_LIST); 
		mainListView.setAdapter(listAdapter);
	}
	@Override
	public void onResume()
	{
		super.onResume();
		setProgressBarIndeterminateVisibility(false);
		
		if(current_view==null)
			return;
		String the_current = (String)((TextView)current_view.findViewById(R.id.rowTextView)).getText(); 
	    SharedPreferences showSettings = getSharedPreferences(the_current, 0);
		String lastChecked = showSettings.getString("last_checked", null);
		try {
			lastChecked = Callisto.sdfDestination.format(Callisto.sdfSource.parse(lastChecked));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		((TextView)current_view.findViewById(R.id.rowSubTextView)).setText(lastChecked);
		current_view=null;
	}
	@Override
	public void onStop()
	{
		super.onStop();
	}
	
	OnItemClickListener selectShow = new OnItemClickListener() 
    {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
      {
    	  if(position==SHOW_LIST.length-1)
    	  {
    		  setProgressBarIndeterminateVisibility(true);
    		  if(Callisto.player!=null && Callisto.player.isPlaying())
    			  Callisto.player.stop();
    		  Callisto.player = MediaPlayer.create(AllShows.this, Uri.parse(SHOW_LIST_AUDIO[SHOW_LIST_AUDIO.length-1]));
    		  Callisto.player.start();
    	  }
    	  else if(SHOW_LIST[position].charAt(0)==' ')
    		  return;
    	  else
    	  {
    		  current_view = view;
	    	  Intent viewShow = new Intent(AllShows.this, ShowList.class);
	    	  viewShow.putExtra("current_show", position);
	    	  setProgressBarIndeterminateVisibility(true);
	    	  startActivity(viewShow);
    	  }
      }
    };
    
    public class AllShowsAdapter extends ArrayAdapter<String>
    {
    	String[] thingy;
    	public AllShowsAdapter(Context context, int textViewResourceId, String[] objects)
    	{
			super(context, textViewResourceId, objects);
			thingy = objects;
			// TODO Auto-generated constructor stub
		}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent)
    	{
	    	// TODO Auto-generated method stub
	    	//return super.getView(position, convertView, parent);
    		
    		LayoutInflater inflater=getLayoutInflater();
    		View row = null;
    		if(AllShows.SHOW_LIST_AUDIO[position]==null)
    		{
    			row=inflater.inflate(R.layout.main_row_head, parent, false);
    			TextView x = ((TextView)row.findViewById(R.id.heading));
    			x.setText(AllShows.SHOW_LIST[position]);
    			x.setFocusable(false);
    			x.setEnabled(false);
    		} else
    		{
		    	row=inflater.inflate(R.layout.main_row, parent, false);
		    	
				((TextView)row.findViewById(R.id.rowTextView)).setText(AllShows.SHOW_LIST[position]);
		    	SharedPreferences showSettings = getSharedPreferences(AllShows.SHOW_LIST[position], 0);
				String lastChecked = showSettings.getString("last_checked", null);
				if(lastChecked!=null)
				{
					try {
						lastChecked = Callisto.sdfDestination.format(Callisto.sdfSource.parse(lastChecked));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					((TextView)row.findViewById(R.id.rowSubTextView)).setText(lastChecked);
				}
	
				
				
		    	/*
		    	ImageView icon=(ImageView)row.findViewById(R.id.icon);
		
		    	
		    	if (DayOfWeek[position]=="Sunday"){
		    	icon.setImageResource(R.drawable.icon);
		    	}
		    	else{
		    	icon.setImageResource(R.drawable.icongray);
		    	}
		    	*/
	
    		}
    		return row;
    	}
    }
    
}
