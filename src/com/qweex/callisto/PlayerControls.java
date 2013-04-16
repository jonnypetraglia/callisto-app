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

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;
import com.qweex.callisto.podcast.EpisodeDesc;
import com.qweex.callisto.podcast.Queue;
import com.qweex.callisto.widgets.CallistoWidget;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * Contains a bunch of listeners and functions to deal with the player controls.
 */
public class PlayerControls
{
    /** Listener for the Next (">") button. Proceeds to the next track, if there is one. */
    public static View.OnClickListener next = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            changeToTrack(v.getContext(), 1, !StaticBlob.playerInfo.isPaused);
        }
    };
    /** Listener for the Previous ("<") button. Goes back to the previous track, if there is one. */
    public static View.OnClickListener previous = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            changeToTrack(v.getContext(), -1, !StaticBlob.playerInfo.isPaused);
        }
    };
    /** Listener for the playlist button; displays the queue. */
    public static View.OnClickListener playlist = new View.OnClickListener()
    {
        View psychV;
        @Override
        public void onClick(View v)
        {
            if(Live.live_player!=null)
            {
                psychV = v;
                Dialog dg = new AlertDialog.Builder(v.getContext())
                        .setTitle("Switch from live back to playlist?")
                        .setPositiveButton("Yup", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Live.live_player.reset();
                                Live.live_player = null;
                                if(StaticBlob.Live_wifiLock!=null && !StaticBlob.Live_wifiLock.isHeld())
                                    StaticBlob.Live_wifiLock.release();
                                StaticBlob.playerInfo.update(psychV.getContext());
                                StaticBlob.mNotificationManager.cancel(StaticBlob.NOTIFICATION_ID);
                            }
                        })
                        .setNegativeButton("Nope", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
                dg.show();
            }
            else {
                Intent newIntent = new Intent(v.getContext(), Queue.class);
                v.getContext().startActivity(newIntent);
            }
        }
    };
    /** Listener for play/pause button; calls playPause(), the function */
    public static View.OnClickListener playPauseListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            playPause(v.getContext(), v);
            CallistoService.audioJackReceiver.wasPausedByThisReceiver = false;
        }
    };
    /** Listener for the seek button; displays a dialog that allows the user to seek to a point in the episode. */
    public static View.OnClickListener seekDialog = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if(StaticBlob.mplayer==null)
                return;

            SeekBar sb = new SeekBar(v.getContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext()).setView(sb);
            final AlertDialog alertDialog = builder.create();

            alertDialog.setTitle(StaticBlob.RESOURCES.getString(R.string.seek_title));
            alertDialog.setMessage(Callisto.formatTimeFromSeconds(StaticBlob.mplayer.getCurrentPosition() / 1000) + "/" + Callisto.formatTimeFromSeconds(StaticBlob.playerInfo.length));
            sb.setMax(StaticBlob.playerInfo.length);
            sb.setProgress(StaticBlob.mplayer.getCurrentPosition()/1000);

            alertDialog.setButton(StaticBlob.RESOURCES.getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                }
            });//*/
            alertDialog.show();
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                    alertDialog.setMessage(Callisto.formatTimeFromSeconds(progress) + "/" + Callisto.formatTimeFromSeconds(StaticBlob.playerInfo.length));
                }
                @Override
                public void onStartTrackingTouch(SeekBar arg0) {}
                @Override

                public void onStopTrackingTouch(SeekBar arg0) {
                    StaticBlob.mplayer.seekTo(arg0.getProgress()*1000);
                    StaticBlob.playerInfo.clock.i=PlayerInfo.SAVE_POSITION_EVERY;
                }
            });
        }
    };

    /** Changes position in queue to another song and optionally starts playing it.
     * @param c The context of the current activity.
     * @param previousOrNext >0 if it should play the next track, <0 for the previous, and 0 for the current
     * @param sp true if the player should start playing when it changes tracks, false otherwise
     */
    public static void changeToTrack(Context c, int previousOrNext, boolean sp)
    {
        Cursor queue = StaticBlob.databaseConnector.advanceQueue(previousOrNext);

        //If there are no items in the queue, stop the player
        if(queue==null || queue.getCount()==0)
        {
            Log.v("*:changeToTrack", "Queue is empty. Pausing.");
            ImageButton x = (ImageButton) ((Activity)c).findViewById(R.id.playPause);
            if(x!=null)
                x.setImageDrawable(StaticBlob.pauseDrawable);
            StaticBlob.mNotificationManager.cancel(StaticBlob.NOTIFICATION_ID);
            stop(c);
            return;
        }

        Log.v("*:changeToTrack", "Queue Size: " + queue.getCount());
        queue.moveToFirst();
        //The queue merely stores the identity (_id) of the entrie's position in the main SQL
        //After obtaining it, we can get all the information about it
        Long id = queue.getLong(queue.getColumnIndex("_id"));
        Long identity = queue.getLong(queue.getColumnIndex("identity"));
        boolean isStreaming = queue.getInt(queue.getColumnIndex("streaming"))>0;
        boolean isVideo = queue.getInt(queue.getColumnIndex("video"))>0;
        Cursor theTargetTrack = StaticBlob.databaseConnector.getOneEpisode(identity);
        theTargetTrack.moveToFirst();

        //Retrieve all of the playerInfo about the new track
        String media_location;
        StaticBlob.playerInfo.title = theTargetTrack.getString(theTargetTrack.getColumnIndex("title"));
        StaticBlob.playerInfo.position = theTargetTrack.getInt(theTargetTrack.getColumnIndex("position"));
        StaticBlob.playerInfo.date = theTargetTrack.getString(theTargetTrack.getColumnIndex("date"));
        StaticBlob.playerInfo.show = theTargetTrack.getString(theTargetTrack.getColumnIndex("show"));
        Log.i("*:changeToTrack", "Loading info: " + StaticBlob.playerInfo.title);
        //Retrieve the location of the new track
        if(isStreaming)
        {
            media_location = theTargetTrack.getString(theTargetTrack.getColumnIndex(isVideo?"vidlink":"mp3link"));
        }
        else
        {
            //Get the date from the info retrieved from SQL. If it is malformed we have a SERIOUS problem and must halt.
            try {
                StaticBlob.playerInfo.date = StaticBlob.sdfFile.format(StaticBlob.sdfRaw.parse(StaticBlob.playerInfo.date));
            } catch (ParseException e) {
                Log.e("*changeToTrack:ParseException", "Error parsing a date from the SQLite db:");
                Log.e("*changeToTrack:ParseException", StaticBlob.playerInfo.date);
                Log.e("*changeToTrack:ParseException", "(This should never happen).");
                e.printStackTrace();
                Toast.makeText(c, StaticBlob.RESOURCES.getString(R.string.queue_error), Toast.LENGTH_SHORT).show();
                StaticBlob.databaseConnector.deleteQueueItem(id);
                return;
            }

            //Here we actually get the path
            File target = new File(Environment.getExternalStorageDirectory(), StaticBlob.storage_path + File.separator + StaticBlob.playerInfo.show);
            target = new File(target, StaticBlob.playerInfo.date + "__" + StaticBlob.playerInfo.title +
                    EpisodeDesc.getExtension(theTargetTrack.getString(theTargetTrack.getColumnIndex(isVideo ? "vidlink" : "mp3link"))));
            //If it doesn't exist, we must halt.
            if(!target.exists())
            {
                if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                {
                    new AlertDialog.Builder(c)
                            .setTitle("No SD Card")
                            .setMessage("There is currently no external storage to write to.")
                            .setNegativeButton("Ok",null)
                            .create().show();
                    return;
                }
                Log.e("*:changeToTrack", "File not found: " + target.getPath());
                Toast.makeText(c, StaticBlob.RESOURCES.getString(R.string.queue_error), Toast.LENGTH_SHORT).show();;
                StaticBlob.databaseConnector.deleteQueueItem(id);
                return;
            }
            //Here we FINALLY get the path.
            media_location = target.getPath();
        }

        //Create the notification for this new track
        //TODO: must we create the intents and whatnot every time?
        Intent notificationIntent = new Intent(c, EpisodeDesc.class);
        notificationIntent.putExtra("id", identity);
        PendingIntent contentIntent = PendingIntent.getActivity(c, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        StaticBlob.notification_playing = new Notification(R.drawable.callisto, null, System.currentTimeMillis());
        StaticBlob.notification_playing.flags = Notification.FLAG_ONGOING_EVENT;
        StaticBlob.notification_playing.setLatestEventInfo(c,  StaticBlob.playerInfo.title,  StaticBlob.playerInfo.show, contentIntent);
        StaticBlob.mNotificationManager.notify(StaticBlob.NOTIFICATION_ID, StaticBlob.notification_playing);


        //Here is where we FINALLY actually play the track.
        if(isVideo)
        {
            Log.i("*:changeToTrack", "New track is a video, creating intent");
            Intent intent= new Intent(c, VideoActivity.class);
            intent.putExtra("uri", media_location);
            intent.putExtra("seek", StaticBlob.playerInfo.position);
            StaticBlob.playerInfo.update(c);
            c.startActivity(intent);
            return;
        }
        try {
            Log.i("*:changeToTrack", "New track is an audio, doing MediaPlayer things");
            if(StaticBlob.mplayer==null)
                StaticBlob.mplayer = new MediaPlayer(); //This could be a problem
            StaticBlob.mplayer.reset();
            StaticBlob.mplayerPrepared.setContext(c);
            StaticBlob.mplayer.setDataSource(media_location);
            Log.i("*:changeToTrack", "Setting source: " + media_location);
            StaticBlob.mplayer.setOnCompletionListener(StaticBlob.trackCompleted);
            StaticBlob.mplayer.setOnErrorListener(StaticBlob.trackCompletedBug);
            StaticBlob.mplayerPrepared.startPlaying = sp;
            StaticBlob.mplayer.setOnPreparedListener(StaticBlob.mplayerPrepared);
            Log.i("*:changeToTrack", "Preparing..." + sp);
            //Streaming requires a dialog
            if(isStreaming)
            {
                StaticBlob.mplayerPrepared.pd = ProgressDialog.show(c, StaticBlob.RESOURCES.getString(R.string.loading), StaticBlob.RESOURCES.getString(R.string.loading_msg), true, false);
                StaticBlob.mplayerPrepared.pd.setCancelable(true);
                StaticBlob.mplayerPrepared.pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        StaticBlob.mplayer.stop();
                        dialog.cancel();
                    }
                });
            }
            StaticBlob.mplayer.prepareAsync();
            //Picks up at the mplayerPrepared listener
        } catch (IllegalArgumentException e) {
            Log.e("*changeToTrack:IllegalArgumentException", "Error attempting to set the data path for MediaPlayer:");
            Log.e("*changeToTrack:IllegalArgumentException", media_location);
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e("*changeToTrack:IllegalStateException", "Error in State for MediaPlayer:");
        } catch (IOException e) {
            Log.e("*changeToTrack:IOException", "IO is another of Jupiter's moons. Did you know that?");
            e.printStackTrace();
        }

        //Update the widgets
        CallistoWidget.updateAllWidgets(c);
    }

    /** Plays or pauses the currently playing track; does not adjust what is the current track **/
    public static void playPause(Context c, View v)
    {
        //-----Live-----
        String live_url = "";
        try {
            live_url = PreferenceManager.getDefaultSharedPreferences(c).getString("live_url", "http://jbradio.out.airtime.pro:8000/jbradio_b");
        } catch(NullPointerException e) {}
        if(Live.live_player!=null)
        {
            if(StaticBlob.live_isPlaying)
            {
                Live.live_player.stop();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(StaticBlob.playDrawable);
            }
            else
            {
                //1. liveInit
                //2. setOnPreparedListener
                //3. setDataSource
                //4. livePrepare
                try {
                    Live.LIVE_Init();
                    Live.live_player.setOnPreparedListener(Live.LIVE_PreparedListener);
                    Live.LIVE_PreparedListener.setContext(c);
                    Live.live_player.setDataSource(live_url);
                    Live.LIVE_Prepare(null);
                    if(v!=null)
                        ((ImageButton)v).setImageDrawable(StaticBlob.pauseDrawable);
                } catch(Exception e){}
            }
            StaticBlob.live_isPlaying = !StaticBlob.live_isPlaying;
            CallistoWidget.updateAllWidgets(c);
            return;
        }

        //-----Local-----
        if(StaticBlob.databaseConnector==null || StaticBlob.databaseConnector.queueCount()==0)
            return;
        if(StaticBlob.mplayer==null)
        {
            Log.d("*:playPause","PlayPause initiated");
            StaticBlob.mplayer = new MediaPlayer();
            StaticBlob.mplayer.setOnCompletionListener(StaticBlob.trackCompleted);
            StaticBlob.mplayer.setOnErrorListener(StaticBlob.trackCompletedBug);
            changeToTrack(c, 0, true);
        }
        else
        {
            Log.d("*:playPause","PlayPause is " + (StaticBlob.playerInfo.isPaused ? "" : "NOT") + "paused");
            if(StaticBlob.playerInfo.isPaused)
            {
                StaticBlob.mplayer.start();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(StaticBlob.pauseDrawable);
            }
            else
            {
                StaticBlob.mplayer.pause();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(StaticBlob.playDrawable);
            }
            StaticBlob.playerInfo.isPaused = !StaticBlob.playerInfo.isPaused;
        }
        CallistoWidget.updateAllWidgets(c);
    }

    /** Stops the player. Clears the notifications, releases resources, etc. */
    public static void stop(Context c)
    {
        if(StaticBlob.playerInfo!=null)
            StaticBlob.playerInfo.clear();
        if(StaticBlob.mplayer!=null) {
            StaticBlob.mplayer.reset();
            StaticBlob.mplayer = null;
        }
        if(Live.live_player!=null) {
            Live.live_player.reset();
            Live.live_player = null;
        }
        StaticBlob.playerInfo.title = null;
        StaticBlob.playerInfo.update(c);
        StaticBlob.mNotificationManager.cancel(StaticBlob.NOTIFICATION_ID);
        CallistoWidget.updateAllWidgets(c);
    }
}
