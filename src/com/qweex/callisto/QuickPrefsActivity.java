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

import java.io.File;

import android.preference.*;

import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;


/** The activity that allows the user to change the preferences.
 * @author MrQweex
 */

public class QuickPrefsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    /** Donation app id for Qweex */
	public final static String DONATION_APP = "com.qweex.donation";

    /** Used to determine if the livestream should be stopped and started */
	private String oldRadioForLiveQuality;
    /** A button to do the play/pausing; essentially calls methods already in place rather than trying to do it from scratch. */
	private ImageButton MagicButtonThatDoesAbsolutelyNothing;	//This has to be an imagebutton because it is defined as such in Callisto.playPause
	
	/** Called when the activity is created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);
        //Deprecated functions ftw!
        addPreferencesFromResource(R.xml.preferences);
        findPreference("irc_max_scrollback").setOnPreferenceChangeListener(numberCheckListener);    //Set listener for assuring that it is just a number
        findPreference("secret").setEnabled(packageExists(DONATION_APP, this));                     //Enable secret features if donation app exists
        
        oldRadioForLiveQuality = PreferenceManager.getDefaultSharedPreferences(this).getString("live_url", "callisto");
        MagicButtonThatDoesAbsolutelyNothing = new ImageButton(this);
        MagicButtonThatDoesAbsolutelyNothing.setOnClickListener(Callisto.playPauseListener);

        this.getPreferenceScreen().findPreference("irc_settings").setOnPreferenceClickListener(setSubpreferenceBG);

        //Show a dialog when the user presses th reset IRC colors
        this.getPreferenceScreen().findPreference("reset_colors").setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
			@Override
			public boolean onPreferenceClick(Preference preference) {
		       	 DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		       		    @Override
		       		    public void onClick(DialogInterface dialog, int which) {
		       		        switch (which){
		       		        case DialogInterface.BUTTON_POSITIVE:
		       		            //Yes button clicked
		       		        	String[] keys = {"text", "back", "topic", "mynick", "me", "links", "pm", "join", "nick", "part", "quit", "kick", "mention", "error"};
		       		        	SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(QuickPrefsActivity.this).edit();
		       		        	for(String k : keys)
		       		        		e.remove("irc_color_" + k);
		       		        	e.commit();
		       		        	finish();
		       		        	android.content.Intent i = new android.content.Intent(QuickPrefsActivity.this, QuickPrefsActivity.class);
		       		        	startActivity(i);
		       		            break;

		       		        case DialogInterface.BUTTON_NEGATIVE:
		       		            //No button clicked
		       		            break;
		       		        }
		       		    }
		       		};

		       		AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext());
		       		builder.setTitle("Dude wait");
		       		builder.setMessage("Are you sure you want to reset the IRC colors to their default values?");
		       		builder.setPositiveButton("Yup", dialogClickListener);
		       		builder.setNegativeButton("Nope", dialogClickListener);
		       		builder.show();
		       		return true;
			}
        });

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /** Sets the preference background color for theming. http://stackoverflow.com/a/3223676/1526210 */
    OnPreferenceClickListener setSubpreferenceBG = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            PreferenceScreen a = (PreferenceScreen) preference;
            a.getDialog().getWindow().setBackgroundDrawableResource(R.color.backClr);
            return false;
        }
    };
    
    /** Called when any of the preferences is changed. Used to perform actions on certain events. */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        //Move files to new storage_path
    	if("storage_path".equals(key))
    	{
    		//Move folder for storage dir
	    	String new_path = sharedPreferences.getString("storage_path", "callisto");
	    	if(new_path==Callisto.storage_path)
	    		return;
	    	File newfile = new File(Environment.getExternalStorageDirectory(), new_path);
	    	newfile.mkdirs();
	    	
	    	File sd = Environment.getExternalStorageDirectory();
	    	File olddir = new File(sd, Callisto.storage_path);
	    	if(!olddir.renameTo(new File(sd, new_path)))
	    	{
	    		Toast.makeText(this, "An error occurred while trying to rename the folder. You might have to do it manually.", Toast.LENGTH_SHORT).show();
	    		Log.e("QuickPrefsActivity:onSharedPreferenceChanged", "Oh crap, a file couldn't be moved");
	    	}
		    Callisto.storage_path = new_path;
    	}
        //Restart stream if live quality changes
    	else if("live_url".equals(key) && Callisto.live_isPlaying)
    	{
    		String new_radio = PreferenceManager.getDefaultSharedPreferences(this).getString("live_url", "callisto");
    		if(!new_radio.equals(oldRadioForLiveQuality))
    		{
				MagicButtonThatDoesAbsolutelyNothing.performClick();
				MagicButtonThatDoesAbsolutelyNothing.performClick();
    			oldRadioForLiveQuality =new_radio;
    		}
    	}
        //Change the IRC scrollback
    	else if(key.equals("irc_max_scrollback"))
    		Callisto.chatView.setMaxLines(PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_max_scrollback", 500));

    }
    
    /** Determines if a package (i.e. application) is installed on the device.
     * http://stackoverflow.com/questions/6758841/how-to-know-perticular-package-application-exist-in-the-device
     * @param targetPackage The target package to test
     * @param c The Context of the where to search? I dunno.
     * @return True of the package is installed, false otherwise
     */
    public static boolean packageExists(String targetPackage, Context c)
    {
	   PackageManager pm=c.getPackageManager();
	   try {
		   pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
	       } catch (NameNotFoundException e) {
	    	   return false;
	    }  
	    return true;
   }
    
    /** Checks to make sure that a number (in this case, the scrollback) is a legit integer */
    //http://stackoverflow.com/questions/3206765/number-preferences-in-preference-activity-in-android
    Preference.OnPreferenceChangeListener numberCheckListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
        	return (!newValue.toString().equals("")  &&  newValue.toString().matches("\\d*")); 
        }
    };


}