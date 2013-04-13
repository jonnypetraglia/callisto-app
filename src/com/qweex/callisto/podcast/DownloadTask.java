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
package com.qweex.callisto.podcast;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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

    private String Title, Date, Link, Show;
    private long TotalSize;
    private File Target;
    private final int NOTIFICATION_ID = 3696;
    private final int TIMEOUT_CONNECTION = 5000;
    private final int TIMEOUT_SOCKET = 30000;
    private NotificationManager mNotificationManager;
    private PendingIntent contentIntent;
    public static boolean running = false;
    private EpisodeDesc context;

    public DownloadTask(EpisodeDesc c)
    {
        super();
        context = c;
    }

    @Override
    protected void onPreExecute()
    {
        running = true;
        Log.i("EpisodeDesc:DownloadTask", "Beginning downloads");
        Intent notificationIntent = new Intent(context, DownloadList.class);
        contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Callisto.notification_download = new Notification(R.drawable.callisto, Callisto.RESOURCES.getString(R.string.beginning_download), System.currentTimeMillis());
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


        Log.e("YO DAWG:", "Preparing to do sum dlinng");
        Log.e("YO DAWGz:", PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", ""));
        while(!PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", "").equals(""))
        {
            try
            {
                String dlList = PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", "");
                Log.e("YO DAWG:", "DL LIST: " + dlList);
                long id = Long.parseLong(dlList.substring(1, dlList.indexOf('|', 1)));
                if(id<=0)
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
                Link = current.getString(current.getColumnIndex(isVideo ? "vidlink" : "mp3link"));
                Title = current.getString(current.getColumnIndex("title"));
                Date = current.getString(current.getColumnIndex("date"));
                Show = current.getString(current.getColumnIndex("show"));
                Log.i("EpisodeDesc:DownloadTask", "Starting download: " + Link);
                Date = Callisto.sdfFile.format(Callisto.sdfRaw.parse(Date));


                Target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + Show);
                Target.mkdirs();
                if(Title.indexOf("|")>0)
                    Title = Title.substring(0, Title.indexOf("|"));
                Title=Title.trim();
                Target = new File(Target, Date + "__" + DownloadList.makeFileFriendly(Title) + EpisodeDesc.getExtension(Link));

                URL url = new URL(Link);
                HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
                ucon.setReadTimeout(TIMEOUT_CONNECTION);
                ucon.setConnectTimeout(TIMEOUT_SOCKET);


                InputStream is = ucon.getInputStream();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
                FileOutputStream outStream;
                byte buff[];
                if(Target.exists())
                {
                    inStream.skip(Target.length());
                    outStream = new FileOutputStream(Target, true);
                }
                else
                    outStream = new FileOutputStream(Target);
                buff = new byte[5 * 1024];
                TotalSize = ucon.getContentLength();
                int len = 0;
                long downloadedSize = Target.length(),
                        perc = 0;

                int SPD_COUNT = 200,
                        dli = 0;
                long lastTime = (new java.util.Date()).getTime(),
                        all_spds = 0;
                double avg_speed = 0;
                DecimalFormat df = new DecimalFormat("#.##");

                //Here is where the actual downloading happens
                while (len != -1)
                {
                    Log.i("EpisodeDesc:DownloadTask", "DERP: " + downloadedSize);
                    dlList = PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", "");
                    if(dlList.equals("") || !(Long.parseLong(dlList.substring(1,dlList.indexOf('|',1)))==id))
                    {
                        Log.i("EpisodeDesc:DownloadTask", "DERPDADSADSA");
                        Target.delete();
                        break;
                    }

                    try {
                        len = inStream.read(buff);
                        if(len==-1)
                            break;


                        outStream.write(buff,0,len);
                        downloadedSize += len;
                        perc = downloadedSize*100;
                        perc /= TotalSize;

                        //Add to the average speed
                        long temp_spd = 0;
                        long time_diff = ((new java.util.Date()).getTime() - lastTime);
                        if(time_diff>0)
                        {
                            temp_spd= len*100/time_diff;
                            dli++;
                        }

                        all_spds += temp_spd;
                        lastTime = (new java.util.Date()).getTime();

                        //If the time is right, do it!
                        if(dli==SPD_COUNT)
                        {
                            dli = 0;
                            avg_speed = all_spds*1.0/SPD_COUNT/100;
                            Log.e("BACON", temp_spd + "->" + all_spds + " == " + avg_speed);
                            all_spds = 0;

                            if(DownloadList.downloadProgress!=null)
                            {
                                int x = (int)(downloadedSize*100/TotalSize);
                                DownloadList.downloadProgress.setMax((int)(TotalSize/1000));
                                DownloadList.downloadProgress.setProgress((int)(downloadedSize/1000));
                            }
                            Callisto.notification_download.setLatestEventInfo(context.getApplicationContext(),
                                    Callisto.RESOURCES.getString(R.string.downloading) + " " +
                                            Callisto.current_download + " " +
                                            Callisto.RESOURCES.getString(R.string.of) + " " +
                                            Callisto.downloading_count + ": " + perc + "%  (" +
                                            df.format(avg_speed) + "kb/s)",
                                    Show + ": " + Title, contentIntent);
                            mNotificationManager.notify(NOTIFICATION_ID, Callisto.notification_download);
                        }
                    } catch (IOException e) {
                        Log.e("EpisodeDesc:DownloadTask:IOException", "IO is a moon");
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {}
                    } catch (Exception e) {
                        Log.e("EpisodeDesc:DownloadTask:??Exception", e.getClass() + " : " + e.getMessage());
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {}
                    }
                }

                dlList = PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", "");
                if(!dlList.equals("") && (Long.parseLong(dlList.substring(1,dlList.indexOf('|',1)))==id))
                {
                    Callisto.current_download++;

                    outStream.flush();
                    outStream.close();
                    inStream.close();
                    Log.i("EpisodeDesc:DownloadTask", "Successfully downloaded to : " + Target.getPath());
                    boolean queue = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("download_to_queue", false);
                    if(queue)
                        Callisto.databaseConnector.appendToQueue(id, false, true==true);//vidSelected);

                    //Move the download from active to completed.
                    SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    String aDownloads = PreferenceManager.getDefaultSharedPreferences(context).getString("ActiveDownloads", "");
                    aDownloads = aDownloads.replace("|" + Long.toString(id) + "|", "|");
                    if(aDownloads.equals("|"))
                        aDownloads="";
                    e.putString("ActiveDownloads", aDownloads);
                    String cDownloads = PreferenceManager.getDefaultSharedPreferences(context).getString("CompletedDownloads", "");
                    if(cDownloads.equals(""))
                        cDownloads = "|";
                    cDownloads = "|" + Long.toString(id) + cDownloads;
                    e.putString("CompletedDownloads", cDownloads);
                    e.commit();

                    if(DownloadList.notifyUpdate!=null)
                        DownloadList.notifyUpdate.sendEmptyMessage(0);
                }
            }catch (ParseException e) {
                Log.e("EpisodeDesc:DownloadTask:ParseException", "Error parsing a date from the SQLite db: ");
                Log.e("EpisodeDesc:DownloadTask:ParseException", Date);
                Log.e("EpisodeDesc:DownloadTask:ParseException", "(This should never happen).");
                e.printStackTrace();
            } catch(Exception e) {
                Log.e("EEEEEEEE " + e.getClass(), "Msg: " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {}
            }
        }
			/*//IDEA: Change intent upon download completion?
			notificationIntent = new Intent(null, Callisto.class);
			contentIntent = PendingIntent.getActivity(EpisodeDesc.this, 0, notificationIntent, 0);
			*/

        if(DownloadList.Download_wifiLock.isHeld())
            DownloadList.Download_wifiLock.release();
        Log.i("EpisodeDesc:DownloadTask", "Finished Downloading");
        mNotificationManager.cancel(NOTIFICATION_ID);
        if(Callisto.downloading_count>0)
        {
            Callisto.notification_download = new Notification(R.drawable.callisto, "Finished downloading " + Callisto.downloading_count + " files", NOTIFICATION_ID);
            Callisto.notification_download.setLatestEventInfo(context.getApplicationContext(), "Finished downloading " + Callisto.downloading_count + " files", null, contentIntent);
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
        running = false;
        Button streamButton = ((Button)((android.app.Activity)context).findViewById(R.id.stream)),
               downloadButton = ((Button)((android.app.Activity)context).findViewById(R.id.download));
        if(result)
        {
            streamButton.setText(Callisto.RESOURCES.getString(R.string.play));
            streamButton.setOnClickListener(context.launchPlay);
            downloadButton.setText(Callisto.RESOURCES.getString(R.string.delete));
            downloadButton.setOnClickListener(context.launchDelete);
        } else
        {
            streamButton.setText(Callisto.RESOURCES.getString(R.string.stream));
            streamButton.setOnClickListener(context.launchStream);
            downloadButton.setText(Callisto.RESOURCES.getString(R.string.download));
            downloadButton.setOnClickListener(context.launchDownload);
        }
    }
}