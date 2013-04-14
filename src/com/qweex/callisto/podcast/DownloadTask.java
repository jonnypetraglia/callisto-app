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
package com.qweex.callisto.podcast;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;

/** A class to start downloading a file outside the UI thread. */
public class DownloadTask extends AsyncTask<String, Object, Boolean>
{
    /** Info about episode */
    private String Title, Date, Link, Show;
    /** Total size of the file to be downloaded */
    private long TotalSize;
    /** Target file to download to */
    private File Target;
    /** ID for download notification */
    private final int NOTIFICATION_ID = 3696;
    /** Timeout Parameters */
    private final int TIMEOUT_CONNECTION = 5000, TIMEOUT_SOCKET = 30000;
    /** For displaying the notification */
    private NotificationManager mNotificationManager;
    /** For notification */
    private PendingIntent contentIntent;
    /** Is the task running? */
    public static boolean running = false;
    /** Context, used for intent and pending intent for notification */
    public Context context;
    /** The number of times the CURRENT download has failed. */
    private int inner_failures;
    /** The number of times trying to re-download the entire list has failed??? */
    private int outer_failures = 0;
    /** The number of files that have failed and been passed.*/
    private int failed = 0;
    /** Limits for how often there can be failures */
    private final int INNER_LIMIT=5, OUTER_LIMIT=10;


    public DownloadTask(Context c)
    {
        super();
        context = c;
    }

    @Override
    protected void onPreExecute()
    {
        running = true;
        Log.i("EpisodeDesc:DownloadTask", "Beginning downloads");

        //Show notification
        Intent notificationIntent = new Intent(context, DownloadList.class);
        contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Callisto.notification_download = new Notification(R.drawable.ic_action_download, Callisto.RESOURCES.getString(R.string.beginning_download), System.currentTimeMillis());
        Callisto.notification_download.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Callisto.notification_download.setLatestEventInfo(context.getApplicationContext(), Callisto.RESOURCES.getString(R.string.downloading) + " " + Callisto.current_download + " " +  Callisto.RESOURCES.getString(R.string.of) + " " + Callisto.downloading_count + ": 0%", Show + ": " + Title, contentIntent);
    }


    @Override
    protected Boolean doInBackground(String... params)
    {
        boolean isVideo;
        Cursor current;

        Callisto.notification_download.setLatestEventInfo(context.getApplicationContext(),
                Callisto.RESOURCES.getString(R.string.downloading) + "...",
                "", contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, Callisto.notification_download);

        long id = 0;
        Log.e("DownloadTask:doInBackground", "Preparing to start");
        while(DownloadList.getDownloadCount(context, DownloadList.ACTIVE)>0)
        {
            if(isCancelled())   //Checks to see if it has been canceled by somewhere else
            {
                mNotificationManager.cancel(NOTIFICATION_ID);
                return false;
            }
            try
            {
                String dlList = PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", "");
                Log.e("DownloadTask:doInBackground", "The download list is: " + dlList);
                id = DownloadList.getDownloadAt(context, DownloadList.ACTIVE, 0);
                if(id<=0)   //A negative ID means it is a video
                {
                    isVideo=true;
                    current = Callisto.databaseConnector.getOneEpisode(id*-1);
                }
                else
                {
                    isVideo=false;
                    current = Callisto.databaseConnector.getOneEpisode(id);
                }
                current.moveToFirst();

                //Get info
                Link = current.getString(current.getColumnIndex(isVideo ? "vidlink" : "mp3link"));
                Title = current.getString(current.getColumnIndex("title"));
                Date = current.getString(current.getColumnIndex("date"));
                Show = current.getString(current.getColumnIndex("show"));
                Log.i("EpisodeDesc:DownloadTask", "Starting download: " + Link);
                Date = Callisto.sdfFile.format(Callisto.sdfRaw.parse(Date));

                //Getting target
                Target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + Show);
                Target.mkdirs();
                if(Title.indexOf("|")>0)
                    Title = Title.substring(0, Title.indexOf("|"));
                Title=Title.trim();
                Target = new File(Target, Date + "__" + DownloadList.makeFileFriendly(Title) + EpisodeDesc.getExtension(Link));

                //Prepare the HTTP
                Log.i("EpisodeDesc:DownloadTask", "Path: " + Target.getPath());
                URL url = new URL(Link);
                Log.i("EpisodeDesc:DownloadTask", "Opening the connection...");
                HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
                String lastModified = ucon.getHeaderField("Last-Modified");
                ucon = (HttpURLConnection) url.openConnection();
                if(Target.exists())
                {
                    ucon.setRequestProperty("Range", "bytes=" + Target.length() + "-");
                    ucon.setRequestProperty("If-Range", lastModified);
                }
                ucon.setReadTimeout(TIMEOUT_CONNECTION);
                ucon.setConnectTimeout(TIMEOUT_SOCKET);
                ucon.connect();

                //Notification
                Callisto.notification_download.setLatestEventInfo(context.getApplicationContext(),
                        Callisto.RESOURCES.getString(R.string.downloading) + " " +
                                Callisto.current_download + " " +
                                Callisto.RESOURCES.getString(R.string.of) + " " +
                                Callisto.downloading_count + " (...)",
                        Show + ": " + Title, contentIntent);
                mNotificationManager.notify(NOTIFICATION_ID, Callisto.notification_download);

                //Actually do the DLing
                InputStream is = ucon.getInputStream();
                TotalSize = ucon.getContentLength() + Target.length();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
                FileOutputStream outStream;
                byte buff[];
                Log.i("EpisodeDesc:DownloadTask", "mmk skipping the downloaded..." + Target.length() + " of " + TotalSize);
                if(Target.exists()) //Append if it exists
                    outStream = new FileOutputStream(Target, true);
                else
                    outStream = new FileOutputStream(Target);
                buff = new byte[5 * 1024];
                Log.i("EpisodeDesc:DownloadTask", "Getting content length (size)");
                int len = 0;
                long downloadedSize = Target.length(),
                        percentDone = 0;

                //SPEED_COUNT == the number of times through the buffer loop to go through before updating the speed
                // currentDownloadLoopIndex == ???
                // lastTime == The time that the last speed was calculated at
                // all_spds == All speeds tabulated thus far from currentDownloadLoopIndex to SPEED_COUNT
                // avg_speed == the average speed when SPEED_COUNT is reached
                int SPEED_COUNT = 200,
                        currentDownloadLoopIndex = 0;
                long lastTime = (new java.util.Date()).getTime(),
                        all_spds = 0;
                double avg_speed = 0;
                DecimalFormat df = new DecimalFormat("#.##");

                //Wifi lock
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if(DownloadList.Download_wifiLock==null)
                    DownloadList.Download_wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "Callisto_download");
                if(!DownloadList.Download_wifiLock.isHeld())
                    DownloadList.Download_wifiLock.acquire();

                Log.i("EpisodeDesc:DownloadTask", "FINALLY starting the download");
                inner_failures = 0;
                //-----------------Here is where the actual downloading happens----------------
                while (len != -1)
                {
                    Log.i("EpisodeDesc:DownloadTask", "DownloadedSize: " + downloadedSize);
                    dlList = PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", "");
                    if(dlList.equals("") || !(Long.parseLong(dlList.substring(1,dlList.indexOf('|',1)))==id))
                    {
                        Log.i("EpisodeDesc:DownloadTask", "Download has been canceled, deleting.");
                        Target.delete();
                        break;
                    }
                    if(isCancelled())
                    {
                        mNotificationManager.cancel(NOTIFICATION_ID);
                        return false;
                    }

                    try
                    {
                        len = inStream.read(buff);
                        if(len==-1)
                            break;

                        outStream.write(buff,0,len);
                        downloadedSize += len;
                        percentDone = downloadedSize*100;
                        percentDone /= TotalSize;

                        //Add to the average speed
                        long temp_spd = 0;
                        long time_diff = ((new java.util.Date()).getTime() - lastTime);
                        if(time_diff>0)
                        {
                            temp_spd= len*100/time_diff;
                            currentDownloadLoopIndex++;
                            all_spds += temp_spd;
                            lastTime = (new java.util.Date()).getTime();
                        }

                    } catch (IOException e) {
                        Log.e("EpisodeDesc:DownloadTask:IOException", "IO is a moon - " + inner_failures);
                        inner_failures++;
                        if(inner_failures==INNER_LIMIT)
                            break;
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {}
                        //Add failure to average
                        currentDownloadLoopIndex++;
                        lastTime = (new java.util.Date()).getTime();

                    } catch (Exception e) {
                        Log.e("EpisodeDesc:DownloadTask:??Exception", e.getClass() + " : " + e.getMessage());
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {}
                        //Add failure to average
                        currentDownloadLoopIndex++;
                        lastTime = (new java.util.Date()).getTime();
                    }

                    //If the time is right, get the average
                    if(currentDownloadLoopIndex>=SPEED_COUNT)
                    {
                        avg_speed = all_spds*1.0/currentDownloadLoopIndex/100;
                        all_spds = 0;
                        currentDownloadLoopIndex = 0;

                        if(DownloadList.downloadProgress!=null)
                        {
                            DownloadList.downloadProgress.setMax((int)(TotalSize/1000));
                            DownloadList.downloadProgress.setProgress((int)(downloadedSize/1000));
                        }
                        Callisto.notification_download.setLatestEventInfo(context.getApplicationContext(),
                                Callisto.RESOURCES.getString(R.string.downloading) + " " +
                                        Callisto.current_download + " " +
                                        Callisto.RESOURCES.getString(R.string.of) + " " +
                                        Callisto.downloading_count + ": " + percentDone + "%  (" +
                                        df.format(avg_speed) + "kb/s)",
                                Show + ": " + Title, contentIntent);
                        mNotificationManager.notify(NOTIFICATION_ID, Callisto.notification_download);
                    }
                }

                outStream.flush();
                outStream.close();
                inStream.close();
                dlList = PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", "");
                if(inner_failures==INNER_LIMIT)
                {

                    throw new Exception("Inner exception has passed " + INNER_LIMIT);
                }
                if(!dlList.equals("") && (Long.parseLong(dlList.substring(1,dlList.indexOf('|',1)))==id))
                {
                    Log.i("EpisodeDesc:DownloadTask", "Trying to finish with " + Target.length() + "==" + TotalSize);
                    if(Target.length()==TotalSize)
                    {
                        Callisto.current_download++;

                        Log.i("EpisodeDesc:DownloadTask", (inner_failures<INNER_LIMIT?"Successfully":"FAILED") + " downloaded to : " + Target.getPath());

                        //Move the download from active to completed.
                        DownloadList.removeDownload(context, DownloadList.ACTIVE, id, isVideo);
                        DownloadList.addDownload(context, DownloadList.COMPLETED, id, isVideo);

                        Log.i("EpisodeDesc:DownloadTask", " " + DownloadList.rebuildHeaderThings);
                        if(DownloadList.rebuildHeaderThings !=null)
                            DownloadList.rebuildHeaderThings.sendEmptyMessage(0);

                        boolean queue = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("download_to_queue", false);
                        if(queue)
                            Callisto.databaseConnector.appendToQueue(id, false, isVideo);
                    }
                } else
                    Target.delete();
            }catch (ParseException e) {
                Log.e("EpisodeDesc:DownloadTask:ParseException", "Error parsing a date from the SQLite db: ");
                Log.e("EpisodeDesc:DownloadTask:ParseException", Date);
                Log.e("EpisodeDesc:DownloadTask:ParseException", "(This should never happen).");
                outer_failures++;
                e.printStackTrace();
            } catch(Exception e) {
                outer_failures++;
                Log.e("EpisodeDesc:DownloadTask:Exception " + e.getClass(), "[" + outer_failures + "] Msg: " + e.getMessage());
                e.printStackTrace();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {}
            }
            if(outer_failures==OUTER_LIMIT)
            {
                //Add to end of active, but with x signifier to show it failed
                SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
                String aDownloads = PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", "");
                aDownloads = aDownloads.replace("|" + Long.toString(id) + "|", "|");
                aDownloads = aDownloads.concat("x" + Long.toString(id) + "|");
                if(aDownloads.equals("|"))
                    aDownloads="";
                boolean quit = false;
                if(quit = aDownloads.charAt(1)=='x')
                    aDownloads = aDownloads.replaceAll("x","");
                e.putString("ActiveDownloads", aDownloads);
                e.commit();
                Log.i("EpisodeDesc:DownloadTask", "New aDownloads: " + aDownloads);
                if(DownloadList.rebuildHeaderThings !=null)
                    DownloadList.rebuildHeaderThings.sendEmptyMessage(0);
                failed++;
                outer_failures=0;

                if(quit)
                    break;
            }
        }
        Log.i("EpisodeDesc:DownloadTask", "Finished Downloading");

        //Wifi lock
        if(DownloadList.Download_wifiLock!=null && DownloadList.Download_wifiLock.isHeld())
            DownloadList.Download_wifiLock.release();

        //Notification
        mNotificationManager.cancel(NOTIFICATION_ID);
        if(Callisto.downloading_count>0)
        {
            Callisto.notification_download = new Notification(R.drawable.ic_action_download, "Finished downloading " + Callisto.downloading_count + " files", NOTIFICATION_ID);
            Callisto.notification_download.setLatestEventInfo(context.getApplicationContext(), "Finished downloading " + Callisto.downloading_count + " files", failed>0 ? (failed + " failed, try them again later") : null, contentIntent);
            Callisto.notification_download.flags = Notification.FLAG_AUTO_CANCEL;
            mNotificationManager.notify(NOTIFICATION_ID, Callisto.notification_download);
            Callisto.current_download=1;
            Callisto.downloading_count=0;
        }
        else
        {
            Callisto.current_download=1;
            Callisto.downloading_count=0;
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result)
    {
        //TODO: Is this even right
        running = false;
        Button streamButton = ((Button)((android.app.Activity)context).findViewById(R.id.stream)),
                downloadButton = ((Button)((android.app.Activity)context).findViewById(R.id.download));
        if(streamButton==null || downloadButton==null)
            return;
        if(result)
        {
            streamButton.setText(Callisto.RESOURCES.getString(R.string.play));
            streamButton.setOnClickListener(((EpisodeDesc)context).launchPlay);
            downloadButton.setText(Callisto.RESOURCES.getString(R.string.delete));
            downloadButton.setOnClickListener(((EpisodeDesc)context).launchDelete);
        } else
        {
            streamButton.setText(Callisto.RESOURCES.getString(R.string.stream));
            streamButton.setOnClickListener(((EpisodeDesc)context).launchStream);
            downloadButton.setText(Callisto.RESOURCES.getString(R.string.download));
            downloadButton.setOnClickListener(((EpisodeDesc)context).launchDownload);
        }
    }
}