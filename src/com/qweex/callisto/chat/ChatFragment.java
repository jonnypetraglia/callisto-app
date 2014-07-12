package com.qweex.callisto.chat;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;

public class ChatFragment extends CallistoFragment {

    String TAG = "Callisto:chat:ChatFragment";

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

    public void connect(String nickname, String username, String real_name, String password) {
        Log.v(TAG, "starting service");
        Intent i = new Intent(master, IrcService.class);
        i.putExtra("nickname", nickname);
        i.putExtra("username", username);
        i.putExtra("real_name", real_name);
        i.putExtra("password", password);
        master.startService(i);
        Log.v(TAG, "started service");
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }
}
