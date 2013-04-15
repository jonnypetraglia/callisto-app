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
package com.qweex.callisto.receivers;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

/** Receives and creates any alarms set in the "Plan" feature. */
public class AlarmNotificationReceiver extends BroadcastReceiver
{
	public final static String PREF_FILE = "alarms";
	
	/** Called when an alarm is received. Creates the alarm and sounds it. */
	@Override
	public void onReceive(Context context, Intent intent)
	{
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
            //Get info about the event from the Extras
        	String show = extras.getString("show");
            String tone = extras.getString("tone");
            int min = extras.getInt("min");
			int isAlarm = extras.getInt("isAlarm"); 
			int vibrate = extras.getInt("vibrate");
			String key = extras.getString("key");

            //Create the notification
			Uri notification_sound;
			try {
				notification_sound = Uri.parse(tone);
			} catch(Exception e)
			{
				notification_sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			}
            NotificationManager mNotificationManager =  (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent notificationIntent = new Intent(context, Callisto.class);
    		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            Callisto.notification_alarm = new Notification(R.drawable.callisto, "JupiterBroadcasting alarm!", System.currentTimeMillis());

            //Set the notification options
            if(isAlarm>0)
            	Callisto.notification_alarm.flags |= Notification.FLAG_INSISTENT;
            else
            	Callisto.notification_alarm.flags |= Notification.FLAG_AUTO_CANCEL;
            if(vibrate>0)
            	Callisto.notification_alarm.vibrate = new long[] {300, 200, 100, 200};
            Callisto.notification_alarm.sound = notification_sound;

            //Show the notification
            if(min==0)
            	Callisto.notification_alarm.setLatestEventInfo(context, "Alarm!", show + " is on right now!", contentIntent);
            else
            	Callisto.notification_alarm.setLatestEventInfo(context, "Alarm!", show + " is in " + min + " minutes", contentIntent);
            mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_alarm);

            //Remove the alarm
            SharedPreferences alarmPrefs = context.getApplicationContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = alarmPrefs.edit();
            edit.remove(key);
            edit.commit();
        }

	}
}