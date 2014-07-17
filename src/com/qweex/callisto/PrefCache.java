package com.qweex.callisto;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import com.qweex.utils.ResCache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PrefCache {

    public static SharedPreferences sharedPreferences;

    protected static HashMap<String, String> strings = new HashMap<String, String>();
    protected static HashMap<String, Set<String>> stringSets = new HashMap<String, Set<String>>();
    protected static HashMap<String, Drawable> drawables = new HashMap<String, Drawable>();
    protected static HashMap<String, Integer> colors = new HashMap<String, Integer>();
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
                val = sharedPreferences.getStringSet(prefKey, new HashSet<String>(Arrays.asList(def)));
            else
                val = sharedPreferences.getStringSet(prefKey, ResCache.strSet(arrId));
            stringSets.put(prefKey, val);
        }
        return stringSets.get(prefKey);
    }

    static public Integer clr(String prefKey, Integer clrId, int def) {
        int val;
        if(!colors.containsKey(prefKey)) {
            if(clrId==null)
                val = sharedPreferences.getInt(prefKey, def);
            else
                val = sharedPreferences.getInt(prefKey, ResCache.clr(clrId));
            colors.put(prefKey, val);
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
        sharedPreferences.edit().putString(prefKey, newVal).commit();
        strings.put(prefKey, newVal);
    }

    static public void updateClr(String prefKey, int newVal) {
        sharedPreferences.edit().putInt(prefKey, newVal).commit();
        colors.put(prefKey, newVal);
    }

    static public void updateInt(String prefKey, int newVal) {
        sharedPreferences.edit().putInt(prefKey, newVal).commit();
        integers.put(prefKey, newVal);
    }

    static public void updateBool(String prefKey, boolean newVal) {
        sharedPreferences.edit().putBoolean(prefKey, newVal).commit();
        booleans.put(prefKey, newVal);
    }

    static public void rm(String... prefKeys) {
        for(String key : prefKeys)
            sharedPreferences.edit().remove(key).commit();
    }

}
