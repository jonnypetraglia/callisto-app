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

import android.content.Context;
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

    public void onCreate(Bundle savedInstanceState)
    {
        //Do some create things
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);
        boolean isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
        if(isLandscape)
            ((LinearLayout)findViewById(R.id.mainVideo)).setOrientation(LinearLayout.HORIZONTAL);

        //Create the IRC stuff
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

        videoView = (VideoView) findViewById(R.id.videoView);

        // Getting the path to the video (either URL or local path)
        Bundle b = getIntent().getExtras();
        if(b==null)
        {
            finish();
            return;
        }
        String path = b.getString("uri");
        int seekto = b.getInt("seek");
        if(path==null)
        {
            finish();
            return;
        }

        try {
            Log.d("VideoActivity:onCreate", path);
            Uri pathToVideo = Uri.parse(path);
            videoView.setVideoURI(pathToVideo);

            // to start it
            videoView.requestFocus();
            videoView.setMediaController(new MediaController(this));
            Log.d("VideoActivity:onCreate", "Seeking to " + seekto);
            //if(seekto<videoView.getDuration())
            //    videoView.seekTo(seekto);
            videoView.start();
        } catch(Exception e)
        {
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
    }
}