package com.qweex.callisto.settings;


import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.qweex.callisto.ResCache;

import java.util.List;

public class SettingsFragmentAdapter extends ArrayAdapter<Preference> {
    List<Preference> preferences;

    public SettingsFragmentAdapter(Context context, List<Preference> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
        preferences = objects;
    }


    /** Inherited method to generate / recycle the view & inject data from relevant data row.
     * @param pos Position of the item in question of the collection.
     * @param v View passed in that might be recycled.
     * @param parent ViewGroup that should be used as parent for inView.
     * @return The View with all data in it.
     */
    @Override
    public View getView(int pos, View v, ViewGroup parent)
    {
        Preference pref = preferences.get(pos);
        v = pref.getView(v, parent);

        v.setBackgroundResource(com.qweex.callisto.R.drawable.sel_logo1);
        View title = v.findViewById(android.R.id.title),
             summary = v.findViewById(android.R.id.summary);
        if(title!=null)
            ((TextView)title).setTextColor(ResCache.clrs(com.qweex.callisto.R.color.text_main_selector));
        if(summary!=null)
            ((TextView)summary).setTextColor(ResCache.clrs(com.qweex.callisto.R.color.text_muted_selector));

        if(pref.getClass() == CheckBoxPreference.class) {
            CheckBoxPreference check = (CheckBoxPreference) pref;
            ((CheckBox)(v.findViewById(android.R.id.checkbox))).setChecked(check.isChecked());
        }

        return v;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position).isEnabled();
    }
}
