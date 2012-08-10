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
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.app.ProgressDialog;
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
    	String new_path = PreferenceManager.getDefaultSharedPreferences(this).getString("storage_path", "callisto");
    	if(new_path==Callisto.storage_path)
    		return;
    	ProgressDialog pd = ProgressDialog.show(QuickPrefsActivity.this, Callisto.RESOURCES.getString(R.string.loading), Callisto.RESOURCES.getString(R.string.loading_msg), true, false); //TODO: Different loading message
    	File newfile = new File(Environment.getExternalStorageDirectory(), new_path);
    	newfile.mkdirs();
    	boolean wasError = false;
    	
    	File dir = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path);
    	newfile = newfile = new File(Environment.getExternalStorageDirectory(), new_path);
    	wasError = dir.renameTo(newfile);
    	/*
    	for (File child : dir.listFiles())
    	{
    		if (".".equals(child.getName()) || "..".equals(child.getName()))
    			continue;
    		newfile = new File(Environment.getExternalStorageDirectory(), new_path + File.separator + child.getName());
    		System.out.println(child.getAbsolutePath());
    		System.out.println(newfile.getAbsolutePath());
    		System.out.println("");
    		wasError = wasError || child.renameTo(newfile);
    	}
    	*/
    	if(wasError)
    		Log.e("QuickPrefsActivity:onSharedPreferenceChanged", "Oh crap, a file couldn't be moved");
    	 
	    Callisto.storage_path = new_path;
	    pd.cancel();
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