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

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

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
	public void onCompletion(MediaPlayer mp) {
		Log.i("*:mplayer:onCompletion", "Playing next track");
		
		if(r!=null)
			r.run();
		
		boolean del = PreferenceManager.getDefaultSharedPreferences(this.c).getBoolean("completion_delete", false);
		if(del)
		{
	        File target = new File(Environment.getExternalStorageDirectory(), Callisto.storage_path + File.separator + Callisto.playerInfo.show);
	        target = new File(target,Callisto.playerInfo.date + "__" + Callisto.playerInfo.title + ".mp3");
	        target.delete();
		}
		Cursor c = Callisto.databaseConnector.currentQueueItem();
		if(c.getCount()==0)
			return;
		c.moveToFirst();
		long id = c.getLong(c.getColumnIndex("identity"));
		Callisto.databaseConnector.updatePosition(id, 0);
		
		Callisto.changeToTrack(this.c, 1, true);
	}
}
