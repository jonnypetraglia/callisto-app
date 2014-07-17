package com.qweex.callisto.catalog.playback;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;

public class PlaybackFragment extends CallistoFragment {

    RelativeLayout layout;

    /** Constructor; supplies MasterActivity reference. */
    public PlaybackFragment(MasterActivity master) {
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
            layout = (RelativeLayout) inflater.inflate(R.layout.playback, null);
        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        return layout;
    }

    @Override
    public void show() {
        master.getSupportActionBar().setTitle(R.string.playback);
    }

    @Override
    public void hide() {
    }
}
