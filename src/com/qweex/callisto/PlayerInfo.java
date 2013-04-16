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
        Log.v("PlayerInfo()", "Initializing PlayerInfo, queue size=" + Callisto.databaseConnector.queueCount());
        if(titleView!=null)
            titleView.setText(Callisto.RESOURCES.getString(R.string.queue_size) + ": " + Callisto.databaseConnector.queueCount());
    }

    /** Updates the player controls, like the title and times. Used excessively when changing Activities.
     * @param c The context for the current Activity.
     */
    public void update(Context c)
    {
        //Update the context for the receiver, unrelated
        CallistoService.audioJackReceiver.contextForPreferences = c;

        //If it's a widget there is no need to update the controls.
        if(Callisto.is_widget)
        {
            Callisto.is_widget = false;
            return;
        }

        Callisto.trackCompleted.setContext(c);

        //Retrieve the length & position, depending on if it is video or audio
        if(VideoActivity.videoView!=null)
        {
            length = VideoActivity.videoView.getDuration()/1000;
            position = VideoActivity.videoView.getCurrentPosition()/1000;
        }
        else if(Callisto.mplayer!=null)
        {
            length = Callisto.mplayer.getDuration()/1000;
            position = Callisto.mplayer.getCurrentPosition()/1000;
        }

        Log.v("*:update", "Update - Title: " + title);

        //titleView
        TextView titleView = (TextView) ((Activity)c).findViewById(R.id.titleBar);
        if(titleView==null)
            Log.w("Callisto:update", "Could not find view: " + "titleView");
        else
        if(title==null && Live.live_player==null)
            titleView.setText("Playlist size: " + Callisto.databaseConnector.queueCount());
        else if(Live.live_player==null)
            titleView.setText(title + " - " + show);
        else
            titleView.setText(title + " - JB Radio");

        //timeView
        Callisto.timeView = (TextView) ((Activity)c).findViewById(R.id.timeAt);
        if(Callisto.timeView==null)
            Log.w("Callisto:update", "Could not find view: " + "TimeView");
        else if(Live.live_player!=null)
        {
            Callisto.timeView.setText("Next ");
            Callisto.timeView.setEnabled(false);
        }
        else
        {
            Callisto.timeView.setText(Callisto.formatTimeFromSeconds(title == null ? 0 : position));
            Callisto.timeView.setEnabled(true);
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
        Callisto.timeProgress = (ProgressBar) ((Activity)c).findViewById(R.id.timeProgress);
        if(Callisto.timeProgress==null)
            Log.w("Callisto:update", "Could not find view: " + "timeProgress");
        else if(Live.live_player!=null)
        {
            Callisto.timeProgress.setMax(1);
            Callisto.timeProgress.setProgress(0);
            Callisto.timeProgress.setEnabled(false);
        }
        else
        {
            Callisto.timeProgress.setMax(length);
            Callisto.timeProgress.setProgress(title==null ? 0 : position);
            Callisto.timeProgress.setEnabled(true);
        }


        ImageButton play = (ImageButton) ((Activity)c).findViewById(R.id.playPause);
        if(play==null)
            Log.w("Callisto:update", "Could not find view: " + "playPause");
        else if(Live.live_player!=null)
        {
            if(Callisto.live_isPlaying)
                play.setImageDrawable(Callisto.pauseDrawable);
            else
                play.setImageDrawable(Callisto.playDrawable);
        } else
        {
            if(Callisto.playerInfo.isPaused)
                play.setImageDrawable(Callisto.playDrawable);
            else
                play.setImageDrawable(Callisto.pauseDrawable);
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
            if(Live.live_player!=null && Callisto.live_isPlaying)
            {
                if(i==CHECK_LIVE_EVERY)
                {
                    Live.LIVE_update = new Live_FetchInfo();
                    Live.LIVE_update.execute((Void [])null);
                    i=0;
                }
                return;
            }
            if((Callisto.mplayer==null || !Callisto.mplayer.isPlaying()) &&
                    (VideoActivity.videoView==null || !VideoActivity.videoView.isPlaying()))
            {
                i=0;
                return;
            }
            try {
                if(VideoActivity.videoView!=null)
                    Callisto.playerInfo.position = VideoActivity.videoView.getCurrentPosition();
                else
                    Callisto.playerInfo.position = Callisto.mplayer.getCurrentPosition();
                current = Callisto.playerInfo.position/1000;
                Callisto.timeProgress.setProgress(current);
                Callisto.timeView.setText(Callisto.formatTimeFromSeconds(current));
                Log.i("Callisto:TimerMethod", "Timer mon " + Callisto.playerInfo.position);

                Log.i("currentprogress", Queue.currentProgress + " !" + (Queue.currentProgress==null?"NULL":"NOTNULL"));
                if(Queue.currentProgress!=null)
                {
                    Log.i("currentprogress", current + "/" + Callisto.playerInfo.length);
                    double xy = (current*100.0) / Callisto.playerInfo.length;
                    Log.i("currentprogress", xy + " !");
                    Queue.currentProgress.setProgress((int)(Double.isNaN(xy) ? 0 : xy));
                }


                if(i==SAVE_POSITION_EVERY)
                {
                    i=0;
                    try {
                        Log.v("Callisto:TimerMethod", "Updating position: " + Callisto.playerInfo.position);
                        Cursor queue = Callisto.databaseConnector.currentQueueItem();
                        queue.moveToFirst();
                        Long identity = queue.getLong(queue.getColumnIndex("identity"));
                        Callisto.databaseConnector.updatePosition(identity, Callisto.playerInfo.position);
                    } catch(NullPointerException e)
                    {
                        Log.e("*:TimerRunnable", "NullPointerException when trying to update timer!");
                    }
                }

            } catch(Exception e)
            {
                System.out.println("HERP!!!");
            }
        }
    };

    static public Handler updateHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(Live.LIVE_PreparedListener.c!=null)
                Callisto.playerInfo.update(Live.LIVE_PreparedListener.c);
        }
    };
}
