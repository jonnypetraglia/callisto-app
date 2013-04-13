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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.webkit.WebView;

//NOTE: At first, I had built a native UI for each element and I fetched the "Topics" list from the website.
//	    But it took forever and it required figuring out how to do a POST, plus, if the form ever changed it would probably break.

/** Form to contact the JB team directly from inside Callisto.
 *  It pulls in JB's form directly from wufoo and formats it for mobile.
 * @author MrQweex
 */
public class ContactForm extends Activity
{
    private final String formURL = "https://jblive.wufoo.com/embed/w7x2r7/";
    //private final String formURL = "https://qweex.wufoo.com/embed/m7x3q1/"; //Used for testing.

    private String asyncResult = "";
    private WebView wv;

    /** Called when the activity is first created. Retrieves the wufoo form and inserts it into the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     *  */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        wv = new WebView(this);
        setContentView(wv);
        setTitle(R.string.contact);

        new FetchForm().execute((Void[]) null);
    }

    /** An AsyncTask to fetch the wufoo form. **/
    private class FetchForm extends AsyncTask<Void, Void, Void>
    {
        /** The progress dialog to show whilst fetching the form **/
        private ProgressDialog baconPDialog;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            baconPDialog = Callisto.BaconDialog(ContactForm.this, Callisto.RESOURCES.getString(R.string.loading)  + "...", null);
            baconPDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    //Finish the activity if the fetching is canceled
                    finish();
                }
            });
            baconPDialog.setCancelable(true);
        }

        @Override
        protected void onPostExecute(Void result)
        {
            if(baconPDialog !=null)
                baconPDialog.hide();
        }


        @Override
        protected Void doInBackground(Void... params) {

            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(formURL);

            HttpResponse response;
            try
            {
                // Read in the css
                InputStream input;
                input = getAssets().open("style.css");
                int size = input.available();
                byte[] buffer = new byte[size];
                input.read(buffer);
                input.close();
                asyncResult = new String(buffer);
                asyncResult = "<style type='text/css'>\n" + asyncResult + "\n</style>\n";
                File file = new File(Environment.getExternalStorageDirectory() + File.separator + Callisto.storage_path + File.separator + "contact.html");
                long serverLastModified=0; // = (HttpURLConnection) new URL("https://jblive.wufoo.com/forms/w7x2r7").openConnection().getLastModified();
                long localLastModified = PreferenceManager.getDefaultSharedPreferences(ContactForm.this).getLong("contact_last_modified", -1);

                //TODO: Check the last modified date of the form, if it has changed update and write it to a file.

                //Fetch the file if has changed
                if(serverLastModified>localLastModified || !file.exists() || true)
                {
                    response = httpClient.execute(httpGet, localContext);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    response.getEntity().getContent()
                            )
                    );

                    String line;
                    //BufferedWriter out = new BufferedWriter(new FileWriter(file));
                    while ((line = reader.readLine()) != null)
                        asyncResult += line + "\n";
                    //out.write(asyncResult);
                    //out.close();
                }
                else
                {   //Read the last saved form from the file
                    input = new BufferedInputStream(new FileInputStream(file));
                    size = input.available();
                    buffer = new byte[size];
                    input.read(buffer);
                    input.close();
                    asyncResult = asyncResult + (new String(buffer));
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //End try/catch; if an error happens it will pop out here.

            //For debugging purposes
            if(formURL.contains("qweex") && false)
            {
                String str1 = "<a href=\"https://master.wufoo.com/forms/m7p0x3/def/field1=qweex.wufoo.com/forms/contact-form/\"";
                String str2 = "Report Abuse</a>";
                String remove = asyncResult.substring(asyncResult.indexOf(str1),
                        asyncResult.indexOf(str2)+str2.length());
                asyncResult = asyncResult.replace(remove, "");
            }

            //Remove Wufoo's CSS
            String str1 = "<!-- CSS -->";
            String str2 = "rel=\"stylesheet\">";
            String remove = asyncResult.substring(asyncResult.indexOf(str1),
                    asyncResult.indexOf(str2)+str2.length());
            asyncResult = asyncResult.replace(remove, "");

            //Load the data into the webview
            wv.loadDataWithBaseURL("http://wufoo.com", asyncResult.replaceAll("width:;", "width:100%;").replaceAll("height:;", "height:60%;"), "text/html", "utf-8", "about:blank");
            return null;
        }
    };


    /** Stops the activity from being re-created from  */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //wv.loadDataWithBaseURL("http://wufoo.com", asyncResult.replaceAll("width:;", "width:" + getWindowManager().getDefaultDisplay().getWidth()*.9 + ";").replaceAll("height:;", "height:" + getWindowManager().getDefaultDisplay().getHeight()*.6 + ";"), "text/html", "utf-8", "about:blank");
    }
}
