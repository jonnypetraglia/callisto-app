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
package com.qweex.callisto.bonuses;

import java.io.File;

import com.qweex.callisto.Callisto;
import com.qweex.callisto.QuickPrefsActivity;
import com.qweex.callisto.R;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;


/** A nice little splash screen that can be enabled or disabled.
 * The plan is for it to be only available if you've donated to JB or maybe Qweex.
 * It's not much, but it's a small thing to make you feel like you got something back. :)
 * @author notbryant
 */

public class SplashScreen extends Activity {
    private int bryantime = 2000;		//This is initialized to 2 seconds but it will
    //become the length of the sound file
    private boolean dudeJustQuit = true;
    private MediaPlayer player;

    /** Called when the activity is created. Sets the view, starts playing the audio, and starts a timer. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!QuickPrefsActivity.packageExists(QuickPrefsActivity.DONATION_APP,this))
        {
            launchApp();
            return;
        }

        setContentView(R.layout.splash);

        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        if(!appSettings.getBoolean("splash_show", true))
        {
            Intent i = new Intent(SplashScreen.this, Callisto.class);
            startActivity(i);
            dudeJustQuit = false;
            finish();
            return;
        }

        if(appSettings.getBoolean("splash_quote", true))
        {
            try {
                File target = new File(Environment.getExternalStorageDirectory(), PreferenceManager.getDefaultSharedPreferences(this).getString("storage_path", "callisto")
                        + File.separator + "extras" + File.separator + "bryan.mp3");
                System.out.println(target.getAbsolutePath());
                player = new MediaPlayer();
                player.setDataSource(target.getAbsolutePath());
                player.prepare();
                player.start();
                bryantime = player.getDuration();
            } catch (Exception e) {}
        }
        t.start();
    }

    /** Called when the activity is done. */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(dudeJustQuit)
        {
            if(player!=null)
                player.reset();
            t.interrupt();
        }
        finish();
    }

    /** Launches the main activity of the app. */
    private void launchApp()
    {
        Intent i = new Intent(SplashScreen.this, Callisto.class);
        startActivity(i);
        finish();
    }

    private Thread t = new Thread() {
        public void run() {
            int time = 100;
            while (time < SplashScreen.this.bryantime+100)
            {
                try {
                    sleep(100);
                    time += 100;
                } catch (InterruptedException e) { break; }
            }
            dudeJustQuit = isFinishing();
            if(!dudeJustQuit)
                launchApp();
        }
    };
}
