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

import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
import com.qweex.callisto.Callisto;
import com.qweex.callisto.PlayerControls;
import com.qweex.callisto.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.qweex.callisto.StaticBlob;

//This activity is for displaying specific information about an episode

/** An activity for showing the specific info about a particular episode.
 * @author MrQweex */
public class EpisodeDesc extends Activity
{
    //-----Local variables-----
    private static final int STOP_ID=Menu.FIRST+1;
    private static final int SHARE_ID=STOP_ID + 1;
    /** Link for the media location */
    private String mp3_link = "", vid_link = "";
    /** The title for the episode */
    private String title = "";
    /** The description for the episode */
    private String description = "";
    /** The link to the page on JB for this specific episode */
    private String link = "";
    /** Size for the media */
    private long mp3_size = 0, vid_size = 0;
    /** The release date for the episode */
    private String date = "";
    /** The show for the episode */
    private String show = "";
    /** The location for the media */
    private File file_location_audio, file_location_video;
    /** The buttons for streaming or downloading the media */
    private Button streamButton, downloadButton;
    /** The ID from the SQL for this episode */
    private long id = 0;
    /** Landscape layout is tweaked slightly */
    private boolean isLandscape;
    /** Makeshift tabs for the different types of media */
    private TextView audioTab, videoTab;
    /** Is the video tab selected? Why am I asking you? You are reading text. You can't respond. */
    private boolean vidSelected = false;

    //-----Static Variables-----
    /** A format that is awesome and does things (for media size) */
    public static final DecimalFormat twoDec = new DecimalFormat("0.00");
    /** Suffixes for -bytes in sizes */
    private static final String[] SUFFIXES = new String[] {"", "K", "M", "G", "T"};
    /** The AsyncTask to download ALL the things in the download queue */
    public static DownloadTask dltask;
    /** Reference to the current EpisodeDesc instance; used to update the buttons by non-user events */
    public static EpisodeDesc currentInstance;

    /** Called when the activity is first created. Sets up the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        String TAG = StaticBlob.TAG();
        /** Create stuff */
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        EpisodeDesc.this.setProgressBarIndeterminateVisibility(false);
        Log.v(TAG, "Launching Activity");
        View info = ((LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.episode, null, false);
        Callisto.build_layout(this, info);

        isLandscape = getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
        //Get the episode ID from extras
        Bundle b = getIntent().getExtras();
        if(b==null)
        {
            Log.e(TAG, "Bundle is null. No extra could be retrieved.");
            Toast.makeText(EpisodeDesc.this, EpisodeDesc.this.getResources().getString(R.string.song_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        id = b.getLong("id", 0);
        //Get a cursor with the episode
        Cursor c = StaticBlob.databaseConnector.getOneEpisode(id);
        if(id==0 || c.getCount()==0)
        {
            Log.e(TAG, "Id is invalid/blank");
            Toast.makeText(EpisodeDesc.this, EpisodeDesc.this.getResources().getString(R.string.song_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Get info
        c.moveToFirst();
        boolean is_new = c.getInt(c.getColumnIndex("new"))>0;
        title = c.getString(c.getColumnIndex("title"));
        date = c.getString(c.getColumnIndex("date"));
        description = c.getString(c.getColumnIndex("description"));
        link = c.getString(c.getColumnIndex("link"));
        mp3_size = c.getLong(c.getColumnIndex("mp3size"));
        mp3_link = c.getString(c.getColumnIndex("mp3link"));
        vid_size = c.getLong(c.getColumnIndex("vidsize"));
        vid_link = c.getString(c.getColumnIndex("vidlink"));
        show = c.getString(c.getColumnIndex("show"));

        //Set ALL the things
        setTitle(title);
        description = android.text.Html.fromHtml(description).toString();
        description = description.replace(String.valueOf((char)0xFFFC),"").trim();		//Relace [obj] in the description
        //Title
        ((TextView)findViewById(R.id.title)).setText(title);
        //Description
        ((TextView)findViewById(R.id.description)).setText(description);
        //Date
        try {
            ((TextView)findViewById(R.id.date)).setText(StaticBlob.sdfDestination.format(StaticBlob.sdfRaw.parse(date)));
        } catch (ParseException e1) {
            Log.e(TAG+":ParseException", "Error parsing a date from the SQLite db: ");
            Log.e(TAG+":ParseException", date);
            Log.e(TAG+":ParseException", "(This should never happen).");
            e1.printStackTrace();
        }
        //Sizes
        ((TextView)findViewById(R.id.audio_size)).setText(formatBytes(mp3_size));
        ((TextView)findViewById(R.id.video_size)).setText(formatBytes(vid_size));


        //-----------File location------------------------
        //Convert the date AGAIN into one that is used for the file path.
        SimpleDateFormat sdfDestination = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = sdfDestination.format(StaticBlob.sdfRaw.parse(date));
        } catch (ParseException e) {
            Log.e(TAG+":ParseException", "Error parsing a date that has already been parsed:");
            Log.e(TAG+":ParseException", date);
            Log.e(TAG+":ParseException", "(This should SERIOUSLY never happen).");
        }
        file_location_audio = new File(StaticBlob.storage_path + File.separator + show);
        file_location_audio = new File(file_location_audio, date + "__" + DownloadList.makeFileFriendly(title) + getExtension(mp3_link));
        if(vid_link!=null)  //Only offer a download for video if one exists.
        {
            file_location_video = new File(StaticBlob.storage_path + File.separator + show);
            file_location_video = new File(file_location_video, date + "__" + DownloadList.makeFileFriendly(title) + getExtension(vid_link));
        }

        streamButton = ((Button)this.findViewById(R.id.stream));
        streamButton.setTextColor(EpisodeDesc.this.getResources().getColor(R.color.txtClr));
        downloadButton = ((Button)this.findViewById(R.id.download));
        downloadButton.setTextColor(EpisodeDesc.this.getResources().getColor(R.color.txtClr));

        //-----------Adjust for Landscape--------------------
        if(isLandscape)
        {
            //1. Rotate it so the orientation is now horizontal
            ((LinearLayout)this.findViewById(R.id.thatWhichIsRotated)).getLayoutParams().width = LayoutParams.FILL_PARENT;
            ((LinearLayout)this.findViewById(R.id.thatWhichIsRotated)).setOrientation(LinearLayout.HORIZONTAL);

            //2. Rotate the buttons too
            ((LinearLayout)this.findViewById(R.id.buttons)).setOrientation(LinearLayout.VERTICAL);
            ((LinearLayout)this.findViewById(R.id.buttons)).findViewById(R.id.stream).getLayoutParams().width = LayoutParams.FILL_PARENT;
            ((LinearLayout)this.findViewById(R.id.buttons)).findViewById(R.id.download).getLayoutParams().width = LayoutParams.FILL_PARENT;

            //3. Move the description to be below the title (instead of below both the title and the MediaBox
            ScrollView desc = (ScrollView) this.findViewById(R.id.scrollView1);
            ((LinearLayout)this.findViewById(R.id.episodeLayout)).removeView(desc);
            ((LinearLayout)this.findViewById(R.id.thatWhichHoldsTheTitle)).addView(desc);

            //4. Set the gravity so that the title and description are greedy
            ((LinearLayout)this.findViewById(R.id.thatWhichHoldsTheTitle)).setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.0f));
            ((LinearLayout)this.findViewById(R.id.mediaBox)).setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 0.0f));

            //5. Set so the MediaBox takes up ALL the space!
            ((LinearLayout)this.findViewById(R.id.mediaBox)).getLayoutParams().height = LayoutParams.FILL_PARENT;
            ((LinearLayout)this.findViewById(R.id.thatWhichIsRotated)).getLayoutParams().height = LayoutParams.FILL_PARENT;

            //6. Adjust the padding so there is more space
            this.findViewById(R.id.headLin).setPadding(0,0,0,0);
        }

        //If the current playing track finishes, redetermine the buttons
        StaticBlob.trackCompleted.setRunnable(new Runnable(){
            public void run() {
                determineButtons();
            }
        });

        //Set 'new?'
        CheckBox rb = ((CheckBox)findViewById(R.id.newImg));
        rb.setChecked(is_new);
        rb.setOnCheckedChangeListener(toggleNew);

        //Set up the tabs
        audioTab = (TextView) findViewById(R.id.audio_tab);
        videoTab = (TextView) findViewById(R.id.video_tab);
        audioTab.setOnClickListener(changeTab);
        videoTab.setOnClickListener(changeTab);

        if(vid_link==null || vid_link.equals(""))
            findViewById(R.id.tabs).setVisibility(View.GONE);
    }

    /** Finds the file extension of a path.
     * @param filename The filename to examine
     * @return The extension of the input file, including the ".".
     */
    public static String getExtension(String filename)
    {
        return filename.substring(filename.lastIndexOf("."));
    }

    /** Called when it is time to create the menu.
     * @param menu Um, the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, STOP_ID, 0, EpisodeDesc.this.getResources().getString(R.string.stop)).setIcon(R.drawable.ic_action_playback_stop);
        menu.add(0, SHARE_ID, 0, EpisodeDesc.this.getResources().getString(R.string.share)).setIcon(R.drawable.ic_action_share);
        return true;
    }

    /** Called when an item in the menu is pressed.
     * @param item The menu item ID that was pressed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case STOP_ID:   //Stop Playing
                PlayerControls.stop(this);
                return true;
            case SHARE_ID:  //Sharing is caring (show the built-in Android dialog
                Intent i=new Intent(android.content.Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject));
                i.putExtra(Intent.EXTRA_TEXT,
                        String.format(getResources().getString(R.string.share_message), show, link));
                startActivity(Intent.createChooser(i, this.getResources().getString(R.string.share)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Called when the activity is resumed, like when you return from another activity or also when it is first created. */
    @Override
    public void onResume()
    {
        String TAG = StaticBlob.TAG();
        super.onResume();
        Log.v(TAG, "Resuming main activity");
        EpisodeDesc.this.setProgressBarIndeterminateVisibility(false);
        StaticBlob.playerInfo.update(EpisodeDesc.this);
        determineButtons();
        if(dltask!=null)
            dltask.context = this;

        currentInstance = this;
    }

    /** Called when the activity is going to be destroyed. */
    @Override
    public void onDestroy()
    {
        String TAG = StaticBlob.TAG();
        super.onDestroy();
        currentInstance = null;
    }

    /** Listener for when the episode's "New" status is toggled. */
    private OnCheckedChangeListener toggleNew = new OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            StaticBlob.databaseConnector.markNew(id, isChecked); }
    };


    /** Listener for when the "play" button is pressed. */
    public OnClickListener launchPlay = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            String TAG = StaticBlob.TAG();
            Log.v(TAG, "Appending item to queue: " + id + " stream: " + false + " vid: " + vidSelected);
            StaticBlob.databaseConnector.appendToQueue(id, false, vidSelected);
            if(StaticBlob.databaseConnector.queueCount()==1)
                PlayerControls.changeToTrack(v.getContext(), 1, true);
            ((Button)v).setText(EpisodeDesc.this.getResources().getString(R.string.enqueued));
            ((Button)v).setEnabled(false);
            StaticBlob.playerInfo.update(v.getContext());
        }
    };

    /** Listener for when the "stream" button is pressed. */
    public OnClickListener launchStream = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            String TAG = StaticBlob.TAG();
            Log.v(TAG, "Appending item to queue: " + id + " stream: " + true + " vid: " + vidSelected);
            StaticBlob.databaseConnector.appendToQueue(id, true, vidSelected);
            if(StaticBlob.databaseConnector.queueCount()==1)
                PlayerControls.changeToTrack(v.getContext(), 1, true);
            ((Button)v).setText(EpisodeDesc.this.getResources().getString(R.string.enqueued));
            ((Button)v).setEnabled(false);
            StaticBlob.playerInfo.update(v.getContext());
        }
    };

    /** Listener for when the "delete" button is pressed. */
    public OnClickListener launchDelete = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {

            StaticBlob.deleteItem(EpisodeDesc.this, id, vidSelected);
            determineButtons();
            StaticBlob.playerInfo.update(v.getContext()); //Update the player controls
        }
    };

    /** Listener for when the "download" button is pressed. */
    public OnClickListener launchDownload = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            String TAG = StaticBlob.TAG();
            //Check for internal storage
            if(!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.no_sd_card)
                        .setMessage(R.string.no_external_storage)
                        .setNegativeButton(android.R.string.ok,null)
                        .create().show();
                Log.w(TAG, "No SD card");
                return;
            }
            //http://www.androidsnippets.com/download-an-http-file-to-sdcard-with-progress-notification

            if(!StaticBlob.databaseConnector.addDownload(id, vidSelected))
                return;

            DownloadTask.downloading_count++;
            Log.i(TAG, "Updated download count: " + DownloadTask.downloading_count);

            //Callisto.download_queue.add(EpisodeDesc.this.id * (vidSelected?-1:1));
            Log.i(TAG, "Adding download: " + (vidSelected ? vid_link : mp3_link));

            if(!DownloadTask.running)
            {
                Log.i(TAG, "Executing downloads");
                dltask = new DownloadTask(EpisodeDesc.this);
                dltask.execute();
            }
            determineButtons();
        }
    };

    /** Listener for when the "cancel" button is pressed. */
    private OnClickListener launchCancel = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            String TAG = StaticBlob.TAG();
            Log.i(TAG, "Removing Download: " + id);
            StaticBlob.databaseConnector.addDownload(id, vidSelected);
            determineButtons();
        }
    };

    /** Listener for when you change tabs. Toggles view visibility and determines buttons */
    private OnClickListener changeTab = new OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if(view==audioTab)
            {
                if(!vidSelected)
                    return;
                videoTab.setBackgroundColor(0xff999999);
                audioTab.setBackgroundColor(0xffcccccc);
                findViewById(R.id.audio_size).setVisibility(View.VISIBLE);
                findViewById(R.id.video_size).setVisibility(View.GONE);
                vidSelected=false;
            }
            else
            {
                if(vidSelected)
                    return;
                videoTab.setBackgroundColor(0xffcccccc);
                audioTab.setBackgroundColor(0xff999999);
                findViewById(R.id.audio_size).setVisibility(View.GONE);
                findViewById(R.id.video_size).setVisibility(View.VISIBLE);
                vidSelected=true;
            }
            determineButtons();
        }
    };

    //TODO: Fix this
    /** Determines the buttons' text and listeners depending on the status of whether the episode has been downloaded already. */
    private void determineButtons()
    {
        File curr = (vidSelected ? file_location_video : file_location_audio);
        long curr_size = (vidSelected ? vid_size : mp3_size);

        //Four factors
        //   * is in DL queue
        //   * exists
        //   * is the right size (i.e. fully downloaded)
        //   * is in the Queue

        // 1. It is in the download queue
        //      buttons:  *Downloading*  Cancel
        if(StaticBlob.databaseConnector.isInDownloadQueue(id, vidSelected))
        {
            streamButton.setText(this.getResources().getString(R.string.downloading));
            streamButton.setEnabled(false);
            downloadButton.setText(this.getResources().getString(R.string.cancel));
            downloadButton.setOnClickListener(launchCancel);
        }
        else if(curr.exists())
        {
            // 2. It is not in the DL queue, it exists, AND it is not the right file size
            //      buttons:  Stream  Delete
            if(curr.length()!=curr_size)
            {
                streamButton.setText(this.getResources().getString(R.string.resume));
                streamButton.setOnClickListener(launchDownload);
            // 3. It is not in the DL queue, it exists, it is the right size, BUT it is in the queue
            //      buttons:  *Enqueued*  Delete
            } else if(StaticBlob.databaseConnector.findInQueue(id, vidSelected).getCount()>0)
            {
                streamButton.setText(this.getResources().getString(R.string.enqueued));
                streamButton.setEnabled(false);
            }
            // 4. It is not in the DL queue, it exists, it is not the right size, and it is not in the queue
            //      buttons:  Play  Delete
            else
            {
                streamButton.setEnabled(true);
                streamButton.setText(this.getResources().getString(R.string.play));
                streamButton.setOnClickListener(launchPlay);
            }
            downloadButton.setText(this.getResources().getString(R.string.delete));
            downloadButton.setOnClickListener(launchDelete);
        }
        // 5. It is not in the DL queue and it does not exist
        //      buttons:  Stream  Download
        else
        {
            streamButton.setEnabled(true);
            streamButton.setText(this.getResources().getString(R.string.stream));
            streamButton.setOnClickListener(launchStream);
            downloadButton.setText(this.getResources().getString(R.string.download));
            downloadButton.setOnClickListener(launchDownload);
        }
    }

    /** Formats a number given in Bytes into a human readable format.
     * @param input The input number to examine in bytes
     * @return A human formatted string, rounded to two decimal places
     */
    public static String formatBytes(long input)
    {
        double temp = input;
        int i;
        for(i=0; temp>5000; i++)
            temp/=1024;
        return (EpisodeDesc.twoDec.format(temp) + " " + EpisodeDesc.SUFFIXES[i] + "B");
    }
}
