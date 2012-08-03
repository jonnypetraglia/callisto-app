package com.qweex.callisto;

import java.text.SimpleDateFormat;
import java.util.Date;

import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.Session;
import jerklib.events.*;
import jerklib.listeners.IRCEventListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewAnimator;


public class IRCChat extends Activity implements IRCEventListener
{
	
	private String SERVER_NAME = "irc.geekshed.net";
	private String CHANNEL_NAME = "#jupiterbroadcasting";
	private String PROFILE;
	private final boolean SHOW_TIME = true;
	
	private String CLR_TOPIC = "Yellow",
				   CLR_ME = "Red",
				   CLR_JOIN = "Blue",
				   CLR_ERROR = "Red",
				   CLR_NICK = "Blue",
				   CLR_PART = CLR_JOIN,
				   CLR_QUIT = CLR_JOIN,
				   CLR_KICK = CLR_JOIN,
				   CLR_MENTION = "LightCoral";
	
	
	private static final int LOG_ID=Menu.FIRST+1;
	
	private ConnectionManager manager;
	private Session session;
	ScrollView sv, sv2;
	TextView chat, syslog;
	EditText input;
	Spanned received;
	SimpleDateFormat sdfTime = new SimpleDateFormat("'('hh:mm:ss aa')'");;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		PROFILE = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_nick", null);
		
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
		
		if(PROFILE==null)
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Login");
			alert.setMessage("Insert your IRC nick");

			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			  PROFILE = input.getText().toString();
			  PreferenceManager.getDefaultSharedPreferences(IRCChat.this).edit().putString("irc_nick", PROFILE);
			  initiate();
			  }
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
				  finish();
			  }
			});

			alert.show();
		}
		initiate();
			
	}
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		manager.quit();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	menu.add(0, LOG_ID, 0, "Log");
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
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      /*
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
		//*/
    }

	
    public void initiate()
    {
		manager = new ConnectionManager(new Profile(PROFILE));
		session = manager.requestConnection(SERVER_NAME);
		session.addIRCEventListener(this);
    }
    
    
    
	public void receiveEvent(IRCEvent e)
	{
		System.out.println(e.getType().toString());
		switch(e.getType())
		{
		//Syslog events
			case CONNECT_COMPLETE:
				ConnectionCompleteEvent c = (ConnectionCompleteEvent) e;
				received = Html.fromHtml(c.getRawEventData());
				//received = Html.fromHtml(c.getActualHostName());
				e.getSession().join(CHANNEL_NAME);
				syslog.post(receiveLog);
				break;
			case JOIN_COMPLETE:
				JoinCompleteEvent jce = (JoinCompleteEvent) e;
				received = Html.fromHtml(jce.getRawEventData());
				syslog.post(receiveLog);
				//((ViewAnimator) findViewById(R.id.viewanimator)).showNext();
				break;
			case MOTD:
				MotdEvent mo = (MotdEvent) e;
				received = Html.fromHtml(mo.getMotdLine() + "<br/>");
				syslog.post(receiveLog);
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
				received = Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") + 
						"<font color='" + CLR_TOPIC + "'>" + t.getTopic() + 
						"<br/>(set by " + t.getSetBy() + " on " + t.getSetWhen() + " )</font><br/>");
				chat.post(receiveMessage);
				break;
			case CHANNEL_MESSAGE:
				MessageEvent m = (MessageEvent) e;
				received = Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<b>" + m.getNick() + "</b>: "
						+ (m.getMessage().indexOf(session.getNick())>0 ? ("<font color='" + CLR_MENTION + "'>" + m.getMessage() + "</font>")
						: m.getMessage()) + "<br/>");
				chat.post(receiveMessage);
				break;
			case JOIN:
				JoinEvent j = (JoinEvent) e;
				received = Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_JOIN + "'>" + j.getNick() + " entered the room.</font><br/>");
				chat.post(receiveMessage);
				break;
			case NICK_CHANGE:
				NickChangeEvent ni = (NickChangeEvent) e;
				received = Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_NICK + "'>" + ni.getOldNick() + " changed their nick to " + ni.getNewNick() + ".</font><br/>");
				chat.post(receiveMessage);
				break;
			case PART:
				PartEvent p = (PartEvent) e;
				received = Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_PART + "'>PART: " + p.getWho() + " (" + p.getPartMessage() + ")</font><br/>");
				chat.post(receiveMessage);
				break;
			case QUIT:
				QuitEvent q = (QuitEvent) e;
				received = Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_QUIT + "'>QUIT: " + q.getNick() + " (" + q.getQuitMessage() + ")</font><br/>");
				chat.post(receiveMessage);
				break;
			case KICK_EVENT:
				KickEvent k = (KickEvent) e;
				received = Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_KICK + "'>" + k.getWho() + " was kicked. (" + k.getMessage() + ")</font><br/>");
				chat.post(receiveMessage);
				break;
			case NICK_IN_USE:
				NickInUseEvent n = (NickInUseEvent) e;
				received = Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "") +
						"<font color='" + CLR_ERROR + "'>" + n.getInUseNick() + " is in use.</font><br/>");
				chat.post(receiveMessage);
				break;
			
			//Errors that display in both
			case CONNECTION_LOST:
				ConnectionLostEvent co = (ConnectionLostEvent) e;
				received = Html.fromHtml(SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "" +
						"<font color='" + CLR_ERROR + "'>CONNECTION WAS LOST</font><br/>");
				chat.post(receiveMessage);
				syslog.post(receiveMessage);
				break;
			case ERROR:
				ErrorEvent ev = (ErrorEvent) e;
				received = Html.fromHtml(SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : "" +
						"<font color='" + CLR_ERROR + "'>AN ERROR OCCURRED</font><br/>");
				chat.post(receiveMessage);
				syslog.post(receiveMessage);
				break;
			default:
				break;
			
		}
	}

	
	Runnable sendMessage = new Runnable(){
		@Override
        public void run() {
			String newMessage = input.getText().toString();
			if(newMessage=="")
				return;
			chat.append(Html.fromHtml("<font color='" + CLR_ME + "'>" + "<b>" + session.getNick() + "</b></font>: " + newMessage + "<br/>"));
			session.sayChannel(session.getChannel(CHANNEL_NAME), newMessage);
			input.setText("");
			
		}
	};
	//*/
	
    Runnable receiveMessage = new Runnable(){
        @Override
        public void run() {
            chat.append(received);
            input.hasFocus();
            chat.invalidate();
            sv.fullScroll(ScrollView.FOCUS_DOWN);
        };	

    };
    
    /*getScrollView().post(new Runnable() {

        @Override
        public void run() {
            getScrollView().fullScroll(ScrollView.FOCUS_DOWN);
        }
    });*/

   
    Runnable receiveLog = new Runnable(){
        @Override
        public void run() {
            syslog.append(received);
            syslog.invalidate();
            sv2.smoothScrollTo(0, syslog.getBottom());
        };	

    };

	
}
