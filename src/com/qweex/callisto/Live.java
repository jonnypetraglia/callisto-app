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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;
import com.qweex.callisto.listeners.OnPreparedListenerWithContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Live
{
    public static MediaPlayer live_player;
    //TODO: wtf
    static FetchInfo LiveUpdate = null;
    /** The url that is used to report statistics -completely anonymous- to the developer when the live fails. **/
    private final static String errorReportURL = "http://software.qweex.com/error_report.php";


    /** Listener for the live player in only the LiveStream activity. Starts it playing or displays an error message. */
    static OnPreparedListenerWithContext LIVE_PreparedListener = new OnPreparedListenerWithContext()
    {
        @Override
        public void onPrepared(MediaPlayer arg0) {
            String TAG = StaticBlob.TAG();
            Log.d(TAG, "PREPARED! " + pd);
            if(pd==null)    //The loading has been canceled
                return;
            pd.cancel();

            try {
                live_player.start();
                LiveUpdate = new FetchInfo();
                LiveUpdate.execute((Void[]) null);
                StaticBlob.live_isPlaying = true;
            }
            catch(Exception e)
            {
                StaticBlob.errorDialog.show();
                e.printStackTrace();
            }
        }
    };

    /** Initiates the live player. Can be called across activities. */
    static public void LIVE_Init()
    {
        String TAG = StaticBlob.TAG();
        Log.d(TAG, "Initiating the live player.");
        live_player = new MediaPlayer();
        Log.d(TAG, "Initiating the live player.");
        live_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "Initiating the live player.");
        live_player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if(LIVE_PreparedListener.pd!=null)
                    LIVE_PreparedListener.pd.hide();
                String whatWhat="";
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                        whatWhat = "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK";
                        break;
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        whatWhat = "MEDIA_ERROR_SERVER_DIED";
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                        whatWhat = "MEDIA_ERROR_UNKNOWN";
                        break;
                    default:
                        whatWhat = "???";
                        return true;
                }
                try{
                    if(StaticBlob.errorDialog !=null)
                        StaticBlob.errorDialog.show();
                }catch(Exception e){}

                try {
                SendErrorReport(whatWhat);
                } catch(Exception e) {}
                return true;
            }
        });
        Log.d(TAG, "Initiating the live player.");
    }

    /** Method to prepare the live player; shows a dialog and then sets it up to be transfered to livePreparedListenerOther. */
    static public void LIVE_Prepare(Context c)
    {
        String TAG = StaticBlob.TAG();
        Log.d(TAG, "Preparing the live player.");
        if(c!=null)
        {
            LIVE_PreparedListener.pd = Callisto.BaconDialog(c, "Buffering...", null);

            /*
            final AnimationDrawable d = (AnimationDrawable) ((ProgressBar) LIVE_PreparedListener.baconPDialog.getWindow().findViewById(android.R.id.progress)).getIndeterminateDrawable();
            ((View)LIVE_PreparedListener.baconPDialog.getWindow().findViewById(android.R.id.progress)).post(new Runnable() {
                @Override
                public void run() {
                    d.start();
                }
            });
            //*/
            LIVE_PreparedListener.pd.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface dialog) {
                    System.out.println("DIALOG IS BEING CANCELED");
                    if(LIVE_PreparedListener.pd!=null)
                        LIVE_PreparedListener.pd.hide();
                    LIVE_PreparedListener.pd = null;
                }

            });
            LIVE_PreparedListener.pd.setCancelable(true);
        }
        live_player.prepareAsync();
    }

    /** Sends an error report to the folks at Qweex. COMPLETELY anonymous. The only information that is sent is the version of Callisto and the version of Android. */
    public static void SendErrorReport(String msg)
    {
        String errorReport = errorReportURL + "?id=Callisto&v=" + StaticBlob.appVersion + "&err=" + android.os.Build.VERSION.RELEASE + "_" + msg;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        HttpGet httpGet = new HttpGet(errorReport);
        try {
            httpClient.execute(httpGet, localContext);
        }catch(Exception e){}
    }

    /** Updates the current and next track information when listening to the live stream. */
    public static class FetchInfo extends AsyncTask<Void, Void, Void>
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
            Intent notificationIntent = new Intent(LIVE_PreparedListener.c, Callisto.class);
            PendingIntent contentIntent = PendingIntent.getActivity(LIVE_PreparedListener.c, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            if(StaticBlob.notification_playing==null)
            {
                StaticBlob.notification_playing = new Notification(R.drawable.callisto, null, System.currentTimeMillis());
                StaticBlob.notification_playing.flags = Notification.FLAG_ONGOING_EVENT;
            }
            StaticBlob.notification_playing.setLatestEventInfo(LIVE_PreparedListener.c, StaticBlob.playerInfo.title,  "JB Radio", contentIntent);
            NotificationManager mNotificationManager =  (NotificationManager) LIVE_PreparedListener.c.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(StaticBlob.NOTIFICATION_ID, StaticBlob.notification_playing);

            return null;
        }
    }
}
