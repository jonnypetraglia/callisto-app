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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/** Receives the boot-up message; re-creates the alarms. */
public class BootNotificationReceiver extends BroadcastReceiver
{
	public static final SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss");
	public final static String PREF_FILE = "alarms";
	Calendar now = Calendar.getInstance();
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences alarmPrefs = context.getApplicationContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
		Map<String,?> alarms = alarmPrefs.getAll();
		SharedPreferences.Editor edit = alarmPrefs.edit();
		Log.d("BootNotify", "Begin");
		for (Map.Entry<String, ?> entry : alarms.entrySet())
		{
			Log.d("BootNotify", "Entry: " + entry);
			String show = entry.getKey().substring(0, entry.getKey().length()-14);
			Calendar time = Calendar.getInstance();
			String value = (String) entry.getValue();
			
			Log.d("BootNotify", "Getting things");
			int min = Integer.parseInt(value.substring(0,value.indexOf("_")));
			Log.d("BootNotify", "Thing1: " + min);
			String tone = value.substring(value.indexOf("_")+1,value.lastIndexOf("_"));
			Log.d("BootNotify", "Thing1: " + tone);
			int isAlarm_and_vibrate = Integer.parseInt(value.substring(value.lastIndexOf("_")+1));
			Log.d("BootNotify", "Thing1: " + isAlarm_and_vibrate);
			int isAlarm = isAlarm_and_vibrate>10?1:0;
			int vibrate = isAlarm_and_vibrate%2!=0?1:0;
			//min_tone_isAlarmvibrate
			
			try {
				time.setTime(sdfRaw.parse(entry.getKey().substring(14)));
			} catch (ParseException e) {}
			
			if(time.before(now))
			{
				edit.remove(entry.getKey());
				continue;
			}
			time.add(Calendar.MINUTE, -1*min);
			
			Log.d("BootNotify", "Creating intent");
			Intent i = new Intent(context, AlarmNotificationReceiver.class);
		    i.putExtra("tone", tone);
		    i.putExtra("min", min);
		    i.putExtra("isAlarm", isAlarm_and_vibrate>10);
		    i.putExtra("vibrate", vibrate);
		    i.putExtra("show", show);
		    PendingIntent pi = PendingIntent.getBroadcast(context.getApplicationContext(), 234324246, i, PendingIntent.FLAG_UPDATE_CURRENT);
		    Log.d("BootNotify", "Setting Alarm");
		    AlarmManager mAlarm = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
		    mAlarm.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pi); //DEBUG
		}
		edit.commit();
		Log.d("BootNotify", "Done");
	}
}