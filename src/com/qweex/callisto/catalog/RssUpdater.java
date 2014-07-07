package com.qweex.callisto.catalog;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.net.URL;
import java.util.LinkedList;

/** Tools to update a show. */
public class RssUpdater extends AsyncTask<RssUpdater.ShowInfo, Void, Void>
{
    DatabaseHelper db;

    public RssUpdater(DatabaseHelper db) {
        this.db = db;
    }

    public static class ShowInfo {
        public int db_id;
        public String audioFeed, videoFeed;
        public SharedPreferences settings;
        String lastChecked, newLastChecked;
    }

    LinkedList<Episode> EpisodesRetrieved = new LinkedList<Episode>();

    @Override
    protected void onPreExecute() {

    }

    XmlPullParser audioParser, videoParser = null;

    @Override
    protected Void doInBackground(ShowInfo... shows) {
        String TAG = "Callisto:RssUpdater";

        boolean doVideo = false;

        for(int i=0; i<shows.length; i++) {
            ShowInfo current = shows[i];
            Log.i(TAG, "Beginning update: " + current.db_id + " " + current.audioFeed + " " + current.videoFeed);

            doVideo = current.videoFeed!=null;

            current.lastChecked = current.settings.getString("last_checked", null);
            String showErrorToast = null;

            try {
                // Create the XPP's
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                audioParser = factory.newPullParser();
                URL url = new URL(current.audioFeed);
                InputStream input = url.openConnection().getInputStream();
                audioParser.setInput(input, null);

                if(doVideo)
                {
                    XmlPullParserFactory factory2 = XmlPullParserFactory.newInstance();
                    factory2.setNamespaceAware(true);
                    videoParser = factory2.newPullParser();
                    URL url2 = new URL(current.videoFeed);
                    InputStream input2 = url2.openConnection().getInputStream();
                    videoParser.setInput(input2, null);
                }

                while(audioParser.getEventType() != XmlPullParser.END_DOCUMENT) {

                    // Seek to first <item>
                    spinUntil("item", doVideo);

                    Episode episode = new Episode();

                    // Extract the data for 1 episode (for audio)
                    while(! ("item".equals(audioParser.getName()) && audioParser.getEventType()==XmlPullParser.END_TAG) ) {

                        if(audioParser.getEventType() != XmlPullParser.START_TAG)
                            continue;

                        // title
                        if(audioParser.getName().equals("title"))
                            episode.Title = audioParser.getText(); else
                        if(audioParser.getName().equals("link"))
                            episode.Link = audioParser.getText(); else
                        if(audioParser.getName().equals("description"))
                            episode.Desc = audioParser.getText(); else
                        if(audioParser.getName().equals("pubDate"))
                            episode.Date = audioParser.getText(); else
                        if(audioParser.getName().equals("enclosure")) {
                            episode.AudioLink = audioParser.getAttributeValue(audioParser.getNamespace(), "url");
                            String length = audioParser.getAttributeValue(audioParser.getNamespace(),"length");
                            episode.AudioSize = Long.parseLong(length);
                        }
                    }

                    // Extract the data for the same episode (for video) & confirm it matches audio
                    while(doVideo && ! ("item".equals(videoParser.getName()) && videoParser.getEventType()==XmlPullParser.END_TAG) ) {

                        if(audioParser.getEventType() != XmlPullParser.START_TAG)
                            continue;

                        if(audioParser.getName().equals("title"))
                            assertSame(episode.Title, videoParser.getText()); else
                        if(audioParser.getName().equals("link"))
                            assertSame(episode.Link, videoParser.getText()); else
                        if(audioParser.getName().equals("description"))
                            assertSame(episode.Desc, videoParser.getText()); else
                        if(audioParser.getName().equals("pubDate"))
                            assertSame(episode.Date, videoParser.getText()); else
                        if(audioParser.getName().equals("enclosure")) {
                            episode.VideoLink = audioParser.getAttributeValue(audioParser.getNamespace(),"url");
                            String length = audioParser.getAttributeValue(audioParser.getNamespace(),"length");
                            episode.VideoSize = Long.parseLong(length);
                        }
                    }

                    // At this point, we should have a full Episode object
                    episode.insert(current.db_id, db);
                }


            }catch(Exception e) {} //!!!!! CHANGE THIS YOU TWAT
        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    void assertSame(String s1, String s2) throws UnfinishedParseException {
        if(!s1.equals(s2))
            throw new UnfinishedParseException(s1 + "!=" + s2);
    }

    // Advances 1 .... uh, whatever XmlPullParser's unit of analysis is
    void advanceOne(XmlPullParser parser) throws UnfinishedParseException, IOException, XmlPullParserException {
        if(parser.next()==XmlPullParser.END_DOCUMENT)
            throw new UnfinishedParseException("???");
    }

    // "Spins" by tossing through XML elements until the specified tag is encountered
    void spinUntil(String tagName, boolean vid) throws XmlPullParserException, IOException, UnfinishedParseException {
        //Audio
        while(! (tagName.equals(audioParser.getName()) && audioParser.getEventType()==XmlPullParser.START_TAG) )
            advanceOne(audioParser);

        if(videoParser==null)
            return;

        // Video
        while(! (tagName.equals(videoParser.getName()) && videoParser.getEventType()==XmlPullParser.START_TAG) )
            advanceOne(videoParser);
    }
}

