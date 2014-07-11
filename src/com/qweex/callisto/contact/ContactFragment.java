package com.qweex.callisto.contact;


import android.os.Bundle;
import android.view.*;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;


/** This fragment displays a customized Wufoo contact form.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class ContactFragment extends CallistoFragment {

    /** URL for Wufoo form. */
    private final String formURL = "https://jblive.wufoo.com/embed/w7x2r7/";
    /** Variables to hold the contents of the files in /assets. */
    private String customCSS = null, customJS = null;
    /** Webview that will contain the form. */
    private WebView wv;
    /** ProgressBar to display while page is loading. */
    private ProgressBar pb;
    LinearLayout layout;

    /** Inherited constructor
     * @param master Reference to MasterActivity
     */
    public ContactFragment(MasterActivity master) {
        super(master);
    }

    /** Inherited method; called each time the fragment is attached to a FragmentActivity.
     * @param inflater Used for instantiating the fragment's view.
     * @param container [ASK_SOMEONE_SMARTER]
     * @param savedInstanceState [ASK_SOMEONE_SMARTER]
     * @return The new / recycled View to be attached.
     */
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
                AssetReader cssReader = new AssetReader(getActivity(), "contact.min.css", cssApplier);
                cssReader.execute((Void[]) null);
            }
            if(customJS==null) {
                AssetReader jsReader = new AssetReader(getActivity(), "contact_draft.min.js", jsApplier);
                jsReader.execute((Void[]) null);
            }
        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        show();
        return layout;
    }

    /** Callback for AssetReader when the css file has been read. */
    AssetReader.Callback cssApplier = new AssetReader.Callback() {
        @Override
        public void call(String css) {
            customCSS = "<style>" + css + "</style>";
            loadPage();
        }
    };

    /** Callback for AssetReader when the javascript file has been read. */
    AssetReader.Callback jsApplier = new AssetReader.Callback() {
        @Override
        public void call(String js) {
            //customJS = "<script type='text/javascript>" + js + "</script>";
            customJS = js;
            loadPage();
        }
    };

    /** Attempt to load the page after the assets have been read. */
    void loadPage() {
        if(customCSS==null || customJS==null || wv==null)
            return;
        wv.addJavascriptInterface(new JavascriptInterface(getActivity(), wv, customCSS), "HTMLOUT");
        wv.setWebViewClient(new RestoreDraftClient(pb));
        wv.loadUrl(formURL);
    }

    /** Inherited method; things to do when the fragment is shown initially. */
    @Override
    public void show() {
        master.getSupportActionBar().setTitle(R.string.contact);
    }

    /** Inherited method; things to do when the fragment is hidden/dismissed. */
    @Override
    public void hide() {
        wv.loadUrl("javascript:" + customJS + ";window.HTMLOUT.saveDraft("+ "tehResult" + ");");
    }
}
