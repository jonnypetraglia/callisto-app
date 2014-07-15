package com.qweex.callisto.settings;

import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class SettingsFragmentParser {

    String TAG = "Callisto:settings:SettingsFragmentsParser";

    SettingsFragment fragment;

    final static public String ANDROIDNS = "http://schemas.android.com/apk/res/android";

    public SettingsFragmentParser(SettingsFragment fragment) {
        this.fragment = fragment;
    }

    public SettingsFragmentScreen loadPreferencesFromResource(int resid) {
        SettingsFragmentScreen res;

        XmlResourceParser parser = null;
        try {
            parser = fragment.getActivity().getResources().getXml(resid);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }
            res = parsePreferenceScreen(parser, null);

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
        return res;
    }

    private SettingsFragmentScreen parsePreferenceScreen(XmlPullParser parser, SettingsFragmentScreen parent)
            throws IOException, XmlPullParserException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {

        String nodeName = parser.getName();
        if (!"PreferenceScreen".equals(nodeName)) {
            throw new RuntimeException(
                    "XML document must start with <PreferenceScreen> tag; found"
                            + nodeName + " at "
                            + parser.getPositionDescription());
        }

        AttributeSet screenAttrs = Xml.asAttributeSet(parser);
        SettingsFragmentScreen result = new SettingsFragmentScreen(fragment, screenAttrs, parent);



        final int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            nodeName = parser.getName();

            AttributeSet newPrefAttrs = Xml.asAttributeSet(parser);
            String x = newPrefAttrs.getAttributeValue(ANDROIDNS, "defaultValue");
            Log.v(TAG, "::default value for new is " + x);

            Preference newPreference;
            if("PreferenceScreen".equals(nodeName))
                newPreference = parsePreferenceScreen(parser, result);
            else {
                try {
                    Class<?> nodeClass = Class.forName(nodeName);

                    newPreference = (Preference) nodeClass.getConstructor(
                            Context.class,
                            AttributeSet.class
                    ).newInstance(
                            fragment.getActivity(),
                            newPrefAttrs
                    );
                } catch(ClassNotFoundException cnf) {
                    Class<?> nodeClass = Class.forName("android.preference." + nodeName);

                    newPreference = (Preference) nodeClass.getConstructor(
                            Context.class,
                            AttributeSet.class
                    ).newInstance(
                            fragment.getActivity(),
                            newPrefAttrs
                    );
                }

            }
            result.addChild(newPreference, new SettingsFragmentScreen.SettingsAttributes(newPrefAttrs));
        }

        return result;
    }
}
