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

import java.util.ArrayList;
import java.util.Collections;

import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

//FEATURE: Progress via a colored background progress bar
//IDEA: Completed downloads

/** An activity to display all the current downloads. 
 * @author MrQweex */

public class DownloadList extends ListActivity
{
	private ListView mainListView;
	private DownloadsAdapter listAdapter ;
	
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
		
		listAdapter = new DownloadsAdapter(this, R.layout.row, Callisto.download_queue);
		
		mainListView.setAdapter(listAdapter);
		mainListView.setBackgroundColor(Callisto.RESOURCES.getColor(R.color.backClr));
		mainListView.setCacheColorHint(Callisto.RESOURCES.getColor(R.color.backClr));
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
			 Collections.swap(Callisto.download_queue,num,num-1);
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
			 if(num==0 || num==Callisto.download_queue.size()-1)
				 return;
			 Collections.swap(Callisto.download_queue,num,num+1);
			 listAdapter.notifyDataSetChanged();
		  }
    };

    /** Listener for the remove button ("x"). Removes a download from the list, and deletes it if it is the current download. */
    OnClickListener removeItem = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 int num = Integer.parseInt((String) tv.getText());
			 Callisto.download_queue.remove(num);
			 listAdapter.notifyDataSetChanged();
			 Callisto.downloading_count--;
		  }
    };

	
    /** Adapter for the downloads view; extended because we need to format the date */
    public class DownloadsAdapter extends BaseAdapter
    {
    	ArrayList<Long> downloadQueue;
    	public DownloadsAdapter(Context context, int textViewResourceId, ArrayList<Long> objects)
    	{
			super();
			downloadQueue = objects;
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
	    	
			Cursor c = Callisto.databaseConnector.getOneEpisode(downloadQueue.get(position));
			c.moveToFirst();
			
			String title = c.getString(c.getColumnIndex("title"));
			String show = c.getString(c.getColumnIndex("show"));
			String media_size = EpisodeDesc.formatBytes(c.getLong(c.getColumnIndex("mp3size")));	//IDEA: adjust for watch if needed
			((TextView)row.findViewById(R.id.hiddenId)).setText(Integer.toString(position));
			((TextView)row.findViewById(R.id.rowTextView)).setText(title);
			((TextView)row.findViewById(R.id.rowSubTextView)).setText(show);
			((TextView)row.findViewById(R.id.rightTextView)).setText(media_size);
			
			
		    ImageButton up = ((ImageButton)row.findViewById(R.id.moveUp));
		    up.setOnClickListener(moveUp);
		    ImageButton down = ((ImageButton)row.findViewById(R.id.moveDown));
		    down.setOnClickListener(moveDown);
		    ImageButton remove = ((ImageButton)row.findViewById(R.id.remove));
		    remove.setOnClickListener(removeItem);
			up.setEnabled(position>0);
			down.setEnabled(position>0);
			
    		return row;
    	}

		@Override
		public int getCount() {
			return downloadQueue.size();
		}

		@Override
		public Object getItem(int arg0) {
			return downloadQueue.get(arg0);
		}

		/** Not current used. */
		@Override
		public long getItemId(int arg0) {
			return 0;
		}
    }
}
