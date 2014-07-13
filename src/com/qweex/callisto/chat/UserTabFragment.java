package com.qweex.callisto.chat;


import com.qweex.callisto.MasterActivity;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.User;

public class UserTabFragment extends TabFragment {
    User user;

    public UserTabFragment(MasterActivity master, User user) {
        super(master);
        this.user = user;
    }

    @Override
    void send(String msg) {
        user.sendMessage(msg);
    }
}
