package com.qweex.callisto;

// This is the Activity for the Splash Screen

import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;

public class SplashScreen extends Activity {
	int bryantime = 2000;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        SharedPreferences showSettings = getSharedPreferences("settings", 0);
		if(showSettings.getBoolean("skip_splash", true))
		{
			Intent i = new Intent(SplashScreen.this, Callisto.class);
            startActivity(i);
            finish();
            return;
		}
        
        try {
	        Thread t = new Thread() {
	            public void run() {
	                try {
	                    int time = 100;
	                    while (time < SplashScreen.this.bryantime) {
	                       sleep(100);
	                       time += 100;
	                    }
	                }
	                catch (InterruptedException e) {
	                    // do nothing
	                }
	                finally {
	                    Intent i = new Intent(SplashScreen.this, Callisto.class);
	                    startActivity(i);
	                    finish();
	                }
	            }
	        };
	        if(!showSettings.getBoolean("no_bryan", true))
	        {
		        MediaPlayer player = new MediaPlayer();
		        AssetFileDescriptor afd = getAssets().openFd("bryan.mp3");
	            player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
	            player.prepare();
	            bryantime = player.getDuration();
	            player.start();
	        }
            t.start();
            } 
        catch (IllegalArgumentException e) {    } 
        catch (IllegalStateException e) { } 
        catch (IOException e) { } 
    }
}
