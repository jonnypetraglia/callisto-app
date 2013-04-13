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
package com.qweex.callisto.irc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.*;
import android.text.style.ImageSpan;
import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import com.qweex.callisto.VideoActivity;
import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.ServerInformation;
import jerklib.Session;
import jerklib.events.*;
import jerklib.listeners.IRCEventListener;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewAnimator;


/** The IRC portion of the app to connect to the JB chatroom.
 * @author MrQweex
 */

public class IRCChat extends Activity implements IRCEventListener
{
	protected static final int LOG_ID=Menu.FIRST+1;
    protected static final int CHANGE_ID=LOG_ID+1;
    protected static final int LOGOUT_ID=CHANGE_ID+1;
    protected static final int NICKLIST_ID=LOGOUT_ID+1;
    protected static final int SAVE_ID=NICKLIST_ID+1;
	private final String SERVER_NAME = "irc.geekshed.net";
	private final String CHANNEL_NAME = "#qweex"; //"#jupiterbroadcasting";
	private String profileNick;
	private String profilePass;
	private boolean SHOW_TIME = true;
	private static int CLR_TEXT,
				   CLR_BACK,
				   CLR_TOPIC,
				   CLR_ME,
				   CLR_JOIN,
				   CLR_MYNICK,
				   CLR_NICK,
				   CLR_PART,
				   CLR_QUIT,
				   CLR_KICK,
				   CLR_ERROR,
				   CLR_MENTION,
				   CLR_PM,
				   CLR_LINKS;
	private static ConnectionManager manager;
	public static Session session;
	private static NotificationManager mNotificationManager;
	private static int mentionCount = 0;
	private static PendingIntent contentIntent;
	private static boolean isFocused = false;
	private ScrollView sv, sv2;
	private EditText input;
	
	private static java.util.Queue<Spanned> chatQueue = new java.util.LinkedList<Spanned>(),
											logQueue = new java.util.LinkedList<Spanned>();
	
	private SimpleDateFormat sdfTime = new SimpleDateFormat("'['HH:mm']'");
	private boolean isLandscape;
	private static HashMap<String, Integer> nickColors = new HashMap<String, Integer>();
	public static List<String> nickList;
	private static Handler ircHandler = null;
	private static Runnable chatUpdater;
	private boolean irssi;
	private int IRSSI_GREEN = 0x00B000;
	private MenuItem Nick_MI, Logout_MI, Log_MI, Save_MI, Change_MI;
	private WifiLock IRC_wifiLock;
	final private int RECONNECT_TRIES = 5;
	private int SESSION_TIMEOUT = 40;
	private EditText user, pass;
	private PopupWindow changeNickDialog;
	private int timeoutCount;
	private Timer loopTimer = new Timer();
	private TimerTask loopTask;
	private String nickSearch = "", lastNickSearched = "";

	
	/** Called when the activity is first created. Sets up the view, mostly, especially if the user is not yet logged in.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(ircHandler==null)
		{
			ircHandler = new Handler();
			chatUpdater = new Runnable()
			{
		        @Override
		        public void run()
		        {
		        	Spanned received;
		        	System.out.println("RECEIVED: " + chatQueue.size());
		        	while(!chatQueue.isEmpty())
		        	{
		        	received = chatQueue.poll();
		        	System.out.println("RECEIVED:" + received.toString());
		        	if(received.equals(new SpannableString("")))
		        		return;
		            
				    View view = (View) Callisto.chatView;
                        Log.e("AtBottom", view + " " + sv);
				    boolean atBottom = (view.getBottom()-(sv.getHeight()+sv.getScrollY())) <= 0;
				    
		            Callisto.chatView.append(received);
		            Linkify.addLinks(Callisto.chatView, Linkify.EMAIL_ADDRESSES);
		            Linkify.addLinks(Callisto.chatView, Linkify.WEB_URLS);
		            received = new SpannableString("");
		            Callisto.chatView.invalidate();
				    
		            System.out.println(view.getBottom()-(sv.getHeight()+sv.getScrollY()));
		            if(atBottom)
		            	sv.post(new Runnable() {      public void run() {
		                    	sv.scrollTo(0, 1000000000); } });
		        	}
		            input.requestFocus();
		        }
			};
		}
		isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
		mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		profileNick = Rot47(PreferenceManager.getDefaultSharedPreferences(this).getString("irc_nick", null));
		profilePass = Rot47(PreferenceManager.getDefaultSharedPreferences(this).getString("irc_pass", null));
		irssi = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_irssi", false);
		if(session!=null)
		{
			resume();
			return;
		}
		//Set up the login screen
		setContentView(R.layout.irc_login);
		user = (EditText) findViewById(R.id.user);
		pass = (EditText) findViewById(R.id.pass);
		Button login = (Button) findViewById(R.id.login);
		user.setText(profileNick);
		pass.setText(profilePass);
		
		LinearLayout ll = (LinearLayout) findViewById(R.id.IRC_Login);
		ll.setPadding(getWindowManager().getDefaultDisplay().getHeight()/10,
				  getWindowManager().getDefaultDisplay().getHeight()/(isLandscape?6:4),
				  getWindowManager().getDefaultDisplay().getHeight()/10,
				  0);
		
		login.setCompoundDrawables(Callisto.RESOURCES.getDrawable(R.drawable.ic_action_key), null, null, null);
		
		login.setOnClickListener(InitiateLogin);
		
		//Build the ChangeNickDialog
		changeNickDialog = new PopupWindow(this);
		android.widget.FrameLayout fl = new android.widget.FrameLayout(this);
		fl.setPadding((int)(10*Callisto.DP), (int)(10*Callisto.DP), (int)(10*Callisto.DP), (int)(10*Callisto.DP));
		fl.addView(getLayoutInflater().inflate(R.layout.irc_login, null, false));
		changeNickDialog.setContentView(fl);
		changeNickDialog.setFocusable(true);
		changeNickDialog.setTouchable(true);
		Button l = (Button) fl.findViewById(R.id.login);
		l.setText("Change");
		l.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String nick = ((EditText)((LinearLayout)v.getParent()).findViewById(R.id.user)).getText().toString();
				String pass = ((EditText)((LinearLayout)v.getParent()).findViewById(R.id.pass)).getText().toString();
				if(nick.equals(""))
				{
					changeNickDialog.dismiss();
					return;
				}
				profileNick = nick;
				parseOutgoing("/nick " + profileNick);
				if(pass.equals(""))
				{
					changeNickDialog.dismiss();
					return;
				}
				profilePass = pass;
				parseOutgoing("/msg nickserv identify " + profilePass);
				return;
			}
		});
		changeNickDialog.setOutsideTouchable(true);
		changeNickDialog.setWidth(getWindowManager().getDefaultDisplay().getWidth()*8/10);
		changeNickDialog.setHeight(getWindowManager().getDefaultDisplay().getHeight()*4/10);
		changeNickDialog.setAnimationStyle(android.R.style.Animation_Dialog);
		
		
		WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		IRC_wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "Callisto_irc");
	}

    protected OnClickListener InitiateLogin= new OnClickListener(){
        @Override
        public void onClick(View v) {
            profileNick = user.getText().toString();
            System.out.println("profileNick: " + profileNick);
            if(profileNick==null || profileNick.trim().equals(""))
            {
                Toast.makeText(IRCChat.this, "Dude, you have to enter a nick.", Toast.LENGTH_SHORT).show();
                return;
            }
            String profilePassword = pass.getText().toString();
            SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(v.getContext()).edit();
            e.putString("irc_nick", Rot47(profileNick));
            e.putString("irc_pass", Rot47(profilePassword));
            e.commit();
            initiate();
        }
    };


    @Override
    public void onPause(){
        super.onPause();
        isFocused = false;
    }

	/** Called when any key is pressed. Used to prevent the activity from finishing if the user is logged in.
	 * @param keyCode I dunno
	 * @param event I dunno
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		System.out.println("DERPY " + keyCode);
		if (keyCode == KeyEvent.KEYCODE_HOME)
		{
			isFocused = false;
		}
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{	
			if(session==null || true)
			{
				finish();
				isFocused = false;
				return true;
			}
		}
	    if (keyCode == KeyEvent.KEYCODE_SEARCH)
	    {
	    	if(nickList==null)
	    		return true;
	    	int i = input.getSelectionStart();
	    	int i2 = input.getSelectionEnd();
	    	if(i!=i2)
	    		return false;
	    	if(nickSearch.equals(""))
	    	{
	    		nickSearch = input.getText().toString();
		    	nickSearch = nickSearch.substring(0, i).substring(nickSearch.lastIndexOf(" ")+1).toUpperCase();
		    	lastNickSearched = nickSearch;
	    	}
	    	
	    	String s = "";
	    	System.out.println("Search: " + nickSearch + " | " + lastNickSearched);
	    	Iterator<String> iterator = nickList.iterator();
	    	while(iterator.hasNext())
	    	{
	    		s = (String) iterator.next();
	    		System.out.println("Herpaderp " + s);
	    		if(s.toUpperCase().equals(lastNickSearched))
	    		{
	    			s = (String) iterator.next();
	    			System.out.println("Herpaderpesque " + s);
	    			if(s==null)
	    			{
	    				System.out.println("Herpaderpeqsudsadsa " + s);
	    				iterator = nickList.iterator();
	    				s = iterator.next();
	    				while(!s.startsWith(nickSearch)); s = iterator.next();
	    			}
	    			break;
	    		}
	    		if(s.toUpperCase().startsWith(nickSearch))
	    			break;
	    	}
	    	if(!iterator.hasNext())
	    		s = null;
	    	System.out.println("HerpaderOP " + s);
	    	if(s!=null)
	    	{
	    		String newt = input.getText().toString().substring(0,i-lastNickSearched.length())
	    				+ s
	    				+ input.getText().toString().substring(i);
	    		input.setText(newt);
	    		try {
	    			input.setSelection(i-lastNickSearched.length()+s.length());
	    		} catch(Exception e){}
	    		lastNickSearched = s.toUpperCase();
	    	}
	    	else
	    		new EditTextFlash(0, input, 200, ircHandler);
	    }
	    return super.onKeyDown(keyCode, event);
	} 
	
	/** Used to reset the string that is used for the nick completion */
	@Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_SEARCH && event.getAction()==KeyEvent.ACTION_DOWN) 
	    	nickSearch = "";
        return super.dispatchKeyEvent(event);
     }
	
	/** Called when it is time to create the menu.
	 * @param menu Um, the menu
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	Log_MI = menu.add(0, LOG_ID, 0, "Log").setIcon(R.drawable.ic_action_import);
    	Change_MI = menu.add(0, CHANGE_ID, 0, "Change Nick").setIcon(R.drawable.ic_action_tag);
    	Nick_MI = menu.add(0, NICKLIST_ID, 0, "NickList").setIcon(R.drawable.ic_action_list);
    	Save_MI = menu.add(0, SAVE_ID, 0, "Save to file").setIcon(R.drawable.ic_action_inbox);
    	Logout_MI = menu.add(0, LOGOUT_ID, 0, "Logout").setIcon(R.drawable.ic_action_goleft);
		updateMenu();
        return true;
    }
    
    /** Updates the menu items because invalidateOptionsMenu() is not supported on all APIs. */
    private void updateMenu()
    {
    	try {
    	Log_MI.setEnabled(session!=null);
    	Nick_MI.setEnabled(session!=null);
//    	Logout_MI.setEnabled(session!=null);
    	Save_MI.setEnabled(session!=null);
    	Nick_MI.setEnabled(session!=null);

        Log.d("DERP", session + "");

    	Change_MI.setTitle(this.getClass()== VideoActivity.class ? "Open IRC" : ((session!=null && session.getRetries()>1 ) ? "Reconnect" : "Change Nick"));
    	} catch(Exception e) {}
    }
    
    /** Called when an item in the menu is pressed.
	 * @param item The menu item ID that was pressed
	 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
        case LOG_ID:
        	((ViewAnimator) findViewById(R.id.viewanimator)).showNext();
            return true;
        case CHANGE_ID:
        	System.out.println("Herpaderp");
        	if("Reconnect".equals(item.getTitle().toString()))
        		actuallyConnect();
        	else
        		changeNickDialog.showAtLocation(findViewById(R.id.viewanimator), android.view.Gravity.CENTER, 0, 0);
    		return true;
        case NICKLIST_ID:
    		this.startActivityForResult(new Intent(this, NickList.class), 1);
        	return true;
        case SAVE_ID:
        	CharSequence cs = Callisto.chatView.getText();
        	File fileLocation = new File(Environment.getExternalStorageDirectory(),
        			Callisto.storage_path + File.separator + 
        			"ChatLog_" + Callisto.sdfRaw.format(new Date()) + 
        			".txt");
        	try {
	        	FileOutputStream fOut = new FileOutputStream(fileLocation);
	        	fOut.write(cs.toString().getBytes());
	    	    fOut.close();
	    	    Toast.makeText(this, "File written to: \n" + fileLocation.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        	} catch(Exception e) {
        		e.printStackTrace();
        		Toast.makeText(this, "Unable to write to: \n" + fileLocation.getAbsolutePath(), Toast.LENGTH_LONG).show();
        	}
        	return true;
        case LOGOUT_ID:
        	logout(null);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    /** Called when an activity is called via startActivityForResult(); called when a nicklist item is selected;
	 * @param requestCode The request code that is passed to startActivityForResult(); to determine what activity is returning a result
	 * @param resultCode The result code set by setResult() in the activity
	 * @param data I dunno
	 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
	    super.onActivityResult(requestCode, resultCode, data);
	    if(resultCode>-1)
	    {
		    int start = input.getSelectionStart();
		    int end = input.getSelectionEnd();
		    input.getText().replace(Math.min(start, end), Math.max(start, end),
		    		nickList.get(resultCode), 0, nickList.get(resultCode).length());
	    }
    }
    
    /** Used to handle when the screen is rotated, to set the necessary padding. */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      LinearLayout ll = (LinearLayout) findViewById(1337);
      if(ll!=null)
		ll.setPadding(getWindowManager().getDefaultDisplay().getHeight()/10,
				  getWindowManager().getDefaultDisplay().getHeight()/(isLandscape?6:4),
				  getWindowManager().getDefaultDisplay().getHeight()/10,
				  0);
    }

    /** Called when the activity either is created or resumes. Basically everything that must be run even whether or not the user is logged in. */
    public void resume()
    {
    	isFocused = true;
    	mentionCount = 0;
    	nickColors.put(profileNick, CLR_MYNICK);
        if(this.getClass() == VideoActivity.class)
        {
            LinearLayout p= ((LinearLayout)findViewById(R.id.mainVideo));
            View v = p.findViewWithTag("VideoLogin");
            p.removeView(v);
            p.findViewById(R.id.videoIrc).setVisibility(View.VISIBLE);
        }
        else
        {
            setContentView(R.layout.irc);
        }
		sv = (ScrollView) findViewById(R.id.scrollView);
		sv.setVerticalFadingEdgeEnabled(false);
		sv.setFillViewport(true);
		
		sv2 = (ScrollView) findViewById(R.id.scrollView2);
		sv2.setVerticalFadingEdgeEnabled(false);
		
		
		ScrollView test = ((ScrollView)Callisto.chatView.getParent());
		if(test!=null)
			test.removeView(Callisto.chatView);
		sv.addView(Callisto.chatView);
		test = ((ScrollView)Callisto.logView.getParent());
		if(test!=null)
			test.removeView(Callisto.logView);
		sv2.addView(Callisto.logView);
		input = (EditText) findViewById(R.id.inputField);
		input.setOnEditorActionListener(new OnEditorActionListener(){
			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				input.post(sendMessage);
				return true;
			}
		});

		System.out.println("CLR: " + Integer.toHexString(CLR_LINKS));
		Callisto.chatView.setBackgroundColor(CLR_BACK);
		Callisto.chatView.setLinkTextColor(0xFF000000 + CLR_LINKS);
		Callisto.logView.setBackgroundColor(CLR_BACK);
		Callisto.logView.setLinkTextColor(0xFF000000 + CLR_LINKS);
		if(irssi && android.os.Build.VERSION.SDK_INT>12) //android.os.Build.VERSION_CODES.GINGERBREAD_MR1
			input.setTextColor(0xff000000 + IRSSI_GREEN);
		if(session!=null && session.getIRCEventListeners().size()==0)
			session.addIRCEventListener(this);
		if(Callisto.notification_chat!=null)
        {
			Callisto.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
            mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
        }
    }

    @Override
    public void onResume()
    {
        if(Callisto.notification_chat!=null)
        {
            Callisto.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
            mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
        }
        super.onResume();
    }
    
	/** called to first initiate the IRC chat. Called only when the user has not logged in yet. */
    public void initiate()
    {
    	updateMenu();
    	findViewById(R.id.IRC_Login).startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
    	SHOW_TIME = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_time", true);
    	if(!IRC_wifiLock.isHeld())
            IRC_wifiLock.acquire();
    	
    	//Read colors
    	if(irssi)
    	{
    		CLR_TEXT=CLR_TOPIC=CLR_ME=CLR_JOIN=CLR_MYNICK=CLR_NICK=CLR_PART=CLR_QUIT=CLR_KICK=CLR_ERROR=CLR_MENTION=CLR_PM=CLR_LINKS=IRSSI_GREEN;
    		CLR_BACK=0x0;
    	}
    	else
    	{
	    		IRCChat.nickColors.put("",
	    	CLR_TEXT = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_text", 0x000000));
	    		IRCChat.nickColors.put("",
	    	CLR_BACK = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_back", 0xFFFFFF));
	    		IRCChat.nickColors.put(" ",
		    CLR_TOPIC = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_topic", 0xB8860B));
		    CLR_MYNICK = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_mynick", 0xFFF5EE);
	    		IRCChat.nickColors.put("   ",
		    CLR_ME = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_me", 0x9400D3));
	    		IRCChat.nickColors.put("    ",
			CLR_JOIN = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_join", 0x0000FF));
	    		IRCChat.nickColors.put("     ",
		    CLR_NICK = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_nick", CLR_JOIN));
	    		IRCChat.nickColors.put("      ",
		    CLR_PART = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_part", CLR_JOIN));
	    		IRCChat.nickColors.put("       ",
		    CLR_QUIT = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_quit", CLR_JOIN));
	    		IRCChat.nickColors.put("        ",
		    CLR_KICK = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_kick", CLR_JOIN));
	    		IRCChat.nickColors.put("         ",
			CLR_ERROR = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_error", 0x800000));
	    		IRCChat.nickColors.put("          ",
			CLR_MENTION = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_mention", 0xF08080));
	    		IRCChat.nickColors.put("           ",
			CLR_PM = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_pm", 0x008B8B));
    		CLR_LINKS = PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_links", 0x0000EE);
    	}
    	resume();
		
		actuallyConnect();
		updateMenu();
    }
    
    protected void actuallyConnect()
    {
    	Intent notificationIntent = new Intent(this, IRCChat.class);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    	Callisto.notification_chat = new Notification(R.drawable.ic_action_dialog, "Connecting to IRC", System.currentTimeMillis());
		Callisto.notification_chat.flags = Notification.FLAG_ONGOING_EVENT;
       	Callisto.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
       	mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
       	
       	
		manager = new ConnectionManager(new Profile(profileNick));
		manager.setAutoReconnect(RECONNECT_TRIES);
		session = manager.requestConnection(SERVER_NAME);
		chatQueue.add(getReceived("[Callisto]", "Attempting to logon.....be patient you silly goose.", CLR_ME));
		ircHandler.post(chatUpdater);
		
		timeoutCount = 0;
	    loopTask = new TimerTask()
		{
			public void run()
			{
				if(session==null || session.isConnected())
				{
					this.cancel();
					return;
				}
				System.out.println("TIMEOUT: " + timeoutCount);
				if(timeoutCount>=SESSION_TIMEOUT)
				{
					manager.quit();
					manager = null;
					session = null;
					updateMenu();
					chatQueue.add(getReceived("[TIMEOUT]", "Connection timed out. Either check your connection, or set a longer timeout in the settings.", CLR_ME));
					ircHandler.post(chatUpdater);
					this.cancel();
				}
				timeoutCount++;
			}
		};
		loopTimer.purge();
		loopTimer.schedule(loopTask, 0, 1000);
		
		session.addIRCEventListener(this);
		/*
		session.setInternalParser(new jerklib.parsers.DefaultInternalEventParser()
		{
			@Override
			public IRCEvent receiveEvent(IRCEvent e)
			{
				//This part isn't needed but I am keeping it in because my tiki doll told me to
				try
				{
				String action = e.getRawEventData();
				action = action.substring(action.lastIndexOf(":")+1);
					try {
						if(action.startsWith("ACTION"))
						{
							action = action.substring("ACTION".length(), action.length()-1);
							//String person = e.getRawEventData().substring(1, action.indexOf("!"));
						}
					}catch(Exception ex){}
				}
				catch(Exception ex){}
				return super.receiveEvent(e);
			}
		});*/
    }
    
    /** Used to logout, quit, or part. */
    public void logout(String quitMsg)
    {
    	if(quitMsg==null)
    		quitMsg = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_quit", null);
    	else
    		session.getChannel(CHANNEL_NAME).part(quitMsg);
    	System.out.println(1);
    	
		int titleColor = 0xFF000000 + CLR_ERROR;
		SpannableString tit = new SpannableString("~~~~~[TERMINATED]~~~~~");
		tit.setSpan(new ForegroundColorSpan(titleColor), 0, tit.length(), 0);
		tit.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, tit.length(), 0);
		
		Spanned s =  (Spanned) TextUtils.concat("\n",Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : ""))
									 , tit);
		chatQueue.add(s);
		ircHandler.post(chatUpdater);
    	
    	new QuitPlz().execute(null);
    	System.out.println(2);
		mNotificationManager.cancel(Callisto.NOTIFICATION_ID);
		isFocused = false;
		System.out.println(3);
		if(IRC_wifiLock!=null && IRC_wifiLock.isHeld())
            IRC_wifiLock.release();
		System.out.println(6);
		finish();
    }
	
    private class QuitPlz extends android.os.AsyncTask<Void, Void, Void>
    {
    @Override
    protected Void doInBackground(Void ...voids)
    {
    	if(manager!=null)
    		manager.quit();
		manager = null;
		session = null;
    	return null;
    }
    }
    
  //INVITE_EVENT
  //NUMERIC_ERROR_EVENT
  //UnresolvedHostnameErrorEvent
    
	/*
	 *what
	 * WATCH???
	 * HELPOP???
	 * SETNAME???
	 * VHOST???
	 * MODE???
	 * 
	 *mod
	 * INVITE
	 * KICK
	 * http://www.geekshed.net/commands/ircop/
	 */
    /** Called when the this class receives any type of IRC event. */
	public void receiveEvent(IRCEvent e)
	{
		try {
		Log.d("IRCCHat:receiveEvent", e.getRawEventData());
		Log.d("IRCCHat:receiveEvent", "---" + e.getType());
		switch(e.getType())
		{
		//Misc events
			case NICK_LIST_EVENT:
				nickList = ((NickListEvent) e).getNicks();
				Collections.sort(nickList, String.CASE_INSENSITIVE_ORDER);
				break;
				//TODO: This might not work. I dunno.
			case CTCP_EVENT:
				CtcpEvent ce = (CtcpEvent) e;
				String realEvent = ce.getCtcpString().substring(0, ce.getCtcpString().indexOf(" "));
				if(realEvent.equals("ACTION"))
				{
					String realAction = ce.getCtcpString().substring(realEvent.length()).trim();
					String realPerson = ce.getRawEventData().substring(1, ce.getRawEventData().indexOf("!"));
					chatQueue.add(getReceived("* " + realPerson + " " + realAction, null, CLR_ME));
					ircHandler.post(chatUpdater);
				}
				break;
			case AWAY_EVENT://This isn't even effing used! (for other people's away
				AwayEvent a = (AwayEvent) e;
				if(a.isYou())
				{
					chatQueue.add(getReceived("You are " + (a.isAway() ? " now " : " no longer ") + "away (" + a.getAwayMessage() + ")", null, CLR_TOPIC));					
				}
				chatQueue.add(getReceived("[AWAY]", a.getNick() + " is away: " + a.getAwayMessage(), CLR_TOPIC));
				ircHandler.post(chatUpdater);
				break;
				
		//Syslog events
			case SERVER_INFORMATION:
				//FORMAT
				ServerInformationEvent s = (ServerInformationEvent) e;
				ServerInformation S = s.getServerInformation();
				logQueue.add(getReceived("[INFO]", S.getServerName(), CLR_TOPIC));
				ircHandler.post(logUpdater);
				break;
			case SERVER_VERSION_EVENT:
				ServerVersionEvent sv = (ServerVersionEvent) e;
				logQueue.add(getReceived("[VERSION]", sv.getVersion(), CLR_TOPIC));
				ircHandler.post(logUpdater);
				break;
			case CONNECT_COMPLETE:
				ConnectionCompleteEvent c = (ConnectionCompleteEvent) e;
				logQueue.add(getReceived(null, c.getActualHostName() + "\nConnection complete", CLR_TOPIC));
				e.getSession().join(CHANNEL_NAME);
				ircHandler.post(logUpdater);
				break;
			case JOIN_COMPLETE:
				//JoinCompleteEvent jce = (JoinCompleteEvent) e;
				chatQueue.add(getReceived("[JOIN]", "Join complete, you are now orbiting Jupiter Broadcasting!", CLR_TOPIC));
                session.sayRaw("NAMES " + CHANNEL_NAME);
				if(profilePass!=null && profilePass!="")
					parseOutgoing("/MSG NickServ identify " + profilePass);
				System.out.println("Decrypted password: " + profilePass);
				ircHandler.post(chatUpdater);
				break;
			case MOTD:
				MotdEvent mo = (MotdEvent) e;
				logQueue.add(getReceived("[MOTD]", mo.getMotdLine(), CLR_TOPIC));
				ircHandler.post(logUpdater);
				break;
			case NOTICE:
				if(e.getRawEventData().contains("Your nickname is now being changed"))
				{
					System.out.println("Changing");
					//logout(null);
					//session.changeNick("Callisto-app");
				}
				NoticeEvent ne = (NoticeEvent) e;
				if((ne.byWho()!=null && ne.byWho().equals("NickServ")) || e.getRawEventData().startsWith(":NickServ"))
				{
					chatQueue.add(getReceived("[NICKSERV]", ne.getNoticeMessage(), CLR_TOPIC));
					System.out.println("FUCK YEAH BANANAS " + chatQueue.size());
					ircHandler.post(chatUpdater);
				}
				else
				{
					logQueue.add(getReceived("[NOTICE]", ne.getNoticeMessage(), CLR_TOPIC));
					System.out.println("fuck no bananas");
					ircHandler.post(logUpdater);
				}
				
				break;
			
		//Chat events
			case TOPIC:
				TopicEvent t = (TopicEvent) e;
				if(t.getTopic()=="")
					chatQueue.add(getReceived("[TOPIC] ", "No Topic Set", CLR_TOPIC));
				else
					chatQueue.add(getReceived("[TOPIC] " + t.getTopic() + " (set by " + t.getSetBy() + " on " + t.getSetWhen() + " )", null, CLR_TOPIC));
				ircHandler.post(chatUpdater);
				break;
			
			case PRIVATE_MESSAGE:
			case CHANNEL_MESSAGE:
				MessageEvent m = (MessageEvent) e;
				if((e.getType()).equals(jerklib.events.IRCEvent.Type.PRIVATE_MESSAGE))
					chatQueue.add(getReceived("->" + m.getNick(), m.getMessage(), CLR_PM));
				else
					chatQueue.add(getReceived(m.getNick(), m.getMessage(), null));
				System.out.println("CHANNEL_MESSAGE: " + m.getMessage());
				ircHandler.post(chatUpdater);
				break;
			case JOIN:
				JoinEvent j = (JoinEvent) e;
				nickList.add(j.getNick());
				chatQueue.add(getReceived(j.getNick() + " entered the room.", null, CLR_JOIN));
				ircHandler.post(chatUpdater);
				break;
			case NICK_CHANGE:
				NickChangeEvent ni = (NickChangeEvent) e;
				chatQueue.add(getReceived(ni.getOldNick() + " changed their nick to " + ni.getNewNick(), null, CLR_NICK));
				ircHandler.post(chatUpdater);
				break;
			case PART:
				PartEvent p = (PartEvent) e;
				nickColors.remove(p.getNick());
				nickList.remove(p.getNick());
				chatQueue.add(getReceived("PART: " + p.getNick() + " (" + p.getPartMessage() + ")", null, CLR_PART));
				ircHandler.post(chatUpdater);
				break;
			case QUIT:
				QuitEvent q = (QuitEvent) e;
				nickColors.remove(q.getNick());
				nickList.remove(q.getNick());
				chatQueue.add(getReceived("QUIT:  " + q.getNick() + " (" + q.getQuitMessage() + ")", null, CLR_QUIT));
				ircHandler.post(chatUpdater);
				break;
			case KICK_EVENT:
				KickEvent k = (KickEvent) e;
				chatQueue.add(getReceived("KICK:  " + k.getWho() + " was kicked by " + k.byWho()  + ". (" + k.getMessage() + ")", null, CLR_KICK));
				ircHandler.post(chatUpdater);
				break;
			case NICK_IN_USE:
				NickInUseEvent n = (NickInUseEvent) e;
				chatQueue.add(getReceived("NICKINUSE:  " + n.getInUseNick() + " is in use.", null, CLR_ERROR));
				ircHandler.post(chatUpdater);
				break;
			case WHO_EVENT:
				WhoEvent we = (WhoEvent) e;
				chatQueue.add(getReceived("[WHO]", we.getNick() + " is " + we.getUserName() + "@" + we.getServerName() + " (" + we.getRealName() + ")", CLR_TOPIC));
				ircHandler.post(chatUpdater);
				break;
			case WHOIS_EVENT:
				WhoisEvent wie = (WhoisEvent) e;
				String var = "";
				for(String event : wie.getChannelNames())
					var = var + " " + event;
				chatQueue.add((Spanned) TextUtils.concat(getReceived("[WHOIS]", wie.getUser() + " is " + wie.getHost() + "@" + wie.whoisServer() + " (" + wie.getRealName() + ")", CLR_TOPIC)
						, getReceived("[WHOIS]", wie.getUser() + " is a user on channels: " + var, CLR_TOPIC)
						, getReceived("[WHOIS]", wie.getUser() + " has been idle for " + wie.secondsIdle() + " seconds", CLR_TOPIC)
						, getReceived("[WHOIS]", wie.getUser() + " has been online since " + wie.signOnTime(), CLR_TOPIC)));
				ircHandler.post(chatUpdater);
				break;
			case WHOWAS_EVENT: //TODO: Fix?
				WhowasEvent wwe = (WhowasEvent) e;
				chatQueue.add(getReceived("[WHO]", wwe.getNick() + " is " + wwe.getUserName() + "@" + wwe.getHostName() + " (" + wwe.getRealName() + ")", CLR_TOPIC));
				break;
			
			//Errors that display in both
			case CONNECTION_LOST:
				//ConnectionLostEvent co = (ConnectionLostEvent) e;
				chatQueue.add(getReceived("CONNECTION WAS LOST", null, CLR_ERROR));
				logQueue.add(getReceived("CONNECTION WAS LOST", null, CLR_ERROR));
				ircHandler.post(chatUpdater);
				ircHandler.post(logUpdater);
				break;
			case ERROR:
				//ErrorEvent ev = (ErrorEvent) e;
				String rrealmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
				this.loopTask.cancel();
				loopTimer.purge();
				chatQueue.add(getReceived("[ERROR]", rrealmsg + " - attempt " + session.getRetries(), CLR_ERROR));
				logQueue.add(getReceived("[ERROR]", rrealmsg + " - attempt " + session.getRetries(), CLR_ERROR));
				ircHandler.post(chatUpdater);
				ircHandler.post(logUpdater);
                /*
				manager.quit();
				manager = null;
				session = null;
				*/
				updateMenu();
				break;
				
			//Events not handled by jerklib
			case DEFAULT:		//ping
				String realType = e.getRawEventData();
				String realmsg = "";
				if(realType.startsWith("PING"))
					realType = "PING";
				else
					realType = realType.substring(realType.indexOf(" ")+1, realType.indexOf(" ", realType.indexOf(" ")+1));
				int i=0;
				try {
					i = Integer.parseInt(realType);
				} catch(Exception asdf) {}
				
				Log.d("IRCChat:receiveEvent:DEFAULT", realType);
				//PING     //TEST
				if(realType.equals("PING"))
				{
					break;
				}
				//ISON
				if(realType.equals("303"))
				{
					String name = e.getRawEventData().substring(e.getRawEventData().lastIndexOf(":")+1);
					if(name.trim().equals(""))
						return;
					chatQueue.add(getReceived("[ISON]", name + " is online", CLR_TOPIC));
					ircHandler.post(chatUpdater);
					break;
				}
				//MAP
				if(realType.equals("006"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					logQueue.add(getReceived("[MAP] " + realmsg, null, CLR_TOPIC));
					ircHandler.post(logUpdater);
					break;
				}
				//LUSERS
				if((i>=250 && i<=255) || i==265 || i==266)
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					logQueue.add(getReceived("[LUSERS]", realmsg, CLR_TOPIC));
					ircHandler.post(logUpdater);
					break;
				}
				/*
				//VERSION //CLEAN: Is this even needed?
				if(realType.equals("351"))
				{
					String realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[VERSION] " + realmsg, null, CLR_TOPIC);
					chatHandler.post(logUpdater);
					return;
				}
				*/
				//RULES
				if(realType.equals("232") || realType.equals("309"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					logQueue.add(getReceived("[RULES]", realmsg, CLR_TOPIC));
					ircHandler.post(logUpdater);
					break;
				}
				//LINKS
				else if(realType.equals("364") || realType.equals("365"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					logQueue.add(getReceived("[LINKS]", realmsg, CLR_TOPIC));
					ircHandler.post(logUpdater);
					break;
				}
				//ADMIN
				else if(i>=256 && i<=259)
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					chatQueue.add(getReceived("[ADMIN]", realmsg, CLR_TOPIC));
					ircHandler.post(chatUpdater);
					break;
				}
				//WHO part 2
				else if(realType.equals("315"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					chatQueue.add(getReceived("[WHO]", realmsg, CLR_TOPIC));
					ircHandler.post(chatUpdater);
				}
				//WHOIS part 2
				else if(realType.equals("307"))
				{
					int ijk = e.getRawEventData().lastIndexOf(" ", e.getRawEventData().indexOf(":",2)-2)+1;
					realmsg = e.getRawEventData().substring(ijk, e.getRawEventData().indexOf(":", 2)-1)
							+ " " + e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					chatQueue.add(getReceived("[WHOIS]", realmsg, CLR_TOPIC));
					ircHandler.post(chatUpdater);
				}
				//USERHOST
				else if(realType.equals("302"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					realmsg.replaceFirst(Pattern.quote("=+"), " is ");	//TODO: Not working? eh?
					chatQueue.add(getReceived("[USERHOST]", realmsg, CLR_TOPIC));
					ircHandler.post(chatUpdater);
				}
				//CREDITS
				else if(realType.equals("371"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					realmsg.replaceFirst(Pattern.quote("=+"), " is ");	//TODO: Not working? eh?
					logQueue.add(getReceived("[CREDITS]", realmsg, CLR_TOPIC));
					ircHandler.post(logUpdater);
				}
				//TIME
				else if(realType.equals("391"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					chatQueue.add(getReceived("[TIME]", realmsg, CLR_TOPIC));
					ircHandler.post(chatUpdater);
				}
				//USERIP
				else if(realType.equals("340"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					chatQueue.add(getReceived("[USERIP]", realmsg, CLR_TOPIC));
					ircHandler.post(chatUpdater);
				}
				//Nicklist? something else? MOTD
				else if(realType.equals("329") || realType.equals("332"))
					break;
                //Names
                else if(realType.equals("353"))
                {
                    realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                    int symbol = -1;
                    int endOfNick = 0;
                    String a_nick;
                    char symbols[] = new char[] {'~', '&', '@', '%', '+'};
                    HashSet<String>[] arrays = new HashSet[] { NickList.Admins, NickList.Owners, NickList.Operators, NickList.HalfOperators, NickList.Voices};

                    for(int index=0; index<symbols.length; index++)
                    {
                        while(symbol<realmsg.length())
                        {
                            symbol = realmsg.indexOf(symbols[index], symbol+1);
                            if(symbol==-1)
                                break;
                            endOfNick = realmsg.indexOf(' ', symbol+1);
                            if(endOfNick==-1)
                                endOfNick=realmsg.length()-1;
                            a_nick = realmsg.substring(symbol+1,endOfNick);
                            arrays[index].add(a_nick.toLowerCase());
                            //NickList.Operators.add(a_nick);
                            symbol = endOfNick+1;
                        }
                    }

                    //TODO: Adjust NAMES so it works for manual commands too
                    //chatQueue.add(getReceived("[NAMES]", realmsg, CLR_TOPIC));
                    //ircHandler.post(chatUpdater);
                }
				//etc
				else
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					logQueue.add(getReceived("[" + realType + "]", realmsg, CLR_TOPIC));
					ircHandler.post(logUpdater);
				}
				break;
			default:
				break;
			
		}
		} catch(Exception e2)
		{
			System.err.println("Dude what? " + e.getRawEventData());
			e2.printStackTrace();
		}
		System.out.println("rETRY: " + session.getRetries());
		if(session.getRetries()>0)
		{
			if(session.getRetries() >= manager.getRetries())
			{
				chatQueue.add(getReceived("[Callisto]", "Maximum amount of retries exceeded. Quitting.", CLR_TOPIC));
				ircHandler.post(chatUpdater);
			}
			else if(false)
			{
				chatQueue.add(getReceived("[Callisto]", "Retrying connection: attempt " + session.getRetries(), CLR_TOPIC));
				ircHandler.post(chatUpdater);
			}
		}
	}
	

	/** Gets a Spanned (i.e. formatted) message from the title, message, and color.
	 * @param theTitle The title (e.g. a nick)
	 * @param theMessage The message
	 * @param specialColor The name of the color resource for the message, or null if it should be looked up from the color list
	 * @return
	 */
	private Spanned getReceived (String theTitle, String theMessage, Integer specialColor)
	{
		int titleColor = 0xFF000000;
		int msgColor = 0xFF000000;
		try {
		 titleColor+= (specialColor!=null ? specialColor :	getNickColor(theTitle));
         if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_modes", false)
            && specialColor==null)
         {
             if(NickList.Owners.contains(theTitle.toLowerCase()))
                 theTitle = "~" + theTitle;
             else if(NickList.Admins.contains(theTitle.toLowerCase()))
                 theTitle = "&" + theTitle;
             else if(NickList.Operators.contains(theTitle.toLowerCase()))
                 theTitle = "@" + theTitle;
             else if(NickList.HalfOperators.contains(theTitle.toLowerCase()))
                 theTitle = "%" + theTitle;
             else if(NickList.Voices.contains(theTitle.toLowerCase()))
                 theTitle = "+" + theTitle;
         }
		 if(theMessage!=null)
			 msgColor+= specialColor;
		} catch(NullPointerException e) {
		}
		System.out.println("getReceived: " + theMessage);
		if((theMessage!=null && session!=null && theMessage.contains(session.getNick()))
            || theTitle.startsWith("->")) //If it's a PM
		{
			msgColor = 0xFF000000 + CLR_MENTION;
			System.out.println("MENTIONED" + isFocused);
			if(!isFocused)
			{
				if(Callisto.notification_chat==null)
					Callisto.notification_chat = new Notification(R.drawable.ic_action_dialog, "Connecting to IRC", System.currentTimeMillis());
                mentionCount++;
                if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_vibrate", false) &&
                        (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_vibrate_all", false) || mentionCount==1))
                    Callisto.notification_chat.defaults |= Notification.DEFAULT_VIBRATE;
				Callisto.notification_chat.setLatestEventInfo(getApplicationContext(), "In the JB Chat",  mentionCount + " new mentions", contentIntent);
				mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
			}
		}
		else
			msgColor = 0xFF000000 + CLR_TEXT;
		
		java.util.ArrayList<Integer[]> bold = new java.util.ArrayList<Integer[]>();
		java.util.ArrayList<Integer[]> underline = new java.util.ArrayList<Integer[]>();
		while(theMessage!=null && theMessage.contains(""))
		{
			System.out.println("Nerts1" + theMessage);
			Integer temp[] = {theMessage.indexOf(""),
							  theMessage.indexOf("", theMessage.indexOf("")+1)};
			bold.add(temp); 
			theMessage = theMessage.replaceFirst("", "").replaceFirst("", "");
		}
		while(theMessage!=null && theMessage.contains(""))
		{
			System.out.println("Nerts2");
			Integer temp[] = {theMessage.indexOf(""),theMessage.indexOf("", theMessage.indexOf("")+1)};
			underline.add(temp); 
			theMessage = theMessage.replaceFirst("", "").replaceFirst("", "");
		}
		
		SpannableString tit = new SpannableString(theTitle==null ? "" : theTitle);
		SpannableString mes = new SpannableString(theMessage==null ? "" : parseEmoticons(theMessage));
		try {
			if(theTitle!=null)
			{
				if(theMessage!=null)
					tit = new SpannableString(tit + ": ");
				tit.setSpan(new ForegroundColorSpan(titleColor), 0, tit.length(), 0);
				tit.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, tit.length(), 0);
			}
			if(theMessage!=null)
			{
				mes.setSpan(new ForegroundColorSpan(msgColor), 0, mes.length(), 0);
				while(!bold.isEmpty() && true)
				{
					mes.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
							bold.get(0)[0],
							bold.get(0)[1],
							0);
					bold.remove(0);
				}
				while(!underline.isEmpty() && true)
				{
					mes.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC),
							bold.get(0)[0],
							bold.get(0)[1],
							0);
					underline.remove(0);
				}
			}
		} catch(Exception ie) {
		}
		
		return (Spanned) TextUtils.concat("\n", Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : ""))
									 , tit, mes);
	}
	
	/** Gets the nick color from the list, adding it if necessary.
	 * @param nickInQ The nick in question
	 * @return The name of the color resource for that specific nick
	 */
	public Integer getNickColor(String nickInQ)
	{
		if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("nick_colors", true))
			return PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_etcnick", 0x2E8B91);
		if(irssi)
			return IRSSI_GREEN;
		if(!nickColors.containsKey(nickInQ))
			nickColors.put(nickInQ, getRandomColor());
		return (Integer) nickColors.get(nickInQ);
	}
	
	/** Generates a random color.
	 * @return An integer with the hex value of a random color. */
	public Integer getRandomColor()
	{
		int back = CLR_BACK;
		int background_avg = 0;
		for(int i=0; i<6; ++i)
		{
			background_avg += back % 16;
			back %= 16;
		}
		background_avg /= 6;
		
		int rndm;
		Random randomGenerator = new Random();
		do {
			rndm = 0;
			for(int i=0; i<6; ++i)
			{
				if(background_avg>0x7)
					rndm += ((randomGenerator.nextInt(background_avg - 0x7) + 0x0) << (i*4));
				else
					rndm += ((randomGenerator.nextInt(0xF - 0x7 + background_avg) + 0x7 + background_avg) << (i*4));
			}
			System.out.println("Rndm: " + rndm);
		} while(nickColors.containsValue(rndm));
		return rndm;
		
		/*
		int rndm = 0xFFFFFF;
		do {
			rndm = 0 + (int)(Math.random() * ((0xFFFFFF - 0) + 1));
		} while(!isAcceptable(rndm) && nickColors.containsValue(rndm));
		return rndm;
		//*/
	}
	
	/** Determines if a random color is acceptable.
	 * @param rgb The RGB (hex) color to examine
	 * @return True if it's acceptable, false otherwise. */
	private boolean isAcceptable(int rgb)
	{
		int red = (rgb >> 16) & 0x000000FF;
		int green = (rgb >> 8) & 0x000000FF;
		int blue = (rgb) & 0x000000FF;

		int ans = ((red*299)+(green*587)+(blue*114))/1000;
		return (ans >= 128) ? true : false;
	}

	/** Runnable to send a message in the UI thread. */
	Runnable sendMessage = new Runnable(){
		@Override
        public void run() {
			String newMessage = input.getText().toString();
			if(newMessage.length()==0)
				return;
			SpannableString st = new SpannableString(session.getNick());
			int colorBro = 0xFF000000 + CLR_ME;
			int colorBro2 = 0xFF000000 + CLR_TEXT;
			
			
			SpannableString st2 = new SpannableString(parseEmoticons(newMessage));
			try {
			st.setSpan(new ForegroundColorSpan(colorBro), 0, session.getNick().length(), 0);
			st.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, session.getNick().length(), 0);
			st2.setSpan(new ForegroundColorSpan(colorBro2), 0, newMessage.length(), 0);
			}
			catch(Exception e) {}
			
			if(parseOutgoing(newMessage))
			{
 				Spanned x = (Spanned) TextUtils.concat(
						"\n",
						Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "")),
						st,
						": ",
						st2
						);
				Callisto.chatView.append(x);
				Linkify.addLinks(Callisto.chatView, Linkify.EMAIL_ADDRESSES);
	            Linkify.addLinks(Callisto.chatView, Linkify.WEB_URLS);
	            Callisto.chatView.invalidate();
			}
			
			input.requestFocus();
			input.setText("");
		}
	};

    private static HashMap<String, Integer> smilyRegexMap = null;
    public CharSequence parseEmoticons(String s)
    {
        if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_emoticons", false))
            return s;

        //ORDER HERE MATTERS
        if(smilyRegexMap==null)
        {
            smilyRegexMap = new HashMap<String, Integer>();
            //smilyRegexMap.put( "<3", R.drawable.ic_action_heart);                  //  <3
            smilyRegexMap.put( ">:(D|\\))", R.drawable.ic_action_emo_evil);        //  >:D or >:)
            smilyRegexMap.put( ":-?\\)", R.drawable.ic_action_emo_basic);          //  :-) or :)
            smilyRegexMap.put( ">:-?(\\(|\\|)", R.drawable.ic_action_emo_angry);   //  >:| or >:( or >:-| or >:-(
            smilyRegexMap.put( "B-\\)", R.drawable.ic_action_emo_basic);           //  B-)
            smilyRegexMap.put( ":'-?\\(", R.drawable.ic_action_emo_cry);           //  :'-( or :'(
            smilyRegexMap.put( ":-?(\\\\|/)", R.drawable.ic_action_emo_err);       //  :\ or :/ or :-\ or :-/
            smilyRegexMap.put( ":-\\*", R.drawable.ic_action_emo_kiss);            //  :-*
            smilyRegexMap.put( ":-?D", R.drawable.ic_action_emo_laugh);            //  :-D or :D
            smilyRegexMap.put( "(x|X)D", R.drawable.ic_action_emo_laugh);          //  XD or xD
            smilyRegexMap.put( ":-?(\\(|\\[)", R.drawable.ic_action_emo_sad);      //  :( or :-( or :[ or :-[
            smilyRegexMap.put( ":-?S", R.drawable.ic_action_emo_shame);            //  :S or :-S
            smilyRegexMap.put( "(:|X|x)-?P", R.drawable.ic_action_emo_tongue);     //  :P or :-P or xP or XP or X-P or x-P
            smilyRegexMap.put( ";-?\\)", R.drawable.ic_action_emo_wink);           //  ;-) or ;)

            smilyRegexMap.put( ":-?(O|o)", R.drawable.ic_action_emo_wonder);       //  :O or :o or :-O or :-o
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(s);

        @SuppressWarnings("rawtypes")
        Iterator it = smilyRegexMap.entrySet().iterator();
        while (it.hasNext())
        {
            @SuppressWarnings("rawtypes")
            Map.Entry pairs = (Map.Entry) it.next();
            Pattern mPattern = Pattern.compile((String) pairs.getKey(),Pattern.CASE_INSENSITIVE);
            Matcher matcher = mPattern.matcher(s);

            while (matcher.find())
            {
                Bitmap smiley = BitmapFactory.decodeResource(Callisto.RESOURCES, ((Integer) pairs.getValue()));
                Object[] spans = builder.getSpans(matcher.start(), matcher.end(), ImageSpan.class);
                if (spans == null || spans.length == 0)
                {
                    builder.setSpan(new ImageSpan(smiley), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return builder;
    }


	
	/** Parses the outgoing message to determine its type.
	 * @param msg The message to be sent
	 * @return True if the output should be appended to the chat log, false otherwise.
	 */
	private boolean parseOutgoing(String msg)
	{
		msg = msg.replace("\n", "");
		
		if(!msg.startsWith("/"))
		{
			session.getChannel(CHANNEL_NAME).say(msg);
			return true;
		}
		
		if(msg.toUpperCase().startsWith("/NS "))
			msg = "/MSG NickServ " + msg.substring("/NS ".length());
		
		if(msg.toUpperCase().startsWith("/NICK "))
		{
			msg =  msg.substring("/NICK ".length());
			session.changeNick(msg);
			return false;
		}
		else if(msg.toUpperCase().startsWith("/QUIT") || msg.toUpperCase().startsWith("/PART"))
		{
			if(msg.equals("/QUIT") || msg.equals("/PART"))
				logout(null);
			else
				logout(msg.substring("/QUIT ".length()));
			return false;
		}
		else if(msg.toUpperCase().startsWith("/WHO "))
		{
			session.who(msg.substring("/WHO ".length()));
			return false;
		}
		else if(msg.toUpperCase().startsWith("/WHOIS "))
		{
			session.whois(msg.substring("/WHOIS ".length()));
			return false;
		}
		else if(msg.toUpperCase().startsWith("/WHOWAS "))
		{
			session.whoWas(msg.substring("/WHOWAS ".length()));
			return false;
		}
		else if(msg.toUpperCase().startsWith("/MSG "))
		{
			String targetNick = msg.substring(
					("/MSG ").length(), msg.indexOf(" ", "/MSG ".length()+1));
			String targetMsg = msg.substring("/MSG ".length() + targetNick.length());
			session.sayPrivate(targetNick, targetMsg);
			if(!targetNick.toUpperCase().equals("NICKSERV") && targetMsg.toUpperCase().startsWith("IDENTIFY"))
			{
				chatQueue.add(getReceived("<-" + targetNick, targetMsg, CLR_PM));
				ircHandler.post(chatUpdater);
			}
			return false;
		}
		else if(msg.toUpperCase().startsWith("/ISON ")
		|| msg.toUpperCase().equals("/MOTD")
		|| msg.toUpperCase().equals("/RULES")
		|| msg.toUpperCase().equals("/LUSERS")
		|| msg.toUpperCase().startsWith("/MAP")
		|| msg.toUpperCase().startsWith("/VERSION")
		|| msg.toUpperCase().startsWith("/LINKS")
		|| msg.toUpperCase().startsWith("/IDENTIFY ")
		|| msg.toUpperCase().startsWith("/ADMIN")
		|| msg.toUpperCase().startsWith("/USERHOST")
		|| msg.toUpperCase().startsWith("/TOPIC ")
		|| msg.toUpperCase().startsWith("/CREDITS")
		|| msg.toUpperCase().startsWith("/TIME")
		|| msg.toUpperCase().startsWith("/DNS")		//Denied
		|| msg.toUpperCase().startsWith("/USERIP ")
		|| msg.toUpperCase().startsWith("/STATS ")	//Denied
		|| msg.toUpperCase().startsWith("/MODULE")	//Posts as "Notice", not "Module". I am ok with this.
		)
		{
			session.sayRaw(msg.substring(1));
			return false;
		}
		else if(msg.toUpperCase().startsWith("/ME "))
		{
			session.action(CHANNEL_NAME, "ACTION" + msg.substring(3));
			chatQueue.add(getReceived("* " + session.getNick() + msg.substring(3), null, CLR_ME));
			ircHandler.post(chatUpdater);
			return false;
		}
		
		if(msg.toUpperCase().startsWith("/PING ")) //TODO: I have no clue if this works right.
		{
			session.action(CHANNEL_NAME, "PING" + msg.substring("/PING".length()));
			//session.ctcp(msg.substring("/PING ".length()), "ping");
			return true;
		}
		else if(msg.toUpperCase().startsWith("/AWAY "))
		{
			session.setAway(msg.toUpperCase().substring("/AWAY ".length()));
			return false;	//TODO: CHECK
		}
		else if(msg.toUpperCase().equals("/AWAY"))
		{
			if(session.isAway())
				session.unsetAway();
			else
				session.setAway("Gone away for now");
			return false;
		}
		
		else if(msg.toUpperCase().startsWith("/JOIN ")
	    || msg.toUpperCase().startsWith("/CYCLE ")
	    || msg.toUpperCase().startsWith("/LIST ")
	    || msg.toUpperCase().startsWith("/KNOCK "))
		{
			Toast.makeText(IRCChat.this, "What, is the JB chat not enough for you?!", Toast.LENGTH_SHORT).show();
			return false;
		}
		else if(msg.startsWith("/"))
		{
			String unknown = msg.substring(1, msg.indexOf(" "));
			Toast.makeText(IRCChat.this, "The command '" + unknown + "' is unknown. E-mail the developer if it's something that Callisto is missing.", Toast.LENGTH_LONG).show();
			return false;
		}
		/*
		if(msg.toUpperCase().startsWith("/MOTD ")
		|| msg.toUpperCase().startsWith("/LUSERS "))
		{
			Toast.makeText(IRCChat.this, "Stop that, you're trying to break stuff", Toast.LENGTH_SHORT).show();
			return false;
		}
		*/
		chatQueue.add(getReceived("[CALLISTO]", "Command not recognized!", CLR_TOPIC));
		ircHandler.post(chatUpdater);
		return false;
	}
	
	/** A runnable to update the log */
	Runnable logUpdater = new Runnable()
	{
        @Override
        public void run()
        {
            Callisto.logView.append(logQueue.remove());
            Callisto.logView.invalidate();
            sv2.fullScroll(ScrollView.FOCUS_DOWN);
        };	

    };
    
    /** A function object to flash the background of an EditText control.
	 *  Changes the background, waits a certain amount of time, then changes it back
	 * @author notbryant */
	static class EditTextFlash
	{
		SpannableString inputText;
		android.text.style.BackgroundColorSpan flash;
		EditText view;
		Handler H;
		int selA, selB;
		/**
		 * @param color The color the flash the background; Salmon (0xFA8072) is a good choice
		 * @param view The EditText to be flashed
		 * @param time The time in milliseconds to flash it
		 * @param H A handler to use to update the view
		 */
		EditTextFlash(int color, EditText view, int time, Handler H)
		{
			this.view = view;
			this.H = H;
			color = Callisto.RESOURCES.getColor(com.qweex.callisto.R.color.Salmon);
			flash = new android.text.style.BackgroundColorSpan(color);
			
			inputText = new SpannableString(view.getText().toString());
    		selA = view.getSelectionStart();
    		selB = view.getSelectionEnd();
    		inputText.setSpan(flash, 0, inputText.length(), 0);
    		view.setText(inputText);
    		view.setSelection(selA, selB);
    		H.postDelayed(R, time);
		}
		private Runnable R = new Runnable()
		{
	        @Override
	        public void run()
	        {
	    		inputText.removeSpan(flash);
	    		view.setText(inputText);
	    		view.setSelection(selA, selB);;
	        }
		};
	}
    
    
	/** A simple but effective obfusication cypher.
	 * https://svn.apache.org/repos/asf/cayenne/main/branches/cayenne-jdk1.5-generics-unpublished/src/main/java/org/apache/cayenne/conf/Rot47PasswordEncoder.java
	 * Used under the Apache license
	 * I took the relevant function and extracted it out of the class. I also removed comments to make the code size smaller.
	 */
    public String Rot47(String value)
    {
        if(value==null)
            return null;
        int length = value.length();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++)
        {
          char c = value.charAt(i);

          // Process letters, numbers, and symbols -- ignore spaces.
          if (c != ' ')
          {
            // Add 47 (it is ROT-47, after all).
            c += 47;

            if (c > '~')
              c -= 94;
          }

          result.append(c);
        }

        return result.toString();
      }

}
