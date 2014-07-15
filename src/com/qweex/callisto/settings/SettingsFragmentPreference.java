package com.qweex.callisto.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

// This is a sample class

public class SettingsFragmentPreference extends Preference implements Preference.OnPreferenceClickListener{

    Object mValue, mDefaultValue;

    private static final String androidns = "http://schemas.android.com/apk/res/android";

    public SettingsFragmentPreference(Context context) {
        super(context);
        init(context, null);
    }

    public SettingsFragmentPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs)
    {
        setOnPreferenceClickListener(this);
        if(attrs != null)
            mDefaultValue = attrs.getAttributeValue(androidns, "defaultValue");
        else
            mDefaultValue = "/mnt/sdcard";
        getValue();
        setSummary((String)mValue);
    }

    Object getValue() {
        if(mValue==null)
            try {
                if(isPersistent()) {
                    mValue = getPersistedString((String) mDefaultValue);
                }
            } catch (ClassCastException e) {
                mValue = mDefaultValue;
            }
        return mValue;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // Do stuff like show a dialog or whatever
        return false;
    }
}
