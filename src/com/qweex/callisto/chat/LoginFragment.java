package com.qweex.callisto.chat;

import android.os.Bundle;
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
import com.qweex.callisto.R;
import com.qweex.callisto.ResCache;
import com.qweex.utils.MultiChooserDialog;

public class LoginFragment extends CallistoFragment implements MultiChooserDialog.OnChosen {

    String TAG = "Callisto:chat_tab:LoginFragment";

    RelativeLayout layout;
    final int[] advancedControls = new int[] {R.id.username, R.id.real_name, R.id.nickserv_password, R.id.select_channels}; //TODO: This comes later, R.id.remember_names, R.id.remember_password};
    String[] channelsToJoin;
    String[] availableChannels = new String[] {
            "#jupiterbroadcasting",
            "#jupiterdev",
            "#jupitergaming",
            "#jupiterops",
            "#qweex"
    };

    /** Constructor; supplies MasterActivity reference. */
    public LoginFragment(MasterActivity master) {
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

            layout.findViewById(R.id.select_channels).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new MultiChooserDialog(master, ResCache.str(R.string.select_channels), LoginFragment.this, availableChannels, channelsToJoin);
                }
            });


            onChosen(new String[] { "#qweex" });
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

            Log.d(TAG, "Logging in from form");
            master.chatFragment.connect(nickname, username, real_name, password, channelsToJoin);
            master.pushFragment(master.chatFragment, false);
        }
    };

    @Override
    public void show() {
        master.getSupportActionBar().setTitle(R.string.chat);
    }

    @Override
    public void hide() {
    }

    @Override
    public void onChosen(String[] selected) {
        layout.findViewById(R.id.connect).setEnabled(selected!=null && selected.length>0);
        channelsToJoin = selected;
    }
}
