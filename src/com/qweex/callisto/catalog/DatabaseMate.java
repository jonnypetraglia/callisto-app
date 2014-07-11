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
package com.qweex.callisto.catalog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.qweex.callisto.DatabaseConnector;

import java.text.SimpleDateFormat;

public class DatabaseMate
{
    DatabaseConnector dbc;
    public static final SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss");

    private static final String TABLE_EPISODES = "episodes";
    public static final String SCHEMA_EPISODES = "CREATE TABLE " + TABLE_EPISODES + " " +
                    "(_id integer primary key autoincrement, " +
                    "show TEXT, " +             //The show for the episode
                    "title TEXT, " +            //The title for a specific episode
                    "date TEXT, " +             //The date of the episode, stored in the form of (yyyyMMddHHmmss)
                    "description TEXT, " +      //The description for a specific episode
                    "link TEXT, " +             //The link to the page for a specific episode
                    "imglink TEXT, " +          //The link to the image
                    "mp3link TEXT, " +          //The link to the audio file for an episode
                    "mp3size INTEGER, " +       //The size of the audio file for an episode, in bytes
                    "vidlink TEXT, " +          //The link to the video file for an episode
                    "vidsize INTEGER, " +       //The size of the video file for an episode, in bytes
                    "new INTEGER, " +           //Whether or not the episode is new
                    "length INTEGER, " +        //The length of the episode
                    "position INTEGER);";       //The last position, in seconds

    public DatabaseMate(DatabaseConnector dbc)
    {
        this.dbc = dbc;
    }

    public void insertEpisode(Episode ep)
    {
        String title = ep.Title;
        int marker = title.lastIndexOf('|');
        if(marker>-1)
            title = title.substring(0, marker);

        Log.i("Callisto", "Inserting episode: " + title + " (" + ep.show + ")");

        ContentValues newEpisode = new ContentValues();
        newEpisode.put("show", ep.show);
        newEpisode.put("new", 1);
        newEpisode.put("title", title);
        newEpisode.put("date", sdfRaw.format(ep.Date.getTime()));
        newEpisode.put("description", ep.Desc);
        newEpisode.put("link", ep.Link);
        newEpisode.put("imglink", ep.Image);
        newEpisode.put("mp3link", ep.AudioLink);
        newEpisode.put("mp3size", ep.AudioSize);
        if(ep.VideoLink!=null)
        {
            newEpisode.put("vidlink", ep.VideoLink);
            newEpisode.put("vidsize", ep.VideoSize);
        }

        dbc.database.insertOrThrow(TABLE_EPISODES, null, newEpisode);
    }

    public void markNew(long id, boolean is_new)
    {
        ContentValues values = new ContentValues();
        values.put("new", (is_new?1:0));
        dbc.database.update(TABLE_EPISODES, values, "_id=?", new String[] { id + ""});
    }

    public void markAllNew(String show, boolean is_new)
    {
        ContentValues values = new ContentValues();
        values.put("new", (is_new?1:0));
        dbc.database.update(TABLE_EPISODES, values, "show=?", new String[]{show});
    }

    public void updatePosition(long id, long position)
    {
        ContentValues values = new ContentValues();
        values.put("position", position);
        dbc.database.update(TABLE_EPISODES, values, "_id=?", new String[] { id + ""});
    }

    public Cursor getShow(String show, boolean filter)
    {
        return dbc.database.query(TABLE_EPISODES ,                                      /* table */
                new String[] {"_id", "title", "date", "new", "mp3link"},                /* columns */
                "show=? " + (filter ? "and new='1'" : "") ,                             /* where */
                new String[] {show},                                                    /* where args */
                null,                                                                   /* groupBy */
                null,                                                                   /* having */
                "date DESC");                                                           /* orderBy */
    }

    public void clearShow(String show)
    {
        dbc.database.delete(TABLE_EPISODES, "show=?", new String[] {show});
    }

    public Episode getOneEpisode(long id)
    {
        Cursor e = dbc.database.query(TABLE_EPISODES,
                new String[] {"_id", "show", "title", "date", "description", "length", "link", "imglink", "mp3link", "mp3size", "vidlink", "vidsize", "new", "position"},
                "_id=?", new String[] {id + ""}, null, null, null);
        return new Episode(e);
    }

    public Episode getPreviousEpisode(Episode ep)
    {
        String date = sdfRaw.format(ep.Date.getTime());

        Cursor e = dbc.database.query(TABLE_EPISODES,
                new String[] {"_id", "show", "title", "date", "description", "length", "link", "imglink", "mp3link", "mp3size", "vidlink", "vidsize", "new", "position"},
                "show=? and date<?",
                new String[] {ep.show, date},
                null,
                null,
                "date DESC",
                "1");
        try {
            return new Episode(e);
        }catch(RuntimeException re) {
            return null;
        }
    }

    public Episode getNextEpisode(Episode ep)
    {
        String date = sdfRaw.format(ep.Date.getTime());

        Cursor e = dbc.database.query(TABLE_EPISODES,
                new String[] {"_id", "show", "title", "date", "description", "length", "link", "imglink", "mp3link", "mp3size", "vidlink", "vidsize", "new", "position"},
                "show=? and date>?",
                new String[] {ep.show, date},
                null,
                null,
                "date ASC",
                "1");
        try {
            return new Episode(e);
        }catch(RuntimeException re) {
            return null;
        }
    }

    public void deleteEpisode(long id)
    {
        dbc.database.delete(TABLE_EPISODES, "_id=?", new String[] {id + ""});
    }
    public Cursor searchEpisodes(String searchTerm, String searchShow)
    {
        return dbc.database.query(TABLE_EPISODES,
                new String[] {"_id", "title", "show", "date"},
                (searchShow!="" ? ("show=? and ") : "") + "(title like '%?%' or description like '%?%')", //where
                new String[] {searchShow, searchTerm, searchTerm},
                null, null, null);
    }
}
