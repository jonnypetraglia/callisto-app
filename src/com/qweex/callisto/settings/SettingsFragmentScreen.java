package com.qweex.callisto.settings;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import com.qweex.callisto.ResCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsFragmentScreen extends Preference {
    String TAG = "Callisto:settings:SettingsFragmentsScreen";

    AttributeSet myAttributes;

    List<Preference> preferences = new ArrayList<Preference>();
    Map<Preference, AttributeSet> prefAttributes = new HashMap<Preference, AttributeSet>();

    SettingsFragmentScreen parent;

    ListView listview;
    SettingsFragmentAdapter adapter;

    final String ANDROIDNS = "http://schemas.android.com/apk/res/android";

    public SettingsFragmentScreen(Context context, AttributeSet attrs, SettingsFragmentScreen parent) {
        super(context, attrs);
        this.myAttributes = attrs;
        this.parent = parent;

    }

    public SettingsFragmentScreen addChild(Preference child, AttributeSet childAttrs) {
        preferences.add(child);
        prefAttributes.put(child, childAttrs);
        return this;
    }

    public List<Preference> getPreferences() { return preferences; }

    public AttributeSet getAttributes(Preference p) { return prefAttributes.get(p); }

    public AttributeSet getMyAttributes() { return myAttributes; }

    public SettingsFragmentScreen getParent() { return parent; }

    public ListView getListView() {
        if(listview==null) {
            for(Preference preference : preferences) {
                // Restore the value from the persisted state
                String defaultValue = prefAttributes.get(preference).getAttributeValue(ANDROIDNS, "defaultValue");
                Log.v(TAG, "Restoring preference " + preference.getKey() + "value: " + defaultValue);

                if(preference instanceof CheckBoxPreference) {
                    boolean restoredValue = preference.getSharedPreferences().getBoolean(preference.getKey(), Boolean.parseBoolean(defaultValue));
                    ((CheckBoxPreference) preference).setChecked(restoredValue);
                    setCheckPreference((CheckBoxPreference) preference, restoredValue, null);
                }

            }

            listview = new ListView(getContext());
            listview.setBackgroundColor(ResCache.clr(com.qweex.callisto.R.color.settings_background));

            listview.setOnItemClickListener(onItemClickListener);

            adapter = new SettingsFragmentAdapter(getContext(), this);

            listview.setAdapter(adapter);
        }
        return listview;
    }



    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Preference preference = preferences.get(position);

            boolean eventHandled = preference.getOnPreferenceClickListener()!=null && preference.getOnPreferenceClickListener().onPreferenceClick(preference);
            if(eventHandled) return;

            if(preference.getClass() == CheckBoxPreference.class)
                setCheckPreference((CheckBoxPreference) preference, !((CheckBoxPreference) preference).isChecked(), view);
        }
    };


    void setCheckPreference(CheckBoxPreference check, boolean newValue, View view) {
        check.setChecked(newValue);

        setDependentPreferencesEnabled(check, check.isChecked());

        if(view!=null)
            ((CheckBox)view.findViewById(android.R.id.checkbox)).setChecked(check.isChecked());

        Log.v(TAG, "Writing new pref value: " + check.isChecked());
        check.getSharedPreferences().edit().putBoolean(check.getKey(), check.isChecked()).commit();
    }


    // Handles dependencies
    void setDependentPreferencesEnabled(Preference pref) {

        Object value = pref.getSharedPreferences().getAll().get(pref.getKey());
        boolean enabled = value!=null;
        if(enabled && value instanceof Boolean)
            enabled = (Boolean) value;


        setDependentPreferencesEnabled(pref, enabled);
    }

    void setDependentPreferencesEnabled(Preference pref, boolean enabled) {
        Log.v(TAG, "Setting dependency statuses of " + pref.getKey() + ": " + enabled);

        for(int i=0; i<preferences.size(); i++) {
            Preference testPref = preferences.get(i);
            if(pref.getKey().equals(testPref.getDependency())) {
                Log.v(TAG, " - Preference " + testPref.getKey() + " is a dependency");
                testPref.setEnabled(enabled);

                if(listview!=null && i>=listview.getFirstVisiblePosition() && i<=listview.getLastVisiblePosition()) {
                    setViewAndChildrenEnabled(listview.getChildAt(i), enabled);
                }

                setDependentPreferencesEnabled(testPref, enabled);
            }
        }
    }

    void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setClickable(enabled);
        if(!(view instanceof ViewGroup))
            return;
        ViewGroup viewGroup = (ViewGroup) view;
        for(int j=0; j<viewGroup.getChildCount(); j++) {
            View subView = viewGroup.getChildAt(j);
            Log.v(TAG, " -- View " + subView.getClass() + " is being set to " + enabled);
            subView.setEnabled(enabled);
            subView.setClickable(enabled);
            setViewAndChildrenEnabled(subView, enabled);
        }
    }
}
