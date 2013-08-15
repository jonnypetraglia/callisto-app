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

import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;


//NOTE: At first, I had built a native UI for each element and I fetched the "Topics" list from the website.
//	    But it took forever and it required figuring out how to do a POST, plus, if the form ever changed it would probably break.

/** Form to contact the JB team directly from inside Callisto.
 *  It pulls in JB's form directly from wufoo and formats it for mobile.
 * @author MrQweex
 */
public class ContactForm extends Activity
{
    /** Url for the Wufoo form */
    private final String formURL = "https://jblive.wufoo.com/embed/w7x2r7/";
    //private final String formURL = "https://qweex.wufoo.com/embed/m7x3q1/"; //Used for testing.
    /** Menu ID */
    final int DRAFT_ID = Menu.FIRST;
    /** String to hold the custom CSS */
    private String customCSS;
    /** Main WebView  */
    private WebView wv;
    /** The progress dialog to show whilst fetching the form **/
    private ProgressDialog baconPDialog;
    /*
    Field4 - First      - input type=text
    Field5 - Last       - input type=text
    Field6 - email      - input type=email
    Field7 - topic      - select
    Field1 - message    - textarea
    Field9 - hidden     - input type=hidden
    Field9_0 - radio1   - input type=radio
    Field9_1 - radio2   - input type=radio
     */
    /** Huge-ass Javascript Query to get the values of all the form elements */
    final String JAVASCRIPT_SAVE_DRAFT =
            "var tehResult = '';" +
                    "var inputs = document.getElementsByTagName('input');" +
                    "for(var i=0; i<inputs.length; i++) {" +
                    "   if(inputs[i].getAttribute('type')=='radio') {" +
                    "       if(inputs[i].checked)   tehResult = tehResult + '|' + inputs[i].id;" +
                    "   } else" +
                    "   if(inputs[i].getAttribute('type')!='hidden' && inputs[i].getAttribute('type')!='submit') {" +
                    "       tehResult = tehResult + '|' + inputs[i].id + '=' + inputs[i].value;" +
                    "   }" +
                    "}" +
                    "inputs = document.getElementsByTagName('textarea');" +
                    "for(var i=0; i<inputs.length; i++) {" +
                    "   if(inputs[i].id!='comment')" +
                    "      tehResult = tehResult + '|' + inputs[i].id + '=' + inputs[i].value;" +
                    "}" +
                    "inputs = document.getElementsByTagName('select');" +
                    "for(var i=0; i<inputs.length; i++) {" +
                    "   tehResult = tehResult + '|' + inputs[i].id + '=' + inputs[i].options[inputs[i].selectedIndex].value;" +
                    "}"
            ;
    private ReadCSS thatWhichWillReadTheCSS;


    /** Called when the activity is first created. Retrieves the wufoo form and inserts it into the view.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     *  */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        wv = new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(new JavascriptInterface(), "HTMLOUT");
        wv.setWebViewClient(new RestoreDraftClient());

        FrameLayout fl = new FrameLayout(this);
        fl.setBackgroundResource(R.color.backClr);
        fl.addView(wv);
        setContentView(fl);
        setTitle(R.string.contact);

        baconPDialog = Callisto.BaconDialog(ContactForm.this, this.getResources().getString(R.string.loading)  + "...", null);
        baconPDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //Finish the activity if the fetching is canceled
                finish();
            }
        });
        baconPDialog.setCancelable(true);
        thatWhichWillReadTheCSS = new ReadCSS();
        thatWhichWillReadTheCSS.execute((Void[]) null);

    }

    /** Stops the activity from being re-created from  */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //wv.loadDataWithBaseURL("http://wufoo.com", asyncResult.replaceAll("width:;", "width:" + getWindowManager().getDefaultDisplay().getWidth()*.9 + ";").replaceAll("height:;", "height:" + getWindowManager().getDefaultDisplay().getHeight()*.6 + ";"), "text/html", "utf-8", "about:blank");
    }

    /** Called when the activity is going to be destroyed. */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        wv = null;
        thatWhichWillReadTheCSS.cancel(true);
    }

    /** An Asyncronous Task to read in the custom CSS file */
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
        {
            if(wv==null)
                finish();
            else
                wv.loadUrl(formURL);
        }
    }

    /** Specialized WebViewClient to handle showing a progress dialog and retrieving the page source if it is from wufoo and not data with our custom CSS */
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
                view.loadUrl("javascript:window.HTMLOUT.CustomCSSApplier(document.getElementsByTagName('html')[0].innerHTML);");
            else
            {
                view.setVisibility(View.VISIBLE);
                if(baconPDialog.isShowing())
                    baconPDialog.hide();
            }
        }
    }

    /** A class to restore the draft stored in the preference, if it exists. */
    class RestoreDraftClient extends MyWebViewClient
    {
        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            if(!url.startsWith("http://wufoo.com/")) //In this case the super's method is going to re-load the page to trigger the JS handler
                return;                             //so we don't need to do anything.

            baconPDialog.hide();
            String draft =  PreferenceManager.getDefaultSharedPreferences(ContactForm.this).getString("ContactDraft", null);
            Log.i("ContactForm:RestoreDraftClient", "Restoring draft.");
            if(draft!=null)
            {
                String javascript = "javascript:";
                String element = null, value = null;
                for(String s : draft.split("\\|"))
                {
                    element = s.split("=")[0];
                    if(element.trim().length()==0)
                        continue;
                    System.out.println("ContactForm:S: " + s);
                    if(s.contains("=") && s.split("=").length==2)
                    {
                        value = s.split("=")[1];
                        javascript = javascript.concat("document.getElementById('" + element + "').value='" + value + "'; ");
                    }
                    else
                    {
                        javascript = javascript.concat("document.getElementById('" + element + "').checked='true'; ");
                    }
                    Log.i("ContactForm:RestoreDraftClient", element + " = " + value);
                }
                Log.i("ContactForm:RestoreDraftClient", javascript);
                view.loadUrl(javascript);
                PreferenceManager.getDefaultSharedPreferences(ContactForm.this).edit().remove("ContactDraft").commit();
            }
            Log.i("ContactForm:RestoreDraftClient", "Changing wvClient");
            //view.setWebViewClient(new MyWebViewClient());
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
            if(wv==null)
                return;
            //Replace Wufoo's CSS with our own
            String str1 = "<!-- CSS -->";
            String str2 = "rel=\"stylesheet\">";
            int ind1 = result.indexOf(str1),
                ind2 = result.indexOf(str2);
            if(ind1<0 || ind2<0)
            {
                wv.post(new Runnable() {
                    @Override
                    public void run() {
                        baconPDialog.hide();
                    }
                });
                new AlertDialog.Builder(ContactForm.this).setTitle(R.string.contact_form_error)
                        .setMessage(R.string.contact_form_message).setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        }).show().setOnDismissListener(
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                finish();
                            }
                        }
                );
                return;
            } else {
                ind2 += str2.length();
                String remove = result.substring(ind1, ind2);
                result = result.replace(remove, customCSS);
            }

            //Load the data into the webview
            wv.loadDataWithBaseURL("http://wufoo.com", result.replaceAll("width:;", "width:100%;").replaceAll("height:;", "height:60%;"), "text/html", "utf-8", "about:blank");
        }

        /** Pulls the values from the HTML fields and saves them to Preferences.
         * @param result A JS variable containing the formatted list passed in from the onKeyDown method. Note: NOT just raw HTML.
         */
        @SuppressWarnings("unused")
        public void saveDraft(String result)
        {
            Log.i("ContactForm:JavascriptInterface:saveDraft", "Draft: " + result);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ContactForm.this).edit();
            editor.putString("ContactDraft", result);
            editor.commit();
            Toast.makeText(ContactForm.this, R.string.message_saved_as_draft, Toast.LENGTH_SHORT).show();
        }

        @SuppressWarnings("unused")
        public void saveDraftAndFinish(String result)
        {
            saveDraft(result);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, DRAFT_ID, 0, R.string.save_draft).setIcon(R.drawable.ic_action_inbox);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case DRAFT_ID:
                wv.loadUrl("javascript:" + JAVASCRIPT_SAVE_DRAFT + ";window.HTMLOUT.saveDraft("+ "tehResult" + ");");
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event)  {
        if(keyCode == android.view.KeyEvent.KEYCODE_BACK && PreferenceManager.getDefaultSharedPreferences(ContactForm.this).getBoolean("contact_draft", true))
        {
            wv.loadUrl("javascript:" + JAVASCRIPT_SAVE_DRAFT + ";window.HTMLOUT.saveDraftAndFinish("+ "tehResult" + ");");
        }
        return super.onKeyDown(keyCode, event);
    }
}
