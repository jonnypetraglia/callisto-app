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