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
package com.qweex.callisto;

//https://www.google.com/calendar/feeds/jalb5frk4cunnaedbfemuqbhv4@group.calendar.google.com/public/basic

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

//FEATURE: Calendar event notifications
//IDEA: Storing the date information in the "PDT" format so it will be correct when you move between timezones without having to refresh

public class CalendarActivity extends Activity {
	static long timeBetween;
	static View lastView;
	
	private final int REFRESH_MENU_ID = Menu.FIRST;
	
	Calendar current_cal = Calendar.getInstance();
	
	private TextView Tmonth;
	private TextView Tyear;
	private Button Bnext;
	private Button Bprev;
	String[] months;
	String[] days;
	LinearLayout[] dayViews = new LinearLayout[7];
	long agendaLastDay = Long.parseLong(Callisto.sdfRaw.format((new Date())));
	LinearLayout agenda;
	
	boolean stupidBug = !(Arrays.asList(TimeZone.getAvailableIDs()).contains("PDT"));
	boolean isLandscape = false;
	final long MILLISECONDS_IN_A_DAY=1000*60*60*24;
	private final static String CALENDAR_FEED_URL = "https://www.google.com/calendar/feeds/jalb5frk4cunnaedbfemuqbhv4@group.calendar.google.com/public/basic?singleevents=false&futureevents=true&&orderby=starttime&sortorder=a";
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(Callisto.RESOURCES.getString(R.string.agenda_title));
        
        isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
        
        months = Callisto.RESOURCES.getStringArray(R.array.months);
        days = Callisto.RESOURCES.getStringArray(R.array.days);
        
        if(!isLandscape)
        {
        	
        	Log.v("CalendarActivity:OnCreate", "Layout is portrait -> agenda");
        	setContentView(R.layout.agenda);
        	
        	agenda = (LinearLayout) findViewById(R.id.agenda);
        	Log.i("CalendarActivity:OnCreate", "Loading the first week on the agenda");
        	Button BloadMore = (Button) findViewById(R.id.loadmore);
        	loadMoreAgenda(true);
        	BloadMore.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					v.setEnabled(false);
					((Button)v).setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr) - 0x88000000);
					loadMoreAgenda(false);
					v.setEnabled(true);
					((Button)v).setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
				}
        		
        	});
        }else {
        	Log.v("CalendarActivity:OnCreate", "Layout is landscape -> calendar");
	        setContentView(R.layout.calendar);
	        
	        Tmonth = (TextView) findViewById(R.id.Tmonth);
	        Tyear = (TextView) findViewById(R.id.Tyear);
	        Bnext = (Button) findViewById(R.id.Bnext);
	        Bprev = (Button) findViewById(R.id.Bprev);
	        
	        
	        LinearLayout allCols = (LinearLayout) findViewById(R.id.allColumns);
	        for (int i=0; i < 7; i++)
	        {
	        	LinearLayout col = (LinearLayout) allCols.findViewWithTag("col");
	        	col.setTag(""); 
	    		dayViews[i] = (LinearLayout) col.findViewWithTag("day");
	    		dayViews[i].setTag("");
	        	
	        }
	        
	        Bnext.setOnClickListener(nextWeek);
	        Bprev.setOnClickListener(prevWeek);
	        Tmonth.setOnClickListener(resetCalender);
	        Tyear.setOnClickListener(resetCalender);
	        
	        
	        updateCalendar();
        
	        if ( savedInstanceState == null )
	    		Tmonth.performClick();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);
    	menu.add(Menu.NONE, REFRESH_MENU_ID, Menu.NONE, Callisto.RESOURCES.getString(R.string.refresh)).setIcon(R.drawable.ic_menu_refresh);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch (item.getItemId())
    	{
    	case REFRESH_MENU_ID:
    		new FetchEventsTask().execute((Object[]) null);
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
      
    /** Called when the user press the '>>' button. */
    public OnClickListener nextWeek = new OnClickListener()
    {
    	@Override
    	public void onClick(View v)
    	{
	    	current_cal.add(Calendar.DATE,7);
	    	updateCalendar();
    	}
    };

    /** Called when the user press the '<<' button. */
    public OnClickListener prevWeek = new OnClickListener()
    {
    	@Override
    	public void onClick(View v)
    	{
	    	current_cal.add(Calendar.DATE,-7);
	    	updateCalendar();
    	}
    };
    

    public OnClickListener resetCalender = new OnClickListener()
    {
    	@Override
    	public void onClick(View v)
    	{
            current_cal = Calendar.getInstance();
            updateCalendar();
    	}    	
    };
    
    // performs database query outside GUI thread
    private class FetchEventsTask extends AsyncTask<Object, Object, Object> 
    {
    	ProgressDialog pd;
    	
 	   @Override
 	   protected void onPreExecute()
 	   {
 	      super.onPreExecute();
 	      if(!isLandscape)
 	    	 agenda.removeAllViews();
 	      pd = ProgressDialog.show(CalendarActivity.this, Callisto.RESOURCES.getString(R.string.loading), Callisto.RESOURCES.getString(R.string.loading_msg), true, false);
 	   }
    	
       @Override
       protected Object doInBackground(Object... params)
       {
    	   try
    	   {
	       	Callisto.databaseConnector.clearCalendar();
	       	
	       	XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	       	factory.setNamespaceAware(true);
	   	    XmlPullParser xpp = factory.newPullParser();
	   	    URL url = new URL(CALENDAR_FEED_URL);
	   	    InputStream input = url.openConnection().getInputStream();
	   	    xpp.setInput(input, null);
	   	    Matcher m = null;
	   	    SimpleDateFormat sdf;
	   	    SimpleDateFormat sdfReplaces = new SimpleDateFormat("EE MMM dd, yyyy hha z", Locale.US);
	   	    SimpleDateFormat sdfReplaces2 = new SimpleDateFormat("EE MMM dd, yyyy hh:mma z", Locale.US);
	   	    SimpleDateFormat sdfRecurring = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz", Locale.US);
	   	    
	   	    int eventType = xpp.getEventType();
	   		  //Skip the first heading
	   		  while(!("entry".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
	   		  {
	   			  eventType = xpp.next();
	   			  if(eventType==XmlPullParser.END_DOCUMENT)
	   				  break;
	   		  }
	   		  eventType = xpp.next();
	   		
      		  while(eventType!=XmlPullParser.END_DOCUMENT)
	   		  {
      			  eventType = xpp.next(); //!REMOVE
	   			  //Title
	   			  while(!("title".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
	   			  {
	   				  eventType = xpp.next();
	   				  if(eventType==XmlPullParser.END_DOCUMENT)
	   					  break;
	   			  }
	   			//!
	   			  eventType = xpp.next();
	   			  String evShow = xpp.getText();
	   			  if(evShow==null)
	   				  break;
	   			  String evType = evShow.substring(0, evShow.indexOf(":")).trim();
	   			  evShow = evShow.substring(evShow.indexOf(":")+1, evShow.length()).trim();
	   			  
	   			  //Title
	   			  while(!("summary".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
	   			  {
	   				  eventType = xpp.next();
	   				  if(eventType==XmlPullParser.END_DOCUMENT)
	   					  break;
	   			  }
	   			  eventType = xpp.next();
	   			  String evDate = xpp.getText();
	   			  if(evDate==null)
	   				  break;
	   			  boolean evRecurring = evDate.startsWith("Recurring");
	   			  
	   			  if(!evRecurring)
	   			  {
	   				  m = (Pattern.compile(".*?When:(.*?)to.*?\n(.*?)<br>")).matcher(evDate);
	   				  if(evDate.contains(":"))
	   					  sdf = sdfReplaces2;
   					  else
	    				  sdf = sdfReplaces;
	   			  }
	   			  else
	   			  {
	   				  m = (Pattern.compile(".*?<br>\n*?First start:(.*?)\n*?<br>")).matcher(evDate);
	   				  sdf = sdfRecurring;
	   			  }
	   			  if(m.find())
	   				  if(m.groupCount()>1)
	   					  evDate = (m.group(1) + m.group(2)).trim();
	   				  else
	   					  evDate = m.group(1).trim();
	   			  else
	   				  continue;
	   			
	   			  Date tempDate = null;
	   			
	   			  
	   			try {
	   				tempDate = sdf.parse(evDate);
	   			} catch (ParseException e) {
	   				try {
	   					if(stupidBug)
	   						evDate = evDate.replace(" PDT", " PST8PDT");
   						tempDate = sdfRecurring.parse(evDate);
	   				} catch (ParseException e1) {
						// TODO EXCEPTION: ParseException
						e1.printStackTrace();
						continue;
	   				}
	   				 
	   			} 

	   			/*
				if(stupidBug && false)	//Can manually adjust the offset, but this is some bad mojo
				{
					  Calendar cal = Calendar.getInstance();
					  cal.setTime(butts);
					  cal.add(Calendar.HOUR, -2);
					  butts = cal.getTime();	    
				}//*/
				evDate = Callisto.sdfRaw.format(tempDate);
   				
	   			  String evTime =evDate.substring(8).trim();
	   			  evDate =  evDate.substring(0,8).trim(); 
	   				
	   			Callisto.databaseConnector.insertEvent(evShow, evType.trim(), evDate, evTime, evRecurring ? tempDate.getDay() : -1);
	   		  }
    	   } catch(XmlPullParserException e) {
    	     }catch (MalformedURLException e2) {
			e2.printStackTrace();
			}catch (IOException e2) {
       			// EXCEPTION
			e2.printStackTrace();	
			}
    	   catch(NullPointerException e)
  			{
  				Log.e("BUTTS", "HERE");
  			}
    	   
          return null;
       }

       @Override
       protected void onPostExecute(Object result)
       {
    	   pd.hide();
    	   if(isLandscape)
    		   Tmonth.performClick();
    	   else
    		   loadMoreAgenda(true);
       }
    }
    
    
    public void updateCalendar()
    {
        Tmonth.setText(months[current_cal.get(Calendar.MONTH)]);
        Tyear.setText(Integer.toString(current_cal.get(Calendar.YEAR)));
        Bprev.setEnabled(true);
        Bprev.setTextColor(Callisto.RESOURCES.getColor(R.color.calHdrClr));
        SimpleDateFormat sdfRawSimple = new SimpleDateFormat("yyyyMMdd");
        
        int dayOfWeek = current_cal.get(Calendar.DAY_OF_WEEK)-1;
        

        current_cal.add(Calendar.DATE,-1*dayOfWeek);
        
        for(int i=0; i<7; i++)
        {
        	int day = current_cal.get(Calendar.DAY_OF_MONTH);
        	if(day==1)
        	{
        		current_cal.add(Calendar.MONTH, 1);
        		Tmonth.setText(months[current_cal.get(Calendar.MONTH)]);
                Tyear.setText(Integer.toString(current_cal.get(Calendar.YEAR)));
                current_cal.add(Calendar.MONTH, -1);
        	}
        	((TextView)dayViews[i].findViewWithTag("dayText")).setText(Integer.toString(day));
        	if(Math.abs(current_cal.getTimeInMillis()-Calendar.getInstance().getTimeInMillis())<=MILLISECONDS_IN_A_DAY
        			&& current_cal.get(Calendar.DATE)==Calendar.getInstance().get(Calendar.DATE))
        	{
        		((TextView)dayViews[i].findViewWithTag("dayText")).setTextColor(0xFF000000 + Callisto.RESOURCES.getColor(R.color.calTodayClr));
        		Bprev.setEnabled(false);
        		Bprev.setTextColor(Callisto.RESOURCES.getColor(R.color.calHdrClr) - 0x88000000);
        	} else
    			((TextView)dayViews[i].findViewWithTag("dayText")).setTextColor(0xFF000000 + Callisto.RESOURCES.getColor(R.color.calDayClr));
        	
        	String s = sdfRawSimple.format(new Date());;
        	Cursor events = Callisto.databaseConnector.dayEvents(s, current_cal.get(Calendar.DAY_OF_WEEK)-1);
        	events.moveToFirst();
        	int j = 1;
        	while (events.isAfterLast() == false) 
        	{
        		String type = events.getString(events.getColumnIndex("type"));
        		String show = events.getString(events.getColumnIndex("show"));
        		String time = events.getString(events.getColumnIndex("time"));
        		
        		TextView event = (TextView) dayViews[i].findViewById(Callisto.RESOURCES.getIdentifier("event" + j, "id", "com.qweex.callisto"));
        		event.setText(show);
        		event.setBackgroundColor(Callisto.RESOURCES.getColor(type.equals("RELEASE") ? R.color.releaseClr : R.color.liveClr));
        		event.setVisibility(View.VISIBLE);
        		events.moveToNext();
        		j++;
        	}
        	
        	for(; j<=10; j++)
        	{
        		TextView event = (TextView) findViewById(Callisto.RESOURCES.getIdentifier("event" + j, "id", "com.qweex.callisto"));
        		event.setVisibility(View.GONE);
        	}
        	current_cal.add(Calendar.DATE,1);
        	//*/
        }
        current_cal.add(Calendar.DATE, -7 + dayOfWeek);
    }
    
    
    
    public void loadMoreAgenda(boolean thisWeek)
    {
    	
    	SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm aa");
    	int dayOfWeek = current_cal.get(Calendar.DAY_OF_WEEK)-1;
    	TextView loading = (TextView) findViewById(R.id.empty);
    	
    	
    	if(Callisto.databaseConnector.eventCount()==0)
    	{
    		loading.setText(Callisto.RESOURCES.getString(R.string.no_events));
    		return;
    	}
    	
    	
    	loading.setVisibility(View.VISIBLE);
    	loading.setText(Callisto.RESOURCES.getString(R.string.loading));
    	
    	//Loop over the 7 days
    	boolean foundEvents = false;
    	for(int i=0; i<7; i++)
    	{
    		
    		//Get the target date as a string: 20120123 for example
    		String targetDate="";
    		int temp;
			targetDate += Integer.toString(current_cal.get(Calendar.YEAR));
    		temp = current_cal.get(Calendar.MONTH);
    		if(temp<10)
    			targetDate+="0";
    		targetDate+=Integer.toString(temp);
			temp = current_cal.get(Calendar.DAY_OF_MONTH);
			if(temp<10)
				targetDate+="0";
			targetDate+=Integer.toString(temp);
			
    		
			
    		//Get a cursor containing the valid events for that day
    		Cursor c=null;
			c = Callisto.databaseConnector.dayEvents(targetDate, current_cal.get(Calendar.DAY_OF_WEEK)-1);
    		c.moveToFirst();
        	
    		//Add these events to the agenda
    		while(!c.isAfterLast())
    		{
    			foundEvents = true;
	        	LayoutInflater inflater = (LayoutInflater) CalendarActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        	View v = inflater.inflate(R.layout.agenda_row, null);
	        	
	        	Date eventDate = null;
		        try
		        {
		        	String d = c.getString(c.getColumnIndex("date"));
		        	String t = c.getString(c.getColumnIndex("time"));
		        	eventDate = Callisto.sdfRaw.parse(d+t);
		        } catch(Exception e)
		        {	e.printStackTrace();    }
		        
		        
		        Date today = new Date();
		        String timeUntil = "NOW!";
		        int timeBetween = 24*60;
		        if(thisWeek && i==0)
		        {
		        	int showTime = eventDate.getHours()*60 + eventDate.getMinutes();
			        int currentTime = today.getHours()*60 + today.getMinutes() - 60; //Subtract 60, so people can catch a show that's already in progress
			        timeBetween = showTime - currentTime;
			        if(timeBetween < 0)
			        {
			        	c.moveToNext();
			        	continue;
			        }
			        timeBetween-=60;
			        if(timeBetween>0)
			        	timeUntil = Callisto.formatTimeFromSeconds(timeBetween) + " left";
			        else
			        	timeUntil = "Now!";
		        }
		        
	        	//Date
		        if(thisWeek)
		        {
		        	if(i==0)
		        		((TextView) v.findViewById(R.id.date)).setText(Callisto.RESOURCES.getString(R.string.today));
		        	else
		        		((TextView) v.findViewById(R.id.date)).setText(days[(i+dayOfWeek)%7]);
		        }
		        else
		        	((TextView) v.findViewById(R.id.date)).setText(current_cal.get(Calendar.MONTH) + "/" + current_cal.get(Calendar.DATE));
		        
		        //Time
		        if(thisWeek && i==0 && timeBetween<=60*12)
		        {
		        	((TextView) v.findViewById(R.id.time)).setText(timeUntil);
		        	((TextView) v.findViewById(R.id.soon)).setText("SOON");
		        }
		        else
		        	((TextView) v.findViewById(R.id.time)).setText(sdfTime.format(eventDate));
		        //Show
		        ((TextView) v.findViewById(R.id.show)).setText(c.getString(c.getColumnIndex("show")));
		        //Type
		        ((TextView) v.findViewById(R.id.type)).setText(c.getString(c.getColumnIndex("type")));
		        agenda.addView(v);
		        
		         //*/
		        c.moveToNext();
    		}
    		current_cal.add(Calendar.DATE, 1);
    	}
    	if(!foundEvents)
    		loading.setText(Callisto.RESOURCES.getString(R.string.no_events));
    	else
    		loading.setVisibility(View.GONE);
    }
}