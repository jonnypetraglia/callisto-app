package com.qweex.callisto;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ImageButton;


public class Queue extends ListActivity
{
	private ListView mainListView;
	NowPlayingAdapter listAdapter;
	Cursor queue;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mainListView = getListView();
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

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, Menu.FIRST, 0, Callisto.RESOURCES.getString(R.string.clear));
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	Callisto.databaseConnector.clearQueue();
		listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
		ImageButton v = (ImageButton) findViewById(R.id.playPause);
		if(v!=null)
			((ImageButton)v).setImageDrawable(Callisto.playDrawable);
	    Callisto.playerInfo.isPaused = true;
	 	Callisto.mplayer.pause();
    	return true;
    }    
	
	OnClickListener moveUp = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 long id = Long.parseLong((String) tv.getText());
			 Callisto.databaseConnector.move(id, true);
			 
			 listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
			 //NowPlaying.this.listAdapter.notifyDataSetChanged();
		  }
    };
    
    OnClickListener moveDown = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 long id = Long.parseLong((String) tv.getText());
			 Callisto.databaseConnector.move(id, false);
			 
			 listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
			 //NowPlaying.this.listAdapter.notifyDataSetChanged();
		  }
    };

    OnClickListener removeItem = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 long id = Long.parseLong((String) tv.getText());
			 long id2=0;
			 Cursor c = Callisto.databaseConnector.currentQueue();
			 if(c.getCount()!=0)
			 {	c.moveToFirst();
			 	id2 = c.getLong(c.getColumnIndex("_id"));
			 }
			 if(id==id2)
			 {
				 boolean isPlaying = (Callisto.mplayer!=null && !Callisto.mplayer.isPlaying());
				 Callisto.playTrack(v.getContext(), 1, !Callisto.playerInfo.isPaused);
				 if(isPlaying)
				 {
				    ((ImageButton)v).setImageDrawable(Callisto.playDrawable);
				    Callisto.playerInfo.isPaused = true;
				 	Callisto.mplayer.pause();
				 }
				 Callisto.databaseConnector.advanceQueue(1);
			 }
			 
			 Callisto.databaseConnector.deleteQueueItem(id);
			 Cursor q = Callisto.databaseConnector.getQueue();
			 listAdapter.changeCursor(q);
			 
			 if(q.getCount()==0)
			 {
		    	Callisto.playerInfo.title = null;
		    	Callisto.playerInfo.show = null;
		    	Callisto.playerInfo.date = null;
		    	Callisto.playerInfo.position = 0;
		    	Callisto.playerInfo.length = 0;
			 }
			 //NowPlaying.this.listAdapter.notifyDataSetChanged();
			 
			 
		  }
    };
	
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
	            v = inflater.inflate(R.layout.row, null);
	       }
    	   this.c = getCursor();
	       this.c.moveToPosition(pos);
	       
	       Long _id = this.c.getLong(c.getColumnIndex("_id"));
	       boolean isCurrent = this.c.getLong(c.getColumnIndex("current"))>0;
	       Long identity = this.c.getLong(c.getColumnIndex("identity"));
	       if(isCurrent)
	    	   v.setBackgroundColor(getResources().getColor(R.color.SandyBrown));
	       else
	    	   v.setBackgroundColor(getResources().getColor(android.R.color.transparent));
	       
	       
	       Cursor c2 = Callisto.databaseConnector.getOneEpisode(identity);
	       c2.moveToFirst();
	       
	       String title = c2.getString(c2.getColumnIndex("title"));
		   String show = c2.getString(c2.getColumnIndex("show"));
		   
		   ((TextView)v.findViewById(R.id.rowTextView)).setText(title);
		   ((TextView)v.findViewById(R.id.rowSubTextView)).setText(show);
		   ((TextView)v.findViewById(R.id.hiddenId)).setText(_id.toString());
		   
		   ImageButton up = ((ImageButton)v.findViewById(R.id.moveUp));
		   up.setOnClickListener(moveUp);
		   ImageButton down = ((ImageButton)v.findViewById(R.id.moveDown));
		   down.setOnClickListener(moveDown);
		   ImageButton remove = ((ImageButton)v.findViewById(R.id.remove));
		   remove.setOnClickListener(removeItem);
		   
		   for(int i=0; i<Hide.length; i++)
			   ((View) v.findViewById(Hide[i])).setVisibility(View.GONE);
	       return(v);
	     
	}
	
	}
}