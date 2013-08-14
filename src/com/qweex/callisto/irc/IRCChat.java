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
package com.qweex.callisto.irc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.*;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.*;
import android.text.style.ImageSpan;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.qweex.callisto.R;

import com.qweex.callisto.StaticBlob;
import com.qweex.callisto.VideoActivity;
import jerklib.*;
import jerklib.events.*;
import jerklib.listeners.IRCEventListener;


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
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.TextView.OnEditorActionListener;


/** The IRC portion of the app to connect to the JB chatroom.
 * @author MrQweex
 */


/* TIPS:
    - /help
    - /me
    - Nickserv
    - /msg      (pm)
    - /nick
    - /whois
    - /quit

    - !suggest
    - !next
    - !schedule

    - !bacon
    - !8ball
    - !angela
    - !allan
 */
public class IRCChat extends Activity implements IRCEventListener
{
    protected static final int LOG_ID=Menu.FIRST+1;
    protected static final int CHANGE_ID=LOG_ID+1;
    protected static final int LOGOUT_ID=CHANGE_ID+1;
    protected static final int NICKLIST_ID=LOGOUT_ID+1;
    protected static final int SAVE_ID=NICKLIST_ID+1;
    protected static final int JBTITLE_ID=SAVE_ID+1;
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
    private static int lastScroll_chat, lastScroll_log;
    private EditText input;

    private SimpleDateFormat sdfTime = new SimpleDateFormat("'['HH:mm']'");
    private boolean isLandscape;
    private static HashMap<String, Integer> nickColors = new HashMap<String, Integer>();
    public static List<String> nickList;
    private static Handler ircHandler = null;
    private static Runnable chatUpdater;
    private boolean irssi;
    private int IRSSI_GREEN = 0x00B000;
    private MenuItem Nick_MI, Logout_MI, Log_MI, Save_MI, Change_MI, JBTitle_MI;
    private WifiLock IRC_wifiLock;
    final private int RECONNECT_TRIES = 5;
    private int SESSION_TIMEOUT = 40;
    private EditText user, pass;
    private PopupWindow changeNickDialog;
    private int timeoutCount;
    private Timer loopTimer = new Timer();
    private TimerTask loopTask;
    private String nickSearch = "", lastNickSearched = "";
    private Pattern mentionPattern;
    //Stole these from nirc; https://github.com/cjstewart88/nirc/blob/master/public/javascripts/client.js
    private String mention_before = "(^|[^a-zA-Z0-9\\[\\]{}\\^`|])",
            mention_after = "([^a-zA-Z0-9\\[\\]{}\\^`|]|$)";
    private boolean IRCOpPermission;

    private ListView chatListview, logListview;
    private static java.util.Queue<IrcMessage> chatQueue = new java.util.LinkedList<IrcMessage>(),
            logQueue = new java.util.LinkedList<IrcMessage>();

    enum SPECIAL_COLORS { ME, TOPIC, PM, JOIN, NICK, PART, KICK, ERROR, QUIT, _OTHER };

    public class IrcMessage
    {
        String title, message, _otherNick;
        SPECIAL_COLORS color;
        Date timestamp;
        int getColor()
        {
            System.out.println("Butts: " + color);
            switch(color)
            {
                case ME:
                    return CLR_ME;
                case TOPIC:
                    return CLR_TOPIC;
                case PM:
                    return CLR_PM;
                case JOIN:
                    return CLR_JOIN;
                case NICK:
                    return CLR_NICK;
                case PART:
                    return CLR_PART;
                case KICK:
                    return CLR_KICK;
                case ERROR:
                    return CLR_ERROR;
                case QUIT:
                    return CLR_QUIT;
            }
            return 0;
        }

        public IrcMessage(String title, String message, SPECIAL_COLORS clr)
        {
            this.title = title;
            this.message = message;
            this.color = clr;
            timestamp = new Date();
        }
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(chatListview!=null)
        {
            lastScroll_chat = chatListview.getFirstVisiblePosition();
            lastScroll_log = logListview.getFirstVisiblePosition();
        }
    }


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
                    IrcMessage received;
                    while(!chatQueue.isEmpty())
                    {
                        received = chatQueue.poll();
                        if(received==null)
                            return;

                        StaticBlob.ircChat.add(received);
                    }
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ((ArrayAdapter)chatListview.getAdapter()).notifyDataSetChanged();
                        }
                    });
                    input.requestFocus();
                }
            };
        }
        isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
        mNotificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        profileNick = Rot47(PreferenceManager.getDefaultSharedPreferences(this).getString("irc_nick", null));
        profilePass = Rot47(PreferenceManager.getDefaultSharedPreferences(this).getString("irc_pass", null));
        irssi = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_irssi", false);
        mentionPattern = Pattern.compile(mention_before + profileNick + mention_after);
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

        login.setCompoundDrawables(this.getResources().getDrawable(R.drawable.ic_action_key), null, null, null);

        login.setOnClickListener(InitiateLogin);

        //Build the ChangeNickDialog
        changeNickDialog = new PopupWindow(this);
        android.widget.FrameLayout fl = new android.widget.FrameLayout(this);
        fl.setPadding((int)(10* StaticBlob.DP), (int)(10* StaticBlob.DP), (int)(10* StaticBlob.DP), (int)(10* StaticBlob.DP));
        fl.addView(getLayoutInflater().inflate(R.layout.irc_login, null, false));
        changeNickDialog.setContentView(fl);
        changeNickDialog.setFocusable(true);
        changeNickDialog.setTouchable(true);
        Button l = (Button) fl.findViewById(R.id.login);
        l.setText("Change");
        l.setSingleLine();
        l.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String nick = ((EditText)((LinearLayout)v.getParent()).findViewById(R.id.user)).getText().toString();
                String pass = ((EditText)((LinearLayout)v.getParent()).findViewById(R.id.pass)).getText().toString();
                Log.e("ADS", nick + "!");
                if(nick.equals(""))
                {
                    changeNickDialog.dismiss();
                    return;
                }
                profileNick = nick;
                mentionPattern = Pattern.compile(mention_before + profileNick + mention_after);
                parseOutgoing("/nick " + profileNick);
                if(pass.equals(""))
                {
                    changeNickDialog.dismiss();
                    return;
                }
                profilePass = pass;
                parseOutgoing("/msg nickserv identify " + profilePass);
                changeNickDialog.dismiss();
                return;
            }
        });
        changeNickDialog.setOutsideTouchable(true);
        changeNickDialog.setWidth(getWindowManager().getDefaultDisplay().getWidth()*8/10);
        changeNickDialog.setHeight(getWindowManager().getDefaultDisplay().getHeight()*4/10);
        changeNickDialog.setAnimationStyle(android.R.style.Animation_Dialog);


        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        IRC_wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "Callisto_irc");

        // Show warning for those evil Verizon users
        String[] naughtyCarriers = new String[] {"verizon"};

        int i = Arrays.asList(naughtyCarriers).indexOf(StaticBlob.teleMgr.getNetworkOperatorName());
        System.out.println("i=" + i + " " + StaticBlob.teleMgr.getNetworkOperatorName());
        if(i>=0)
            new AlertDialog.Builder(this).setTitle("Warning for " + titleCase(naughtyCarriers[i]))
                    .setMessage("It has been reported that your carrier actively blocks all IRC traffic, so wifi is recommended." +
                        "\n\nOtherwise, unexpected behaviour may result, abandon all hope, ye who enter here.")
                    .setNegativeButton(android.R.string.ok, null)
                    .setIcon(R.drawable.ic_action_cancel)
                    .show();

    }

    public static String titleCase(String realName) {
        String space = " ";
        String[] names = realName.split(space);
        StringBuilder b = new StringBuilder();
        for (String name : names) {
            if (name == null || name.isEmpty()) {
                b.append(space);
                continue;
            }
            b.append(name.substring(0, 1).toUpperCase())
                    .append(name.substring(1).toLowerCase())
                    .append(space);
        }
        return b.toString();
    }

    protected OnClickListener InitiateLogin= new OnClickListener(){
        @Override
        public void onClick(View v) {
            profileNick = user.getText().toString();
            mentionPattern = Pattern.compile(mention_before + profileNick + mention_after);
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
        JBTitle_MI = menu.add(0, JBTITLE_ID, 0, "JBTitle").setIcon(R.drawable.ic_action_like);
        updateMenu();
        return true;
    }

    /** Updates the menu items because invalidateOptionsMenu() is not supported on all APIs. */
    private void updateMenu()
    {
        try {
            Log_MI.setEnabled(session!=null);
            Nick_MI.setEnabled(session!=null);
    	    Logout_MI.setEnabled(session!=null);
            Save_MI.setEnabled(session!=null);

            Log.d("DERP", session + "");

            Change_MI.setTitle(this.getClass()== VideoActivity.class ? "Open IRC" : ((session!=null && session.getRetries()>1 ) ? "Reconnect" : "Change Nick"));
            Change_MI.setEnabled(this.getClass()==VideoActivity.class || (session!=null));
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
                CharSequence cs = "";
                IrcMessage ircm;
                for(int i=0; i<StaticBlob.ircChat.size(); i++)
                {
                    ircm = StaticBlob.ircChat.get(i);
                    TextUtils.concat(cs,ircm.title + ircm.message);
                }
                File fileLocation = new File(StaticBlob.storage_path + File.separator +
                                "ChatLog_" + StaticBlob.sdfRaw.format(new Date()) +
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
            case JBTITLE_ID:
                startActivity(new Intent(this, com.qweex.callisto.moar.jbtitle.class));
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
        //Read colors
        if(irssi)
        {
            CLR_TEXT=CLR_TOPIC=CLR_ME=CLR_JOIN=CLR_MYNICK=CLR_NICK=CLR_PART=CLR_QUIT=CLR_KICK=CLR_ERROR=CLR_MENTION=CLR_PM=CLR_LINKS=IRSSI_GREEN;
            CLR_BACK=0x000000;
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

        input = new EditText(this);
        //input.getLayoutParams().width = ViewGroup.LayoutParams.FILL_PARENT;
        input.setEms(10);
        input.setTextColor(this.getResources().getColor(R.color.txtClr));
        input.setMaxLines(1);
        input.setSingleLine();
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFocusable(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input = (EditText) findViewById(R.id.inputField);


        if(android.os.Build.VERSION.SDK_INT >= 11)
            input.setImeOptions( input.getImeOptions() | EditorInfo.IME_FLAG_NAVIGATE_NEXT); //TODO: Version
        input.setOnEditorActionListener(new OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
                input.post(sendMessage);
                return true;
            }
        });

        chatListview = (ListView) findViewById(R.id.chatView);
        chatListview.setVerticalFadingEdgeEnabled(false);
        logListview = (ListView) findViewById(R.id.logView);
        logListview.setVerticalFadingEdgeEnabled(false);
//        chatListview.addFooterView(input);

        ((ViewAnimator)findViewById(R.id.viewanimator)).setBackgroundColor(0xFF000000 + CLR_BACK);
        if(irssi && android.os.Build.VERSION.SDK_INT>12) //android.os.Build.VERSION_CODES.GINGERBREAD_MR1
            input.setTextColor(0xff000000 + IRSSI_GREEN);
        if(session!=null && session.getIRCEventListeners().size()==0)
            session.addIRCEventListener(this);
        if(StaticBlob.notification_chat!=null)
        {
            StaticBlob.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
            StaticBlob.notification_chat.defaults = 0;    //This will be over-written when a mention happens, but we have to set it to ALL to disable an annoying buzz when resuming the activity
            mNotificationManager.notify(StaticBlob.NOTIFICATION_ID, StaticBlob.notification_chat);
        }


        chatListview.setAdapter(new IrcAdapter<IrcMessage>(this,R.layout.irc_line,StaticBlob.ircChat));
        chatListview.smoothScrollToPosition(lastScroll_chat);
        logListview.setAdapter(new IrcAdapter<IrcMessage>(this,R.layout.irc_line,StaticBlob.ircLog));
        logListview.smoothScrollToPosition(lastScroll_log);
    }

    @Override
    public void onResume()
    {
        if(StaticBlob.notification_chat!=null)
        {
            StaticBlob.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
            mNotificationManager.notify(StaticBlob.NOTIFICATION_ID, StaticBlob.notification_chat);
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

        resume();

        actuallyConnect();
        updateMenu();
    }

    protected void actuallyConnect()
    {
        Intent notificationIntent = new Intent(this, IRCChat.class);
        contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        StaticBlob.notification_chat = new Notification(R.drawable.ic_action_dialog, null, System.currentTimeMillis());
        StaticBlob.notification_chat.flags = Notification.FLAG_ONGOING_EVENT;
        StaticBlob.notification_chat.setLatestEventInfo(this,  "In the JB Chat",  "No new mentions", contentIntent);
        mNotificationManager.notify(StaticBlob.NOTIFICATION_ID, StaticBlob.notification_chat);


        manager = new ConnectionManager(new Profile(profileNick));
        manager.setAutoReconnect(RECONNECT_TRIES);
        int port = 6667;
        try {
            port = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("irc_port", "6667"));
        }catch(Exception e){}
        session = manager.requestConnection(SERVER_NAME, port);
        session.setRejoinOnKick(false);
        chatQueue.add(new IrcMessage("[Callisto]", "Attempting to logon.....be patient you silly goose.", SPECIAL_COLORS.ME));
        ircHandler.post(chatUpdater);
        logQueue.add(new IrcMessage("[Callisto]", "Intiating connection to " + SERVER_NAME + " on port " + port, SPECIAL_COLORS.ME));

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
                    chatQueue.add(new IrcMessage("[TIMEOUT]", "Connection timed out. Either check your connection, or set a longer timeout in the settings.", SPECIAL_COLORS.ME));
                    ircHandler.post(chatUpdater);
                    this.cancel();
                }
                timeoutCount++;
            }
        };
        loopTimer.purge();
        loopTimer.schedule(loopTask, 0, 1000);

        session.addIRCEventListener(this);
    }

    /** Used to logout, quit, or part. */
    public void logout(String quitMsg)
    {
        if(quitMsg==null)
            quitMsg = PreferenceManager.getDefaultSharedPreferences(this).getString("irc_quit", null);
        else
            session.getChannel(CHANNEL_NAME).part(quitMsg);
        System.out.println(1);


        IrcMessage ircm = new IrcMessage("~~~~~[TERMINATED]~~~~~",null, SPECIAL_COLORS.ERROR);
        chatQueue.add(ircm);
        ircHandler.post(chatUpdater);

        new QuitPlz().execute((Void[])null);
        mNotificationManager.cancel(StaticBlob.NOTIFICATION_ID);
        StaticBlob.notification_chat = null;
        isFocused = false;
        if(IRC_wifiLock!=null && IRC_wifiLock.isHeld())
            IRC_wifiLock.release();
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
                    if(nickList==null)
                    {
                        nickList = ((NickListEvent) e).getNicks();
                        Collections.sort(nickList, String.CASE_INSENSITIVE_ORDER);
                        break;
                    }
                    NickListEvent nle = (NickListEvent) e;
                    String allNicks = "";
                    for(String s : nle.getNicks())
                    {
                        allNicks = allNicks.concat("[" + s + "] ");
                        if(allNicks.length()>60)
                        {
                            chatQueue.add(new IrcMessage("[NAMES]", allNicks, SPECIAL_COLORS.TOPIC));
                            allNicks = "";
                        }
                    }
                    if(allNicks.length()>0)
                        chatQueue.add(new IrcMessage("[NAMES]", allNicks, SPECIAL_COLORS.TOPIC));
                    ircHandler.post(chatUpdater);
                    break;
                //TODO: This might not work. I dunno.
                case CTCP_EVENT:
                    CtcpEvent ce = (CtcpEvent) e;
                    String realEvent = ce.getCtcpString().substring(0, ce.getCtcpString().indexOf(" "));
                    if(realEvent.equals("ACTION"))
                    {
                        String realAction = ce.getCtcpString().substring(realEvent.length()).trim();
                        String realPerson = ce.getRawEventData().substring(1, ce.getRawEventData().indexOf("!"));
                        chatQueue.add(new IrcMessage("* " + realPerson + " " + realAction, null, SPECIAL_COLORS.ME));
                        ircHandler.post(chatUpdater);
                    }
                    break;
                case AWAY_EVENT://This isn't even effing used! (for other people's away
                    AwayEvent a = (AwayEvent) e;
                    if(a.isYou())
                    {
                        chatQueue.add(new IrcMessage("You are " + (a.isAway() ? " now " : " no longer ") + "away (" + a.getAwayMessage() + ")", null, SPECIAL_COLORS.TOPIC));
                    }
                    chatQueue.add(new IrcMessage("[AWAY]", a.getNick() + " is away: " + a.getAwayMessage(), SPECIAL_COLORS.TOPIC));
                    ircHandler.post(chatUpdater);
                    break;
                case MODE_EVENT:
                    Log.d("DERP;", e + "!");
                    List<ModeAdjustment> lm = ((ModeEvent) e).getModeAdjustments();
                    String setter = ((ModeEvent)e).setBy(), plus = "", minus = "";
                    ArrayList<String> prettified = new ArrayList<String>();
                    for(ModeAdjustment ma : lm)
                    {
                        //if(!setter.toLowerCase().endsWith(".geekshed.net") && !setter.equals(""))
                        {
                            String tmp = setter; //(setter.equals(session.getNick())?"You":setter);
                            switch(ma.getMode())
                            {
                                case 'm':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("un");
                                    tmp = tmp.concat("moderated'.");
                                    break;
                                case 'p':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("public'.");
                                    else
                                        tmp = tmp.concat("private'.");
                                    break;
                                case 's':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("visible'.");
                                    else
                                        tmp = tmp.concat("secret'.");
                                    break;
                                case 'i':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("not ");
                                    tmp = tmp.concat("invite only'.");
                                    break;
                                case 'n':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    else
                                        tmp = tmp.concat("allow ");
                                    tmp = tmp.concat("messages from outside'");
                                    break;
                                case 't':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    tmp = tmp.concat("topic protection'.");
                                    break;
                                case 'R':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("doesn't ");
                                    tmp = tmp.concat("require register'.");
                                    break;
                                case 'c':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    tmp = tmp.concat("colours'.");
                                    break;
                                case 'Q':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    tmp = tmp.concat("kicks allowed'.");
                                    break;
                                case 'O':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("not ");
                                    tmp = tmp.concat("Ops only'.");
                                    break;
                                case 'A':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("not ");
                                    tmp = tmp.concat("Admins only'.");
                                    break;
                                case 'K':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    tmp = tmp.concat("knocking'.");
                                    break;
                                case 'V':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    tmp = tmp.concat("inviting'.");
                                    break;
                                case 'S':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("do not ");
                                    tmp = tmp.concat("strip colours'.");
                                    break;
                                case 'l':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    tmp = tmp.concat("channel limit" + (ma.getArgument()!=null + " [" + ma.getArgument() + " users]") + "'.");
                                    break;
                                case 'k':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    tmp = tmp.concat("key required'.");
                                    break;
                                case 'L':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    tmp = tmp.concat("key required'.");
                                    break;
                                case 'N':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("no ");
                                    tmp = tmp.concat("nick changes allowed'.");
                                    break;
                                case 'G':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("not ");
                                    tmp = tmp.concat("G-rated'.");
                                    break;
                                case 'u':
                                    tmp = tmp.concat(" sets channel mode to '");
                                    if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                        tmp = tmp.concat("not ");
                                    tmp = tmp.concat("auditorium'.");
                                    break;

                                //OoiwghskSaANTCcfrxebWqBFIHdvtGz
                                case 'q':
                                    if(ma.getAction().equals(ModeAdjustment.Action.PLUS))
                                        tmp = tmp.concat(" gives ");
                                    else
                                        tmp = tmp.concat(" takes ");
                                    tmp = tmp.concat(ma.getArgument() + " '" + "Owner" + "'");
                                    break;
                                case 'o':
                                    if(ma.getAction().equals(ModeAdjustment.Action.PLUS))
                                        tmp = tmp.concat(" gives ");
                                    else
                                        tmp = tmp.concat(" takes ");
                                    tmp = tmp.concat(ma.getArgument() + " '" + "Operator" + "'");
                                    break;
                                case 'v':
                                    if(ma.getAction().equals(ModeAdjustment.Action.PLUS))
                                        tmp = tmp.concat(" gives ");
                                    else
                                        tmp = tmp.concat(" takes ");
                                    tmp = tmp.concat(ma.getArgument() + " '" + "Voice" + "'");
                                    break;
                                case 'a':
                                    if(ma.getAction().equals(ModeAdjustment.Action.PLUS))
                                        tmp = tmp.concat(" gives ");
                                    else
                                        tmp = tmp.concat(" takes ");
                                    tmp = tmp.concat(ma.getArgument() + " '" + "Admin" + "'");
                                    break;
                                case 'h':
                                    if(ma.getAction().equals(ModeAdjustment.Action.PLUS))
                                        tmp = tmp.concat(" gives ");
                                    else
                                        tmp = tmp.concat(" takes ");
                                    tmp = tmp.concat(ma.getArgument() + " '" + "Admin" + "'");
                                    break;
                                case 'b':
                                    if(ma.getAction().equals(ModeAdjustment.Action.PLUS))
                                        tmp = tmp.concat(" sets  ");
                                    else
                                        tmp = tmp.concat(" lifts ");
                                    tmp = tmp.concat("a ban on " + ma.getArgument());
                                    break;
                                case 'e':
                                    if(ma.getAction().equals(ModeAdjustment.Action.PLUS))
                                        tmp = tmp.concat(" sets  ");
                                    else
                                        tmp = tmp.concat(" lifts ");
                                    tmp = tmp.concat("a ban exception on " + ma.getArgument());
                                    break;

                                default:
                                    // L f O
                                    tmp = tmp.concat(" sets channel mode: " + (ma.getAction().equals(ModeAdjustment.Action.MINUS)?'-':'+') + ma.getMode());
                            }
                            prettified.add(tmp);
                        }
                        //else
                        {
                            //No argument means it is changing your personal modes......I think
                            //if((ma.getArgument()==null || ma.getArgument().equals("")))
                            {
                                if(ma.getAction().equals(ModeAdjustment.Action.MINUS))
                                    minus = minus.concat(ma.getMode() + "");
                                else
                                    plus = plus.concat(ma.getMode() + "");
                            }
                        }
                        Log.e("DERP:", ((ModeEvent)e).setBy() + " " + ma.getMode() + " " + ma.getAction() + " " + ma.getArgument() + ((ModeEvent)e).getChannel());
                    }

                    Log.d("Derp...", "Hellooooo " + prettified.size());
                    for(String s : prettified)
                    {
                        chatQueue.add(new IrcMessage("***" + s, null, SPECIAL_COLORS.TOPIC));       //TODO: Different color?
                        ircHandler.post(chatUpdater);
                    }
                    if(!plus.equals(""))
                        plus = "+" + plus;
                    if(!minus.equals(""))
                        minus = "-" + minus;
                    if((setter.toLowerCase().endsWith(".geekshed.net") || setter.equals("ChanServ")) && (!plus.equals("") || !minus.equals("")))
                    {;
                        Log.d("Derp...", (setter.toLowerCase().endsWith(".geekshed.net")==true) + " " + (!plus.equals("")) + " " + (!minus.equals("")));
                        logQueue.add(new IrcMessage("[MODE]", setter + " has changed your personal modes: " + plus + minus, SPECIAL_COLORS.TOPIC));       //TODO: Different color?
                        ircHandler.post(logUpdater);
                    }
                    break;

                //Syslog events
                case SERVER_INFORMATION:
                    //FORMAT
                    ServerInformationEvent s = (ServerInformationEvent) e;
                    ServerInformation S = s.getServerInformation();
                    logQueue.add(new IrcMessage("[INFO]", S.getServerName(), SPECIAL_COLORS.TOPIC));
                    ircHandler.post(logUpdater);
                    break;
                case SERVER_VERSION_EVENT:
                    ServerVersionEvent sv = (ServerVersionEvent) e;
                    logQueue.add(new IrcMessage("[VERSION]", sv.getVersion(), SPECIAL_COLORS.TOPIC));
                    ircHandler.post(logUpdater);
                    break;
                case CONNECT_COMPLETE:
                    ConnectionCompleteEvent c = (ConnectionCompleteEvent) e;
                    logQueue.add(new IrcMessage(null, c.getActualHostName() + "\nConnection complete", SPECIAL_COLORS.TOPIC));
                    e.getSession().join(CHANNEL_NAME);
                    ircHandler.post(logUpdater);
                    break;
                case JOIN_COMPLETE:
                    //JoinCompleteEvent jce = (JoinCompleteEvent) e;
                    chatQueue.add(new IrcMessage("[JOIN]", "Join complete, you are now orbiting Jupiter Broadcasting!", SPECIAL_COLORS.TOPIC));
                    if(profilePass!=null && profilePass!=null && !profilePass.equals(""))
                        parseOutgoing("/MSG NickServ identify " + profilePass);
                    System.out.println("Decrypted password: " + profilePass);
                    ircHandler.post(chatUpdater);
                    break;
                case MOTD:
                    MotdEvent mo = (MotdEvent) e;
                    logQueue.add(new IrcMessage("[MOTD]", mo.getMotdLine(), SPECIAL_COLORS.TOPIC));
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
                        chatQueue.add(new IrcMessage("[NICKSERV]", ne.getNoticeMessage(), SPECIAL_COLORS.TOPIC));
                        System.out.println("FUCK YEAH BANANAS " + chatQueue.size());
                        ircHandler.post(chatUpdater);
                    }
                    else
                    {
                        logQueue.add(new IrcMessage("[NOTICE]", ne.getNoticeMessage(), SPECIAL_COLORS.TOPIC));
                        System.out.println("fuck no bananas");
                        ircHandler.post(logUpdater);
                    }

                    break;

                //Chat events
                case TOPIC:
                    TopicEvent t = (TopicEvent) e;
                    if(t.getTopic()=="")
                        chatQueue.add(new IrcMessage("[TOPIC] ", "No Topic Set", SPECIAL_COLORS.TOPIC));
                    else
                        chatQueue.add(new IrcMessage("[TOPIC] " + t.getTopic() + " (set by " + t.getSetBy() + " on " + t.getSetWhen() + " )", null, SPECIAL_COLORS.TOPIC));
                    ircHandler.post(chatUpdater);
                    break;

                case PRIVATE_MESSAGE:
                case CHANNEL_MESSAGE:
                    MessageEvent m = (MessageEvent) e;
                    if((e.getType()).equals(jerklib.events.IRCEvent.Type.PRIVATE_MESSAGE))
                        chatQueue.add(new IrcMessage("->" + m.getNick(), m.getMessage(), SPECIAL_COLORS.PM));
                    else
                        chatQueue.add(new IrcMessage(m.getNick(), m.getMessage(), SPECIAL_COLORS._OTHER));
                    System.out.println("CHANNEL_MESSAGE: " + m.getMessage());
                    ircHandler.post(chatUpdater);
                    break;
                case JOIN:
                    JoinEvent j = (JoinEvent) e;
                    nickList.add(j.getNick());
                    chatQueue.add(new IrcMessage(j.getNick() + " entered the room.", null, SPECIAL_COLORS.JOIN));
                    ircHandler.post(chatUpdater);
                    break;
                case NICK_CHANGE:
                    NickChangeEvent ni = (NickChangeEvent) e;
                    chatQueue.add(new IrcMessage(ni.getOldNick() + " changed their nick to " + ni.getNewNick(), null, SPECIAL_COLORS.NICK));
                    ircHandler.post(chatUpdater);
                    break;
                case PART:
                    PartEvent p = (PartEvent) e;
                    nickColors.remove(p.getNick());
                    nickList.remove(p.getNick());
                    chatQueue.add(new IrcMessage("PART: " + p.getNick() + " (" + p.getPartMessage() + ")", null, SPECIAL_COLORS.PART));
                    ircHandler.post(chatUpdater);
                    break;
                case QUIT:
                    QuitEvent q = (QuitEvent) e;
                    nickColors.remove(q.getNick());
                    nickList.remove(q.getNick());
                    chatQueue.add(new IrcMessage("QUIT:  " + q.getNick() + " (" + q.getQuitMessage() + ")", null, SPECIAL_COLORS.QUIT));
                    ircHandler.post(chatUpdater);
                    break;
                case KICK_EVENT:
                    KickEvent k = (KickEvent) e;
                    chatQueue.add(new IrcMessage("KICK:  " + k.getWho() + " was kicked by " + k.byWho()  + ". (" + k.getMessage() + ")", null, SPECIAL_COLORS.KICK));
                    ircHandler.post(chatUpdater);
                    break;
                case NICK_IN_USE:
                    NickInUseEvent n = (NickInUseEvent) e;
                    chatQueue.add(new IrcMessage("NICKINUSE:  " + n.getInUseNick() + " is in use.", null, SPECIAL_COLORS.ERROR));
                    ircHandler.post(chatUpdater);
                    break;
                case WHO_EVENT:
                    WhoEvent we = (WhoEvent) e;
                    chatQueue.add(new IrcMessage("[WHO]", we.getNick() + " is " + we.getUserName() + "@" + we.getServerName() + " (" + we.getRealName() + ")", SPECIAL_COLORS.TOPIC));
                    ircHandler.post(chatUpdater);
                    break;
                case WHOIS_EVENT:
                    WhoisEvent wie = (WhoisEvent) e;
                    String var = "";
                    for(String event : wie.getChannelNames())
                        var = var + " " + event;

                    chatQueue.add(new IrcMessage("[WHOIS]", wie.getUser() + " is " + wie.getHost() + "@" + wie.whoisServer() + " (" + wie.getRealName() + ")", SPECIAL_COLORS.TOPIC ));
                    chatQueue.add(new IrcMessage("[WHOIS]", wie.getUser() + " is a user on channels: " + var, SPECIAL_COLORS.TOPIC ));
                    chatQueue.add(new IrcMessage("[WHOIS]", wie.getUser() + " has been idle for " + wie.secondsIdle() + " seconds", SPECIAL_COLORS.TOPIC ));
                    chatQueue.add(new IrcMessage("[WHOIS]", wie.getUser() + " has been online since " + wie.signOnTime(), SPECIAL_COLORS.TOPIC ));
                    ircHandler.post(chatUpdater);
                    break;
                case WHOWAS_EVENT: //TODO: Fix?
                    WhowasEvent wwe = (WhowasEvent) e;
                    chatQueue.add(new IrcMessage("[WHO]", wwe.getNick() + " is " + wwe.getUserName() + "@" + wwe.getHostName() + " (" + wwe.getRealName() + ")", SPECIAL_COLORS.TOPIC));
                    break;

                //Errors that display in both
                case CONNECTION_LOST:
                    //ConnectionLostEvent co = (ConnectionLostEvent) e;
                    chatQueue.add(new IrcMessage("CONNECTION WAS LOST - attempt " + session.getRetries(), null, SPECIAL_COLORS.ERROR));
                    logQueue.add(new IrcMessage("CONNECTION WAS LOST - attempt " + session.getRetries(), null, SPECIAL_COLORS.ERROR));
                    ircHandler.post(chatUpdater);
                    ircHandler.post(logUpdater);
                    break;
                case ERROR:
                    final int
                            ERR_NOSUCHNICK = 401,
                            ERR_NOTEXTTOSEND = 412,
                            ERR_NICKNAMEINUSE = 433,
                            ERR_USERONCHANNEL = 443,
                            ERR_CHANOPRIVSNEEDED = 482,
                            ERR_NONONREG = 486
                                    ;
                    boolean retry = false, absolutefail = false;

                    if(e.getClass()==UnresolvedHostnameErrorEvent.class)
                        retry = true;
                    else {
                        int errorCode = Integer.parseInt(e.getRawEventData().split(" ")[1]);
                        Log.e("ERROR: ", "Code: " + errorCode);
                        switch(errorCode)
                        {
                            case ERR_NOSUCHNICK:
                            case ERR_CHANOPRIVSNEEDED:
                            case ERR_NONONREG:
                            case ERR_NOTEXTTOSEND:
                            case ERR_NICKNAMEINUSE:
                            case ERR_USERONCHANNEL:
                                retry = false;
                        }
                    }

                    String rrealmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                    this.loopTask.cancel();
                    loopTimer.purge();
                    chatQueue.add(new IrcMessage("[ERROR]", rrealmsg + (retry ? " - attempt " + session.getRetries() : ""), SPECIAL_COLORS.ERROR));
                    logQueue.add(new IrcMessage("[ERROR]", rrealmsg + (retry ? " - attempt " + session.getRetries() : ""), SPECIAL_COLORS.ERROR));
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
                        chatQueue.add(new IrcMessage("[ISON]", name + " is online", SPECIAL_COLORS.TOPIC));
                        ircHandler.post(chatUpdater);
                        break;
                    }
                    //MAP
                    if(realType.equals("006"))
                    {
                        realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        logQueue.add(new IrcMessage("[MAP] " + realmsg, null, SPECIAL_COLORS.TOPIC));
                        ircHandler.post(logUpdater);
                        break;
                    }
                    //LUSERS
                    if((i>=250 && i<=255) || i==265 || i==266)
                    {
                        realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        logQueue.add(new IrcMessage("[LUSERS]", realmsg, SPECIAL_COLORS.TOPIC));
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
                        logQueue.add(new IrcMessage("[RULES]", realmsg, SPECIAL_COLORS.TOPIC));
                        ircHandler.post(logUpdater);
                        break;
                    }
                    //LINKS
                    else if(realType.equals("364") || realType.equals("365"))
                    {
                        realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        logQueue.add(new IrcMessage("[LINKS]", realmsg, SPECIAL_COLORS.TOPIC));
                        ircHandler.post(logUpdater);
                        break;
                    }
                    //ADMIN
                    else if(i>=256 && i<=259)
                    {
                        realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        chatQueue.add(new IrcMessage("[ADMIN]", realmsg, SPECIAL_COLORS.TOPIC));
                        ircHandler.post(chatUpdater);
                        break;
                    }
                    //WHO part 2
                    else if(realType.equals("315"))
                    {
                        realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        chatQueue.add(new IrcMessage("[WHO]", realmsg, SPECIAL_COLORS.TOPIC));
                        ircHandler.post(chatUpdater);
                    }
                    //WHOIS part 2
                    else if(realType.equals("307"))
                    {
                        int ijk = e.getRawEventData().lastIndexOf(" ", e.getRawEventData().indexOf(":",2)-2)+1;
                        realmsg = e.getRawEventData().substring(ijk, e.getRawEventData().indexOf(":", 2)-1)
                                + " " + e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        chatQueue.add(new IrcMessage("[WHOIS]", realmsg, SPECIAL_COLORS.TOPIC));
                        ircHandler.post(chatUpdater);
                    }
                    //USERHOST
                    else if(realType.equals("302"))
                    {
                        realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        realmsg.replaceFirst(Pattern.quote("=+"), " is ");	//TODO: Not working? eh?
                        chatQueue.add(new IrcMessage("[USERHOST]", realmsg, SPECIAL_COLORS.TOPIC));
                        ircHandler.post(chatUpdater);
                    }
                    //CREDITS
                    else if(realType.equals("371"))
                    {
                        realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        realmsg.replaceFirst(Pattern.quote("=+"), " is ");	//TODO: Not working? eh?
                        logQueue.add(new IrcMessage("[CREDITS]", realmsg, SPECIAL_COLORS.TOPIC));
                        ircHandler.post(logUpdater);
                    }
                    //TIME
                    else if(realType.equals("391"))
                    {
                        realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        chatQueue.add(new IrcMessage("[TIME]", realmsg, SPECIAL_COLORS.TOPIC));
                        ircHandler.post(chatUpdater);
                    }
                    //USERIP
                    else if(realType.equals("340"))
                    {
                        realmsg = e.getRawEventData().substring(e.getRawEventData().indexOf(":", 2)+1);
                        chatQueue.add(new IrcMessage("[USERIP]", realmsg, SPECIAL_COLORS.TOPIC));
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
                        logQueue.add(new IrcMessage("[" + realType + "]", realmsg, SPECIAL_COLORS.TOPIC));
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
                chatQueue.add(new IrcMessage("[Callisto]", "Maximum amount of retries exceeded. Quitting.", SPECIAL_COLORS.TOPIC));
                ircHandler.post(chatUpdater);
            }
            else if(false)
            {
                chatQueue.add(new IrcMessage("[Callisto]", "Retrying connection: attempt " + session.getRetries(), SPECIAL_COLORS.TOPIC));
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
    private Spanned getReceived(IrcMessage ircm)
    {
        String theTitle = ircm.title,
               theMessage = ircm.message;
        int specialColor = ircm.getColor();
        int titleColor = 0xFF000000;
        int msgColor = 0xFF000000;
        try {
            titleColor+= (ircm.color!=SPECIAL_COLORS._OTHER ? specialColor :	getNickColor(theTitle));
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_modes", false)
                    && ircm.color==SPECIAL_COLORS._OTHER)
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
        System.out.println("getReceived: " + theMessage + " - " + mentionPattern.pattern());
        if( //!theTitle.equals("[004]") &&
                ((theMessage!=null && session!=null && mentionPattern.matcher(theMessage).find())   //If it mentions you
                        || (theTitle!=null && theTitle.startsWith("->")))) //If it's a PM
        {
            msgColor = 0xFF000000 + CLR_MENTION;
            System.out.println("MENTIONED" + isFocused);
            if(!isFocused)
            {
                if(StaticBlob.notification_chat==null)
                    StaticBlob.notification_chat = new Notification(R.drawable.ic_action_dialog, null, System.currentTimeMillis());
                mentionCount++;
                StaticBlob.notification_chat.defaults = 0;
                if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_vibrate", true) &&
                        (mentionCount==1 || PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_vibrate_all", false)))
                    StaticBlob.notification_chat.defaults |= Notification.DEFAULT_VIBRATE;
                if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_sound", true) &&
                        (mentionCount==1 || PreferenceManager.getDefaultSharedPreferences(this).getBoolean("irc_sound_all", false)))
                    StaticBlob.notification_chat.defaults |= Notification.DEFAULT_SOUND;
                StaticBlob.notification_chat.setLatestEventInfo(getApplicationContext(), "In the JB Chat",  mentionCount + " new mentions", contentIntent);
                mNotificationManager.notify(StaticBlob.NOTIFICATION_ID, StaticBlob.notification_chat);
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

        return (Spanned) TextUtils.concat(Html.fromHtml((SHOW_TIME ? ("<small>" + sdfTime.format(new Date()) + "</small> ") : ""))
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

            if(parseOutgoing(newMessage))
            {
                StaticBlob.ircChat.add(new IrcMessage(session.getNick(), newMessage, SPECIAL_COLORS.ME));
                ((ArrayAdapter)chatListview.getAdapter()).notifyDataSetChanged();
            }

            chatListview.smoothScrollToPosition(StaticBlob.ircChat.size()-1); //TODO: For some reason
            input.requestFocus();
            input.setText("");
        }
    };

    private static HashMap<String, Integer> smilyRegexMap = null;
    public CharSequence parseEmoticons(Spanned s)
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
                Bitmap smiley = BitmapFactory.decodeResource(this.getResources(), ((Integer) pairs.getValue()));
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
            String targetNick = "";
            String targetMsg = "";
            try {
                targetNick = msg.substring("/MSG ".length());
                targetNick = msg.substring(("/MSG ").length(), msg.indexOf(" ", "/MSG ".length()+1));
                targetMsg = msg.substring("/MSG ".length() + targetNick.length());
            }catch(Exception e){}
            session.sayPrivate(targetNick, targetMsg);
            if(!targetNick.toUpperCase().equals("NICKSERV") && targetMsg.toUpperCase().startsWith("IDENTIFY"))
            {
                chatQueue.add(new IrcMessage("<-" + targetNick, targetMsg, SPECIAL_COLORS.PM));
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
                || msg.toUpperCase().startsWith("/LICENSE")
                || msg.toUpperCase().startsWith("/NAMES")
                || msg.toUpperCase().startsWith("/ISON ")
                || msg.toUpperCase().startsWith("/PING ")
                || msg.toUpperCase().startsWith("/PONG ")
                || msg.toUpperCase().startsWith("/STATS ")
                || msg.toUpperCase().startsWith("/HELPOP")
                || msg.toUpperCase().startsWith("/SETNAME")
                || msg.toUpperCase().startsWith("/VHOST")
                )
        {
            session.sayRaw(msg.substring(1) + " " + CHANNEL_NAME);
            return false;
        }
        else if(msg.toUpperCase().startsWith("/ME "))
        {
            session.action(CHANNEL_NAME, "ACTION" + msg.substring(3));
            chatQueue.add(new IrcMessage("* " + session.getNick() + msg.substring(3), null, SPECIAL_COLORS.ME));
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
        //Op stuff. Because I am an Op. SUCK IT, PEOPLE WHO ALWAYS SAID I WOULDN'T AMOUNT TO ANYTHING.
        else if(msg.toUpperCase().startsWith("/KICK "))
        {
            msg = msg.substring("/KICK ".length());
            String nick = msg.split("\\s+")[0];
            String reason = nick.length()<msg.length()?msg.substring(nick.length()+1) : session.getNick();
            Log.e("DERP!:", nick + " | " + reason);
            session.getChannel(CHANNEL_NAME).kick(nick, reason);
            return false;
        }
        else if(msg.toUpperCase().startsWith("/INVITE "))
        {
            msg = msg.substring("/INVITE ".length());
            String nick = msg.split("\\s+")[0];
            try {
                if(!msg.substring(nick.length()+1).toLowerCase().equals("#jupiterbroadcasting"))
                {
                    Toast.makeText(IRCChat.this, "What, is the JB chat not enough for them?!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }catch(Exception e){}
            session.invite(nick, session.getChannel(CHANNEL_NAME));
            return true;
        }
        else if(msg.toUpperCase().startsWith("/OPER "))
        {
            session.getChannel(CHANNEL_NAME).op(msg.substring("/OPER ".length()));
            return false;
        }
        else if(msg.toUpperCase().startsWith("/WATCH"))
        {
            session.sayRaw(msg.substring(1));
            return false;
        }
        else if(msg.toUpperCase().startsWith("/MODE "))
        {
            session.getChannel(CHANNEL_NAME).mode(msg.substring("/MODE ".length()));
            return false;
        }
        else if(msg.startsWith("/"))
        {
            //PRIVMSG
            //NOTICE?

            String unknown = msg;
            try {
                unknown = msg.substring(1, msg.indexOf(" "));
            }catch(Exception e){
                unknown = unknown.substring(1);
            }
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
        chatQueue.add(new IrcMessage("[CALLISTO]", "Command not recognized!", SPECIAL_COLORS.TOPIC));
        ircHandler.post(chatUpdater);
        return false;
    }

    /** A runnable to update the log */
    Runnable logUpdater = new Runnable()
    {
        @Override
        public void run()
        {
            Log.e("ASDSADSADS", logQueue.peek() + "1");
            StaticBlob.ircLog.add(logQueue.remove());
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    ((ArrayAdapter)logListview.getAdapter()).notifyDataSetChanged();
                }
            });
        };

    };

    /** A function object to flash the background of an EditText control.
     *  Changes the background, waits a certain amount of time, then changes it back
     * @author notbryant */
    class EditTextFlash
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
            color = IRCChat.this.getResources().getColor(com.qweex.callisto.R.color.Salmon);
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


    public class IrcAdapter<E> extends ArrayAdapter<E>
    {
        ArrayListWithMaximum<E> data;
        ColorStateList cls;
        int textViewResourceId;
        void init(int i)
        {
            this.textViewResourceId = i;
            cls = new ColorStateList(
                    new int[][] {
                            new int[] { android.R.attr.state_pressed},
                            new int[] { android.R.attr.state_focused},
                            new int[] {}
                    },
                    new int [] {
                            Color.BLUE,
                            0xFF000000 + CLR_LINKS,
                            0xFF000000 + CLR_LINKS,
                    }
            );
        }

        public IrcAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            init(textViewResourceId);
        }

        public IrcAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
            init(textViewResourceId);
        }

        public IrcAdapter(Context context, int textViewResourceId, E[] objects) {
            super(context, textViewResourceId, objects);
            init(textViewResourceId);
        }

        public IrcAdapter(Context context, int resource, int textViewResourceId, E[] objects) {
            super(context, resource, textViewResourceId, objects);
            init(textViewResourceId);
        }

        public IrcAdapter(Context context, int textViewResourceId, List<E> objects) {
            super(context, textViewResourceId, objects);
            data = (ArrayListWithMaximum<E>) objects;
            init(textViewResourceId);
        }

        public IrcAdapter(Context context, int resource, int textViewResourceId, List<E> objects) {
            super(context, resource, textViewResourceId, objects);
            data = (ArrayListWithMaximum<E>) objects;
            init(textViewResourceId);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if(convertView==null)
                convertView = getLayoutInflater().inflate(textViewResourceId, null, false);

            ((TextView)convertView).setText( parseEmoticons((Spanned) getReceived((IrcMessage)data.get(position))) );
            ((TextView) convertView).setTextColor(0xff000000 + CLR_TEXT);
            ((TextView) convertView).setLinkTextColor(cls);
            return convertView;
        }
    }
}
