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
import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View.OnClickListener;

/** An activity to display all the current downloads. 
 * @author MrQweex */

public class DownloadList extends ListActivity
{
	/** Contains the ProgressBar of the current download, for use with updating. */
    private static final int CLEAR_ID=Menu.FIRST+1, CLEAR_ID2 = CLEAR_ID+1, PAUSE_ID=CLEAR_ID2+1;
	public static ProgressBar downloadProgress = null;
	private ListView mainListView;
	public static HeaderAdapter listAdapter ;
	public static Handler notifyUpdate;
    public static WifiManager.WifiLock Download_wifiLock;
    public List<HeaderAdapter.Item> headerThings;

    final static public String ACTIVE = "ActiveDownloads", COMPLETED = "CompletedDownloads";

    /** Called when the activity is first created. Sets up the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mainListView = getListView();
        try {
		    mainListView.setBackgroundColor(Callisto.RESOURCES.getColor(R.color.backClr));
        } catch(NullPointerException e)
        {
            if(mainListView==null)
                Log.e("DownloadList:onCreate", "mainListView is null for some dumb reason");
            if(Callisto.RESOURCES==null)
                Log.e("DownloadList:onCreate", "RESOURCES is null for some dumb reason");
            finish();
            return;
        }
		setTitle("Downloads");
		
		TextView noResults = new TextView(this);
			noResults.setBackgroundColor(Callisto.RESOURCES.getColor(R.color.backClr));
			noResults.setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
			noResults.setText(Callisto.RESOURCES.getString(R.string.list_empty));
			noResults.setTypeface(null, 2);
			noResults.setGravity(Gravity.CENTER_HORIZONTAL);
			noResults.setPadding(10,20,10,20);
		((ViewGroup)mainListView.getParent()).addView(noResults);
		mainListView.setEmptyView(noResults);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);



        headerThings = new ArrayList<HeaderAdapter.Item>();
        //Cleanse the lists
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).edit();
        String active = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).getString(ACTIVE, "|").replaceAll("x", "").replaceAll("//|0", "");
        while(active.contains("||"))
            active = active.replace("||","|");
        //if(active.endsWith("|"))
           // active = active.substring(0, active.length()-2);
        e.putString(ACTIVE, active);
        String completed = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).getString(COMPLETED,"|").replaceAll("x","").replaceAll("//|0","");
        while(completed.contains("||"))
            completed = completed.replace("||","|");
        //if(completed.endsWith("|"))
           // completed = completed.substring(0, completed.length()-2);
        e.putString(COMPLETED, completed);
        e.commit();

        //START
        int tempInt = getDownloadCount(DownloadList.this, ACTIVE);
        Log.d(":", "REBUILDING HEADER THIGNS");
        if(tempInt>0)
        {
            headerThings.add(new DownloadHeader("Active"));
            for(int i=0; i<tempInt; i++)
            {
                Log.d(":", "Adding act");
                headerThings.add(new DownloadRow());
            }
        }
        tempInt = getDownloadCount(DownloadList.this, COMPLETED);
        if(tempInt>0)
        {
            headerThings.add(new DownloadHeader("Completed"));
            for(int i=0; i<tempInt; i++)
            {
                Log.d(":", "Adding comp");
                headerThings.add(new DownloadRow());
            }
        }
        //END
        Log.d("DownloadList:onCreate", "Total: " + tempInt + " " + getDownloadCount(this,ACTIVE));
        listAdapter = new HeaderAdapter(this, headerThings);



		//listAdapter = new DownloadsAdapter(this, R.layout.row, Callisto.download_queue);
		
		mainListView.setAdapter(listAdapter);
		mainListView.setBackgroundColor(Callisto.RESOURCES.getColor(R.color.backClr));
		mainListView.setCacheColorHint(Callisto.RESOURCES.getColor(R.color.backClr));
		
		notifyUpdate = new Handler()
	    {
	        @Override
	        public void handleMessage(Message msg)
	        {
                headerThings.clear();
                int tempInt = getDownloadCount(DownloadList.this, ACTIVE);
                String active = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).getString(ACTIVE, "|").replaceAll("x","");
                Log.d(":", "REBUILDING HEADER THIGNS");
                if(tempInt>0)
                {
                    headerThings.add(new DownloadHeader("Active"));
                    for(String s : active.split("\\|"))
                    {
                        if(s.length()==0 || s.equals("0"))
                            continue;
                        Log.d(":", "Adding act " + s);
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
                        Log.d(":", "Adding comp " + s);
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
        boolean paoosay = (DownloadTask.running || getDownloadCount(this, ACTIVE)==0);
        menu.add(0, PAUSE_ID, 0, paoosay ? "PAUSE" : "RESUME").setIcon(paoosay ? R.drawable.ic_action_playback_pause : R.drawable.ic_action_playback_play).setEnabled(!(paoosay && !DownloadTask.running));
        menu.add(0, CLEAR_ID, 0, "Clear Completed").setIcon(R.drawable.ic_action_trash).setEnabled(getDownloadCount(this, COMPLETED)>0);
        menu.add(0, CLEAR_ID2, 0, "Cancel Active").setIcon(R.drawable.ic_action_trash).setEnabled(getDownloadCount(this, ACTIVE)>0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        SharedPreferences.Editor e;
        switch (item.getItemId())
        {
            case CLEAR_ID:
                e = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).edit();
                e.remove(COMPLETED);
                e.commit();
                notifyUpdate.sendEmptyMessage(0);
                item.setEnabled(false);
                headerThings.remove(0);
                for(int i=0; i<PreferenceManager.getDefaultSharedPreferences(DownloadList.this).getString(COMPLETED,"|").replaceAll("x","").split("\\|").length; i++)
                    headerThings.remove(0);
                break;
            case CLEAR_ID2:
                e = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).edit();
                e.remove(ACTIVE);
                e.commit();
                notifyUpdate.sendEmptyMessage(0);
                item.setEnabled(false);
                break;
            case PAUSE_ID:
                if(DownloadTask.running)
                {
                    item.setIcon(R.drawable.ic_action_playback_play);
                    item.setTitle("Resume");
                    EpisodeDesc.dltask.cancel(true);
                    DownloadTask.running = false;
                } else
                {
                    if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
                    {
                        new AlertDialog.Builder(this)
                                .setTitle("No SD Card")
                                .setMessage("There is currently no external storage to write to.")
                                .setNegativeButton("Ok",null)
                                .create().show();
                        return true;
                    }
                    item.setIcon(R.drawable.ic_action_playback_pause);
                    item.setTitle("Pause");
                    SharedPreferences pf = PreferenceManager.getDefaultSharedPreferences(this);
                    Callisto.downloading_count = getDownloadCount(this, ACTIVE);
                    EpisodeDesc.dltask = new DownloadTask(DownloadList.this);
                    DownloadTask.running = true;
                    EpisodeDesc.dltask.execute();
                }
                break;
        }
        return true;
    }

        /** Listener for the up button ("^"). Moves a download up in the list. */
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
    
    /** Listener for the down button ("v"). Moves a download down in the list. */
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

    /** Listener for the remove button ("x"). Removes a download from the list, and deletes it if it is the current download. */
    OnClickListener removeItem = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
             View parent = (View)(v.getParent());
			 TextView tv = (TextView) parent.findViewById(R.id.hiddenId);
			 int num = Integer.parseInt((String) tv.getText());
              Log.d("DownloadList:remoteItem", "Removing item at: " + num);
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
			 Callisto.downloading_count--;
		  }
    };



    public class DownloadHeader implements HeaderAdapter.Item
    {
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
                {
                    (row.findViewById(hide[i])).setVisibility(View.GONE);
                }
            }

            boolean completed = false;
            long id;
            position--;
            if(position>=getDownloadCount(parent.getContext(), ACTIVE))     //It's completed
            {
                completed = true;
                position = position - getDownloadCount(parent.getContext(), ACTIVE);
                if(getDownloadCount(parent.getContext(), ACTIVE)>0)
                    position=position-1;     //To adjust for the "Active" header
                id = getDownloadAt(parent.getContext(), COMPLETED, position);
                row.findViewById(R.id.moveUp).setEnabled(false);
            }
            else
            {
                id = getDownloadAt(parent.getContext(), ACTIVE, position);
                row.findViewById(R.id.moveUp).setEnabled(true);
            }

            boolean isVideo = id<0;
            if(isVideo)
                id = id*-1;
            Log.e(":", "Requested id:" + id);
            Cursor c = Callisto.databaseConnector.getOneEpisode(id);
            c.moveToFirst();

            String title = c.getString(c.getColumnIndex("title"));
            String show = c.getString(c.getColumnIndex("show"));
            String media_size = EpisodeDesc.formatBytes(c.getLong(c.getColumnIndex(isVideo?"vidsize":"mp3size")));	//IDEA: adjust for watch if needed
            ((TextView)row.findViewById(R.id.hiddenId)).setText(Integer.toString(position));
            ((TextView)row.findViewById(R.id.rowTextView)).setText(title);
            ((TextView)row.findViewById(R.id.rowTextView)).setTag(Integer.toString(position));
            ((TextView)row.findViewById(R.id.rowSubTextView)).setText(show);
            ((TextView)row.findViewById(R.id.rightTextView)).setText(media_size);


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
            ImageButton remove = ((ImageButton)row.findViewById(R.id.remove));
            remove.setOnClickListener(removeItem);

            try {
                String date = Callisto.sdfFile.format(Callisto.sdfRaw.parse(c.getString(c.getColumnIndex("date"))));
                File file_location = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + show);
                file_location = new File(file_location, date + "__" + makeFileFriendly(title) + EpisodeDesc.getExtension(c.getString(c.getColumnIndex(isVideo?"vidlink":"mp3link")))); //IDEA: Adjust for watch
                ProgressBar progress = ((ProgressBar)row.findViewById(R.id.progress));
                int x = (int)(file_location.length()*100/c.getLong(c.getColumnIndex(isVideo?"vidsize":"mp3size")));
                progress.setMax(100);
                progress.setProgress(x);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            (row.findViewById(R.id.row)).measure(0,0);
            int x =(row.findViewById(R.id.row)).getMeasuredHeight();
            //Update the progressbar height
            ((ProgressBar)row.findViewById(R.id.progress)).getLayoutParams().height=x;
            ((ProgressBar)row.findViewById(R.id.progress)).setMinimumHeight(x);
            ((ProgressBar)row.findViewById(R.id.progress)).invalidate();

            if(downloadProgress==null || downloadProgress == (ProgressBar) row.findViewById(R.id.progress))
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
    public static String makeFileFriendly(String burt)
    {
        return burt.replaceAll("[\\?]", "_"); //[\\?:;\*"<>\|]
        //return burt;
    }


    public static long getDownloadAt(Context c, String pref, int num)
    {
        num++;
        String[] derp = PreferenceManager.getDefaultSharedPreferences(c).getString(pref,"|").replaceAll("x","").split("\\|");
        Log.e("PARSING:", PreferenceManager.getDefaultSharedPreferences(c).getString(pref,"|").replaceAll("x","") + "      at " + num + " with size " + derp.length);
        Log.e("PARSING:", derp[num] + " ");
        return Long.parseLong( derp[num] );
    }

    public static int getDownloadCount(Context c, String pref)
    {
        return PreferenceManager.getDefaultSharedPreferences(c).getString(pref,"|").split("\\|").length-1;
    }

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

    public static void moveDownload(Context c, String pref, int idToMove, boolean isVid, boolean down)
    {
        Long realId = getDownloadAt(c, pref, idToMove);

        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(c).edit();
        String aDownloads = PreferenceManager.getDefaultSharedPreferences(c).getString(pref, "");
        Log.e("OLD:aDOWNLOAD", aDownloads + " -> " + realId);
        aDownloads = aDownloads.replace("|" + realId + "|","||");
        int A = aDownloads.indexOf("||");
        int B = aDownloads.indexOf("|", A+2);
        Log.e("A:aDOWNLOAD",                                        " " +  B);
        Log.e("A:aDOWNLOAD", aDownloads.substring(1,A) + "( " + A);
        Log.e("B:aDOWNLOAD", aDownloads.substring(A+2,B-1) + "( " + B);
        Log.e("C:aDOWNLOAD", aDownloads.substring(B+1) + "( " + B+1);
        aDownloads = aDownloads.substring(0,A+1) + aDownloads.substring(A+2, B+1) + Long.toString(realId) + "|" + aDownloads.substring(B+1);
        Log.e("NEW:aDOWNLOAD", aDownloads);
        //e.putString(pref,aDownloads);
        //e.commit();
    }
}
