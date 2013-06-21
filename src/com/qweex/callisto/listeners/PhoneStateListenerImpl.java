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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.qweex.callisto.PlayerControls;
import com.qweex.callisto.StaticBlob;

public class PhoneStateListenerImpl extends PhoneStateListener
{
    public Context c;

    public PhoneStateListenerImpl(Context c)
    {
        super();
        this.c = c;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber)
    {
        if (state == TelephonyManager.CALL_STATE_RINGING)
        {
            //Incoming call: Pause music
            Log.d("StaticBlob::onCallStateChanged", "State: RINGING");
            if(StaticBlob.live_isPlaying
                    || !StaticBlob.playerInfo.isPaused)
            {
                Log.d("StaticBlob::onCallStateChanged", "live_isPlaying || !isPaused");
                PlayerControls.playPause(c, null);
                StaticBlob.pauseCause = StaticBlob.PauseCause.PhoneCall;
            }
        /*} else if(state == TelephonyManager.CALL_STATE_IDLE)
        {
            Log.d("StaticBlob::onCallStateChanged", "State: IDLE");*/
        } else if(state == TelephonyManager.CALL_STATE_OFFHOOK || state == TelephonyManager.CALL_STATE_IDLE)
        {
            //Not in call: Play music
            Log.d("StaticBlob::onCallStateChanged", "State: OFFHOOK/IDLE");
            if(StaticBlob.pauseCause == StaticBlob.PauseCause.PhoneCall &&
                    StaticBlob.playerInfo.isPaused)
            {
                Log.d("StaticBlob::onCallStateChanged", "live_isPlaying || !isPaused");
                PlayerControls.playPause(c, null);
            }
        }
        super.onCallStateChanged(state, incomingNumber);
    }
};