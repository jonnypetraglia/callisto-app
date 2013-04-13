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

/**
 * Displays upcoming shows for Jupiter Broadcasting. A vertical orientation displays an agenda view,
 * and a horizontal orientation displays a weekly view.
 *
 * @author MrQweex
 */

public class CalendarActivity extends Activity {
    /**
     * The Calendar variable that holds the current date that is selected. Like the month in the landscape view. Basically this is the most important variable in this activity.
     */
    private Calendar current_cal = Calendar.getInstance();
    /**
     * Refresh menu item
     */
    private final int REFRESH_MENU_ID = Menu.FIRST;
    /**
     * The feed URL for the JB Google Calendar
     */
    private final static String CALENDAR_FEED_URL = "https://www.google.com/calendar/feeds/jalb5frk4cunnaedbfemuqbhv4@group.calendar.google.com/public/basic?singleevents=false&futureevents=true&&orderby=starttime&sortorder=a";
    /**
     * Dude come on, why are you even reading this.
     */
    private final long MILLISECONDS_IN_A_DAY = 1000 * 60 * 60 * 24;
    /**
     * The Month and Year in the title
     */
    private TextView Tmonth, Tyear;
    /**
     * The buttons in the title bar
     */
    private Button Bnext, Bprev;
    /**
     * The months [January, February, etc]
     */
    private String[] months;
    /**
     * The days [Sunday, Monday, etc]
     */
    private String[] days;
    /**
     * The 7 day views
     */
    private LinearLayout[] dayViews = new LinearLayout[7];
    /**
     * The dialog that is shown when an event is created or edited
     */
    private Dialog createEventDialog;
    /**
     * The popup window that is shown when you press a show
     */
    private PopupWindow popUp;
    /**
     * Used to determine if it's the agenga or week view
     */
    private boolean isLandscape = false;
    /**
     * A piece of information for the popup (i.e. the 'popUp' variable)
     */
    private TextView popUp_title, popUp_type, popUp_date, popUp_time;
    /**
     * The progress dialog to show when refreshing the calendar
     */
    private ProgressDialog progressDialog;
    /**
     * The task to fetch the RSS file and insert it into the SQL database
     */
    private FetchEventsTask fetchTask;
    /**
     * A LinearLayout that will hold the individual shows. The activity uses this instead of a ListView.
     */
    private LinearLayout agenda;

    /**
     * Theoretically the date parsing should be able to parse PDT. However, apparently Android documentation and Stackoverflow and big goddamn liars. So we need this.
     */
    boolean stupidBug = !(Arrays.asList(TimeZone.getAvailableIDs()).contains("PDT"));
    /**
     * The height of the popUp PopupWindow after it is shown
     */
    int TheHeightOfTheFreakingPopup = 0;


    /**
     * Called when the activity is first created. Determines the orientation of the screen then initializes the appropriate view.
     *
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(Callisto.RESOURCES.getString(R.string.agenda_title));

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
        popupV.findViewById(R.id.editEvent).setOnClickListener(editEvent);

        //Weekly View
        if (!isLandscape) {
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

            //Create the "Load More" button at the bottom
            Button loadMoreButton = (Button) findViewById(R.id.loadmore);
            loadMoreAgenda(true);
            loadMoreButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setEnabled(false);
                    ((Button) v).setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr) - 0x88000000);
                    loadMoreAgenda(false);
                    v.setEnabled(true);
                    ((Button) v).setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
                }

            });
            //Agenda View
        } else {
            Log.v("CalendarActivity:OnCreate", "Layout is landscape -> calendar");
            setContentView(R.layout.calendar);

            //Create the day views
            LinearLayout allCols = (LinearLayout) findViewById(R.id.allColumns);
            for (int i = 0; i < 7; i++) {
                LinearLayout col = (LinearLayout) allCols.findViewWithTag("col");
                col.setTag("");
                dayViews[i] = (LinearLayout) col.findViewWithTag("day");
                dayViews[i].setTag("");
            }

            //Set the title listeners
            Tmonth = (TextView) findViewById(R.id.Tmonth);
            Tyear = (TextView) findViewById(R.id.Tyear);
            Bnext = (Button) findViewById(R.id.Bnext);
            Bprev = (Button) findViewById(R.id.Bprev);
            Tmonth.setOnClickListener(resetCalender);
            Tyear.setOnClickListener(resetCalender);
            Bnext.setOnClickListener(nextWeek);
            Bprev.setOnClickListener(prevWeek);

            //Refresh to the current day
            updateCalendar();
            if (savedInstanceState == null)
                Tmonth.performClick();
        }
    }

    /**
     * Called when any key is pressed. Used do dismiss the event info popup when you press back.
     *
     * @param keyCode I dunno
     * @param event   I dunno
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Override back button
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //Dismiss the progress dialog
            if (progressDialog != null && progressDialog.isShowing()) {
                fetchTask.cancel(true);
                progressDialog.cancel();
            }
            //Dismiss the popup
            if (popUp.isShowing()) {
                popUp.dismiss();
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Called when the activity is resumed, like when you return from another activity or also when it is first created.
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.v("CalendarActivity:OnResume", "Resuming Calendar");
        //Show the popUp really quickly so we can measure it later
        if (!isLandscape)
            findViewById(R.id.mainAgenda).post(new Runnable() {
                public void run() {
                    popUp.showAsDropDown(agenda);
                    popUp.dismiss();
                }
            });
    }

    /**
     * Called when it is time to create the menu.
     *
     * @param menu Um, the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, REFRESH_MENU_ID, Menu.NONE, Callisto.RESOURCES.getString(R.string.reload)).setIcon(R.drawable.ic_action_reload);
        return true;
    }

    /**
     * Called when an item in the menu is pressed.
     *
     * @param item The menu item ID that was pressed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case REFRESH_MENU_ID:   //Refresh the SQL database from the RSS feed
                fetchTask = new FetchEventsTask();
                fetchTask.execute((Object[]) null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the user press the '>>' button.
     */
    public OnClickListener nextWeek = new OnClickListener() {
        @Override
        public void onClick(View v) {
            current_cal.add(Calendar.DATE, 7);
            updateCalendar();
        }
    };

    /**
     * Called when the user press the '<<' button.
     */
    public OnClickListener prevWeek = new OnClickListener() {
        @Override
        public void onClick(View v) {
            current_cal.add(Calendar.DATE, -7);
            updateCalendar();
        }
    };

    /**
     * Resets the calendar to the current date.
     */
    public OnClickListener resetCalender = new OnClickListener() {
        @Override
        public void onClick(View v) {
            current_cal = Calendar.getInstance();
            updateCalendar();
        }
    };

    /**
     * Calculates the difference between Jupiter time and local time.
     * http://obscuredclarity.blogspot.com/2010/08/determine-time-difference-between-two.html *
     */
    private int timezoneDifference()
    {
        //TODO: Remove?
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

    /**
     * Performs database query outside GUI thread.
     */
    private class FetchEventsTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //If it's Agenda, remove the show event views
            if (!isLandscape)
                agenda.removeAllViews();
            Log.v("CalendarActivity:FetchEventsTask", "Preparing to fetch new events");
            //Build the progress dialog for fetching
            progressDialog = ProgressDialog.show(CalendarActivity.this, Callisto.RESOURCES.getString(R.string.loading), Callisto.RESOURCES.getString(R.string.loading_msg), true, false);
            progressDialog.setCancelable(true);
        }

        @Override
        protected Object doInBackground(Object... params)
        {
            try {
                Log.v("CalendarActivity:FetchEventsTask", "Initializing the Xml pulling");
                //Initialize the XML pulling
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                URL url = new URL(CALENDAR_FEED_URL);
                InputStream input = url.openConnection().getInputStream();
                xpp.setInput(input, null);
                Matcher theMatcher = null;
                int eventType = xpp.getEventType();

                //Create the date parsers
                SimpleDateFormat sdf;
                SimpleDateFormat sdfReplaces = new SimpleDateFormat("EE MMM dd, yyyy hha z", Locale.US);
                SimpleDateFormat sdfReplaces2 = new SimpleDateFormat("EE MMM dd, yyyy hh:mma z", Locale.US);
                SimpleDateFormat sdfRecurring = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz", Locale.US);

                //Clear the calendar's current events
                Callisto.databaseConnector.clearCalendar();

                //Skip the first heading
                Log.v("CalendarActivity:FetchEventsTask", "Skipping the first heading");
                while (!("entry".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)) {
                    eventType = xpp.next();
                    if (eventType == XmlPullParser.END_DOCUMENT)
                        break;
                }
                eventType = xpp.next();

                Log.v("CalendarActivity:FetchEventsTask", "Entering the main loop");
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    eventType = xpp.next(); //TODO: what is this here for

                    Log.v("CalendarActivity:FetchEventsTask", "(skipping the first part)");
                    // (skip the first part)
                    while (!("title".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)) {
                        eventType = xpp.next();
                        if (eventType == XmlPullParser.END_DOCUMENT)
                            break;
                    }

                    //Show
                    eventType = xpp.next();
                    String evShow = xpp.getText();
                    Log.v("CalendarActivity:FetchEventsTask", "Getting show: " + evShow);
                    if (evShow == null)
                        break;
                    String evType = evShow.substring(0, evShow.indexOf(":")).trim();
                    evShow = evShow.substring(evShow.indexOf(":") + 1, evShow.length()).trim();

                    // (skip the unnecessary data)
                    Log.v("CalendarActivity:FetchEventsTask", "(skipping the summary parts)");
                    while (!("summary".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
                    {
                        eventType = xpp.next();
                        if (eventType == XmlPullParser.END_DOCUMENT)
                            break;
                    }

                    // (the START of the Date; this is not the end)
                    eventType = xpp.next();
                    String evDate = xpp.getText();
                    Log.v("CalendarActivity:FetchEventsTask", "Starting to get the date: " + evDate);
                    if (evDate == null)
                        break;
                    boolean evRecurring = evDate.startsWith("Recurring");
                    // (build a regex depending on if it's recurring)
                    if (!evRecurring) {
                        theMatcher = (Pattern.compile(".*?When:(.*?)to.*?\n(.*?)<br>")).matcher(evDate);
                        if (evDate.contains(":"))
                            sdf = sdfReplaces2;
                        else
                            sdf = sdfReplaces;
                    } else {
                        theMatcher = (Pattern.compile(".*?<br>\n*?First start:(.*?)\n*?<br>")).matcher(evDate);
                        sdf = sdfRecurring;
                    }
                    // (find the regex and extract the Date)
                    if (theMatcher.find())
                        if (theMatcher.groupCount() > 1)
                            evDate = (theMatcher.group(1) + theMatcher.group(2)).trim();
                        else
                            evDate = theMatcher.group(1).trim();
                    else
                        continue;

                    //Create a Calendar object to hold the date
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
                    // (not yet)
                    evDate = Callisto.sdfRaw.format(tempDate.getTime());

                    //Date (FINALLY) and Time
                    String evTime = evDate.substring(8).trim();
                    evDate = evDate.substring(0, 8).trim();
                    Log.v("CalendarActivity:FetchEventsTask", "Getting date: " + evDate);
                    Log.v("CalendarActivity:FetchEventsTask", "Getting time: " + evTime);

                    //Chriiiiiiis fix this
                    if (evShow.equals("The Linux Action Show!"))
                        evShow = "Linux Action Show";

                    //Insert the event
                    Callisto.databaseConnector.insertEvent(evShow, evType.trim(), evDate, evTime, evRecurring ? tempDate.get(Calendar.DAY_OF_WEEK) : -1);

                }   //End of the while loop
                //If an error is thrown, it will break out of the loop. I don't even know if this is a good idea.
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                return "XML";
            } catch (MalformedURLException e2) {
                e2.printStackTrace();
                return "URL";
            } catch (IOException e2) {
                e2.printStackTrace();
                return "I/O";
            } catch (NullPointerException e) {
                return "NULL";
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {   //The result will be a string if there was an error, null otherwise

            progressDialog.hide();
            if (result != null)     //Display an error message if there was an error
            {
                Toast.makeText(CalendarActivity.this, result + " occurred. Maybe your connection might be flaky?", Toast.LENGTH_LONG).show();
                Log.v("CalendarActivity:FetchEventsTask", "ERROR: " + result);
            }
            else
            {
                if (isLandscape)
                    Tmonth.performClick();  //Go to the current week
                else
                {
                    ((Button) findViewById(R.id.loadmore)).setText(Callisto.RESOURCES.getString(R.string.load_more));       //The button has different text if you've never updated.
                    loadMoreAgenda(true);   //Load the first few event thingies after the update
                }
            }
        }
    }

    /**
     * Updates the Weekly view after changing the current the week.
     */
    public void updateCalendar()
    {
        Log.v("CalendarActivity:updateCalendar", "Starting to update the Calendar");
        //Update some of the views
        Tmonth.setText(months[current_cal.get(Calendar.MONTH)]);
        Tyear.setText(Integer.toString(current_cal.get(Calendar.YEAR)));
        Bprev.setEnabled(true);
        Bprev.setTextColor(Callisto.RESOURCES.getColor(R.color.calHdrClr));

        int dayOfWeek = current_cal.get(Calendar.DAY_OF_WEEK) - 1;
        Calendar today = Calendar.getInstance();
        current_cal.add(Calendar.DATE, -1 * dayOfWeek);

        //Iterate over the days
        for (int i = 0; i < 7; i++)
        {
            int dayOfTheMonth = current_cal.get(Calendar.DAY_OF_MONTH);
            if (dayOfTheMonth == 1)
            {
                current_cal.add(Calendar.MONTH, 1);
                Tmonth.setText(months[current_cal.get(Calendar.MONTH)]);
                Tyear.setText(Integer.toString(current_cal.get(Calendar.YEAR)));
                current_cal.add(Calendar.MONTH, -1);
            }
            ((TextView) dayViews[i].findViewWithTag("dayText")).setText(Integer.toString(dayOfTheMonth));


            //If it occurs Today (either before now or after now), set some pretty colors
            if (Math.abs(current_cal.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) <= MILLISECONDS_IN_A_DAY
                    && current_cal.get(Calendar.DATE) == Calendar.getInstance().get(Calendar.DATE))
            {
                ((TextView) dayViews[i].findViewWithTag("dayText")).setTextColor(0xFF000000 + Callisto.RESOURCES.getColor(R.color.calTodayClr));
                Bprev.setEnabled(false);
                Bprev.setTextColor(Callisto.RESOURCES.getColor(R.color.calHdrClr) - 0x88000000);
            }
            else
            {
                ((TextView) dayViews[i].findViewWithTag("dayText")).setTextColor(0xFF000000 + Callisto.RESOURCES.getColor(R.color.calDayClr));
                //TODO: Is this even necessary?
                if(current_cal.before(today))   //Hide shows that have passed?
                {
                    current_cal.add(Calendar.DATE, 1);
                    for (int j = 1; j <= 10; j++)
                    {
                        TextView event = (TextView) dayViews[i].findViewById(Callisto.RESOURCES.getIdentifier("event" + j, "id", "com.qweex.callisto"));
                        if (event == null)
                            continue;
                        event.setVisibility(View.GONE);
                    }
                    continue;
                }
            }

            //Get the events for this specific day
            Cursor events = Callisto.databaseConnector.dayEvents(Callisto.sdfRawSimple1.format(current_cal.getTime()), current_cal.get(Calendar.DAY_OF_WEEK) - 1);
            events.moveToFirst();
            int numberOfEvents = 1;

            //Iterates through those events
            while(events.isAfterLast() == false)
            {
                String type = events.getString(events.getColumnIndex("type"));
                String show = events.getString(events.getColumnIndex("show"));
                String _id = events.getString(events.getColumnIndex("_id"));

                Log.v("CalendarActivity:updateCalendar", "Getting show for: " + show);
                TextView event = (TextView) dayViews[i].findViewById(Callisto.RESOURCES.getIdentifier("event" + numberOfEvents, "id", "com.qweex.callisto"));
                View w = (View) event.getParent();
                TextView id = (TextView) w.findViewById(Callisto.RESOURCES.getIdentifier("id", "id", "com.qweex.callisto"));
                id.setText(_id);

                event.setText(show);
                event.setBackgroundColor(Callisto.RESOURCES.getColor(type.equals("RELEASE") ? R.color.releaseClr : R.color.liveClr));
                event.setVisibility(View.VISIBLE);
                event.setOnClickListener(clickEvent);

                events.moveToNext();
                numberOfEvents++;
            }
            //Add empty views that are BS, man.
            for(; numberOfEvents <= 10; numberOfEvents++)
            {
                TextView event = (TextView) dayViews[i].findViewById(Callisto.RESOURCES.getIdentifier("event" + numberOfEvents, "id", "com.qweex.callisto"));
                if (event == null)
                    continue;
                event.setVisibility(View.GONE);
            }
            current_cal.add(Calendar.DATE, 1);
        } //End of the for loop iterating over the 7 days
        current_cal.add(Calendar.DATE, -7 + dayOfWeek);
    }

    /**
     * Called when the user the presses the "Edit" button for an existing alarm.
     */
    public OnClickListener editEvent = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v("CalendarActivity:editEvent", "Editing an event");
            //Create the dialog
            LayoutInflater inflater = (LayoutInflater) CalendarActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View editEventView = inflater.inflate(R.layout.edit_alarm, null);
            createEventDialog = new Dialog(CalendarActivity.this);
            createEventDialog.setContentView(editEventView);
            if (isLandscape)
                ((LinearLayout) editEventView.findViewById(R.id.landscapeRotate)).setOrientation(LinearLayout.HORIZONTAL);
            createEventDialog.setTitle("Set Alarm/Notification");
            createEventDialog.show();

            //Create a number picker
            NumberPicker NumP = (NumberPicker) editEventView.findViewById(R.id.minutesBefore);
            NumP.setSuffix(" min before");
            NumP.setIncrement(15);
            NumP.setRange(0, 300);

            //Create a ringtone manager; lists the ringtones for selection
            RingtoneManager mRingtoneManager2 = new RingtoneManager(CalendarActivity.this);
            mRingtoneManager2.setType(RingtoneManager.TYPE_RINGTONE);
            mRingtoneManager2.setIncludeDrm(true);
            Cursor mCursor2 = mRingtoneManager2.getCursor();
            startManagingCursor(mCursor2);
                //Creating the adapter
            String[] from = {mCursor2.getColumnName(RingtoneManager.TITLE_COLUMN_INDEX), mCursor2.getColumnName(RingtoneManager.ID_COLUMN_INDEX), mCursor2.getColumnName(RingtoneManager.URI_COLUMN_INDEX)};
            int[] to = {R.id.text1, R.id.id1, R.id.uri};
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(CalendarActivity.this, R.layout.simple_spinner_item_plus, mCursor2, from, to);
            adapter.setDropDownViewResource(R.layout.simple_spinner_item_plus);
            Spinner s = (Spinner) editEventView.findViewById(R.id.spinner1);
            s.setAdapter(adapter);


            //Set the previous if it exists
            String key = (String) ((TextView) popUp.getContentView().findViewById(R.id.key)).getText();
            key = Callisto.alarmPrefs.getString(key, "");
            String tone = "";
            int min = 0;
            boolean vibrate = false, isAlarm = false;
            if(!key.equals(""))
            {
                Log.v("CalendarActivity:editEvent", "There is a previous alarm. Adding it.");
                try {
                    //Get the info about this existing alarm
                    min = Integer.parseInt(key.substring(0, key.indexOf("_")));
                    tone = key.substring(key.indexOf("_") + 1, key.lastIndexOf("_"));
                    int i = Integer.parseInt(key.substring(key.lastIndexOf("_") + 1));
                    isAlarm = i > 10 ? true : false;
                    vibrate = i % 2 != 0 ? true : false;

                    Cursor c = mRingtoneManager2.getCursor();
                    c.moveToFirst();
                    i = 1;
                    //Have to find which ringtone it matches. I hate that this is the way you have to do this.
                    while (c.moveToNext())
                    {
                        int x = c.getInt(RingtoneManager.ID_COLUMN_INDEX);
                        if (tone.endsWith(Integer.toString(x)))
                        {
                            s.setSelection(i);
                            break;
                        }
                        i++;
                    }
                    ((RadioButton) editEventView.findViewById(R.id.isAlarm)).setChecked(isAlarm);
                    ((CheckBox) editEventView.findViewById(R.id.vibrate)).setChecked(vibrate);
                    ((NumberPicker) editEventView.findViewById(R.id.minutesBefore)).setValue(min);
                } catch(Exception e) {
                    Log.e("CalendarActivity:editEvent", "Error Happened: " + e.getClass());
                    e.printStackTrace();
                }
            }

            //Set the onClickListener for the save button
            editEventView.findViewById(R.id.save).setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Log.v("CalendarActivity:editEvent:Save", "Getting info of new alarm");
                    View editEventView = (View) v.getParent();

                    //Get the info for the alarm
                    Spinner s = (Spinner) editEventView.findViewById(R.id.spinner1);
                    String tone = (String) ((TextView) (s.getSelectedView()).findViewById(R.id.id1)).getText();
                    String uri = (String) ((TextView) (s.getSelectedView()).findViewById(R.id.uri)).getText();
                    String key = (String) ((TextView) popUp.getContentView().findViewById(R.id.key)).getText();
                    int min = ((NumberPicker) editEventView.findViewById(R.id.minutesBefore)).getValue();
                    int isAlarm = ((RadioButton) editEventView.findViewById(R.id.isAlarm)).isChecked() ? 1 : 0;
                    int vibrate = ((CheckBox) editEventView.findViewById(R.id.vibrate)).isChecked() ? 1 : 0;
                    Calendar time = Calendar.getInstance();
                    try {
                        time.setTime(Callisto.sdfRaw.parse(key.substring(key.length() - 14)));
                        time.add(Calendar.MINUTE, -1 * min);
                    } catch(ParseException e) {
                        //TODO: does this need to have anything?
                    }

                    Log.v("CalendarActivity:editEvent:Save", "Creating the AlarmManager");
                    //Create the AlamManager
                    Intent i = new Intent(CalendarActivity.this, AlarmNotificationReceiver.class);
                    i.putExtra("tone", uri + "/" + tone);
                    i.putExtra("min", min);
                    i.putExtra("isAlarm", isAlarm);
                    i.putExtra("vibrate", vibrate);
                    i.putExtra("show", key.substring(0, key.length() - 14));
                    i.putExtra("key", key);
                    Toast.makeText(CalendarActivity.this, "Creating event...", Toast.LENGTH_SHORT).show();
                    PendingIntent pi = PendingIntent.getBroadcast(CalendarActivity.this.getApplicationContext(), 234324246, i, PendingIntent.FLAG_UPDATE_CURRENT);
                    AlarmManager mAlarm = (AlarmManager) CalendarActivity.this.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    mAlarm.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pi);

                    Log.v("CalendarActivity:editEvent:Save", "Saving the alarm");
                    //Save the alarm to the preferences
                    SharedPreferences.Editor edit = Callisto.alarmPrefs.edit();
                    edit.putString(key, Integer.toString(min) + "_" + uri + "/" + tone + "_" + isAlarm + "" + vibrate);
                    edit.commit();
                    createEventDialog.dismiss();
                    popUp.dismiss();
                }
            });
        }
    };

    /**
     * Listener for play/pause button.
     */
    public OnClickListener clickEvent = new OnClickListener()
    {
        @Override
        public void onClick(View v) {
            Log.v("CalendarActivity:clickEvent", "Event has been clicked");
            if (popUp.isShowing())
                popUp.dismiss();
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            View vParent = (View) (isLandscape ? v.getParent() : v);
            String id = (String) ((TextView) vParent.findViewById(R.id.id)).getText();

            //Check for require info (hobo exception handling)
            if (id.equals(""))
                return;
            long _id = 0;
            try {
                _id = Long.parseLong(id);
            } catch (NumberFormatException e) {
                Log.e("CalendarActivity:clickEvent", "A number parse error occurred: " + _id);
                return;
            }
            Cursor c = Callisto.databaseConnector.getOneEvent(_id);
            if (!c.moveToFirst())
                return;

            //Prepare the popup!
            String show = c.getString(c.getColumnIndex("show"));
            String date = "", time = "";
            popUp_title.setText(show);
            String title = show;
            show = c.getString(c.getColumnIndex("type"));
            popUp_type.setText(show);
            Log.v("CalendarActivity:clickEvent", "The show is: " + show);

            //Get the actual date; the methods depends on the current layout
            try {
                if(isLandscape)
                {
                    vParent = (View) vParent.getParent();
                    //Get the day
                    String day = (String) ((TextView) vParent.findViewById(R.id.theDay)).getText();
                    //Get the month
                    String month = (String) ((TextView) findViewById(R.id.Tmonth)).getText();
                    int i;
                    for (i = 0; i < months.length; i++)
                        if (month.equals(months[i]))
                            break;
                    month = Integer.toString(i + 1);
                    //Get the year
                    String year = (String) ((TextView) findViewById(R.id.Tyear)).getText();
                    date = Callisto.sdfRawSimple1.format(Callisto.sdfDestination.parse(month + "/" + day + "/" + year));
                }
                else
                {
                    date = (String) ((TextView) v.findViewById(R.id.date)).getText();
                    if(date.contains("/"))      // MM/dd or dd/MM
                    {
                        SimpleDateFormat sdf = new SimpleDateFormat(Callisto.europeanDates ? "dd/MM" : "MM/dd");
                        if(date.indexOf("/", date.indexOf("/") + 1) > 0)
                            sdf = Callisto.sdfDestination;
                        Calendar dt = Calendar.getInstance();
                        dt.setTime(sdf.parse(date));
                        dt.set(Calendar.YEAR, (new java.util.Date()).getYear());
                        date = Callisto.sdfRawSimple1.format(dt.getTime());
                    }
                    //Today
                    else if (date.equals(Callisto.RESOURCES.getString(R.string.today)))
                        date = Callisto.sdfRawSimple1.format(new java.util.Date());
                    else
                    {
                        //Mon Tue Wed
                        for (int i = 1; i <= days.length; i++)
                        {
                            if (date.equals(days[i - 1]))
                            {
                                i = (i - 1 + 7) % 7;
                                Calendar cl = Calendar.getInstance();
                                int j = (cl.get(Calendar.DAY_OF_WEEK));// % 7;
                                if (j <= i)
                                    cl.add(Calendar.DATE, i - j);
                                else
                                    cl.add(Calendar.DATE, 7 + i - j);
                                date = Callisto.sdfRawSimple1.format(cl.getTime());
                                break;
                            }
                        }
                    }
                }
                Log.v("CalendarActivity:clickEvent", "The date is: " + date);

                show = Callisto.sdfDestination.format(Callisto.sdfRawSimple1.parse(date));
                popUp_date.setText(show);
                //Might just want to read this from the UI
                time = c.getString(c.getColumnIndex("time"));
                show = Callisto.sdfTime.format(Callisto.sdfRawSimple2.parse(time));
                popUp_time.setText(show);
            } catch (ParseException e) {
                Log.v("CalendarActivity:clickEvent", "ParseException");
            }

            //TODO: Dude I don't even know
            Map<String, ?> alarms = Callisto.alarmPrefs.getAll();
            String key = title + date + time;
            ((TextView) popUp.getContentView().findViewById(R.id.key)).setText(key);
            if(alarms.containsKey(popUp_title)) {
                popUp.getContentView().findViewById(R.id.hasAlarm).setVisibility(View.VISIBLE);
                ((TextView) popUp.getContentView().findViewById(R.id.alarmInfo)).setText("Alarm set for this show");
                ((Button) popUp.getContentView().findViewById(R.id.editEvent)).setText("Edit");
            } else if(alarms.containsKey(key)) {
                popUp.getContentView().findViewById(R.id.hasAlarm).setVisibility(View.VISIBLE);
                ((TextView) popUp.getContentView().findViewById(R.id.alarmInfo)).setText("Alarm set for just this episode");
                ((Button) popUp.getContentView().findViewById(R.id.editEvent)).setText("Edit");
            } else {
                popUp.getContentView().findViewById(R.id.hasAlarm).setVisibility(View.INVISIBLE);
                ((TextView) popUp.getContentView().findViewById(R.id.alarmInfo)).setText("No alarm set");
                ((Button) popUp.getContentView().findViewById(R.id.editEvent)).setText("Add");
            }

            //Measure the popup
            popUp.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            TheHeightOfTheFreakingPopup = popUp.getContentView().getMeasuredHeight();
            boolean isLowerHalf = pos[1] > getWindowManager().getDefaultDisplay().getHeight() / 2;
            Log.v("CalendarActivity:clickEvent", "Height of the freaking popup: " + TheHeightOfTheFreakingPopup);

            //Show the popup
            if(isLandscape)
            {
                popUp.getContentView().findViewById(R.id.thing).setBackgroundDrawable(getResources().getDrawable(R.drawable.spinner_ab_focused_holo_light_none));
                int y = getWindowManager().getDefaultDisplay().getHeight() / 2;
                popUp.showAsDropDown(findViewById(R.id.linearLayout1), 0, (y - TheHeightOfTheFreakingPopup) / 2);
            } else if(isLowerHalf) {
                popUp.getContentView().findViewById(R.id.thing).setBackgroundDrawable(getResources().getDrawable(R.drawable.spinner_ab_focused_holo_light));
                popUp.showAsDropDown(v, 0, -1 * v.getHeight() - TheHeightOfTheFreakingPopup - (isLowerHalf ? 0 : 40));
            } else {
                popUp.getContentView().findViewById(R.id.thing).setBackgroundDrawable(getResources().getDrawable(R.drawable.spinner_ab_focused_holo_light_flip));
                popUp.showAsDropDown(v, 0, -5);
            }
        }
    };


    /**
     * Loads 7 more days worth of events into the Agenda view
     *
     * @param thisWeek If it is this week
     */
    public void loadMoreAgenda(boolean thisWeek)
    {
        Log.v("CalendarActivity:loadMoreAgenda", "Loading more agenda");
        int dayOfWeek = current_cal.get(Calendar.DAY_OF_WEEK) - 1;
        TextView loading = (TextView) findViewById(R.id.empty);

        //Check to see if there are any more events. Hint: there should be.
        if(Callisto.databaseConnector.eventCount() == 0)
        {
            Log.v("CalendarActivity:loadMoreAgenda", "eventCount() has returned 0");
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

        //Show the loading message
        loading.setVisibility(View.VISIBLE);
        loading.setText(Callisto.RESOURCES.getString(R.string.loading));

        //Loop over the 7 days
        boolean foundEvents = false;
        for(int i = 0; i < 7; i++)
        {
            //Get the target date as a string: 20120123 for example
            String targetDate = "";
            int temp;
            targetDate += Integer.toString(current_cal.get(Calendar.YEAR));
            temp = current_cal.get(Calendar.MONTH);
            if (temp < 10)
                targetDate += "0";
            targetDate += Integer.toString(temp);
            temp = current_cal.get(Calendar.DAY_OF_MONTH);
            if (temp < 10)
                targetDate += "0";
            targetDate += Integer.toString(temp);
            Log.v("CalendarActivity:loadMoreAgenda", "Target date: " + targetDate);


            //Get a cursor containing the valid events for that day
            Cursor c = Callisto.databaseConnector.dayEvents(targetDate, current_cal.get(Calendar.DAY_OF_WEEK));
            c.moveToFirst();

            //Add these events to the agenda
            while(!c.isAfterLast())
            {
                foundEvents = true; //Boolean to mark if there were any events found

                //Inflate the view & set its listener
                LayoutInflater inflater = (LayoutInflater) CalendarActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View v = inflater.inflate(R.layout.agenda_row, null);
                v.setOnClickListener(clickEvent);

                //Get the eventDate
                Calendar eventDate = Calendar.getInstance();
                try {
                    String d = c.getString(c.getColumnIndex("date"));
                    String t = c.getString(c.getColumnIndex("time"));
                    eventDate.setTime(Callisto.sdfRaw.parse(d + t));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Get the timeUntil
                Calendar today = Calendar.getInstance();
                String timeUntil = "NOW!";
                int timeBetween = 24 * 60;
                if(thisWeek && i==0)
                {
                    int showTime = eventDate.get(Calendar.HOUR_OF_DAY) * 60 + eventDate.get(Calendar.MINUTE);
                    int currentTime = today.get(Calendar.HOUR_OF_DAY) * 60 + today.get(Calendar.MINUTE);
                    timeBetween = showTime - currentTime; //Add 60, so people can catch a show that's already in progress
                    if(timeBetween + 60 < 0) {
                        c.moveToNext();
                        continue;
                    }
                    timeBetween -= 60;
                    if(timeBetween > 0)
                        timeUntil = Callisto.formatTimeFromSeconds(timeBetween) + " left";
                    else
                        timeUntil = "Now!";
                }
                Log.v("CalendarActivity:loadMoreAgenda", "Time Until: " + timeUntil);

                //Set the date
                if(thisWeek)
                {
                    if(i==0)
                        ((TextView) v.findViewById(R.id.date)).setText(Callisto.RESOURCES.getString(R.string.today));
                    else
                        ((TextView) v.findViewById(R.id.date)).setText(days[(i + dayOfWeek) % 7]);
                } else
                {
                    String temp2;
                    if (Callisto.europeanDates)
                        temp2 = current_cal.get(Calendar.DATE) + "/" + (current_cal.get(Calendar.MONTH) + 1);
                    else
                        temp2 = (current_cal.get(Calendar.MONTH) + 1) + "/" + current_cal.get(Calendar.DATE);

                    ((TextView) v.findViewById(R.id.date)).setText(temp2 +
                            //If it's next year, display the year
                            (current_cal.get(Calendar.YEAR) != today.get(Calendar.YEAR) ? ("/" + current_cal.get(Calendar.YEAR)) : "")
                    );
                }

                //Set the time
                if (thisWeek && i == 0 && timeBetween <= 60 * 12)
                {
                    ((TextView) v.findViewById(R.id.time)).setText(timeUntil);
                    ((TextView) v.findViewById(R.id.soon)).setText("SOON");
                } else
                    ((TextView) v.findViewById(R.id.time)).setText(Callisto.sdfTime.format(eventDate.getTime()));

                //Show
                ((TextView) v.findViewById(R.id.show)).setText(c.getString(c.getColumnIndex("show")));
                //Type
                ((TextView) v.findViewById(R.id.type)).setText(c.getString(c.getColumnIndex("type")));
                //_id
                ((TextView) v.findViewById(R.id.id)).setText(c.getString(c.getColumnIndex("_id")));
                agenda.addView(v);

                c.moveToNext();
            }   //End the while for the events of 1 day
            current_cal.add(Calendar.DATE, 1);
        }   //End the for over 7 days

        if (!foundEvents)
            loading.setText(Callisto.RESOURCES.getString(R.string.no_events));
        else
            loading.setVisibility(View.GONE);
    }
}