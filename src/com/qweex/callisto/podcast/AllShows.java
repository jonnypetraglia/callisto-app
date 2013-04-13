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
package com.qweex.callisto.podcast;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//IDEA: Change method of having a header in the listview; Using separate layout files for the heading causes it to have to inflate the view every time rather than just update it
//FIXME: Scrolling in the AllShows view while something is playing is jerky

/** An activity to list all the shows on JupiterBroadcasting. 
 * It was designed to work with both audio and video, but the video needs some definite tweaking.
 * @author MrQweex
 */

public class AllShows extends Activity {
	
	/** A boolean value as to if the user has selected video instead of audio. */
	public static boolean IS_VIDEO;
	/** A static array containing corresponding info for the shows.
	 *  A sub-heading will start with a space in the name list and have a value of null in the feeds.
	 */
	public static final String[] SHOW_LIST = Callisto.RESOURCES.getStringArray(R.array.shows);
	public static final String[] SHOW_LIST_AUDIO = Callisto.RESOURCES.getStringArray(R.array.shows_audio);
	public static final String[] SHOW_LIST_VIDEO = Callisto.RESOURCES.getStringArray(R.array.shows_video);
	
	//-----Local Variables-----
	private final int STOP_ID = Menu.FIRST + 1;
	private final int DOWNLOADS_ID = STOP_ID+1;
	private final int UPDATE_ID = DOWNLOADS_ID+1;
	private static ListView mainListView; 		 //Static so that it can be used in a static handler
	private static HeaderAdapter listAdapter ; //Static so that it can be used in a static handler
	private View current_view = null;			 //This is for adjusting the date on a show in OnResume
	private SharedPreferences current_showSettings;
	
	/** Called when the activity is first created. Sets up the view.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 */
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

        List<HeaderAdapter.Item> headerThings = new ArrayList<HeaderAdapter.Item>();
        for(int i=0; i<SHOW_LIST.length; i++)
        {
            if(SHOW_LIST[i].startsWith(" "))
                headerThings.add(new AllShowsHeader());
            else
                headerThings.add(new AllShowsRow());
        }

        listAdapter = new HeaderAdapter(this, headerThings);
        mainListView.setAdapter(listAdapter);
		mainListView.setBackgroundColor(getResources().getColor(R.color.backClr));
		mainListView.setCacheColorHint(getResources().getColor(R.color.backClr));

		
		IS_VIDEO = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("is_video", false);
		if(IS_VIDEO)
			this.setTitle(getResources().getString(R.string.watch));
		else
			this.setTitle(getResources().getString(R.string.listen));
	}
	
	/** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
	@Override
	public void onResume()
	{
		super.onResume();
		Log.v("AllShows:onResume", "Resuming AllShows");
		setProgressBarIndeterminateVisibility(false);
		Callisto.playerInfo.update(AllShows.this);
		
		if(current_view==null)
			return;
		TextView current_textview = (TextView)current_view.findViewById(R.id.rowTextView);
		if(current_textview==null)
			return;

		String the_current_show = (String)(current_textview).getText();


        int i = Callisto.databaseConnector.getShow(the_current_show, true).getCount();
        ((TextView)current_view.findViewById(R.id.showUnwatched)).setTextColor((i>0 ? 0xff000000 : 0x11000000) + Callisto.RESOURCES.getColor(R.color.txtClr));
        ((TextView)current_view.findViewById(R.id.showUnwatched)).setText(Integer.toString(i));

        //Updated the "last_checked"
		SharedPreferences showSettings = getSharedPreferences(the_current_show, 0);
	   	
	   	String lastChecked = showSettings.getString("last_checked", null);
		Log.v("AllShows:onResume", "Resuming after:" + the_current_show + "| " + lastChecked);
		if(lastChecked!=null)
			try {
				lastChecked = Callisto.sdfDestination.format(Callisto.sdfSource.parse(lastChecked));
				((TextView)current_view.findViewById(R.id.rowSubTextView)).setText(lastChecked);
			} catch (ParseException e) {
				Log.e("AllShows:OnResume:ParseException", "Error parsing a date from the SharedPreferences..");
				Log.e("AllShows:OnResume:ParseException", lastChecked);
				Log.e("AllShows:OnResume:ParseException", "(This should never happen).");
			}

        //Done away with in favor of numbers
        /*
		if(Callisto.databaseConnector.getShow(the_current_show, true).getCount()==0)
			current_view.setBackgroundDrawable(Callisto.RESOURCES.getDrawable(android.R.drawable.list_selector_background));
		else
			current_view.setBackgroundResource(R.drawable.main_colored);
        */
			
		
		current_view=null;
	}
	
	/** Called when a search is requested.
	 * @return true if the search was handled, false otherwise
	 */
	@Override
	public boolean onSearchRequested ()
	{
		SearchResultsActivity.searchShow = "";
		startSearch(null, false, null, false);
		return true;
	}
	
	/** Currently not used */
	@Override
	public void onStop()
	{
		//TODO: OnStop
		super.onStop();
	}
	
	/** Called when it is time to create the menu.
	 * @param menu Um, the menu
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	menu.add(0, STOP_ID, 0, Callisto.RESOURCES.getString(R.string.stop)).setIcon(R.drawable.ic_action_playback_stop);
    	//android.graphics.drawable.Drawable DownloadIcon = getResources().getDrawable(R.drawable.ic_menu_forward);
    	
    	android.graphics.drawable.Drawable DownloadIcon = getResources().getDrawable(R.drawable.ic_action_download);
	
    	
    	menu.add(0, DOWNLOADS_ID, 0, getResources().getString(R.string.downloads)).setIcon(DownloadIcon);
    	menu.add(0, UPDATE_ID, 0, getResources().getString(R.string.refresh_all)).setIcon(R.drawable.ic_action_reload);
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
        case DOWNLOADS_ID:
        	Intent newIntent = new Intent(AllShows.this, DownloadList.class);
        	startActivity(newIntent);
            return true;
        case UPDATE_ID:
        	UpdateAllHandler.sendMessage(new Message());
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    Handler UpdateAllHandler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {  	new UpdateAllShowsTask().execute((Object[]) null);   }
	};
    
    /** Updates the show outside of the UI thread. */
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
           boolean done = false,
                   skip_inactive = PreferenceManager.getDefaultSharedPreferences(AllShows.this).getBoolean("download_to_queue", false);
            for(int i=0; i<SHOW_LIST_AUDIO.length; i++)
            {
                current_view = mainListView.getChildAt(i);
                if(SHOW_LIST_AUDIO[i]==null)
                {
                    if(done && skip_inactive)
                        break;
                    done = true;
                    continue;
                }
                current_showSettings = getSharedPreferences(AllShows.SHOW_LIST[i], 0);
                Callisto.updateShow(i, current_showSettings);
                UpdateHandler.sendEmptyMessage(0);
            }

            return null;
        }
       
       Handler UpdateHandler = new Handler() {
			@Override 
			public void handleMessage(Message msg)
			{ mainListView.setAdapter(listAdapter);	}
		};
        
       @Override
       protected void onPostExecute(Object result)
       {
    	   Toast.makeText(AllShows.this, Callisto.RESOURCES.getString(R.string.finished_update), Toast.LENGTH_SHORT).show();
    	   setProgressBarIndeterminateVisibility(false);
       }
    }
    
	
	/** Listener for when a show is selected. */
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



    /***************************** Header *****************************/
    public class AllShowsHeader implements HeaderAdapter.Item
    {
        @Override
        public int getViewType()
        {
            return HeaderAdapter.RowType.HEADER_ITEM.ordinal();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View row = convertView;
            if(row == null)
            {
                LayoutInflater inflater=getLayoutInflater();
                row = (View) inflater.inflate(R.layout.main_row_head, parent, false);
            }

            TextView x = ((TextView)row.findViewById(R.id.heading));
            x.setText(AllShows.SHOW_LIST[position]);
            x.setFocusable(false);
            x.setEnabled(false);
            return row;
        }
    }

    public class AllShowsRow implements HeaderAdapter.Item
    {
        @Override
        public int getViewType()
        {
            return HeaderAdapter.RowType.LIST_ITEM.ordinal();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View row = convertView;
            if(row == null)
            {
                LayoutInflater inflater=getLayoutInflater();
                row = (View) inflater.inflate(R.layout.main_row_head, parent, false);
            }

            if(row==null || row.findViewById(R.id.showUnwatched)==null)
            {
                LayoutInflater inflater=getLayoutInflater();
                row=inflater.inflate(R.layout.main_row, parent, false);
            }

            //Get the show icon
            String[] exts = {".jpg", ".gif", ".png"};	//Technically, this can be removed since the images are all shrunken and re-compressed to JPGs when they are downloaded
            File f;

            for(String ext : exts)
            {
                f = new File(Environment.getExternalStorageDirectory() + File.separator +
                        Callisto.storage_path + File.separator +
                        AllShows.SHOW_LIST[position] + ext);
                if(f.exists())
                {
                    Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
                    ImageView img = (ImageView) row.findViewById(R.id.img);
                    img.setImageBitmap(bitmap);
                    break;
                }
            }

            int i = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[position], true).getCount();
            ((TextView)row.findViewById(R.id.showUnwatched)).setTextColor((i>0 ? 0xff000000 : 0x11000000) + Callisto.RESOURCES.getColor(R.color.txtClr));
            ((TextView)row.findViewById(R.id.showUnwatched)).setText(Integer.toString(i));

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
            return row;
        }
    }
}
