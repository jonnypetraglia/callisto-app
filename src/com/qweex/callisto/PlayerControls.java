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
 * Created with IntelliJ IDEA.
 * User: notbryant
 * Date: 4/15/13
 * Time: 9:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayerControls
{
    /** Listener for the Next (">") button. Proceeds to the next track, if there is one. */
    public static View.OnClickListener next = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            changeToTrack(v.getContext(), 1, !Callisto.playerInfo.isPaused);
        }
    };
    /** Listener for the Previous ("<") button. Goes back to the previous track, if there is one. */
    public static View.OnClickListener previous = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            changeToTrack(v.getContext(), -1, !Callisto.playerInfo.isPaused);
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
                                if(Callisto.Live_wifiLock!=null && !Callisto.Live_wifiLock.isHeld())
                                    Callisto.Live_wifiLock.release();
                                Callisto.playerInfo.update(psychV.getContext());
                                Callisto.mNotificationManager.cancel(Callisto.NOTIFICATION_ID);
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
            if(Callisto.mplayer==null)
                return;

            SeekBar sb = new SeekBar(v.getContext());
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext()).setView(sb);
            final AlertDialog alertDialog = builder.create();

            alertDialog.setTitle(Callisto.RESOURCES.getString(R.string.seek_title));
            alertDialog.setMessage(Callisto.formatTimeFromSeconds(Callisto.mplayer.getCurrentPosition() / 1000) + "/" + Callisto.formatTimeFromSeconds(Callisto.playerInfo.length));
            sb.setMax(Callisto.playerInfo.length);
            sb.setProgress(Callisto.mplayer.getCurrentPosition()/1000);

            alertDialog.setButton(Callisto.RESOURCES.getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                }
            });//*/
            alertDialog.show();
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                    alertDialog.setMessage(Callisto.formatTimeFromSeconds(progress) + "/" + Callisto.formatTimeFromSeconds(Callisto.playerInfo.length));
                }
                @Override
                public void onStartTrackingTouch(SeekBar arg0) {}
                @Override

                public void onStopTrackingTouch(SeekBar arg0) {
                    Callisto.mplayer.seekTo(arg0.getProgress()*1000);
                    Callisto.playerInfo.clock.i=PlayerInfo.SAVE_POSITION_EVERY;
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
        Cursor queue = Callisto.databaseConnector.advanceQueue(previousOrNext);

        //If there are no items in the queue, stop the player
        if(queue==null || queue.getCount()==0)
        {
            Log.v("*:changeToTrack", "Queue is empty. Pausing.");
            ImageButton x = (ImageButton) ((Activity)c).findViewById(R.id.playPause);
            if(x!=null)
                x.setImageDrawable(Callisto.pauseDrawable);
            Callisto.mNotificationManager.cancel(Callisto.NOTIFICATION_ID);
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
        Cursor theTargetTrack = Callisto.databaseConnector.getOneEpisode(identity);
        theTargetTrack.moveToFirst();

        //Retrieve all of the playerInfo about the new track
        String media_location;
        Callisto.playerInfo.title = theTargetTrack.getString(theTargetTrack.getColumnIndex("title"));
        Callisto.playerInfo.position = theTargetTrack.getInt(theTargetTrack.getColumnIndex("position"));
        Callisto.playerInfo.date = theTargetTrack.getString(theTargetTrack.getColumnIndex("date"));
        Callisto.playerInfo.show = theTargetTrack.getString(theTargetTrack.getColumnIndex("show"));
        Log.i("*:changeToTrack", "Loading info: " + Callisto.playerInfo.title);
        //Retrieve the location of the new track
        if(isStreaming)
        {
            media_location = theTargetTrack.getString(theTargetTrack.getColumnIndex(isVideo?"vidlink":"mp3link"));
        }
        else
        {
            //Get the date from the info retrieved from SQL. If it is malformed we have a SERIOUS problem and must halt.
            try {
                Callisto.playerInfo.date = Callisto.sdfFile.format(Callisto.sdfRaw.parse(Callisto.playerInfo.date));
            } catch (ParseException e) {
                Log.e("*changeToTrack:ParseException", "Error parsing a date from the SQLite db:");
                Log.e("*changeToTrack:ParseException", Callisto.playerInfo.date);
                Log.e("*changeToTrack:ParseException", "(This should never happen).");
                e.printStackTrace();
                Toast.makeText(c, Callisto.RESOURCES.getString(R.string.queue_error), Toast.LENGTH_SHORT).show();
                Callisto.databaseConnector.deleteQueueItem(id);
                return;
            }

            //Here we actually get the path
            File target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + Callisto.playerInfo.show);
            target = new File(target,Callisto.playerInfo.date + "__" + Callisto.playerInfo.title +
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
                Toast.makeText(c, Callisto.RESOURCES.getString(R.string.queue_error), Toast.LENGTH_SHORT).show();;
                Callisto.databaseConnector.deleteQueueItem(id);
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
        Callisto.notification_playing = new Notification(R.drawable.callisto, null, System.currentTimeMillis());
        Callisto.notification_playing.flags = Notification.FLAG_ONGOING_EVENT;
        Callisto.notification_playing.setLatestEventInfo(c,  Callisto.playerInfo.title,  Callisto.playerInfo.show, contentIntent);
        Callisto.mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_playing);


        //Here is where we FINALLY actually play the track.
        if(isVideo)
        {
            Log.i("*:changeToTrack", "New track is a video, creating intent");
            Intent intent= new Intent(c, VideoActivity.class);
            intent.putExtra("uri", media_location);
            intent.putExtra("seek", Callisto.playerInfo.position);
            Callisto.playerInfo.update(c);
            c.startActivity(intent);
            return;
        }
        try {
            Log.i("*:changeToTrack", "New track is an audio, doing MediaPlayer things");
            if(Callisto.mplayer==null)
                Callisto.mplayer = new MediaPlayer(); //This could be a problem
            Callisto.mplayer.reset();
            Callisto.mplayerPrepared.setContext(c);
            Callisto.mplayer.setDataSource(media_location);
            Log.i("*:changeToTrack", "Setting source: " + media_location);
            Callisto.mplayer.setOnCompletionListener(Callisto.trackCompleted);
            Callisto.mplayer.setOnErrorListener(Callisto.trackCompletedBug);
            Callisto.mplayerPrepared.startPlaying = sp;
            Callisto.mplayer.setOnPreparedListener(Callisto.mplayerPrepared);
            Log.i("*:changeToTrack", "Preparing..." + sp);
            //Streaming requires a dialog
            if(isStreaming)
            {
                Callisto.mplayerPrepared.pd = ProgressDialog.show(c, Callisto.RESOURCES.getString(R.string.loading), Callisto.RESOURCES.getString(R.string.loading_msg), true, false);
                Callisto.mplayerPrepared.pd.setCancelable(true);
                Callisto.mplayerPrepared.pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Callisto.mplayer.stop();
                        dialog.cancel();
                    }
                });
            }
            Callisto.mplayer.prepareAsync();
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
            if(Callisto.live_isPlaying)
            {
                Live.live_player.stop();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(Callisto.playDrawable);
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
                        ((ImageButton)v).setImageDrawable(Callisto.pauseDrawable);
                } catch(Exception e){}
            }
            Callisto.live_isPlaying = !Callisto.live_isPlaying;
            CallistoWidget.updateAllWidgets(c);
            return;
        }

        //-----Local-----
        if(Callisto.databaseConnector==null || Callisto.databaseConnector.queueCount()==0)
            return;
        if(Callisto.mplayer==null)
        {
            Log.d("*:playPause","PlayPause initiated");
            Callisto.mplayer = new MediaPlayer();
            Callisto.mplayer.setOnCompletionListener(Callisto.trackCompleted);
            Callisto.mplayer.setOnErrorListener(Callisto.trackCompletedBug);
            changeToTrack(c, 0, true);
        }
        else
        {
            Log.d("*:playPause","PlayPause is " + (Callisto.playerInfo.isPaused ? "" : "NOT") + "paused");
            if(Callisto.playerInfo.isPaused)
            {
                Callisto.mplayer.start();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(Callisto.pauseDrawable);
            }
            else
            {
                Callisto.mplayer.pause();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(Callisto.playDrawable);
            }
            Callisto.playerInfo.isPaused = !Callisto.playerInfo.isPaused;
        }
        CallistoWidget.updateAllWidgets(c);
    }

    /** Stops the player. Clears the notifications, releases resources, etc. */
    public static void stop(Context c)
    {
        if(Callisto.playerInfo!=null)
            Callisto.playerInfo.clear();
        if(Callisto.mplayer!=null) {
            Callisto.mplayer.reset();
            Callisto.mplayer = null;
        }
        if(Live.live_player!=null) {
            Live.live_player.reset();
            Live.live_player = null;
        }
        Callisto.playerInfo.title = null;
        Callisto.playerInfo.update(c);
        Callisto.mNotificationManager.cancel(Callisto.NOTIFICATION_ID);
        CallistoWidget.updateAllWidgets(c);
    }
}
