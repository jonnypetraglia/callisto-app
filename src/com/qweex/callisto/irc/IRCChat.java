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



import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jibble.pircbot.*;

import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

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
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewAnimator;




//TODO: URL detection
//FIXME: /me support (does not support sending, does not even get event for receiving)
//FIXME: TextView is not exactly lined up right;
//IDEA: Nicklist



public class IRCChat extends Activity
{
	
	private IRCService theIRC;
	
	private final int MAX_SCROLLBACK = 100;
	
	private static final int LOG_ID=Menu.FIRST+1;
	private static final int LOGOUT_ID=LOG_ID+1;
	static boolean isFocused = false;
	boolean isLandscape;
	
	static ScrollView sv;
	static ScrollView sv2;
	static TextView chat;
	static TextView syslog;
	static EditText input;
	
	
	

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		
		super.onCreate(savedInstanceState);
		isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
		
	/*
		if(session!=null)
		{
			resume();
			chat.setText(chatLog);
			return;
		}
		*/
		
    	String profileNick = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_nick", null);
		String profilePass = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_pass", null);
		
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
		pass.setRawInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD); //TODO: Change this
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_HOME)
		{
			isFocused = false;
		}
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{	
			/*
			if(session==null)
			{
				finish();
				return true;
			}
			*/
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
	    	
	    	
	    	/*
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
	    		input.setBackgroundColor(Callisto.RESOURCES.getColor(R.color.Salmon));
	    		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("BUTTS");
				}
	    		input.setBackgroundDrawable(Callisto.RESOURCES.getDrawable(android.R.drawable.editbox_background));
	    	}
	    	*/
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
    	IRCService.mentionCount = 0;
    	if(Callisto.notification_chat!=null)
			Callisto.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", IRCService.contentIntent);
    	

		setContentView(R.layout.irc);
		sv = (ScrollView) findViewById(R.id.scrollView);
		sv2 = (ScrollView) findViewById(R.id.scrollView2);
		chat = (TextView) findViewById(R.id.chat);
		syslog = (TextView) findViewById(R.id.syslog);
		input = (EditText) findViewById(R.id.inputField);
		input.setOnEditorActionListener(new OnEditorActionListener(){
			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				input.post(theIRC.sendMessage);
				return false;
			}
		});
    }
    
	
    public void initiate()
    {
    	findViewById(1337).startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
    	resume();
		
    	String profileNick = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_nick", null);
		String profilePass = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_pass", null);
		
        theIRC = new IRCService(profileNick, this);
        theIRC.setVerbose(true);
        theIRC.setAutoNickChange(true);
        
    	try {
			theIRC.connect(IRCService.SERVER_NAME);
		} catch (NickAlreadyInUseException e) {
			boolean SHOW_TIME = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_time", true);
			SimpleDateFormat sdfTime = new SimpleDateFormat("'['HH:mm']'");
			String CLR_ERROR = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_color_error", "Maroon");
			IRCService.received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
			"<font color='" + CLR_ERROR + "'>" + e.getMessage() + " is in use.</font><br/>");//TODO: check
			chat.post(receiveMessage);
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IrcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        theIRC.joinChannel(IRCService.CHANNEL_NAME);
        if(profilePass!=null)
        	theIRC.identify(profilePass);
		
		
    }
    
    public void logout()
    {
		isFocused = false;
		finish();
    }
    
    
    /*
	public void receiveEvent(IRCEvent e)
	{
		System.out.println("|" + e.getRawEventData() + "|");
		System.out.println("---" + e.getType());
		switch(e.getType())
		{
		//Syslog events
			case MOTD:
				MotdEvent mo = (MotdEvent) e;
				received =  Html.fromHtml(mo.getMotdLine() + "<br/>");
				syslog.post(receiveLog);
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
	*/
	

	
	static public Runnable sendMessage = new Runnable(){
		@Override
        public void run()
		{
			input.setText("");
		}
	};
	

	
	
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
	
    static Runnable receiveMessage = new Runnable(){
        @Override
        public void run() {
            chat.append(IRCService.received);
            chat.invalidate();
            sv.fullScroll(ScrollView.FOCUS_DOWN);
            input.requestFocus();
        };	

    };

   
    static Runnable receiveLog = new Runnable(){
        @Override
        public void run() {
            syslog.append(IRCService.received);
            syslog.invalidate();
            sv2.smoothScrollTo(0, syslog.getBottom());
        };	

    };
}
