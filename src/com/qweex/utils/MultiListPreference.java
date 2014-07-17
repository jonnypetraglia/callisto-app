package com.qweex.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import java.util.*;


public class MultiListPreference extends Preference implements Preference.OnPreferenceClickListener, MultiChooserDialog.OnChosen {

    String[] entries, values, chosen, defaultValue;
    boolean atLeastOne = false;

    private static final String androidns = "http://schemas.android.com/apk/res/android";

    public MultiListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public MultiListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MultiListPreference(Context context) {
        super(context);
        init(context, null);
    }

    private Void init(Context context, AttributeSet attrs)
    {
        setOnPreferenceClickListener(this);
        if(attrs==null)
            return die();

        int entriesId = attrs.getAttributeResourceValue(androidns, "entries", 0),
            valuesId = attrs.getAttributeResourceValue(androidns, "entryValues", 0);
        int defaultId = attrs.getAttributeResourceValue(androidns, "defaultValue", 0);
        if(entriesId==0 || valuesId==0)
            return die();

        entries = context.getResources().getStringArray(entriesId);
        values = context.getResources().getStringArray(valuesId);
        if(entries.length!=values.length || entries.length<1)
            return die();


        if(defaultId!=0)
            defaultValue = context.getResources().getStringArray(defaultId);

        setAtLeastOne(attrs.getAttributeBooleanValue(androidns, "defaultValue", atLeastOne));

        getChosen();

        return null;
    }

    Void die() throws IllegalArgumentException {
        //throw new IllegalArgumentException("Invalid attributes supplied to MultiListPreference.");
        return null;
    }

    public void setAtLeastOne(boolean set) {
        atLeastOne = set;
    }

    public void setEntryValues(String[] entries, String[] values) {
        if(entries==null || values==null || entries.length!=values.length || entries.length==0)
            throw new IllegalArgumentException("Invalid attributes supplied to MultiListPreference.");
        this.entries = entries;
        this.values = values;
    }

    public void setDefaultValue(String[] defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String[] getChosen() {
        if(this.chosen==null) {
            Set<String> chosen = getSharedPreferences().getStringSet(getKey(), new LinkedHashSet<String>(Arrays.asList(defaultValue)));
            this.chosen = chosen.toArray(new String[chosen.size()]);
        }
        return chosen;
    }

    @Override
    public void onChosen(String[] selected) {
        chosen = selected;
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for(String s : chosen)
            set.add(s);
        getSharedPreferences().edit().putStringSet(getKey(), set).commit();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        String[] chosenEntries = new String[chosen.length];

        ArrayList<String> valueList = new ArrayList<String>(Arrays.asList(values));

        for(int i=0; i<chosen.length; ++i)
            chosenEntries[i] = entries[ valueList.indexOf( chosen[i] ) ];


        new MultiChooserDialog(getContext(),
                               getTitle()!=null ? getTitle().toString() : null,
                               this,
                               atLeastOne,
                               entries,
                               chosenEntries
                );

        return false;
    }

    @Override
    public SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }
}
