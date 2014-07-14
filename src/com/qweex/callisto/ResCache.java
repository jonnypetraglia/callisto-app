package com.qweex.callisto;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.HashMap;

/** Resource accessor that also caches the results for quicker future access.
 * @author      Jon Petraglia <notbryant@gmail.com>
 * */
public class ResCache {
    static String TAG = "Callisto:TAG";

    public static Resources resources;
    static HashMap<Integer, String> strings = new HashMap<Integer, String>();
    static HashMap<Integer, Drawable> drawables = new HashMap<Integer, Drawable>();
    static HashMap<Integer, Integer> colors = new HashMap<Integer, Integer>();
    static HashMap<Integer, Integer> integers = new HashMap<Integer, Integer>();

    static public String str(Integer stringId, Object... args) {
        if(!strings.containsKey(stringId))
            strings.put(stringId, resources.getString(stringId));
        return String.format(strings.get(stringId), args);
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

    static public Integer inte(Integer intId) {
        if(!integers.containsKey(intId))
            integers.put(intId, resources.getColor(intId));
        return integers.get(intId);
    }
}
