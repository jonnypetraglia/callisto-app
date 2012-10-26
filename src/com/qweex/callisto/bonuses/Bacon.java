package com.qweex.callisto.bonuses;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.webkit.WebView;

public class Bacon extends Activity {
	MediaPlayer player;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		WebView w = new WebView(this);
		w.loadDataWithBaseURL("file:///android_asset/", "<img style='width:100%' src='baconlove.gif' />", "text/html", "utf-8", null);
		setContentView(w);
		
		try {
		player = new MediaPlayer();
        AssetFileDescriptor afd = getAssets().openFd("gangnam.mid");
        player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
        player.prepare();
        player.start();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		player.reset();
		finish();
	}
}
