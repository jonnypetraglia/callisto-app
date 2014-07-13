package com.qweex.callisto.chat;


import com.qweex.callisto.MasterActivity;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.User;

public class ChannelTabFragment extends TabFragment {
    Channel channel;

    public ChannelTabFragment(MasterActivity master, Channel channel) {
        super(master);
        this.channel = channel;
    }

    @Override
    void send(String msg) {
        channel.sendMessage(msg);
    }
}
