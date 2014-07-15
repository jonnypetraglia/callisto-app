package com.qweex.callisto.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;

/**
 * General guide to using this shit:
 *   - Create a filled Screen by either of the following two methods:
 *     - Inflate from XML (pass null to the 'screen' argument in the constructor)
 *     - Create manually via
 *       - newScreen = new SettingsFragmentScreen()
 *       - newScreen.add(new Preference(...))
 *       - new SettingsFragment(activity, newScreen);
 *   - The following native Preferences are supported: (Preference, CheckBoxPreference, ListPreference)
 *   - Writing a custom fragment means you have to take care of the clickListener and all that, either inside the Fragment or inside this class.
 *   - openChild() is called when a sub-fragment is selected. Easiest thing to do: create a new SettingsFragment with the passed in Screen & push it on the stack.
 */

public class SettingsFragment extends CallistoFragment {

    String TAG = "Callisto:settings:SettingsFragment";

    SharedPreferences sharedPreferences;
    SettingsFragmentScreen screen;

    /** Constructor; supplies MasterActivity reference. */
    public SettingsFragment(MasterActivity master, SettingsFragmentScreen screen) {
        super(master);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(master);
        this.screen = screen;
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
        if(screen==null) {
            SettingsFragmentParser parser = new SettingsFragmentParser(this);
            this.screen = parser.loadPreferencesFromResource(R.xml.preferences);
            Log.v(TAG, "Loaded preferences: " + this.screen.getPreferences().size());
        } else {
            if(screen.getListView().getParent()!=null)
                ((ViewGroup)screen.getListView().getParent()).removeView(screen.getListView());
        }
        show();
        return screen.getListView();
    }

    @Override
    public void show() {
        master.getSupportActionBar().setTitle(com.qweex.callisto.R.string.settings);
        master.getSupportActionBar().setSubtitle(screen.getTitle());
        master.drawerToggle.setDrawerIndicatorEnabled(screen.getParent()==null);
    }

    @Override
    public void hide() {
        master.drawerToggle.setDrawerIndicatorEnabled(screen.getParent()!=null);
    }

    public void openChild(SettingsFragmentScreen childScreen) {
        SettingsFragment child = new SettingsFragment(master, childScreen);

        master.pushFragment(child, true);
    }
}
