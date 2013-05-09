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
package com.qweex.callisto;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.qweex.callisto.listeners.OnCompletionListenerWithContext;
import com.qweex.callisto.listeners.OnErrorListenerWithContext;
import com.qweex.callisto.listeners.OnPreparedListenerWithContext;

import java.text.SimpleDateFormat;

/**
 * Literally a giant "blob" to hold all the static things in the app, like date formats that are used frequently, preferences, shared resources, etc.
 */
public class StaticBlob
{
    /** One of the various date formats used across various Activities. The usage for most should be self-documenting from the name. */
    public static final SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final SimpleDateFormat sdfRawSimple1 = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat sdfRawSimple2 = new SimpleDateFormat("HHmmss");
    public static final SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm aa");
    public static final SimpleDateFormat sdfSource = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
    public static final SimpleDateFormat sdfFile = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat sdfHuman = new SimpleDateFormat("MMM d");
    public static final SimpleDateFormat sdfHumanLong = new SimpleDateFormat("MMM d, yyyy");
    //TODO: wtf?
    public static final int NOTIFICATION_ID = 1337;
    //TODO: wtf?
    public final static String PREF_FILE = "alarms";
    /** The media players are used across the app to control playing either live or podcast'ed episodes. */
    public static MediaPlayer mplayer = null;
    /** Used to connect to the SQLlite database. For more information, see the DatabaseConnector class.*/
    public static DatabaseConnector databaseConnector;
    /** Notifications are used in various activities */
    public static Notification notification_download;
    public static Notification notification_playing;
    public static Notification notification_chat;
    public static Notification notification_alarm;
    /** The path on the SD card to store downloaded episodes. Customizable via the settings dialog. */
    public static String storage_path;
    /** The number of downloads that has been queued up. */
    public static int downloading_count = 0;
    /** The current download number. */
    public static int current_download = 1;
    public static SimpleDateFormat sdfDestination;
    /** Shared Drawable resources for updating the player control graphics. */
    public static Drawable playDrawable;
    public static Drawable pauseDrawable;
    /** An instance of the PlayerInfo class, to keep track of the info of a track when updating the player controls across the activities. */
    public static PlayerInfo playerInfo;
    public static String[] SHOW_LIST_VIDEO;
    public static String[] SHOW_LIST_AUDIO;
    /** A static array containing corresponding info for the shows.
     *  A sub-heading will start with a space in the name list and have a value of null in the feeds.
     */
    public static String[] SHOW_LIST;
    /** The value of a dip, to be used programatically when updating a view. */
    public static float DP;
    /** One of the Views for the IRC client. Having them static lets them be updated at any time, and not be destroyed when you leave the IRC screen. */
    public static TextView chatView;
    public static TextView logView;
    /** If you should be a "weirdo" that likes dates in a logically correct manner. */
    public static boolean europeanDates;
    //TODO: wtf?
    public static OnCompletionListenerWithContext trackCompleted;
    public static OnErrorListenerWithContext trackCompletedBug;
    public static OnPreparedListenerWithContext mplayerPrepared;
    /** Used for notifications */
    public static NotificationManager mNotificationManager;
    //TODO: wtf?
    public static SharedPreferences alarmPrefs;
    /** The status of the live player; true if it is playing, false if it is paused or not in use. */
    public static boolean live_isPlaying = false;
    /** The Version of Callisto. Set to -1 if it cannot be determined. */
    public static int appVersion = -1;
    /** Contains whether or not the activity has been launched via the widget. **/
    public static boolean is_widget;
    /** Locks the wifi when downloading or streaming **/
    public static WifiManager.WifiLock Live_wifiLock;
    //TODO: wtf
    public static Dialog errorDialog;
    //TODO: wtf
    public static Dialog liveDg;
    //TODO: wtf
    public static TextView timeView;
    //TODO: wtf
    public static ProgressBar timeProgress;

    public static void formatAlertDialogButtons(AlertDialog d)
    {
        d.getButton(Dialog.BUTTON_POSITIVE).setBackgroundResource(R.drawable.blue_button);
        d.getButton(Dialog.BUTTON_POSITIVE).setPadding((int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP));
        d.getButton(Dialog.BUTTON_NEGATIVE).setBackgroundResource(R.drawable.blue_button);
        d.getButton(Dialog.BUTTON_NEGATIVE).setPadding((int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP), (int) (15 * StaticBlob.DP));
        ((View)d.getButton(Dialog.BUTTON_POSITIVE).getParent()).setBackgroundResource(R.color.backClr);
        View msg = ((ViewGroup) ((ViewGroup)(d.getButton(Dialog.BUTTON_POSITIVE).getParent().getParent().getParent())).getChildAt(1)).getChildAt(0);
        msg.setBackgroundResource(R.color.backClr);
        if(((TextView)((android.widget.ScrollView)msg).getChildAt(0))!=null)
            ((TextView)((android.widget.ScrollView)msg).getChildAt(0)).setTextColor(d.getContext().getResources().getColor(R.color.txtClr));
        //ViewGroup x = (ViewGroup) ((ViewGroup) ((ViewGroup)(d.getButton(Dialog.BUTTON_POSITIVE).getParent().getParent().getParent())).getChildAt(0)).getChildAt(0);
        //x.setBackgroundResource(R.color.backClr);
    }
}
