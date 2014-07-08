package com.qweex.callisto.catalog;

import java.util.Calendar;

class Episode
{
    public String Title = null, Desc = null, Link = null, AudioLink = null, VideoLink = null;
    public long AudioSize = -1, VideoSize = -1;
    public Calendar Date;
    String show_id;

    public Episode(String show_id) {
        this.show_id = show_id;
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
