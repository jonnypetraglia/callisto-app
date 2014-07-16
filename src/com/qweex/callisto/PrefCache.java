package com.qweex.callisto;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import com.qweex.utils.ResCache;

public class PrefCache {

    // Does not allow formatting
    static public String str(String prefKey, Integer stringId) {
        return ResCache.str(stringId);
    }


    static public Drawable draw(String prefKey, Integer drawId) {
        return ResCache.draw(drawId);
    }

    static public Integer clr(String prefKey, Integer clrId) {
        return ResCache.clr(clrId);
    }

    static public ColorStateList clrs(String prefKey, Integer clrsId) {
        return ResCache.clrs(clrsId);
    }

    static public Integer inte(String prefKey, Integer intId) {
        return ResCache.inte(intId);
    }
}
