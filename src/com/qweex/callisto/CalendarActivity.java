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
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.qweex.utils.NumberPicker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

//CLEAN: popup is incorrect margins for on top

/** Displays upcoming shows for Jupiter Broadcasting. A vertical orientation displays an agenda view,
 * and a horizontal orientation displays a weekly view.
 * @author MrQweex*/

public class CalendarActivity extends Activity {
	private final int REFRESH_MENU_ID = Menu.FIRST;
	private final static String CALENDAR_FEED_URL = "https://www.google.com/calendar/feeds/jalb5frk4cunnaedbfemuqbhv4@group.calendar.google.com/public/basic?singleevents=false&futureevents=true&&orderby=starttime&sortorder=a";
	private final long MILLISECONDS_IN_A_DAY=1000*60*60*24;
	private TextView Tmonth;
	private TextView Tyear;
	private Button Bnext;
	private Button Bprev;
	private String[] months;
	private String[] days;
	private LinearLayout[] dayViews = new LinearLayout[7];
	private LinearLayout agenda;
	private Dialog dg;
	private PopupWindow popUp;
	private Calendar current_cal = Calendar.getInstance();
	private boolean isLandscape = false;
	private TextView popUp_title, popUp_type, popUp_date, popUp_time;
	private ProgressDialog pd;
	private FetchEventsTask fetchTask;
	private static Drawable popup_above;
	
	boolean stupidBug = !(Arrays.asList(TimeZone.getAvailableIDs()).contains("PDT"));
	int TheHeightOfTheFreakingPopup = 0;
	
    
	/** Called when the activity is first created. Determines the orientation of the screen then initializes the appropriate view.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 *  */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(Callisto.RESOURCES.getString(R.string.agenda_title));
        
        
        //TODO: flip
        popup_above = getResources().getDrawable(R.drawable.popup_inline_error);
        
        isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
        
        months = Callisto.RESOURCES.getStringArray(R.array.months);
        days = Callisto.RESOURCES.getStringArray(R.array.days);
        
    //Set up the pop up for viewing an event's info
    	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View popupV = inflater.inflate(R.layout.event_info, null, false);
		popUp = new PopupWindow(popupV);
		popUp.setWindowLayoutMode(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		popUp_title = (TextView) popupV.findViewById(R.id.title);
		popUp_type = (TextView) popupV.findViewById(R.id.type);
		popUp_date = (TextView) popupV.findViewById(R.id.date);
		popUp_time = (TextView) popupV.findViewById(R.id.time);
		((Button) popupV.findViewById(R.id.editEvent)).setOnClickListener(editEvent);
		
   //Weekly View
        if(!isLandscape)
        {
        	
        	Log.v("CalendarActivity:OnCreate", "Layout is portrait -> agenda");
        	setContentView(R.layout.agenda);
        	
        	agenda = (LinearLayout) findViewById(R.id.agenda);
        	Log.i("CalendarActivity:OnCreate", "Loading the first week on the agenda");
        	
        	
			(findViewById(R.id.scrollView1)).setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
			        popUp.dismiss();
			        return false;
				}
			});

			
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
	//Agenda View
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
	
	/** Called when any key is pressed. Used do dismiss the event info popup when you press back.
	 * @param keyCode I dunno
	 * @param event I dunno
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

	    // Override back button
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	if(pd!=null && pd.isShowing())
	    	{
	    		fetchTask.cancel(true);
	    		pd.cancel();
	    	}
	        if (popUp.isShowing()) {
	        	popUp.dismiss();
	            return false;
	        }
	    }
	    return super.onKeyDown(keyCode, event);
	} 
	
	/** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
	@Override
	public void onResume()
	{
		super.onResume();
		
		if(!isLandscape)
			 findViewById(R.id.mainAgenda).post(new Runnable() {
				   public void run() {
					   popUp.showAsDropDown(agenda);
					   popUp.dismiss();
				   }
				});

	}
	
	/** Called when it is time to create the menu.
	 * @param menu Um, the menu
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);
    	menu.add(Menu.NONE, REFRESH_MENU_ID, Menu.NONE, Callisto.RESOURCES.getString(R.string.reload)).setIcon(R.drawable.ic_action_reload);
    	return true;
    }
    
    /** Called when an item in the menu is pressed.
	 * @param item The menu item ID that was pressed
	 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch (item.getItemId())
    	{
    	case REFRESH_MENU_ID:
    		fetchTask = new FetchEventsTask();
    		fetchTask.execute((Object[]) null);
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
    
    /** Resets the calendar to the current date. */
    public OnClickListener resetCalender = new OnClickListener()
    {
    	@Override
    	public void onClick(View v)
    	{
            current_cal = Calendar.getInstance();
            updateCalendar();
    	}    	
    };
    
    /** Calculates the difference between Jupiter time and local time.
     * http://obscuredclarity.blogspot.com/2010/08/determine-time-difference-between-two.html **/
    private int timezoneDifference()
    {
    	int hourDifference, dayDifference;
    	
    	Calendar Jupiter = new java.util.GregorianCalendar(TimeZone.getTimeZone("PST8PDT"));
		Jupiter.setTimeInMillis(new java.util.Date().getTime());
		int JupiterHourOfDay = Jupiter.get(Calendar.HOUR_OF_DAY);
		int JupiterDayOfMonth = Jupiter.get(Calendar.DAY_OF_MONTH);
    	
    	// Local Time  
    	int localHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);  
    	int localDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);  
    	  
    	// Difference between New York and Local Time (for me Germany)  
    	hourDifference = JupiterHourOfDay - localHourOfDay;  
    	dayDifference = JupiterDayOfMonth - localDayOfMonth;  
    	if (dayDifference != 0) 
    		hourDifference = hourDifference + 24;    
    	return hourDifference;
    }
    
    /** Performs database query outside GUI thread. */
    private class FetchEventsTask extends AsyncTask<Object, Object, Object> 
    {
    	
 	   @Override
 	   protected void onPreExecute()
 	   {
 	      super.onPreExecute();
 	      if(!isLandscape)
 	    	 agenda.removeAllViews();
 	      pd = ProgressDialog.show(CalendarActivity.this, Callisto.RESOURCES.getString(R.string.loading), Callisto.RESOURCES.getString(R.string.loading_msg), true, false);
 	      pd.setCancelable(true);
 	   }
    	
       @Override
       protected Object doInBackground(Object... params)
       {
    	   try
    	   {  	
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
	   	    
	   	    Callisto.databaseConnector.clearCalendar();
	   	    
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
	   			
	   			  Calendar tempDate = Calendar.getInstance();
	   			
	   			  
	   			try {
	   				tempDate.setTime(sdf.parse(evDate));
	   			} catch (ParseException e) {
	   				try {
	   					if(stupidBug)
	   						evDate = evDate.replace(" PDT", " PST8PDT");
   						tempDate.setTime(sdfRecurring.parse(evDate));
   						e.printStackTrace();
	   				} catch (ParseException e1) {
						// TODO EXCEPTION: ParseException
						e1.printStackTrace();
						continue;
	   				}
	   			} 

	   			
				evDate = Callisto.sdfRaw.format(tempDate.getTime());
   				
	   			String evTime =evDate.substring(8).trim();
	   			evDate =  evDate.substring(0,8).trim();
	   			  
	   			System.out.println("TZD: " + tempDate.get(Calendar.DAY_OF_WEEK) + " " + evType.trim());

                 //Chriiiiiiis fix this
                 if(evShow.equals("The Linux Action Show!"))
                     evShow = "Linux Action Show";
	   				
	   			Callisto.databaseConnector.insertEvent(evShow, evType.trim(), evDate, evTime, evRecurring ? tempDate.get(Calendar.DAY_OF_WEEK) : -1);
	   		  }
    	   } catch(XmlPullParserException e) {
    		   e.printStackTrace();
    		   return "XML";
    	     }catch (MalformedURLException e2) {
    	    	 e2.printStackTrace();
    	    	 return "URL";
			}catch (IOException e2) {
				e2.printStackTrace();	
				return "I/O";
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
    	   if(result!=null)
    		   Toast.makeText(CalendarActivity.this, (String)(result) + " occurred. Maybe your connection might be flaky?", Toast.LENGTH_LONG).show();
    	   else {
    		   if(isLandscape)
    			   Tmonth.performClick();
    		   else
               {
                   ((Button) findViewById(R.id.loadmore)).setText(Callisto.RESOURCES.getString(R.string.load_more));
    			   loadMoreAgenda(true);
               }
    	   }
       }
    }
    
    /** Updates the Weekly view after adjusting the week. */
    public void updateCalendar()
    {
        Tmonth.setText(months[current_cal.get(Calendar.MONTH)]);
        Tyear.setText(Integer.toString(current_cal.get(Calendar.YEAR)));
        Bprev.setEnabled(true);
        Bprev.setTextColor(Callisto.RESOURCES.getColor(R.color.calHdrClr));
        
        int dayOfWeek = current_cal.get(Calendar.DAY_OF_WEEK)-1;
        
        Calendar today = Calendar.getInstance();
        
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
        	{
    			((TextView)dayViews[i].findViewWithTag("dayText")).setTextColor(0xFF000000 + Callisto.RESOURCES.getColor(R.color.calDayClr));
        	
	        	if(current_cal.before(today))
	        	{
	        		current_cal.add(Calendar.DATE,1);
	            	for(int j=1; j<=10; j++)
	            	{
	            		TextView event = (TextView) dayViews[i].findViewById(Callisto.RESOURCES.getIdentifier("event" + j, "id", "com.qweex.callisto"));
	            		if(event==null)
	            			continue;
	            		event.setVisibility(View.GONE);
	            	}
	        		continue;
	        	}
        	}
        	
        	String s = Callisto.sdfRawSimple1.format(current_cal.getTime());
        	Cursor events = Callisto.databaseConnector.dayEvents(s, current_cal.get(Calendar.DAY_OF_WEEK)-1);
        	events.moveToFirst();
        	int j = 1;
        	
        	while (events.isAfterLast() == false) 
        	{
        		String type = events.getString(events.getColumnIndex("type"));
        		String show = events.getString(events.getColumnIndex("show"));
        		String _id  = events.getString(events.getColumnIndex("_id")); 
        		
        		
        		TextView event = (TextView) dayViews[i].findViewById(Callisto.RESOURCES.getIdentifier("event" + j, "id", "com.qweex.callisto"));
        		View w = (View) event.getParent();
        		TextView id = (TextView) w.findViewById(Callisto.RESOURCES.getIdentifier("id", "id", "com.qweex.callisto"));
        		id.setText(_id);
        		
        		event.setText(show);
        		event.setBackgroundColor(Callisto.RESOURCES.getColor(type.equals("RELEASE") ? R.color.releaseClr : R.color.liveClr));
        		event.setVisibility(View.VISIBLE);
        		event.setOnClickListener(clickEvent);
        		
        		events.moveToNext();
        		j++;
        	}
        	for(; j<=10; j++)
        	{
        		TextView event = (TextView) dayViews[i].findViewById(Callisto.RESOURCES.getIdentifier("event" + j, "id", "com.qweex.callisto"));
        		if(event==null)
        			continue;
        		event.setVisibility(View.GONE);
        	}
        	current_cal.add(Calendar.DATE,1);
        	
        }
        current_cal.add(Calendar.DATE, -7 + dayOfWeek);
    }
    
    /** Called when the user the presses the "Edit" button for an existing alarm. */
    public OnClickListener editEvent = new OnClickListener() {
		@Override
		public void onClick(View v) {
			LayoutInflater inflater = (LayoutInflater) CalendarActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	View vi = inflater.inflate(R.layout.edit_alarm, null);
			dg = new Dialog(CalendarActivity.this);
			dg.setContentView(vi);
			if(isLandscape)
				((LinearLayout)vi.findViewById(R.id.landscapeRotate)).setOrientation(LinearLayout.HORIZONTAL);
			dg.setTitle("Set Alarm/Notification");
			dg.show();
			
			NumberPicker NumP = (NumberPicker) vi.findViewById(R.id.minutesBefore);
			NumP.setSuffix(" min before");
			NumP.setIncrement(15);
			NumP.setRange(0, 300);
			
			RingtoneManager mRingtoneManager2 = new RingtoneManager(CalendarActivity.this);
		    mRingtoneManager2.setType(RingtoneManager.TYPE_RINGTONE);
		    mRingtoneManager2.setIncludeDrm(true);
		    Cursor mCursor2 = mRingtoneManager2.getCursor();
		    startManagingCursor(mCursor2);
		    String[] from = {mCursor2.getColumnName(RingtoneManager.TITLE_COLUMN_INDEX), mCursor2.getColumnName(RingtoneManager.ID_COLUMN_INDEX), mCursor2.getColumnName(RingtoneManager.URI_COLUMN_INDEX)};
		    int[] to = {R.id.text1, R.id.id1, R.id.uri};
		    SimpleCursorAdapter adapter = new SimpleCursorAdapter(CalendarActivity.this, R.layout.simple_spinner_item_plus, mCursor2, from, to );
		    adapter.setDropDownViewResource( R.layout.simple_spinner_item_plus );
		    Spinner s = (Spinner) vi.findViewById( R.id.spinner1);
		    s.setAdapter(adapter);
			
			//Set the previous if it exists
			String key = (String) ((TextView) popUp.getContentView().findViewById(R.id.key)).getText();
			key = Callisto.alarmPrefs.getString(key, "");
			String tone = "";
			int min = 0;
			boolean vibrate=false, isAlarm=false;
			if(key!="")
			{
				try {
				min = Integer.parseInt(key.substring(0, key.indexOf("_")));
				tone = key.substring(key.indexOf("_")+1,key.lastIndexOf("_"));
				int i = Integer.parseInt(key.substring(key.lastIndexOf("_")+1));
				isAlarm=i>10?true:false;
				vibrate= i%2!=0?true:false;
				((NumberPicker) vi.findViewById(R.id.minutesBefore)).setValue(min);
				Cursor c = mRingtoneManager2.getCursor();
				c.moveToFirst();
				i=1;
				while(c.moveToNext())
				{
					int x = c.getInt(RingtoneManager.ID_COLUMN_INDEX);
					System.out.println(x + "==" + tone);
					if(tone.endsWith(Integer.toString(x)))
					{
						s.setSelection(i);
						break;
					}
					i++;
				}
				((RadioButton) vi.findViewById(R.id.isAlarm)).setChecked(isAlarm);
				((CheckBox) vi.findViewById(R.id.vibrate)).setChecked(vibrate);
				} catch(Exception e) {
					System.out.println("crap" + key);
					e.printStackTrace();
				}
			}
		    
		    ((Button)vi.findViewById(R.id.save)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					View x = (View) v.getParent();
					
					Spinner s = (Spinner) x.findViewById( R.id.spinner1);
					String tone = (String) ((TextView)(s.getSelectedView()).findViewById(R.id.id1)).getText();
					String uri = (String) ((TextView)(s.getSelectedView()).findViewById(R.id.uri)).getText();
					String key = (String) ((TextView) popUp.getContentView().findViewById(R.id.key)).getText();
					int min = ((NumberPicker) x.findViewById(R.id.minutesBefore)).getValue();
					int isAlarm = ((RadioButton) x.findViewById(R.id.isAlarm)).isChecked()?1:0;
					int vibrate = ((CheckBox) x.findViewById(R.id.vibrate)).isChecked()?1:0;
					
					Calendar time = Calendar.getInstance();
					try {
						time.setTime(Callisto.sdfRaw.parse(key.substring(key.length()-14)));
						time.add(Calendar.MINUTE, -1*min);
					} catch (ParseException e) {}
					
					
					Intent i = new Intent(CalendarActivity.this, AlarmNotificationReceiver.class);
				    i.putExtra("tone", uri + "/" + tone);
				    i.putExtra("min", min);
				    i.putExtra("isAlarm", isAlarm);
				    i.putExtra("vibrate", vibrate);
				    i.putExtra("show", key.substring(0, key.length()-14));
				    i.putExtra("key", key);
				    Toast.makeText(CalendarActivity.this, "Creating event...", Toast.LENGTH_SHORT).show();
				    PendingIntent pi = PendingIntent.getBroadcast(CalendarActivity.this.getApplicationContext(), 234324246, i, PendingIntent.FLAG_UPDATE_CURRENT);
				    AlarmManager mAlarm = (AlarmManager) CalendarActivity.this.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
				    //mAlarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3*1000, pi); //DEBUG
				    mAlarm.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pi);
				    
					
					
					SharedPreferences.Editor edit = Callisto.alarmPrefs.edit();
					edit.putString(key, Integer.toString(min) + "_" + uri + "/" + tone + "_" + isAlarm + "" + vibrate);
					edit.commit();
					dg.dismiss();
					popUp.dismiss();
				}
		    });
			
		}
		
	};
    
    /** Listener for play/pause button. */
	public OnClickListener clickEvent = new OnClickListener() 
    {
		@Override
		  public void onClick(View v) 
		  {
			Log.v("CalendarActivity:clickEvent", "Event has been clicked");
			if(popUp.isShowing())
				popUp.dismiss();
			int[] pos = new int[2];
			v.getLocationOnScreen(pos);
			View w = (View) (isLandscape? v.getParent() : v);
			String id = (String) ((TextView)w.findViewById(R.id.id)).getText();
			if(id.equals(""))
				return;
			long _id = 0;
			try {
				_id = Long.parseLong(id);
			} catch(NumberFormatException e)
			{
				return;
			}
			
			Cursor c = Callisto.databaseConnector.getOneEvent(_id);
			if(!c.moveToFirst())
				return;
			String s = c.getString(c.getColumnIndex("show"));
			String d = "", t = "";
			popUp_title.setText(s);
			String title = s;
			s = c.getString(c.getColumnIndex("type"));
			popUp_type.setText(s);
			try {
				if(isLandscape)
				{
					w = (View) w.getParent();
					String day = (String) ((TextView)w.findViewById(R.id.theDay)).getText();
					String month = (String) ((TextView)findViewById(R.id.Tmonth)).getText();
					int i;
					for(i=0; i<months.length; i++)
						if(month.equals(months[i]))
							break;
					month = Integer.toString(i+1); 
					String year = (String) ((TextView)findViewById(R.id.Tyear)).getText();
					d = Callisto.sdfRawSimple1.format(Callisto.sdfDestination.parse(month + "/" + day + "/" + year));
				}
				else
				{
					//Agenda view
					d = (String) ((TextView)v.findViewById(R.id.date)).getText();
					// MM/dd format
					if(d.contains("/"))
					{
						SimpleDateFormat sdf = new SimpleDateFormat(Callisto.europeanDates ? "dd/MM" : "MM/dd");
						if(d.indexOf("/", d.indexOf("/")+1)>0)
						{
							sdf = Callisto.sdfDestination;
							System.out.println("PARSEDERP" + d.indexOf("/", d.indexOf("/")) + " " +  d.indexOf("/"));
						}
						Calendar dt = Calendar.getInstance();
						System.out.println("PARSE " + (Callisto.europeanDates ? "dd/MM" : "MM/dd") + " - "  + d);
						dt.setTime(sdf.parse(d));
						dt.set(Calendar.YEAR, (new java.util.Date()).getYear());
						d = Callisto.sdfRawSimple1.format(dt.getTime());
					}
					//Today
					else if(d.equals(Callisto.RESOURCES.getString(R.string.today)))
						d = Callisto.sdfRawSimple1.format(new java.util.Date());
					else
						//Mon Tue Wed
						for(int i=1; i<=days.length; i++)
							if(d.equals(days[i-1]))
							{
								i=(i-1+7)%7;
								Calendar cl = Calendar.getInstance();
								int j = (cl.get(Calendar.DAY_OF_WEEK));// % 7;
								System.out.println(j + "<<" + i);
								if(j<=i)
									cl.add(Calendar.DATE, i-j);
								else
									cl.add(Calendar.DATE, 7+i-j);
								d = Callisto.sdfRawSimple1.format(cl.getTime());
								break;
							}
				}
				
				s = Callisto.sdfDestination.format(Callisto.sdfRawSimple1.parse(d));
				popUp_date.setText(s);
				//Might just want to read this from the UI
				t = c.getString(c.getColumnIndex("time"));
				s = Callisto.sdfTime.format(Callisto.sdfRawSimple2.parse(t));
				popUp_time.setText(s);
			} catch (ParseException e) {
				System.out.println("PARSEEXCEPTION");
			}
			
			Map<String,?> alarms = Callisto.alarmPrefs.getAll();
			String key = title + d + t;
			((TextView) popUp.getContentView().findViewById(R.id.key)).setText(key);
			if(alarms.containsKey(popUp_title))
			{
				popUp.getContentView().findViewById(R.id.hasAlarm).setVisibility(View.VISIBLE);
				((TextView)popUp.getContentView().findViewById(R.id.alarmInfo)).setText("Alarm set for this show");
				((Button)popUp.getContentView().findViewById(R.id.editEvent)).setText("Edit");
			}
			else if(alarms.containsKey(key))
			{
				popUp.getContentView().findViewById(R.id.hasAlarm).setVisibility(View.VISIBLE);
				((TextView)popUp.getContentView().findViewById(R.id.alarmInfo)).setText("Alarm set for just this episode");
				((Button)popUp.getContentView().findViewById(R.id.editEvent)).setText("Edit");
			}
			else
			{
				popUp.getContentView().findViewById(R.id.hasAlarm).setVisibility(View.INVISIBLE);
				((TextView)popUp.getContentView().findViewById(R.id.alarmInfo)).setText("No alarm set");
				((Button)popUp.getContentView().findViewById(R.id.editEvent)).setText("Add");
			}
			
		    popUp.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		    TheHeightOfTheFreakingPopup = popUp.getContentView().getMeasuredHeight();
			boolean isLowerHalf = pos[1]>getWindowManager().getDefaultDisplay().getHeight()/2;
			Log.v("CalendarActivity:clickEvent", "Height of the freaking popup: " + TheHeightOfTheFreakingPopup);
			if(isLandscape)
			{
				popUp.getContentView().findViewById(R.id.thing).setBackgroundDrawable(getResources().getDrawable(R.drawable.spinner_ab_focused_holo_light_none));
				int y = getWindowManager().getDefaultDisplay().getHeight()/2;
				popUp.showAsDropDown(findViewById(R.id.linearLayout1), 0, (y-TheHeightOfTheFreakingPopup)/2);
			}
			else if(isLowerHalf)
			{
				popUp.getContentView().findViewById(R.id.thing).setBackgroundDrawable(getResources().getDrawable(R.drawable.spinner_ab_focused_holo_light));
				popUp.showAsDropDown(v, 0, -1*v.getHeight() - TheHeightOfTheFreakingPopup - (isLowerHalf ? 0 : 40));
			}
			else
			{
				popUp.getContentView().findViewById(R.id.thing).setBackgroundDrawable(getResources().getDrawable(R.drawable.spinner_ab_focused_holo_light_flip));
				popUp.showAsDropDown(v, 0, -5);
			}
		  }
    };
    
    
    /** Loads 7 more days worth of events into the Agenda view */
    public void loadMoreAgenda(boolean thisWeek)
    {
    	
    	int dayOfWeek = current_cal.get(Calendar.DAY_OF_WEEK)-1;
    	TextView loading = (TextView) findViewById(R.id.empty);
    	
    	
    	if(Callisto.databaseConnector.eventCount()==0)
    	{
    		if(thisWeek)
    		{
	    		loading.setText(Callisto.RESOURCES.getString(R.string.no_events));
	    		((Button) findViewById(R.id.loadmore)).setText(Callisto.RESOURCES.getString(R.string.fetch));
	    		return;
    		}
    		fetchTask = new FetchEventsTask();
    		fetchTask.execute((Object[]) null);
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
			c = Callisto.databaseConnector.dayEvents(targetDate, current_cal.get(Calendar.DAY_OF_WEEK));
    		c.moveToFirst();
        	
    		//Add these events to the agenda
    		while(!c.isAfterLast())
    		{
    			foundEvents = true;
	        	LayoutInflater inflater = (LayoutInflater) CalendarActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        	View v = inflater.inflate(R.layout.agenda_row, null);
	        	v.setOnClickListener(clickEvent);
	        	
	        	Calendar eventDate = Calendar.getInstance();
		        try
		        {
		        	String d = c.getString(c.getColumnIndex("date"));
		        	String t = c.getString(c.getColumnIndex("time"));
		        	eventDate.setTime(Callisto.sdfRaw.parse(d+t));
		        } catch(Exception e)
		        {	e.printStackTrace();    }
		        
		        
		        Calendar today = Calendar.getInstance();
		        String timeUntil = "NOW!";
		        int timeBetween = 24*60;
		        if(thisWeek && i==0)
		        {
		        	int showTime = eventDate.get(Calendar.HOUR_OF_DAY)*60 + eventDate.get(Calendar.MINUTE);
			        int currentTime = today.get(Calendar.HOUR_OF_DAY)*60 + today.get(Calendar.MINUTE);
			        timeBetween = showTime - currentTime; //Add 60, so people can catch a show that's already in progress
			        if(timeBetween+60 < 0)
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
		        
		        
		        System.out.println(current_cal.get(Calendar.YEAR) + "==" + today.get(Calendar.YEAR));
	        	//Date
		        if(thisWeek)
		        {
		        	if(i==0)
		        		((TextView) v.findViewById(R.id.date)).setText(Callisto.RESOURCES.getString(R.string.today));
		        	else
		        		((TextView) v.findViewById(R.id.date)).setText(days[(i+dayOfWeek)%7]);
		        }
		        else
		        {
		        	//Etc
	        		String temp2;
			        if(Callisto.europeanDates)
			        	temp2 = current_cal.get(Calendar.DATE) + "/" + (current_cal.get(Calendar.MONTH)+1);
			        else
			        	temp2 = (current_cal.get(Calendar.MONTH)+1) + "/" + current_cal.get(Calendar.DATE);
			        	
		        	((TextView) v.findViewById(R.id.date)).setText(temp2 + 
	    					//If it's next year, display the year
	    					(current_cal.get(Calendar.YEAR)!=today.get(Calendar.YEAR) ? ("/" + current_cal.get(Calendar.YEAR)) : "")
	        			);
		        }
		        //Time
		        if(thisWeek && i==0 && timeBetween<=60*12)
		        {
		        	((TextView) v.findViewById(R.id.time)).setText(timeUntil);
		        	((TextView) v.findViewById(R.id.soon)).setText("SOON");
		        }
		        else
		        	((TextView) v.findViewById(R.id.time)).setText(Callisto.sdfTime.format(eventDate.getTime()));
		        //Show
		        ((TextView) v.findViewById(R.id.show)).setText(c.getString(c.getColumnIndex("show")));
		        //Type
		        ((TextView) v.findViewById(R.id.type)).setText(c.getString(c.getColumnIndex("type")));
		        //_id
		        ((TextView) v.findViewById(R.id.id)).setText(c.getString(c.getColumnIndex("_id")));
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