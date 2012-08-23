/*
Copyright (C) 2012 Qweex
This file is a part of Callisto.

Callisto is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

Callisto is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Callisto; If not, see <http://www.gnu.org/licenses/>.
*/
package com.qweex.callisto;

import java.io.File;

import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

//FEATURE: Configure when update will happen
//FEATURE: Automatically delete songs upon completion
//FEATURE: Automatically add songs to queue after downloading

public class QuickPrefsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{    
	public final static String DONATION_APP = "com.qweex.donation";
	private String old_radio;
	ImageButton MagicButtonThatDoesAbsolutelyNothing;	//This has to be an imagebutton because it of playPause in Callisto.java
	
    @Override
    public void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);        
        addPreferencesFromResource(R.xml.preferences);
        findPreference("irc_max_scrollback").setOnPreferenceChangeListener(numberCheckListener);
        findPreference("secret").setEnabled(packageExists(DONATION_APP, this));
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
        old_radio = PreferenceManager.getDefaultSharedPreferences(this).getString("live_url", "callisto");
        MagicButtonThatDoesAbsolutelyNothing = new ImageButton(this);
        MagicButtonThatDoesAbsolutelyNothing.setOnClickListener(Callisto.playPause);
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
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
    	else if("live_url".equals(key) && Callisto.live_isPlaying)
    	{
    		String new_radio = PreferenceManager.getDefaultSharedPreferences(this).getString("live_url", "callisto");
    		if(!new_radio.equals(old_radio))
    		{
				MagicButtonThatDoesAbsolutelyNothing.performClick();
				MagicButtonThatDoesAbsolutelyNothing.performClick();
    			old_radio=new_radio;
    		}
    	}
    	else if(key=="irc_max_scrollback")
    		Callisto.chatView.setMaxLines(PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_max_scrollback", 500));
	    
    }
    
    //http://stackoverflow.com/questions/6758841/how-to-know-perticular-package-application-exist-in-the-device
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
    
    //http://stackoverflow.com/questions/3206765/number-preferences-in-preference-activity-in-android
    //@Override
    Preference.OnPreferenceChangeListener numberCheckListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            //Check that the string is an integer.
            return numberCheck(newValue);
        }
    };


    private boolean numberCheck(Object newValue) {
	    if( !newValue.toString().equals("")  &&  newValue.toString().matches("\\d*") ) {
	        return true;
	    }
	    else {
	        return false;
    }
}

}