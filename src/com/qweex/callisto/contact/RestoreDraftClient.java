package com.qweex.callisto.contact;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/** Is able to detect when a WebView has started/stopped loading.
 * Restores a draft to the contact form.
 *
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class RestoreDraftClient extends WebViewClient
{
    String TAG = "Callisto:Contact:RestoreDraftClient";

    /** ProgressBar to show while loading. */
    //TODO: Is this even used?
    ProgressBar progressBar;

    /** Constructor
     * @param pb ProgressBar to show while loading.
     */
    public RestoreDraftClient(ProgressBar pb)
    {
        progressBar = pb;
    }

    /** Inherited; called when the page has started loading. Hides the webview & shows the progressbar.
     * @param view Webview that is loading.
     * @param url URL that is loading.
     * @param favicon [ASK_SOMEONE_SMARTER]
     */
    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon)
    {
        view.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    /** Inherited; called when the page finishes. Restores the draft.
     * @param view Webview that has finished loading.
     * @param url URL that has finished loading.
     */
    @Override
    public void onPageFinished(WebView view, String url)
    {
        super.onPageFinished(view, url);
        Log.d("Callisto", "Loaded website " + url);

        // JavascriptInterface -after injecting the CSS- will reload the page with a base of wufoo.com
        // If it's still jb.wufoo.com or whatever, there's no need to restore the draft yet.
        if(!url.startsWith("http://wufoo.com/")) {
            view.loadUrl("javascript:window.HTMLOUT.CustomCSSApplier(document.documentElement.outerHTML);");
            return;
        }
        view.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        // Restore the draft
        String draft =  PreferenceManager.getDefaultSharedPreferences(view.getContext()).getString("ContactDraft", null);
        if(draft!=null)
        {
            Log.i(TAG, "Restoring draft.");
            String javascript = "javascript:";
            String element = null, value = null;
            for(String s : draft.split("\\|"))
            {
                element = s.split("=")[0];
                if(element.trim().length()==0)
                    continue;
                if(s.contains("=") && s.split("=").length==2)
                {
                    value = s.split("=")[1];
                    javascript = javascript.concat("document.getElementById('" + element + "').value='" + value + "'; ");
                }
                else
                {
                    javascript = javascript.concat("document.getElementById('" + element + "').checked='true'; ");
                }
                Log.i(TAG, element + " = " + value);
            }
            Log.i(TAG, javascript);
            view.loadUrl(javascript);
            PreferenceManager.getDefaultSharedPreferences(view.getContext()).edit().remove("ContactDraft").commit();
        }
    }
}