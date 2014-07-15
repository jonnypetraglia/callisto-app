package com.qweex.callisto.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;
import com.qweex.callisto.ResCache;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsFragment extends CallistoFragment {

    String TAG = "Callisto:settings:SettingsFragment";

    ListView listview;
    SettingsFragmentAdapter adapter;
    Activity mActivity;
    SharedPreferences sharedPreferences;
    List<Preference> preferences;
    Map<Preference, AttributeSet> prefAttributes;

    final String ANDROIDNS = "http://schemas.android.com/apk/res/android";

    /** Constructor; supplies MasterActivity reference. */
    public SettingsFragment(MasterActivity master) {
        super(master);
        mActivity = master;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(master);

        preferences = new ArrayList<Preference>();
        prefAttributes = new HashMap<Preference, AttributeSet>();

        preferences = loadPreferencesFromResource(R.xml.preferences, preferences, prefAttributes);

        Log.v(TAG,"Loaded preferences: " + preferences.size());


        for(Preference preference : preferences) {
            String defaultValue = prefAttributes.get(preference).getAttributeValue(ANDROIDNS, "defaultValue");
            Log.v(TAG,"Restoring preference " + preference.getKey() + "value: " + defaultValue);

            if(preference instanceof CheckBoxPreference) {
                boolean restoredValue = sharedPreferences.getBoolean(preference.getKey(), Boolean.parseBoolean(defaultValue));
                ((CheckBoxPreference) preference).setChecked(restoredValue);
                setCheckPreference((CheckBoxPreference) preference, restoredValue, null);
            }
        }
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
        if(listview==null) {

            listview = new ListView(master);
            listview.setBackgroundColor(ResCache.clr(com.qweex.callisto.R.color.settings_background));

            listview.setOnItemClickListener(onItemClickListener);

            //findPreference("irc_max_scrollback").setOnPreferenceChangeListener(numberCheckListener);    //Set listener for assuring that it is just a number
            //irc_settings = this.getPreferenceScreen().findPreference("irc_settings");

            adapter = new SettingsFragmentAdapter(inflater.getContext(), preferences);

            listview.setAdapter(adapter);

        } else {
            ((ViewGroup)listview.getParent()).removeView(listview);
        }
        show();
        return listview;
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
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

        //TODO: Write Preference Status
        Log.v(TAG, "Writing new pref value: " + check.isChecked());
        sharedPreferences.edit().putBoolean(check.getKey(), check.isChecked()).commit();
    }


    // Handles dependencies
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


    public List<Preference> loadPreferencesFromResource(int resid, List<Preference> target, Map<Preference, AttributeSet> targetAttrs) {

        // Classes that can be parsed
        ListPreference a;
        CheckBoxPreference b;

        XmlResourceParser parser = null;
        try {
            parser = mActivity.getResources().getXml(resid);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String nodeName = parser.getName();
            if (!"PreferenceScreen".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <PreferenceScreen> tag; found"
                                + nodeName + " at "
                                + parser.getPositionDescription());
            }

            final int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();


                AttributeSet attrs = Xml.asAttributeSet(parser);
                Preference newPreference;
                try {
                    Class<?> nodeClass = Class.forName(nodeName);

                    newPreference = (Preference) nodeClass.getConstructor(
                            Context.class,
                            AttributeSet.class
                    ).newInstance(
                            mActivity,
                            attrs
                    );
                } catch(ClassNotFoundException cnf) {
                    Class<?> nodeClass = Class.forName("android.preference." + nodeName);

                    newPreference = (Preference) nodeClass.getConstructor(
                            Context.class,
                            AttributeSet.class
                    ).newInstance(
                            mActivity,
                            attrs
                    );
                }
                target.add(newPreference);
                targetAttrs.put(newPreference, attrs);
            }

        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (java.lang.InstantiationException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error parsing headers", e);
        } finally {
            if (parser != null)
                parser.close();
        }
        return target;
    }
}
