package com.qweex.callisto.contact;

import android.app.Activity;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;

/** An Asyncronous Task to read in a file from the /assets folder.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class AssetReader extends AsyncTask<Void, Void, String>
{
    /** Reference to Activity to call getAssets(). */
    Activity activity;
    /** Filename to read. */
    String assetToRead;
    /** Callback to call upon completion. */
    Callback callback;

    /** Constructor.
     *
     * @param act Reference to Activity to call getAssets().
     * @param asset Filename to read.
     * @param call Callback to call upon completion.
     */
    public AssetReader(Activity act, String asset, Callback call) {
        activity = act;
        assetToRead = asset;
        callback = call;
    }

    /**
     * Inherited method; function that actually runs in the background.
     * @param params [not used]
     * @return The resultant string of the file contents.
     */
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

    /** Inherited method; called after async part is finished. */
    @Override
    protected void onPostExecute(String result)
    {
        callback.call(result);
    }

    /** Simple interface for callback. */
    public abstract static class Callback {
        abstract public void call(String result);
    }
}