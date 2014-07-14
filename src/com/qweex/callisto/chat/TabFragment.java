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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public abstract class TabFragment extends CallistoFragment {
    String TAG = "Callisto:chat:TabFragment";

    /** Date format for the Log */
    public static final SimpleDateFormat sdfLog = new SimpleDateFormat("yyyy-MM-dd");
    public static final String LOG_SUBDIR = "logs";

    RelativeLayout layout;
    ListView listView;
    EditText inputField;
    ArrayList<IrcMessage> messages = new ArrayList<IrcMessage>();
    ChatListAdapter chatListAdapter;
    private Queue<IrcMessage> msgQueue = new LinkedList<IrcMessage>();

    /** Constructor; supplies MasterActivity reference. */
    public TabFragment(MasterActivity master) {
        super(master);
        Log.v(TAG, "()");
        chatListAdapter = new ChatListAdapter(master, R.layout.chat_line, messages);
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
            listView.setAdapter(chatListAdapter);

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
            v.setText("");
            Log.v(TAG, "Sending Message " +  msg);
            send(msg);
            return true;
        }
    };

    // Run on NOT UI THREAD
    synchronized public void receive(IrcMessage ircMessage) {
        IrcMessage[] messageSplit = ircMessage.splitByNewline();

        Log.d(TAG, ">>>Received '" + ircMessage.toString() + "'");

        Collections.addAll(msgQueue, messageSplit);

        notifyQueue();
        log(ircMessage);
    }

    synchronized public void notifyQueue() {
        master.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(msgQueue==null)
                    return;
                while(!msgQueue.isEmpty()) {
                    messages.add(msgQueue.poll());
                    chatListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    abstract void send(String msg);
    abstract void log(IrcMessage msg);
}
