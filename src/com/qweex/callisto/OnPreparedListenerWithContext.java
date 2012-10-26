package com.qweex.callisto;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.MediaPlayer.OnPreparedListener;

public abstract class OnPreparedListenerWithContext implements OnPreparedListener
{
	Context c;
	public ProgressDialog pd = null;
	public boolean startPlaying = false;

	public void setContext(Context c)
	{
		this.c = c;
	}

	
}