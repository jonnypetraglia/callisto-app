/*
 * Copyright (C) 2012-2014 Qweex
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.view.*;
import android.widget.*;
import com.qweex.callisto.irc.IRCChat;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/** A class to view live video and back-catalog video.
 * Extends IRCChat because (ideally) it will have IRC display either beside or below the video.
 */
public class VideoActivity extends IRCChat
{
    /** VideoView to contain the video, Duh. */
    static VideoView videoView;
    /* The login info for the IRC */
    View login;
    int seekto;
    AlertDialog d;

    public void onCreate(Bundle savedInstanceState)
    {
        String TAG = StaticBlob.TAG();
        if(android.os.Build.VERSION.SDK_INT >= 11)
            setTheme(R.style.Default_New);
        //Do some create things
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.video);
        StaticBlob.playerInfo.update(this);
        boolean isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
        if(isLandscape)
            ((LinearLayout)findViewById(R.id.mainVideo)).setOrientation(LinearLayout.HORIZONTAL);

        videoView = new VideoView(this);
        videoView.setId(R.id.videoView);
        setContentView(videoView);
        videoView.getLayoutParams().width= LinearLayout.LayoutParams.FILL_PARENT;
        videoView.getLayoutParams().height= LinearLayout.LayoutParams.FILL_PARENT;
        //Create the IRC stuff
        /*
        if(IRCChat.session==null)
        {
            login = ((LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.irc_login, null, false);
            login.setTag("VideoLogin");
            login.setPadding(getWindowManager().getDefaultDisplay().getHeight()/25,
                    25,
                    getWindowManager().getDefaultDisplay().getHeight()/10,
                    25);
            ((LinearLayout)findViewById(R.id.mainVideo)).addView(login);
            findViewById(R.id.videoIrc).setVisibility(View.GONE);

            findViewById(R.id.login).setOnClickListener(InitiateLogin);
        }
        else
        {
            try {
                //Remove existing parent & add it for chat
                if(StaticBlob.chatView.getParent()!=null)
                    ((ScrollView) StaticBlob.chatView.getParent()).removeView(StaticBlob.chatView);
                ((ScrollView) findViewById(R.id.scrollView)).addView(StaticBlob.chatView);
                //Remove existing parent & add it for log
                if(StaticBlob.logView.getParent()!=null)
                    ((ScrollView) StaticBlob.logView.getParent()).removeView(StaticBlob.logView);
                ((ScrollView) findViewById(R.id.scrollView2)).addView(StaticBlob.logView);
            } catch(Exception e) {}
        }
        */
        videoView = (VideoView) findViewById(R.id.videoView);

        Log.d(TAG, " " + videoView);
        // Getting the path to the video (either URL or local path)
        Bundle b = getIntent().getExtras();
        if(b==null)
        {
            Toast.makeText(this, "Unable to get the target video to load. Please don't hate me. :(", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String path = b.getString("uri");
        seekto = b.getInt("seek");
        if(path==null)
        {
            Toast.makeText(this, "Unable to get the target path to load. Please don't hate me. :(", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            d = Callisto.BaconDialog(this,"Loading, bro", "Dude, loading, bro. Give it a sec.");
            d.setCancelable(true);
            d.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            });
            d.show();
            Log.d(TAG, "URI: " + path);
            Uri pathToVideo = Uri.parse(path);
            videoView.setVideoURI(pathToVideo);

            // to start it
            videoView.requestFocus();
            videoView.setMediaController(new MediaController(this));
            videoView.start();
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    String TAG = StaticBlob.TAG();
                    Log.d(TAG, "Seeking to " + seekto + "/" + videoView.getDuration());
                    if(seekto<videoView.getDuration())
                        videoView.seekTo(seekto);
                    d.hide();
                }
            });
        } catch(Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "There was a problem loading the video. Please don't hate me. :(", Toast.LENGTH_SHORT).show();
            finish();
        }

        return;
        // to check if it is still playing
//        boolean isPlaying = videoView.isPlaying();
//
//        // to stop it
//        videoView.stopPlayback();
//        videoView.clearFocus();
    }

    //TODO: Remove this once we get chat working
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case NICKLIST_ID:
                //TODO: show the nicklist
                //Need to show popup window instead of creating activity for result
                return true;
            case CHANGE_ID:
                //TODO: WHAT
                if("Open IRC".equals(item.getTitle().toString()))
                {
                    //Show the IRC controls
                    actuallyConnect();
                }
                else
                    return super.onOptionsItemSelected(item);
                return true;
            case LOGOUT_ID:
                //Hide the IRC controls
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        findViewById(R.id.videoView).getLayoutParams().width= LinearLayout.LayoutParams.FILL_PARENT;
        findViewById(R.id.videoView).getLayoutParams().height= LinearLayout.LayoutParams.FILL_PARENT;
//        if(getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight())
//            ((LinearLayout)findViewById(R.id.mainVideo)).setOrientation(LinearLayout.HORIZONTAL);
//        else
//            ((LinearLayout)findViewById(R.id.mainVideo)).setOrientation(LinearLayout.VERTICAL);
    }

    /** Called when the activity is destroyed; set the videoView to null so the time updater knows that it's not playing. */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        videoView=null;
        PlayerControls.stop(this);
    }
}