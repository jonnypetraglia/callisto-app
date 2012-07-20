package com.qweex.callisto;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ImageButton;



public class NowPlaying extends ListActivity
{
	private ListView mainListView;
	NowPlayingAdapter listAdapter;
	Cursor queue;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mainListView = getListView();
		TextView noResults = new TextView(this);
		noResults.setText("Playlist is empty");
		noResults.setTypeface(null, 2);
		noResults.setGravity(Gravity.CENTER_HORIZONTAL);
		noResults.setPadding(10,20,10,20);
		setTitle("Now Playing");
		((ViewGroup)mainListView.getParent()).addView(noResults);
		mainListView.setEmptyView(noResults);


		
		Callisto.databaseConnector.open();
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

        menu.add(0, Menu.FIRST, 0, "Clear");


        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	Callisto.databaseConnector.open();
    	Callisto.databaseConnector.clearQueue();
		listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
		Callisto.databaseConnector.close();
    	return true;
    }    
	
	OnClickListener moveUp = new OnClickListener() 
    {
		 @Override
		  public void onClick(View v) 
		  {
			 TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
			 long id = Long.parseLong((String) tv.getText());
			 if(id==1)
				 return;
			 Callisto.databaseConnector.swap(id, id-1);
			 
			 Callisto.databaseConnector.open();
			 listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
			 Callisto.databaseConnector.close();
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
			 if(id==1 || id==Callisto.databaseConnector.queueCount())
				 return;
			 Callisto.databaseConnector.swap(id, id+1);
			 
			 Callisto.databaseConnector.open();
			 listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
			 Callisto.databaseConnector.close();
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
			 
			 Callisto.databaseConnector.deleteQueueItem(id);
			 Callisto.databaseConnector.open();
			 listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
			 Callisto.databaseConnector.close();
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
	       Long identity = this.c.getLong(c.getColumnIndex("identity"));
	       
	       Callisto.databaseConnector.open();
	       Cursor c2 = Callisto.databaseConnector.getOneEpisode(identity);
	       c2.moveToFirst();
	       
	       String title = c2.getString(c2.getColumnIndex("title"));
		   String show = c2.getString(c2.getColumnIndex("show"));
		   String length = c2.getString(c2.getColumnIndex("mp3size"));
		   
		   ((TextView)v.findViewById(R.id.rowTextView)).setText(title);
		   ((TextView)v.findViewById(R.id.rowSubTextView)).setText(show);
		   //((TextView)v.findViewById(R.id.rightTextView)).setText(length);
		   ((TextView)v.findViewById(R.id.hiddenId)).setText(_id.toString());
		   
		   ImageButton up = ((ImageButton)v.findViewById(R.id.moveUp));
		   up.setOnClickListener(moveUp);
		   ImageButton down = ((ImageButton)v.findViewById(R.id.moveDown));
		   down.setOnClickListener(moveDown);
		   ImageButton remove = ((ImageButton)v.findViewById(R.id.remove));
		   remove.setOnClickListener(removeItem);
		   
		   for(int i=0; i<Hide.length; i++)
        	   ((View) v.findViewById(Hide[i])).setVisibility(View.GONE);
	       /*
	       int is_new = this.c.getInt(this.c.getColumnIndex("new"));
	       if(is_new>0)
	    	   ((ImageView)v.findViewById(R.id.newImg)).setImageDrawable(this.context.getResources().getDrawable(R.drawable.btn_rating_star_on_normal));
	       */
		   Callisto.databaseConnector.close();
	       return(v);
	     
	}
	
	}
}