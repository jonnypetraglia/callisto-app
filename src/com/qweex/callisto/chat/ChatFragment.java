package com.qweex.callisto.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.User;

import java.util.HashMap;

public class ChatFragment extends CallistoFragment implements ActionBar.TabListener {

    String TAG = "Callisto:chat_tab:ChatFragment";

    HashMap<String, TabFragment> tabFragments = new HashMap<String, TabFragment>();

    HashMap<IrcConnection, ServerTabFragment> serverTabFragments = new HashMap<IrcConnection, ServerTabFragment>();
    HashMap<Channel, ChannelTabFragment> channelTabFragments = new HashMap<Channel, ChannelTabFragment>();
    HashMap<User, UserTabFragment> userTabFragments = new HashMap<User, UserTabFragment>();

    public IrcServiceAdapter ircConnectionAdapter;

    RelativeLayout layout;
    ListView listview;

    /** Constructor; supplies MasterActivity reference. */
    public ChatFragment(MasterActivity master) {
        super(master);
        ircConnectionAdapter = new IrcServiceAdapter(this);
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
            layout = (RelativeLayout) inflater.inflate(R.layout.chat_tab, null);

            listview = (ListView) layout.findViewById(android.R.id.list);

            master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        show();
        return layout;
    }

    /** Create the service that will house the connection.
     * @param nickname Duh.
     * @param username Duh.
     * @param real_name Duh.
     * @param password Duh.
     * @param channels DUUUUUUUUH.
     */
    public void connect(String nickname, String username, String real_name, String password, String[] channels) {
        IrcService.chatFragment = this;

        Log.v(TAG, "Starting service");
        Intent i = new Intent(master, IrcService.class);
        i.putExtra("nickname", nickname);
        i.putExtra("username", username);
        i.putExtra("real_name", real_name);
        i.putExtra("password", password);
        i.putExtra("channels", channels);
        master.startService(i);
        Log.v(TAG, "Started service");
    }

    @Override
    public void show() {
        //TODO
    }

    @Override
    public void hide() {
        //TODO
    }

    /** Create a server tab.
     * @param server The server to associate with the tab.
     */
    public void createTab(IrcConnection server) {
        Log.v(TAG, "Creating Server tab");

        ActionBar.Tab tab = master.getSupportActionBar().newTab();
        tab.setText(server.getServer().getAddress());
        master.getSupportActionBar().addTab(tab);

        ServerTabFragment tabFragment = new ServerTabFragment(master, server);
        tabFragments.put(server.getServer().getAddress(), tabFragment);
        serverTabFragments.put(server, tabFragment);
    }

    /** Create a channel tab.
     * @param channel The channel to associate with the tab.
     */
    public void createTab(Channel channel) {
        Log.v(TAG, "Creating Channel tab");

        ActionBar.Tab tab = master.getSupportActionBar().newTab();
        tab.setText(channel.getName());
        master.getSupportActionBar().addTab(tab);

        ChannelTabFragment tabFragment = new ChannelTabFragment(master, channel);
        tabFragments.put(channel.getName(), tabFragment);
        channelTabFragments.put(channel, tabFragment);
    }

    /** Create a user (private message) tab.
     * @param user The user object to associate with the tab.
     */
    //TODO: What should this be stored under? Username? Nickname? WHAT?
    public void createTab(User user) {
        Log.v(TAG, "Creating User tab");

        ActionBar.Tab tab = master.getSupportActionBar().newTab();
        tab.setText(user.getNick());
        tab.setTag(user.getHostName());
        master.getSupportActionBar().addTab(tab);

        UserTabFragment tabFragment = new UserTabFragment(master, user);
        tabFragments.put(user.getHostName(), tabFragment);
        userTabFragments.put(user, tabFragment);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        TabFragment tabFragment = tabFragments.get(tab.getTag());
        fragmentTransaction.replace(android.R.id.content, tabFragment);
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        //fragmentTransaction.remove(tabFragments.get(tab.getPosition()));
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }


    /** Receive a message destined for a Server tab
     * @param ircConnection The Server connection.
     * @param ircMessage The message.
     * @param propogate Whether or not it should be propogated to channels.
     */
    public void receive(IrcConnection ircConnection, IrcMessage ircMessage, boolean propogate) {
        serverTabFragments.get(ircConnection).append(ircMessage);

        //TODO: Log here
    }

    /** Receive a message destined for a Server tab
     * @param channel The Server connection.
     * @param ircMessage The message.
     */
    public void receive(Channel channel, IrcMessage ircMessage) {
        channelTabFragments.get(channel).append(ircMessage);

        //TODO: Log here
    }

    /** Receive a message destined for a Server tab
     * @param user The user it is sent from.
     * @param ircMessage The message.
     */
    public void receive(User user, IrcMessage ircMessage) {
        //TODO: Create tab

        userTabFragments.get(user).append(ircMessage);

        //TODO: Log here
    }
}
