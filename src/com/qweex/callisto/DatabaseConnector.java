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

//This class is for communicating with the SQlite database
//It has 3 different tables in it: episodes, queue, and calendar

public class DatabaseConnector 
{
	//------Basic Functions
	private static final String DATABASE_NAME = "JBShows.db";
	private static final String DATABASE_TABLE = "episodes";
	private static final String DATABASE_QUEUE = "queue";
	private static final String DATABASE_CALENDAR = "calendar";
	private SQLiteDatabase database;
	private DatabaseOpenHelper databaseOpenHelper = null;
	
	public DatabaseConnector(Context context) 
	{
	    databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
	}
	
	public void open() throws SQLException 
	{
	   database = databaseOpenHelper.getWritableDatabase();
	}
	
	public void close() 
	{
	   if (database != null)
	      database.close();
	}
	
	//------DATABASE_TABLE
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
	
	public void markNew(long id, boolean is_new)
	{
	   database.execSQL("UPDATE " + DATABASE_TABLE  + " SET new='" + (is_new?1:0) + "'" + 
			   (id!=0 ? (" WHERE _id='" + id + "'") : ""));
	}
	
	public void updatePosition(long id, long position)
	{
		database.execSQL("UPDATE " + DATABASE_TABLE  + " SET position='" + position + "' WHERE _id='" + id + "'");
	}
	
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
	
	public Cursor getShowNew(String show)
	{
		   return database.query(DATABASE_TABLE /* table */,
				    new String[] {"_id", "title", "date", "new"} /* columns */,
				    "show = '" + show + "' and new='1'"/* where or selection */,
		        	null /* selectionArgs i.e. value to replace ? */,
		        	null /* groupBy */,
		        	null /* having */,
				   	"date DESC"); /* orderBy */
	}
	
	public void clearShow(String show)
	{
		database.delete(DATABASE_TABLE, "show = '" + show + "'", null);
	}
	
	public Cursor getOneEpisode(long id) 
	{
		Cursor c = database.query(DATABASE_TABLE, new String[] {"_id", "show", "title", "new", "date", "description", "mp3link", "mp3size", "vidlink", "vidsize", "position"},
					"_id=" + id, null, null, null, null);
		return c; 
	}
	
	public void deleteEpisode(long id) 
	{
	   database.delete(DATABASE_TABLE, "_id=" + id, null);
	}
	
	
	//-------DATABASE_QUEUE
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
	
	private void updateQueue(long id, long identity, int isCurrent, int streaming)
	{
	   ContentValues editEpisode = new ContentValues();
	   editEpisode.put("identity", identity);
	   editEpisode.put("current", isCurrent);
	   editEpisode.put("streaming", streaming);
	
	   database.update(DATABASE_QUEUE, editEpisode, "_id=" + id, null);
	}
	
		//isDown = true means direction is back, otherwise moves forward
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
	
	public long queueCount() {
	    String sql = "SELECT COUNT(*) FROM " + DATABASE_QUEUE;
	    SQLiteStatement statement = database.compileStatement(sql);
	    long count = statement.simpleQueryForLong();
	    return count;
	}
	
	public void clearQueue() {
		database.execSQL("DELETE FROM " + DATABASE_QUEUE + ";");
	}
	
	public boolean isInQueue(long identity) 
	{
	   Cursor c = database.query(DATABASE_QUEUE, new String[] {"_id"},
			   					"identity=" + identity, null, null, null, null);
	   return (c.getCount()>0);
	}
	
	public void deleteQueueItem(long id) 
	{
	   database.delete(DATABASE_QUEUE, "_id=" + id, null);
	}
	
	
	//Advances the marker for the current item of the queue and returns a cursor containing that item
		//if "next" is true, it moves forward, false moves backwards
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
	
	public Cursor currentQueue()
	{
		Cursor c = database.query(DATABASE_QUEUE, new String[] {"_id", "identity", "streaming"}, "current>'0'", null, null, null, null, "1");
		if(c.getCount()==0)
			return null;
	   return c;
		
	}
	
	
	//------DATABASE_CALENDAR
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
	
	public Cursor dayEvents(String specificDay, int dayOfWeek) 
	{
		String query = "recurring=" + dayOfWeek + " or date like '" + specificDay + "'";
		String sortby = "time";
		Cursor c = database.query(DATABASE_CALENDAR, new String[] {"_id", "show", "type", "date", "time"},
			   					query, null, null, null,
			   					sortby);
	   return c;
	}
	
	
	public int eventCount()
	{
		return (database.query(DATABASE_CALENDAR, new String[] {},
					null, null, null, null,
					null)).getCount();
	}
	
	public void clearCalendar() {
		database.execSQL("DELETE FROM " + DATABASE_CALENDAR + ";");
	}
	
	//------Helper open class
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
