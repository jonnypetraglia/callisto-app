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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

/** Updates the current and next track information when listening to the live stream. */
public class Live_FetchInfo extends AsyncTask<Void, Void, Void>
{
    /** The address of the info to fetch */
    private static final String infoURL = "http://jbradio.airtime.pro/api/live-info";
    /** A Regex matcher to extract the info */
    private static Matcher liveMatcher = null;

    @Override
    protected Void doInBackground(Void... c)
    {
        //Prepare HTTP stuffs
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        HttpGet httpGet = new HttpGet(infoURL);
        HttpResponse response;
        try
        {
            //Read the data into a variable
            response = httpClient.execute(httpGet, localContext);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            response.getEntity().getContent()
                    )
            );
            String line, result = "";
            while ((line = reader.readLine()) != null)
                result += line + "\n";

            //Extract the title and show
            liveMatcher = (Pattern.compile(".*?\"currentShow\".*?"
                    + "\"name\":\"(.*?)\""
                    + ".*"
                    + "\"name\":\"(.*?)\""
                    + ".*?")
            ).matcher(result);
            if(liveMatcher.find())
                StaticBlob.playerInfo.title = liveMatcher.group(1);
            if(liveMatcher.groupCount()>1)
                StaticBlob.playerInfo.show = liveMatcher.group(2);

            //Send a message to update the player controls
            PlayerInfo.updateHandler.sendEmptyMessage(0);
            //CallistoWidget.updateAllWidgets(Callisto.LIVE_PreparedListener.c);

        } catch (ClientProtocolException e) {
            // TODO EXCEPTION
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        };

        //Update the notification
        Intent notificationIntent = new Intent(Live.LIVE_PreparedListener.c, Callisto.class);
        PendingIntent contentIntent = PendingIntent.getActivity(Live.LIVE_PreparedListener.c, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        if(StaticBlob.notification_playing==null)
        {
            StaticBlob.notification_playing = new Notification(R.drawable.callisto, null, System.currentTimeMillis());
            StaticBlob.notification_playing.flags = Notification.FLAG_ONGOING_EVENT;
        }
        StaticBlob.notification_playing.setLatestEventInfo(Live.LIVE_PreparedListener.c, StaticBlob.playerInfo.title,  "JB Radio", contentIntent);
        NotificationManager mNotificationManager =  (NotificationManager) Live.LIVE_PreparedListener.c.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(StaticBlob.NOTIFICATION_ID, StaticBlob.notification_playing);

        return null;
    }
}