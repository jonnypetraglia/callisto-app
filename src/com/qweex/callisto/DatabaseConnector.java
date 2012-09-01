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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
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
	private static final String DATABASE_NAME = "JBShows.db";
	private static final String DATABASE_TABLE = "episodes";
	private static final String DATABASE_QUEUE = "queue";
	private static final String DATABASE_CALENDAR = "calendar";
	private SQLiteDatabase database;
	private DatabaseOpenHelper databaseOpenHelper = null;
	
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
	}
	
	/** Closes the database when you are done with it. */
	public void close() 
	{
	   if (database != null)
	      database.close();
	}
	
	//------DATABASE_TABLE
	/** [DATABASE_TABLE] Inserts a new episode into the database. The arguments should be self explanatory. */
	public void insertEpisode(String show,
							  String title,
							  String date,
							  String description,
							  String medialink,
							  long mediasize,
							  boolean isVideo) 
	{
	   ContentValues newEpisode = new ContentValues();
	   newEpisode.put("show", show);
	   newEpisode.put("new", 1);
	   newEpisode.put("title", title);
	   newEpisode.put("date", date);
	   newEpisode.put("description", description);
	   newEpisode.put((isVideo ? "vid" : "mp3") + "link", medialink);
	   newEpisode.put((isVideo ? "vid" : "mp3") + "size", mediasize);
	
	   database.insert(DATABASE_TABLE, null, newEpisode);
	}
	
	/** [DATABASE_TABLE] Marks an episode as either new or not new. */
	public void markNew(long id, boolean is_new)
	{
	   database.execSQL("UPDATE " + DATABASE_TABLE  + " SET new='" + (is_new?1:0) + "'" + 
			   (id!=0 ? (" WHERE _id='" + id + "'") : ""));
	}
	
	/** [DATABASE_TABLE] Updates a an episode entry.
	 * @return True = an episode was found and updated, False = no episode found */
	public boolean updateMedia(String show, String title,
							boolean isVideo, String newLink, long newSize)
	{
		Cursor c = database.query(DATABASE_TABLE, new String[] {"_id"},
				"title='" + title + "' AND show='" + show + "'", null, null, null, null);
		if(c.getCount()==0)
			return false;
		c.moveToFirst();
		String newType = (isVideo ? "vid" : "mp3");
		long id = c.getLong(c.getColumnIndex("_id"));
		database.execSQL("UPDATE " + DATABASE_TABLE  + " SET " + newType + "link='" + newLink + "' WHERE _id='" + id + "'");
		database.execSQL("UPDATE " + DATABASE_TABLE  + " SET " + newType + "size='" + newLink + "' WHERE _id='" + id + "'");
		return true;
	}
	
	/** [DATABASE_TABLE] Updates the position of an episode entry.
	 * @param id The ID of the entry to set 
	 * @param position The new position for the entry
	 */
	public void updatePosition(long id, long position)
	{
		database.execSQL("UPDATE " + DATABASE_TABLE  + " SET position='" + position + "' WHERE _id='" + id + "'");
	}
	
	/** [DATABASE_TABLE] Gets episodes of a specific show
	 * @param show The show to retrieve
	 * @param filter True to only show new episodes, False to show all
	 * @return A Cursor containing the episodes
	 */
	public Cursor getShow(String show, boolean filter)
	{
		   return database.query(DATABASE_TABLE /* table */,
				    new String[] {"_id", "title", "date", "new", "mp3link"} /* columns */,
				    "show = '" + show + "'" + (filter ? " and new='1'" : "") /* where or selection */,
		        	null /* selectionArgs i.e. value to replace ? */,
		        	null /* groupBy */,
		        	null /* having */,
				   	"date DESC"); /* orderBy */
	}
	
	/** [DATABASE_TABLE] Clears all episodes out of a show.
	 * @param show The show to clear
	 */
	public void clearShow(String show)
	{
		database.delete(DATABASE_TABLE, "show = '" + show + "'", null);
	}
	
	/** [DATABASE_TABLE] Gets one specific episode from the database.
	 * @param id The ID of the entry to retrieve
	 * @return A Cursor containing the one episode.
	 */
	public Cursor getOneEpisode(long id) 
	{
		Cursor c = database.query(DATABASE_TABLE, new String[] {"_id", "show", "title", "new", "date", "description", "mp3link", "mp3size", "vidlink", "vidsize", "position"},
					"_id=" + id, null, null, null, null);
		return c; 
	}
	
	/** [DATABASE_TABLE] Deletes one episode
	 * @param id The ID of the entry to be deleted
	 */
	public void deleteEpisode(long id) 
	{
	   database.delete(DATABASE_TABLE, "_id=" + id, null);
	}
	
	
	//-------DATABASE_QUEUE
	/** [DATABASE_QUEUE] Gets the entirety of the queue.
	 * @return A Cursor containing the queue.
	 */
	public Cursor getQueue()
	{
		   Cursor things = null;
		   things = database.query(DATABASE_QUEUE,
				    new String[] {"_id", "identity", "current", "streaming"},
				    null,
		        	null,
		        	null,
		        	null,
				   	"_id");
		   return things;
	}
	
	/** [DATABASE_QUEUE] 
	 * @param identity The identity (note, not ID) of what to be appended
	 * @param isStreaming True if it is a streaming entry, false otherwise
	 */
	public void appendToQueue(long identity, boolean isStreaming)
	{
		   if(database.query(DATABASE_QUEUE, new String[] {"_id"}, "identity=" + identity, null, null, null, null).getCount()==0)
		   {
			   ContentValues newEntry = new ContentValues();
			   newEntry.put("identity", identity);
			   newEntry.put("current", 0);
			   newEntry.put("streaming", isStreaming ? 1 : 0);
			   database.insert(DATABASE_QUEUE, null, newEntry);
		   } else
			   Log.w("*:appendToQueue", "Song is already in queue: " + identity);
	}
	
	/** [DATABASE_QUEUE] Updates a queue item.
	 * @param id The ID to update
	 * @param identity The identity (that is, the ID for DATABASE_TABLE)
	 * @param isCurrent Greater than 0 if it is the current track, 0 otherwise.
	 * @param streaming Greater than 0 if it is streaming, 0 otherwise.
	 */
	private void updateQueue(long id, long identity, int isCurrent, int streaming)
	{
	   ContentValues editEpisode = new ContentValues();
	   editEpisode.put("identity", identity);
	   editEpisode.put("current", isCurrent);
	   editEpisode.put("streaming", streaming);
	
	   database.update(DATABASE_QUEUE, editEpisode, "_id=" + id, null);
	}
	
	/** [DATABASE_QUEUE] Moves an item either forward or back in the queue
	 * @param id1 The ID to move either forward or back
	 * @param isDown True means direction is back, otherwise moves forward.
	 */
	public void move(long id1, boolean isDown)
	{
		
	       Cursor c1 = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "current", "streaming"},	"_id=" + id1, null, null, null, null);
	       c1.moveToFirst();
	       long identity1 = c1.getLong(c1.getColumnIndex("identity"));
	       int current1 = c1.getInt(c1.getColumnIndex("current"));
	       int streaming1 = c1.getInt(c1.getColumnIndex("streaming"));
		   
	       Cursor c2;
	       if(isDown)
	    	   c2 = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "current", "streaming"},	"_id<" + id1, null, null, null, "_id DESC", "1");
	       else
	    	   c2 = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "current", "streaming"},	"_id>" + id1, null, null, null, "_id ASC", "1");
	       if(c2.getCount()==0)
	    	   return;
	       c2.moveToFirst();
	       long id2 = c2.getLong(c2.getColumnIndex("_id"));
	       long identity2 = c2.getLong(c2.getColumnIndex("identity"));
	       int current2 = c2.getInt(c2.getColumnIndex("current"));
	       int streaming2 = c2.getInt(c2.getColumnIndex("streaming"));
	       
	       updateQueue(id1, identity2, current2, streaming2);
	       updateQueue(id2, identity1, current1, streaming1);
		   
	}
	
	/** [DATABASE_QUEUE] Gets the number of items in the queue.
	 * @return The number of items in the queue
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
	 * @param identity
	 * @return
	 */
	public boolean isInQueue(long identity) 
	{
	   Cursor c = database.query(DATABASE_QUEUE, new String[] {"_id"},
			   					"identity=" + identity, null, null, null, null);
	   return (c.getCount()>0);
	}
	
	/** [DATABASE_QUEUE] Removes one item from the queue
	 * @param id The ID of the item to remove from the queue
	 */
	public void deleteQueueItem(long id) 
	{
	   database.delete(DATABASE_QUEUE, "_id=" + id, null);
	}
	
	/** [DATABASE_QUEUE] Advances the marker for the current item of the queue and returns a cursor containing that item
	 * @param previous True moves it forward forward, false moves backward
	 * @return A Cursor containing the new current item
	 */
	public Cursor advanceQueue(int previous)
	{
		long id;
		Cursor c = currentQueue();
		if(c==null || c.getCount()==0)
		{
			Log.v("DatabaseConnector:advanceQueue", "No current found in queue.");
			c = getQueue();
			if(c.getCount()==0)
				return null;
			c.moveToFirst();
			id = c.getLong(c.getColumnIndex("_id"));
			database.execSQL("UPDATE " + DATABASE_QUEUE  + " SET current=1 WHERE _id=" + id); //CLEAN: make more efficient
			return getQueue();
		}
		if(previous!=0)
		{
			Log.v("DatabaseConnector:advanceQueue", "An old current was found in the queue");
			c.moveToFirst();
			id = c.getLong(c.getColumnIndex("_id"));
			//Set the old one to be not current
			database.execSQL("UPDATE " + DATABASE_QUEUE  + " SET current=0" + " WHERE _id=" + id + "");
			//Get the new one and set it to be current
			if(previous<0)
				c = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "streaming"}, "_id<" + id, null, null, null, "_id DESC", "1");
			else
				c = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "streaming"}, "_id>" + id, null, null, null, null, "1");
			if(c.moveToFirst())
			{
				Log.v("DatabaseConnector:advanceQueue", "A new current was found in the queue");
				id = c.getLong(c.getColumnIndex("_id"));
				database.execSQL("UPDATE " + DATABASE_QUEUE  + " SET current='1'" + " WHERE _id='" + id + "'");
			}
		}
		return currentQueue();
	}
	
	/** [DATABASE_QUEUE] Gets the current item in the queue.
	 * @return A Cursor containing the urrent item
	 */
	public Cursor currentQueue()
	{
		Cursor c = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "streaming"}, "current>'0'", null, null, null, null, "1");
		if(c.getCount()==0)
			return null;
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
	
	/** Gets one event. Isn't that obvious?
	 * @param id The ID of the event to retrieve
	 * @return A cursor containing the one event
	 */
	public Cursor getOneEvent(long id) 
	{
		Cursor c = database.query(DATABASE_CALENDAR, new String[] {"_id", "show", "type", "date", "time", "recurring"},
					"_id=" + id, null, null, null, null);
		return c; 
	}
	
	/** Retrieves all events for a specific day
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
	
	/** Retrieves the number of number of events
	 * @return The number of events
	 */
	public int eventCount()
	{
		return (database.query(DATABASE_CALENDAR, new String[] {},
					null, null, null, null,
					null)).getCount();
	}
	
	/** Clears all events from the database */
	public void clearCalendar() {
		database.execSQL("DELETE FROM " + DATABASE_CALENDAR + ";");
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
	      String createQuery = "CREATE TABLE " + DATABASE_TABLE + " " + 
	         "(_id integer primary key autoincrement, " +
	         	"show TEXT, " +
	         	"new INT, " + 
	         	"title TEXT, " +
	         	"date TEXT, " +
	         	"description TEXT, " +
	         	"position INTEGER, " +
	         	"mp3link TEXT, " +
	         	"mp3size INTEGER, " + 
	         	"vidlink TEXT, " + 
	         	"vidsize INTEGER);";
	      db.execSQL(createQuery);
	      
	      
	      String createQuery2 = "CREATE TABLE " + DATABASE_QUEUE + " " + 
	 	         "(_id integer primary key autoincrement, " +
	 	         	"identity INTEGER, " + 
	 	         	"current INTEGER, " + 
	 	         	"streaming INTEGER);";
	 	      db.execSQL(createQuery2);
	 	      
	 	  String createQuery3 = "CREATE TABLE " + DATABASE_CALENDAR + " " + 
	 			 "(_id integer primary key autoincrement, " +
	 			   "show TEXT, " +
	 			   "type TEXT, " + 
	 			   "date TEXT, " + 
	 			   "time TEXT, " + 
	 			   "recurring INTEGER);";
	 	  	db.execSQL(createQuery3);
	   }
	
	   //FEATURE: Need to expect onUpgrade
	   @Override
	   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	   {
	   }
	}
}
