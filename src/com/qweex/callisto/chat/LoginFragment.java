package com.qweex.callisto.chat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.PrefCache;
import com.qweex.callisto.R;
import com.qweex.utils.ResCache;
import com.qweex.utils.MultiChooserDialog;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class LoginFragment extends CallistoFragment implements MultiChooserDialog.OnChosen {

    String TAG = "Callisto:chat_tab:LoginFragment";

    RelativeLayout layout;
    final int[] advancedControls = new int[] {R.id.username, R.id.real_name, R.id.nickserv_password, R.id.select_channels, R.id.remember_names, R.id.remember_password};
    String[] channelsToJoin;
    static private String[] availableChannels = null;

    /** Constructor; supplies MasterActivity reference. */
    public LoginFragment(MasterActivity master) {
        super(master);
        getAvailableChannels(master);
        // Read "Channels to Join" from preferences"
        Set<String> channs = PrefCache.strSet("chat_channels", R.array.chat_channels_default, null);
        channelsToJoin = channs.toArray(new String[channs.size()]);
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
            layout = (RelativeLayout) inflater.inflate(R.layout.chat_login, null);

            layout.findViewById(R.id.advanced).setOnClickListener(showAdvanced);
            layout.findViewById(R.id.connect).setOnClickListener(connect);

            layout.findViewById(R.id.remember_names).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v;
                    if(!cb.isChecked())
                        ((CheckBox)layout.findViewById(R.id.remember_password)).setChecked(false);
                    layout.findViewById(R.id.remember_password).setEnabled(cb.isChecked());
                }
            });

            layout.findViewById(R.id.connect).setEnabled(channelsToJoin.length>0);

            layout.findViewById(R.id.select_channels).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new MultiChooserDialog(master, ResCache.str(R.string.select_channels), LoginFragment.this, true, availableChannels, channelsToJoin);
                }
            });

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(master);

            String nick = PrefCache.str("chat_nickname", null, null),
                   username = PrefCache.str("chat_username", null, null),
                   real_name = PrefCache.str("chat_real_name", null, null),
                   nickserv_pass = PrefCache.str("chat_nickserv_password", null, null);
            boolean remember_names = PrefCache.bool("chat_remember_names", null, true),
                    remember_password = PrefCache.bool("chat_remember_names", null, remember_names);

            ((TextView)layout.findViewById(R.id.nick)).setText(nick);
            ((TextView)layout.findViewById(R.id.username)).setText(username);
            ((TextView)layout.findViewById(R.id.real_name)).setText(real_name);
            ((TextView)layout.findViewById(R.id.nickserv_password)).setText(nickserv_pass);
            ((CheckBox)layout.findViewById(R.id.remember_names)).setChecked(remember_names);
            ((CheckBox)layout.findViewById(R.id.remember_password)).setChecked(remember_password);

        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        show();
        return layout;
    }

    View.OnClickListener showAdvanced = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            for(int id : advancedControls)
                layout.findViewById(id).setVisibility(View.VISIBLE);
            ((TextView)v).setText(R.string.basic);
            v.setOnClickListener(hideAdvanced);
        }
    };

    View.OnClickListener hideAdvanced = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            for(int id : advancedControls)
                layout.findViewById(id).setVisibility(View.GONE);
            ((TextView)v).setText(R.string.advanced);
            v.setOnClickListener(showAdvanced);
        }
    };

    View.OnClickListener connect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String nickname = ((TextView)layout.findViewById(R.id.nick)).getText().toString();
            if(nickname==null || nickname.length()==0) {
                Toast.makeText(master, R.string.a_nickname_is_required, Toast.LENGTH_SHORT);
                return;
            }
            String username = ((TextView)layout.findViewById(R.id.username)).getText().toString();
            String real_name = ((TextView)layout.findViewById(R.id.real_name)).getText().toString();
            String password = ((TextView)layout.findViewById(R.id.nickserv_password)).getText().toString();

            boolean remember_names = ((CheckBox)layout.findViewById(R.id.remember_names)).isChecked(),
                    remember_password = ((CheckBox)layout.findViewById(R.id.remember_password)).isChecked();

            PrefCache.updateBool("chat_remember_names", remember_names);
            PrefCache.updateBool("chat_remember_password", remember_password);
            PrefCache.updateStr("chat_username", username);
            if(remember_names) {
                PrefCache.updateStr("chat_nickname", nickname);
                PrefCache.updateStr("chat_username", username);
                PrefCache.updateStr("chat_real_name", real_name);
                if(remember_password)
                    PrefCache.updateStr("chat_nickserv_password", username);
                else
                    PrefCache.rm("chat_nickserv_password");

            } else {
                PrefCache.rm("chat_nickname");
                PrefCache.rm("chat_username");
                PrefCache.rm("chat_real_name");
                PrefCache.rm("chat_nickserv_password");
            }

            Log.d(TAG, "Logging in from form");
            master.chatFragment.connect(nickname, username, real_name, password, channelsToJoin);
            master.pushFragment(master.chatFragment, false);
        }
    };

    /** Reads available channels from a JSON file onDemand; can also be called from other Classes to get channels. */
    public static String[] getAvailableChannels(Activity master) {
        if(availableChannels!=null)
            return availableChannels;
        InputStream is = null;
        try {
            is = master.getAssets().open("chat_channels.json");

            JSONArray json = null;
            try {
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                json = new JSONArray(new String(buffer, "UTF-8"));

                availableChannels = new String[json.length()];
                for(int i=0; i<json.length(); ++i)
                    availableChannels[i] = json.getString(i);

            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return availableChannels;
    }

    @Override
    public void show() {
        master.getSupportActionBar().setTitle(R.string.chat);
    }

    @Override
    public void hide() {
    }

    @Override
    public void onChosen(String[] selected) {
        if(selected!=null)
            channelsToJoin = selected;
        layout.findViewById(R.id.connect).setEnabled(channelsToJoin.length>0);
    }
}
