package com.qweex.callisto;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

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
		Cursor c = Callisto.databaseConnector.currentQueue();
		if(c.getCount()==0)
			return;
		c.moveToFirst();
		long id = c.getLong(c.getColumnIndex("identity"));
		Callisto.databaseConnector.updatePosition(id, 0);
		
		Callisto.changeToTrack(this.c, 1, true);
	}
}
