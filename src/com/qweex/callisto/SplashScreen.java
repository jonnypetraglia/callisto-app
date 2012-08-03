package com.qweex.callisto;

import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;

//This is the Activity for the Splash Screen
//The plan is for it to be only available if you've donated to JB.
//It's not much, but it's a small thing to make you feel like you got something back. :)

//FIXME: App doesn't effing exit when you call finish!

public class SplashScreen extends Activity {
	private int bryantime = 2000;		//This is initialized to 2 seconds but it will
										//become the length of the sound file
	boolean dudeJustQuit = true;
	MediaPlayer player;
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
        
        try {
	        if(appSettings.getBoolean("splash_quote", true))
	        {
		        player = new MediaPlayer();
		        AssetFileDescriptor afd = getAssets().openFd("bryan.mp3");
	            player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
	            player.prepare();
	            bryantime = player.getDuration();
	            player.start();
	        }
            t.start();
            } 
        catch (IllegalArgumentException e) { launchApp(); } 
        catch (IllegalStateException e) { launchApp(); } 
        catch (IOException e) { launchApp(); } 
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	if(dudeJustQuit)
    	{
	    	player.reset();
	    	t.interrupt();
    	}
    	finish();
    }
    
    private void launchApp()
    {
        Intent i = new Intent(SplashScreen.this, Callisto.class);
        startActivity(i);
        finish();
    }
    
	Thread t = new Thread() {
        public void run() {
            int time = 100;
            while (time < SplashScreen.this.bryantime)
            {
               try {
				sleep(100);
				time += 100;
               } catch (InterruptedException e) { break; } 
            }
            dudeJustQuit = isFinishing();
            
            System.out.println("Dudejustquit " + dudeJustQuit);
            if(!dudeJustQuit)
            	launchApp();
    }
	};
    

}
