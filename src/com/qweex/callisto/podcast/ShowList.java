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
import java.util.Date;

import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/** An activity for displaying the episodes of a show.
 * @author MrQweex */


public class ShowList extends Activity
{
	public static int currentShow = -1;			//This number is taken from AllShows
    public static View current_episode = null;		//This is for adjusting the status of an episode in OnResume
    
    //-----Local Variables-----
    private String feedURL;
    private ListView mainListView = null;
    private CursorAdapter showAdapter;
	private static final int STOP_ID=Menu.FIRST+1;
	private static final int RELOAD_ID=STOP_ID+1;
	private static final int CLEAR_ID=RELOAD_ID+1;
	private static final int FILTER_ID=CLEAR_ID+1;
	private static final int MARK_ID = FILTER_ID+1;
	
	private static TextView loading;
	private static Button refresh;
	private boolean filter;
	public SharedPreferences showSettings;
	
	/** Called when the activity is first created. Sets up the view.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 *  */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	    Log.v("ShowList:OnCreate", "Launching Activity");
	    mainListView = new ListView(this);
	    Callisto.build_layout(this, mainListView);
	    
	    //if(currentShow==-1)	//I don't know why I put this here.
	    	currentShow = getIntent().getExtras().getInt("current_show");
	    showSettings = getSharedPreferences(AllShows.SHOW_LIST[currentShow], 0);
		filter = showSettings.getBoolean("filter", false);
		loading = (TextView) findViewById(android.R.id.empty);
		if(Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter).getCount()==0)
			loading.setText(Callisto.RESOURCES.getString(R.string.list_empty));
		else
			loading.setText(Callisto.RESOURCES.getString(R.string.loading));
		loading.setGravity(Gravity.CENTER_HORIZONTAL);
		if(showSettings.getString("last_checked", null)==null)
		{
			refresh = new Button(this);
			refresh.setText(Callisto.RESOURCES.getString(R.string.refresh));
			refresh.setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
			LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			p.setMargins(20,10,20,10);
			refresh.setLayoutParams(p);
			((LinearLayout)loading.getParent()).setGravity(Gravity.CENTER_HORIZONTAL);
			refresh.setPadding(100, 10, 100, 10);
			refresh.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					reload();
				}
			});
			((LinearLayout)mainListView.getParent()).addView(refresh,1);
			((LinearLayout)mainListView.getParent()).setBackgroundColor(Callisto.RESOURCES.getColor(R.color.backClr));
			mainListView.setEmptyView(refresh);
			loading.setVisibility(View.INVISIBLE);
		}
		else
		{
			mainListView.setEmptyView(loading);
		}
	    mainListView.setOnItemClickListener(selectEpisode);
	    mainListView.setBackgroundColor(getResources().getColor(R.color.backClr));
	    mainListView.setCacheColorHint(getResources().getColor(R.color.backClr));
	    
	    setTitle(AllShows.SHOW_LIST[currentShow]);
	    if(AllShows.IS_VIDEO)
	    	feedURL = AllShows.SHOW_LIST_VIDEO[currentShow];
	    else
	    	feedURL = AllShows.SHOW_LIST_AUDIO[currentShow];	    
	    
	    new GetShowTask().execute((Object[]) null);
	    Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
	    showAdapter = new ShowListCursorAdapter(ShowList.this, R.layout.row, r);
	    mainListView.setAdapter(showAdapter);
		return;
	}
	
	/** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
	@Override
	public void onResume()
	{
		super.onResume();
		setProgressBarIndeterminateVisibility(false);
		Callisto.playerInfo.update(ShowList.this);
		
		if(current_episode==null)
			return;
		
		
		long id = Long.parseLong(
				(String) ((TextView)((View) current_episode).findViewById(R.id.hiddenId)).getText()
				);
		Cursor c = Callisto.databaseConnector.getOneEpisode(id);
		c.moveToFirst();
		boolean is_new = c.getInt(c.getColumnIndex("new"))>0;
		((CheckBox)((View) current_episode).findViewById(R.id.img)).setChecked(is_new);
		current_episode=null;
	}
	
	/** Called when a search is requested.
	 * @return true if the search was handled, false otherwise
	 */
	@Override
	public boolean onSearchRequested ()
	{
		SearchResultsActivity.searchShow = AllShows.SHOW_LIST[currentShow];
		startSearch(null, false, null, false);
		return true;
	}
	
	/** Not Current Used */
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
    	menu.add(0, STOP_ID, 0, Callisto.RESOURCES.getString(R.string.stop)).setIcon(R.drawable.ic_media_stop);
    	menu.add(0, RELOAD_ID, 0, Callisto.RESOURCES.getString(R.string.refresh)).setIcon(R.drawable.ic_menu_refresh);
    	//menu.add(0, CLEAR_ID, 0, Callisto.RESOURCES.getString(R.string.clear)).setIcon(R.drawable.ic_menu_clear_playlist);
    	menu.add(0, FILTER_ID, 0, Callisto.RESOURCES.getString(filter ? R.string.unfilter : R.string.filter)).setIcon(android.R.drawable.ic_menu_zoom);
    	menu.add(0, MARK_ID, 0, Callisto.RESOURCES.getString(R.string.mark)).setIcon(R.drawable.ic_menu_mark);
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
        case RELOAD_ID:
        	reload();
            return true;
        case CLEAR_ID:
        	new AlertDialog.Builder(this)
        	.setTitle(Callisto.RESOURCES.getString(R.string.confirm))
        	.setMessage(Callisto.RESOURCES.getString(R.string.confirm_clear))
        	.setIcon(android.R.drawable.ic_dialog_alert)
        	.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int whichButton) {
                	loading.setText(Callisto.RESOURCES.getString(R.string.list_empty));
                	Callisto.databaseConnector.clearShow(AllShows.SHOW_LIST[currentShow]);
                	Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
            	   	ShowList.this.showAdapter.changeCursor(r);
            	   	ShowList.this.showAdapter.notifyDataSetChanged();
            	   	SharedPreferences.Editor editor = showSettings.edit();
            	   	editor.putString("last_checked", null);
            	   	editor.commit();
        	    }})
        	 .setNegativeButton(android.R.string.no, null).show();
        	return true;
        case FILTER_ID:
        	filter = !filter;
        	if(filter)
        		item.setTitle(Callisto.RESOURCES.getString(R.string.unfilter));
        	else
        		item.setTitle(Callisto.RESOURCES.getString(R.string.filter));
    		Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
        	ShowList.this.showAdapter.changeCursor(r);
    	   	ShowList.this.showAdapter.notifyDataSetChanged();
    	   	SharedPreferences.Editor editor = showSettings.edit();
    	   	editor.putBoolean("filter", filter);
    	   	editor.commit();
        	return true;
        case MARK_ID:
        	new AlertDialog.Builder(this)
        	.setTitle(Callisto.RESOURCES.getString(R.string.mark_all))
        	.setPositiveButton(Callisto.RESOURCES.getString(R.string.new_), new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int whichButton) {
        	    	Callisto.databaseConnector.markAllNew(AllShows.SHOW_LIST[currentShow], true);
                	Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
            	   	ShowList.this.showAdapter.changeCursor(r);
            	   	ShowList.this.showAdapter.notifyDataSetChanged();
        	    }})
        	 .setNegativeButton(Callisto.RESOURCES.getString(R.string.old), new DialogInterface.OnClickListener() {
         	    public void onClick(DialogInterface dialog, int whichButton) {
         	    	Callisto.databaseConnector.markAllNew(AllShows.SHOW_LIST[currentShow], false);
                 	Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
             	   	ShowList.this.showAdapter.changeCursor(r);
             	   	ShowList.this.showAdapter.notifyDataSetChanged();
         	    }}).show();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
    /** Reloads the current show and adds any new episodes to the list. */
    private void reload()
    {
    	if(refresh!=null)
    		refresh.setVisibility(View.GONE);
    	loading.setVisibility(View.VISIBLE);
		mainListView.setEmptyView(loading);
    	loading.setText(Callisto.RESOURCES.getString(R.string.loading));
    	setProgressBarIndeterminateVisibility(true);
    	new UpdateShowTask().execute((Object[]) null);
	    new GetShowTask().execute((Object[]) null);
    }

	/** A handler for changing the cursor after an update; need this to perform tasks on UI elements outside of UI thread. */
	private Handler updateHandler = new Handler()
	{
        @Override
        public void handleMessage(Message msg)
        {
        	if(msg.arg1!=0)
        	{
	    	   	Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
	    	   	ShowList.this.showAdapter.changeCursor(r);
	    	   	ShowList.this.showAdapter.notifyDataSetChanged();
	    	   	Log.i("ShowList:handleMessage", "Changing cursor");
        	}
        	else
        	{
        		Log.i("ShowList:handleMessage", "Not Changing cursor");
        		loading.setText(Callisto.RESOURCES.getString(R.string.list_empty));
        	}
        	//setProgressBarIndeterminateVisibility(false);
		}
	};

	
   /** Updates the show outside of the UI thread. */
   private class UpdateShowTask extends AsyncTask<Object, Object, Object> 
   {
	   @Override
	   protected void onPreExecute()
	   {
		   TextView loading = (TextView) ShowList.this.findViewById(android.R.id.empty);
		   loading.setText(Callisto.RESOURCES.getString(R.string.loading));
	   }
	   
      @Override
      protected Object doInBackground(Object... params)
      {
		  return Callisto.updateShow(currentShow, showSettings, AllShows.IS_VIDEO);
      }
      
      @Override
      protected void onPostExecute(Object result)
      {
    	  TextView loading = (TextView) ShowList.this.findViewById(android.R.id.empty);
    	  loading.setText(Callisto.RESOURCES.getString(R.string.list_empty));
    	  if(result==null)
    		  Toast.makeText(ShowList.this, Callisto.RESOURCES.getString(R.string.update_error), Toast.LENGTH_LONG).show();
    	  else if(mainListView!=null)
	   		  ShowList.this.updateHandler.sendMessage((Message) result);
    	  setProgressBarIndeterminateVisibility(false);
      }
   }
   
   /** Gets episode of a show outside of the UI thread. */
   private class GetShowTask extends AsyncTask<Object, Object, Cursor> 
   {
      @Override
      protected Cursor doInBackground(Object... params)
      {
         return Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], ShowList.this.filter);
      }

      @Override
      protected void onPostExecute(Cursor result)
      {
         showAdapter.changeCursor(result);
      }
   }
	
   /** Listener for when an item is selected. */
	OnItemClickListener selectEpisode = new OnItemClickListener() 
    {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id)
      {
    	  current_episode = view;
    	  Intent viewEpisode = new Intent(ShowList.this, EpisodeDesc.class);
    	  String content = (String)((TextView)view.findViewById(R.id.hiddenId)).getText();
    	  long contentL = Long.parseLong(content);
    	  viewEpisode.putExtra("id", contentL);
    	  setProgressBarIndeterminateVisibility(true);
    	  startActivity(viewEpisode);
      }
    };
    
    /** Listener for when an episode's "New" status is toggled. */
	public OnCheckedChangeListener toggleNew = new OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			Long id = Long.parseLong((String)
					((TextView)((View) buttonView.getParent()).findViewById(R.id.hiddenId)).getText());
			Callisto.databaseConnector.markNew(
					id
					, isChecked);
			Cursor c = Callisto.databaseConnector.getOneEpisode(id);
			c.moveToFirst();
		}
	};
    
    /** Adapter for the episode list. */
    public class ShowListCursorAdapter extends SimpleCursorAdapter
    {
        private Cursor c;
        private Context context;

    	public ShowListCursorAdapter(Context context, int layout, Cursor c) {
    		super(context, layout, c, new String[] {}, new int[] {});
    		this.c = c;
    		this.context = context;
    	}

    	public View getView(int pos, View inView, ViewGroup parent) {
           View v = inView;
           if (v == null) {
                LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.row, null);
           }
    	   this.c = getCursor();
           this.c.moveToPosition(pos);

           Date tempDate = new Date();
           int thisYear = tempDate.getYear();
           //Set the data From->To
           
    	   
           //Get info
    	   long id = this.c.getLong(this.c.getColumnIndex("_id"));
    	   this.c = Callisto.databaseConnector.getOneEpisode(id);
    	   this.c.moveToFirst();
    	   
           String date = this.c.getString(this.c.getColumnIndex("date"));
           String title = this.c.getString(this.c.getColumnIndex("title"));
           String media_link = this.c.getString(this.c.getColumnIndex("mp3link"));
    	   
    	   //_id
    	   ((TextView) v.findViewById(R.id.hiddenId)).setText(Long.toString(id));
    	   //title
    	   ((TextView) v.findViewById(R.id.rowTextView)).setText(title);
    	   //date
		   String d = date;
		   try {
			  	tempDate = Callisto.sdfRaw.parse(d);
			  	if(tempDate.getYear()==thisYear)
			  		d = Callisto.sdfHuman.format(tempDate);
		  		else
		  			d = Callisto.sdfHumanLong.format(tempDate);
				//d = Callisto.sdfDestination.format();
			} catch (ParseException e) {
				Log.e("ShowList:ShowListAdapter:ParseException", "Error parsing a date from the SQLite db: ");
				Log.e("ShowList:ShowListAdapter:ParseException", d);
				Log.e("ShowList:ShowListAdapter:ParseException", "(This should never happen).");
				e.printStackTrace();
			}
		    ((TextView) v.findViewById(R.id.rowSubTextView)).setText(d);
		    //new
		    ((TextView) v.findViewById(R.id.rightTextView)).setText(this.c.getString(this.c.getColumnIndex("new")));
           
		    
		    
			File file_location = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + AllShows.SHOW_LIST[currentShow]);
	   		file_location = new File(file_location, Callisto.sdfFile.format(tempDate) + "__" + title + EpisodeDesc.getExtension(media_link));
		    
		    if(file_location.exists())
            {
        	   ((TextView) v.findViewById(R.id.rowTextView)).setTypeface(null, Typeface.BOLD);
        	   ((TextView) v.findViewById(R.id.rowSubTextView)).setTypeface(null, Typeface.BOLD);
            }
		    else
		    {
		    	((TextView) v.findViewById(R.id.rowTextView)).setTypeface(null, 0);
        	    ((TextView) v.findViewById(R.id.rowSubTextView)).setTypeface(null, 0);
		    }
		    
           //Hide the specific views
	       int[] hide = new int[] { R.id.rightTextView, R.id.moveUp, R.id.moveDown, R.id.remove, R.id.progress };
           for(int i=0; i<hide.length; i++)
        	   ((View) v.findViewById(hide[i])).setVisibility(View.GONE);
           
           //Check the Jupiter icon if it is new
           boolean is_new = this.c.getInt(this.c.getColumnIndex("new"))>0;
           CheckBox rb = ((CheckBox)v.findViewById(R.id.img));
           rb.setChecked(is_new);
           rb.setOnCheckedChangeListener(toggleNew);
           
           return(v);
    	}
    }
}
