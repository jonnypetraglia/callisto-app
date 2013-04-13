package com.qweex.callisto;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;

public class OnErrorListenerWithContext implements OnErrorListener
{
	Context c;

	public void setContext(Context c)
	{
		this.c = c;
	}
	
	

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		System.out.println("Next Track Bug");
		//Callisto.changeToTrack(this.c);
		return true;
	}
	
}