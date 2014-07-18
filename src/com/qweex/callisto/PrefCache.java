package com.qweex.callisto;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.qweex.utils.ResCache;
import com.qweex.utils.ResCache.Color;

import java.util.*;

public class PrefCache {
    static String TAG = "Callisto:PrefCache";

    public static SharedPreferences sharedPreferences;

    protected static HashMap<String, String> strings = new HashMap<String, String>();
    protected static HashMap<String, Set<String>> stringSets = new HashMap<String, Set<String>>();
    protected static HashMap<String, Drawable> drawables = new HashMap<String, Drawable>();
    protected static HashMap<String, Color> colors = new HashMap<String, Color>();
    protected static HashMap<String, Set<Color>> colorSets = new HashMap<String, Set<Color>>();
    protected static HashMap<String, Integer> integers = new HashMap<String, Integer>();
    protected static HashMap<String, Boolean> booleans = new HashMap<String, Boolean>();
    protected static HashMap<String, ColorStateList> colorStateLists = new HashMap<String, ColorStateList>();

    // Does not allow formatting
    static public String str(String prefKey, Integer stringId, String def) {
        String val;
        if(!strings.containsKey(prefKey)) {
            if(stringId==null)
                val = sharedPreferences.getString(prefKey, def);
            else
                val = sharedPreferences.getString(prefKey, ResCache.str(stringId));
            strings.put(prefKey, val);
        }
        return strings.get(prefKey);
    }

    static public Set<String> strSet(String prefKey, Integer arrId, String[] def) {
        Set<String> val;
        if(!stringSets.containsKey(prefKey)) {
            if(arrId==null)
                val = sharedPreferences.getStringSet(prefKey, new LinkedHashSet<String>(Arrays.asList(def)));
            else
                val = sharedPreferences.getStringSet(prefKey, ResCache.strSet(arrId));
            stringSets.put(prefKey, val);
        }
        return stringSets.get(prefKey);
    }

    static public Color clr(String prefKey, Integer clrId, Color def) {
        int val;
        if(!colors.containsKey(prefKey)) {
            if(clrId==null)
                val = sharedPreferences.getInt(prefKey, def.val());
            else
                val = sharedPreferences.getInt(prefKey, ResCache.clr(clrId).val());
            colors.put(prefKey, new Color(val));
        }
        return colors.get(prefKey);
    }

    static public Integer inte(String prefKey, Integer intId, int def) {
        int val;
        if(!integers.containsKey(prefKey)) {
            if(intId==null)
                val = sharedPreferences.getInt(prefKey, def);
            else
                val = sharedPreferences.getInt(prefKey, ResCache.inte(intId));
            integers.put(prefKey, val);
        }
        return integers.get(prefKey);
    }

    static public Set<Color> clrSet(String prefKey, Integer strId, Color[] def) {
        Set<Color> val = new LinkedHashSet<Color>();
        if(!colorSets.containsKey(prefKey)) {
            Set<String> rawSet = sharedPreferences.getStringSet(prefKey, null);
            if(rawSet==null) {
            // No preference exists; load default

                if(strId==null)
                // Load default from the array provided
                    val = new LinkedHashSet<Color>(Arrays.asList(def));
                else {
                // Load default from Resources
                    rawSet = ResCache.strSet(R.array.chat_nick_colors);
                    Log.v(TAG, "retrieving ClrSet rawSet: " + rawSet.size());
                }
            }

            if(rawSet!=null) {
            // If rawSet was set by either the preferences OR by the resources, parse them into ints.
                for(String s : rawSet)
                    try{
                        val.add(new Color(s));
                    } catch(Exception e){
                        Log.e(TAG, "Error parsing into int: " + s);
                    }
            }

            Log.v(TAG, "retrieving ClrSet val: " + val.size());

            colorSets.put(prefKey, val);
        }
        return colorSets.get(prefKey);
    }

    static public Boolean bool(String prefKey, Integer boolId, boolean def) {
        boolean val;
        if(!booleans.containsKey(prefKey)) {
            if(boolId==null)
                val = sharedPreferences.getBoolean(prefKey, def);
            else
                val = sharedPreferences.getBoolean(prefKey, ResCache.bool(boolId));
            booleans.put(prefKey, val);
        }
        return booleans.get(prefKey);
    }

    static public void updateStr(String prefKey, String newVal) {
        Log.d(TAG, "Updating value: " + prefKey + "=" + newVal);
        sharedPreferences.edit().putString(prefKey, newVal).commit();
        strings.put(prefKey, newVal);
    }

    static public void updateStrSet(String prefKey, Set<String> newVal) {
        sharedPreferences.edit().putStringSet(prefKey, newVal).commit();
        stringSets.put(prefKey, newVal);
    }

    static public void updateClrSet(String prefKey, Set<Color> newVal) {

        Set<String> rawVal = new LinkedHashSet<String>();
        for(Color i : newVal)
            rawVal.add(i + "");

        sharedPreferences.edit().putStringSet(prefKey, rawVal).commit();
        colorSets.put(prefKey, newVal);
    }

    static public void updateClr(String prefKey, Color newVal) {
        Log.d(TAG, "Updating value: " + prefKey + "=" + newVal);
        sharedPreferences.edit().putInt(prefKey, newVal.val()).commit();
        colors.put(prefKey, newVal);
    }

    static public void updateInt(String prefKey, int newVal) {
        Log.d(TAG, "Updating value: " + prefKey + "=" + newVal);
        sharedPreferences.edit().putInt(prefKey, newVal).commit();
        integers.put(prefKey, newVal);
    }

    static public void updateBool(String prefKey, boolean newVal) {
        Log.d(TAG, "Updating value: " + prefKey + "=" + newVal);
        sharedPreferences.edit().putBoolean(prefKey, newVal).commit();
        booleans.put(prefKey, newVal);
    }

    static public void update(String prefKey, Object newVal) {
        if(newVal instanceof String) {
            updateStr(prefKey, (String)newVal);
            return;
        }
        if(newVal instanceof String[]) {
            //noinspection ConstantConditions
            updateStrSet(prefKey, new LinkedHashSet<String>(Arrays.asList((String[])newVal)));
            return;
        }
        if(newVal instanceof Set) {
            //noinspection unchecked
            Object first = ((Set)newVal).iterator().next();
            if(first.getClass().equals(String.class))
                updateStrSet(prefKey, (Set<String>) newVal);
            else if(first.getClass().equals(Color.class))
                updateClrSet(prefKey, (Set<Color>) newVal);
            return;
        }
        if(newVal instanceof Boolean) {
            updateBool(prefKey, (Boolean) newVal);
            return;
        }
        if(newVal instanceof Integer) {
            updateInt(prefKey, (Integer) newVal);
            return;
        }
        if(newVal instanceof Color) {
            updateClr(prefKey, (Color) newVal);
            return;
        }

        Log.e(TAG, "ERROR unknown type: " + newVal.getClass());
    }

    static public void rm(String... prefKeys) {
        for(String key : prefKeys)
            sharedPreferences.edit().remove(key).commit();
    }

}
