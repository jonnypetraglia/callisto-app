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
import com.qweex.callisto.DatabaseConnector;

public class DatabaseMate
{
    DatabaseConnector dbc;
    private static final String TABLE_EPISODES = "episodes";
    public static final String SCHEMA_EPISODES = "CREATE TABLE " + TABLE_EPISODES + " " +
                    "(_id integer primary key autoincrement, " +
                    "show_id TEXT, " +          //The show id for the episode
                    "title TEXT, " +            //The title for a specific episode
                    "date TEXT, " +             //The date of the episode, stored in the form of (yyyyMMddHHmmss)
                    "description TEXT, " +      //The description for a specific episode
                    "length INTEGER, " +        //The length of the episode
                    "link TEXT, " +             //The link to the page for a specific episode
                    "mp3link TEXT, " +          //The link to the audio file for an episode
                    "mp3size INTEGER, " +       //The size of the audio file for an episode, in bytes
                    "vidlink TEXT, " +          //The link to the video file for an episode
                    "vidsize INTEGER, " +       //The size of the video file for an episode, in bytes
                    "new INTEGER, " +           //Whether or not the episode is new
                    "position INTEGER);";       //The last position, in seconds

    public DatabaseMate(DatabaseConnector dbc)
    {
        this.dbc = dbc;
    }

    public void insertEpisode(String show_id,
                              String title,
                              String date,
                              String description,
                              String link,
                              String audioLink,
                              long audioSize,
                              String videoLink,
                              long videoSize)
    {
        ContentValues newEpisode = new ContentValues();
        newEpisode.put("show_id", show_id);
        newEpisode.put("new", 1);
        newEpisode.put("title", title);
        newEpisode.put("date", date);
        newEpisode.put("description", description);
        newEpisode.put("link", link);
        newEpisode.put("mp3link", audioLink);
        newEpisode.put("mp3size", audioSize);
        if(videoLink!=null)
        {
            newEpisode.put("vidlink", videoLink);
            newEpisode.put("vidsize", videoSize);
        }

        dbc.database.insert(TABLE_EPISODES, null, newEpisode);
    }

    public void markNew(long id, boolean is_new)
    {
        ContentValues values = new ContentValues();
        values.put("new", (is_new?1:0));
        dbc.database.update(TABLE_EPISODES, values, "_id=?", new String[] { id + ""});
    }

    public void markAllNew(String show_id, boolean is_new)
    {
        ContentValues values = new ContentValues();
        values.put("new", (is_new?1:0));
        dbc.database.update(TABLE_EPISODES, values, "show_id=?", new String[]{show_id});
    }

    public void updatePosition(long id, long position)
    {
        ContentValues values = new ContentValues();
        values.put("position", position);
        dbc.database.update(TABLE_EPISODES, values, "_id=?", new String[] { id + ""});
    }

    public Cursor getShow(String show, boolean filter)
    {
        return dbc.database.query(TABLE_EPISODES ,                                          /* table */
                new String[] {"_id", "title", "date", "new", "mp3link"},                /* columns */
                "show = '?'" + (filter ? " and new='1'" : "") ,                         /* where */
                new String[] {show},                                                    /* where args */
                null,                                                                   /* groupBy */
                null,                                                                   /* having */
                "date DESC");                                                           /* orderBy */
    }

    public void clearShow(String show_id)
    {
        dbc.database.delete(TABLE_EPISODES, "show=?", new String[] {show_id});
    }

    public Cursor getOneEpisode(long id)
    {
        return dbc.database.query(TABLE_EPISODES,
                new String[] {"_id", "show_id", "title", "new", "date", "description", "length", "link", "mp3link", "mp3size", "vidlink", "vidsize", "new", "position"},
                "_id=?", new String[] {id + ""}, null, null, null);
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
