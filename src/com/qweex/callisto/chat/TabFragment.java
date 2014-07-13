package com.qweex.callisto.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;
import com.sorcix.sirc.Channel;

import java.util.ArrayList;

public abstract class TabFragment extends CallistoFragment {
    String TAG = "Callisto:chat:TabFragment";

    RelativeLayout layout;
    ListView listView;
    EditText inputField;
    ArrayList<IrcMessage> messages = new ArrayList<IrcMessage>();

    /** Constructor; supplies MasterActivity reference. */
    public TabFragment(MasterActivity master) {
        super(master);
        Log.v(TAG, "()");
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
            Log.v(TAG, "onCreateView");
            layout = (RelativeLayout) inflater.inflate(R.layout.chat_tab, null);
            listView = (ListView) layout.findViewById(android.R.id.list);
            inputField = (EditText) layout.findViewById(android.R.id.edit);

            inputField.setOnEditorActionListener(sendMessage);
        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        show();
        return layout;
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    TextView.OnEditorActionListener sendMessage = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            String msg = v.getText().toString();
            Log.v(TAG, "Sending Message");
            send(msg);
            return false;
        }
    };


    abstract void send(String msg);

    void append(IrcMessage m) {
        messages.add(m);
    }
}
