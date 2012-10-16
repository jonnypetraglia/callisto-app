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

import java.io.File;
import java.text.ParseException;

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
	private static AllShowsAdapter listAdapter ; //Static so that it can be used in a static handler
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
		listAdapter = new AllShowsAdapter(this, R.layout.main_row, SHOW_LIST);
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
		if(Callisto.databaseConnector.getShow(the_current, true).getCount()==0)
			current_view.setBackgroundDrawable(Callisto.RESOURCES.getDrawable(android.R.drawable.list_selector_background));
		else
			current_view.setBackgroundResource(R.drawable.main_colored);
			
		
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
    	menu.add(0, STOP_ID, 0, Callisto.RESOURCES.getString(R.string.stop)).setIcon(R.drawable.stop);
    	menu.add(0, DOWNLOADS_ID, 0, getResources().getString(R.string.downloads)).setIcon(R.drawable.ic_menu_forward);
    	menu.add(0, UPDATE_ID, 0, getResources().getString(R.string.refresh_all)).setIcon(R.drawable.ic_menu_refresh);
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
       	for(int i=0; i<SHOW_LIST_AUDIO.length; i++)
       	{
       		current_view = mainListView.getChildAt(i);
       		if(SHOW_LIST_AUDIO[i]==null)
       			continue;
       		current_showSettings = getSharedPreferences(AllShows.SHOW_LIST[i], 0);
			Callisto.updateShow(i, current_showSettings, false);
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
    
    /** Adapter for this class's ListView. Extended because the date needs to be formatted. */
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
				
				
				if( Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[position], true).getCount()==0 )
					row.setBackgroundDrawable(Callisto.RESOURCES.getDrawable(android.R.drawable.list_selector_background));
				else
					row.setBackgroundResource(R.drawable.main_colored);
					
	
    		}
    		return row;
    	}
    }
    
}
