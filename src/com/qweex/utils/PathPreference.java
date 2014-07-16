package com.qweex.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

//http://www.codeproject.com/Articles/547636/Android-Ready-to-use-simple-directory-chooser-dial
public class PathPreference extends Preference implements Preference.OnPreferenceClickListener,
        DirectoryChooserDialog.OnDirectoryChosen
{
    String mDefaultPath, mPath, mPrefix;
    View mView;

    private static final String androidns = "http://schemas.android.com/apk/res/android";

    public PathPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public PathPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PathPreference(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs)
    {
        setOnPreferenceClickListener(this);
        if(attrs != null)
            mDefaultPath = attrs.getAttributeValue(androidns, "defaultValue");
        else
            mDefaultPath = "/mnt/sdcard";
        getPath();
        Log.d("PathPreference", "Loaded value is: " + mPath);
        setSummary(mPath);
    }

    public String getPath()
    {
        if(mPath==null)
            try {
                if(isPersistent()) {
                    mPath = getSharedPreferences().getString(getKey(), mDefaultPath);
                }
            } catch (ClassCastException e) {
                mPath = mDefaultPath;
            }
        return mPath;
    }

    @Override
    public View getView(View v, ViewGroup parent) {
        Log.d("PathPreference", "Getting view & value is: " + mPath);
        v = super.getView(v, parent);
        ((TextView)v.findViewById(android.R.id.title)).setText(getTitle());
        ((TextView)v.findViewById(android.R.id.summary)).setText(getSummary());
        return v;
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        DirectoryChooserDialog d = new DirectoryChooserDialog(getContext(),this);
        d.setNewFolderEnabled(true);
        d.chooseDirectory(getPath());
        return false;
    }

    @Override
    public void onDirectoryChosen(String chosenDir)
    {

        Log.v("PathPreference", "Chose dir: " + chosenDir);
        mPath = chosenDir;
        setSummary(chosenDir);

        getSharedPreferences().edit().putString(getKey(), mPath).commit();

        try {
            getOnPreferenceChangeListener().onPreferenceChange(this, mPath);
        } catch (NullPointerException e) {}
    }

    @Override
    public SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }
}
