package com.qweex.callisto.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.qweex.callisto.R;

import java.io.File;

public class SettingsActivity extends PreferenceActivity {

    String TAG = "Callisto:settings:SettingsActivity";

    private Preference irc_settings;

    /** Called when the activity is created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //addPreferencesFromResource(R.xml.preferences);
        findPreference("irc_max_scrollback").setOnPreferenceChangeListener(numberCheckListener);    //Set listener for assuring that it is just a number
        irc_settings = this.getPreferenceScreen().findPreference("irc_settings");

        getPreferenceScreen().findPreference("reset_colors").setOnPreferenceClickListener(resetColors);

        //!irc_settings.setOnPreferenceClickListener(setSubpreferenceBG);
    }

    /** Called when any of the preferences is changed. Used to perform actions on certain events. */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        /*
        //Move files to new storage_path
        if("storage_path".equals(key))
        {
            //Move folder for storage dir
            String new_path = sharedPreferences.getString("storage_path", "callisto");
            if(!new_path.startsWith("/"))
                new_path = Environment.getExternalStorageDirectory().toString() + File.separator + new_path;
            if(new_path.equals(StaticBlob.storage_path))
                return;
            File newfile = new File(new_path);
            newfile.mkdirs();

            File olddir = new File(StaticBlob.storage_path);
            if(!olddir.renameTo(new File(new_path)))
            {
                Toast.makeText(this, R.string.move_folder_error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Oh crap, a file couldn't be moved");
            }
            StaticBlob.storage_path = new_path;
        }
        //Restart stream if live quality changes
        else if("live_url".equals(key) && StaticBlob.live_isPlaying)
        {
            String new_radio = PreferenceManager.getDefaultSharedPreferences(this).getString("live_url", "callisto");
            if(!new_radio.equals(oldRadioForLiveQuality))
            {
                MagicButtonThatDoesAbsolutelyNothing.performClick();
                MagicButtonThatDoesAbsolutelyNothing.performClick();
                oldRadioForLiveQuality =new_radio;
            }
        }
        //Change the IRC scrollback
        else if(key.equals("irc_max_scrollback"))
        {
            StaticBlob.ircChat.setMaximumCapacity(PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_max_scrollback", 500));
            StaticBlob.ircLog.setMaximumCapacity(PreferenceManager.getDefaultSharedPreferences(this).getInt("irc_max_scrollback", 500));
        }
        else if(key.equals("hide_notification_when_paused") && StaticBlob.mplayer!=null && StaticBlob.playerInfo.isPaused)
        {
            if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("hide_notification_when_paused", false))
                StaticBlob.mNotificationManager.cancel(StaticBlob.NOTIFICATION_ID);
            else
            {
                long id = -1;
                try {
                    Cursor c = StaticBlob.databaseConnector.currentQueueItem();
                    c.moveToFirst();
                    id = c.getLong(c.getColumnIndex("identity"));
                }catch(Exception e){}
                PlayerControls.createNotification(this, id);
            }
        }
        */
    }

    /** Checks to make sure that a number (in this case, the scrollback) is a legit integer
     * http://stackoverflow.com/questions/3206765/number-preferences-in-preference-activity-in-android
     * */
    Preference.OnPreferenceChangeListener numberCheckListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return (!newValue.toString().equals("")  &&  newValue.toString().matches("\\d*"));
        }
    };

    /** Resets the colors */
    Preference.OnPreferenceClickListener resetColors = new Preference.OnPreferenceClickListener()
    {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            String[] keys = {"text", "back", "topic", "mynick", "me", "links", "pm", "join", "nick", "part", "quit", "kick", "mention", "error"};
                            SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit();
                            for(String k : keys)
                                e.remove("irc_color_" + k);
                            e.commit();
                            finish();
                            android.content.Intent i = new android.content.Intent(SettingsActivity.this, SettingsActivity.class);
                            startActivity(i);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext());
            builder.setTitle(R.string.reset_colors_title);
            builder.setMessage(R.string.reset_colors_message);
            builder.setPositiveButton(android.R.string.yes, dialogClickListener);
            builder.setNegativeButton(android.R.string.no, dialogClickListener);
            builder.show();
            return true;
        }
    };
}
