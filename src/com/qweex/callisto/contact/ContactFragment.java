package com.qweex.callisto.contact;


import android.os.Bundle;
import android.view.*;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.R;

public class ContactFragment extends CallistoFragment {

    private final String formURL = "https://jblive.wufoo.com/embed/w7x2r7/";
    final int DRAFT_ID = Menu.FIRST;
    private String customCSS = null, customJS = null;
    private WebView wv;
    private ProgressBar pb;
    private AssetReader cssReader, jsReader;
    LinearLayout layout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        if(layout==null) {
            layout = (LinearLayout) inflater.inflate(R.layout.contact, null);

            pb = (ProgressBar) layout.findViewById(R.id.progressBar);
            wv = (WebView) layout.findViewById(R.id.webView);

            wv.getSettings().setJavaScriptEnabled(true);

            if(customCSS==null) {
                cssReader = new AssetReader(getActivity(), "contact.min.css", cssApplier);
                cssReader.execute((Void[]) null);
            }
            if(customJS==null) {
                jsReader = new AssetReader(getActivity(), "contact_draft.min.js", jsApplier);
                jsReader.execute((Void[]) null);
            }
        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }

        return layout;
    }

    AssetReader.Callback cssApplier = new AssetReader.Callback() {
        @Override
        public void call(String css) {
            customCSS = "<style>" + css + "</style>";
            loadPage();
        }
    };

    AssetReader.Callback jsApplier = new AssetReader.Callback() {
        @Override
        public void call(String js) {
            //customJS = "<script type='text/javascript>" + js + "</script>";
            customJS = js;
            loadPage();
        }
    };

    void loadPage() {
        if(customCSS==null || customJS==null || wv==null)
            return;
        wv.addJavascriptInterface(new JavascriptInterface(getActivity(), wv, customCSS), "HTMLOUT");
        wv.setWebViewClient(new RestoreDraftClient(pb));
        wv.loadUrl(formURL);
    }

    @Override
    public void show() {}

    @Override
    public void hide() {
        wv.loadUrl("javascript:" + customJS + ";window.HTMLOUT.saveDraft("+ "tehResult" + ");");
    }
}
