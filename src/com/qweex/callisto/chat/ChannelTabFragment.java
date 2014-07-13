package com.qweex.callisto.chat;


import android.util.Log;
import com.qweex.callisto.MasterActivity;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.User;

public class ChannelTabFragment extends TabFragment {
    String TAG = super.TAG + ":Channel";

    Channel channel;

    public ChannelTabFragment(MasterActivity master, Channel channel) {
        super(master);
        Log.v(TAG, "Creating Channel Fragment");
        this.channel = channel;
    }

    @Override
    void send(String msg) {
        channel.sendMessage(msg);
    }
}
