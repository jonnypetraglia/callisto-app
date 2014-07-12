package com.qweex.callisto.catalog;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import com.qweex.callisto.DatabaseConnector;

import java.text.SimpleDateFormat;
import java.util.LinkedList;

/** A specialized class to help with the database.
 *
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class DatabaseMate
{
    String TAG = "Callisto:catalog:DatabaseMate";

    /** Reference to the DatabaseConnector class to actually do all the database actions. */
    DatabaseConnector dbc;
    /** The format that dates are stored in the database. */
    public static final SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss");

    /** Info about the DB. */
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

    /** Constructor; sets teh DatabaseConnector reference.
     * @param dbc DatabaseConnector reference.
     */
    public DatabaseMate(DatabaseConnector dbc)
    {
        this.dbc = dbc;
    }

    /** Inserts an episode into the database
     * @param ep The episode to insert.
     */
    public void insertEpisode(Episode ep)
    {
        String title = ep.Title;
        int marker = title.lastIndexOf('|');
        if(marker>-1)
            title = title.substring(0, marker);

        Log.i(TAG, "Inserting episode: " + title + " (" + ep.show + ")");

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

    public void insertEpisodes(final LinkedList<Episode> episodes) {
        while(!episodes.isEmpty())
            insertEpisode(episodes.pop());
    }

    /** Marks an episode as new or not new.
     * @param id The id of the episode.
     * @param is_new Whether or not the episode is new.
     */
    public void markNew(long id, boolean is_new)
    {
        ContentValues values = new ContentValues();
        values.put("new", (is_new?1:0));
        dbc.database.update(TABLE_EPISODES, values, "_id=?", new String[] { id + ""});
    }

    /** Marks all episodes of a show as new.
     * @param show The show.
     * @param is_new Whether or not the episodes are new.
     */
    public void markAllNew(String show, boolean is_new)
    {
        ContentValues values = new ContentValues();
        values.put("new", (is_new?1:0));
        dbc.database.update(TABLE_EPISODES, values, "show=?", new String[]{show});
    }

    /** Updates the position of an episode.
     * @param id The id of the episode.
     * @param position The position. DUH.
     */
    public void updatePosition(long id, long position)
    {
        ContentValues values = new ContentValues();
        values.put("position", position);
        dbc.database.update(TABLE_EPISODES, values, "_id=?", new String[] { id + ""});
    }

    /** Gets all episodes of a show.
     * @param show The show to get. As stored in the JSON file.
     * @param filter Whether or not to only get new episodes
     * @return A cursor containing the episodes.
     */
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

    /** Removes all episodes from a show. For debugging purposes, really.
     * @param show The show to clear.
     */
    public void clearShow(String show)
    {
        dbc.database.delete(TABLE_EPISODES, "show=?", new String[] {show});
    }

    /** Retrieve one episode from the database.
     * @param id The id of the episode.
     * @throws RuntimeException If the episode doesn't exist or the Episode object otherwise cannot be created.
     * @return
     */
    public Episode getOneEpisode(long id)
    {
        Cursor e = dbc.database.query(TABLE_EPISODES,
                new String[] {"_id", "show", "title", "date", "description", "length", "link", "imglink", "mp3link", "mp3size", "vidlink", "vidsize", "new", "position"},
                "_id=?", new String[] {id + ""}, null, null, null);
        return new Episode(e);
    }

    /** Retrieve the last (chronological) episode before the one supplied.
     * @param ep The episode of which to get the previous.
     * @return The Episode object or null, if there are none.
     */
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

    /** Retrieve the next (chronological) episode after the one supplied.
     * @param ep The episode of which to get the next.
     * @return The Episode object or null, if there are none.
     */
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


    /** Search Episodes for a term, in Title and Description.
     * @param searchTerm The term to search for
     * @param searchShow The show to search; supply `null` to search all.
     * @return A cursor containing the episodes that matched the search term.
     */
    public Cursor searchEpisodes(String searchTerm, String searchShow)
    {
        return dbc.database.query(TABLE_EPISODES,
                new String[] {"_id", "title", "show", "date"},
                (searchShow!="" ? ("show=? and ") : "") + "(title like '%?%' or description like '%?%')", //where
                new String[] {searchShow, searchTerm, searchTerm},
                null, null, null);
    }
}
