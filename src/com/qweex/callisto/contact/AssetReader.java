package com.qweex.callisto.contact;

import android.app.Activity;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;

/** An Asyncronous Task to read in the custom CSS file */
public class AssetReader extends AsyncTask<Void, Void, String>
{
    Activity activity;
    String assetToRead;
    Callback callback;

    public AssetReader(Activity act, String asset, Callback call) {
        activity = act;
        assetToRead = asset;
        callback = call;
    }

    @Override
    protected String doInBackground(Void... params) {
        String result;
        InputStream input;

        try {
            input = activity.getAssets().open(assetToRead);
            int size = input.available();
            byte[] buffer = new byte[size];
            input.read(buffer);
            input.close();
            result = new String(buffer);
        }catch(IOException io)
        {
            return null;
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result)
    {
        callback.call(result);
    }

    public abstract static class Callback {
        abstract public void call(String result);
    }
}