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

import java.text.ParseException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//This activity lists all the shows on JupiterBroadcasting
// It was designed to work with both audio and video, but the video needs some definite tweaking
//FEATURE: a way to update the list without updating the app?
//FEATURE: maybe an icon next to each show?
//FEATURE: "has new episodes" icon
//FEATURE: Search
//IDEA: Change method of having a header in the listview; Using separate layout files for the heading causes it to have to inflate the view every time rather than just update it

public class AllShows extends Activity {
	
	public static boolean IS_VIDEO = false;
	
	//These are static arrays containing the show names and corresponding feed URLS
	// A sub-heading will start with a space in the name list and have a value of null in the feeds
	
	public static final String[] SHOW_LIST = Callisto.RESOURCES.getStringArray(R.array.shows);
	public static final String[] SHOW_LIST_AUDIO = Callisto.RESOURCES.getStringArray(R.array.shows_audio);
	public static final String[] SHOW_LIST_VIDEO = Callisto.RESOURCES.getStringArray(R.array.shows_video);
	
	//-----Local Variables-----
	private static final int DOWNLOADS_ID = Menu.FIRST+1;
	private static final int UPDATE_ID = DOWNLOADS_ID+1;
	private static ListView mainListView;
	private static AllShowsAdapter listAdapter ;
	private static View current_view = null;		//This is for adjusting the date on a show in OnResume
	private static SharedPreferences current_showSettings;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		Log.v("AllShows:OnCreate", "Launching Activity");
		
		mainListView = new ListView(this);
		Callisto.build_layout(this, mainListView);
		TextView loading = (TextView) findViewById(android.R.id.empty);
	    loading.setText(getResources().getString(R.string.loading));
	    loading.setGravity(Gravity.CENTER_HORIZONTAL);
	    mainListView.setEmptyView(loading);
		mainListView.setOnItemClickListener(selectShow);
		listAdapter = new AllShowsAdapter(this, R.layout.main_row, SHOW_LIST); 
		mainListView.setAdapter(listAdapter);
		mainListView.setBackgroundColor(getResources().getColor(R.color.backClr));
		mainListView.setCacheColorHint(getResources().getColor(R.color.backClr));
		
		//IS_VIDEO=getIntent().getExtras().getBoolean("is_video");
		IS_VIDEO = false; //IDEA: add watch
		if(IS_VIDEO)
			this.setTitle(getResources().getString(R.string.watch));
		else
			this.setTitle(getResources().getString(R.string.listen));
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		setProgressBarIndeterminateVisibility(false);
		Callisto.playerInfo.update(AllShows.this);
		
		if(current_view==null)
			return;
		TextView current_textview = (TextView)current_view.findViewById(R.id.rowTextView);
		if(current_textview==null)
			return;
		String the_current = (String)(current_textview).getText();
		SharedPreferences showSettings = getSharedPreferences(the_current, 0);
	   	
	   	String lastChecked = showSettings.getString("last_checked", null);
		Log.v("AllShows:onResume", "Resuming after:" + the_current + "| " + lastChecked);
		if(lastChecked!=null)
			try {
				lastChecked = Callisto.sdfDestination.format(Callisto.sdfSource.parse(lastChecked));
				((TextView)current_view.findViewById(R.id.rowSubTextView)).setText(lastChecked);
			} catch (ParseException e) {
				Log.e("AllShows:OnResume:ParseException", "Error parsing a date from the SharedPreferences..");
				Log.e("AllShows:OnResume:ParseException", lastChecked);
				Log.e("AllShows:OnResume:ParseException", "(This should never happen).");
			}
		current_view=null;
	}
	
	@Override
	public void onStop()
	{
		//TODO: OnStop
		super.onStop();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	menu.add(0, DOWNLOADS_ID, 0, getResources().getString(R.string.downloads)).setIcon(R.drawable.ic_menu_forward);
    	menu.add(0, UPDATE_ID, 0, getResources().getString(R.string.refresh_all)).setIcon(R.drawable.ic_menu_refresh);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
 
        switch (item.getItemId())
        {
        case DOWNLOADS_ID:
        	Intent newIntent = new Intent(AllShows.this, DownloadList.class);
        	startActivity(newIntent);
            return true;
        case UPDATE_ID:
        	updateAllHandler.sendMessage(new Message());
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    
    Handler updateAllHandler = new Handler()
	{
        @Override
        public void handleMessage(Message msg)
        {
        	new UpdateAllShowsTask().execute((Object[]) null);
        }
	};
	
	

	
	

	
    
    // Updates the show outside of the UI thread
    private class UpdateAllShowsTask extends AsyncTask<Object, Object, Object> 
    {
    	@Override
    	protected void onPreExecute()
    	{
    		Toast.makeText(AllShows.this, Callisto.RESOURCES.getString(R.string.beginning_update), Toast.LENGTH_SHORT).show();
    		setProgressBarIndeterminateVisibility(true);
    	}
    	
       @Override
       protected Object doInBackground(Object... params)
       {
       	for(int i=0; i<SHOW_LIST_AUDIO.length; i++)
       	{
       		current_view = mainListView.getChildAt(i);
       		if(SHOW_LIST_AUDIO[i]==null)
       			continue;
       		current_showSettings = getSharedPreferences(AllShows.SHOW_LIST[i], 0);
			Callisto.updateShow(i, current_showSettings, false);
			updateTextHandler.sendEmptyMessage(0);
       	}
       	
       	return null;
        }
        
       @Override
       protected void onPostExecute(Object result)
       {
    	   Toast.makeText(AllShows.this, Callisto.RESOURCES.getString(R.string.finished_update), Toast.LENGTH_SHORT).show();
    	   setProgressBarIndeterminateVisibility(false);
       }
    }
    
	public static Handler updateTextHandler = new Handler()
	{
		@Override 
		public void handleMessage(Message msg)
		{ 
			mainListView.setAdapter(listAdapter);
		}
	};
    
    
    
	
	//Listener for when a show is selected
	OnItemClickListener selectShow = new OnItemClickListener() 
    {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
      {
    	  Log.d("AllShows:selectShow", "Selected show at position: " + position);
    	  if(SHOW_LIST[position].charAt(0)==' ')
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
    
    //Adapter for this view; extended because we need to format the date
    public class AllShowsAdapter extends ArrayAdapter<String>
    {
    	public AllShowsAdapter(Context context, int textViewResourceId, String[] objects)
    	{
			super(context, textViewResourceId, objects);
		}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent)
    	{
    		View row = convertView;
    		if(AllShows.SHOW_LIST_AUDIO[position]==null)
    		{
    			//if(row==null)
    			//{	
    				LayoutInflater inflater=getLayoutInflater();
    				row=inflater.inflate(R.layout.main_row_head, parent, false);
    			//}
    			TextView x = ((TextView)row.findViewById(R.id.heading));
    			x.setText(AllShows.SHOW_LIST[position]);
    			x.setFocusable(false);
    			x.setEnabled(false);
    		} else
    		{
    			//if(row==null)
    			//{	
    				LayoutInflater inflater=getLayoutInflater();
    				row=inflater.inflate(R.layout.main_row, parent, false);
    			//}
		    	
				((TextView)row.findViewById(R.id.rowTextView)).setText(AllShows.SHOW_LIST[position]);
		    	SharedPreferences showSettings = getSharedPreferences(AllShows.SHOW_LIST[position], 0);
				String lastChecked = showSettings.getString("last_checked", null);
				if(lastChecked!=null)
				{
					try {
						lastChecked = Callisto.sdfDestination.format(Callisto.sdfSource.parse(lastChecked));
					} catch (ParseException e) {
						Log.e("AllShows:AllShowsAdapter:ParseException", "Error parsing a date from the SharedPreferences..");
						Log.e("AllShows:AllShowsAdapter:ParseException", lastChecked);
						Log.e("AllShows:AllShowsAdapter:ParseException", "(This should never happen).");
						e.printStackTrace();
					}
					((TextView)row.findViewById(R.id.rowSubTextView)).setText(lastChecked);
				}
	
    		}
    		return row;
    	}
    }
    
}
