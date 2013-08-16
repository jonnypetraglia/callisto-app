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

package com.qweex.callisto.listeners;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import com.qweex.callisto.PlayerControls;
import com.qweex.callisto.StaticBlob;
import com.qweex.callisto.podcast.DownloadList;

/** Silly class that just adds a context; on finishing a track it tries to move to the next track */
public class OnCompletionListenerWithContext implements OnCompletionListener
{
	Context c;
	Runnable r;

	public void setContext(Context c)
	{
		this.c = c;
	}
	
	public void setRunnable(Runnable r)
	{
		this.r = r;
	}
	
	@Override
	public void onCompletion(MediaPlayer mp)
    {
        String TAG = StaticBlob.TAG();
		Log.i(TAG, "Playing next track");
		
		if(r!=null)
			r.run();
		
		boolean shouldDelete = PreferenceManager.getDefaultSharedPreferences(this.c).getBoolean("completion_delete", false);

        Cursor c = StaticBlob.databaseConnector.currentQueueItem();
        if(c.getCount()==0)
        {
            Log.e(TAG, "Track completed but queue is empty. wut");
            return;
        }
        c.moveToFirst();
        long identity = c.getLong(c.getColumnIndex("identity"));
        boolean isVideo = c.getInt(c.getColumnIndex("video"))>0;

        StaticBlob.databaseConnector.updatePosition(identity, 0);
		if(shouldDelete)
		    StaticBlob.deleteItem(this.c, identity, isVideo);       //SHOULD automatically advance the queue
	}
}
