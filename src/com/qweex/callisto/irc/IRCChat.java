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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;


import jerklib.ConnectionManager;
import jerklib.Profile;
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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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


//TODO: URL detection
//FIXME: /me support (does not support sending, does not even get event for receiving)
//FIXME: TextView is not exactly lined up right;
//IDEA: Nicklist

public class IRCChat extends Activity implements IRCEventListener
{
	
	private String SERVER_NAME = "irc.freenode.net"; //"irc.geekshed.net";
	private String CHANNEL_NAME = "#jerklib"; //"#jupiterbroadcasting";
	private String profileNick;
	private String profilePass;
	private boolean SHOW_TIME = true;
	private final int MAX_SCROLLBACK = 100;
	
	private String CLR_TEXT = "Black",
				   CLR_TOPIC = "DarkGoldenRod",
				   CLR_ME = "SeaGreen",
				   CLR_JOIN = "Blue",
				   CLR_NICK = "Blue",
				   CLR_PART = CLR_JOIN,
				   CLR_QUIT = CLR_JOIN,
				   CLR_KICK = CLR_JOIN,
				   CLR_ERROR = "Maroon",
				   CLR_MENTION = "LightCoral";
	
	
	private static final int LOG_ID=Menu.FIRST+1;
	private static final int LOGOUT_ID=LOG_ID+1;
	
	private static ConnectionManager manager;
	private static Session session;
	private static CharSequence chatLog = "";
	private static NotificationManager mNotificationManager;
	private static int mentionCount = 0;
	private static PendingIntent contentIntent;
	private static boolean isFocused = false;
	ScrollView sv, sv2;
	TextView chat, syslog;
	EditText input;
	Spanned received;
	SimpleDateFormat sdfTime = new SimpleDateFormat("'['HH:mm']'");
	boolean isLandscape;
	
	public static HashMap<String, CharSequence> nickColors = new HashMap<String, CharSequence>();
	public static ArrayList<CharSequence> COLOR_LIST;
	List<String> nickList;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
		mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		COLOR_LIST = new ArrayList<CharSequence>(Arrays.asList(Callisto.RESOURCES.getTextArray(R.array.colors)));
		Collections.shuffle(COLOR_LIST);
		
		profileNick = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_nick", null);
		profilePass = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_pass", null);
		if(session!=null)
		{
			resume();
			chat.setText(chatLog);
			return;
		}
		
		LinearLayout ll = new LinearLayout(this);
		ll.setBackgroundColor(Callisto.RESOURCES.getColor(R.color.backClr));
		ll.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params 
			= new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		ll.setLayoutParams(params);
		ll.setId(1337);
		ll.setPadding(getWindowManager().getDefaultDisplay().getHeight()/10,
				  getWindowManager().getDefaultDisplay().getHeight()/(isLandscape?1000:4),
				  getWindowManager().getDefaultDisplay().getHeight()/10,
				  0);
		final EditText user = new EditText(this);
		final EditText pass = new EditText(this);
		Button login = new Button(this);
		params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		user.setText(profileNick);
		pass.setText(profilePass);
		user.setLayoutParams(params);
		pass.setLayoutParams(params);
		login.setLayoutParams(params);
		login.setCompoundDrawables(Callisto.RESOURCES.getDrawable(R.drawable.ic_menu_login), null, null, null);
		
		user.setHint("Nick");
		pass.setHint("Password (Optional)");
		pass.setRawInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
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
		
		ll.addView(user);
		ll.addView(pass);
		ll.addView(login);
		setContentView(ll);
	}
	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_HOME)
		{
			isFocused = false;
		}
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{	
			if(session==null)
			{
				finish();
				return true;
			}
			isFocused = false;
			Intent i = new Intent(IRCChat.this, Callisto.class);
            startActivity(i);
	        return true;
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
					System.out.println("BUTTS");
				}
	    		input.setBackgroundDrawable(Callisto.RESOURCES.getDrawable(android.R.drawable.editbox_background));
	    		//*/
	    	}
	    }
	    return super.onKeyDown(keyCode, event);
	} 
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	menu.add(0, LOG_ID, 0, "Log").setIcon(R.drawable.ic_menu_chat_dashboard);
    	menu.add(0, LOGOUT_ID, 0, "Logout").setIcon(R.drawable.ic_menu_close_clear_cancel);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
 
        switch (item.getItemId())
        {
        case LOG_ID:
        	((ViewAnimator) findViewById(R.id.viewanimator)).showNext();
            return true;
        case LOGOUT_ID:
        	logout();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      LinearLayout ll = (LinearLayout) findViewById(1337);
      if(ll!=null)
		ll.setPadding(getWindowManager().getDefaultDisplay().getHeight()/10,
				  getWindowManager().getDefaultDisplay().getHeight()/(isLandscape?1000:4),
				  getWindowManager().getDefaultDisplay().getHeight()/10,
				  0);
    }

    public void resume()
    {
    	isFocused = true;
    	mentionCount = 0;
    	nickColors.put(profileNick, CLR_ME);
		COLOR_LIST.remove(CLR_ME);
		setContentView(R.layout.irc);
		sv = (ScrollView) findViewById(R.id.scrollView);
		sv2 = (ScrollView) findViewById(R.id.scrollView2);
		chat = (TextView) findViewById(R.id.chat);
		syslog = (TextView) findViewById(R.id.syslog);
		input = (EditText) findViewById(R.id.inputField);
		input.setOnEditorActionListener(new OnEditorActionListener(){
			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				input.post(sendMessage);
				return false;
			}
		});
		if(session!=null)
			session.addIRCEventListener(this);
		if(Callisto.notification_chat!=null)
			Callisto.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
    }
    
	
    public void initiate()
    {
    	findViewById(1337).startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
    	resume();
    	SHOW_TIME = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_time", true);
    	
    	//Read colors
    	CLR_TEXT = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_text", "Black");
	    CLR_TOPIC = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_topic", "DarkGoldenRod");
	    CLR_ME = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_me", "SeaGreen");
		CLR_JOIN = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_join", "Blue");
	    CLR_NICK = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_text", "Blue");
	    CLR_PART = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_part", CLR_JOIN);
	    CLR_QUIT = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_quit", CLR_JOIN);
	    CLR_KICK = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_kick", CLR_JOIN);
		CLR_ERROR = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_error", "Maroon");
		CLR_MENTION = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_mention", "LightCoral");
    	
        final NickServAuthPlugin auth = new NickServAuthPlugin(profilePass, 'e', session, Arrays.asList(CHANNEL_NAME));
		
		manager = new ConnectionManager(new Profile(profileNick));
		session = manager.requestConnection(SERVER_NAME);
		session.onEvent(auth, Type.CONNECT_COMPLETE , Type.MODE_EVENT);
		session.addIRCEventListener(this);
		Intent notificationIntent = new Intent(this, IRCChat.class);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    	Callisto.notification_chat = new Notification(R.drawable.callisto, "Connecting to IRC", System.currentTimeMillis());
		Callisto.notification_chat.flags = Notification.FLAG_ONGOING_EVENT;
       	Callisto.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
       	
       	mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
    }
    
    public void logout()
    {
    	String q = profilePass = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_quit", null);
    	if(q==null)
    		manager.quit();
    	else
    		manager.quit(q);
		manager = null;
		session = null;
		mNotificationManager.cancel(Callisto.NOTIFICATION_ID);
		isFocused = false;
		finish();
    }
    
    
    
	public void receiveEvent(IRCEvent e)
	{
		System.out.println("|" + e.getRawEventData() + "|");
		System.out.println("---" + e.getType());
		switch(e.getType())
		{
		//Syslog events
			case CONNECT_COMPLETE:
				ConnectionCompleteEvent c = (ConnectionCompleteEvent) e;
				received =  Html.fromHtml(c.getRawEventData());
				e.getSession().join(CHANNEL_NAME);
				syslog.post(receiveLog);
				break;
			case JOIN_COMPLETE:
				JoinCompleteEvent jce = (JoinCompleteEvent) e;
				received =  Html.fromHtml(jce.getRawEventData());
				syslog.post(receiveLog);
				break;
			case MOTD:
				MotdEvent mo = (MotdEvent) e;
				received =  Html.fromHtml(mo.getMotdLine() + "<br/>");
				syslog.post(receiveLog);
				break;
			case NOTICE:
				NoticeEvent ne = (NoticeEvent) e;
				System.out.println("|" + ne.byWho() + "|");
				if(ne.byWho().equals("NickServ"))
				{
					received =  Html.fromHtml(ne.getNoticeMessage() + "<br/>");
					received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
							"<font color='" + CLR_TOPIC + "'>" + ne.getNoticeMessage() + "</font><br/>");
					chat.post(receiveMessage);
				}
				break;
				
			case NICK_LIST_EVENT:
				nickList = ((NickListEvent) e).getNicks();
				Collections.sort(nickList, String.CASE_INSENSITIVE_ORDER);
				break;
				
//AWAY_EVENT = someone aways
//CHANNEL_LIST_EVENT
//CTCP_EVENT
//INVITE_EVENT
//NICK_LIST_EVENT
//NOTICE_EVENT
//NUMERIC_ERROR_EVENT
//SERVER_INFORMATION_EVENT
//SERVER_VERSION_EVENT
//UnresolvedHostnameErrorEvent
//WhoEvent, WhoisEvent, WhowasEvent
			
		//Chat events
			case TOPIC:
				TopicEvent t = (TopicEvent) e;
				received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") + 
						"<font color='" + CLR_TOPIC + "'>" + t.getTopic() + 
						"<br/>(set by " + t.getSetBy() + " on " + t.getSetWhen() + " )</font><br/>");
				chat.post(receiveMessage);
				break;
			case CHANNEL_MESSAGE:
				MessageEvent m = (MessageEvent) e;
				int colorBro = 0xFF000000 +  
						Callisto.RESOURCES.getColor(
						Callisto.RESOURCES.getIdentifier(getNickColor(m.getNick()), "color", "com.qweex.callisto"));
				int colorBro2 = 0xFF000000 +  
						Callisto.RESOURCES.getColor(
						Callisto.RESOURCES.getIdentifier(m.getMessage().contains(session.getNick()) ? CLR_MENTION : CLR_TEXT, "color", "com.qweex.callisto"));
				System.out.println("BUTTS1: " + m.getMessage().contains(session.getNick()));
				System.out.println("BUTTS2: " + !isFocused);
				if(m.getMessage().contains(session.getNick()) && !isFocused)
				{
					Callisto.notification_chat.setLatestEventInfo(getApplicationContext(), "In the JB Chat",  ++mentionCount + " new mentions", contentIntent);
					mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
					System.out.println("BUTTS3: " + mentionCount);
					if(mentionCount==1)//TODO: Fix the notification to be sent for the first mentionE
					{
						mNotificationManager.notify(Callisto.NOTIFICATION_ID-1, new Notification(R.drawable.callisto, "New mentions!", System.currentTimeMillis()));
						mNotificationManager.cancel(Callisto.NOTIFICATION_ID-1);
					}
				}
				
				
				SpannableString st = new SpannableString(m.getNick());
				SpannableString st2 = new SpannableString(m.getMessage());
				try {
				st.setSpan(new ForegroundColorSpan(colorBro), 0, m.getNick().length(), 0);
				st.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, m.getNick().length(), 0);
				st2.setSpan(new ForegroundColorSpan(colorBro2), 0, m.getMessage().length(), 0);
				} catch(Exception ieieieie) {}
				
				
				received = (Spanned) TextUtils.concat(Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : ""))
											 , st, ": ", st2, "\n");
				chat.post(receiveMessage);
				break;
			case JOIN:
				JoinEvent j = (JoinEvent) e;
				nickList.add(j.getNick());
				received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_JOIN + "'>" + j.getNick() + " entered the room.</font><br/>");
				chat.post(receiveMessage);
				break;
			case NICK_CHANGE:
				NickChangeEvent ni = (NickChangeEvent) e;
				received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_NICK + "'>" + ni.getOldNick() + " changed their nick to " + ni.getNewNick() + ".</font><br/>");
				chat.post(receiveMessage);
				break;
			case PART:
				PartEvent p = (PartEvent) e;
				COLOR_LIST.add(nickColors.get(p.getWho()));
				nickColors.remove(p.getWho());
				nickList.remove(p.getWho());
				received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_PART + "'>PART: " + p.getWho() + " (" + p.getPartMessage() + ")</font><br/>");
				chat.post(receiveMessage);
				break;
			case QUIT:
				QuitEvent q = (QuitEvent) e;
				COLOR_LIST.add(nickColors.get(q.getNick()));
				nickColors.remove(q.getNick());
				nickList.remove(q.getNick());
				received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_QUIT + "'>QUIT: " + q.getNick() + " (" + q.getQuitMessage() + ")</font><br/>");
				chat.post(receiveMessage);
				break;
			case KICK_EVENT:
				KickEvent k = (KickEvent) e;
				received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_KICK + "'>" + k.getWho() + " was kicked. (" + k.getMessage() + ")</font><br/>");
				chat.post(receiveMessage);
				break;
			case NICK_IN_USE:
				NickInUseEvent n = (NickInUseEvent) e;
				received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_ERROR + "'>" + n.getInUseNick() + " is in use.</font><br/>");
				chat.post(receiveMessage);
				break;
			
			//Errors that display in both
			case CONNECTION_LOST:
				ConnectionLostEvent co = (ConnectionLostEvent) e;
				received =  Html.fromHtml(SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "" +
						"<font color='" + CLR_ERROR + "'>CONNECTION WAS LOST</font><br/>");
				chat.post(receiveMessage);
				syslog.post(receiveMessage);
				break;
			case ERROR:
				ErrorEvent ev = (ErrorEvent) e;
				received =  Html.fromHtml(SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "" +
						"<font color='" + CLR_ERROR + "'>AN ERROR OCCURRED</font><br/>");
				chat.post(receiveMessage);
				syslog.post(receiveMessage);
				break;
				
			//Events not handled by jerklib
			case DEFAULT:		//ping
				String realType = e.getRawEventData();
				realType = realType.substring(realType.indexOf(" ")+1, realType.indexOf(" ", realType.indexOf(" ")+1));
				int i=0;
				try {
					i = Integer.parseInt(realType);
				} catch(Exception asdf) {}
				
				System.out.println("!!!" + realType + "!!!");
				//ISON
				if(realType.equals("303"))
				{
					String name = e.getRawEventData().substring(e.getRawEventData().lastIndexOf(":")+1);
					System.out.println(name);
					if(name.trim().equals(""))
						return;
					received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") + 
							"<font color='" + CLR_TOPIC + "'>[ISON] " + name + " is online</font><br/>");
					chat.post(receiveMessage);
					return;
				}
				//MAP
				if((i>=375 && i<=379))
				{
					String realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") + 
							"<font color='" + CLR_TOPIC + "'>[MAP] " + realmsg + "</font><br/>");
					chat.post(receiveMessage);
					return;
				}
				//LUSERS
				if((i>=250 && i<=255) || i==265 || i==266)
				{
					String realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") + 
							"<font color='" + CLR_TOPIC + "'>[LUSERS] " + realmsg + "</font><br/>");
					chat.post(receiveMessage);
					return;
				}
				//VERSION
				if(realType.equals("351"))
				{
					String realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
					received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") + 
							"<font color='" + CLR_TOPIC + "'>[VERSION] " + realmsg + "</font><br/>");
					chat.post(receiveMessage);
					return;
				}
				break;
				
				
			default:
				break;
			
		}
	}
	
	public String getNickColor(String nickInQ)
	{
		if(!nickColors.containsKey(nickInQ))
		{
			nickColors.put(nickInQ, COLOR_LIST.get(0));
			COLOR_LIST.remove(0);
		}
		return (String) nickColors.get(nickInQ);
	}

	
	Runnable sendMessage = new Runnable(){
		@Override
        public void run() {
			String newMessage = input.getText().toString();
			if(newMessage=="")
				return;
			SpannableString st = new SpannableString(session.getNick());
			int colorBro = 0xFF000000 +  
					Callisto.RESOURCES.getColor(
					Callisto.RESOURCES.getIdentifier(CLR_ME, "color", "com.qweex.callisto"));
			int colorBro2 = 0xFF000000 +  
					Callisto.RESOURCES.getColor(
					Callisto.RESOURCES.getIdentifier(CLR_TEXT, "color", "com.qweex.callisto"));
			
			SpannableString st2 = new SpannableString(newMessage);
			try {
			st.setSpan(new ForegroundColorSpan(colorBro), 0, session.getNick().length(), 0);
			st.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, session.getNick().length(), 0);
			st2.setSpan(new ForegroundColorSpan(colorBro2), 0, newMessage.length(), 0);
			}
			catch(Exception e) {}
			
			
			
			if(parseOutgoing(newMessage))
			{
				chat.append(Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "")));
				chat.append(st);
				chat.append(": ");
				chat.append(st2);
				chat.append("\n");
				chatLog = TextUtils.concat(chatLog, 
						Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "")),
						st,
						": ",
						st2,
						"\n"
						);
			}
			input.requestFocus();
			input.setText("");
		}
	};
	
	
	//Boolean return value tells if the output should be appended to the chat log
	private boolean parseOutgoing(String msg)
	{
		if(!msg.trim().startsWith("/"))
		{
			session.getChannel(CHANNEL_NAME).say(msg);
			return true;
		}
		if(msg.trim().toUpperCase().startsWith("/NICK "))
		{
			msg =  msg.trim().substring("/NICK ".length());
			session.changeNick(msg);
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/QUIT ") || msg.trim().toUpperCase().startsWith("/PART"))
		{
			logout();
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/WHO ")) //TEST
		{
			session.who(msg.trim().substring("/WHO ".length()));
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/WHOIS ")) //TEST
		{
			session.whois(msg.trim().substring("/WHOIS ".length()));
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/WHOWAS ")) //TEST
		{
			session.whoWas(msg.trim().substring("/WHOWAS ".length()));
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/ISON ")
		|| msg.trim().toUpperCase().equals("/MOTD")
		|| msg.trim().toUpperCase().equals("/RULES")	//TEST
		|| msg.trim().toUpperCase().equals("/LUSERS")	//TEST
		|| msg.trim().toUpperCase().startsWith("/MAP")	//TEST
		|| msg.trim().toUpperCase().startsWith("/VERSION"))
		{
			session.sayRaw(msg.trim().substring(1));
			return false;
		}
		
		
		if(msg.trim().toUpperCase().startsWith("/IDENTIFY "))	//TEST
		{
			new NickServAuthPlugin(msg.trim().substring("/IDENTIFY ".length()), 'e', session, Arrays.asList(CHANNEL_NAME));
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/PING ")) //TODO: /PING
		{
			System.out.println("BUTTS |" + msg.trim().substring(6) + "|");
			session.ctcp(msg.trim().substring("/PING ".length()), "ping");
			return false;
		}
		
		
		if(msg.trim().toUpperCase().startsWith("/JOIN ")
	    || msg.trim().toUpperCase().startsWith("/CYCLE ")
	    || msg.trim().toUpperCase().startsWith("/LIST ")
	    || msg.trim().toUpperCase().startsWith("/KNOCK "))
		{
			Toast.makeText(IRCChat.this, "What, is the JB chat not enough for you?!", Toast.LENGTH_SHORT).show();
			return false;
		}
		/*
		if(msg.trim().toUpperCase().startsWith("/MOTD ")
		|| msg.trim().toUpperCase().startsWith("/LUSERS "))
		{
			Toast.makeText(IRCChat.this, "Stop that, you're trying to break stuff", Toast.LENGTH_SHORT).show();
			return false;
		}
		*/
		session.getChannel(CHANNEL_NAME).say(msg);
		return true;
	}
	
	
	/*
	 *local
	 * PING
	 * AWAY
	 * MSG
	 * 
	 * 
	 *what
	 * WATCH???
	 * HELPOP???
	 * SETNAME???
	 * VHOST???
	 * MODE???
	 *
	 * 
	 *mod
	 * INVITE
	 * KICK
	 * http://www.geekshed.net/commands/ircop/
	 * 
	 * 
	 *naw
	 * VERSION
	 * LINK
	 * ADMIN
	 * USERHOST
	 * TOPIC
	 * CREDITS
	 * TIME
	 * DNS
	 * USERIP
	 * STATS
	 * MODULE
	 */
	
    Runnable receiveMessage = new Runnable(){
        @Override
        public void run() {
            chat.append(received);
            chatLog = TextUtils.concat(chatLog, received);
            chat.invalidate();
            sv.fullScroll(ScrollView.FOCUS_DOWN);
            input.requestFocus();
        };	

    };

   
    Runnable receiveLog = new Runnable(){
        @Override
        public void run() {
            syslog.append(received);
            syslog.invalidate();
            sv2.smoothScrollTo(0, syslog.getBottom());
        };	

    };

	
}
