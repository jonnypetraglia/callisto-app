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

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.qweex.callisto.R;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View.OnClickListener;
import com.qweex.callisto.StaticBlob;

/** An activity to display all the current downloads. 
 * @author MrQweex */

public class DownloadList extends ListActivity
{
    /** Strrrrrrrriiiiiiiinnnnnnnngggggssssss */
    final static public String ACTIVE = "ActiveDownloads", COMPLETED = "CompletedDownloads";
	/** Menu items */
    private static final int CLEAR_COMP_ID =Menu.FIRST+1, CLEAR_ACTIVE_ID = CLEAR_COMP_ID +1, PAUSE_ID= CLEAR_ACTIVE_ID +1;
    /** The Progressbar view of the current download; used for updating as it is downloaded */
	public static ProgressBar downloadProgress = null;
    /** The main listview */
	private ListView mainListView;
    /** Adapter for listview; includes header for Active and Completed */
	public static HeaderAdapter listAdapter ;
    /** Used to re-create the dummy headerThings that contains the counts for Active and Completed downloads required for the list adapter. Confused? that's normal. */
	public static Handler rebuildHeaderThings;
    /** Keeps device awake when downloading */
    public static WifiManager.WifiLock Download_wifiLock;
    /** A dummy array that contains objects for the adapter.
     * The important thing about this object is not the objects, but the NUMBER of objects and their order.
     * For example:
     *      1 header
     *      4 items
     *      1 header
     *      3 items
     * The adapter will use this to call getView for the correct view types the correct number of times.
     * But the data inside the list is not used at all in any other way.
     */
    public List<HeaderAdapter.Item> headerThings;

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
		    mainListView.setBackgroundColor(StaticBlob.RESOURCES.getColor(R.color.backClr));
        } catch(NullPointerException e) //TODO: This should NEVER happen. Seriously, what the hell.
        {
            if(mainListView==null)
                Log.e("DownloadList:onCreate", "mainListView is null for some dumb reason");
            if(StaticBlob.RESOURCES==null)
                Log.e("DownloadList:onCreate", "RESOURCES is null for some dumb reason");
            finish();
            return;
        }
		setTitle("Downloads");

        //Empty view
		TextView noResults = new TextView(this);
			noResults.setBackgroundColor(StaticBlob.RESOURCES.getColor(R.color.backClr));
			noResults.setTextColor(StaticBlob.RESOURCES.getColor(R.color.txtClr));
			noResults.setText(StaticBlob.RESOURCES.getString(R.string.list_empty));
			noResults.setTypeface(null, 2);
			noResults.setGravity(Gravity.CENTER_HORIZONTAL);
			noResults.setPadding(10,20,10,20);
		((ViewGroup)mainListView.getParent()).addView(noResults);
		mainListView.setEmptyView(noResults);


        headerThings = new ArrayList<HeaderAdapter.Item>();
        //Cleanse the lists
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).edit();
        String active = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).getString(ACTIVE, "|").replaceAll("x", "").replaceAll("//|0", "");
        while(active.contains("||"))
            active = active.replace("||","|");
        String[] activeCleanse = active.split("\\|");
        active = "|";
        for(int i=0; i<activeCleanse.length; i++)
        {
            if("".equals(activeCleanse[i]))
                continue;
            Cursor c = StaticBlob.databaseConnector.getOneEpisode(Long.parseLong(activeCleanse[i]));
            if(c.getCount()==1)
                active = active.concat(activeCleanse[i]).concat("|");
        }
        active = active.concat("|");
        e.putString(ACTIVE, active);
        String completed = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).getString(COMPLETED,"|").replaceAll("x","").replaceAll("//|0","");
        while(completed.contains("||"))
            completed = completed.replace("||","|");
        String[] completedCleanse = completed.split("\\|");
        completed = "|";
        for(int i=0; i<completedCleanse.length; i++)
        {
            if("".equals(completedCleanse[i]))
                continue;
            Cursor c = StaticBlob.databaseConnector.getOneEpisode(Long.parseLong(completedCleanse[i]));
            if(c.getCount()==1)
                completed = completed.concat(completedCleanse[i]).concat("|");
        }
        completed = completed.concat("|");
        e.putString(COMPLETED, completed);
        e.commit();

        //Header Things - START
        int tempInt = getDownloadCount(DownloadList.this, ACTIVE);
        Log.d("DownloadList:onCreate", "Rebuilding header things");
        if(tempInt>0)
        {
            headerThings.add(new DownloadHeader("Active"));
            for(int i=0; i<tempInt; i++)
            {
                Log.d("DownloadList:onCreate", "Adding active");
                headerThings.add(new DownloadRow());
            }
        }
        tempInt = getDownloadCount(DownloadList.this, COMPLETED);
        if(tempInt>0)
        {
            headerThings.add(new DownloadHeader("Completed"));
            for(int i=0; i<tempInt; i++)
            {
                Log.d("DownloadList:onCreate", "Adding completed");
                headerThings.add(new DownloadRow());
            }
        }
        //END
        Log.d("DownloadList:onCreate", "Total: " + tempInt + " " + getDownloadCount(this,ACTIVE));

        //Listview things
        listAdapter = new HeaderAdapter(this, headerThings);
		mainListView.setAdapter(listAdapter);
		mainListView.setBackgroundColor(StaticBlob.RESOURCES.getColor(R.color.backClr));
		mainListView.setCacheColorHint(StaticBlob.RESOURCES.getColor(R.color.backClr));

        //like identical to the stuff above but slightly different.
	    rebuildHeaderThings = new Handler()
	    {
	        @Override
	        public void handleMessage(Message msg)
	        {
                headerThings.clear();
                int tempInt = getDownloadCount(DownloadList.this, ACTIVE);
                String active = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).getString(ACTIVE, "|").replaceAll("x","");
                Log.d("DownloadList:onCreate", "Rebuilding header things");
                if(tempInt>0)
                {
                    headerThings.add(new DownloadHeader("Active"));
                    for(String s : active.split("\\|"))
                    {
                        if(s.length()==0 || s.equals("0"))
                            continue;
                        Log.d("DownloadList:onCreate", "Adding active");
                        headerThings.add(new DownloadRow());
                    }
                }
                tempInt = getDownloadCount(DownloadList.this, COMPLETED);
                String completed = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).getString(COMPLETED, "|").replaceAll("x","");
                if(tempInt>0)
                {
                    headerThings.add(new DownloadHeader("Completed"));
                    for(String s : completed.split("\\|"))
                    {
                        if(s.length()==0 || s.equals("0"))
                            continue;
                        Log.d("DownloadList:onCreate", "Adding completed");
                        headerThings.add(new DownloadRow());
                    }
                }
	        	if(listAdapter!=null)
	        		listAdapter.notifyDataSetChanged();
	        }
	    };
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //Pronounce this outloud
        boolean paoosay = (DownloadTask.running || getDownloadCount(this, ACTIVE)==0);
        menu.add(0, PAUSE_ID, 0, paoosay ? "PAUSE" : "RESUME").setIcon(paoosay ? R.drawable.ic_action_playback_pause : R.drawable.ic_action_playback_play).setEnabled(!(paoosay && !DownloadTask.running));
        menu.add(0, CLEAR_COMP_ID, 0, "Clear Completed").setIcon(R.drawable.ic_action_trash).setEnabled(getDownloadCount(this, COMPLETED)>0);
        menu.add(0, CLEAR_ACTIVE_ID, 0, "Cancel Active").setIcon(R.drawable.ic_action_trash).setEnabled(getDownloadCount(this, ACTIVE)>0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        SharedPreferences.Editor e;
        switch (item.getItemId())
        {
            case CLEAR_COMP_ID:     //Clears completed
                e = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).edit();
                e.remove(COMPLETED);
                e.commit();
                rebuildHeaderThings.sendEmptyMessage(0);
                item.setEnabled(false);
                headerThings.remove(0);
                for(int i=0; i<PreferenceManager.getDefaultSharedPreferences(DownloadList.this).getString(COMPLETED,"|").replaceAll("x","").split("\\|").length; i++)
                    headerThings.remove(0);
                break;
            case CLEAR_ACTIVE_ID:   //Clears (that is, cancels) the active
                e = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).edit();
                e.remove(ACTIVE);
                e.commit();
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
                    StaticBlob.downloading_count = getDownloadCount(this, ACTIVE);
                    EpisodeDesc.dltask = new DownloadTask(DownloadList.this);
                    DownloadTask.running = true;
                    EpisodeDesc.dltask.execute();
                }
                break;
        }
        return true;
    }

        /** @deprecated
         * Listener for the up button ("^"). Moves a download up in the list. */
	OnClickListener moveUp = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 int num = Integer.parseInt((String) tv.getText());
			 if(num==0 || num==1)
				 return;
			 //Collections.swap(activeDownloads,num,num-1);
			 listAdapter.notifyDataSetChanged();
		  }
    };
    
    /** @deprecated
     * Listener for the down button ("v"). Moves a download down in the list. */
    OnClickListener moveDown = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 int num = Integer.parseInt((String) tv.getText());
			 if(num==0 || num>=getDownloadCount(v.getContext(),ACTIVE))
				 return;
              moveDownload(v.getContext(),ACTIVE,num,false,false);
			 //Collections.swap(activeDownloads,num,num+1);
			 listAdapter.notifyDataSetChanged();
		  }
    };

    /** @deprecated
     * Moves a download
     * @param c Context, used for accessing preferences
     * @param pref The preference (for either completed or active)
     * @param idToMove The ID of the item to move
     * @param isVid If the item to be moved is a vid (not currently used)
     * @param down If down; otherwise goes up
     */
    public static void moveDownload(Context c, String pref, int idToMove, boolean isVid, boolean down)
    {
        Long realId = getDownloadAt(c, pref, idToMove);

        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(c).edit();
        String aDownloads = PreferenceManager.getDefaultSharedPreferences(c).getString(pref, "");
        aDownloads = aDownloads.replace("|" + realId + "|","||");
        int A = aDownloads.indexOf("||");
        int B = aDownloads.indexOf("|", A+2);
        aDownloads = aDownloads.substring(0,A+1) + aDownloads.substring(A+2, B+1) + Long.toString(realId) + "|" + aDownloads.substring(B+1);
        //e.putString(pref,aDownloads);
        //e.commit();
    }

    /** Listener for the remove button ("x"). Removes a download from the list, and deletes it if it is the current download. */
    OnClickListener removeItem = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
             View parent = (View)(v.getParent());
			 TextView tv = (TextView) parent.findViewById(R.id.hiddenId);
			 int num = Integer.parseInt((String) tv.getText());
              Log.d("DownloadList:removeItem", "Removing item at: " + num);
             if(parent.findViewById(R.id.moveUp).isEnabled()==false) //It's a completed download
             {
                 removeDownloadAt(v.getContext(), COMPLETED, num);
                headerThings.remove(getDownloadCount(v.getContext(), ACTIVE)+num+1);
                if(getDownloadCount(v.getContext(),COMPLETED)==0)
                    headerThings.remove(headerThings.size()-1);
             } else
             {
                 removeDownloadAt(v.getContext(), ACTIVE, num);
                headerThings.remove(num+1);
                if(getDownloadCount(v.getContext(), ACTIVE)==0)
                    headerThings.remove(0);
             }
			 listAdapter.notifyDataSetChanged();
			 StaticBlob.downloading_count--;
		  }
    };


    /** The header for this activity's ListView, contains either "Active" or "Completed" */
    public class DownloadHeader implements HeaderAdapter.Item
    {
        /** The name to display */
        private String name;

        public DownloadHeader(String name)
        {
            this.name = name;
        }

        public String getText(){ return name;}

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
            x.setText(name);
            x.setFocusable(false);
            x.setEnabled(false);
            return row;
        }
    }

    /** The row for this activity's ListView */
    public class DownloadRow implements HeaderAdapter.Item
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

            if(row==null)
            {
                LayoutInflater inflater=getLayoutInflater();
                row=inflater.inflate(R.layout.row, parent, false);

                int[] hide = new int[] {R.id.grabber, R.id.moveUp, R.id.moveDown, R.id.img};
                for(int i=0; i<hide.length; i++)
                    (row.findViewById(hide[i])).setVisibility(View.GONE);
            }

            long id;
            position--;
            if(position>=getDownloadCount(parent.getContext(), ACTIVE))     //It's completed
            {
                position = position - getDownloadCount(parent.getContext(), ACTIVE);
                if(getDownloadCount(parent.getContext(), ACTIVE)>0)
                    position=position-1;     //To adjust for the "Active" header
                id = getDownloadAt(parent.getContext(), COMPLETED, position);
                Log.e(":", "COMP[" + position + "]: " + PreferenceManager.getDefaultSharedPreferences(parent.getContext()).getString(COMPLETED,"|"));
                //row.findViewById(R.id.moveUp).setEnabled(false);
            }
            else
            {
                Log.e(":", "COMP[" + position + "]: " + PreferenceManager.getDefaultSharedPreferences(parent.getContext()).getString(ACTIVE,"|"));
                id = getDownloadAt(parent.getContext(), ACTIVE, position);
                //row.findViewById(R.id.moveUp).setEnabled(true);
            }

            boolean isVideo = id<0;
            if(isVideo)
                id = id*-1;
            Log.e(":", "Requested id:" + id);
            Cursor c = StaticBlob.databaseConnector.getOneEpisode(id);
            c.moveToFirst();

            //Set the data
            String title = c.getString(c.getColumnIndex("title"));
            String show = c.getString(c.getColumnIndex("show"));
            String media_size = EpisodeDesc.formatBytes(c.getLong(c.getColumnIndex(isVideo?"vidsize":"mp3size")));	//IDEA: adjust for watch if needed
            ((TextView)row.findViewById(R.id.hiddenId)).setText(Integer.toString(position));
            ((TextView)row.findViewById(R.id.rowTextView)).setText(title);
            ((TextView)row.findViewById(R.id.rowTextView)).setTag(Integer.toString(position));
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
                File file_location = new File(Environment.getExternalStorageDirectory(), StaticBlob.storage_path + File.separator + show);
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
            }

            return row;
        }
    }


    //Integer.MAX_VALUE
    /** Makes a string friendly to the filesystem by replacing offending characters with underscores
     * @param input The input. DUH. GOD. DO I HAVE TO TELL YOU EVERYTHING.
     * */
    public static String makeFileFriendly(String input)
    {
        return input.replaceAll("[\\?]", "_"); //[\\?:;\*"<>\|]
    }

    /** Retrieves the ID from the list of all downloads at a specific location
     * @param c Context, needed for getting a SharedPReferences
     * @param pref The preference (for either completed or active)
     * @param num The index of which download to retrieve
     * @return The ID of
     * */
    public static long getDownloadAt(Context c, String pref, int num)
    {
        String[] derp = PreferenceManager.getDefaultSharedPreferences(c).getString(pref,"|").replaceAll("x","").split("\\|");
        return Long.parseLong( derp[num+1] );
    }

    /** Retrieves the number of downloads
     * @param c Context, used for getting SharedPreferences
     * @param pref The preference (for either completed or active)
     * @return The number of downloads for the preference
     * */
    public static int getDownloadCount(Context c, String pref)
    {
        return PreferenceManager.getDefaultSharedPreferences(c).getString(pref,"|").split("\\|").length-1;
    }

    /** Adds a download
     * @param c Context, used for accessing preferences
     * @param pref The preference (for either completed or active)
     * @param idToAdd The ID of the item to add
     * @param isVid If the item adding is vid
     */
    public static void addDownload(Context c, String pref, Long idToAdd, boolean isVid)
    {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(c).edit();
        String aDownloads = PreferenceManager.getDefaultSharedPreferences(c).getString(pref, "");
        if(aDownloads.equals(""))
            aDownloads = "|";
        if(pref.equals(ACTIVE))
            aDownloads = aDownloads.concat(Long.toString(idToAdd * (isVid?-1:1)) + "|");
        else
            aDownloads = "|" + Long.toString(idToAdd * (isVid?-1:1)) + aDownloads;
        Log.i("EpisodeDesc:addDownload", "Updated " + pref + " list: " + aDownloads);
        e.putString(pref, aDownloads);
        e.commit();
    }

    /** Removes a download at a specific location
     * @param c Context, used for accessing preferences
     * @param pref The preference (for either completed or active)
     * @param position The position at which to remove
     */
    public static void removeDownloadAt(Context c, String pref, int position)
    {
        String s = PreferenceManager.getDefaultSharedPreferences(c).getString(pref, "");
        try {
        removeDownload(c, pref, s.substring(1).split("\\|")[position], false);
        } catch(Exception e)
        {
            Toast.makeText(c, "An error occurred whilst trying to remove. But I used the word 'whilst' so please don't hate me. [" + s + "]", Toast.LENGTH_SHORT).show();
        }
    }

    /** Removes a download by ID
     * @param c Context, used for accessing preferences
     * @param pref The preference (for either completed or active)
     * @param idToRemove The ID of the item to remove
     * @param isVid If the item to be removed is a vid (not currently used)
     */
    public static void removeDownload(Context c, String pref, Long idToRemove, boolean isVid) { removeDownload(c, pref, Long.toString(idToRemove), isVid);}
    public static void removeDownload(Context c, String pref, String idToRemove, boolean isVid)
    {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(c).edit();
        String cDownloads = PreferenceManager.getDefaultSharedPreferences(c).getString(pref, "");
        Log.i("EpisodeDesc:removeDownload", "pre-up " + pref + " list: " + cDownloads + " [ " + idToRemove + " ] ");
        cDownloads = cDownloads.replace("|" + idToRemove + "|", "|");
        if(cDownloads.equals("|"))
            cDownloads="";
        e.putString(pref, cDownloads);
        e.commit();
        Log.i("EpisodeDesc:removeDownload", "Updated " + pref + " list: " + cDownloads);
    }
}
