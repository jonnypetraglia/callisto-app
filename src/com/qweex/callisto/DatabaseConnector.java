package com.qweex.callisto;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteStatement;

public class DatabaseConnector 
{
	// database name
	private static final String DATABASE_NAME = "JBShows.db";
	private static final String DATABASE_TABLE = "JB";
	private static final String DATABASE_QUEUE = "queue";
	private SQLiteDatabase database; // database object
	private DatabaseOpenHelper databaseOpenHelper; // database helper
	
	// public constructor for DatabaseConnector
	public DatabaseConnector(Context context) 
	{
	    databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
	}
	
	// create or open a database for reading/writing
	public void open() throws SQLException 
	{
	   database = databaseOpenHelper.getWritableDatabase();
	}
	
	public void close() 
	{
	   if (database != null)
	      database.close();
	}
	
	public void insertEpisode(String show,
							  String title,
							  String date,
							  String description,
							  String mp3link,
							  String mp3size) 
	{
	   ContentValues newEpisode = new ContentValues();
	   newEpisode.put("show", show);
	   newEpisode.put("new", 1);
	   newEpisode.put("title", title);
	   newEpisode.put("date", date);
	   newEpisode.put("description", description);
	   newEpisode.put("mp3link", mp3link);
	   newEpisode.put("mp3size", mp3size);
	
	   open();
	   database.insert(DATABASE_TABLE, null, newEpisode);
	   close();
	}
	
	// inserts a new contact in the database
	public void markNew(long id, boolean is_new)
	{
	   open();
	   database.execSQL("UPDATE " + DATABASE_TABLE  + " SET new='" + (is_new?1:0) + "' WHERE _id='" + id + "'");
	   close();
	}
	
	public void updateQueue(long id, long identity)
	{
	   ContentValues editEpisode = new ContentValues();
	   editEpisode.put("identity", identity);
	
	   open();
	   database.update(DATABASE_QUEUE, editEpisode, "_id=" + id, null);
	   close();
	}
	
	public void swap(long id1, long id2)
	{
			open();
	       Cursor c1 = database.query(DATABASE_QUEUE, new String[] {"_id", "identity"},	"_id=" + id1, null, null, null, null);
	       c1.moveToFirst();
	       long identity1 = c1.getLong(c1.getColumnIndex("identity"));
		   
	       Cursor c2 = database.query(DATABASE_QUEUE, new String[] {"_id", "identity"},	"_id=" + id2, null, null, null, null);
	       c2.moveToFirst();
	       long identity2 = c2.getLong(c2.getColumnIndex("identity"));
	       close();
	       
	       updateQueue(id1, identity2);
	       updateQueue(id2, identity1);
		   
	}
	
	public long queueCount() {
		open();
	    String sql = "SELECT COUNT(*) FROM " + DATABASE_QUEUE;
	    SQLiteStatement statement = database.compileStatement(sql);
	    long count = statement.simpleQueryForLong();
	    close();
	    return count;
	}
	
	public void clearQueue() {
		database.execSQL("DELETE FROM " + DATABASE_QUEUE + ";");
	}
	
	
	public void appendToQueue(long identity)
	{
		   open();
		   if(database.query(DATABASE_QUEUE, new String[] {"_id"}, "identity=" + identity, null, null, null, null).getCount()==0)
		   {
			   ContentValues newEntry = new ContentValues();
			   newEntry.put("identity", identity);
			   database.insert(DATABASE_QUEUE, null, newEntry);
		   } else
		   {
			   System.out.println("Already in queue"); //TODO
		   }
		   close();
	}
	
	public Cursor getQueue()
	{
		   Cursor things = null;
		   things = database.query(DATABASE_QUEUE,
				    new String[] {"_id", "identity"},
				    null,
		        	null,
		        	null,
		        	null,
				   	"_id");
		   return things;
	}
	
	public boolean isInQueue(long identity) 
	{
		open();
	   Cursor c = database.query(DATABASE_QUEUE, new String[] {"_id"},
			   					"identity=" + identity, null, null, null, null);
	   close();
	   return (c.getCount()>0);
	}
	
	public void deleteQueueItem(long id) 
	{
	   open();
	   database.delete(DATABASE_QUEUE, "_id=" + id, null);
	   close();
	}
	
	public Cursor getShow(String show)
	{
		   Cursor things;
		   things = database.query(DATABASE_TABLE /* table */,
				    new String[] {"_id", "title", "date", "new"} /* columns */,
				    "show = '" + show + "'" /* where or selection */,
		        	null /* selectionArgs i.e. value to replace ? */,
		        	null /* groupBy */,
		        	null /* having */,
				   	"date DESC"); /* orderBy */
		   return things;
	}
	
	// get a Cursor containing all information about the contact specified
	// by the given id
	public Cursor getOneEpisode(long id) 
	{
	   return database.query(DATABASE_TABLE, new String[] {"_id", "show", "title", "new", "date", "description", "mp3link", "position", "mp3size"},
			   					"_id=" + id, null, null, null, null);
	}
	
	public void deleteContact(long id) 
	{
	   open();
	   database.delete(DATABASE_TABLE, "_id=" + id, null);
	   close();
	}
	
	private class DatabaseOpenHelper extends SQLiteOpenHelper 
	{
	   public DatabaseOpenHelper(Context context, String name, CursorFactory factory, int version) 
	   {
	      super(context, name, factory, version);
	   }
	
	   // creates the contacts table when the database is created
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
	         	"mp3link TEXT, " +
	         	"position INTEGER, " + 
	         	"mp3size INTEGER);";
	      db.execSQL(createQuery);
	      
	      
	      String createQuery2 = "CREATE TABLE " + DATABASE_QUEUE + " " + 
	 	         "(_id integer primary key autoincrement, " +
	 	         	"identity INTEGER);";
	 	      db.execSQL(createQuery2);
	   }
	
	   @Override
	   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	   {
	   }
	}
}
