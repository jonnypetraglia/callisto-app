/*
 * Copyright (C) 2012-2013 Qweex
 * This file is a part of Callisto.
 *
 * Callisto is free software; it is released under the
 * Open Software License v3.0 without warranty. The OSL is an OSI approved,
 * copyleft license, meaning you are free to redistribute
 * the source code under the terms of the OSL.
 *
 * You should have received a copy of the Open Software License
 * along with Callisto; If not, see <http://rosenlaw.com/OSL3.0-explained.htm>
 * or check OSI's website at <http://opensource.org/licenses/OSL-3.0>.
 */
package com.qweex.callisto.widgets;

import java.util.Arrays;

import com.qweex.callisto.*;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**  Manages the widgets for the Callisto app.
 * @author MrQweex */

public class CallistoWidget extends AppWidgetProvider
{
    /** Called when the widgets have been notified that they need to be updated
     * @param context Check the Android docs
     * @param appWidgetManager Check the Android docs
     * @param appWidgetIds Check the Android docs
     */
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        final int N = appWidgetIds.length;

        Log.v("CallistoWidget:onUpdate",  "Updating widgets " + Arrays.asList(appWidgetIds));
        for (int i = 0; i < N; i++)
        {
            int appWidgetId = appWidgetIds[i];
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
            //views.setImageViewBitmap(R.id.widgetButton, playIcon);

            PendingIntent pi = PendingIntent.getActivity(context, 0, new Intent(context, Callisto.class), PendingIntent.FLAG_CANCEL_CURRENT);
            views.setOnClickPendingIntent(R.id.widget, pi);

            Intent intent = new Intent(context, CallistoWidget.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);

            if(StaticBlob.playerInfo!=null)
                Log.v("CallistoWidget:onUpdate", "WIDGET UPDATE" + StaticBlob.playerInfo.isPaused);
            if((Live.live_player!=null && !StaticBlob.live_isPlaying) ||
                    (StaticBlob.playerInfo!=null && StaticBlob.playerInfo.isPaused))
                views.setImageViewResource(R.id.widgetButton, R.drawable.ic_action_playback_play);
                //views.setImageViewBitmap(R.id.widgetButton, playIcon);
            else
                views.setImageViewResource(R.id.widgetButton, R.drawable.ic_action_playback_pause);
            //views.setImageViewBitmap(R.id.widgetButton, pauseIcon);


            // Updates the text and button
            if(StaticBlob.playerInfo!=null && StaticBlob.playerInfo.title!=null)
            {
                views.setTextViewText(R.id.widgetTitle, StaticBlob.playerInfo.title);
                views.setTextViewText(R.id.widgetShow, StaticBlob.playerInfo.show);
            }
            else
            {
                views.setTextViewText(R.id.widgetTitle, "Callisto");
                views.setTextViewText(R.id.widgetShow, "(Press to enqueue episodes)");
            }
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /** Produces a call to update all the widgets.
     * @param c The context, used for getApplicationContext()*/
    public static void updateAllWidgets(Context c)
    {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c.getApplicationContext());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(c, CallistoWidget.class));
        if (appWidgetIds.length > 0)
            new CallistoWidget().onUpdate(c, appWidgetManager, appWidgetIds);
    }


    /** Called when the user presses a button on the widget. Calls the Callisto static method to play or pause.
     * @param context The...uh....context....
     * @param intent The...uh....intent....
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);
        if("android.appwidget.action.APPWIDGET_UPDATE".equals(intent.getAction())
                || "android.appwidget.action.APPWIDGET_ENABLED".equals(intent.getAction()))
            return;
        Log.i("CallistoWidget:onReceive", "A button has been pressed on a widget" + intent.getAction());
        StaticBlob.init(context);
        StaticBlob.is_widget = true;
        StaticBlob.pauseCause = StaticBlob.PauseCause.User;
        PlayerControls.playPause(context, null);
    }

}
