package com.qweex.callisto.catalog;

import android.database.Cursor;
import java.text.ParseException;
import java.util.Calendar;

/** A data structure to hold all info for an episode.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
class Episode
{
    /** Episode info **/
    public String Title = null, Desc = null, Link = null, Image = null, AudioLink = null, VideoLink = null;
    public long AudioSize = -1, VideoSize = -1;
    public Calendar Date;
    String show;
    public long Position, Length;
    public boolean New;

    /** Metadata **/
    public long episode_id;

    /** Constructor for when you are constructing an episode.
     * @param show The show that the episode will belong to.
     */
    public Episode(String show) {
        this.show = show;
    }

    /** Creates an Episode object from a cursor from the database.
     * @param c Cursor from the database that contains the episode to create.
     * @throws RuntimeException If the cursor is empty or the Date fails to parse.
     */
    public Episode(Cursor c) {
        if(c.getCount()==0)
            throw new RuntimeException();
        c.moveToFirst();
        episode_id = c.getLong(c.getColumnIndex("_id"));

        show = c.getString(c.getColumnIndex("show"));

        Date = Calendar.getInstance();
        try {
            Date.setTime(DatabaseMate.sdfRaw.parse(c.getString(c.getColumnIndex("date"))));
        } catch(ParseException e) {
            //Aw yiss bad practices
            throw new RuntimeException();
        }

        Title = c.getString(c.getColumnIndex("title"));
        Desc = c.getString(c.getColumnIndex("description"));
        Link = c.getString(c.getColumnIndex("link"));
        Image = c.getString(c.getColumnIndex("imglink"));
        AudioLink = c.getString(c.getColumnIndex("mp3link"));
        VideoLink = c.getString(c.getColumnIndex("vidlink"));
        AudioSize = c.getLong(c.getColumnIndex("mp3size"));
        VideoSize = c.getLong(c.getColumnIndex("vidsize"));

        Position = c.getLong(c.getColumnIndex("position"));
        Length = c.getLong(c.getColumnIndex("length"));
        New = c.getInt(c.getColumnIndex("new"))>1;
    }

    /** Asserts that the episode is "complete", i.e. that it contains the minimal amount of info.
     * @throws UnfinishedParseException
     */
    public void assertComplete() throws UnfinishedParseException {
        if(Title==null)
            throw new UnfinishedParseException("Title");
        //if(Link==null)                                        //Link is optional because an episode of CR doesn't have one...
        //    throw new UnfinishedParseException("Link");
        //if(Desc==null)                                        //Desc is optional because an episode of Unfilter doesn't have one.
        //    throw new UnfinishedParseException("Desc");
        if(Date==null)
            throw new UnfinishedParseException("Date");
        if(AudioLink==null)
            throw new UnfinishedParseException("AudioLink");
        if(AudioSize<0)
            throw new UnfinishedParseException("AudioSize");
    }
}
