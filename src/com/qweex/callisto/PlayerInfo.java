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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.qweex.callisto.podcast.Queue;

import java.util.Timer;
import java.util.TimerTask;

/** Essentially a struct with a few functions to handle the player, particularly updating it when switching activities. */
public class PlayerInfo
{
    /** A reference to the MediaPlayer currently in use; either the main one or the live one. **/
    public MediaPlayer player;
    /** Data of the currently playing track **/
    public String title, show, date;
    /** Track information of the currently playing track, measured in seconds **/
    public int position = 0, length = 0;
    /** Holds whether or not the player is paused. **/
    public boolean isPaused = true;
    private Callisto callisto;
    //TODO: wtf
    private Timer timeTimer = null;
    //TODO: wtf
    public static TimerRunnable clock;
    public static final int SAVE_POSITION_EVERY = 40,
            CHECK_LIVE_EVERY = 400;	//Cycles, not necessarily seconds
    //TODO: wtf
    static int current;

    /** Constructor for the PlayerInfo class. Good to call when first creating the player controls, to set then to something.
     *  @param c The context for the current Activity.
     */
    public PlayerInfo(Callisto callisto, Context c)
    {
        this.callisto = callisto;
        TextView titleView = (TextView) ((Activity)c).findViewById(R.id.titleBar);
        Log.v("PlayerInfo()", "Initializing PlayerInfo, queue size=" + StaticBlob.databaseConnector.queueCount());
        if(titleView!=null)
            titleView.setText(c.getResources().getString(R.string.queue_size) + ": " + StaticBlob.databaseConnector.queueCount());
    }

    /** Updates the player controls, like the title and times. Used excessively when changing Activities.
     * @param c The context for the current Activity.
     */
    public void update(Context c)
    {
        //Update the context for the receiver, unrelated
        CallistoService.audioJackReceiver.contextForPreferences = c;

        //If it's a widget there is no need to update the controls.
        if(StaticBlob.is_widget)
        {
            StaticBlob.is_widget = false;
            return;
        }

        StaticBlob.trackCompleted.setContext(c);

        //Retrieve the length & position, depending on if it is video or audio
        if(VideoActivity.videoView!=null)
        {
            length = VideoActivity.videoView.getDuration()/1000;
            position = VideoActivity.videoView.getCurrentPosition()/1000;
            Log.v("*:update", "Length=" + length + " | Position=" + position);
        }
        else if(StaticBlob.mplayer!=null)
        {
            length = StaticBlob.mplayer.getDuration()/1000;
            position = StaticBlob.mplayer.getCurrentPosition()/1000;
        }

        Log.v("*:update", "Update - Title: " + title);

        //titleView
        TextView titleView = (TextView) ((Activity)c).findViewById(R.id.titleBar);
        if(titleView==null)
            Log.w("Callisto:update", "Could not find view: " + "titleView");
        else
        if(title==null && Live.live_player==null)
            titleView.setText("Playlist size: " + StaticBlob.databaseConnector.queueCount());
        else if(Live.live_player==null)
            titleView.setText(title + " - " + show);
        else
            titleView.setText(title + " - JB Radio");

        //timeView
        StaticBlob.timeView = (TextView) ((Activity)c).findViewById(R.id.timeAt);
        if(StaticBlob.timeView==null)
            Log.w("Callisto:update", "Could not find view: " + "TimeView");
        else if(Live.live_player!=null)
        {
            StaticBlob.timeView.setText("Next ");
            StaticBlob.timeView.setEnabled(false);
        }
        else
        {
            StaticBlob.timeView.setText(Callisto.formatTimeFromSeconds(title == null ? 0 : position));
            StaticBlob.timeView.setEnabled(true);
        }

        //lengthView
        TextView lengthView = (TextView) ((Activity)c).findViewById(R.id.length);
        if(lengthView==null)
            Log.w("Callisto:update", "Could not find view: " + "lengthView");
        else if(Live.live_player!=null)
        {
            lengthView.setText(show);
            lengthView.setEnabled(false);
        }
        else
        {
            lengthView.setText(Callisto.formatTimeFromSeconds(title == null ? 0 : length));
            lengthView.setEnabled(true);
        }

        //timeProgress
        StaticBlob.timeProgress = (ProgressBar) ((Activity)c).findViewById(R.id.timeProgress);
        if(StaticBlob.timeProgress==null)
            Log.w("Callisto:update", "Could not find view: " + "timeProgress");
        else if(Live.live_player!=null)
        {
            StaticBlob.timeProgress.setMax(1);
            StaticBlob.timeProgress.setProgress(0);
            StaticBlob.timeProgress.setEnabled(false);
        }
        else
        {
            StaticBlob.timeProgress.setMax(length);
            StaticBlob.timeProgress.setProgress(title==null ? 0 : position);
            StaticBlob.timeProgress.setEnabled(true);
        }


        ImageButton play = (ImageButton) ((Activity)c).findViewById(R.id.playPause);
        if(play==null)
            Log.w("Callisto:update", "Could not find view: " + "playPause");
        else if(Live.live_player!=null)
        {
            if(StaticBlob.live_isPlaying)
                play.setImageDrawable(StaticBlob.pauseDrawable);
            else
                play.setImageDrawable(StaticBlob.playDrawable);
        } else
        {
            if(StaticBlob.playerInfo.isPaused)
                play.setImageDrawable(StaticBlob.playDrawable);
            else
                play.setImageDrawable(StaticBlob.pauseDrawable);
        }

        //Disables views if it is live
        if(((Activity)c).findViewById(R.id.seek)!=null)
            ((Activity)c).findViewById(R.id.seek).setEnabled(Live.live_player==null);
        if(((Activity)c).findViewById(R.id.previous)!=null)
            ((Activity)c).findViewById(R.id.previous).setEnabled(Live.live_player==null);
        if(((Activity)c).findViewById(R.id.next)!=null)
            ((Activity)c).findViewById(R.id.next).setEnabled(Live.live_player==null);

        if(timeTimer==null)
        {
            Log.i("Callisto:PlayerInfo:update","Starting timer");
            timeTimer = new Timer();
            timeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    TimerMethod();
                }
            }, 0, 250);
        }
    }

    /** Clears all internal values to their respective blank values (i.e. null or 0). */
    public void clear()
    {
        title = show = date = null;
        position = length = 0;
        isPaused = true;
    }


    /** A simple menthod to run TimerRunnable in the UI Thread to allow it to update Views.*/
    private void TimerMethod()
    {
        if(clock==null)
            clock = new TimerRunnable();
        TimerHandler.post(clock);
//		Callisto.this.runOnUiThread(TimerRunnable);
    }

    private Handler TimerHandler = new Handler();

    /** A runnable to be used in conjunction with the update() function. Updates the player time every set amount of time. */
    class TimerRunnable implements Runnable
            //Runnable TimerRunnable = new Runnable()
    {
        public int i=0;
        public void run()
        {
            i++;
            if(Live.live_player!=null && StaticBlob.live_isPlaying)
            {
                if(i>=CHECK_LIVE_EVERY)
                {
                    Live.LiveUpdate = new Live.FetchInfo();
                    Live.LiveUpdate.execute((Void [])null);
                    i=0;
                }
                return;
            }
            if((StaticBlob.mplayer==null || !StaticBlob.mplayer.isPlaying()) &&
                    (VideoActivity.videoView==null || !VideoActivity.videoView.isPlaying()))
            {
                i=0;
                return;
            }
            try {
                current = StaticBlob.playerInfo.position/1000;
                if(VideoActivity.videoView!=null)
                    StaticBlob.playerInfo.position = VideoActivity.videoView.getCurrentPosition();
                else
                {
                    StaticBlob.playerInfo.position = StaticBlob.mplayer.getCurrentPosition();
                    StaticBlob.timeProgress.setProgress(current);
                    StaticBlob.timeView.setText(Callisto.formatTimeFromSeconds(current));
                    if(PlayerControls.currentTime!=null)
                    {
                        PlayerControls.currentTime.setText(Callisto.formatTimeFromSeconds(current));
                        android.widget.SeekBar sb = (android.widget.SeekBar)
                                                    ((android.view.View) PlayerControls.currentTime.getParent().getParent())
                                                            .findViewById(R.id.seekBar);
                        sb.setProgress(current);
                    }
                }

                if(Queue.currentProgress!=null)
                {
                    double xy = (current*100.0) / StaticBlob.playerInfo.length;
                    Queue.currentProgress.setProgress((int)(Double.isNaN(xy) ? 0 : xy));
                }


                if(i==SAVE_POSITION_EVERY)
                {
                    i=0;
                    try {
                        Log.v("Callisto:TimerMethod", "Updating position: " + StaticBlob.playerInfo.position);
                        Cursor queue = StaticBlob.databaseConnector.currentQueueItem();
                        queue.moveToFirst();
                        Long identity = queue.getLong(queue.getColumnIndex("identity"));
                        StaticBlob.databaseConnector.updatePosition(identity, StaticBlob.playerInfo.position);
                    } catch(NullPointerException e)
                    {
                        Log.e("*:TimerRunnable", "NullPointerException when trying to update timer!");
                    }
                }

            } catch(Exception e)
            {
                Log.e("PlayerInfo:TimerRunnable","Error whilst trying to update time: " + e.getClass());
            }
        }
    };

    static public Handler updateHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(Live.LIVE_PreparedListener.c!=null)
                StaticBlob.playerInfo.update(Live.LIVE_PreparedListener.c);
        }
    };
}
