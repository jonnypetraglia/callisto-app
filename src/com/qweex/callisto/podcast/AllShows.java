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
import com.qweex.callisto.PlayerControls;
import com.qweex.callisto.R;

import android.app.Activity;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.qweex.callisto.StaticBlob;

//IDEA: Change method of having a header in the listview; Using separate layout files for the heading causes it to have to inflate the view every time rather than just update it
//FIXME: Scrolling in the AllShows view while something is playing is jerky

/** An activity to list all the shows on JupiterBroadcasting. 
 * @author MrQweex
 */
public class AllShows extends Activity {

    //-----Local Variables-----
    /** Menu ID */
    private final int STOP_ID = Menu.FIRST + 1, DOWNLOADS_ID = STOP_ID+1, UPDATE_ID = DOWNLOADS_ID+1;
    /** Listview for episodes; Static so that it can be used in a static handler. */
    private static ListView mainListView;
    /** Adapter for listview; Static so that it can be used in a static handler. */
    private static HeaderAdapter listAdapter ;
    /** Bookmark View used for adjusting the date in OnResume */
    private View current_view = null;
    /** Preferences to get settings for the current show (used in the adapter) */
    private SharedPreferences current_showSettings;

    /** Called when the activity is first created. Sets up the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        //DO create stuff
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

        //headerThings is a dummy list used entirely just for numbers, like '1 header, 2 items, 1 header, 4 items'.
        // from there the adapter does the actual important stuff using other arrays
        List<HeaderAdapter.Item> headerThings = new ArrayList<HeaderAdapter.Item>();
        for(int i=0; i< StaticBlob.SHOW_LIST.length; i++)
        {
            if(StaticBlob.SHOW_LIST[i].startsWith(" "))
                headerThings.add(new AllShowsHeader());
            else
                headerThings.add(new AllShowsRow());
        }

        //Ok set up the ListView
        listAdapter = new HeaderAdapter(this, headerThings);
        mainListView.setAdapter(listAdapter);
        mainListView.setBackgroundColor(getResources().getColor(R.color.backClr));
        mainListView.setCacheColorHint(getResources().getColor(R.color.backClr));
    }

    /** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
    @Override
    public void onResume()
    {
        super.onResume();
        Log.v("AllShows:onResume", "Resuming AllShows");
        setProgressBarIndeterminateVisibility(false);
        StaticBlob.playerInfo.update(AllShows.this);      //Update the player controls

        //If there is no current_view (i.e. if it's not returning from a ShowList activity) we're done.
        if(current_view==null)
            return;
        TextView current_textview = (TextView)current_view.findViewById(R.id.rowTextView);

        try
        {
            String the_current_show = (String)(current_textview).getText();
            int currentShowCount = StaticBlob.databaseConnector.getShow(the_current_show, true).getCount();

            ((TextView)current_view.findViewById(R.id.showUnwatched)).setTextColor((currentShowCount>0 ? 0xff000000 : 0x11000000) + this.getResources().getColor(R.color.txtClr));
            ((TextView)current_view.findViewById(R.id.showUnwatched)).setText(Integer.toString(currentShowCount));

            //Updated the "last_checked" time for this show in the view, not in the preferences. It was updated in the preferences by the update AsyncTask.
            SharedPreferences showSettings = getSharedPreferences(the_current_show, 0);
            String lastChecked = showSettings.getString("last_checked", null);
            Log.v("AllShows:onResume", "Resuming after:" + the_current_show + "| " + lastChecked);
            if(lastChecked!=null)
            {
                try {
                    lastChecked = StaticBlob.sdfDestination.format(StaticBlob.sdfSource.parse(lastChecked));
                    ((TextView)current_view.findViewById(R.id.rowSubTextView)).setText(lastChecked);
                } catch (ParseException e) {
                    Log.e("AllShows:OnResume:ParseException", "Error parsing a date from the SharedPreferences..");
                    Log.e("AllShows:OnResume:ParseException", lastChecked);
                    Log.e("AllShows:OnResume:ParseException", "(This should never happen).");
                }
            }
        }catch(NullPointerException npe)
        {
            Log.e("AllShows:onResume", "Null Pointer: " + npe.getMessage() + " (this should never happen)");
        }
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
        menu.add(0, STOP_ID, 0, this.getResources().getString(R.string.stop)).setIcon(R.drawable.ic_action_playback_stop);
        menu.add(0, DOWNLOADS_ID, 0, getResources().getString(R.string.downloads)).setIcon(R.drawable.ic_action_download);
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
                PlayerControls.stop(this);
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

    //** A handler to execute an UpdateAllShowsTask */
    Handler UpdateAllHandler = new Handler()
    {
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
            Toast.makeText(AllShows.this, AllShows.this.getResources().getString(R.string.beginning_update), Toast.LENGTH_SHORT).show();
            setProgressBarIndeterminateVisibility(true);
        }

        //Return null on failure, an empty message on success
        @Override
        protected Object doInBackground(Object... params)
        {
            boolean done = false,
                    skip_inactive = PreferenceManager.getDefaultSharedPreferences(AllShows.this).getBoolean("skip_inactive", false);
            boolean error_happened = false;
            for(int i=0; i< StaticBlob.SHOW_LIST_AUDIO.length; i++)
            {
                current_view = mainListView.getChildAt(i);
                if(StaticBlob.SHOW_LIST_AUDIO[i]==null)
                {
                    if(done && skip_inactive)
                        break;
                    done = true;
                    continue;
                }
                current_showSettings = getSharedPreferences(StaticBlob.SHOW_LIST[i], 0);
                UpdateShow us = new UpdateShow();
                Message m = us.doUpdate(i, current_showSettings);
                error_happened |= (m.arg1==-1);
                UpdateAdapter.sendEmptyMessage(0);
            }
            if(error_happened)
                return null;    //failure
            return new Message(); //success
        }

        /** Update the adapter */
        Handler UpdateAdapter = new Handler() {
            @Override
            public void handleMessage(Message msg)
            {
                if(msg==null)
                    Toast.makeText(AllShows.this, AllShows.this.getResources().getString(R.string.update_error), Toast.LENGTH_LONG).show();
                mainListView.setAdapter(listAdapter);
            }
        };

        @Override
        protected void onPostExecute(Object result)
        {
            Toast.makeText(AllShows.this, AllShows.this.getResources().getString(R.string.finished_update), Toast.LENGTH_SHORT).show();
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
            if(StaticBlob.SHOW_LIST[position].charAt(0)==' ')
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
    /** A header for the listview */
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
                row = inflater.inflate(R.layout.main_row_head, parent, false);
            }

            TextView x = ((TextView)row.findViewById(R.id.heading));
            x.setText(StaticBlob.SHOW_LIST[position]);
            x.setFocusable(false);
            x.setEnabled(false);
            return row;
        }
    }

    /** A row (i.e. non-header) for the listview */
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
                row = inflater.inflate(R.layout.main_row, parent, false);
            }

            //Get the show icon
            String[] exts = {".jpg", ".gif", ".png"};	//Technically, this can be removed since the images are all shrunken and re-compressed to JPGs when they are downloaded
            File f;
            for(String ext : exts)
            {
                f = new File(Environment.getExternalStorageDirectory() + File.separator +
                        StaticBlob.storage_path + File.separator +
                        StaticBlob.SHOW_LIST[position] + ext);
                if(f.exists())
                {
                    Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
                    ImageView img = (ImageView) row.findViewById(R.id.img);
                    if(img!=null) //TODO: This should never happen why did it happen once
                        img.setImageBitmap(bitmap);
                    break;
                }
            }

            //Set the unwatched count & color
            int currentShowUnwatchedCount = StaticBlob.databaseConnector.getShow(StaticBlob.SHOW_LIST[position], true).getCount();
            ((TextView)row.findViewById(R.id.showUnwatched)).setTextColor((currentShowUnwatchedCount>0 ? 0xff000000 : 0x11000000) + AllShows.this.getResources().getColor(R.color.txtClr));
            ((TextView)row.findViewById(R.id.showUnwatched)).setText(Integer.toString(currentShowUnwatchedCount));

            //Set the show text
            ((TextView)row.findViewById(R.id.rowTextView)).setText(StaticBlob.SHOW_LIST[position]);

            //Set the lastChecked view
            SharedPreferences showSettings = getSharedPreferences(StaticBlob.SHOW_LIST[position], 0);
            String lastChecked = showSettings.getString("last_checked", null);
            if(lastChecked!=null)
            {
                try {
                    lastChecked = StaticBlob.sdfDestination.format(StaticBlob.sdfSource.parse(lastChecked));
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
