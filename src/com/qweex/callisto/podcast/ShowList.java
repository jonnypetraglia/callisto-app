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
import java.util.Date;

import android.app.Dialog;
import com.qweex.callisto.*;

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
    /** The number of the show (according to the AllShows array) that is the current show. */
    public static String currentShow = "", currentShowAudio = "", currentShowVideo = "";
    /** Used to adjust the watched status and downloaded status of an episode in the OnResume" */
    public static View current_episode = null;

    //-----Local Variables-----
    /** Listview for episodes */
    private ListView mainListView = null;
    /** The SQL adapter for the listview */
    private CursorAdapter showAdapter;
    /** Menu ID */
    private static final int STOP_ID=Menu.FIRST+1, RELOAD_ID=STOP_ID+1, CLEAR_ID=RELOAD_ID+1, FILTER_ID=CLEAR_ID+1, MARK_ID = FILTER_ID+1;

    /** The empty listview view for showing when it is loading */
    private static TextView loading;
    /** The empty listview view for when it has not ever been updated */
    private static Button refresh;
    /** Whether or not it is set to only show new episodes; set via the menu item */
    private boolean filter;
    /** Preferences for this show; contains things like last checked time */
    public SharedPreferences showSettings;

    public View currentQueueItem, currentDownloadItem;
    public static ShowList thisInstance;

    /** Called when the activity is first created. Sets up the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     *  */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        String TAG = StaticBlob.TAG();
        if(android.os.Build.VERSION.SDK_INT >= 11)
            setTheme(R.style.Default_New);
        //General create stuff
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Log.v(TAG, "Launching Activity");
        mainListView = new ListView(this);
        Callisto.build_layout(this, mainListView);

        //Get the settings for this show
        currentShow = getIntent().getExtras().getString("current_show");
        currentShowAudio = getIntent().getExtras().getString("current_show_audio");
        currentShowVideo = getIntent().getExtras().getString("current_show_video");
        showSettings = getSharedPreferences(currentShow, 0);
        filter = showSettings.getBoolean("filter", false);
        loading = (TextView) findViewById(android.R.id.empty);
        //If it has been checked before but there are no episodes, show that it is empty, not just loading.
        if(StaticBlob.databaseConnector.getShow(currentShow, filter).getCount()==0)
            loading.setText(this.getResources().getString(R.string.list_empty));
        else
            loading.setText(this.getResources().getString(R.string.loading));
        loading.setGravity(Gravity.CENTER_HORIZONTAL);
        //If it has never been checked before add a refresh button
        if(showSettings.getString("last_checked", null)==null)
        {
            refresh = new Button(this);
            refresh.setText(this.getResources().getString(R.string.refresh));
            refresh.setTextColor(this.getResources().getColor(R.color.txtClr));
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
            ((LinearLayout)mainListView.getParent()).setBackgroundColor(this.getResources().getColor(R.color.backClr));
            mainListView.setEmptyView(refresh);
            loading.setVisibility(View.INVISIBLE);
        }
        else
        {
            mainListView.setEmptyView(loading);
        }
        //Finish setting up the listview
        mainListView.setOnItemClickListener(selectEpisode);
        mainListView.setBackgroundColor(getResources().getColor(R.color.backClr));
        mainListView.setCacheColorHint(getResources().getColor(R.color.backClr));
        setTitle(currentShow);
        //Get the shows from the SQL; make it async because of reasons
        new GetShowTask().execute((Object[]) null);
        Cursor r = StaticBlob.databaseConnector.getShow(currentShow, filter);
        showAdapter = new ShowListCursorAdapter(ShowList.this, R.layout.row, r);
        mainListView.setAdapter(showAdapter);
        return;
    }

    /** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
    @Override
    public void onResume()
    {
        String TAG = StaticBlob.TAG();
        super.onResume();
        thisInstance = this;
        setProgressBarIndeterminateVisibility(false);
        StaticBlob.playerInfo.update(ShowList.this);      //Update player controls
        if(StaticBlob.audioFocus!=null)
            StaticBlob.audioFocus.setContext(this);
        if(CallistoService.audioJackReceiver!=null)
            CallistoService.audioJackReceiver.setContext(this);

        if(current_episode==null)       //If we aren't returning from an EpisodeDesc, we're done.
            return;

        try {
            long id = Long.parseLong(
                    (String) ((TextView) current_episode.findViewById(R.id.hiddenId)).getText()
            );  //Get the id of the episode that was visited in an EpisodeDesc
            Cursor c = StaticBlob.databaseConnector.getOneEpisode(id);
            c.moveToFirst();

            //New marker
            boolean is_new = c.getInt(c.getColumnIndex("new"))>0;
            ((CheckBox)current_episode.findViewById(R.id.img)).setChecked(is_new);

            Date tempDate = StaticBlob.sdfRaw.parse(c.getString(c.getColumnIndex("date")));   //Need this for file location
            String title = c.getString(c.getColumnIndex("title")),
                    mp3_link = c.getString(c.getColumnIndex("mp3link")),     //Need this for file extension
                    vid_link = c.getString(c.getColumnIndex("vidlink"));     // ^
            File music_file_location = new File(StaticBlob.storage_path + File.separator + currentShow);
            music_file_location = new File(music_file_location, StaticBlob.sdfFile.format(tempDate) + "__" + StaticBlob.makeFileFriendly(title) + EpisodeDesc.getExtension(mp3_link));
            File video_file_location = new File(StaticBlob.storage_path + File.separator + currentShow);
            video_file_location = new File(video_file_location, StaticBlob.sdfFile.format(tempDate) + "__" + StaticBlob.makeFileFriendly(title) + EpisodeDesc.getExtension(vid_link));

            runOnUiThread(new updateBoldOrItalic(id, current_episode, music_file_location, video_file_location, c.getLong(c.getColumnIndex("mp3size")), c.getLong(c.getColumnIndex("vidsize"))));

            c = StaticBlob.databaseConnector.currentQueueItem();
            if(currentQueueItem==null || currentQueueItem == current_episode)
            {
                if(c.getCount()>0)
                {
                    c.moveToFirst();
                    if(c.getLong(c.getColumnIndex("identity"))==id)
                        currentQueueItem = current_episode;
                    else
                        currentQueueItem = null;
                }
                else
                    currentQueueItem = null;
            }
            // Mark current download item
            c = StaticBlob.databaseConnector.getActiveDownloads();
            if(currentDownloadItem==null || currentDownloadItem == current_episode)
            {
                if(c.getCount()>0)
                {
                    c.moveToFirst();
                    Log.i(TAG, "  currentDownloadItem =? " + currentDownloadItem + "   " + c.getLong(c.getColumnIndex("identity")) + " == " + id);
                    if(c.getLong(c.getColumnIndex("identity"))==id)
                        currentDownloadItem = current_episode;
                    else
                        currentDownloadItem = null;
                }
                else
                    currentDownloadItem = null;
            }

        }catch(Exception e)
        {
            Log.e(TAG, "Error: " + e.getClass() + " - " + e.getMessage() + "(this should never happen...?)");
        }
        current_episode=null;
    }

    /** Called when the activity is going to be destroyed. */
    @Override
    public void onDestroy()
    {
        String TAG = StaticBlob.TAG();
        super.onDestroy();
        thisInstance = null;
    }

    /** Called when a search is requested.
     * @return true if the search was handled, false otherwise
     */
    @Override
    public boolean onSearchRequested ()
    {
        SearchResultsActivity.searchShow = currentShow;
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
        menu.add(0, STOP_ID, 0, this.getResources().getString(R.string.stop)).setIcon(R.drawable.ic_action_playback_stop);
        menu.add(0, RELOAD_ID, 0, this.getResources().getString(R.string.refresh)).setIcon(R.drawable.ic_action_reload);
        menu.add(0, CLEAR_ID, 0, this.getResources().getString(R.string.clear)).setIcon(R.drawable.ic_action_trash);
        menu.add(0, FILTER_ID, 0, this.getResources().getString(filter ? R.string.unfilter : R.string.filter)).setIcon(R.drawable.ic_action_filter);
        menu.add(0, MARK_ID, 0, this.getResources().getString(R.string.mark)).setIcon(R.drawable.ic_action_tick);
        return true;
    }

    /** Called when an item in the menu is pressed.
     * @param item The menu item ID that was pressed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        AlertDialog d;
        switch (item.getItemId())
        {
            case STOP_ID:   //Stop playing
                PlayerControls.stop(this);
                return true;
            case RELOAD_ID: //Check for new episodes
                reload();
                return true;
            case CLEAR_ID:  //Show a dialog to clear all items of this show from the SQL database
                d = new AlertDialog.Builder(this)
                        .setTitle(this.getResources().getString(R.string.confirm))
                        .setMessage(this.getResources().getString(R.string.confirm_clear))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                loading.setText(ShowList.this.getResources().getString(R.string.list_empty));
                                StaticBlob.databaseConnector.clearShow(ShowList.this, currentShow);
                                Cursor r = StaticBlob.databaseConnector.getShow(currentShow, filter);
                                ShowList.this.showAdapter.changeCursor(r);
                                ShowList.this.showAdapter.notifyDataSetChanged();
                                SharedPreferences.Editor editor = showSettings.edit();
                                editor.putString("last_checked", null);
                                editor.commit();
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
                StaticBlob.formatAlertDialogButtons(d);
                return true;
            case FILTER_ID: //Filter or unfilter the results showing only new or all
                filter = !filter;
                if(filter)
                    item.setTitle(this.getResources().getString(R.string.unfilter));
                else
                    item.setTitle(this.getResources().getString(R.string.filter));
                Cursor r = StaticBlob.databaseConnector.getShow(currentShow, filter);
                ShowList.this.showAdapter.changeCursor(r);
                ShowList.this.showAdapter.notifyDataSetChanged();
                SharedPreferences.Editor editor = showSettings.edit();
                editor.putBoolean("filter", filter);
                editor.commit();
                return true;
            case MARK_ID:   //Mark all in the show as new or old
                d = new AlertDialog.Builder(this)
                        .setTitle(this.getResources().getString(R.string.mark_all))
                        .setPositiveButton(this.getResources().getString(R.string.new_), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                StaticBlob.databaseConnector.markAllNew(currentShow, true);
                                Cursor r = StaticBlob.databaseConnector.getShow(currentShow, filter);
                                ShowList.this.showAdapter.changeCursor(r);
                                ShowList.this.showAdapter.notifyDataSetChanged();
                            }})
                        .setNegativeButton(this.getResources().getString(R.string.old), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                StaticBlob.databaseConnector.markAllNew(currentShow, false);
                                Cursor r = StaticBlob.databaseConnector.getShow(currentShow, filter);
                                ShowList.this.showAdapter.changeCursor(r);
                                ShowList.this.showAdapter.notifyDataSetChanged();
                            }}).show();
                d.getButton(Dialog.BUTTON_POSITIVE).setBackgroundResource(R.drawable.blue_button);
                d.getButton(Dialog.BUTTON_POSITIVE).setPadding((int)(15*StaticBlob.DP),(int)(15*StaticBlob.DP),(int)(15*StaticBlob.DP),(int)(15*StaticBlob.DP));
                d.getButton(Dialog.BUTTON_NEGATIVE).setBackgroundResource(R.drawable.blue_button);
                d.getButton(Dialog.BUTTON_NEGATIVE).setPadding((int)(15*StaticBlob.DP),(int)(15*StaticBlob.DP),(int)(15*StaticBlob.DP),(int)(15*StaticBlob.DP));
                ((View)d.getButton(Dialog.BUTTON_POSITIVE).getParent()).setBackgroundResource(R.color.backClr);
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
        loading.setText(this.getResources().getString(R.string.loading));
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
            String TAG = StaticBlob.TAG();
            if(msg.arg1!=0)
            {
                Cursor r = StaticBlob.databaseConnector.getShow(currentShow, filter);
                ShowList.this.showAdapter.changeCursor(r);
                ShowList.this.showAdapter.notifyDataSetChanged();
                Log.i(TAG, "Changing cursor");
            }
            else
            {
                Log.i(TAG, "Not Changing cursor");
                loading.setText(ShowList.this.getResources().getString(R.string.list_empty));
            }
        }
    };


    /** Updates the show outside of the UI thread. */
    private class UpdateShowTask extends AsyncTask<Object, Object, Message>
    {
        @Override
        protected void onPreExecute()
        {
            TextView loading = (TextView) ShowList.this.findViewById(android.R.id.empty);
            loading.setText(ShowList.this.getResources().getString(R.string.loading));
        }

        @Override
        protected Message doInBackground(Object... params)
        {
            UpdateShow us = new UpdateShow();
            return us.doUpdate(currentShow, showSettings, currentShowAudio, currentShowVideo);
        }

        @Override
        protected void onPostExecute(Message result)
        {
            String TAG = StaticBlob.TAG();
            TextView loading = (TextView) ShowList.this.findViewById(android.R.id.empty);
            loading.setText(ShowList.this.getResources().getString(R.string.list_empty));
            if(result.arg1==-1)
            {
                Log.w(TAG, "An error occurred while updating the show " + currentShow);
                Toast.makeText(ShowList.this, getResources().getString(R.string.show_update_error) + ":\n\n" + result.getData().getString("ERROR"),
                        Toast.LENGTH_LONG).show();
            }
            else if(mainListView!=null)
                ShowList.this.updateHandler.sendMessage((Message) result);
            setProgressBarIndeterminateVisibility(false);
        }
    }

    /** Gets episode of a show's episodes from the SQL outside of the UI thread. */
    private class GetShowTask extends AsyncTask<Object, Object, Cursor>
    {
        @Override
        protected Cursor doInBackground(Object... params)
        {
            return StaticBlob.databaseConnector.getShow(currentShow, ShowList.this.filter);
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
            String TAG = StaticBlob.TAG();
            current_episode = view;
            Intent viewEpisode = new Intent(ShowList.this, EpisodeDesc.class);
            long clickedID = Long.parseLong((String)((TextView)view.findViewById(R.id.hiddenId)).getText());
            viewEpisode.putExtra("id", clickedID);
            Log.v(TAG, "Selected ID: " + clickedID);
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
            String TAG = StaticBlob.TAG();
            Long id = Long.parseLong((String)
                    ((TextView)((View) buttonView.getParent()).findViewById(R.id.hiddenId)).getText());
            StaticBlob.databaseConnector.markNew(
                    id
                    , isChecked);
            Log.v(TAG, "Toggling new for:" + id + " is " + isChecked);
            Cursor c = StaticBlob.databaseConnector.getOneEpisode(id);
            c.moveToFirst();
        }
    };

    /** Adapter for the episode list. */
    public class ShowListCursorAdapter extends SimpleCursorAdapter
    {
        /** Cursor for the data */
        private Cursor c;
        /** Context needed for a LayoutInflater */
        private Context context;

        public ShowListCursorAdapter(Context context, int layout, Cursor c) {
            super(context, layout, c, new String[] {}, new int[] {});
            this.c = c;
            this.context = context;
        }

        public View getView(int pos, View inView, ViewGroup parent)
        {
            String TAG = StaticBlob.TAG();
            View v = inView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.row, null);
            }
            this.c = getCursor();
            this.c.moveToPosition(pos);

            Date tempDate = new Date(); //We use this variable to get thisYear as well as parsing the actual date later
            int thisYear = tempDate.getYear();      //If the date for the show is this year, no need to show the year
            //Set the data From->To

            //Get info for selected episode
            long id = this.c.getLong(this.c.getColumnIndex("_id"));
            this.c = StaticBlob.databaseConnector.getOneEpisode(id);
            this.c.moveToFirst();
            String date = this.c.getString(this.c.getColumnIndex("date"));
            String title = this.c.getString(this.c.getColumnIndex("title"));
            String mp3_link = this.c.getString(this.c.getColumnIndex("mp3link"));
            String vid_link = this.c.getString(this.c.getColumnIndex("vidlink"));

            //_id
            ((TextView) v.findViewById(R.id.hiddenId)).setText(Long.toString(id));
            //title
            ((TextView) v.findViewById(R.id.rowTextView)).setText(title);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)v.findViewById(R.id.rowTextView).getLayoutParams();
            lp.setMargins((int)(10*StaticBlob.DP), lp.topMargin, lp.rightMargin, lp.bottomMargin);;
            //date
            String d = date;
            try {
                tempDate = StaticBlob.sdfRaw.parse(d);
                if(tempDate.getYear()==thisYear)
                    d = StaticBlob.sdfHuman.format(tempDate);
                else
                    d = StaticBlob.sdfHumanLong.format(tempDate);
                //d = Callisto.sdfDestination.format();
            } catch (ParseException e) {
                Log.e(TAG+":ParseException", "Error parsing a date from the SQLite db: ");
                Log.e(TAG+":ParseException", d);
                Log.e(TAG+":ParseException", "(This should never happen).");
                e.printStackTrace();
            }
            ((TextView) v.findViewById(R.id.rowSubTextView)).setText(d);

            File music_file_location = null, video_file_location = null;
            try {
                music_file_location = new File(StaticBlob.storage_path + File.separator + currentShow);
                music_file_location = new File(music_file_location, StaticBlob.sdfFile.format(tempDate) + "__" + StaticBlob.makeFileFriendly(title) + EpisodeDesc.getExtension(mp3_link));
            }catch(NullPointerException npe) {
                Log.e(TAG, "Null pointer when determining file status: Audio");
            }
            try {
                video_file_location = new File(StaticBlob.storage_path + File.separator + currentShow);
                video_file_location = new File(video_file_location, StaticBlob.sdfFile.format(tempDate) + "__" + StaticBlob.makeFileFriendly(title) + EpisodeDesc.getExtension(vid_link));
            }catch(NullPointerException npe) {
                Log.e(TAG, "Null pointer when determining file status: Video");
            }

            runOnUiThread(new updateBoldOrItalic(id, v, music_file_location, video_file_location, this.c.getLong(this.c.getColumnIndex("mp3size")), this.c.getLong(this.c.getColumnIndex("vidsize"))));


            // Mark current queue item
            Cursor c = StaticBlob.databaseConnector.currentQueueItem();
            if(currentQueueItem==null || currentQueueItem == v)
            {
                if(c.getCount()>0)
                {
                    c.moveToFirst();
                    if(c.getLong(c.getColumnIndex("identity"))==id)
                        currentQueueItem = v;
                    else
                        currentQueueItem = null;
                }
                else
                    currentQueueItem = null;
            }
            // Mark current download item
            c = StaticBlob.databaseConnector.getActiveDownloads();
            if(currentDownloadItem==null || currentDownloadItem == v)
            {
                if(c.getCount()>0)
                {
                    c.moveToFirst();
                    Log.i(TAG, "  currentDownloadItem =? " + currentDownloadItem + "   " + c.getLong(c.getColumnIndex("identity")) + " == " + id);
                    if(c.getLong(c.getColumnIndex("identity"))==id)
                        currentDownloadItem = v;
                    else
                        currentDownloadItem = null;
                }
                else
                    currentDownloadItem = null;
            }
            Log.i(TAG, "currentDownloadItem == " + currentDownloadItem);

            //Hide the specific views
            int[] hide = new int[] { R.id.remove, R.id.progress, R.id.grabber, R.id.rightTextView};
            for(int i=0; i<hide.length; i++)
                v.findViewById(hide[i]).setVisibility(View.GONE);

            //Check the Jupiter icon if it is new
            boolean is_new = this.c.getInt(this.c.getColumnIndex("new"))>0;
            CheckBox rb = ((CheckBox)v.findViewById(R.id.img));
            rb.setChecked(is_new);
            rb.setOnCheckedChangeListener(toggleNew);

            return(v);
        }
    }

    public class updateBoldOrItalic implements Runnable
    {
        long id, mp3size, vidsize;
        View v;
        File music_file_location, video_file_location;
        public updateBoldOrItalic(long id, View v, File music_file_location, File video_file_location, long mp3size, long vidsize)
        {
            this.id = id;
            this.v = v;
            this.music_file_location = music_file_location;
            this.video_file_location = video_file_location;
            this.mp3size = mp3size;
            this.vidsize = vidsize;
        }

        @Override
        public void run() {
            String TAG = StaticBlob.TAG();
            //Set the effects for if the episode has been downloaded or is in queue, etc
            boolean inDLQueue = StaticBlob.databaseConnector.isInActiveDownloadQueueAtAll(id);

            boolean mp3exists   = (music_file_location!=null && music_file_location.exists());
            boolean mp3complete = mp3exists && music_file_location.length()==mp3size;
            boolean videxists   = (video_file_location!=null && video_file_location.exists());
            boolean vidcomplete = videxists && video_file_location.length()==vidsize;

            if(inDLQueue || mp3exists || videxists)
            {
                if(mp3complete || vidcomplete)
                {
                    ((TextView) v.findViewById(R.id.rowTextView)).setTypeface(null, Typeface.BOLD);
                    ((TextView) v.findViewById(R.id.rowSubTextView)).setTypeface(null, Typeface.BOLD);
                } else
                {
                    ((TextView) v.findViewById(R.id.rowTextView)).setTypeface(null, Typeface.ITALIC);
                    ((TextView) v.findViewById(R.id.rowSubTextView)).setTypeface(null, Typeface.ITALIC);
                }
            }
            else    //No typeface
            {
                ((TextView) v.findViewById(R.id.rowTextView)).setTypeface(null, 0);
                ((TextView) v.findViewById(R.id.rowSubTextView)).setTypeface(null, 0);
            }
        }
    }
}
