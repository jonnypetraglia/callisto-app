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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/** An activity to display all the current downloads. 
 * @author MrQweex */

public class DownloadList extends ListActivity
{
	/** Contains the ProgressBar of the current download, for use with updating. */
	public static ProgressBar downloadProgress = null;
	private ListView mainListView;
	private static HeaderAdapter listAdapter ;
	public static Handler notifyUpdate;
    List<Long> activeDownloads = new ArrayList<Long>(), completedDownloads = new ArrayList<Long>();
    public static WifiManager.WifiLock Download_wifiLock;
    List<HeaderAdapter.Item> headerThings;

    /** Called when the activity is first created. Sets up the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mainListView = getListView();
		mainListView.setBackgroundColor(Callisto.RESOURCES.getColor(R.color.backClr));
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

        for(String s : sp.getString("ActiveDownloads", "").split("\\|"))
        {
            Log.e("ActiveDownloads:", "s: " + s);
            if(s.equals(""))
                continue;
            try{
            activeDownloads.add(Long.parseLong(s));
            }catch(Exception e){}
        }
        for(String s : sp.getString("CompletedDownloads", "").split("\\|"))
        {
            Log.e("CompletedDownloads:", "s: " + s);
            if(s.equals(""))
                continue;
            try{
                completedDownloads.add(Long.parseLong(s));
            }catch(Exception e){}
        }


        headerThings = new ArrayList<HeaderAdapter.Item>();
        if(activeDownloads.size()>0)
        {
            headerThings.add(new DownloadHeader("Active"));
            for(int i=0; i<activeDownloads.size(); i++)
                headerThings.add(new DownloadRow());
        }
        if(completedDownloads.size()>0)
        {
            headerThings.add(new DownloadHeader("Completed"));
            for(int i=0; i<completedDownloads.size(); i++)
                headerThings.add(new DownloadRow());
        }
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
	        	if(listAdapter!=null)
	        		listAdapter.notifyDataSetChanged();
	        }
	    };
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
			 Collections.swap(activeDownloads,num,num-1);
             writeActiveQueue();
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
			 if(num==0 || num==activeDownloads.size()-1)
				 return;
			 Collections.swap(activeDownloads,num,num+1);
             writeActiveQueue();
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
             if(parent.findViewById(R.id.moveUp).getVisibility()==View.GONE) //It's a completed download
             {
                completedDownloads.remove(num);
                headerThings.remove(activeDownloads.size()+num+1);
                if(completedDownloads.size()==0)
                    headerThings.remove(headerThings.size()-1);
                writeCompletedQueue();
             } else
             {
                activeDownloads.remove(num);
                headerThings.remove(num+1);
                if(activeDownloads.size()==0)
                    headerThings.remove(0);
                writeActiveQueue();
             }
			 listAdapter.notifyDataSetChanged();
			 Callisto.downloading_count--;
		  }
    };

    public void writeActiveQueue()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('|');
        for(long s: activeDownloads) {
            sb.append(s).append('|');
        }
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).edit();
        e.putString("ActiveDownloads", sb.toString());
        e.commit();
    }

    public void writeCompletedQueue()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('|');
        for(long s: completedDownloads) {
            sb.append(s).append('|');
        }
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(DownloadList.this).edit();
        e.putString("CompletedDownloads", sb.toString());
        e.commit();
    }



    public class DownloadHeader implements HeaderAdapter.Item
    {
        private String name;

        public DownloadHeader(String name)
        {
            this.name = name;
        }

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
                (row.findViewById(R.id.img)).setVisibility(View.GONE);
            }

            boolean completed = false;
            long id;
            position--;
            Log.e("Derpy", position + " vs " + completedDownloads.size() + " ( - " + activeDownloads.size());
            if(position>=activeDownloads.size())
            {
                completed = true;
                position = position - activeDownloads.size();
                if(activeDownloads.size()>0)
                    position--;     //To adjust for the "Active" header
                id = completedDownloads.get(position);
            }
            else
                id = activeDownloads.get(position);

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
            ((TextView)row.findViewById(R.id.rowSubTextView)).setText(show);
            ((TextView)row.findViewById(R.id.rightTextView)).setText(media_size);


            ImageButton up = ((ImageButton)row.findViewById(R.id.moveUp));
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

            if(position==0)
                downloadProgress = (ProgressBar) row.findViewById(R.id.progress);

            return row;
        }
    }


    //Integer.MAX_VALUE

    public static String makeFileFriendly(String input)
    {
        String output = input;

        return output;
    }
}
