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

/** Receives and creates any alarms set in the "Plan" feature. */
public class AlarmNotificationReceiver extends BroadcastReceiver
{
	/** Called when an alarm is received. Creates the alarm and sounds it. */
	@Override
	public void onReceive(Context context, Intent intent)
	{
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
        	String show = extras.getString("show");
            String tone = extras.getString("tone");
            int min = extras.getInt("min");
			int isAlarm = extras.getInt("isAlarm"); 
			int vibrate = extras.getInt("vibrate");
			String key = extras.getString("key");
            
			Uri notification;
			try {
				notification = Uri.parse(tone);
			} catch(Exception e)
			{
				notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			}
            NotificationManager mNotificationManager =  (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent notificationIntent = new Intent(context, Callisto.class);
    		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            Callisto.notification_alarm = new Notification(R.drawable.callisto, "Connecting to IRC", System.currentTimeMillis());
            
            if(isAlarm>0)
            	Callisto.notification_alarm.flags |= Notification.FLAG_INSISTENT;
            else
            	Callisto.notification_alarm.flags |= Notification.FLAG_AUTO_CANCEL;
            if(vibrate>0)
            	Callisto.notification_alarm.vibrate = new long[] {300, 200, 100, 200};
            Callisto.notification_alarm.sound = notification;
    		
            if(min==0)
            	Callisto.notification_alarm.setLatestEventInfo(context, "Alarm!", show + " is on right now!", contentIntent);
            else
            	Callisto.notification_alarm.setLatestEventInfo(context, "Alarm!", show + " is in " + min + " minutes", contentIntent);
            mNotificationManager.notify(Callisto.NOTIFICATION_ID, Callisto.notification_alarm);
            
            SharedPreferences.Editor edit = Callisto.alarmPrefs.edit();
            edit.remove(key);
            edit.commit();
        }

	}
}