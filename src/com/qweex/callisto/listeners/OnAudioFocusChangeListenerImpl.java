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

package com.qweex.callisto.listeners;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import com.qweex.callisto.PlayerControls;
import com.qweex.callisto.StaticBlob;


/**
 * An implementation of the listener to handle when the audio focus changes.
 */
public class OnAudioFocusChangeListenerImpl implements AudioManager.OnAudioFocusChangeListener
{
    /** The audio manager; used to request focus and dismiss it. */
    AudioManager am;
    /** Used to create an AudioManager and to pass to PlayPause. */
    Context c;

    /** Builds it && grabs; essentially the same as "setContext(); grab();" */
    public OnAudioFocusChangeListenerImpl(Context c)
    {
        super();
        setContext(c);
        int r = grab();
        Log.d("OnAudioFocusChangeListenerImpl", "Creating a new one: " + r);
    }

    /** Handles the change; there are 3 types of loss and 3 types of gain. */
    @Override
    public void onAudioFocusChange(int i)
    {
        switch(i)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                if(StaticBlob.playerInfo.isPaused)
                {
                    PlayerControls.playPause(c, null);
                    Log.d("OnAudioFocusChangeListenerImpl:onAudioFocusChange", "GAIN");
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:                  //STOP
                Log.d("OnAudioFocusChangeListenerImpl:onAudioFocusChange", "LOSS");
                PlayerControls.stop(c);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:        //PAUSE
                if(!StaticBlob.playerInfo.isPaused)
                {
                    Log.d("OnAudioFocusChangeListenerImpl:onAudioFocusChange", "LOSS_TRANSIENT");
                    PlayerControls.playPause(c, null);
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:   //LOWER VOLUME
                //Used to indicate that its lost focus, but it can "duck", meaning it doesn't have to just die.
                //Example: GPS voice speaks, you lower volume, not pause.
        }
    }

    /** Set the internal context */
    public void setContext(Context c)
    {
        this.c = c;
    }

    /** Grabs the audio focus, if it is able.
     * @return the result from requestAudioFocus; AUDIOFOCUS_REQUEST_{FAILED,GRANTED} */
    public int grab()
    {
        if(am==null)
            am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        return am.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }

    /** Release the audio focus. */
    public void drop()
    {
        //am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
        am.abandonAudioFocus(this);
    }
}
