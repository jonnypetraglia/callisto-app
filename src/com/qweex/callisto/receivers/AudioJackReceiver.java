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

package com.qweex.callisto.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import com.qweex.callisto.PlayerControls;
import com.qweex.callisto.R;
import com.qweex.callisto.StaticBlob;

/** A receiver to handle when the audio jack is plugged and unplugged.
 * It uses the preferences to determine if it should stop playback on unplug and then resume playback on re-plug.
 * It only resumes playback if it was the one to stop it. If the user presses the play button (i.e. "playPauseListener"),
 * it will sense that the user is acting and not intervene.
 */

public class AudioJackReceiver extends BroadcastReceiver
{
    /** Not only for receiving the shared preferences, but also for finding the button for updating the drawable.
     * (If there is one). Kept updated by playerInfo.update().
     */
    public Context contextForPreferences;
    /** It is its name. If it is paused was it paused by the receiver or by the user? */
    private boolean pastInitialCreate = false;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String TAG = StaticBlob.TAG();
        if(!pastInitialCreate)
        {
            pastInitialCreate=true;
            return;
        }
        View v = null;
        if(contextForPreferences instanceof Activity)
            v = ((Activity) contextForPreferences).findViewById(R.id.playPause);
        if(intent.getExtras().getInt("state") == 0)
        {
            if(PreferenceManager.getDefaultSharedPreferences(contextForPreferences).getBoolean("pause_unplugged", true)
                    && !StaticBlob.playerInfo.isPaused && StaticBlob.mplayer!=null)
            {
                PlayerControls.playPause(contextForPreferences, v);
                StaticBlob.pauseCause = StaticBlob.PauseCause.AudioJack;
            }
            Log.i(TAG, "HEADSET IS OR HAS BEEN DISCONNECTED");
        }else
        {
            if(PreferenceManager.getDefaultSharedPreferences(contextForPreferences).getBoolean("play_replugged", true)
                    && StaticBlob.playerInfo.isPaused && StaticBlob.mplayer!=null
                && StaticBlob.pauseCause == StaticBlob.PauseCause.AudioJack)
            {
                PlayerControls.playPause(contextForPreferences, v);
            }
            Log.i(TAG, "HEADSET IS OR HAS BEEN RECONNECTED");
        }
    }
}