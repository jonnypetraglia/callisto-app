package com.qweex.callisto.catalog;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.qweex.callisto.R;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

public class CatalogAdapter extends SimpleCursorAdapter
{
    private Cursor cursor;
    private Context context;
    private int layout_id;

    public CatalogAdapter(Context context, int layout, Cursor cursor) {
        super(context, layout, cursor, new String[] {}, new int[] {});
        this.cursor = cursor;
        this.layout_id = layout;
        this.context = context;
    }

    public View getView(int pos, View inView, ViewGroup parent)
    {
        View v = inView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(layout_id, null);
        }
        cursor = getCursor();
        cursor.moveToPosition(pos);

        TextView title = (TextView) v.findViewById(R.id.title),
                 date  = (TextView) v.findViewById(R.id.date);

        /*
        Date tempDate = new Date(); //We use this variable to get thisYear as well as parsing the actual date later
        int thisYear = tempDate.getYear();      //If the date for the show is this year, no need to show the year
        //Set the data From->To

        //Get info for selected episode
        long id = this.c.getLong(this.c.getColumnIndex("_id"));
        this.c = StaticBlob.databaseConnector.getOneEpisode(id);
        this.c.moveToFirst();
        String date = this.c.getString(this.c.getColumnIndex("date"));
        String title = this.c.getString(this.c.getColumnIndex("title"));
        String mp3_link = this.c.getString(this.c.getColumnIndex("mp3link"));
        String vid_link = this.c.getString(this.c.getColumnIndex("vidlink"));

        //_id
        ((TextView) v.findViewById(R.id.hiddenId)).setText(Long.toString(id));
        //title
        ((TextView) v.findViewById(R.id.rowTextView)).setText(title);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)v.findViewById(R.id.rowTextView).getLayoutParams();
        lp.setMargins((int)(10*StaticBlob.DP), lp.topMargin, lp.rightMargin, lp.bottomMargin);;
        //date
        String d = date;
        try {
            tempDate = StaticBlob.sdfRaw.parse(d);
            if(tempDate.getYear()==thisYear)
                d = StaticBlob.sdfHuman.format(tempDate);
            else
                d = StaticBlob.sdfHumanLong.format(tempDate);
            //d = Callisto.sdfDestination.format();
        } catch (ParseException e) {
            Log.e(TAG + ":ParseException", "Error parsing a date from the SQLite db: ");
            Log.e(TAG+":ParseException", d);
            Log.e(TAG+":ParseException", "(This should never happen).");
            e.printStackTrace();
        }
        ((TextView) v.findViewById(R.id.rowSubTextView)).setText(d);

        File music_file_location = null, video_file_location = null;
        try {
            music_file_location = new File(StaticBlob.storage_path + File.separator + currentShow);
            music_file_location = new File(music_file_location, StaticBlob.sdfFile.format(tempDate) + "__" + StaticBlob.makeFileFriendly(title) + EpisodeDesc.getExtension(mp3_link));
        }catch(NullPointerException npe) {
            Log.e(TAG, "Null pointer when determining file status: Audio");
        }
        try {
            video_file_location = new File(StaticBlob.storage_path + File.separator + currentShow);
            video_file_location = new File(video_file_location, StaticBlob.sdfFile.format(tempDate) + "__" + StaticBlob.makeFileFriendly(title) + EpisodeDesc.getExtension(vid_link));
        }catch(NullPointerException npe) {
            Log.e(TAG, "Null pointer when determining file status: Video");
        }

        runOnUiThread(new updateBoldOrItalic(id, v, music_file_location, video_file_location, this.c.getLong(this.c.getColumnIndex("mp3size")), this.c.getLong(this.c.getColumnIndex("vidsize"))));


        // Mark current queue item
        Cursor c = StaticBlob.databaseConnector.currentQueueItem();
        if(currentQueueItem==null || currentQueueItem == v)
        {
            if(c.getCount()>0)
            {
                c.moveToFirst();
                if(c.getLong(c.getColumnIndex("identity"))==id)
                    currentQueueItem = v;
                else
                    currentQueueItem = null;
            }
            else
                currentQueueItem = null;
        }
        // Mark current download item
        c = StaticBlob.databaseConnector.getActiveDownloads();
        if(currentDownloadItem==null || currentDownloadItem == v)
        {
            if(c.getCount()>0)
            {
                c.moveToFirst();
                Log.i(TAG, "  currentDownloadItem =? " + currentDownloadItem + "   " + c.getLong(c.getColumnIndex("identity")) + " == " + id);
                if(c.getLong(c.getColumnIndex("identity"))==id)
                    currentDownloadItem = v;
                else
                    currentDownloadItem = null;
            }
            else
                currentDownloadItem = null;
        }
        Log.i(TAG, "currentDownloadItem == " + currentDownloadItem);

        //Hide the specific views
        int[] hide = new int[] { R.id.remove, R.id.progress, R.id.grabber, R.id.rightTextView};
        for(int i=0; i<hide.length; i++)
            v.findViewById(hide[i]).setVisibility(View.GONE);

        //Check the Jupiter icon if it is new
        boolean is_new = this.c.getInt(this.c.getColumnIndex("new"))>0;
        CheckBox rb = ((CheckBox)v.findViewById(R.id.img));
        rb.setChecked(is_new);
        rb.setOnCheckedChangeListener(toggleNew);
//*/
        return(v);
    }
}