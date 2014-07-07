package com.qweex.callisto.catalog;

class Episode
{
    public String Date = null, Title = null, Desc = null, Link = null, AudioLink = null, VideoLink = null;
    public long AudioSize = -1, VideoSize = -1;

    public void insert(String show_id, DatabaseMate db) throws UnfinishedParseException
    {
        assertComplete();
        db.insertEpisode(show_id, Title, Date, Desc, Link, AudioLink, AudioSize, VideoLink, VideoSize);
    }

    public void assertComplete() throws UnfinishedParseException {
        if(Title==null)
            throw new UnfinishedParseException("Title");
        if(Link==null)
            throw new UnfinishedParseException("Link");
        if(Desc==null)
            throw new UnfinishedParseException("Desc");
        if(Date==null)
            throw new UnfinishedParseException("Date");
        if(AudioLink==null)
            throw new UnfinishedParseException("AudioLink");
        if(AudioSize<0)
            throw new UnfinishedParseException("AudioSize");
    }
}
