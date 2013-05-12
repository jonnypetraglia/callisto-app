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

package com.qweex.callisto.moar;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.qweex.callisto.R;

/* Add refresh  */

public class jbtitle extends Activity
{
    // body -> wrap/container_12 -> content -> titles -> suggestions_table -> table=sortable/zebra-striped
    String customCSS =
            "#footer, #header nav, .development, .subtitle, clear, .heart { display:none; }\n" +
            "body { width:100%; max-width:100%;}\n" +
            "ul { display:none; }\n" +
            ".push { display:hide; }" +
            ".irc_help { margin-left:1em; margin-right:1em; }\n" +


            ".suggestions_table, .suggestions_table table, .suggestions_table table th, " +
                ".suggestions_table table td, .suggestions_table table th.title, .suggestions_table table td.title " +
                ".suggestions_table table th.votes, .suggestions_table table td.votes " +
                " { width:auto; padding:0; }" +


            "#bot_love\n { text-align:center;  margin:0; padding:0; display:block; vertical-align: bottom; height:2.5em; line-height:4em;}\n" +

            "#titles .suggestions_table { margin:0; padding:0; width:100%; }\n" +

            "#content { padding-bottom:0; }\n" +
            "#wrap #content { min-height:auto; margin:0; }\n" +
            "td { font-size: 0.8em; }\n" +
            "#wrap .footer_push { margin:0: padding:0; height:2.5em; vertical-align: bottom; line-height:4.5em;}\n" +
            "#header h1.logo:hover .heart { opacity:0; }\n" +
            "#header h1.logo .heart { opacity:0; }\n" +
            "";
    WebView wv;
    boolean readyForCSS;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setProgressBarVisibility(true);
        setTitle("");
        customCSS = "<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                    "<style type='text/css'>\n" + customCSS + "\n</style>\n";
        wv = new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(new JavascriptInterface(), "HTMLOUT");
        setContentView(wv);
        wv.setWebChromeClient(wcc);
        wv.setVisibility(View.INVISIBLE);
        ((View)wv.getParent()).setBackgroundColor(0xffffffff);
        wv.loadUrl("http://jbbot.jupitercolony.com:5000/titles");
    }

    WebChromeClient wcc = new WebChromeClient() {
        public void onProgressChanged(WebView view, int progress) {
            boolean der= (readyForCSS && progress!=100) ||
                    (readyForCSS && progress==100);
            Log.e("DSADS " + readyForCSS, progress + "! " + ((readyForCSS ? 5000 : 0) + progress * 50));
            jbtitle.this.setProgress((der ? 5000 : 0) + progress * 50);
            if(progress==100)
            {
                readyForCSS = !readyForCSS;
                if(!!readyForCSS)
                    view.loadUrl("javascript:window.HTMLOUT.CustomCSSApplier(document.getElementsByTagName('html')[0].innerHTML);");
                else
                {
                    wv.setVisibility(View.VISIBLE);
                    //Remove the link in the irc help
                    view.loadUrl("javascript:document.getElementsByClassName('irc_help')[0].children[0].href = '#';");
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, Menu.FIRST, 0, this.getResources().getString(R.string.refresh));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case Menu.FIRST:
                Log.d("DSAD", wv.getUrl() + " " + wv.getOriginalUrl());
                wv.loadUrl("http://jbbot.jupitercolony.com:5000/titles");
            default:
                return true;
        }
    }

    /** Javascript Handler class to handle when we retrieve source of a page and want to apply CSS, as well as retrieving data for the draft feature */
    class JavascriptInterface
    {
        /** Applies the custom CSS by doing JS voodoo and reloading the page.
         * @param result The HTML page source that is passed by the WebViewClient when the page has loaded.
         **/
        @SuppressWarnings("unused")
        public void CustomCSSApplier(String result)
        {
            //Replace Wufoo's CSS with our own
            Log.d("DERDSFAFA", result);
            String str1 = "<link href=\"/css/960.css\" rel=\"stylesheet\">";
            String str2 = "<link href=\"/css/showbot.css?v=5\" rel=\"stylesheet\">";
            String str3 = "<!-- Modernizer -->";
            result = result.replace(str1,"");
            String remove = result.substring(result.indexOf(str3),
                    result.indexOf(str3)+str3.length());

            result = result.replace(remove, customCSS);

            //Load the data into the webview
            Log.d("DERDSFAFA", result);
            wv.setVisibility(View.INVISIBLE);
            wv.loadDataWithBaseURL(wv.getUrl(), result, "text/html", "utf-8", "about:blank");
        }
    }
}
