package com.qweex.callisto;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

/** Class to assist in contacting the Sqlite database.
 *
 * Also uses several "DatabaseMate" classes that have methods divided by specific use case.
 *
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class DatabaseConnector
{
    /** The Database Version; for use with updating new versions. */
    private final int DATABASE_VERSION = 2;
    /** The database file name. */
    private static final String DATABASE_NAME = "Callisto.db";

    /** The raw Database object. */
    public SQLiteDatabase database;
    /** An opener to help with opening and upgrading the database. */
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
        /** Inherited constructor. */
        public DatabaseOpenHelper(Context context, String name, CursorFactory factory, int version)
        {
            super(context, name, factory, version);
        }

        /** Inherited method; called on database creation, i.e. when the app is freshly installed.
         * @param db The raw database object to execute queries on.
         */
        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(com.qweex.callisto.catalog.DatabaseMate.SCHEMA_EPISODES);
        }

        /** Inherited method; called when the app is upgraded.
         * @param db The raw database object to execute queries on.
         * @param oldVersion The old version of the app.
         * @param newVersion The new version of the app.
         */
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
                    onCreate(db);
            }
        }
    }
}
