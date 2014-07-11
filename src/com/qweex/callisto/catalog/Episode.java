package com.qweex.callisto.catalog;

import android.database.Cursor;

import java.util.Calendar;

class Episode
{
    public String Title = null, Desc = null, Link = null, Image = null, AudioLink = null, VideoLink = null;
    public long AudioSize = -1, VideoSize = -1;
    public Calendar Date;
    String show_id;
    public long episode_id;

    public long Position, Length;
    public boolean New;

    public Episode(String show_id) {
        this.show_id = show_id;
    }

    public Episode(Cursor c) {
        if(c.getCount()==0)
            throw new RuntimeException();
        c.moveToFirst();
        episode_id = c.getLong(c.getColumnIndex("_id"));

        show_id = c.getString(c.getColumnIndex("show_id"));

        Date = Calendar.getInstance();
        Date.setTime(new java.util.Date(Long.parseLong(c.getString(c.getColumnIndex("date")))));

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

    @Override
    public String toString() {
        return Title + "|" + Desc + "|" + Date + "|" + Link + "|" + AudioLink + "|" + VideoLink + "|" + AudioSize + "|" + VideoSize;
    }
}
