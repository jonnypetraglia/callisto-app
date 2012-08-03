package com.qweex.callisto;

import java.text.ParseException;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

//This activity is for displaying the episodes of a show

//FEATURE: Ellipsisize list

public class ShowList extends Activity
{
	public static int currentShow = -1;			//This number is taken from AllShows
    public static View current_episode = null;		//This is for adjusting the status of an episode in OnResume
    
    //-----Local Variables-----
    private String feedURL;
    private ListView mainListView = null;
    private CursorAdapter showAdapter;
	private final String[] from = new String[] {"_id", "title", "date", "new"};
	private final int[] to = new int[] { R.id.hiddenId, R.id.rowTextView, R.id.rowSubTextView, R.id.rightTextView };
	private final int[] hide = new int[] { R.id.rightTextView, R.id.moveUp, R.id.moveDown, R.id.remove };
	private static final int RELOAD_ID=Menu.FIRST+1;
	private static final int CLEAR_ID=RELOAD_ID+1;
	private static final int FILTER_ID=CLEAR_ID+1;
	private static final int MARK_ID = FILTER_ID+1;
	
	private static TextView loading;
	private boolean filter;
	public SharedPreferences showSettings;
	
	//This constructor can be used to update a show without creating it as an activity.
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	    Log.v("ShowList:OnCreate", "Launching Activity");
	    mainListView = new ListView(this);
	    Callisto.build_layout(this, mainListView);
	    
	    //if(currentShow==-1)	//I don't know why I put this here.
	    	currentShow = getIntent().getExtras().getInt("current_show");
	    showSettings = getSharedPreferences(AllShows.SHOW_LIST[currentShow], 0);
		filter = showSettings.getBoolean("filter", false);
	    loading = (TextView) findViewById(android.R.id.empty);
	    if(Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter).getCount()==0)
	    	loading.setText(Callisto.RESOURCES.getString(R.string.list_empty));
	    else
	    	loading.setText(Callisto.RESOURCES.getString(R.string.loading));
	    loading.setGravity(Gravity.CENTER_HORIZONTAL);
	    mainListView.setEmptyView(loading);
	    mainListView.setOnItemClickListener(selectEpisode);
	    mainListView.setBackgroundColor(getResources().getColor(R.color.backClr));
	    mainListView.setCacheColorHint(getResources().getColor(R.color.backClr));
	    
	    setTitle(AllShows.SHOW_LIST[currentShow]);
	    if(AllShows.IS_VIDEO)
	    	feedURL = AllShows.SHOW_LIST_VIDEO[currentShow];
	    else
	    	feedURL = AllShows.SHOW_LIST_AUDIO[currentShow];	    
	    
	    new GetShowTask().execute((Object[]) null);
	    showAdapter = new ShowListCursorAdapter(ShowList.this, R.layout.row, null, from, to, hide);
	    mainListView.setAdapter(showAdapter);
		return;
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		setProgressBarIndeterminateVisibility(false);
		Callisto.playerInfo.update(ShowList.this);
		
		if(current_episode==null)
			return;
		
		long id = Long.parseLong(
				(String) ((TextView)((View) current_episode.getParent()).findViewById(R.id.hiddenId)).getText()
				);
		Cursor c = Callisto.databaseConnector.getOneEpisode(id);
		c.moveToFirst();
		boolean is_new = c.getInt(c.getColumnIndex("new"))>0;
		((CheckBox)((View) current_episode.getParent()).findViewById(R.id.img)).setChecked(is_new);
		current_episode=null;
	}
	
	@Override
	public void onStop()
	{
		//TODO: OnStop
		super.onStop();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	menu.add(0, RELOAD_ID, 0, Callisto.RESOURCES.getString(R.string.refresh)).setIcon(R.drawable.ic_menu_refresh);
    	menu.add(0, CLEAR_ID, 0, Callisto.RESOURCES.getString(R.string.clear)).setIcon(R.drawable.ic_menu_clear_playlist);
    	menu.add(0, FILTER_ID, 0, Callisto.RESOURCES.getString(filter ? R.string.unfilter : R.string.filter)).setIcon(R.drawable.ic_menu_zoom);
    	menu.add(0, MARK_ID, 0, Callisto.RESOURCES.getString(R.string.mark)).setIcon(R.drawable.ic_menu_mark);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	
        switch (item.getItemId())
        {
        case RELOAD_ID:
        	loading.setText(Callisto.RESOURCES.getString(R.string.loading));
        	setProgressBarIndeterminateVisibility(true);
        	new UpdateShowTask().execute((Object[]) null);
    	    new GetShowTask().execute((Object[]) null);
            return true;
        case CLEAR_ID:
        	new AlertDialog.Builder(this)
        	.setTitle(Callisto.RESOURCES.getString(R.string.confirm))
        	.setMessage(Callisto.RESOURCES.getString(R.string.confirm_clear))
        	.setIcon(android.R.drawable.ic_dialog_alert)
        	.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int whichButton) {
                	loading.setText(Callisto.RESOURCES.getString(R.string.list_empty));
                	Callisto.databaseConnector.clearShow(AllShows.SHOW_LIST[currentShow]);
                	Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
            	   	ShowList.this.showAdapter.changeCursor(r);
            	   	ShowList.this.showAdapter.notifyDataSetChanged();
            	   	SharedPreferences.Editor editor = showSettings.edit();
            	   	editor.putString("last_checked", null);
            	   	editor.commit();
        	    }})
        	 .setNegativeButton(android.R.string.no, null).show();
        	return true;
        case FILTER_ID:
        	filter = !filter;
        	if(filter)
        		item.setTitle(Callisto.RESOURCES.getString(R.string.unfilter));
        	else
        		item.setTitle(Callisto.RESOURCES.getString(R.string.filter));
    		Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
        	ShowList.this.showAdapter.changeCursor(r);
    	   	ShowList.this.showAdapter.notifyDataSetChanged();
    	   	SharedPreferences.Editor editor = showSettings.edit();
    	   	editor.putBoolean("filter", filter);
    	   	editor.commit();
        	return true;
        case MARK_ID:
        	new AlertDialog.Builder(this)
        	.setTitle(Callisto.RESOURCES.getString(R.string.mark_all))
        	.setPositiveButton(Callisto.RESOURCES.getString(R.string.new_), new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int whichButton) {
        	    	Callisto.databaseConnector.markNew(0, true);
                	Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
            	   	ShowList.this.showAdapter.changeCursor(r);
            	   	ShowList.this.showAdapter.notifyDataSetChanged();
        	    }})
        	 .setNegativeButton(Callisto.RESOURCES.getString(R.string.old), new DialogInterface.OnClickListener() {
         	    public void onClick(DialogInterface dialog, int whichButton) {
         	    	Callisto.databaseConnector.markNew(0, false);
                 	Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
             	   	ShowList.this.showAdapter.changeCursor(r);
             	   	ShowList.this.showAdapter.notifyDataSetChanged();
         	    }}).show();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	

	//This handler is for changing the cursor after an update; need this to perform tasks on UI elements outside of UI thread
	private Handler updateHandler = new Handler()
	{
        @Override
        public void handleMessage(Message msg)
        {
        	if(msg.arg1!=0)
        	{
	    	   	Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], filter);
	    	   	ShowList.this.showAdapter.changeCursor(r);
	    	   	ShowList.this.showAdapter.notifyDataSetChanged();
	    	   	Log.i("ShowList:handleMessage", "Changing cursor");
        	}
        	else
        	{
        		Log.i("ShowList:handleMessage", "Not Changing cursor");
        		loading.setText(Callisto.RESOURCES.getString(R.string.list_empty));
        	}
        	setProgressBarIndeterminateVisibility(false);
		}
	};

	
	
   // Updates the show outside of the UI thread
   private class UpdateShowTask extends AsyncTask<Object, Object, Object> 
   {
      @Override
      protected Object doInBackground(Object... params)
      {
		  return Callisto.updateShow(currentShow, showSettings, AllShows.IS_VIDEO);
      }
      
      @Override
      protected void onPostExecute(Object result)
      {
	  	  if(mainListView!=null)
	   		  ShowList.this.updateHandler.sendMessage((Message) result);
      }
   }
   
   // Gets episode of a show outside of the UI thread
   private class GetShowTask extends AsyncTask<Object, Object, Cursor> 
   {
      @Override
      protected Cursor doInBackground(Object... params)
      {
         return Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow], ShowList.this.filter);
      }

      @Override
      protected void onPostExecute(Cursor result)
      {
         showAdapter.changeCursor(result);
      }
   }
	
   //Listener for when an item is selected
	OnItemClickListener selectEpisode = new OnItemClickListener() 
    {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id)
      {
    	  current_episode = view;
    	  Intent viewEpisode = new Intent(ShowList.this, EpisodeDesc.class);
    	  String content = (String)((TextView)view.findViewById(R.id.hiddenId)).getText();
    	  long contentL = Long.parseLong(content);
    	  viewEpisode.putExtra("id", contentL);
    	  setProgressBarIndeterminateVisibility(true);
    	  startActivity(viewEpisode);
      }
    };
    
    //Listener for when an episode's "New" status is toggled
	public OnCheckedChangeListener toggleNew = new OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			//this is way too hard to read
			Callisto.databaseConnector.markNew(
					Long.parseLong((String)
							((TextView)((View) buttonView.getParent()).findViewById(R.id.hiddenId)).getText())
					, isChecked);
		}
	};
    
    //Adapter for the episode list
    public class ShowListCursorAdapter extends SimpleCursorAdapter
    {
        private Cursor c;
        private Context context;
        private String[] From;
        private int[] To;
        private int[] Hide;

    	public ShowListCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int[] hide) {
    		super(context, layout, c, from, to);
    		this.c = c;
    		this.context = context;
    		Hide = hide;
    		From = from;
    		To = to;
    	}

    	public View getView(int pos, View inView, ViewGroup parent) {
           View v = inView;
           if (v == null) {
                LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.row, null);
           }
    	   this.c = getCursor();
           this.c.moveToPosition(pos);

           Date tempDate = new Date();
           int thisYear = tempDate.getYear();
           //Set the data From->To
           for(int i=0; i<From.length; i++)
           {
        	   if(From[i].equals("date"))
        	   {
    			  String d = this.c.getString(this.c.getColumnIndex(From[i]));
    			  try {
    				  	tempDate = Callisto.sdfRaw.parse(d);
    				  	if(tempDate.getYear()==thisYear)
    				  		d = Callisto.sdfHuman.format(tempDate);
				  		else
				  			d = Callisto.sdfHumanLong.format(tempDate);
						//d = Callisto.sdfDestination.format();
					} catch (ParseException e) {
						Log.e("ShowList:ShowListAdapter:ParseException", "Error parsing a date from the SQLite db: ");
						Log.e("ShowList:ShowListAdapter:ParseException", d);
						Log.e("ShowList:ShowListAdapter:ParseException", "(This should never happen).");
						e.printStackTrace();
					}
    			  ((TextView) v.findViewById(To[i])).setText(d);
        	   }
        	   else
        		   ((TextView) v.findViewById(To[i])).setText(this.c.getString(this.c.getColumnIndex(From[i])));
           }
           //Hide the specific views
           for(int i=0; i<Hide.length; i++)
        	   ((View) v.findViewById(Hide[i])).setVisibility(View.GONE);
           
           boolean is_new = this.c.getInt(this.c.getColumnIndex("new"))>0;
           CheckBox rb = ((CheckBox)v.findViewById(R.id.img));
           rb.setChecked(is_new);
           rb.setOnCheckedChangeListener(toggleNew);
           return(v);
    	}
    }
}
