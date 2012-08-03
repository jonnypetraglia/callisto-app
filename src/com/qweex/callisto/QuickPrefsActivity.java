package com.qweex.callisto;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

//FEATURE: move files when changing storage path

//FEATURE: Configure when update will happen
//Configure radio quality
//FEATURE: Automatically delete songs upon completion
//FEATURE: Automatically add songs to queue after downloading
//FEATURE: Stop/start stream if live settings change

public class QuickPrefsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{    
	public final static String DONATION_APP = "com.qweex.donation";

    @Override
    public void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);        
        addPreferencesFromResource(R.xml.preferences);
        findPreference("secret").setEnabled(packageExists(DONATION_APP, this));
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
    	Callisto.storage_path = PreferenceManager.getDefaultSharedPreferences(this).getString("storage_path", "callisto");
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
}