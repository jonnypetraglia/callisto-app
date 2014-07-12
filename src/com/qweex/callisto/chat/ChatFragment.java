package com.qweex.callisto.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;

public class ChatFragment extends CallistoFragment {

    View layout;

    /** Constructor; supplies MasterActivity reference. */
    public ChatFragment(MasterActivity master) {
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
        /*
        if(layout==null) {
            layout = (LinearLayout) inflater.inflate(R.layout.contact, null);
        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        */
        show();
        return layout;
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }
}
