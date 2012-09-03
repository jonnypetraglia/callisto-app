/*
Copyright (C) 2012 Qweex
This file is a part of Callisto.

Callisto is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

Callisto is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Callisto; If not, see <http://www.gnu.org/licenses/>.
*/
package com.qweex.callisto;

import java.util.Arrays;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**  Manages the widgets for the Callisto app.
 * @author MrQweex */

public class CallistoWidget extends AppWidgetProvider {
	
	/** Called when the widgets have been notified that they need to be updated
	 * @param context Check the Android docs
	 * @param appWidgetManager Check the Android docs
	 * @param appWidgetIds Check the Android docs
	 */
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    final int N = appWidgetIds.length;
    Log.v("CallistoWidget:onUpdate",  "Updating widgets " + Arrays.asList(appWidgetIds));
    for (int i = 0; i < N; i++) {
      int appWidgetId = appWidgetIds[i];
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
      
      
      PendingIntent pi = PendingIntent.getActivity(context, 0, new Intent(context, Callisto.class), PendingIntent.FLAG_CANCEL_CURRENT);
      views.setOnClickPendingIntent(R.id.widget, pi);
      
      Intent intent = new Intent(context, WidgetHandler.class);
      intent.putExtra("is_widget", true);
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
      
      views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);
      
      // Updates the text and button
      if(Callisto.playerInfo!=null)
      {
	      views.setTextViewText(R.id.widgetTitle, Callisto.playerInfo.title);
	      views.setTextViewText(R.id.widgetShow, Callisto.playerInfo.show);
	      if(Callisto.playerInfo.isPaused)
	    	  views.setImageViewResource(R.id.widgetButton, R.drawable.ic_media_play_lg);
	      else
	    	  views.setImageViewResource(R.id.widgetButton, R.drawable.ic_media_pause_lg);
      }
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }
}
