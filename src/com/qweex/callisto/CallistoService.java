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

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import com.qweex.callisto.receivers.AudioJackReceiver;

/* An always-running service to do things. Nice things. These things currently include:
 *   -Handling the audio jack plug/unplug (including outside of the application)
 */
public class CallistoService extends Service
{
    /** A receiver to handle when the the audio jack is unplugged */
    public static AudioJackReceiver audioJackReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate()
    {
        registerReceiver(audioJackReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        Log.i("CallistoService:onStart", "Registering Receiver");
    }
}
