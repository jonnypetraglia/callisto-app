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

import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import com.qweex.callisto.listeners.OnPreparedListenerWithContext;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * Created with IntelliJ IDEA.
 * User: notbryant
 * Date: 4/15/13
 * Time: 8:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class Live
{
    public static MediaPlayer live_player;
    //TODO: wtf
    static Live_FetchInfo LIVE_update = null;
    /** The url that is used to report statistics -completely anonymous- to the developer when the live fails. **/
    private final static String errorReportURL = "http://software.qweex.com/error_report.php";


    /** Listener for the live player in only the LiveStream activity. Starts it playing or displays an error message. */
    static OnPreparedListenerWithContext LIVE_PreparedListener = new OnPreparedListenerWithContext()
    {
        @Override
        public void onPrepared(MediaPlayer arg0) {

            Log.e("LLLLLLLL:", "PREPARED!");
            if(pd!=null)
            {
                if(!pd.isShowing())
                    return;
                pd.hide();
            }
            //*/
            try {
                live_player.start();
                LIVE_update = new Live_FetchInfo();
                LIVE_update.execute((Void [])null);
                Callisto.live_isPlaying = true;
            }
            catch(Exception e)
            {
                Callisto.errorDialog.show();
                e.printStackTrace();
            }
        }
    };

    /** Initiates the live player. Can be called across activities. */
    static public void LIVE_Init()
    {
        Log.d("LiveStream:liveInit", "Initiating the live player.");
        live_player = new MediaPlayer();
        Log.d("LiveStream:liveInit", "Initiating the live player.");
        live_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        Log.d("LiveStream:liveInit", "Initiating the live player.");
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
                    if(Callisto.errorDialog !=null)
                        Callisto.errorDialog.show();
                }catch(Exception e){}

                System.out.println(whatWhat);
                SendErrorReport(whatWhat);
                return true;
            }
        });
        Log.d("LiveStream:liveInit", "Initiating the live player.");
    }

    /** Method to prepare the live player; shows a dialog and then sets it up to be transfered to livePreparedListenerOther. */
    static public void LIVE_Prepare(Context c)
    {
        Log.d("LiveStream:LIVE_Prepare", "Preparing the live player.");
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
            LIVE_PreparedListener.pd.setOnDismissListener(new DialogInterface.OnDismissListener()
            {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    LIVE_PreparedListener.pd.cancel();
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
        String errorReport = errorReportURL + "?id=Callisto&v=" + Callisto.appVersion + "&err=" + android.os.Build.VERSION.RELEASE + "_" + msg;
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        HttpGet httpGet = new HttpGet(errorReport);
        try {
            httpClient.execute(httpGet, localContext);
        }catch(Exception e){}
    }
}
