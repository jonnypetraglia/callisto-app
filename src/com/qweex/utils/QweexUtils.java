/*
        DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                    Version 2, December 2004

 Copyright (C) 2013-2014 Jon Petraglia <MrQweex@qweex.com>

 Everyone is permitted to copy and distribute verbatim or modified
 copies of this license document, and changing it is allowed as long
 as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. You just DO WHAT THE FUCK YOU WANT TO.
 */
package com.qweex.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;

import java.lang.reflect.Field;

/**
 * Created with IntelliJ IDEA.
 * User: notbryant
 * Date: 4/15/13
 * Time: 4:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class QweexUtils
{
    //http://stackoverflow.com/a/9624844/1526210
    @TargetApi(4)
    public static boolean isTabletDevice(android.content.Context activityContext)
    {
        // Verifies if the Generalized Size of the device is XLARGE to be
        // considered a Tablet
        boolean xlarge = ((activityContext.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) >=	//Changed this from == to >= because my tablet was returning 8 instead of 4.
                Configuration.SCREENLAYOUT_SIZE_LARGE);


        // If XLarge, checks if the Generalized Density is at least MDPI (160dpi)
        if (xlarge)
        {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            Activity activity = (Activity) activityContext;
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            //This next block lets us get constants that are not available in lower APIs.
            // If they aren't available, it's safe to assume that the device is not a tablet.
            // If you have a tablet or TV running Android 1.5, what the fuck is wrong with you.
            int xhigh = -1, tv = -1;
            try {
                Field f = android.util.DisplayMetrics.class.getDeclaredField("DENSITY_XHIGH");
                xhigh = (Integer) f.get(null);
                f = android.util.DisplayMetrics.class.getDeclaredField("DENSITY_TV");
                xhigh = (Integer) f.get(null);
            }catch(Exception e){}

            // MDPI=160, DEFAULT=160, DENSITY_HIGH=240, DENSITY_MEDIUM=160, DENSITY_TV=213, DENSITY_XHIGH=320
            if (metrics.densityDpi == android.util.DisplayMetrics.DENSITY_DEFAULT
                    || metrics.densityDpi == android.util.DisplayMetrics.DENSITY_HIGH
                    || metrics.densityDpi == android.util.DisplayMetrics.DENSITY_MEDIUM
                    || metrics.densityDpi == tv
                    || metrics.densityDpi == xhigh
                    )
            {
                return true;
            }
        }
        return false;
    }
}
