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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.qweex.callisto.listeners.OnAudioFocusChangeListenerImpl;
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
    private static View seekView;
    private static AlertDialog alertDialog;
    public static TextView currentTime, totalTime;
    public static SeekBar seekBar;

    private long startPlayTime;

    public static void createSeekView(LayoutInflater inflater)
    {
        seekView = inflater.inflate(R.layout.seekbar_dialog, null, false);

        //Seekbar
        SeekBar sb = (SeekBar) seekView.findViewById(R.id.seekBar);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                if(fromUser)
                    currentTime.setText(Callisto.formatTimeFromSeconds(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar arg0) {}
            @Override

            public void onStopTrackingTouch(SeekBar arg0) {
                StaticBlob.mplayer.seekTo(arg0.getProgress()*1000);
                StaticBlob.playerInfo.clock.i=PlayerInfo.SAVE_POSITION_EVERY;
            }
        });

        //Buttons
        int[] b = new int[] {R.id.n10, R.id.n5, R.id.n1, R.id.n10s, R.id.p10s, R.id.p1, R.id.p5, R.id.p10};
        for(int i=0; i<b.length; i++)
        {
            seekView.findViewById(b[i]).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    String amnt = ((Button)v).getText().toString();
                    int amntInSec = Integer.parseInt(amnt.substring(1,amnt.length()-1));
                    if(amnt.charAt(0)=='-')
                        amntInSec*=-1;
                    if(amnt.charAt(amnt.length()-1)=='m')
                        amntInSec*=60;
                    StaticBlob.mplayer.seekTo(StaticBlob.mplayer.getCurrentPosition()+amntInSec*1000);
                    StaticBlob.playerInfo.clock.i=PlayerInfo.SAVE_POSITION_EVERY;
                    ((SeekBar)seekView.findViewById(R.id.seekBar)).setProgress(StaticBlob.mplayer.getCurrentPosition()/1000);
                    alertDialog.setMessage(Callisto.formatTimeFromSeconds(StaticBlob.mplayer.getCurrentPosition()/1000) + "/" + Callisto.formatTimeFromSeconds(StaticBlob.playerInfo.length));
                }
            });
        }
    }

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
                AlertDialog dg = new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.switch_to_live)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
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
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                StaticBlob.formatAlertDialogButtons(dg);
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
            StaticBlob.pauseCause = StaticBlob.PauseCause.User;
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

            //Create it.
            if(seekView.getParent()!=null)
                ((ViewGroup)seekView.getParent()).removeView(seekView);
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext()).setView(seekView);
            alertDialog = builder.create();
            alertDialog.setButton(v.getContext().getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.dismiss();
                }
            });//*/
            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    currentTime = null;
                }
            });
            alertDialog.setTitle(v.getContext().getResources().getString(R.string.seek_title));
            ((SeekBar)seekView.findViewById(R.id.seekBar)).setMax(StaticBlob.playerInfo.length);
            ((SeekBar)seekView.findViewById(R.id.seekBar)).setProgress(StaticBlob.mplayer.getCurrentPosition()/1000);
            alertDialog.show();

            currentTime = (TextView) seekView.findViewById(R.id.currentTime);
            ((TextView)seekView.findViewById(R.id.totalTime)).setText("/" + Callisto.formatTimeFromSeconds(StaticBlob.playerInfo.length));
        }
    };

    /** Changes position in queue to another song and optionally starts playing it.
     * @param c The context of the current activity.
     * @param previousOrNext >0 if it should play the next track, <0 for the previous, and 0 for the current
     * @param sp true if the player should start playing when it changes tracks, false otherwise
     */
    public static void changeToTrack(Context c, int previousOrNext, boolean sp)
    {
        String TAG = StaticBlob.TAG();
        Cursor queue = StaticBlob.databaseConnector.advanceQueue(previousOrNext);

        //If there are no items in the queue, stop the player
        if(queue==null || queue.getCount()==0)
        {
            Log.v(TAG, "Queue is empty. Pausing.");
            ImageButton x = (ImageButton) ((Activity)c).findViewById(R.id.playPause);
            if(x!=null)
                x.setImageDrawable(StaticBlob.pauseDrawable);
            StaticBlob.mNotificationManager.cancel(StaticBlob.NOTIFICATION_ID);
            stop(c);
            return;
        }

        Log.v(TAG, "Queue Size: " + queue.getCount());
        queue.moveToFirst();
        //The queue merely stores the identity (_id) of the entrie's position in the main SQL
        //After obtaining it, we can get all the information about it
        Long id = queue.getLong(queue.getColumnIndex("_id"));
        Long pos = queue.getLong(queue.getColumnIndex("position"));
        Long identity = queue.getLong(queue.getColumnIndex("identity"));
        boolean isStreaming = queue.getInt(queue.getColumnIndex("streaming"))>0;
        boolean isVideo = queue.getInt(queue.getColumnIndex("video"))>0;
        Cursor theTargetTrack = StaticBlob.databaseConnector.getOneEpisode(identity);
        theTargetTrack.moveToFirst();

        //Retrieve all of the playerInfo about the new track
        String media_location;
        if(StaticBlob.playerInfo==null)
            StaticBlob.playerInfo = new PlayerInfo(c);

        StaticBlob.playerInfo.title = theTargetTrack.getString(theTargetTrack.getColumnIndex("title"));
        StaticBlob.playerInfo.position = theTargetTrack.getInt(theTargetTrack.getColumnIndex("position"));
        StaticBlob.playerInfo.date = theTargetTrack.getString(theTargetTrack.getColumnIndex("date"));
        StaticBlob.playerInfo.show = theTargetTrack.getString(theTargetTrack.getColumnIndex("show"));
        Log.i(TAG, "Loading info: " + StaticBlob.playerInfo.title + " | " + StaticBlob.playerInfo.date);
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
                Log.e(TAG+":ParseException", "Error parsing a date from the SQLite db:");
                Log.e(TAG+":ParseException", StaticBlob.playerInfo.date);
                Log.e(TAG+":ParseException", "(This should never happen).");
                e.printStackTrace();
                Toast.makeText(c, c.getResources().getString(R.string.queue_error), Toast.LENGTH_SHORT).show();
                StaticBlob.databaseConnector.deleteQueueItem(pos);
                return;
            }

            //Here we actually get the path
            if(StaticBlob.storage_path==null)
            {
                StaticBlob.storage_path = PreferenceManager.getDefaultSharedPreferences(c).getString("storage_path", "callisto");
                if(!StaticBlob.storage_path.startsWith("/"))
                    StaticBlob.storage_path = Environment.getExternalStorageDirectory().toString() + File.separator + StaticBlob.storage_path;
            }
            File target = new File(StaticBlob.storage_path + File.separator + StaticBlob.playerInfo.show);
            target = new File(target, StaticBlob.playerInfo.date + "__" + StaticBlob.makeFileFriendly(StaticBlob.playerInfo.title) +
                    EpisodeDesc.getExtension(theTargetTrack.getString(theTargetTrack.getColumnIndex(isVideo ? "vidlink" : "mp3link"))));
            //If it doesn't exist, we must halt.
            if(!target.exists())
            {
                if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                {
                    new AlertDialog.Builder(c)
                            .setTitle(R.string.no_sd_card)
                            .setMessage(R.string.no_external_storage)
                            .setNegativeButton(android.R.string.ok,null)
                            .create().show();
                    return;
                }
                Log.e("*:changeToTrack", "File not found: " + target.getPath());
                Toast.makeText(c, c.getResources().getString(R.string.queue_error), Toast.LENGTH_SHORT).show();;
                StaticBlob.databaseConnector.deleteQueueItem(pos);
                return;
            }
            //Here we FINALLY get the path.
            media_location = target.getPath();
        }

        //Create the notification for this new track
        createNotification(c, identity);
        Log.d(TAG, "ok, finally preparing to play: " + media_location);

        //Here is where we FINALLY actually play the track.
        if(isVideo && sp)
        {
            ImageButton x = (ImageButton) ((Activity)c).findViewById(R.id.playPause);
            if(x!=null)
                x.setImageDrawable(StaticBlob.pauseDrawable);
            if(StaticBlob.mplayer!=null)
                StaticBlob.mplayer.stop();
            Log.i(TAG, "New track is a video, creating intent");
            Intent intent= new Intent(c, VideoActivity.class);
            intent.putExtra("uri", media_location);
            Log.i(TAG, "seek " + StaticBlob.playerInfo.position);
            intent.putExtra("seek", StaticBlob.playerInfo.position);
            c.startActivity(intent);
            return;
        }
        try {
            Log.i(TAG, "New track is an audio, doing MediaPlayer things");
            if(StaticBlob.mplayer==null)
                StaticBlob.mplayer = new MediaPlayer(); //This could be a problem
            StaticBlob.mplayer.reset();
            StaticBlob.mplayerPrepared.setContext(c);
            StaticBlob.mplayer.setDataSource(media_location);
            Log.i(TAG, "Setting source: " + media_location);
            StaticBlob.mplayer.setOnCompletionListener(StaticBlob.trackCompleted);
            StaticBlob.mplayer.setOnErrorListener(StaticBlob.trackCompletedBug);
            StaticBlob.mplayer.setOnPreparedListener(StaticBlob.mplayerPrepared);
            Log.i(TAG, "Preparing...? " + sp);
            //Streaming requires a dialog
            if(isStreaming)
            {
                if(!sp)
                {
                    StaticBlob.mplayer = null;
                    return;
                }
                try {
                StaticBlob.mplayerPrepared.startPlaying = sp;
                StaticBlob.mplayerPrepared.pd = ProgressDialog.show(c, c.getResources().getString(R.string.loading), c.getResources().getString(R.string.loading_msg), true, false);
                StaticBlob.mplayerPrepared.pd.setCancelable(true);
                StaticBlob.mplayerPrepared.pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        StaticBlob.mplayer.stop();
                        StaticBlob.mplayerPrepared.pd.hide();
                        StaticBlob.mplayerPrepared.pd = null;
                    }
                });
                } catch(Exception e) {}
            }
            StaticBlob.mplayer.prepareAsync();
            //Picks up at the mplayerPrepared listener
        } catch (IllegalArgumentException e) {
            Log.e(TAG+":IllegalArgumentException", "Error attempting to set the data path for MediaPlayer:");
            Log.e(TAG+":IllegalArgumentException", media_location);
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e(TAG+":IllegalStateException", "Error in State for MediaPlayer:");
        } catch (IOException e) {
            Log.e(TAG+":IOException", "IO is another of Jupiter's moons. Did you know that?");
            e.printStackTrace();
        }
    }

    public static void createNotification(Context c, Long identity)
    {
        //TODO: must we create the intents and whatnot every time?
        Intent notificationIntent = new Intent(c, EpisodeDesc.class);
        notificationIntent.putExtra("id", identity);
        PendingIntent contentIntent = PendingIntent.getActivity(c, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        StaticBlob.notification_playing = new Notification(R.drawable.callisto, null, System.currentTimeMillis());
        StaticBlob.notification_playing.flags = Notification.FLAG_ONGOING_EVENT;
        StaticBlob.notification_playing.setLatestEventInfo(c,  StaticBlob.playerInfo.title,  StaticBlob.playerInfo.show, contentIntent);
        if(StaticBlob.mNotificationManager==null)
            StaticBlob.mNotificationManager =  (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        StaticBlob.mNotificationManager.notify(StaticBlob.NOTIFICATION_ID, StaticBlob.notification_playing);
    }

    /** Plays or pauses the currently playing track; does not adjust what is the current track **/
    public static void playPause(Context c, View v)
    {
        String TAG = StaticBlob.TAG();
        Log.d(TAG,"PlayPause()");
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
                View x = new View(c);   //Dummy view only useful for carrying the context
                Callisto.launchAudio.onClick(x);
            }
            StaticBlob.live_isPlaying = !StaticBlob.live_isPlaying;
            CallistoWidget.updateAllWidgets(c);
            return;
        }

        //-----Local-----
        if(StaticBlob.databaseConnector==null || StaticBlob.databaseConnector.queueCount()==0)
        {
            StaticBlob.databaseConnector = new DatabaseConnector(c);
            if(StaticBlob.databaseConnector==null)
                return;
            StaticBlob.databaseConnector.open();
            if(StaticBlob.databaseConnector.queueCount()==0)
                return;
        }
        if(StaticBlob.mplayer==null)
        {
            Log.d(TAG,"PlayPause initiated");
            StaticBlob.mplayer = new MediaPlayer();
            StaticBlob.mplayer.setOnCompletionListener(StaticBlob.trackCompleted);
            StaticBlob.mplayer.setOnErrorListener(StaticBlob.trackCompletedBug);
            StaticBlob.mplayer.setOnPreparedListener(StaticBlob.mplayerPrepared);
            changeToTrack(c, 0, true);
        }
        else
        {
            Log.d(TAG,"PlayPause is " + (StaticBlob.playerInfo.isPaused ? "" : "NOT") + "paused");
            if(StaticBlob.playerInfo.isPaused)
            {
                Cursor isvid = StaticBlob.databaseConnector.currentQueueItem();
                isvid.moveToFirst();
                if(isvid.getInt(isvid.getColumnIndex("video"))>0)
                    changeToTrack(c, 0, true);
                else
                {
                    if(StaticBlob.audioFocus==null )//&& android.os.Build.VERSION.SDK_INT >= 11)
                        StaticBlob.audioFocus = new OnAudioFocusChangeListenerImpl(c);
                    if(StaticBlob.playerInfo.isPaused)
                        StaticBlob.mplayer.start();
                    else
                        StaticBlob.mplayer.prepareAsync();
                }
                Log.d(TAG,"Playing!!!!!");
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(StaticBlob.pauseDrawable);

                long id = -1;
                try {
                    Cursor cu = StaticBlob.databaseConnector.currentQueueItem();
                    cu.moveToFirst();
                    id = cu.getLong(cu.getColumnIndex("identity"));
                }catch(Exception e){}
                createNotification(c, id);
            }
            else
            {
                StaticBlob.mplayer.pause();
                if(v!=null)
                    ((ImageButton)v).setImageDrawable(StaticBlob.playDrawable);
                if(PreferenceManager.getDefaultSharedPreferences(c).getBoolean("hide_notification_when_paused", false))
                    StaticBlob.mNotificationManager.cancel(StaticBlob.NOTIFICATION_ID);
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
        if(StaticBlob.audioFocus!=null)
            StaticBlob.audioFocus.drop();
    }
}
