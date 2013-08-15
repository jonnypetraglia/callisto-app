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

import android.app.AlertDialog;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.qweex.callisto.R;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View.OnClickListener;
import com.qweex.callisto.StaticBlob;

/** An activity to display all the current downloads. 
 * @author MrQweex */

public class DownloadList extends ListActivity
{
	/** Menu items */
    private static final int CLEAR_COMP_ID =Menu.FIRST+1, CLEAR_ACTIVE_ID = CLEAR_COMP_ID +1, PAUSE_ID= CLEAR_ACTIVE_ID +1;
    /** The Progressbar view of the current download; used for updating as it is downloaded */
	public static ProgressBar downloadProgress = null;
    /** The main listview */
	private ListView mainListView;
    /** Adapter for listview; includes header for Active and Completed */
	public static DownloadAdapter listAdapter ;
    /** Used to re-create the dummy headerThings that contains the counts for Active and Completed downloads required for the list adapter. Confused? that's normal. */
	public static Handler rebuildHeaderThings;
    /** Keeps device awake when downloading */
    public static WifiManager.WifiLock Download_wifiLock;

    /** Called when the activity is first created. Sets up the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
        //Do some create things
		super.onCreate(savedInstanceState);
		mainListView = getListView();
        try {
		    mainListView.setBackgroundColor(this.getResources().getColor(R.color.backClr));
        } catch(NullPointerException e) //TODO: This should NEVER happen. Seriously, what the hell.
        {
            if(mainListView==null)
                Log.e("DownloadList:onCreate", "mainListView is null for some dumb reason");
            if(this.getResources()==null)
                Log.e("DownloadList:onCreate", "RESOURCES is null for some dumb reason");
            finish();
            return;
        }
		setTitle("Downloads");

        //Empty view
		TextView noResults = new TextView(this);
			noResults.setBackgroundColor(this.getResources().getColor(R.color.backClr));
			noResults.setTextColor(this.getResources().getColor(R.color.txtClr));
			noResults.setText(this.getResources().getString(R.string.list_empty));
			noResults.setTypeface(null, 2);
			noResults.setGravity(Gravity.CENTER_HORIZONTAL);
			noResults.setPadding(10,20,10,20);
		((ViewGroup)mainListView.getParent()).addView(noResults);
		mainListView.setEmptyView(noResults);


        Cursor active = StaticBlob.databaseConnector.getActiveDownloads(),
             complete = StaticBlob.databaseConnector.getCompleteDownloads();
        MatrixCursor activeHeader = new MatrixCursor(new String[] { "_id", "identity", "video", "active" });
        MatrixCursor completeHeader = new MatrixCursor(new String[] { "_id", "identity", "video", "active" });

        if(active.getCount()>0)
            activeHeader.addRow(new String[] { "-1", "-1", "-1", "-1" });
        if(complete.getCount()>0)
            completeHeader.addRow(new String[] { "-2", "-2", "-2", "-2" });
        Cursor[] cursors = new Cursor[] {activeHeader, active, completeHeader, complete};
        Cursor extendedCursor = new MergeCursor(cursors);
        extendedCursor.moveToFirst();
        for(int i=0; i<extendedCursor.getCount(); i++)
        {
            Log.i("Downloads: ", extendedCursor.getString(extendedCursor.getColumnIndex("identity")));
            extendedCursor.moveToNext();
        }
        listAdapter = new DownloadAdapter(this, 0, extendedCursor, new String[] {}, new int[] {}, 0);
        mainListView.setAdapter(listAdapter);


		mainListView.setBackgroundColor(this.getResources().getColor(R.color.backClr));
		mainListView.setCacheColorHint(this.getResources().getColor(R.color.backClr));

        //like identical to the stuff above but slightly different.
	    rebuildHeaderThings = new Handler()
	    {
	        @Override
	        public void handleMessage(Message msg)
	        {
                Cursor active = StaticBlob.databaseConnector.getActiveDownloads(),
                        complete = StaticBlob.databaseConnector.getCompleteDownloads();
                MatrixCursor activeHeader = new MatrixCursor(new String[] { "_id", "identity", "video", "active" });
                MatrixCursor completeHeader = new MatrixCursor(new String[] { "_id", "identity", "video", "active" });

                if(active.getCount()>0)
                    activeHeader.addRow(new String[] { "-1", "-1", "-1", "-1" });
                if(complete.getCount()>0)
                    completeHeader.addRow(new String[] { "-2", "-2", "-2", "-2" });
                Cursor[] cursors = new Cursor[] {activeHeader, active, completeHeader, complete};
                Cursor extendedCursor = new MergeCursor(cursors);

                try {
                if(listAdapter!=null)
                    listAdapter.changeCursor(extendedCursor);
                    //listAdapter.notifyDataSetChanged();
                }catch(IllegalArgumentException i) {}
	        }
	    };
	}


    public class DownloadAdapter extends SimpleCursorAdapter
    {
        /** Cursor for the data */
        private Cursor c;
        /** Context needed for a LayoutInflater */
        private Context context;


        public DownloadAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to);
            this.c = c;
            this.context = context;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent)
        {
            long identity, _id;
            boolean video;

            this.c = getCursor();
            this.c.moveToPosition(pos);
            _id = this.c.getLong(this.c.getColumnIndex("_id"));
            identity = this.c.getLong(this.c.getColumnIndex("identity"));
            video = this.c.getInt(this.c.getColumnIndex("video"))>0;

            if(_id<0)
                return getViewHeader(convertView, parent, identity);
            return getViewRow(pos, convertView, parent, _id, identity, video);
        }

        public View getViewHeader(View convertView, ViewGroup parent, long identity)
        {
            View row = convertView;
            if(row == null || row.findViewById(R.id.heading)==null)
            {
                LayoutInflater inflater=getLayoutInflater();
                row = (View) inflater.inflate(R.layout.main_row_head, parent, false);
            }

            TextView x = ((TextView)row.findViewById(R.id.heading));
            if(identity==-1)
                x.setText("Active");
            else
                x.setText("Completed");
            x.setFocusable(false);
            x.setEnabled(false);
            return row;
        }

        public View getViewRow(int position, View convertView, ViewGroup parent, long _id, long identity, boolean isVideo)
        {
            View row = convertView;

            if(row==null || row.findViewById(R.id.hiddenId)==null)
            {
                LayoutInflater inflater=getLayoutInflater();
                row=inflater.inflate(R.layout.row, parent, false);

                int[] hide = new int[] {R.id.grabber, R.id.moveUp, R.id.moveDown, R.id.img};
                for(int i=0; i<hide.length; i++)
                    (row.findViewById(hide[i])).setVisibility(View.GONE);
            }


            Cursor c = StaticBlob.databaseConnector.getOneEpisode(identity);
            if(c.getCount()==0)
                return row;
            c.moveToFirst();

            //Set the data
            String title = c.getString(c.getColumnIndex("title"));
            String show = c.getString(c.getColumnIndex("show"));
            String media_size = EpisodeDesc.formatBytes(c.getLong(c.getColumnIndex(isVideo?"vidsize":"mp3size")));	//IDEA: adjust for watch if needed
            ((TextView)row.findViewById(R.id.hiddenId)).setText(Long.toString(_id));
            ((TextView)row.findViewById(R.id.rowTextView)).setText(title);
            ((TextView)row.findViewById(R.id.rowTextView)).setTag(identity);
            ((TextView)row.findViewById(R.id.rowSubTextView)).setText(show);
            ((TextView)row.findViewById(R.id.rightTextView)).setText(media_size);
            ((ImageButton)row.findViewById(R.id.remove)).setOnClickListener(removeItem);

            /*ImageButton up = ((ImageButton)row.findViewById(R.id.moveUp));
            ImageButton down = ((ImageButton)row.findViewById(R.id.moveDown));
            up.setOnClickListener(moveUp);
            down.setOnClickListener(moveDown);
            if(completed)
            {
                up.setVisibility(View.GONE);
                down.setVisibility(View.GONE);
            }
            else
            {
                up.setVisibility(View.VISIBLE);
                down.setVisibility(View.VISIBLE);
                up.setEnabled(position>0);
                down.setEnabled(position>0);
            }
            */

            //Progress
            try {
                String date = StaticBlob.sdfFile.format(StaticBlob.sdfRaw.parse(c.getString(c.getColumnIndex("date"))));
                File file_location = new File(StaticBlob.storage_path + File.separator + show);
                file_location = new File(file_location, date + "__" + makeFileFriendly(title) + EpisodeDesc.getExtension(c.getString(c.getColumnIndex(isVideo?"vidlink":"mp3link")))); //IDEA: Adjust for watch
                ProgressBar progress = ((ProgressBar)row.findViewById(R.id.progress));
                int x = (int)(file_location.length()*100/c.getLong(c.getColumnIndex(isVideo?"vidsize":"mp3size")));
                progress.setMax(100);
                progress.setProgress(x);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            //Progressbar Height
            (row.findViewById(R.id.row)).measure(0,0);
            int x =(row.findViewById(R.id.row)).getMeasuredHeight();
            //Update the progressbar height
            row.findViewById(R.id.progress).getLayoutParams().height=x;
            row.findViewById(R.id.progress).setMinimumHeight(x);
            row.findViewById(R.id.progress).invalidate();

            //If it is the current download, set it
            //  NOTE: This only works if the current download is the top (i.e. position 0)
            if(downloadProgress==null || downloadProgress == row.findViewById(R.id.progress))
            {
                if(position==0)
                    downloadProgress = (ProgressBar) row.findViewById(R.id.progress);
                else
                    downloadProgress = null;
                Log.d("DERPPPPPP", downloadProgress + " !!!! " + position);
            }

            return row;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //Pronounce this outloud
        boolean paoosay = (DownloadTask.running || StaticBlob.databaseConnector.getActiveDownloads().getCount()==0);
        menu.add(0, PAUSE_ID, 0, paoosay ? "PAUSE" : "RESUME").setIcon(paoosay ? R.drawable.ic_action_playback_pause : R.drawable.ic_action_playback_play).setEnabled(!(paoosay && !DownloadTask.running));
        menu.add(0, CLEAR_COMP_ID, 0, "Clear Completed").setIcon(R.drawable.ic_action_trash).setEnabled(StaticBlob.databaseConnector.getCompleteDownloads().getCount()>0);
        menu.add(0, CLEAR_ACTIVE_ID, 0, "Cancel Active").setIcon(R.drawable.ic_action_trash).setEnabled(StaticBlob.databaseConnector.getActiveDownloads().getCount()>0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case CLEAR_COMP_ID:     //Clears completed
                StaticBlob.databaseConnector.clearCompleteDownloads();
                rebuildHeaderThings.sendEmptyMessage(0);
                item.setEnabled(false);
                break;
            case CLEAR_ACTIVE_ID:   //Clears (that is, cancels) the active
                StaticBlob.databaseConnector.clearActiveDownloads();
                rebuildHeaderThings.sendEmptyMessage(0);
                item.setEnabled(false);
                break;
            case PAUSE_ID:          //Pause or resume downloading
                if(DownloadTask.running)    //Pause
                {
                    item.setIcon(R.drawable.ic_action_playback_play);
                    item.setTitle("Resume");
                    EpisodeDesc.dltask.cancel(true);
                    DownloadTask.running = false;
                }
                else  //Resume
                {
                    //Check for sd card
                    if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
                    {
                        new AlertDialog.Builder(this)
                                .setTitle("No SD Card")
                                .setMessage("There is currently no external storage to write to.")
                                .setNegativeButton("Ok",null)
                                .create().show();
                        Log.w("DownloadList:onOptionsItemSelected", "No SD card");
                        return true;
                    }
                    item.setIcon(R.drawable.ic_action_playback_pause);
                    item.setTitle("Pause");
                    StaticBlob.downloading_count = StaticBlob.databaseConnector.getActiveDownloads().getCount();
                    EpisodeDesc.dltask = new DownloadTask(DownloadList.this);
                    DownloadTask.running = true;
                    EpisodeDesc.dltask.execute();
                }
                break;
        }
        return true;
    }

    /** Listener for the remove button ("x"). Removes a download from the list, and deletes it if it is the current download. */
    OnClickListener removeItem = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
             View parent = (View)(v.getParent());
			 TextView tv = (TextView) parent.findViewById(R.id.hiddenId);
			 int id = Integer.parseInt((String) tv.getText());
             StaticBlob.databaseConnector.removeDownload(id);

			 StaticBlob.downloading_count--;
             rebuildHeaderThings.sendEmptyMessage(0);
		  }
    };

    //Integer.MAX_VALUE
    /** Makes a string friendly to the filesystem by replacing offending characters with underscores
     * @param input The input. DUH. GOD. DO I HAVE TO TELL YOU EVERYTHING.
     * */
    public static String makeFileFriendly(String input)
    {
        return input.replaceAll("[\\?]", "_"); //[\\?:;\*"<>\|]
    }
}
