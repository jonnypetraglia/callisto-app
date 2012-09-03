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

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

/** A very small class that handles when the user presses the button on the widget.
 * @author MrQweex */

public class WidgetHandler extends Activity {

	/** Called when the activity is first created. Immediately calls Callisto's playPause_ function.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		finish();
		Callisto.is_widget = true;
		Callisto.playPause_(this, null);
		WidgetHandler.updateAllWidgets(this);
	}
	
	/** Produces a call to update all the widgets. 
	 * @param c The context, used for getApplicationContext()*/
	public static void updateAllWidgets(Context c){
	    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c.getApplicationContext());
	    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(c, CallistoWidget.class));
	    if (appWidgetIds.length > 0) {
	        new CallistoWidget().onUpdate(c, appWidgetManager, appWidgetIds);
	    }
	}
}
