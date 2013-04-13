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

import com.andrefy.ddviewlist.DDListView;
import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ImageButton;

/** An activity for showing the queued episodes.
 * @author MrQweex */

public class Queue extends ListActivity
{
	private DDListView mainListView;
	NowPlayingAdapter listAdapter;
	Cursor queue;
	
	/** Called when the activity is first created. Sets up the view.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 */
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        mainListView = new DDListView(this, null);
        mainListView.setId(android.R.id.list);
        mainListView.setDropListener(mDropListener, R.id.grabber);
        setContentView(mainListView);


		//mainListView = getListView();
		mainListView.setBackgroundColor(getResources().getColor(R.color.backClr));
		TextView noResults = new TextView(this);
			noResults.setBackgroundColor(getResources().getColor(R.color.backClr));
			noResults.setTextColor(getResources().getColor(R.color.txtClr));
			noResults.setText(Callisto.RESOURCES.getString(R.string.list_empty));
			noResults.setTypeface(null, 2);
			noResults.setGravity(Gravity.CENTER_HORIZONTAL);
			noResults.setPadding(10,20,10,20);
		setTitle(Callisto.RESOURCES.getString(R.string.queue));
		((ViewGroup)mainListView.getParent()).addView(noResults);
		mainListView.setEmptyView(noResults);


		queue = Callisto.databaseConnector.getQueue();
		
		String[] from = new String[] {"_id", "identity"};
		int[] to = new int[] { R.id.hiddenId, R.id.rowTextView, R.id.rowSubTextView };
		int[] hide = new int[] { R.id.img, R.id.rightTextView };
		listAdapter = new NowPlayingAdapter(this, R.layout.row, queue, from, to, hide); 
		mainListView.setAdapter(listAdapter);
	}

	/** Called when it is time to create the menu. */
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, Menu.FIRST, 0, Callisto.RESOURCES.getString(R.string.clear)).setIcon(R.drawable.ic_action_trash);
        return true;
    }
	
	/** Called when an item is selected. */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case Menu.FIRST:
	    	Log.d("Queue:onOptionsItemSelected", "Clearing queue");
	    	if(Callisto.mplayer!=null)
		    	Callisto.mplayer.stop();
	    	Callisto.databaseConnector.clearQueue();
			listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
			Log.d("Queue:onOptionsItemSelected", "Cursor changed");
			updateNowPlaying(0);
		    Callisto.playerInfo.isPaused = true;
		 	Log.d("Queue:onOptionsItemSelected", "Derp");
	    	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }    
	
    /** Listener for the up button ("^"). Moves an entry up in the queue. */
	OnClickListener moveUp = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 long id = Long.parseLong((String) tv.getText());
			 Callisto.databaseConnector.move(id, 1);
			 updateNowPlaying(id);
			 
			 listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
			 //NowPlaying.this.listAdapter.notifyDataSetChanged();
		  }
    };
    
    /** Listener for the down button ("v"). Moves an entry down in the queue. */
    OnClickListener moveDown = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 long id = Long.parseLong((String) tv.getText());
			 Callisto.databaseConnector.move(id, -1);
			 updateNowPlaying(id);
			 
			 listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
		  }
    };
    
    void updateNowPlaying(long id)
    {
    	if(id==0)
    	{
    		Callisto.playerInfo.title = null;
    		Callisto.playerInfo.show = null;
    		Callisto.playerInfo.date = "";
    		Callisto.playerInfo.position = 0;
    		Callisto.playerInfo.length = 0;
    		return;
    	}
    	Cursor c2 = Callisto.databaseConnector.getOneEpisode(id);
    	c2.moveToFirst();
    	Callisto.playerInfo.title = c2.getString(c2.getColumnIndex("title"));
    	Callisto.playerInfo.show = c2.getString(c2.getColumnIndex("show"));
    	Callisto.playerInfo.date = c2.getString(c2.getColumnIndex("date"));
    	Callisto.playerInfo.position = c2.getInt(c2.getColumnIndex("position"));
    }

    /** Listener for the remove button ("x"). Moves an entry up in the queue. */
    OnClickListener removeItem = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 Log.d("Queue:removeItem", "Clicked to remove an item in the queue");
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 long id = Long.parseLong((String) tv.getText());
			 long current_id = id;
			 
			 Cursor c = Callisto.databaseConnector.currentQueue();
			 
			 if(c.getCount()==0)
				 updateNowPlaying(0);
			 else
			 {
				 c.moveToFirst();
				 current_id = c.getLong(c.getColumnIndex("_id"));
				 updateNowPlaying(c.getLong(c.getColumnIndex("identity")));
				 if(id==current_id)
				 {
					 Log.d("Queue:removeItem", "Removing the current item; advancing to next");
					 boolean isPlaying = (Callisto.mplayer!=null && !Callisto.mplayer.isPlaying());
					 Callisto.playTrack(v.getContext(), 1, !Callisto.playerInfo.isPaused);
					 if(isPlaying)
					 {
						 Log.d("Queue:removeItem", "Track is playing");
					    Callisto.playerInfo.isPaused = true;
					 }
					 
				 }
			 }
			 Callisto.databaseConnector.move(id, 0);
			 			 
			 listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
			 Log.d("Queue:removeItem", "Done");
	  }
    };
	
    /** An adapter for the Queue class. Used to update the queue from the SQLite database. */
	public class NowPlayingAdapter extends SimpleCursorAdapter
	{
	
	    private Cursor c;
	    private Context context;
	    String[] From;
	    int[] To;
	    int[] Hide;
	
		public NowPlayingAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int[] hide) {
			super(context, layout, c, from, to);
			this.c = c;
			this.context = context;
			From = from;
			To = to;
			Hide = hide;
		}
	
		public View getView(int pos, View inView, ViewGroup parent) {
	       View v = inView;


	       if (v == null) {
	            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            v = inflater.inflate(R.layout.row, parent, false);
	       }

           if(mainListView.mItemHeightNormal==-1)
           {
               try {
               v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
               mainListView.mItemHeightNormal = v.getMeasuredHeight();
               mainListView.mItemHeightHalf = mainListView.mItemHeightNormal / 2;
               mainListView.mItemHeightExpanded = mainListView.mItemHeightNormal * 2;
               mainListView.mItemColor = Callisto.RESOURCES.getColor(R.color.backClr);
               Log.d(":", "MEASURED");
               }catch(NullPointerException e){}
           }

    	   this.c = getCursor();
    	   if(this.c.getCount()==0)
    		   return v;
	       this.c.moveToPosition(pos);
	       
	       Long _id = this.c.getLong(c.getColumnIndex("_id"));
	       boolean isCurrent = this.c.getLong(c.getColumnIndex("current"))>0;
	       Long identity = this.c.getLong(c.getColumnIndex("identity"));
            /*
	       if(isCurrent)
	    	   v.setBackgroundColor(getResources().getColor(R.color.SandyBrown));
	       else
	    	   v.setBackgroundColor(getResources().getColor(android.R.color.transparent));
	    	   */
            (v.findViewById(R.id.row)).measure(0,0);
            int x =(v.findViewById(R.id.row)).getMeasuredHeight();
            Log.e("DERP", "IS: " + x);

            //Update the progressbar height
            try {
                Cursor time = Callisto.databaseConnector.getOneEpisode(identity);
                time.moveToFirst();
                int xy = (int) (time.getLong(time.getColumnIndex("position"))*100.0 / Callisto.databaseConnector.getLength(identity));
                ((ProgressBar)v.findViewById(R.id.progress)).setProgress(Double.isNaN(xy) ? 0 : xy);
            } catch(Exception e){
                ((ProgressBar)v.findViewById(R.id.progress)).setProgress(0);
            }
            ((ProgressBar)v.findViewById(R.id.progress)).getLayoutParams().height=x;
            ((ProgressBar)v.findViewById(R.id.progress)).setMinimumHeight(x);
            ((ProgressBar)v.findViewById(R.id.progress)).invalidate();

	       
	       Cursor c2 = Callisto.databaseConnector.getOneEpisode(identity);
	       if(c2.getCount()==0)
	    	   return v;
	       c2.moveToFirst();
	       
	       String title = c2.getString(c2.getColumnIndex("title"));
		   String show = c2.getString(c2.getColumnIndex("show"));
		   long position = c2.getLong(c2.getColumnIndex("position"));
		   
		   ((TextView)v.findViewById(R.id.rowTextView)).setText(title);
		   ((TextView)v.findViewById(R.id.rowSubTextView)).setText(show);
		   ((TextView)v.findViewById(R.id.hiddenId)).setText(_id.toString());
		   
		   ImageButton up = ((ImageButton)v.findViewById(R.id.moveUp));
		   up.setOnClickListener(moveUp);
		   ImageButton down = ((ImageButton)v.findViewById(R.id.moveDown));
		   down.setOnClickListener(moveDown);
		   ImageButton remove = ((ImageButton)v.findViewById(R.id.remove));
		   remove.setOnClickListener(removeItem);

           up.setVisibility(View.GONE);
           down.setVisibility(View.GONE);
		   for(int i=0; i<Hide.length; i++)
			   ((View) v.findViewById(Hide[i])).setVisibility(View.GONE);
	       return(v);
	     
	}
	
	}

    //Drop Listener
    private DDListView.DropListener mDropListener = new DDListView.DropListener() {
        public void drop(int from, int to)
        {
            //to-=(from<to?1:0);
            //derp.add(to, derp.remove(from));
            //mAdapter.notifyDataSetChanged();
        }
    };
}