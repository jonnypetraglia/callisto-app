package com.qweex.callisto;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.sax.Element;
import android.sax.RootElement;
import android.view.LayoutInflater;
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

public class ShowList extends Activity
{
	public static int currentShow = 0;
    private String feedURL = "http://news.google.com/?output=rss";
    private String feedName = "Google!";
    public static View current_episode = null;
    
    private ArrayList<String> list;
    private ListView mainListView;
    public CursorAdapter showAdapter;	//TODO: private?
	RootElement root = new RootElement("rss");
    Element itemlist = root.getChild("channel");
    Element item =  itemlist.getChild("item");
    String[] suffixes = new String[] {"", "K", "M", "G", "T"};
   
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	    
	    mainListView = new ListView(this);
		Callisto.build_layout(this, mainListView);
	    
	    list = new ArrayList<String>();
	    currentShow = getIntent().getExtras().getInt("current_show");
	    this.setTitle(AllShows.SHOW_LIST[currentShow]);
	    if(AllShows.IS_VIDEO)
	    	feedURL = AllShows.SHOW_LIST_VIDEO[currentShow];
	    else
	    	feedURL = AllShows.SHOW_LIST_AUDIO[currentShow];	    
	    
		String[] from = new String[] {"_id", "title", "date", "new"};
		int[] to = new int[] { R.id.hiddenId, R.id.rowTextView, R.id.rowSubTextView, R.id.rightTextView };
		int[] hide = new int[] { R.id.rightTextView, R.id.moveUp, R.id.moveDown, R.id.remove };
	    showAdapter = new ShowListCursorAdapter(ShowList.this, R.layout.row, null, from, to, hide);
	    mainListView.setAdapter(showAdapter);
	    mainListView.setOnItemClickListener(selectEpisode);
	    new UpdateShowTask().execute((Object[]) null);
	    new GetShowTask().execute((Object[]) null);
		return;
	}
	
	Handler mHandler = new Handler()
	{
        @Override
        public void handleMessage(Message msg)
        {
    	   	Callisto.databaseConnector.open();
    	   	Cursor r = Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow]);
    	   	showAdapter.changeCursor(r);
    	   	Callisto.databaseConnector.close();
    		ShowList.this.showAdapter.notifyDataSetChanged();
		}
	};

	
	public void updateShow() throws ParseException
	{
		  String epDate = null;
		  SharedPreferences showSettings = getSharedPreferences(AllShows.SHOW_LIST[currentShow], 0);
		  String lastChecked = showSettings.getString("last_checked", null);
		  
		  String newLastChecked = null;
	   	  try
    	  {
    		  XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    		  factory.setNamespaceAware(true);
    		  XmlPullParser xpp = factory.newPullParser();
    		  URL url = new URL(feedURL);
    		  InputStream input = url.openConnection().getInputStream();
    		  xpp.setInput(input, null);
          
    		  int eventType = xpp.getEventType();
    		  
    		  //Skip the first heading
    		  while(!("item".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
    		  {
				  eventType = xpp.next();
				  if(eventType==XmlPullParser.END_DOCUMENT)
					  break;
			  }
    		  eventType = xpp.next();
    		  
    		  int x = 1;
    		  while(eventType!=XmlPullParser.END_DOCUMENT)
    		  {
				  //Title
				  while(!("title".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
				  {
					  eventType = xpp.next();
					  if(eventType==XmlPullParser.END_DOCUMENT)
						  break;
				  }
				  eventType = xpp.next();
				  String epTitle = xpp.getText();
				  if(epTitle==null)
					  break;
				  if(epTitle.indexOf("|")>0)
						epTitle = epTitle.substring(0, epTitle.indexOf("|")).trim();
				  
				  //Description
				  while(!("description".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
				  {
					  eventType = xpp.next();
					  if(eventType==XmlPullParser.END_DOCUMENT)
						  break;
				  }
				  eventType = xpp.next();
				  String epDesc = xpp.getText();
				  if(epDesc==null)
					  break;
				  
				  //Date
				  while(!("pubDate".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
				  {
					  eventType = xpp.next();
					  if(eventType==XmlPullParser.END_DOCUMENT)
						  break;
				  }
				  eventType = xpp.next();
				  epDate = xpp.getText();
				  
				  if(epDate==null)
					  break;
				  if(lastChecked!=null && !Callisto.sdfSource.parse(epDate).after(Callisto.sdfDestination.parse(lastChecked)))
					  break;
				  if(newLastChecked==null)
					  newLastChecked = epDate;
	
				  //Media link and size
				  while(!("enclosure".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
				  {
					  eventType = xpp.next();
					  if(eventType==XmlPullParser.END_DOCUMENT)
						  break;
				  }
				  
				  String epMediaLink = xpp.getAttributeValue(xpp.getNamespace(),"url");
				  if(epMediaLink==null)
					  break;
				  String epMediaSize = xpp.getAttributeValue(xpp.getNamespace(),"length");
				  if(epMediaSize==null)
					  break;
				  
				  
				  double temp =  Double.parseDouble(epMediaSize);
				  DecimalFormat twoDec = new DecimalFormat("0.00");
				  int i;
				  for(i=0; temp>5000; i++)
					  temp/=1024;
				  epMediaSize=twoDec.format(temp) + " " + suffixes[i] + "B";
				  
				  String epDate2 = Callisto.sdfRaw.format(Callisto.sdfSource.parse(epDate));
		    	  Callisto.databaseConnector.insertEpisode(AllShows.SHOW_LIST[currentShow], epTitle,
		    			  																	epDate2,
		    			  																	epDesc,
		    			  																	epMediaLink,
		    			  																	epMediaSize);
		    	  
		    	  
    		  }
  		   } catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
  			e.printStackTrace();
		   } catch (MalformedURLException e) {
			e.printStackTrace();
		   } catch (IOException e) {
				e.printStackTrace();
		   } catch (NullPointerException e) {
			   System.out.println("DONE ERROR1  " + e.getMessage());
		   } catch (ParseException e) {
		   	   System.out.println("DONE ERROR2");
		   }
	   	
	   	   if(newLastChecked!=null)
	   	   {
	   		   SharedPreferences.Editor editor = showSettings.edit();
	   		   editor.putString("last_checked", newLastChecked);
	   		   editor.commit();
	   	   }
 	  ShowList.this.mHandler.sendMessage(new Message());
	}
	@Override
	public void onResume()
	{
		super.onResume();
		setProgressBarIndeterminateVisibility(false);
		
		if(current_episode==null)
			return;
		
		String idS = (String) ((TextView)(current_episode.findViewById(R.id.hiddenId))).getText();
		Long id = Long.parseLong(idS);
		System.out.println("id=" + id);
		Callisto.databaseConnector.open();
		Cursor c = Callisto.databaseConnector.getQueue();
		System.out.println("count=" + c.getCount());
		Callisto.databaseConnector.close();
		c.moveToFirst();
		//boolean is_new = c.getInt(c.getColumnIndex("new"))>0;
		
		//((CheckBox)((View) current_episode.getParent()).findViewById(R.id.img)).setChecked(is_new);

		current_episode=null;
	}
	@Override
	public void onStop()
	{
		super.onStop();
	}
	
	
   // performs database query outside GUI thread
   private class UpdateShowTask extends AsyncTask<Object, Object, Object> 
   {
      @Override
      protected Object doInBackground(Object... params)
      {
    	  try {
			updateShow();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	  return null;
      }
   }
   // performs database query outside GUI thread
   private class GetShowTask extends AsyncTask<Object, Object, Cursor> 
   {
      @Override
      protected Cursor doInBackground(Object... params)
      {
         Callisto.databaseConnector.open();
         return Callisto.databaseConnector.getShow(AllShows.SHOW_LIST[currentShow]);
      }

      @Override
      protected void onPostExecute(Cursor result)
      {
         showAdapter.changeCursor(result); // set the adapter's Cursor
         Callisto.databaseConnector.close();
      }
   }
	
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
    
    
    
    
    public class ShowListCursorAdapter extends SimpleCursorAdapter
    {

        private Cursor c;
        private Context context;
        String[] From;
        int[] To;
        int[] Hide;
        boolean hideLast, hideButtons;

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
           //if(c==null)
        	   this.c = getCursor();
           this.c.moveToPosition(pos);

           
           for(int i=0; i<From.length; i++)
           {
        	   if(From[i].equals("date"))
        	   {
        		  SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss");
    			  SimpleDateFormat sdfDestination = new SimpleDateFormat("MM/dd/yyyy");
    			  String d = this.c.getString(this.c.getColumnIndex(From[i]));
    			  try {
						d = sdfDestination.format(sdfRaw.parse(d));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			  ((TextView) v.findViewById(To[i])).setText(d);
        	   }
        	   else
        	   {
        		   System.out.println(From[i] + "==" + this.c.getString(this.c.getColumnIndex(From[i])));
        		   ((TextView) v.findViewById(To[i])).setText(this.c.getString(this.c.getColumnIndex(From[i])));
        	   }
           }
           for(int i=0; i<Hide.length; i++)
        	   ((View) v.findViewById(Hide[i])).setVisibility(View.GONE);
           
           boolean is_new = this.c.getInt(this.c.getColumnIndex("new"))>0;
           CheckBox rb = ((CheckBox)v.findViewById(R.id.img));
           rb.setChecked(is_new);
           rb.setOnCheckedChangeListener(toggleNew);
           
           return(v);
    	}
    }
    
	public OnCheckedChangeListener toggleNew = new OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			Callisto.databaseConnector.markNew(
					Long.parseLong((String)
							((TextView)((View) buttonView.getParent()).findViewById(R.id.hiddenId)).getText())
					, isChecked);
		}
	};
}
