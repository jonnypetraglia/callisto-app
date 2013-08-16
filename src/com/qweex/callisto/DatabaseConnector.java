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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/** A tool for communicating with the SQlite database
 *  It has 3 different tables in it: episodes, queue, and calendar
 * @author MrQweex
 */

public class DatabaseConnector 
{
	//------Basic Functions
    /** The file containing the databases. */
	private static final String DATABASE_NAME = "JBShows.db";
    /** One of the tables in the SQL database. */
	private static final String DATABASE_EPISODES = "episodes",
                                DATABASE_QUEUE = "queue",
                                DATABASE_CALENDAR = "calendar",
                                DATABASE_DOWNLOADS = "downloads",
                                DATABASE_STATS = "stats";
    /** The database for the app. */
	private SQLiteDatabase database;
    /** A tool to help with the opening of the database. It's in the Android doc examples, yo.*/
	private DatabaseOpenHelper databaseOpenHelper;
	
	/** Constructor for the class.
	 * @param context The context to associate with the connector.
	 * */
	public DatabaseConnector(Context context) 
	{
	    databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
	}
	
	/** Opens the database so that it can be read or written. */
	public void open() throws SQLException 
	{
	   database = databaseOpenHelper.getWritableDatabase();
        databaseOpenHelper.onUpgrade(database, 0, 1);
	}
	
	/** Closes the database when you are done with it. */
	public void close() 
	{
	   if (database != null)
	      database.close();
	}
	
	//------DATABASE_EPISODES
	/** [DATABASE_EPISODES] Inserts a new episode into the database. The arguments should be self explanatory.
     * @param audioSize In bytes
     * @param videoSize In bytes */
	public void insertEpisode(String show,
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
	   newEpisode.put("show", show);
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
	
	   database.insert(DATABASE_EPISODES, null, newEpisode);
	}
	
	/** [DATABASE_EPISODES] Marks an episode as either new or not new.
     * @param id The ID of an episode in the DATABASE_EPISODES table.
     * @param is_new The status to set of the selected episode
     * */
	public void markNew(long id, boolean is_new)
	{
	   database.execSQL("UPDATE " + DATABASE_EPISODES + " SET new='" + (is_new?1:0) + "'" +
			   " WHERE _id='" + id + "'");
	}
	
	/** [DATABASE_EPISODES] Marks all episodes of a show as either new or not new.
     * @param show The name of the show to edit in the DATABASE_EPISODES table.
     * @param is_new The status to set of the selected show
     * */
	public void markAllNew(String show, boolean is_new)
	{
	   database.execSQL("UPDATE " + DATABASE_EPISODES + " SET new='" + (is_new?1:0) + "'" +
			   " WHERE show='" + show + "'");
	}
	
	/** [DATABASE_EPISODES] Updates the position of an episode entry.
	 * @param id The ID of the entry to set from the DATABASE_EPISODES table.
	 * @param position The new position for the entry
	 */
	public void updatePosition(long id, long position)
	{
		database.execSQL("UPDATE " + DATABASE_EPISODES + " SET position='" + position + "' WHERE _id='" + id + "'");
	}
	
	/** [DATABASE_EPISODES] Gets episodes of a specific show
	 * @param show The show to retrieve from the DATABASE_EPISODES table.
	 * @param filter True to only show new episodes, False to show all
	 * @return A Cursor containing the episodes
	 */
	public Cursor getShow(String show, boolean filter)
	{
		   return database.query(DATABASE_EPISODES /* table */,
				    new String[] {"_id", "title", "date", "new", "mp3link"} /* columns */,
				    "show = '" + show + "'" + (filter ? " and new='1'" : "") /* where or selection */,
		        	null /* selectionArgs i.e. value to replace ? */,
		        	null /* groupBy */,
		        	null /* having */,
				   	"date DESC"); /* orderBy */
	}
	
	/** [DATABASE_EPISODES] Clears all episodes out of a show.
	 * @param show The show to clear from the DATABASE_EPISODES table.
	 */
	public void clearShow(String show)
	{
		database.delete(DATABASE_EPISODES, "show = '" + show + "'", null);
	}
	
	/** [DATABASE_EPISODES] Gets one specific episode from the database.
	 * @param id The ID of the entry to retrieve
	 * @return A Cursor containing the one episode.
	 */
	public Cursor getOneEpisode(long id) 
	{
		Cursor c = database.query(DATABASE_EPISODES, new String[] {"_id", "show", "title", "new", "date", "description", "link", "mp3link", "mp3size", "vidlink", "vidsize", "position"},
					"_id=" + id, null, null, null, null);
		return c; 
	}
	
	/** [DATABASE_EPISODES] Deletes one episode
	 * @param id The ID of the entry to be deleted from the DATABASE_EPISODES table.
	 */
	public void deleteEpisode(long id) 
	{
	   database.delete(DATABASE_EPISODES, "_id=" + id, null);
	}
	
	/** [DATABASE_EPISODES] Searches all shows for a term
	 * @param searchTerm The search term that is to be searched....for....from the DATABASE_EPISODES table. Is searched inside title and description
     * @param searchShow The search show to be filtered, if any, from the DATABASE_EPISODES table.
	 * @return A cursor containing the results
	 */
	public Cursor searchEpisodes(String searchTerm, String searchShow)
	{
		Cursor c = database.query(DATABASE_EPISODES, new String[] {"_id", "title", "show", "date"}
				, "(title like '%" + searchTerm + "%' or " +
				  "description like '%" + searchTerm + "%')" + 
				  (searchShow!="" ? (" and show='" + searchShow + "'") : "")
				, null, null, null, null);
		return c;
	}

    /** [DATABASE_EPISODES] Retrieves the length of the episode file
     * @param id The id of the element to examine from the DATABASE_EPISODES table.
     * @return The length
     */
    public int getLength(long id)
    {
        Cursor c = database.query(DATABASE_EPISODES, new String[] {"_id", "length"},
                "_id=" + id, null, null, null, null);
        try {
            c.moveToFirst();
            return c.getInt(c.getColumnIndex("length"));
        } catch(Exception e)
        {
            return 0;
        }
    }
	
	
	//-------DATABASE_QUEUE
	/** [DATABASE_QUEUE] Gets the entirety of the queue.
	 * @return A Cursor containing the queue from the DATABASE_QUEUE table.
	 */
	public Cursor getQueue()
	{
           this.deleteQueueItem(-1);
		   Cursor things = null;
		   things = database.query(DATABASE_QUEUE,
				    new String[] {"_id", "identity", "current", "video", "streaming"},
				    null,
		        	null,
		        	null,
		        	null,
				   	"_id");
		   return things;
	}
	
	/** [DATABASE_QUEUE] 
	 * @param identity The identity (note, not ID) of what to be appended. "Identity" is the ID from the DATABASE_EPISODES table, NOT from DATABASE_QUEUE.
	 * @param isStreaming True if it is a streaming entry, false otherwise
     * @param isVideo True if it is a video entity, false otherwise.
	 */
	public void appendToQueue(long identity, boolean isStreaming, boolean isVideo)
	{
        String TAG = StaticBlob.TAG();
        if(database.query(DATABASE_QUEUE, new String[] {"_id"}, "identity=" + identity + " AND video='" + (isVideo?1:0) + "'",
                null, null, null, null).getCount()==0)
        {
            ContentValues newEntry = new ContentValues();
            newEntry.put("identity", identity);
            newEntry.put("current", 0);
            newEntry.put("streaming", isStreaming ? 1 : 0);
            newEntry.put("video", isVideo ? 1 : 0);
            database.insert(DATABASE_QUEUE, null, newEntry);
        } else
            Log.w(TAG, "Item is already in queue: " + identity);
	}
	
	/** [DATABASE_QUEUE] Updates a queue item.
	 * @param id The ID to update from the DATABASE_QUEUE table.
	 * @param identity The identity (that is, the ID for DATABASE_EPISODES) for the updated item to now hold
	 * @param isCurrent Greater than 0 if it is the current track, 0 otherwise.
	 * @param streaming Greater than 0 if it is streaming, 0 otherwise.
	 */
	private void updateQueue(long id, long identity, int isCurrent, int streaming, int video)
	{
	   ContentValues editEpisode = new ContentValues();
	   editEpisode.put("identity", identity);
	   editEpisode.put("current", isCurrent);
	   editEpisode.put("streaming", streaming);
        editEpisode.put("video", video);
	
	   database.update(DATABASE_QUEUE, editEpisode, "_id=" + id, null);
	}
	
	/** @Deprecated
     * [DATABASE_QUEUE] Moves an item either forward or back in the queue
	 * @param id1 The ID to move either forward or back from the DATABASE_QUEUE table.
	 * @param dir The direction to move; >0 is up one, <0 is down one, 0 is delete
	 */
	public void move(long id1, int dir)
	{
		   //Get the one that is selected
	       Cursor c1 = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "current", "video", "streaming"},	"_id=" + id1, null, null, null, null);
	       c1.moveToFirst();
	       long identity1 = c1.getLong(c1.getColumnIndex("identity"));
	       int current1 = c1.getInt(c1.getColumnIndex("current"));
	       int streaming1 = c1.getInt(c1.getColumnIndex("streaming"));
           int video1 = c1.getInt(c1.getColumnIndex("video"));
		   
	       //Get the one that it will move with
	       Cursor c2;
	       if(dir<0)
	    	   c2 = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "current", "video", "streaming"},	"_id<" + id1, null, null, null, "_id DESC", "1");
	       else
	    	   c2 = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "current", "video", "streaming"},	"_id>" + id1, null, null, null, "_id ASC", "1");
	       if(dir==0)
	    	   deleteQueueItem(id1);
	       if(c2.getCount()==0)
	    	   return;
	       c2.moveToFirst();
	       long id2 = c2.getLong(c2.getColumnIndex("_id"));
	       long identity2 = c2.getLong(c2.getColumnIndex("identity"));
	       int current2 = c2.getInt(c2.getColumnIndex("current"));
	       int streaming2 = c2.getInt(c2.getColumnIndex("streaming"));
           int video2 = c2.getInt(c2.getColumnIndex("video"));
	       
	       if(dir!=0)
	       {
		       updateQueue(id1, identity2, current2, streaming2, video2);
		       updateQueue(id2, identity1, current1, streaming1, video1);
	       }

	}

    /** [DATABASE_QUEUE] Moves an item in the queue
     * @param fromID The ID that is the target of the moving.
     * @param toID The ID that the item will be inserted before(?) from the DATABASE_QUEUE table.
     */
    public void moveQueue(int fromID, int toID)
    {
        long special = -1;
        this.deleteQueueItem(special);
        database.execSQL("UPDATE " + DATABASE_QUEUE  + " SET _id=" + special + " WHERE _id=" + fromID);
        //Move up
            //This has to be more complicated because when it walks through linearly and tries to update, it runs into a uniqueness problem.
            // Example:
            // 5. B
            // 6. C
            // 7. D
            // Move D to point 5
            // STEP 1:
            // 6. B
            // 6. C
            // -1. D
            // ERROR
        if(toID<fromID)
        {
            Cursor c = database.query(DATABASE_CALENDAR, new String[] {"_id"},
                    "_id>=" + toID + " AND _id<" + fromID, null, null, null, "_id");
            if(c.getCount()>0)
            {
                c.moveToLast();
                while(!c.isBeforeFirst())
                {
                    database.execSQL("UPDATE " + DATABASE_QUEUE + " SET _id=_id+1 WHERE _id=" + c.getLong(c.getColumnIndex("_id")));
                    c.moveToPrevious();
                }
            }
            //Failed attemps at trying to do it without a while loop
            //database.execSQL("UPDATE " + DATABASE_QUEUE + " SET _id=_id+1 WHERE _id=(Select * from " + DATABASE_QUEUE +  " WHERE (_id >= " + toID + " AND _id<" + fromID + ") Order By _id DESC);");
            //database.execSQL("UPDATE " + DATABASE_QUEUE  + " SET _id=_id+1 WHERE (_id>=" + toID + " AND _id<" + fromID + ")");
        }
        //Move down
        else    //(to>from)
        {
            database.execSQL("UPDATE " + DATABASE_QUEUE  + " SET _id=_id-1 WHERE (_id>" + fromID + " AND _id<=" + toID + ")");
        }
        //Set the actual item that is being moved
        database.execSQL("UPDATE " + DATABASE_QUEUE + " SET _id=" + toID + " WHERE _id=" + special);
    }
	
	/** [DATABASE_QUEUE] Gets the number of items in the queue.
	 * @return The number of items in the queue.
	 */
	public long queueCount() {
	    String sql = "SELECT COUNT(*) FROM " + DATABASE_QUEUE;
	    SQLiteStatement statement = database.compileStatement(sql);
	    long count = statement.simpleQueryForLong();
	    return count;
	}
	
	/** [DATABASE_QUEUE] Removes all items from the queue */
	public void clearQueue() {
		database.execSQL("DELETE FROM " + DATABASE_QUEUE + ";");
	}
	
	/** [DATABASE_QUEUE] Tests if an item is in the the queue
	 * @param identity The ID of an item in the DATABASE_EPISODES to check for.
	 * @return
	 */
	public boolean isInQueue(long identity) 
	{
	   Cursor c = database.query(DATABASE_QUEUE, new String[] {"_id"},
			   					"identity=" + identity, null, null, null, null);
	   return (c.getCount()>0);
	}
	
	/** [DATABASE_QUEUE] Removes one item from the queue
	 * @param id The ID of the item from the DATABASE_QUEUE table to remove.
	 */
	public void deleteQueueItem(long id) 
	{
	   database.delete(DATABASE_QUEUE, "_id=" + id, null);
	}
	
	/** [DATABASE_QUEUE] Advances the marker for the current item of the queue and returns a cursor containing that item
	 * @param forward >0 moves the marker forward, <0 moves it backward
	 * @return A Cursor containing the new current item
	 */
	public Cursor advanceQueue(int forward)
	{
        String TAG = StaticBlob.TAG();
		long id;
		Cursor c = currentQueueItem();

        //No current item is found
		if(c==null || c.getCount()==0)
		{
			Log.v(TAG, "No current found in queue.");
			c = getQueue();
			if(c.getCount()==0)
				return null;
			c.moveToFirst();
			id = c.getLong(c.getColumnIndex("_id"));
			database.execSQL("UPDATE " + DATABASE_QUEUE  + " SET current=1 WHERE _id=" + id); //CLEAN: make more efficient
			return getQueue();
		}
        //Update the queue database
		if(forward!=0)
		{
			Log.v(TAG, "An old current was found in the queue");
			c.moveToFirst();
			id = c.getLong(c.getColumnIndex("_id"));
			//Set the old one to be not current
			database.execSQL("UPDATE " + DATABASE_QUEUE  + " SET current=0" + " WHERE _id=" + id + "");
			//Get the new one and set it to be current
			if(forward<0)
				c = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "video", "streaming"}, "_id<" + id, null, null, null, "_id DESC", "1");
			else
				c = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "video", "streaming"}, "_id>" + id, null, null, null, null, "1");
			if(c.moveToFirst())
			{
				Log.v(TAG, "A new current was found in the queue");
				id = c.getLong(c.getColumnIndex("_id"));
				database.execSQL("UPDATE " + DATABASE_QUEUE  + " SET current='1'" + " WHERE _id='" + id + "'");
			}
		}
		return currentQueueItem();
	}
	
	/** [DATABASE_QUEUE] Gets the current item in the queue.
	 * @return A Cursor containing the current item from the DATABASE_QUEUE table.
	 */
	public Cursor currentQueueItem()
	{
	   Cursor c = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "video", "streaming"}, "current>'0'", null, null, null, null, "1");
	   return c;
		
	}
	
	//------DATABASE_CALENDAR
	/** [DATABASE_CALENDAR] Inserts a new event. Parameters should be (mostly) self explanatory
	 * @param recurring 0 if the event is non-recurring, the day index otherwise (1-7 for Sun-Sat)
	 */
	public void insertEvent(String show,
			  String type,
			  String date,
			  String time,
			  int recurring) 
	{
		ContentValues newEvent = new ContentValues();
		newEvent.put("show", show);
		newEvent.put("type", type);
		newEvent.put("date", date);
		newEvent.put("time", time);
		newEvent.put("recurring", recurring);
		
		database.insert(DATABASE_CALENDAR, null, newEvent);
	}
	
	/** [DATABASE_CALENDAR] Gets one event. Isn't that obvious?
	 * @param id The ID of the event to retrieve from the DATABASE_CALENDAR table.
	 * @return A cursor containing the one event
	 */
	public Cursor getOneEvent(long id) 
	{
		Cursor c = database.query(DATABASE_CALENDAR, new String[] {"_id", "show", "type", "date", "time", "recurring"},
					"_id=" + id, null, null, null, null);
		return c; 
	}
	
	/** [DATABASE_CALENDAR] Retrieves all events for a specific day
	 * @param specificDay the specific day (e.g. 20120123)
	 * @param dayOfWeek The day....wait for it....of the week. 1-7 for Sun-Sat
	 * @return A Cursor containing all events for the specified day
	 */
	public Cursor dayEvents(String specificDay, int dayOfWeek) 
	{
		String query = "recurring='" + dayOfWeek + "' or date like '" + specificDay + "'";
		String sortby = "time";
		Cursor c = database.query(DATABASE_CALENDAR, new String[] {"_id", "show", "type", "date", "time"},
			   					query, null, null, null,
			   					sortby);
	   return c;
	}
	
	/** [DATABASE_CALENDAR] Retrieves the number of number of events
	 * @return The number of events
	 */
	public int eventCount()
	{
		return (database.query(DATABASE_CALENDAR, new String[] {},
					null, null, null, null,
					null)).getCount();
	}
	
	/** [DATABASE_CALENDAR] Clears all events from the database */
	public void clearCalendar() {
		database.execSQL("DELETE FROM " + DATABASE_CALENDAR + ";");
	}

    /** [DATABASE_EPISODES] Insert a length */
    public void putLength(String title, long length)
    {
        database.execSQL("UPDATE " + DATABASE_EPISODES + " SET length='" + length + "'" +
                " WHERE title='" + title + "'");
    }

    /** [DATABASE_DOWNLOADS] Retrieves all active downloads */
    public Cursor getActiveDownloads()
    {
        return database.query(DATABASE_DOWNLOADS, new String[] {"_id", "identity", "video", "active"},
                "active>0", null, null, null, "_id");
    }

    /** [DATABASE_DOWNLOADS] Retrieves all completed downloads */
    public Cursor getCompleteDownloads()
    {
        return database.query(DATABASE_DOWNLOADS, new String[] {"_id", "identity", "video", "active"},
                "active<1", null, null, null, "_id ASC");
    }

    /** [DATABASE_DOWNLOADS] Tests if an id is in the download queue */
    public boolean isInDownloadQueue(long id)
    {
        Cursor c = database.query(DATABASE_DOWNLOADS, new String[] {"_id", "identity", "video", "active"},
                "_id='" + id + "'", null, null, null, null);
        return c.getCount()>0;
    }

    /** [DATABASE_DOWNLOADS] Adds a new active download
     * @param identity The Identity (i.e. _id from EPISODES)
     * @param video If the item is a video, otherwise it is audio
     * @return If a new download was actually added
     * */
    public boolean addDownload(long identity, boolean video)     //note 'identity' == the _id in EPISODES table
    {
        String TAG = StaticBlob.TAG();
        Log.d(TAG, "Adding: " + identity + " . " + video);
        Cursor c = database.query(DATABASE_DOWNLOADS, new String[] {"_id", "identity", "video", "active"},
                "identity='" + identity + "' AND video='" + (video?1:0) + "'", null, null, null, null);
        if(c.getCount()>0)
        {
            Log.d(TAG, "Found duplicate when trying to add: " + identity + " . " + video);
            return false;
        }

        ContentValues newDownload = new ContentValues();
        newDownload.put("identity", identity);
        newDownload.put("video", video);
        newDownload.put("active", 1);

        database.insert(DATABASE_DOWNLOADS, null, newDownload);
        return true;
    }

    /** [DATABASE_DOWNLOADS] Adds a new active download
     * @param id The Identity (i.e. _id from DOWNLOADS)
     * @return If a download was deleted
     * */
    public boolean removeDownload(long id)     //note 'id' == the _id in DOWNLOADS table
    {
        return database.delete(DATABASE_DOWNLOADS, "_id=" + id, null)>0;
    }

    /** [DATABASE_DOWNLOADS] Adds a new active download */
    public void markDownloadComplete(long id)     //note 'id' == the _id in DOWNLOADS table
    {
        database.execSQL("UPDATE " + DATABASE_DOWNLOADS + " SET active='0'" +
                " WHERE _id='" + id + "'");
    }

    /** [DATABASE_DOWNLOADS] Adds a new active download */
    public void clearActiveDownloads()
    {
        database.execSQL("DELETE FROM " + DATABASE_DOWNLOADS + " WHERE active>0;");
    }

    /** [DATABASE_DOWNLOADS] Adds a new active download */
    public void clearCompleteDownloads()
    {
        database.execSQL("DELETE FROM " + DATABASE_DOWNLOADS + " WHERE active<1;");
    }

	/** Helper open class for DatabaseConnector */
	private class DatabaseOpenHelper extends SQLiteOpenHelper 
	{
        static final int DB_VERSION = 2;
        public DatabaseOpenHelper(Context context, String name, CursorFactory factory, int version)
	   {
	      super(context, name, factory, version);
	   }
	
	   @Override
	   public void onCreate(SQLiteDatabase db) 
	   {
	      String createQuery = "CREATE TABLE " + DATABASE_EPISODES + " " +
	         "(_id integer primary key autoincrement, " +
	         	"show TEXT, " +             //The show for the episode
	         	"new INT, " +               //Whether or not the episode is new
	         	"title TEXT, " +            //The title for a specific episode
	         	"date TEXT, " +             //The date of the episode, stored in the form of Callisto.sdfRaw (yyyyMMddHHmmss)
	         	"description TEXT, " +      //The description for a specific episode
	         	"link TEXT, " +             //The link to the page for a specific episode
	         	"position INTEGER, " +      //The last position, in seconds
	         	"mp3link TEXT, " +          //The link to the audio file for an episode
	         	"mp3size INTEGER, " +       //The size of the audio file for an episode, in bytes
	         	"vidlink TEXT, " +          //The link to the video file for an episode
	         	"vidsize INTEGER);";        //The size of the video file for an episode, in bytes
	      db.execSQL(createQuery);
	      
	      
	      String createQuery2 = "CREATE TABLE " + DATABASE_QUEUE + " " + 
	 	         "(_id integer primary key autoincrement, " +
	 	         	"identity INTEGER, " +      //An ID that is in the DATABASE_EPISODES table. Essentially it should be a foreign key, but it's not because I am teh dumb with databases.
	 	         	"current INTEGER, " +       //>0 if is the current queue item; ideally only one row in the table should have >0.
	 	         	"video INTEGER, " +         //>0 if the queue item is a video file, otherwise it's assumed to be audio
	 	         	"streaming INTEGER);";      //>0 if the queue item is streaming, otherwise it's assumed to be local
	 	      db.execSQL(createQuery2);
	 	      
	 	  String createQuery3 = "CREATE TABLE " + DATABASE_CALENDAR + " " + 
	 			 "(_id integer primary key autoincrement, " +
	 			   "show TEXT, " +              //The show for the event, same as the shows of DATABASE_EPISODES
	 			   "type TEXT, " +              //The type for the event, either "LIVE" or "RELEASE"
	 			   "date TEXT, " +              //The date of the event, stored in format yyyyMMdd
	 			   "time TEXT, " +              //The time of the event, stored in format HHmmss
	 			   "recurring INTEGER);";       //>0 if the event is recurring every week, otherwise it's a one-time event.
	 	  	db.execSQL(createQuery3);

           String createQuery4 = "CREATE TABLE " + DATABASE_DOWNLOADS + " " +
                   "(_id integer primary key autoincrement, " +
                   "identity INTEGER, " +
                   "video INTEGER, " +
                   "active INTEGER);";        //An ID that is in the DATABASE_EPISODES table. Essentially it should be a foreign key, but it's not because I am teh dumb with databases.
           db.execSQL(createQuery4);

           String createQuery5 = "CREATE TABLE " + DATABASE_STATS + " " +
                   "(_id integer primary key autoincrement, " +
                   "show TEXT, " +
                   "audio_seconds INTEGER, " +
                   "video_seconds INTEGER);";
           db.execSQL(createQuery5);
	   }

	   @Override
	   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	   {
           String TAG = StaticBlob.TAG();
           //I really should have taken my Database class before trying to mess with databases.
           Log.e(TAG, "Upgrading DB");
           try {
           String sql = "ALTER TABLE " + DATABASE_EPISODES + " ADD COLUMN length INTEGER";
           db.execSQL(sql);
           } catch(SQLiteException e){}

           try {
               String sql = "ALTER TABLE " + DATABASE_QUEUE + " ADD COLUMN video INTEGER";
               db.execSQL(sql);
           } catch(SQLiteException e){}

           try {
               String sql = "ALTER TABLE " + DATABASE_QUEUE + " DROP COLUMN streaming";
               db.execSQL(sql);
           } catch(SQLiteException e){}

           try {
               String createQuery4 = "CREATE TABLE " + DATABASE_DOWNLOADS + " " +
                       "(_id integer primary key autoincrement, " +
                       "identity INTEGER, " +
                       "video INTEGER, " +
                       "active INTEGER);";        //An ID that is in the DATABASE_EPISODES table. Essentially it should be a foreign key, but it's not because I am teh dumb with databases.
               db.execSQL(createQuery4);
           } catch(SQLiteException e){}
	   }
	}
}
