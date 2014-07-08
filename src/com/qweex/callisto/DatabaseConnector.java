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

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;


public class DatabaseConnector
{
    private final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "Callisto.db";

    public SQLiteDatabase database;
    private DatabaseOpenHelper databaseOpenHelper;


    /** Constructor for the class.
     * @param context The context to associate with the connector.
     * */
    public DatabaseConnector(Context context)
    {
        databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
        open();
    }

    /** Opens the database so that it can be read or written. */
    public void open() throws SQLException
    {
        database = databaseOpenHelper.getWritableDatabase();
    }

    /** Closes the database when you are done with it. */
    public void close()
    {
        if (database != null)
            database.close();
    }

    /** Helper open class for DatabaseConnector */
    private class DatabaseOpenHelper extends SQLiteOpenHelper
    {
        public DatabaseOpenHelper(Context context, String name, CursorFactory factory, int version)
        {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(com.qweex.callisto.catalog.DatabaseMate.SCHEMA_EPISODES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            switch(oldVersion) {
                case 2:             //Initial upgrade to v2....Drop ALL the tables!
                    db.execSQL("DROP TABLE IF EXISTS episodes");
                    db.execSQL("DROP TABLE IF EXISTS queue");
                    db.execSQL("DROP TABLE IF EXISTS calendar");
                    db.execSQL("DROP TABLE IF EXISTS downloads");
                    db.execSQL("DROP TABLE IF EXISTS stats");
                    db.execSQL("DROP TABLE IF EXISTS custom_feeds");
            }
        }
    }
}
