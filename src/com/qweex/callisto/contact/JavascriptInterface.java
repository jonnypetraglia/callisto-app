package com.qweex.callisto.contact;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;
import com.qweex.callisto.R;

/** Runs Java code when a WebView makes a Javascript function call.
 * Applies some custom CSS to a page after it is loaded & saves a draft.
 *
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class JavascriptInterface
{
    /** Reference to an activity for displaying alerts & saving drafts? */
    Activity activity;
    /** Webview that the interface is attached to. */
    WebView wv;
    /** CSS to apply to the page after it has loaded. */
    String customCSS;

    public JavascriptInterface(Activity act, WebView w, String c) {
        activity = act;
        wv = w;
        customCSS = c;
    }

    /** Applies the custom CSS by doing JS voodoo and reloading the page.
     * @param fullHTML The HTML page source that is passed by the WebViewClient when the page has loaded.
     **/
    @SuppressWarnings("unused")
    public void CustomCSSApplier(String fullHTML)
    {
        //Replace Wufoo's CSS with our own
        String str1 = "<!-- CSS -->";
        String str2 = "rel=\"stylesheet\">";
        int ind1 = fullHTML.indexOf(str1),
        ind2 = fullHTML.indexOf(str2);
        if(ind1<0 || ind2<0)
        {
            new AlertDialog.Builder(activity).setTitle(R.string.contact_form_error)
                    .setMessage(R.string.contact_form_message).setNeutralButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    }).show().setOnDismissListener(
                    new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            //TODO: Finish()
                        }
                    }
            );
            return;
        } else {
            ind2 += str2.length();
            String remove = fullHTML.substring(ind1, ind2);
            fullHTML = fullHTML.replace(remove, customCSS);
        }

        wv.loadDataWithBaseURL("http://wufoo.com", fullHTML.replaceAll("width:;", "width:100%;").replaceAll("height:;", "height:60%;"), "text/html", "utf-8", "about:blank");
    }

    /** Pulls the values from the HTML fields and saves them to Preferences.
     * @param result A JS variable containing the formatted list passed in from the onKeyDown method. Note: NOT just raw HTML.
     */
    @SuppressWarnings("unused")
    public void saveDraft(String result)
    {
        Log.d("Callisto", "Save draft: " + result);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        editor.putString("ContactDraft", result);
        editor.commit();
    }

    // I don't think this is still used but it's hard to tell -_-
    @SuppressWarnings("unused")
    public void saveDraftAndFinish(String result)
    {
        saveDraft(result);
        //finish();
    }
}