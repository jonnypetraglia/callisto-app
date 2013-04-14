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

import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
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

    /** String to hold the result thingy */
    private String asyncResult = "";
    /** String to hold the custom CSS */
    private String customCSS;
    /** Main WebView  */
    private WebView wv;
    /** The progress dialog to show whilst fetching the form **/
    private ProgressDialog baconPDialog;

    /** Called when the activity is first created. Retrieves the wufoo form and inserts it into the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     *  */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        wv = new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(new MyIncompleteFormHandler(), "HTMLOUT");
        wv.setWebViewClient(new MyWebViewClient());
        FrameLayout fl = new FrameLayout(this);
        fl.setBackgroundResource(R.color.backClr);
        fl.addView(wv);
        setContentView(fl);
        setTitle(R.string.contact);

        baconPDialog = Callisto.BaconDialog(ContactForm.this, Callisto.RESOURCES.getString(R.string.loading)  + "...", null);
        baconPDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //Finish the activity if the fetching is canceled
                finish();
            }
        });
        baconPDialog.setCancelable(true);
        new ReadCSS().execute((Void[]) null);

    }

    /** Stops the activity from being re-created from  */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //wv.loadDataWithBaseURL("http://wufoo.com", asyncResult.replaceAll("width:;", "width:" + getWindowManager().getDefaultDisplay().getWidth()*.9 + ";").replaceAll("height:;", "height:" + getWindowManager().getDefaultDisplay().getHeight()*.6 + ";"), "text/html", "utf-8", "about:blank");
    }

    private class ReadCSS extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params) {
            // Read in the css
            InputStream input;
            try {
                input = getAssets().open("style.css");
                int size = input.available();
                byte[] buffer = new byte[size];
                input.read(buffer);
                input.close();
                customCSS = new String(buffer);
                customCSS = "<style type='text/css'>\n" + customCSS + "\n</style>\n";
            }catch(IOException io)
            {
                finish();   //TODO: ???
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v)
        {   wv.loadUrl(formURL);   }
    }

    class MyWebViewClient extends WebViewClient
    {
        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon)
        {
            view.setVisibility(View.INVISIBLE);
            baconPDialog.show();
        }
        @Override
        public void onPageFinished(WebView view, String url)
        {
            Log.d("derp", "Loading: " + url);
            if(!url.startsWith("http://wufoo.com/"))
                view.loadUrl("javascript:window.HTMLOUT.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
            else
            {
                view.setVisibility(View.VISIBLE);
                if(baconPDialog.isShowing())
                    baconPDialog.hide();
            }
        }

    }

    class MyIncompleteFormHandler
    {
        @SuppressWarnings("unused")
        public void processHTML(String result)
        {
            //Replace Wufoo's CSS with our own
            String str1 = "<!-- CSS -->";
            String str2 = "rel=\"stylesheet\">";
            String remove = result.substring(result.indexOf(str1),
                    result.indexOf(str2)+str2.length());
            result = result.replace(remove, customCSS);

            //Load the data into the webview
            wv.loadDataWithBaseURL("http://wufoo.com", result.replaceAll("width:;", "width:100%;").replaceAll("height:;", "height:60%;"), "text/html", "utf-8", "about:blank");
        }
    }
}
