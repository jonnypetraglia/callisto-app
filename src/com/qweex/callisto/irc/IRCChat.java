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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.ServerInformation;
import jerklib.Session;
import jerklib.events.*;
import jerklib.events.IRCEvent.Type;
import jerklib.listeners.IRCEventListener;
import jerklib.util.NickServAuthPlugin;


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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewAnimator;


/** The IRC portion of the app to connect to the JB chatroom.
 * @author MrQweex
 */

public class IRCChat extends Activity implements IRCEventListener
{
	private static final int LOG_ID=Menu.FIRST+1;
	private static final int LOGOUT_ID=LOG_ID+1;
	private static final int NICKLIST_ID=LOGOUT_ID+1;
	private final String SERVER_NAME = "irc.geekshed.net";
	private final String CHANNEL_NAME = "#jupiterbroadcasting";
	private String profileNick;
	private String profilePass;
	private boolean SHOW_TIME = true;
	private int CLR_TEXT,
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
				   CLR_PM;
	private static ConnectionManager manager;
	private static Session session;
	private static NotificationManager mNotificationManager;
	private static int mentionCount = 0;
	private static PendingIntent contentIntent;
	private static boolean isFocused = false;
	private ScrollView sv, sv2;
	private EditText input;
	private static Spanned received;
	private SimpleDateFormat sdfTime = new SimpleDateFormat("'['HH:mm']'");
	private boolean isLandscape;
	private static HashMap<String, Integer> nickColors = new HashMap<String, Integer>();
	public static List<String> nickList;
	private static Handler chatHandler = null;
	private static Runnable chatUpdater;
	private boolean irssi;
	private int IRSSI_GREEN = 0x00B000;
	private MenuItem Nick_MI, Logout_MI, Log_MI;
	private WifiLock IRC_wifiLock;
	
	/** Called when the activity is first created. Sets up the view, mostly, especially if the user is not yet logged in.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(chatHandler==null)
		{
			chatHandler = new Handler();
			chatUpdater = new Runnable()
			{
		        @Override
		        public void run()
		        {
		        	System.out.println("RECEIVED:" + received.toString());
		        	if(received.equals(new SpannableString("")))
		        		return;
		            Callisto.chatView.append(received);
		            Linkify.addLinks(Callisto.chatView, Linkify.EMAIL_ADDRESSES);
		            Linkify.addLinks(Callisto.chatView, Linkify.WEB_URLS);
		            received = new SpannableString("");
		            Callisto.chatView.invalidate();
		            System.out.println(Callisto.chatView.getBottom() + " - (" + sv.getHeight() + " + " + sv.getScrollY() + ")<200");
		            boolean atBottom = Callisto.chatView.getBottom() - (sv.getHeight() + sv.getScrollY()) < 200;
		            if(atBottom)
		            	sv.fullScroll(ScrollView.FOCUS_DOWN);
		            input.requestFocus();
		            //sv.isDirty();
		        }
			};
		}
		isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
		mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		profileNick = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_nick", null);
		profilePass = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_pass", null);
		irssi = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_irssi", false);
		if(session!=null)
		{
			resume();
			return;
		}
		//Set up the login screen
		LinearLayout ll = new LinearLayout(this);
		ll.setBackgroundColor(Callisto.RESOURCES.getColor(R.color.backClr));
		ll.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params 
			= new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		ll.setLayoutParams(params);
		ll.setId(1337);
		ll.setPadding(getWindowManager().getDefaultDisplay().getHeight()/10,
				  getWindowManager().getDefaultDisplay().getHeight()/(isLandscape?6:4),
				  getWindowManager().getDefaultDisplay().getHeight()/10,
				  0);
		final EditText user = new EditText(this);
		final EditText pass = new EditText(this);
		final Button login = new Button(this);
		params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		user.setText(profileNick);
		pass.setText(profilePass);
		user.setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
		pass.setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
		login.setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
		user.setLayoutParams(params);
		pass.setLayoutParams(params);
		login.setLayoutParams(params);
		pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		login.setCompoundDrawables(Callisto.RESOURCES.getDrawable(R.drawable.ic_menu_login), null, null, null);
		
		user.setHint("Nick");
		pass.setHint("Password (Optional)");
		login.setText("Login");
		login.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
		login.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String nick = user.getText().toString();
				if(nick==null || nick.trim().equals(""))
				{
					Toast.makeText(IRCChat.this, "Dude, you have to enter a nick.", Toast.LENGTH_SHORT).show();
					return;
				}
				String passwd = pass.getText().toString();
				SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(v.getContext()).edit();
				e.putString("irc_nick", nick);
				e.putString("irc_pass", passwd);
				profileNick = nick;
				e.commit();
				initiate();
			}
		});
		
		WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		IRC_wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "Callisto_irc");
		
		ll.addView(user);
		ll.addView(pass);
		ll.addView(login);
		setContentView(ll);
	}
	

	/** Called when any key is pressed. Used to prevent the activity from finishing if the user is logged in.
	 * @param keyCode I dunno
	 * @param event I dunno
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

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
			/*
			Intent i = new Intent(IRCChat.this, Callisto.class);
            startActivity(i);
	        return true;
	        */
		}
	    if (keyCode == KeyEvent.KEYCODE_SEARCH)
	    {
	    	String t = input.getText().toString();
	    	int i = input.getSelectionStart();
	    	int i2 = input.getSelectionEnd();
	    	if(i!=i2)
	    		return false;
	    	t = t.substring(0, i);
	    	t = t.substring(t.lastIndexOf(" ")+1);
	    	String s = "";
	    	
	    	Iterator<String> iterator = nickList.iterator();
	    	while(iterator.hasNext())
	    	{
	    		s = (String) iterator.next();
	    		if(s.toUpperCase().equals(t.toUpperCase()))
	    		{
	    			s = (String) iterator.next();
	    			break;
	    		}
	    		if(s.toUpperCase().startsWith(t.toUpperCase()))
	    			break;
	    	}
	    	if(!iterator.hasNext())
	    		s = null;
	    	if(s!=null)
	    	{
	    		String newt = input.getText().toString().substring(0,i-t.length())
	    				+ s
	    				+ input.getText().toString().substring(i);
	    		input.setText(newt);
	    		try {
	    			input.setSelection(i-t.length()+s.length());
	    		} catch(Exception e){}
	    	}
	    	else
	    	{
	    		//TODO: Notify user that no nick was found
	    		/*
	    		input.setBackgroundColor(Callisto.RESOURCES.getColor(R.color.Salmon));
	    		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
	    		input.setBackgroundDrawable(Callisto.RESOURCES.getDrawable(android.R.drawable.editbox_background));
	    		//*/
	    	}
	    }
	    return super.onKeyDown(keyCode, event);
	} 
	
	/** Called when it is time to create the menu.
	 * @param menu Um, the menu
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	Log_MI = menu.add(0, LOG_ID, 0, "Log").setIcon(R.drawable.ic_menu_chat_dashboard);
    	Nick_MI = menu.add(0, NICKLIST_ID, 0, "NickList").setIcon(R.drawable.ic_menu_allfriends);
    	Logout_MI = menu.add(0, LOGOUT_ID, 0, "Logout").setIcon(R.drawable.ic_menu_close_clear_cancel);
		updateMenu();
        return true;
    }
    
    /** Updates the menu items because invalidateOptionsMenu() is not supported on all APIs. */
    private void updateMenu()
    {
    	try {
    	Log_MI.setEnabled(session!=null);
    	Logout_MI.setEnabled(session!=null);
    	Nick_MI.setEnabled(session!=null);
    	} catch(Exception e) {
    		System.out.println("Unable to update menu items.");
    	}
    }
    
    /** Called when an item in the menu is pressed.
	 * @param item The menu item ID that was pressed
	 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case LOG_ID:
        	((ViewAnimator) findViewById(R.id.viewanimator)).showNext();
            return true;
        case NICKLIST_ID:
        	this.startActivityForResult(new Intent(this, NickList.class), 1);
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
	    System.out.println(resultCode);
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
		setContentView(R.layout.irc);
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
				return false;
			}
		});
		if(irssi)
		{
			((LinearLayout) findViewById(R.id.lin)).setBackgroundColor(0xFF000000);
			((ScrollView) findViewById(R.id.scrollView2)).setBackgroundColor(0xFF000000);
			if(android.os.Build.VERSION.SDK_INT>12) //android.os.Build.VERSION_CODES.GINGERBREAD_MR1
				input.setTextColor(0xff000000 + IRSSI_GREEN);
		}
		else
		{
			((LinearLayout) findViewById(R.id.lin)).setBackgroundColor(0xFF000000 + PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_back", 0xFFFFFF));
			((ScrollView) findViewById(R.id.scrollView2)).setBackgroundColor(0xFF000000 + PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_color_back", 0xFFFFFF));
		}
		
		if(session!=null)
			session.addIRCEventListener(this);
		if(Callisto.notification_chat!=null)
			Callisto.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
    }
    
	/** called to first initiate the IRC chat. Called only when the user has not logged in yet. */
    public void initiate()
    {
    	updateMenu();
    	findViewById(1337).startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
    	SHOW_TIME = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_time", true);
    	if(!IRC_wifiLock.isHeld())
            IRC_wifiLock.acquire();
    	
    	//Read colors
    	if(irssi)
    	{
    		CLR_TEXT=CLR_TOPIC=CLR_ME=CLR_JOIN=CLR_MYNICK=CLR_NICK=CLR_PART=CLR_QUIT=CLR_KICK=CLR_ERROR=CLR_MENTION=CLR_PM=IRSSI_GREEN;
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
    	}
    	resume();
		
		Intent notificationIntent = new Intent(this, IRCChat.class);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    	Callisto.notification_chat = new Notification(R.drawable.callisto, "Connecting to IRC", System.currentTimeMillis());
		Callisto.notification_chat.flags = Notification.FLAG_ONGOING_EVENT;
       	Callisto.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
       	mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
       	
       	
		manager = new ConnectionManager(new Profile(profileNick));
		session = manager.requestConnection(SERVER_NAME);
		if(profilePass!=null && profilePass!="")
		{
			final NickServAuthPlugin auth = new NickServAuthPlugin(profilePass, 'e', session, Arrays.asList(CHANNEL_NAME));
			session.onEvent(auth, Type.CONNECT_COMPLETE , Type.MODE_EVENT);
		}
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
		updateMenu();
    }
    
    /** Used to logout, quit, or part. */
    public void logout(String quitMsg)
    {
    	if(quitMsg==null)
    		quitMsg = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_quit", null);
    	if(quitMsg!=null)
    		session.getChannel(CHANNEL_NAME).part(quitMsg);
    	chatHandler.post(quitHandler);
		mNotificationManager.cancel(Callisto.NOTIFICATION_ID);
		isFocused = false;
		if(IRC_wifiLock.isHeld())
            IRC_wifiLock.release();
		finish();
    }
    
    Runnable quitHandler  = new Runnable()
	{
        @Override
        public void run()
        {
    		int titleColor = 0xFF000000 + CLR_ERROR;
    		SpannableString tit = new SpannableString("~~~~~[TERMINATED]~~~~~");
			tit.setSpan(new ForegroundColorSpan(titleColor), 0, tit.length(), 0);
			tit.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, tit.length(), 0);
    		
    		Spanned s =  (Spanned) TextUtils.concat(Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : ""))
    									 , tit, "\n");
    		Callisto.chatView.append(s);
        	manager.quit();
    		manager = null;
    		session = null;
        }
	};
    
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
					received = getReceived("* " + realPerson + " " + realAction, null, CLR_ME);
					chatHandler.post(chatUpdater);
				}
				break;
			case AWAY_EVENT://This isn't even effing used! (for other people's away
				AwayEvent a = (AwayEvent) e;
				if(a.isYou())
				{
					received = getReceived("You are " + (a.isAway() ? " now " : " no longer ") + "away (" + a.getAwayMessage() + ")", null, CLR_TOPIC);					
				}
				received = getReceived("[AWAY]", a.getNick() + " is away: " + a.getAwayMessage(), CLR_TOPIC);
				chatHandler.post(chatUpdater);
				break;
				
		//Syslog events
			case SERVER_INFORMATION:
				//FORMAT
				ServerInformationEvent s = (ServerInformationEvent) e;
				ServerInformation S = s.getServerInformation();
				received = getReceived("[INFO]", S.getServerName(), CLR_TOPIC);
				chatHandler.post(logUpdater);
				break;
			case SERVER_VERSION_EVENT:
				ServerVersionEvent sv = (ServerVersionEvent) e;
				received = getReceived("[VERSION]", sv.getVersion(), CLR_TOPIC);
				chatHandler.post(logUpdater);
				break;
			case CONNECT_COMPLETE:
				ConnectionCompleteEvent c = (ConnectionCompleteEvent) e;
				received = getReceived(null, c.getActualHostName() + "\nConnection complete", CLR_TOPIC);
				e.getSession().join(CHANNEL_NAME);
				chatHandler.post(logUpdater);
				break;
			case JOIN_COMPLETE:
				//JoinCompleteEvent jce = (JoinCompleteEvent) e;
				received = getReceived("[JOIN]", "Join complete, you are now orbiting Jupiter Broadcasting!", CLR_TOPIC);
				chatHandler.post(chatUpdater);
				break;
			case MOTD:
				MotdEvent mo = (MotdEvent) e;
				received = getReceived("[MOTD]", mo.getMotdLine(), CLR_TOPIC);
				chatHandler.post(logUpdater);
				break;
			case NOTICE:
				if(e.getRawEventData().contains("Your nickname is now being changed"))
				{
					System.out.println("Changing");
					logout(null);
					return;
					//session.changeNick("Callisto-app");
				}
				NoticeEvent ne = (NoticeEvent) e;
				if((ne.byWho()!=null && ne.byWho().equals("NickServ")) || e.getRawEventData().startsWith(":NickServ"))
				{
					received = getReceived("[NICKSERV]", ne.getNoticeMessage(), CLR_TOPIC);
					chatHandler.post(chatUpdater);
				}
				else
				{
					received = getReceived("[NOTICE]", ne.getNoticeMessage(), CLR_TOPIC);
					chatHandler.post(logUpdater);
				}
				
				break;
			
		//Chat events
			case TOPIC:
				TopicEvent t = (TopicEvent) e;
				received = getReceived(t.getTopic() +  " (set by " + t.getSetBy() + " on " + t.getSetWhen() + " )", null, CLR_TOPIC);
				chatHandler.post(chatUpdater);
				break;
			
			case PRIVATE_MESSAGE:
			case CHANNEL_MESSAGE:
				MessageEvent m = (MessageEvent) e;
				if((e.getType()).equals(jerklib.events.IRCEvent.Type.PRIVATE_MESSAGE))
					received = getReceived("->" + m.getNick(), m.getMessage(), CLR_PM);
				else
					received = getReceived(m.getNick(), m.getMessage(), null);
				System.out.println("CHANNEL_MESSAGE: " + m.getMessage());
				chatHandler.post(chatUpdater);
				break;
			case JOIN:
				JoinEvent j = (JoinEvent) e;
				nickList.add(j.getNick());
				received = getReceived(j.getNick() + " entered the room.", null, CLR_JOIN);
				chatHandler.post(chatUpdater);
				break;
			case NICK_CHANGE:
				NickChangeEvent ni = (NickChangeEvent) e;
				received = getReceived(ni.getOldNick() + " changed their nick to " + ni.getNewNick(), null, CLR_NICK);
				chatHandler.post(chatUpdater);
				break;
			case PART:
				PartEvent p = (PartEvent) e;
				nickColors.remove(p.getWho());
				nickList.remove(p.getWho());
				received = getReceived("PART: " + p.getWho() + " (" + p.getPartMessage() + ")", null, CLR_PART);
				chatHandler.post(chatUpdater);
				break;
			case QUIT:
				QuitEvent q = (QuitEvent) e;
				nickColors.remove(q.getNick());
				nickList.remove(q.getNick());
				received = getReceived("QUIT:  " + q.getNick() + " (" + q.getQuitMessage() + ")", null, CLR_QUIT);
				chatHandler.post(chatUpdater);
				break;
			case KICK_EVENT:
				KickEvent k = (KickEvent) e;
				received = getReceived("KICK:  " + k.getWho() + " was kicked by " + k.byWho()  + ". (" + k.getMessage() + ")", null, CLR_KICK);
				chatHandler.post(chatUpdater);
				break;
			case NICK_IN_USE:
				NickInUseEvent n = (NickInUseEvent) e;
				received = getReceived("NICKINUSE:  " + n.getInUseNick() + " is in use.", null, CLR_ERROR);
				chatHandler.post(chatUpdater);
				break;
			case WHO_EVENT:
				WhoEvent we = (WhoEvent) e;
				received = getReceived("[WHO]", we.getNick() + " is " + we.getUserName() + "@" + we.getServerName() + " (" + we.getRealName() + ")", CLR_TOPIC);
				chatHandler.post(chatUpdater);
				break;
			case WHOIS_EVENT:
				WhoisEvent wie = (WhoisEvent) e;
				String var = "";
				for(String event : wie.getChannelNames())
					var = var + " " + event;
				received = (Spanned) TextUtils.concat(getReceived("[WHOIS]", wie.getUser() + " is " + wie.getHost() + "@" + wie.whoisServer() + " (" + wie.getRealName() + ")", CLR_TOPIC)
						, getReceived("[WHOIS]", wie.getUser() + " is a user on channels: " + var, CLR_TOPIC)
						, getReceived("[WHOIS]", wie.getUser() + " has been idle for " + wie.secondsIdle() + " seconds", CLR_TOPIC)
						, getReceived("[WHOIS]", wie.getUser() + " has been online since " + wie.signOnTime(), CLR_TOPIC));
				chatHandler.post(chatUpdater);
				break;
			case WHOWAS_EVENT: //TODO: Fix?
				WhowasEvent wwe = (WhowasEvent) e;
				received = getReceived("[WHO]", wwe.getNick() + " is " + wwe.getUserName() + "@" + wwe.getHostName() + " (" + wwe.getRealName() + ")", CLR_TOPIC);
				break;
			
			//Errors that display in both
			case CONNECTION_LOST:
				//ConnectionLostEvent co = (ConnectionLostEvent) e;
				received = getReceived("CONNECTION WAS LOST", null, CLR_ERROR);
				chatHandler.post(chatUpdater);
				chatHandler.post(logUpdater);
				break;
			case ERROR:
				//ErrorEvent ev = (ErrorEvent) e;
				String rrealmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
				received = getReceived("[ERROR OCCURRED]", rrealmsg, CLR_ERROR);
				chatHandler.post(chatUpdater);
				chatHandler.post(logUpdater);
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
					return;
				}
				//ISON
				if(realType.equals("303"))
				{
					String name = e.getRawEventData().substring(e.getRawEventData().lastIndexOf(":")+1);
					if(name.trim().equals(""))
						return;
					received = getReceived("[ISON]", name + " is online", CLR_TOPIC);
					chatHandler.post(chatUpdater);
					return;
				}
				//MAP
				if(realType.equals("006"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[MAP] " + realmsg, null, CLR_TOPIC);
					chatHandler.post(logUpdater);
					return;
				}
				//LUSERS
				if((i>=250 && i<=255) || i==265 || i==266)
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[LUSERS]", realmsg, CLR_TOPIC);
					chatHandler.post(logUpdater);
					return;
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
					received = getReceived("[RULES]", realmsg, CLR_TOPIC);
					chatHandler.post(logUpdater);
					return;
				}
				//LINKS
				else if(realType.equals("364") || realType.equals("365"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[LINKS]", realmsg, CLR_TOPIC);
					chatHandler.post(logUpdater);
					return;
				}
				//ADMIN
				else if(i>=256 && i<=259)
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[ADMIN]", realmsg, CLR_TOPIC);
					chatHandler.post(chatUpdater);
					return;
				}
				//WHO part 2
				else if(realType.equals("315"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[WHO]", realmsg, CLR_TOPIC);
					chatHandler.post(chatUpdater);
				}
				//WHOIS part 2
				else if(realType.equals("307"))
				{
					int ijk = e.getRawEventData().lastIndexOf(" ", e.getRawEventData().indexOf(":",2)-2)+1;
					realmsg = e.getRawEventData().substring(ijk, e.getRawEventData().indexOf(":", 2)-1)
							+ " " + e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[WHOIS]", realmsg, CLR_TOPIC);
					chatHandler.post(chatUpdater);
				}
				//USERHOST
				else if(realType.equals("302"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					realmsg.replaceFirst(Pattern.quote("=+"), " is ");	//TODO: Not working? eh?
					received = getReceived("[USERHOST]", realmsg, CLR_TOPIC);
					chatHandler.post(chatUpdater);
				}
				//CREDITS
				else if(realType.equals("371"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					realmsg.replaceFirst(Pattern.quote("=+"), " is ");	//TODO: Not working? eh?
					received = getReceived("[CREDITS]", realmsg, CLR_TOPIC);
					chatHandler.post(logUpdater);
				}
				//TIME
				else if(realType.equals("391"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[TIME]", realmsg, CLR_TOPIC);
					chatHandler.post(chatUpdater);
				}
				//USERIP
				else if(realType.equals("340"))
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[TIME]", realmsg, CLR_TOPIC);
					chatHandler.post(chatUpdater);
				}
				//etc
				else
				{
					realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received = getReceived("[" + realType + "]", realmsg, CLR_TOPIC);
					chatHandler.post(logUpdater);
				}
				break;
			default:
				break;
			
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
		 if(theMessage!=null)
			 msgColor+= specialColor;
		} catch(NullPointerException e) {
		}
		if(theMessage!=null && theMessage.contains(session.getNick()))
		{
			msgColor = 0xFF000000 + CLR_MENTION;
			System.out.println("MENTIONED" + isFocused);
			if(!isFocused)
			{
				if(Callisto.notification_chat==null)
					Callisto.notification_chat = new Notification(R.drawable.callisto, "Connecting to IRC", System.currentTimeMillis());
				Callisto.notification_chat.setLatestEventInfo(getApplicationContext(), "In the JB Chat",  ++mentionCount + " new mentions", contentIntent);
				mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
				if(mentionCount==1)//TODO: Fix the notification to be sent for the first mention
				{
					mNotificationManager.notify(Callisto.NOTIFICATION_ID-1, new Notification(R.drawable.callisto, "New mentions!", System.currentTimeMillis()));
					//mNotificationManager.cancel(Callisto.NOTIFICATION_ID-1);
				}
			}
		}
		else
			msgColor = 0xFF000000 + CLR_TEXT;
		
		SpannableString tit = new SpannableString(theTitle==null ? "" : theTitle);
		SpannableString mes = new SpannableString(theMessage==null ? "" : theMessage);
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
			}
		} catch(Exception ieieieie) {
		}
		
		return (Spanned) TextUtils.concat(Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : ""))
									 , tit, mes, "\n");
	}
	
	/** Gets the nick color from the list, adding it if necessary.
	 * @param nickInQ The nick in question
	 * @return The name of the color resource for that specific nick
	 */
	public Integer getNickColor(String nickInQ)
	{
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
			if(newMessage=="")
				return;
			SpannableString st = new SpannableString(session.getNick());
			int colorBro = 0xFF000000 + CLR_ME;
			int colorBro2 = 0xFF000000 + CLR_TEXT;
			
			
			SpannableString st2 = new SpannableString(newMessage);
			try {
			st.setSpan(new ForegroundColorSpan(colorBro), 0, session.getNick().length(), 0);
			st.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, session.getNick().length(), 0);
			st2.setSpan(new ForegroundColorSpan(colorBro2), 0, newMessage.length(), 0);
			}
			catch(Exception e) {}
			
			if(parseOutgoing(newMessage))
			{
				Spanned x = (Spanned) TextUtils.concat(
						Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "")),
						st,
						": ",
						st2,
						"\n"
						);
				Callisto.chatView.append(x);
			}
			input.requestFocus();
			input.setText("");
		}
	};
	
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
		if(msg.toUpperCase().startsWith("/NICK "))
		{
			msg =  msg.substring("/NICK ".length());
			session.changeNick(msg);
			return false;
		}
		if(msg.toUpperCase().startsWith("/QUIT") || msg.toUpperCase().startsWith("/PART"))
		{
			if(msg.equals("/QUIT") || msg.equals("/PART"))
				logout(null);
			else
				logout(msg.substring("/QUIT ".length()));
			return false;
		}
		if(msg.toUpperCase().startsWith("/WHO "))
		{
			session.who(msg.substring("/WHO ".length()));
			return false;
		}
		if(msg.toUpperCase().startsWith("/WHOIS "))
		{
			session.whois(msg.substring("/WHOIS ".length()));
			return false;
		}
		if(msg.toUpperCase().startsWith("/WHOWAS "))
		{
			session.whoWas(msg.substring("/WHOWAS ".length()));
			return false;
		}
		if(msg.toUpperCase().startsWith("/MSG "))
		{
			String targetNick = msg.substring("/MSG ".length(), msg.indexOf(" ", "/MSG ".length()+1));
			String targetMsg = msg.substring("/MSG ".length() + targetNick.length()); 
			session.sayPrivate(targetNick, targetMsg);
			received = getReceived("<-" + targetNick, targetMsg, CLR_PM);
			chatHandler.post(chatUpdater);
			return false;
		}
		if(msg.toUpperCase().startsWith("/ISON ")
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
		if(msg.toUpperCase().startsWith("/ME "))
		{
			session.action(CHANNEL_NAME, "ACTION" + msg.substring(3));
			received = getReceived("* " + session.getNick() + msg.substring(3), null, CLR_ME);
			chatHandler.post(chatUpdater);
			return false;
		}
		
		if(msg.toUpperCase().startsWith("/PING ")) //TODO: I have no clue if this works right.
		{
			session.action(CHANNEL_NAME, "PING" + msg.substring("/PING".length()));
			//session.ctcp(msg.substring("/PING ".length()), "ping");
			return true;
		}
		if(msg.toUpperCase().startsWith("/AWAY "))
		{
			session.setAway(msg.toUpperCase().substring("/AWAY ".length()));
			return false;	//TODO: CHECK
		}
		if(msg.toUpperCase().equals("/AWAY"))
		{
			if(session.isAway())
				session.unsetAway();
			else
				session.setAway("Gone away for now");
			return false;
		}
		
		if(msg.toUpperCase().startsWith("/JOIN ")
	    || msg.toUpperCase().startsWith("/CYCLE ")
	    || msg.toUpperCase().startsWith("/LIST ")
	    || msg.toUpperCase().startsWith("/KNOCK "))
		{
			Toast.makeText(IRCChat.this, "What, is the JB chat not enough for you?!", Toast.LENGTH_SHORT).show();
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
		received = getReceived("[CALLISTO]", "Command not recognized!", CLR_TOPIC);
		chatHandler.post(chatUpdater);
		return false;
	}
	
	/** A runnable to update the log */
	Runnable logUpdater = new Runnable()
	{
        @Override
        public void run()
        {
            Callisto.logView.append(received);
            Callisto.logView.invalidate();
            sv2.fullScroll(ScrollView.FOCUS_DOWN);
        };	

    };

}
