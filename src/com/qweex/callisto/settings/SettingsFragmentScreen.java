package com.qweex.callisto.settings;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.qweex.callisto.PrefCache;
import com.qweex.utils.ResCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsFragmentScreen extends Preference {
    String TAG = "Callisto:settings:SettingsFragmentsScreen";

    AttributeSet myAttributes;

    List<Preference> preferences = new ArrayList<Preference>();
    HashMap<String, Preference> preferencesByKey = new HashMap<String, Preference>();
    Map<Preference, SettingsAttributes> prefAttributes = new HashMap<Preference, SettingsAttributes>();

    SettingsFragmentScreen parent;

    ListView listview;
    SettingsFragmentAdapter adapter;
    Dialog listPreferenceDialog, editPreferenceDialog;
    SettingsFragment fragment;


    public SettingsFragmentScreen(SettingsFragment fragment, AttributeSet attrs, SettingsFragmentScreen parent) {
        super(fragment.getActivity(), attrs);
        this.fragment = fragment;
        this.myAttributes = attrs;
        this.parent = parent;
    }

    public SettingsFragmentScreen addChild(Preference child, SettingsAttributes childAttrs) {
        return addChild(child, childAttrs, preferences.size());
    }

    public SettingsFragmentScreen addChild(Preference child, SettingsAttributes childAttrs, int position) {
        if(listview!=null)
            throw new IllegalStateException("Attempted to add child after ListView has been created.");
        Log.v(TAG, "Adding child: " + child.getKey() + " | " + childAttrs + " to position " + position);
        preferences.add(position, child);
        prefAttributes.put(child, childAttrs);
        if(child.getKey()!=null)
            preferencesByKey.put(child.getKey(), child);
        return this;
    }

    public List<Preference> getPreferences() { return preferences; }

    public Preference getPreference(String key) {
        if(preferencesByKey.containsKey(key))
            return preferencesByKey.get(key);
        else
            return null;
    }

    public SettingsAttributes getAttributes(Preference p) { return prefAttributes.get(p); }

    public AttributeSet getMyAttributes() { return myAttributes; }

    public SettingsFragmentScreen getParent() { return parent; }

    public ListView getListView() {
        if(listview==null) {
            Log.v(TAG, "Restoring Preference Values");
            for(Preference preference : preferences) {

                String defaultValue;
                try {
                    SettingsAttributes attrs = prefAttributes.get(preference);
                    defaultValue = attrs.get("defaultValue");
                    Log.v(TAG, ":Restoring preference " + preference.getKey() + " defaultValue: " + defaultValue);
                } catch(Exception e) {
                    defaultValue = null;
                }


                if(preference instanceof CheckBoxPreference) {
                    boolean trueDefaultValue = false;
                    if(defaultValue!=null)
                        trueDefaultValue = Boolean.parseBoolean(defaultValue);
                    boolean restoredValue = getSharedPreferences().getBoolean(preference.getKey(), trueDefaultValue);
                    CheckBoxPreference check = ((CheckBoxPreference) preference);
                    check.setChecked(restoredValue);
                } else
                if(preference instanceof ListPreference) {
                    String restoredValue = getSharedPreferences().getString(preference.getKey(), defaultValue);
                    ListPreference list = ((ListPreference) preference);
                    list.setValue(restoredValue);
                } else
                if(preference instanceof EditTextPreference) {
                    String restoredValue = getSharedPreferences().getString(preference.getKey(), defaultValue);
                    EditTextPreference edit = ((EditTextPreference) preference);
                    edit.setText(restoredValue);
                } else
                if(preference instanceof SettingsFragmentScreen) {
                    //Pass
                } else
                if(preference instanceof PreferenceCategory) {
                    //Pass
                } else {
                    //((SettingsFragmentInit)preference).restoreInitialValue(defaultValue);

                    preference.setOnPreferenceChangeListener(customPreferenceChangeListener);
                    Log.v(TAG, "Custom preference type passed in: " + preference.getClass().getName());
                    Log.v(TAG, "Hopefully the custom class implements a click listener");
                }

                setDependentPreferencesEnabled(preference);
            }

            Log.v(TAG, "Creating ListView");
            listview = new ListView(getContext());
            listview.setBackgroundColor(PrefCache.clr("chat_color_background", com.qweex.callisto.R.color.chat_bg, 0));

            adapter = new SettingsFragmentAdapter(getContext(), this);
            listview.setAdapter(adapter);

            listview.setOnItemClickListener(onItemClickListener);
        }
        return listview;
    }



    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(!view.isEnabled())
                return;
            Preference preference = preferences.get(position);

            boolean eventHandled = preference.getOnPreferenceClickListener()!=null && preference.getOnPreferenceClickListener().onPreferenceClick(preference);
            if(eventHandled) return;

            if(preference.getClass() == CheckBoxPreference.class)
                setCheckPreference((CheckBoxPreference) preference, !((CheckBoxPreference) preference).isChecked(), view);
            else
            if(preference.getClass() == ListPreference.class)
                showListPreference((ListPreference) preference, view);
            else
            if(preference.getClass() == EditTextPreference.class)
                showEditPreference((EditTextPreference) preference, view);
            else
            if(preference.getClass() == SettingsFragmentScreen.class)
                fragment.openChild((SettingsFragmentScreen) preference);
        }
    };

    void showListPreference(final ListPreference list, final View listviewView) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        ListView lv = new ListView(builder.getContext());
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(builder.getContext(), R.layout.simple_list_item_1, list.getEntries());
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setListPreference(list, list.getEntryValues()[position].toString(), listviewView);
                listPreferenceDialog.dismiss();
            }
        });

        builder.setTitle(list.getTitle());

        if(list.getEntry()!=null)
            builder.setMessage(ResCache.str(com.qweex.callisto.R.string.current_value) + ": " + list.getEntry());
        builder.setView(lv);
        builder.setNegativeButton(android.R.string.cancel, null);
        listPreferenceDialog = builder.show();
    }

    void showEditPreference(final EditTextPreference edit, final View listviewView) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        final EditText editText = new EditText(builder.getContext());

        builder.setTitle(edit.getTitle());

        if(edit.getText()!=null)
            builder.setMessage(ResCache.str(com.qweex.callisto.R.string.current_value) + ": " + edit.getText());
        builder.setView(editText);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setEditPreference(edit, editText.getText().toString(), listviewView);
                editPreferenceDialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        editPreferenceDialog = builder.show();
    }

    void setListPreference(ListPreference list, String newValue, View view) {
        list.setValue(newValue);

        //TODO: Update view?

        Log.v(TAG, "Writing new pref value: " + list.getValue());
        getSharedPreferences().edit().putString(list.getKey(), list.getValue()).commit();
        setDependentPreferencesEnabled(list);
    }

    void setEditPreference(EditTextPreference edit, String newValue, View view) {
        edit.setText(newValue);

        //TODO: Update view?

        Log.v(TAG, "Writing new pref value: " + newValue);

        getSharedPreferences().edit().putString(edit.getKey(), edit.getText()).commit();
        setDependentPreferencesEnabled(edit);
    }


    void setCheckPreference(CheckBoxPreference check, boolean newValue, View view) {
        check.setChecked(newValue);

        if(view!=null)
            ((CheckBox)view.findViewById(android.R.id.checkbox)).setChecked(check.isChecked());

        Log.v(TAG, "Writing new pref value: " + check.isChecked());
        getSharedPreferences().edit().putBoolean(check.getKey(), check.isChecked()).commit();
        setDependentPreferencesEnabled(check);
    }


    // Handles dependencies
    void setDependentPreferencesEnabled(Preference pref) {

        Object value = getSharedPreferences().getAll().get(pref.getKey());
        boolean enabled = value!=null;
        //if(enabled && value instanceof Boolean)
        //    enabled = (Boolean) value;

        setDependentPreferencesEnabled(pref, enabled);
    }

    void setDependentPreferencesEnabled(Preference pref, boolean enabled) {
        if(pref.getKey()==null)
            return;

        if(pref.shouldDisableDependents())
            enabled = !enabled;

        Log.v(TAG, "Setting dependency statuses of " + pref.getKey() + ": " + enabled);

        for(int i=0; i<preferences.size(); i++) {
            Preference testPref = preferences.get(i);
            if(pref.getKey().equals(testPref.getDependency())) {
                Log.v(TAG, " - Preference " + testPref.getKey() + " is a dependency");
                testPref.setEnabled(enabled);

                if(listview!=null && i>=listview.getFirstVisiblePosition() && i<=listview.getLastVisiblePosition()) {
                    setViewAndChildrenEnabled(listview.getChildAt(i-listview.getFirstVisiblePosition()), enabled);
                }

                setDependentPreferencesEnabled(testPref, enabled);
            }
        }
    }

    void setViewAndChildrenEnabled(View view, boolean enabled) {
        if(view==null)
            return;
        Log.v(TAG, " -- View " + view.getClass() + " is being set to " + enabled);
        view.setEnabled(enabled);
        if(!(view instanceof ViewGroup))
            return;

        ViewGroup viewGroup = (ViewGroup) view;
        for(int j=0; j<viewGroup.getChildCount(); j++)
            setViewAndChildrenEnabled(viewGroup.getChildAt(j), enabled);
    }

    OnPreferenceChangeListener customPreferenceChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            int index = preferences.indexOf(preference);
            //adapter.getView(index, listview.getChildAt(index - listview.getFirstVisiblePosition()), listview);
            adapter.notifyDataSetChanged();
            return false;
        }
    };

    @Override
    public SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    public static class SettingsAttributes {
        Map<String, String> attrs = new HashMap<String, String>();

        public SettingsAttributes(AttributeSet attrs) {
            for(int i=0; i<attrs.getAttributeCount(); i++) {
                this.attrs.put(
                        attrs.getAttributeName(i),
                        attrs.getAttributeValue(i)
                );
            }
        }

        public String get(String name) {
            if(attrs.containsKey(name))
                return attrs.get(name);
            return null;
        }
    }
}
