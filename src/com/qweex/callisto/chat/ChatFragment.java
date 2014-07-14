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
import android.widget.Toast;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.User;

import java.util.HashMap;

/** Houses & manages the TabFragments.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class ChatFragment extends CallistoFragment {

    String TAG = "Callisto:chat:ChatFragment";

    /** Adapter that manages receiving the messages directly from the sirc library. */
    IrcServiceAdapter ircConnectionAdapter;

    /** Views. VIEWS! */
    RelativeLayout layout;
    ListView listview;

    /** Lists of fragments */
    HashMap<IrcConnection, ServerTabFragment> serverTabs = new HashMap<IrcConnection, ServerTabFragment>();
    HashMap<Channel, ChannelTabFragment> channelTabs = new HashMap<Channel, ChannelTabFragment>();
    HashMap<User, UserTabFragment> userTabs = new HashMap<User, UserTabFragment>();

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
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    @Override
    public void hide() {
        //TODO
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////// Create a tab ////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    /** Create a server tab.
     * @param server The server to associate with the tab.
     */
    public void createTab(final IrcConnection server) {
        Log.v(TAG, "Creating Server tab");
        final ServerTabFragment tabFragment = new ServerTabFragment(master, server);
        serverTabs.put(server, tabFragment);

        master.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Creating Server tab on UI thread");
                ActionBar.Tab tab = master.getSupportActionBar().newTab();
                tab.setText(server.getServerAddress());
                tab.setTag(server);
                tab.setTabListener(new ChatTabListener(tabFragment));
                master.getSupportActionBar().addTab(tab);

                // Check for any waiting messages that arrived before the GUI was ready
                serverTabs.get(server).notifyQueue();
            }
        });
    }

    /** Create a channel tab.
     * @param channel The channel to associate with the tab.
     */
    public void createTab(IrcConnection server, final Channel channel) {
        Log.v(TAG, "Creating Channel tab");
        final ChannelTabFragment tabFragment = new ChannelTabFragment(master, server, channel);
        channelTabs.put(channel, tabFragment);

        master.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Creating Channel tab on UI thread");

                ActionBar.Tab tab = master.getSupportActionBar().newTab();
                tab.setText(channel.getName());
                tab.setTag(channel);
                tab.setTabListener(new ChatTabListener(tabFragment));
                master.getSupportActionBar().addTab(tab);

                // Check for any waiting messages that arrived before the GUI was readys
                channelTabs.get(channel).notifyQueue();
            }
        });
    }

    /** Create a user (private message) tab.
     * @param user The user object to associate with the tab.
     */
    public void createTab(IrcConnection server, final User user) {
        Log.v(TAG, "Creating User tab");
        final UserTabFragment tabFragment = new UserTabFragment(master, server, user);
        userTabs.put(user, tabFragment);

        master.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Creating User tab on UI thread");

                ActionBar.Tab tab = master.getSupportActionBar().newTab();
                tab.setText(user.getNick());
                tab.setTag(user);
                tab.setTabListener(new ChatTabListener(tabFragment));
                master.getSupportActionBar().addTab(tab);

                // Check for any waiting messages that arrived before the GUI was ready
                userTabs.get(user).notifyQueue();
            }
        });


    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////// Receiving messages & passing them on to their fragments //////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    /** Receive a message destined for a Server tab
     * @param server The Server connection.
     * @param ircMessage The message.
     * @param propogate Whether or not it should be propogated to channels.
     */
    public void receive(IrcConnection server, IrcMessage ircMessage, boolean propogate) {
        Log.d(TAG, "Received");
        Log.d(TAG, " comes from server: " + server.getServer().getAddress());
        Log.d(TAG, " message is " + ircMessage.toString());

        serverTabs.get(server).receive(ircMessage);
    }

    /** Receive a message destined for a Server tab
     * @param channel The Server connection.
     * @param ircMessage The message.
     */
    public void receive(Channel channel, IrcMessage ircMessage) {
        Log.d(TAG, "Received");
        Log.d(TAG, " comes from channel: " + channel.getName());
        Log.d(TAG, " message is " + ircMessage.toString());

        channelTabs.get(channel).receive(ircMessage);
    }

    /** Receive a message destined for a Server tab
     * @param user The user it is sent from.
     * @param ircMessage The message.
     */
    public void receive(User user, IrcMessage ircMessage) {
        Log.d(TAG, "Received");
        Log.d(TAG, " comes from user: " + user.getNick());
        Log.d(TAG, " message is " + ircMessage.toString());

        //TODO: Create tab if it doesn't exist ???
        userTabs.get(user).receive(ircMessage);
    }

    public void handleError(Exception e) {
        Log.e(TAG, "ERROR: " + e.toString());
        e.printStackTrace();
        Toast.makeText(master, e.getMessage(), Toast.LENGTH_SHORT);
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////// Classes ///////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////

    /** Listener for when the Tab changes */
    class ChatTabListener implements ActionBar.TabListener {
        TabFragment mFragment;
        public ChatTabListener(TabFragment fragment) {
            mFragment = fragment;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.add(R.id.main_fragment, mFragment);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.remove(mFragment);
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            Toast.makeText(master, "Reselected!", Toast.LENGTH_SHORT).show();
        }
    }
}
