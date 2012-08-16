package com.qweex.callisto.irc;

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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.Toast;


public class IRCService extends PircBot
{
	private static String CLR_TEXT = "Black",
			   CLR_TOPIC = "DarkGoldenRod",
			   CLR_ME = "SeaGreen",
			   CLR_JOIN = "Blue",
			   CLR_NICK = "Blue",
			   CLR_PART = CLR_JOIN,
			   CLR_QUIT = CLR_JOIN,
			   CLR_KICK = CLR_JOIN,
			   CLR_ERROR = "Maroon",
			   CLR_MENTION = "LightCoral";
	private Context context;
	
	
	public static String SERVER_NAME = "irc.freenode.net"; //"irc.geekshed.net";
	public static String CHANNEL_NAME = "#jerklib"; //"#jupiterbroadcasting";
	
	static int mentionCount = 0;
	private static NotificationManager mNotificationManager;
	static PendingIntent contentIntent;
	private static CharSequence chatLog = "";
	private boolean SHOW_TIME = true;
	private SimpleDateFormat sdfTime = new SimpleDateFormat("'['HH:mm']'");
	public static Spanned received;
	
	public static HashMap<String, CharSequence> nickColors = new HashMap<String, CharSequence>();
	public static ArrayList<CharSequence> COLOR_LIST;
	List<String> nickList;
	
    public IRCService(String profileNick, Context c)
    {
        this.setName(profileNick);
        this.context = c;
		//Read colors
    	CLR_TEXT = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_text", "Black");
	    CLR_TOPIC = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_topic", "DarkGoldenRod");
	    CLR_ME = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_me", "SeaGreen");
		CLR_JOIN = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_join", "Blue");
	    CLR_NICK = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_text", "Blue");
	    CLR_PART = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_part", CLR_JOIN);
	    CLR_QUIT = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_quit", CLR_JOIN);
	    CLR_KICK = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_kick", CLR_JOIN);
		CLR_ERROR = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_error", "Maroon");
		CLR_MENTION = PreferenceManager.getDefaultSharedPreferences(c).getString("irc_color_mention", "LightCoral");
		SHOW_TIME = PreferenceManager.getDefaultSharedPreferences(c).getBoolean("irc_time", true);
		
		mNotificationManager =  (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
		
		COLOR_LIST = new ArrayList<CharSequence>(Arrays.asList(Callisto.RESOURCES.getTextArray(R.array.colors)));
		Collections.shuffle(COLOR_LIST);
    	nickColors.put(profileNick, CLR_ME);
		COLOR_LIST.remove(CLR_ME);
    }
    
	public static String getNickColor(String nickInQ)
	{
		if(!nickColors.containsKey(nickInQ))
		{
			nickColors.put(nickInQ, COLOR_LIST.get(0));
			COLOR_LIST.remove(0);
		}
		return (String) nickColors.get(nickInQ);
	}
    
	public Runnable sendMessage = new Runnable(){
		@Override
        public void run() {
			String newMessage = IRCChat.input.getText().toString();
			if(newMessage=="")
				return;
			SpannableString st = new SpannableString(IRCService.this.getNick());
			int colorBro = 0xFF000000 +  
					Callisto.RESOURCES.getColor(
					Callisto.RESOURCES.getIdentifier(CLR_ME, "color", "com.qweex.callisto"));
			int colorBro2 = 0xFF000000 +  
					Callisto.RESOURCES.getColor(
					Callisto.RESOURCES.getIdentifier(CLR_TEXT, "color", "com.qweex.callisto"));
			
			SpannableString st2 = new SpannableString(newMessage);
			try {
			st.setSpan(new ForegroundColorSpan(colorBro), 0, IRCService.this.getNick().length(), 0);
			st.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, IRCService.this.getNick().length(), 0);
			st2.setSpan(new ForegroundColorSpan(colorBro2), 0, newMessage.length(), 0);
			}
			catch(Exception e) {}
			
			received = (Spanned) TextUtils.concat(Html.fromHtml((IRCService.this.SHOW_TIME ? ("<small>" + IRCService.this.sdfTime.format(new Date()) + "</small> ") : ""))
					, st, ": ", st2, "\n");
			if(parseOutgoing(newMessage))
				IRCChat.chat.post(IRCChat.receiveMessage);
			IRCChat.chat.post(IRCChat.sendMessage);
			chatLog = TextUtils.concat(chatLog, received);
		}
	};
	
   public void logout()
    {
    	String q = PreferenceManager.getDefaultSharedPreferences(this.context).getString("irc_quit", null);
    	if(q==null)
    		this.quitServer();
    	else
    		this.quitServer(q);
		
		mNotificationManager.cancel(Callisto.NOTIFICATION_ID);
		//IRCChat.logout(); TODO
    }
	
	private boolean parseOutgoing(String msg)
	{
		if(!msg.trim().startsWith("/"))
		{
			this.sendMessage(CHANNEL_NAME, msg);
			return true;
		}
		if(msg.trim().toUpperCase().startsWith("/NICK "))
		{
			msg =  msg.trim().substring("/NICK ".length());
			this.changeNick(msg);
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/QUIT ") || msg.trim().toUpperCase().startsWith("/PART"))
		{
			logout();
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/WHO ")) //TEST
		{
			
			//session.who(msg.trim().substring("/WHO ".length()));
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/WHOIS ")) //TEST
		{
			//session.whois(msg.trim().substring("/WHOIS ".length()));
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/WHOWAS ")) //TEST
		{
			//session.whoWas(msg.trim().substring("/WHOWAS ".length()));
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/ISON ")
		|| msg.trim().toUpperCase().equals("/MOTD")
		|| msg.trim().toUpperCase().equals("/RULES")	//TEST
		|| msg.trim().toUpperCase().equals("/LUSERS")	//TEST
		|| msg.trim().toUpperCase().startsWith("/MAP")	//TEST
		|| msg.trim().toUpperCase().startsWith("/VERSION"))
		{
			this.sendRawLine(msg.trim().substring(1));
			return false;
		}
		
		
		if(msg.trim().toUpperCase().startsWith("/IDENTIFY "))	//TEST
		{
			this.identify((msg.trim().substring("/IDENTIFY ".length())));
			return false;
		}
		if(msg.trim().toUpperCase().startsWith("/PING ")) //TODO: /PING
		{
			//session.ctcp(msg.trim().substring("/PING ".length()), "ping");
			return false;
		}
		
		
		if(msg.trim().toUpperCase().startsWith("/JOIN ")
	    || msg.trim().toUpperCase().startsWith("/CYCLE ")
	    || msg.trim().toUpperCase().startsWith("/LIST ")
	    || msg.trim().toUpperCase().startsWith("/KNOCK "))
		{
			Toast.makeText(this.context, "What, is the JB chat not enough for you?!", Toast.LENGTH_SHORT).show();
			return false;    	
		}
		
		if(msg.trim().toUpperCase().startsWith("/MOTD ")
		|| msg.trim().toUpperCase().startsWith("/LUSERS "))
		{
			Toast.makeText(this.context, "Stop that, you're trying to break stuff", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		this.sendMessage(CHANNEL_NAME, msg);
		return true;
		
	}
	
	

	
	
	
	//Chat events
    @Override
    public void onConnect()
    {
    	Intent notificationIntent = new Intent(this.context, IRCChat.class);
		contentIntent = PendingIntent.getActivity(this.context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    	Callisto.notification_chat = new Notification(R.drawable.callisto, "Connecting to IRC", System.currentTimeMillis());
		Callisto.notification_chat.flags = Notification.FLAG_ONGOING_EVENT;
       	Callisto.notification_chat.setLatestEventInfo(this.context,  "In the JB Chat",  "No new mentions", contentIntent);
       	mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
       	
		//received =  Html.fromHtml(c.getRawEventData()); //TODO
		//syslog.post(receiveLog);
    }
    @Override
    public void onTopic(String channel, String topic, String setBy, long date, boolean changed)
    {
		received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") + 
				"<font color='" + CLR_TOPIC + "'>" + topic + 
				"<br/>(set by " + setBy + " on " + date + " )</font><br/>"); //TODO: FormatDate
		IRCChat.chat.post(IRCChat.receiveMessage);
		chatLog = TextUtils.concat(chatLog, received);
    }
    
    @Override
    public void onJoin(String channel, String sender, String login, String hostname) 
    {
		nickList.add(sender);
		received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
				"<font color='" + CLR_JOIN + "'>" + sender + " entered the room.</font><br/>");
		IRCChat.chat.post(IRCChat.receiveMessage);
		chatLog = TextUtils.concat(chatLog, received);
    }
    //TODO: MOTD
    
    @Override
    public void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) 
    {
    	System.out.println("|" + sourceNick + "|");
		if(sourceNick.equals("NickServ"))
		{
			received =  Html.fromHtml(notice + "<br/>"); //TODO: fix
			received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
					"<font color='" + CLR_TOPIC + "'>" + notice + "</font><br/>");
			IRCChat.chat.post(IRCChat.receiveMessage);
			chatLog = TextUtils.concat(chatLog, received);
		}
    }
    
    @Override
    public void onUserList(String channel, User[] users) 
    {
    	nickList = new ArrayList<String>();
		for(int i=0; i<users.length; i++)
			nickList.add(users[i].getNick());
		Collections.sort(nickList, String.CASE_INSENSITIVE_ORDER);
    }
    
    @Override
    public void onNickChange(String oldNick, String login, String hostname, String newNick) 
    {
		received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
				"<font color='" + CLR_NICK + "'>" + oldNick + " changed their nick to " + newNick + ".</font><br/>");
		IRCChat.chat.post(IRCChat.receiveMessage);
		chatLog = TextUtils.concat(chatLog, received);
    }
    
    @Override
    public void onPart(String channel, String sender, String login, String hostname) 
    {
		COLOR_LIST.add(nickColors.get(sender));
		nickColors.remove(sender);
		nickList.remove(sender);
		received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
				"<font color='" + CLR_PART + "'>PART: " + sender + " (" + "" + ")</font><br/>"); //TODO: partmessage
		IRCChat.chat.post(IRCChat.receiveMessage);
		chatLog = TextUtils.concat(chatLog, received);
    }
    
    @Override
    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) 
    {
		COLOR_LIST.add(nickColors.get(sourceNick));
		nickColors.remove(sourceNick);
		nickList.remove(sourceNick);
		received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
				"<font color='" + CLR_QUIT + "'>QUIT: " + sourceNick + " (" + reason + ")</font><br/>");
		IRCChat.chat.post(IRCChat.receiveMessage);
		chatLog = TextUtils.concat(chatLog, received);
    }
    
    @Override
    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) 
    {
		received =  Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
				"<font color='" + CLR_KICK + "'>" + recipientNick + " was kicked by " + kickerNick + ". (" + reason + ")</font><br/>");
		IRCChat.chat.post(IRCChat.receiveMessage);
		chatLog = TextUtils.concat(chatLog, received);
    }
    
    //ISON
    //MAP
    //LUSERS
    
    @Override
    public void onAction(String sender, String login, String hostname, String target, String action) 
    {
    	System.out.println("BUTTS |" + target + "\\/" + action + "|");
    }
    
    @Override
    public void onUnknown(String line)
    {
    	System.out.println("BUTTS |" + line + "|");
    }
    
    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) 
    {
    	System.out.println("BUTTTTTSSSS");
    	int colorBro = 0xFF000000 +  
				Callisto.RESOURCES.getColor(
				Callisto.RESOURCES.getIdentifier(getNickColor(sender), "color", "com.qweex.callisto"));
		int colorBro2 = 0xFF000000 +  
				Callisto.RESOURCES.getColor(
				Callisto.RESOURCES.getIdentifier(message.contains(getNick()) ? CLR_MENTION : CLR_TEXT, "color", "com.qweex.callisto"));
    	
		SpannableString st = new SpannableString(sender);
		SpannableString st2 = new SpannableString(message);
		try {
		st.setSpan(new ForegroundColorSpan(colorBro), 0, sender.length(), 0);
		st.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, sender.length(), 0);
		st2.setSpan(new ForegroundColorSpan(colorBro2), 0, message.length(), 0);
		} catch(Exception ieieieie) {}
		
		if(message.contains(getNick()) && !IRCChat.isFocused)
		{
			Callisto.notification_chat.setLatestEventInfo(context, "In the JB Chat",  ++mentionCount + " new mentions", contentIntent);
			mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_chat);
			if(mentionCount==1)//TODO: Fix the notification to be sent for the first mentionE
			{
				mNotificationManager.notify(Callisto.NOTIFICATION_ID-1, new Notification(R.drawable.callisto, "New mentions!", System.currentTimeMillis()));
				mNotificationManager.cancel(Callisto.NOTIFICATION_ID-1);
			}
		}
		
		received = (Spanned) TextUtils.concat(Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : ""))
				 , st, ": ", st2, "\n");
    	//IRCChat.syslog.post(IRCChat.receiveLog);
		IRCChat.chat.post(IRCChat.receiveMessage);
		chatLog = TextUtils.concat(chatLog, received);
    }
}
