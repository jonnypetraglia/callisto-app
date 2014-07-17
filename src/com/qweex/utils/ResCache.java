package com.qweex.utils;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/** Resource accessor that also caches the results for quicker future access.
 * @author      Jon Petraglia <notbryant@gmail.com>
 * */
public class ResCache {
    static String TAG = "Callisto:ResCache:TAG";

    public static Resources resources;
    protected static HashMap<Integer, String> strings = new HashMap<Integer, String>();
    protected static HashMap<Integer, Set<String>> stringSets = new HashMap<Integer, Set<String>>();
    protected static HashMap<Integer, Drawable> drawables = new HashMap<Integer, Drawable>();
    protected static HashMap<Integer, Integer> colors = new HashMap<Integer, Integer>();
    protected static HashMap<Integer, Integer> integers = new HashMap<Integer, Integer>();
    protected static HashMap<Integer, Boolean> booleans = new HashMap<Integer, Boolean>();
    protected static HashMap<Integer, ColorStateList> colorStateLists = new HashMap<Integer, ColorStateList>();

    static public String str(Integer stringId, Object... args) {
        if(!strings.containsKey(stringId))
            strings.put(stringId, resources.getString(stringId));
        return String.format(strings.get(stringId), args);
    }

    static public Set<String> strSet(Integer arrId) {
        if(!stringSets.containsKey(arrId))
            stringSets.put(arrId, new LinkedHashSet<String>(Arrays.asList(resources.getStringArray(arrId))));
        return stringSets.get(arrId);
    }

    static public Drawable draw(Integer drawId) {
        if(!drawables.containsKey(drawId))
            drawables.put(drawId, resources.getDrawable(drawId));
        return drawables.get(drawId);
    }

    static public Integer clr(Integer clrId) {
        if(!colors.containsKey(clrId))
            colors.put(clrId, resources.getColor(clrId));
        return colors.get(clrId);
    }

    static public ColorStateList clrs(Integer clrsId) {
        if(!colorStateLists.containsKey(clrsId))
            colorStateLists.put(clrsId, resources.getColorStateList(clrsId));
        return colorStateLists.get(clrsId);
    }

    static public Integer inte(Integer intId) {
        if(!integers.containsKey(intId))
            integers.put(intId, resources.getColor(intId));
        return integers.get(intId);
    }

    static public Boolean bool(Integer boolId) {
        if(!booleans.containsKey(boolId))
            booleans.put(boolId, resources.getBoolean(boolId));
        return booleans.get(boolId);
    }
}
