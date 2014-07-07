package com.qweex.callisto.contact;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class RestoreDraftClient extends WebViewClient
{
    ProgressBar progressBar;

    public RestoreDraftClient(ProgressBar pb)
    {
        progressBar = pb;
    }

    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon)
    {
        view.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageFinished(WebView view, String url)
    {
        String TAG = "Callisto";
        super.onPageFinished(view, url);
        Log.d("Callisto", "Loaded website " + url);
        if(!url.startsWith("http://wufoo.com/")) {
            view.loadUrl("javascript:window.HTMLOUT.CustomCSSApplier(document.documentElement.outerHTML);");
            return;
        }
        view.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

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