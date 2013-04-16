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

import android.widget.*;
import com.andrefy.ddviewlist.DDListView;
import com.qweex.callisto.Callisto;
import com.qweex.callisto.PlayerControls;
import com.qweex.callisto.R;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

/** An activity for showing the queued episodes.
 * @author MrQweex */

public class Queue extends ListActivity
{
    /** The Drag-n-Drop Listview for this activity */
    private DDListView mainListView;
    /** The adapter for the listview */
    private QueueAdapter listAdapter;
    /** The cursor for containing the current queue items */
    private Cursor queue;
    /** The id for dragging and dropping, specifically the "From", i.e. the one that is being dragged */
    private static int FromID = -1;
    /** The progress for the currently downloading file. Used to update. */
    public static ProgressBar currentProgress;

    /** Called when the activity is first created. Sets up the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // Do some create things
        super.onCreate(savedInstanceState);
        setTitle(Callisto.RESOURCES.getString(R.string.queue));
        mainListView = new DDListView(this, null);
        mainListView.setId(android.R.id.list);
        mainListView.setDropListener(mDropListener, R.id.grabber);
        mainListView.setDragListener(mDragListener);
        setContentView(mainListView);
        mainListView.setVerticalFadingEdgeEnabled(false);
        mainListView.setBackgroundColor(getResources().getColor(R.color.backClr));

        //A textview for if there are no results
        TextView noResults = new TextView(this);
        noResults.setBackgroundColor(getResources().getColor(R.color.backClr));
        noResults.setTextColor(getResources().getColor(R.color.txtClr));
        noResults.setText(Callisto.RESOURCES.getString(R.string.list_empty));
        noResults.setTypeface(null, 2);
        noResults.setGravity(Gravity.CENTER_HORIZONTAL);
        noResults.setPadding(10,20,10,20);
        ((ViewGroup)mainListView.getParent()).addView(noResults);
        mainListView.setEmptyView(noResults);

        //Get der queue
        queue = Callisto.databaseConnector.getQueue();

        //Create the adapter
        String[] from = new String[] {"_id", "identity"};
        int[] to = new int[] { R.id.hiddenId, R.id.rowTextView, R.id.rowSubTextView };
        int[] hide = new int[] { R.id.rightTextView, R.id.img };        //Hide stuff from our all-in-one row layout
        listAdapter = new QueueAdapter(this, R.layout.row, queue, from, to, hide);
        mainListView.setAdapter(listAdapter);
    }

    /** Called when it is time to create the menu. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, Menu.FIRST, 0, Callisto.RESOURCES.getString(R.string.clear)).setIcon(R.drawable.ic_action_trash);
        return true;
    }

    /** Called when an item is selected. */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case Menu.FIRST:
                Log.d("Queue:onOptionsItemSelected", "Clearing queue");
                if(Callisto.mplayer!=null)
                    Callisto.mplayer.stop();
                Callisto.databaseConnector.clearQueue();
                listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
                Log.d("Queue:onOptionsItemSelected", "Cursor changed");
                updateNowPlaying(0);
                Callisto.playerInfo.isPaused = true;
                Log.d("Queue:onOptionsItemSelected", "Derp");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** @deprecated
     *  Listener for the up button ("^"). Moves an entry up in the queue. */
    OnClickListener moveUp = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
            long id = Long.parseLong((String) tv.getText());
            Callisto.databaseConnector.move(id, 1);
            updateNowPlaying(id);

            listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
            //NowPlaying.this.listAdapter.notifyDataSetChanged();
        }
    };

    /** @deprecated
     *  Listener for the down button ("v"). Moves an entry down in the queue. */
    OnClickListener moveDown = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
            long id = Long.parseLong((String) tv.getText());
            Callisto.databaseConnector.move(id, -1);
            updateNowPlaying(id);

            listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
        }
    };

    /** Updates the now playing information when messing with the queue (like removing the currently playing item maybe?
     * @param id The ID of the new item */
    void updateNowPlaying(long id)
    {
        if(id==0)
        {
            Callisto.playerInfo.title = null;
            Callisto.playerInfo.show = null;
            Callisto.playerInfo.date = "";
            Callisto.playerInfo.position = 0;
            Callisto.playerInfo.length = 0;
            return;
        }
        Cursor c2 = Callisto.databaseConnector.getOneEpisode(id);
        c2.moveToFirst();
        Callisto.playerInfo.title = c2.getString(c2.getColumnIndex("title"));
        Callisto.playerInfo.show = c2.getString(c2.getColumnIndex("show"));
        Callisto.playerInfo.date = c2.getString(c2.getColumnIndex("date"));
        Callisto.playerInfo.position = c2.getInt(c2.getColumnIndex("position"));
    }

    /** Listener for the remove button ("x"). */
    OnClickListener removeItem = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Log.d("Queue:removeItem", "Clicked to remove an item in the queue");
            TextView tv = (TextView)((View)(v.getParent())).findViewById(R.id.hiddenId);
            long id = Long.parseLong((String) tv.getText());
            long current_id = id;

            Cursor c = Callisto.databaseConnector.currentQueueItem();

            if(c.getCount()==0)
                updateNowPlaying(0);
            else
            {
                c.moveToFirst();
                current_id = c.getLong(c.getColumnIndex("_id"));
                updateNowPlaying(c.getLong(c.getColumnIndex("identity")));
                if(id==current_id)
                {
                    Log.d("Queue:removeItem", "Removing the current item; advancing to next");
                    boolean isPlaying = (Callisto.mplayer!=null && !Callisto.mplayer.isPlaying());
                    PlayerControls.changeToTrack(v.getContext(), 1, !Callisto.playerInfo.isPaused);
                    if(isPlaying)
                    {
                        Log.d("Queue:removeItem", "Track is playing");
                        Callisto.playerInfo.isPaused = true;
                    }
                }
            }
            Callisto.databaseConnector.move(id, 0);
            listAdapter.changeCursor(Callisto.databaseConnector.getQueue());
            Log.d("Queue:removeItem", "Done");
        }
    };

    /** An adapter for the Queue class. Used to update the queue from the SQLite database. */
    public class QueueAdapter extends SimpleCursorAdapter
    {
        /** The cursor containing the items */
        private Cursor c;
        /** The context, used for LayoutInflater */
        private Context context;
        /** What views to hide */
        private int[] Hide;

        public QueueAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int[] hide) {
            super(context, layout, c, from, to);
            this.c = c;
            this.context = context;
            Hide = hide;
        }

        public View getView(int pos, View inView, ViewGroup parent)
        {
            View v = inView;
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.row, parent, false);
            }

            //Set the item height for being expanded and not.
            // Required cause of the drag and drop
            if(mainListView.mItemHeightNormal==-1)   //This will eval to true if it has never been measured yet.
            {
                try {
                    v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    mainListView.mItemHeightNormal = v.getMeasuredHeight();
                    mainListView.mItemHeightHalf = mainListView.mItemHeightNormal / 2;
                    mainListView.mItemHeightExpanded = mainListView.mItemHeightNormal * 2;
                    mainListView.mItemColor = Callisto.RESOURCES.getColor(R.color.backClr);
                    Log.d(":", "MEASURED");
                } catch(NullPointerException e){}
            }

            this.c = getCursor();
            if(this.c.getCount()==0)    //WTF this should never happen
                return v;
            this.c.moveToPosition(pos);

            //Get info for the queue entry
            Long _id = this.c.getLong(c.getColumnIndex("_id"));
            boolean isCurrent = this.c.getLong(c.getColumnIndex("current"))>0;
            boolean isStreaming = this.c.getLong(c.getColumnIndex("streaming"))>0;
            boolean isVideo = this.c.getLong(c.getColumnIndex("video"))>0;
            Long identity = this.c.getLong(c.getColumnIndex("identity"));
            (v.findViewById(R.id.row)).measure(0,0);
            int measuredHeight =(v.findViewById(R.id.row)).getMeasuredHeight();

            //Update the progressbar height
            try {
                Cursor time = Callisto.databaseConnector.getOneEpisode(identity);
                time.moveToFirst();
                int xy = (int) (time.getLong(time.getColumnIndex("position"))*100.0 / Callisto.databaseConnector.getLength(identity));
                ((ProgressBar)v.findViewById(R.id.progress)).setProgress(Double.isNaN(xy) ? 0 : xy);
            } catch(Exception e){
                ((ProgressBar)v.findViewById(R.id.progress)).setProgress(0);
            }
            v.findViewById(R.id.progress).getLayoutParams().height=measuredHeight;
            v.findViewById(R.id.progress).setMinimumHeight(measuredHeight);
            v.findViewById(R.id.progress).invalidate();


            //-------Ok before we were dealing with queue information now we actually deal with the info from the episode itself.
            Cursor c_actualEpisode = Callisto.databaseConnector.getOneEpisode(identity);
            if(c_actualEpisode.getCount()==0)
                return v;
            c_actualEpisode.moveToFirst();
            String title = c_actualEpisode.getString(c_actualEpisode.getColumnIndex("title"));
            String show = c_actualEpisode.getString(c_actualEpisode.getColumnIndex("show"));
            long position = c_actualEpisode.getLong(c_actualEpisode.getColumnIndex("position"));

            //Update the views
            ((TextView)v.findViewById(R.id.rowTextView)).setText(title);
            ((TextView)v.findViewById(R.id.rowSubTextView)).setText(show);
            ((TextView)v.findViewById(R.id.hiddenId)).setText(_id.toString());

            //Listeners
            ImageButton up = ((ImageButton)v.findViewById(R.id.moveUp));
            up.setOnClickListener(moveUp);
            ImageButton down = ((ImageButton)v.findViewById(R.id.moveDown));
            down.setOnClickListener(moveDown);
            ImageButton remove = ((ImageButton)v.findViewById(R.id.remove));
            remove.setOnClickListener(removeItem);

            v.findViewById(R.id.streamingIcon).setVisibility(isStreaming ? View.VISIBLE : View.GONE);
            if(isVideo)
            {
                v.findViewById(R.id.mediaType).setVisibility(View.VISIBLE);
                ((ImageView)v.findViewById(R.id.mediaType)).setImageResource(R.drawable.ic_action_movie);
            }
            else
            {
                v.findViewById(R.id.mediaType).setVisibility(View.GONE);
                ((ImageView)v.findViewById(R.id.mediaType)).setImageResource(R.drawable.ic_action_music_1);
            }

            if(isCurrent)
                currentProgress = ((ProgressBar)v.findViewById(R.id.progress));
            else if(((ProgressBar)v.findViewById(R.id.progress))==currentProgress)
                currentProgress=null;
            Log.i("Queue:QueueAdapter", "isCurrent: " + isCurrent + " / " + currentProgress);

            up.setVisibility(View.GONE);
            down.setVisibility(View.GONE);
            for(int i=0; i<Hide.length; i++)
                v.findViewById(Hide[i]).setVisibility(View.GONE);
            return(v);
        }
    }

    /** Drop Listener for ListView; called when a dragged item is released */
    private DDListView.DropListener mDropListener = new DDListView.DropListener()
    {
        public void drop(int from, int to)
        {
            //to-=(from<to?1:0);
            if(from==to)
                return;
            int toID   = Integer.parseInt(((TextView)mainListView.getChildAt(to - mainListView.getFirstVisiblePosition()).findViewById(R.id.hiddenId)).getText().toString());
            //int fromID = from+1, toID = to+1;

            Callisto.databaseConnector.moveQueue(Queue.FromID, toID+1);
            Toast.makeText(Queue.this,"Moved, pray to the gods it has worked", Toast.LENGTH_SHORT);
            listAdapter.getCursor().requery();
            listAdapter.notifyDataSetChanged();
            Queue.FromID = -1;
        }
    };

    /** Drag Listener for Listview: called when a item is clicked to be dragged */
    private DDListView.DragListener mDragListener = new DDListView.DragListener()
    {
        @Override
        public void drag(int from, int to)
        {
            if(Queue.FromID==-1)
                Queue.FromID = Integer.parseInt(((TextView)mainListView.getChildAt(from - mainListView.getFirstVisiblePosition()).findViewById(R.id.hiddenId)).getText().toString());
        }
    };
}