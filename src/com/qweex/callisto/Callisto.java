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

import java.io.File;
import java.util.*;

import android.content.*;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.qweex.callisto.podcast.*;
import com.qweex.utils.ImgTxtButton;
import com.qweex.utils.QweexUtils;
import com.qweex.utils.XBMCStyleListViewMenu;

import com.qweex.callisto.donate.Donate;
import com.qweex.callisto.irc.IRCChat;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.LinearLayout.LayoutParams;


//Task Tags: todo clean feature fixme wtf idea

//CLEAN: Rename IDs in layout XML
//CLEAN: Strings.xml

/** This class is the main activity, but also houses all of the static variables needed for the app.
 Things like the MediaPlayer, the Notifications, and the player Listeners are all here.
 @author MrQweex
 */
public class Callisto extends Activity
{
    /** Titles for the tabletMenu */
    String[] tabletMenu = getResources().getStringArray(R.array.tablet_menu);
    /** Ids used for onclicklisteners and the tablet launching activities */
    int[] buttonIds = new int[] {R.id.listen, R.id.live, R.id.plan, R.id.chat, R.id.contact, R.id.donate};
    /** Menu ID for this activity */
    private final int STOP_ID=Menu.FIRST+1, SETTINGS_ID=STOP_ID+1, MORE_ID=SETTINGS_ID+1, RELEASE_ID=MORE_ID+1,
                      TITLE_ID=RELEASE_ID+1, TWITTER_ID=TITLE_ID+1, QUIT_ID=TWITTER_ID+1;
    /** The Dialog for displaying what is new in this version **/
    private Dialog news;


    /** Called when the activity is first created. Sets up the view for the main screen and additionally initiates many of the static variables for the app.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
    //@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //This is the most reliable way I've found to determine if it is landscape
        boolean isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();

        Log.d("CALLISTO: This is my PID", "There are many like it, but this one is mine.");
        StaticBlob.init(this);

        //Update shows
        SharedPreferences pf = PreferenceManager.getDefaultSharedPreferences(this);
        int lastVersion = pf.getInt("appVersion", 0);

        //Check for pending downloads. If there are any.....um, do something.
        //if(!pf.getString(DownloadList.ACTIVE, "").equals("") && !pf.getString(DownloadList.ACTIVE, "").equals("|"))
        {
            //TODO: Display message
            //Callisto.downloading_count = pf.getString("ActiveDownloads", "").length() - pf.getString("ActiveDownloads", "").replaceAll("\\|", "").length() - 1;
            //new DownloadTask(Callisto.this).execute();
        }

        //Check to see if there is any external storage to write to
        if(!new File(StaticBlob.storage_path).exists())
        {
            if(StaticBlob.storage_path.startsWith(android.os.Environment.getExternalStorageDirectory().toString()) &&
                    !android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
                Toast.makeText(this, R.string.no_external_storage, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, R.string.folder_not_exist, Toast.LENGTH_SHORT).show();
        }

        //Create seek dialog
        PlayerControls.createSeekView(getLayoutInflater());

        boolean isTablet= QweexUtils.isTabletDevice(this);
        //Display the release notes if recently updated
        if((StaticBlob.appVersion>lastVersion))
        {
            showUpdateNews();
            if(isTablet)    //TODO: Remove this once the menu is good
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("new_mainscreen", true).commit();
            pf.edit().putInt("appVersion", StaticBlob.appVersion).commit();
        }

        //**********************Do the activity creation stuff - The stuff that is specific to the Callisto mainscreen activity**********************//
        //Set the content view
        //TODO: change this to '|=' once the menu is sufficiently advanced.
        isTablet = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("new_mainscreen", false);
        if(isTablet)
            initTablet();
        else
            initPhone();

        if(Live.LIVE_PreparedListener.pd!=null)
            Live.LIVE_PreparedListener.pd.show();
    }

    /** Initiating the activity for a Phone formfactor device OR for any device running 2.1 or earlier. (I dunno if Eclair tablets exist but if they do, they shouldn't.)
     * Note that everything in here should be specific to THIS activity. Everything concerning the app in general should
     * be in the OnCreate.
     */
    void initPhone()
    {
        setContentView(R.layout.main);
        //This loop sets the onClickListeners and adjusts the button settings if the view is landscape
        ImgTxtButton temp;
        boolean isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
        for(int i=0; i<buttonIds.length; i++)
        {
            temp = (ImgTxtButton)findViewById(buttonIds[i]);
            temp.setOnClickListener(startAct);
            if(isLandscape)
                temp.setOrientation(ImgTxtButton.HORIZONTAL);
        }

        //Set the player on click listeners; this is usually done by the PlayerInfo object, when switching activities.
        findViewById(R.id.playPause).setOnClickListener(PlayerControls.playPauseListener);
        findViewById(R.id.playlist).setOnClickListener(PlayerControls.playlist);
        findViewById(R.id.seek).setOnClickListener(PlayerControls.seekDialog);
        findViewById(R.id.next).setOnClickListener(PlayerControls.next);
        findViewById(R.id.previous).setOnClickListener(PlayerControls.previous);

        findViewById(R.id.live).setOnClickListener(LivePlayButton);
    }

    /** Initiating the activity for a Tablet device. */
    void initTablet()
    {
        setContentView(R.layout.main_tablet);

        ((View)findViewById(R.id.listView).getParent()).setBackgroundResource(R.drawable.tabback);
        XBMCStyleListViewMenu slvm = (XBMCStyleListViewMenu) this.findViewById(R.id.listView);
        if(QweexUtils.isTabletDevice(this))
            slvm.setSelectedSize(125);
        else
            slvm.setSelectedSize(75);
        slvm.setData(Arrays.asList(tabletMenu));
        slvm.setOnMainItemClickListener(new XBMCStyleListViewMenu.OnMainItemClickListener()
        {
            @Override
            public void onMainItemClick(View v, int position) {
                View dummy = new View(Callisto.this);
                dummy.setId(buttonIds[position]);
                if(buttonIds[position]==R.id.live)
                    LivePlayButton.onClick(dummy);
                else
                    startAct.onClick(dummy);
            }
        });

        //Set the player on click listeners; this is usually done by the PlayerInfo object, when switching activities.
        findViewById(R.id.playPause).setOnClickListener(PlayerControls.playPauseListener);
        findViewById(R.id.playlist).setOnClickListener(PlayerControls.playlist);
        findViewById(R.id.seek).setOnClickListener(PlayerControls.seekDialog);
        findViewById(R.id.next).setOnClickListener(PlayerControls.next);
        findViewById(R.id.previous).setOnClickListener(PlayerControls.previous);
    }

    /** Called when the activity is going to be destroyed. */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(news!=null)
            news.dismiss();
        Log.v("Callisto:onDestroy", "Destroying main activity");
        if(StaticBlob.errorDialog !=null)
            StaticBlob.errorDialog.dismiss();
        if(Live.LIVE_PreparedListener.pd!=null)
            Live.LIVE_PreparedListener.pd.dismiss();
    }

    /** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
    @Override
    public void onResume()
    {
        super.onResume();
        Log.v("Callisto:onResume", Live.LIVE_PreparedListener.pd + " ");
        if(Live.LIVE_PreparedListener.pd!=null)
            Live.LIVE_PreparedListener.pd.show();
        Log.v("Callisto:onResume", "Resuming main activity");
        if(CallistoService.audioJackReceiver!=null)
            CallistoService.audioJackReceiver.contextForPreferences = Callisto.this;
        if(StaticBlob.playerInfo!=null)
            StaticBlob.playerInfo.update(Callisto.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, STOP_ID, 0, this.getResources().getString(R.string.stop)).setIcon(R.drawable.ic_action_playback_stop);
        menu.add(0, SETTINGS_ID, 0, this.getResources().getString(R.string.settings)).setIcon(R.drawable.ic_action_settings);
        SubMenu theSubMenu = menu.addSubMenu(0, MORE_ID, 0, this.getResources().getString(R.string.more)).setIcon(R.drawable.ic_action_more);
        theSubMenu.add(0, TITLE_ID, 0, "JBTitle").setIcon(R.drawable.ic_action_like);
        //theSubMenu.add(0, TWITTER_ID, 0, "Twitter").setIcon(R.drawable.ic_action_twitter);
        theSubMenu.add(0, RELEASE_ID, 0, this.getResources().getString(R.string.release_notes)).setIcon(R.drawable.ic_action_info);

        //Stuffs for the donations stuffs
        //if(QuickPrefsActivity.packageExists(QuickPrefsActivity.DONATION_APP,this))
        //{
            //String baconString = "Get Bacon";
            //File target = new File(Environment.getExternalStorageDirectory(), StaticBlob.storage_path + File.separator + "extras");
            //if(target.exists())
                //baconString = this.getResources().getString(R.string.bacon);
            //theSubMenu.add(0, CHRISROLL_ID, 0, "Chrisrolled!").setEnabled(QuickPrefsActivity.packageExists(QuickPrefsActivity.DONATION_APP, this));
        //}

        menu.add(0, QUIT_ID, 0, this.getResources().getString(R.string.quit)).setIcon(R.drawable.ic_action_io);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case STOP_ID:
                PlayerControls.stop(this);
                return true;
            case SETTINGS_ID:
                startActivity(new Intent(this, QuickPrefsActivity.class));
                return true;
            case RELEASE_ID:
                showUpdateNews();
                return true;
            case TITLE_ID:
                startActivity(new Intent(this, com.qweex.callisto.moar.jbtitle.class));
                return true;
            case TWITTER_ID:
                startActivity(new Intent(this, com.qweex.callisto.moar.twit.class));
                return true;
            case QUIT_ID:
                finish();
                StaticBlob.mNotificationManager.cancelAll();
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Creates the layout for various activities, adding the Player Controls.
     *  It essentially takes whatever "mainView" is and wraps it and the Controls in a vertical LinearLayout.
     *  @param c The current context in which to build.
     *  @param mainView The main view of the Activity to be wrapped above the player controls
     */
    public static void build_layout(Context c, View mainView)
    {
        Log.v("*:build_layout", "Building the layout");

        // Inflate the controls and the empty view
        View controls = ((LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.controls, null, false);
        TextView empty = new TextView(c);
        empty.setId(android.R.id.empty);
        empty.setBackgroundColor(c.getResources().getColor(R.color.backClr));
        empty.setTextColor(c.getResources().getColor(R.color.txtClr));

        //Create what will be the total layout
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams   //main params
                (LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1f);
        LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams   //child params
                (LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0f);
        LinearLayout.LayoutParams xParams = new LinearLayout.LayoutParams   //main params
                (LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 0f);

        //Build a horizontal view for holding the mainView and the empty view
        LinearLayout horl = new LinearLayout(c);
        horl.setOrientation(LinearLayout.HORIZONTAL);
        horl.addView(mainView, xParams);
        horl.addView(empty, cParams);
        horl.setBackgroundColor(c.getResources().getColor(R.color.backClr));

        //Add the views to the total layout
        layout.addView(horl, mParams);
        layout.addView(controls, cParams);
        ((Activity)c).setContentView(layout);

        //Set the control listeners
        controls.findViewById(R.id.playPause).setOnClickListener(PlayerControls.playPauseListener);
        controls.findViewById(R.id.playlist).setOnClickListener(PlayerControls.playlist);
        controls.findViewById(R.id.seek).setOnClickListener(PlayerControls.seekDialog);
        controls.findViewById(R.id.next).setOnClickListener(PlayerControls.next);
        controls.findViewById(R.id.previous).setOnClickListener(PlayerControls.previous);
        Log.v("*:build_layout", "Finished building the layout");
    }

    //*********************************** On Click Listeners ***********************************//

    /** Listener to start the different activities for the main buttons on the home screen.*/
    OnClickListener startAct = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Intent newIntent;
            switch(v.getId())
            {
                case R.id.plan:
                    newIntent = new Intent(Callisto.this, CalendarActivity.class);
                    break;
                case R.id.chat:
                    newIntent = new Intent(Callisto.this, IRCChat.class);
                    break;
                case R.id.contact:
                    newIntent = new Intent(Callisto.this, ContactForm.class);
                    break;
                case R.id.donate:
                    newIntent = new Intent(Callisto.this, Donate.class);
                    break;
                case R.id.live:
                    //Not used
                default:
                    newIntent = new Intent(Callisto.this, AllShows.class);
                    break;
            }
            startActivity(newIntent);
        }
    };

    OnClickListener launchVideo = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            StaticBlob.liveDg.dismiss();
            if(StaticBlob.live_isPlaying && Live.live_player!=null)
                Live.live_player.stop();
            String live_video = PreferenceManager.getDefaultSharedPreferences(v.getContext()).getString("video_url", "rtsp://videocdn-us.geocdn.scaleengine.net/jblive/live/jblive.stream");
            Intent intent= new Intent(v.getContext(), VideoActivity.class);
            intent.putExtra("uri", live_video);
            StaticBlob.playerInfo.update(v.getContext());
            v.getContext().startActivity(intent);
            return;
        }
    };

    public static OnClickListener launchAudio = new OnClickListener()
    {
        @Override
        public void onClick(View v) {
            StaticBlob.liveDg.dismiss();
            if(Live.live_player == null || !StaticBlob.live_isPlaying)
            {
                Log.d("LiveStream:playButton", "Live player does not exist, creating it.");
                if(StaticBlob.mplayer!=null)
                    StaticBlob.mplayer.reset();
                StaticBlob.mplayer=null;
                Live.LIVE_Init();
                Live.live_player.setOnPreparedListener(Live.LIVE_PreparedListener);
                Live.LIVE_PreparedListener.setContext(v.getContext());
                String live_url = PreferenceManager.getDefaultSharedPreferences(v.getContext()).getString("live_url", "http://jbradio.out.airtime.pro:8000/jbradio_b");
                Log.d("LiveStream:playButton", "Alright so getting url");
                try {
                    Live.live_player.setDataSource(live_url);
                    if(!StaticBlob.Live_wifiLock.isHeld())
                        StaticBlob.Live_wifiLock.acquire();
                    Live.LIVE_Prepare(v.getContext());
                } catch (Exception e) {
                    //errorDialog.show();
                    e.printStackTrace();
                    Live.SendErrorReport("EXCEPTION");
                }
            }
            else
            {
                Log.d("LiveStream:playButton", "Live player does exist.");
                if(StaticBlob.live_isPlaying)
                {
                    Log.d("LiveStream:playButton", "Pausing.");
                    Live.live_player.pause();
                }
                else
                {
                    if(!StaticBlob.Live_wifiLock.isHeld())
                        StaticBlob.Live_wifiLock.acquire();
                    Log.d("LiveStream:playButton", "Playing.");
                    Live.live_player.start();
                }
                StaticBlob.live_isPlaying = !StaticBlob.live_isPlaying;
            }
            Log.d("LiveStream:playButton", "Done");
        }
    };

    /** Listener for the Live button. */
    OnClickListener LivePlayButton = new OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            Log.d("LiveStream:playButton", "Clicked play button");
            if(StaticBlob.mplayer!=null)
            {
                AlertDialog d = new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.switch_to_live)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                StaticBlob.mNotificationManager.cancel(StaticBlob.NOTIFICATION_ID);
                                StaticBlob.mplayer.reset();
                                StaticBlob.mplayer = null;
                                v.performClick();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                StaticBlob.formatAlertDialogButtons(d);
                return;
            }
            StaticBlob.liveDg.show();
            StaticBlob.liveDg.getWindow().findViewById(R.id.audio).setOnClickListener(launchAudio);
            StaticBlob.liveDg.getWindow().findViewById(R.id.video).setOnClickListener(launchVideo);
        }
    };


    //*********************************** Misc stuffs ***********************************//

    /** Builds & shows the update news for the current version */
    public void showUpdateNews()
    {
        //Build and Footer
        TextView newsHeader = new TextView(this);
        newsHeader.setPadding(5, 5, 5, 5);
        newsHeader.setBackgroundResource(R.color.backClr);
        newsHeader.setTextColor(R.color.kuler_4);
        TextView newsFooter = new TextView(this);
        newsFooter.setPadding(5, 5, 5, 5);
        newsFooter.setBackgroundResource(R.color.backClr);
        newsFooter.setTextColor(R.color.kuler_4);

        //Create the listview & data for it
        final android.widget.ExpandableListView elv = new android.widget.ExpandableListView(this);
        java.util.List<java.util.Map<String, String>> groupData = new ArrayList<java.util.Map<String, String>>();
        java.util.List<java.util.List<java.util.Map<String, String>>> childData = new ArrayList<java.util.List<java.util.Map<String, String>>>();

        //Read the file; halt if there's an error
        java.io.BufferedReader bufReader;
        try {
            bufReader = new java.io.BufferedReader(new java.io.InputStreamReader(getAssets().open("UpdateNotes")));
        } catch(Exception e) { return; }

        //Read the file line by line & process it
        String line=null;
        int place = 111;    //The placing that the current item is; numbers are a stupid way to do it. Stop judging me.
        try {
            while( (line=bufReader.readLine()) != null )
            {
                switch(place)
                {
                    case 111:   //The starting state; Does not require a new line for the first line of the header
                    case 1:     //The header
                        if("".equals(line))
                        {
                            place--;
                            continue;
                        }
                        newsHeader.setText(newsHeader.getText() + (place==111?"":"\n") + line);
                        if(place==111) place=1;
                        break;
                    case -1:    //The footer
                        newsFooter.setText(newsFooter.getText() + "\n" + line);
                        break;
                    case 0:     //An item with expandable subpoints
                        if("".equals(line))
                        {
                            place--;
                            continue;
                        }
                        java.util.Map<String, String> curGroupMap = new java.util.HashMap<String, String>();
                        java.util.List<java.util.Map<String, String>> children = new ArrayList<java.util.Map<String, String>>();
                        groupData.add(curGroupMap);

                        int x = line.indexOf("--",1);
                        int y = line.indexOf("--", x+2);;
                        curGroupMap.put("TITLE", line.substring(0, x).trim());
                        while(true)
                        {
                            if(y==-1)
                                y = line.length();
                            java.util.Map<String, String> curChildMap = new java.util.HashMap<String, String>();
                            children.add(curChildMap);
                            curChildMap.put("DESCRIPTION", line.substring(x+2, y).trim());
                            if(y==line.length())
                                break;
                            x = line.indexOf("--", x+2);
                            y = line.indexOf("--", x+2);
                        }
                        childData.add(children);
                        break;
                }
            }
        } catch(Exception e) {}

        //Yeaaah create the adapter! Yeah!
        android.widget.SimpleExpandableListAdapter mAdapter = new SimpleExpandableListAdapterWithLinkify(
                this,
                groupData,
                R.layout.news_listitem1,
                new String[] { "TITLE" },
                new int[] { android.R.id.text1 },
                childData,
                R.layout.news_listitem2,
                new String[] { "DESCRIPTION" },
                new int[] { android.R.id.text1 }
        );
        elv.addHeaderView(newsHeader);
        elv.addFooterView(newsFooter);
        elv.setAdapter(mAdapter);
        elv.setGroupIndicator(null);
        news = new Dialog(this);
        //Set the title with the version number if possible
        try {
            news.setTitle(getResources().getString(R.string.version) + " " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch(Exception e) {
            news.setTitle(R.string.release_notes);
        }
        //Show it!
        news.addContentView(elv, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        news.show();
    }

    /** It is what it is: a SimpleExandableListAdapter that Linkify's all the items when adding them. */
    class SimpleExpandableListAdapterWithLinkify extends SimpleExpandableListAdapter
    {
        public SimpleExpandableListAdapterWithLinkify(Context context, List<? extends Map<String, ?>> groupData, int groupLayout, String[] groupFrom, int[] groupTo, List<? extends List<? extends Map<String, ?>>> childData, int childLayout, String[] childFrom, int[] childTo) {
            super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);
        }
        @Override
        public View getChildView (int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
        {
            convertView = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
            Linkify.addLinks((TextView)convertView, Linkify.ALL);
            return convertView;
        }
    }

    /** Create a BACON loading dialog */
    static public ProgressDialog BaconDialog(Context c, String title, String message)
    {
        if(message==null)
            message= c.getResources().getString(R.string.loading_msg);
        ProgressDialog pDialog = new ProgressDialog(c);
        pDialog.setCancelable(false);
        pDialog.setTitle(title);
        pDialog.setMessage(message);
        pDialog.setIcon(R.drawable.ic_action_gear);
        pDialog.show();

        //((View)LIVE_PreparedListener.baconPDialog.getWindow().findViewById(android.R.id.progress).getParent().getParent().getParent().getParent().getParent()).setBackgroundDrawable(RESOURCES.getDrawable(R.drawable.bacon_anim));
        ((View)(pDialog.getWindow().findViewById(android.R.id.progress)).getParent()).setBackgroundColor(0xFFFFFFFF);
        ((TextView)pDialog.getWindow().findViewById(android.R.id.message)).setTextColor(0xff000000);
        ((TextView)pDialog.getWindow().findViewById(android.R.id.message)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, (float) 17.0);
        ((View) pDialog.getWindow().findViewById(android.R.id.message)).setPadding(15, 5, 5, 5);
        ((ProgressBar) pDialog.getWindow().findViewById(android.R.id.progress)).setLayoutParams(new LayoutParams(96, 96));
        ((ProgressBar) pDialog.getWindow().findViewById(android.R.id.progress)).setIndeterminateDrawable(c.getResources().getDrawable(R.drawable.bacon_anim));
        return pDialog;
    }

    /** Converts a time in seconds to a human readable format.
     *  @param seconds The raw number of seconds to format.
     *  @return A string formatted in either HH:mm:ss or mm:ss, depending on how many hours are calculated.
     */
    public static String formatTimeFromSeconds(int seconds)
    {
        int minutes = seconds / 60;
        seconds %= 60;
        if(minutes>=60)
        {
            int hours = minutes / 60;
            minutes %= 60;
            return ((Integer.toString(hours) + ":" +
                    (minutes<10?"0":"") + Integer.toString(minutes) + ":" +
                    (seconds<10?"0":"") + Integer.toString(seconds)));
        }
        else
            return ((Integer.toString(minutes) + ":" +
                    (seconds<10?"0":"") + Integer.toString(seconds)));
    }
}
