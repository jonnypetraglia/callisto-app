package com.qweex.callisto.catalog;

import android.os.AsyncTask;
import android.util.Log;
import com.qweex.callisto.PRIVATE;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class RssUpdater extends AsyncTask<ShowInfo, Void, Void>
{
    static SimpleDateFormat sdfSource = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    DatabaseMate db;

    LinkedList<Episode> EpisodesRetrieved = new LinkedList<Episode>();
    XmlPullParser audioParser, videoParser = null;
    ArrayList<ShowInfo> items;
    Callback callback;

    public RssUpdater(DatabaseMate db, Callback c) {
        this.db = db;
        this.callback = c;
    }

    public boolean isRunning() { return items!=null; }

    @Override
    protected void onPostExecute(Void v) {
        items = null;
        callback.call(EpisodesRetrieved);
    }

    public void addItem(ShowInfo show) {
        items.add(show);
    }
    public void addItems(ArrayList<ShowInfo> shows) {
        for(ShowInfo s : shows)
            items.add(s);
    }

    @Override
    protected Void doInBackground(ShowInfo... shows) {
        String TAG = "Callisto:RssUpdater";

        items = new ArrayList<ShowInfo>(Arrays.asList(shows));

        boolean doVideo = false;

        for(int i=0; i<items.size(); i++) {
            ShowInfo current = items.get(i);
            Log.i(TAG, "Beginning update: " + current.id + " " + current.audioFeed + " " + current.videoFeed);

            if(PRIVATE.DEBUG)
                db.clearShow(current.id);

            doVideo = current.videoFeed!=null;

            //current.lastChecked = current.settings.getString("last_checked", null);
            String showErrorToast = null;

            try {
                // Create the XPPs
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
                    try {
                        spinUntil("item");
                    } catch(UnfinishedParseException upe) { //It's cool, we weren't in the middle of an item. This SHOULD be thrown.
                        break;
                    }

                    Episode episode = new Episode(current.id);

                    // Extract the data for 1 episode (for audio)
                    while(! ("item".equals(audioParser.getName()) && audioParser.getEventType()==XmlPullParser.END_TAG) ) {

                        advanceOne(audioParser);
                        if(audioParser.getEventType() != XmlPullParser.START_TAG)
                            continue;

                        // title
                        if(audioParser.getName().equals("title"))
                            episode.Title = textOfNext(audioParser);
                        else
                        if(audioParser.getName().equals("link"))
                            episode.Link = textOfNext(audioParser);
                        else
                        if(audioParser.getName().equals("description"))
                            episode.Desc = textOfNext(audioParser);
                        else
                        if(audioParser.getName().equals("pubDate")) {
                            episode.Date = Calendar.getInstance();
                            episode.Date.setTime(sdfSource.parse(textOfNext(audioParser)));
                        } else
                        if(audioParser.getName().equals("enclosure")) {
                            episode.AudioLink = audioParser.getAttributeValue(audioParser.getNamespace(), "url");
                            String length = audioParser.getAttributeValue(audioParser.getNamespace(),"length");
                            episode.AudioSize = Long.parseLong(length);
                        }
                    }

                    // Extract the data for the same episode (for video) & confirm it matches audio
                    while(doVideo && ! ("item".equals(videoParser.getName()) && videoParser.getEventType()==XmlPullParser.END_TAG) ) {
                        advanceOne(videoParser);

                        if(videoParser.getEventType() != XmlPullParser.START_TAG)
                            continue;

                        if(videoParser.getName().equals("title"))
                            assertSame(episode.Title, textOfNext(videoParser));
                        else
                        if(videoParser.getName().equals("link"))
                            assertSame(episode.Link, textOfNext(videoParser));
                        else
                        if(videoParser.getName().equals("description"))
                            assertSame(episode.Desc, textOfNext(videoParser));
                        else
                        if(videoParser.getName().equals("pubDate")) {
                            String testDate = textOfNext(videoParser);
                            Calendar c1 = Calendar.getInstance(), c2 = Calendar.getInstance();
                            c1.setTime(sdfSource.parse(testDate));
                            c2.setTime(sdfSource.parse(testDate));
                            c1.add(Calendar.DATE, -1);
                            c2.add(Calendar.DATE, 1);
                            if(!(episode.Date.after(c1) && episode.Date.before(c2))) {
                                Log.w(TAG, sdfSource.format(c1.getTime()));
                                Log.w(TAG, "VS");
                                Log.w(TAG, sdfSource.format(c2.getTime()));
                                throw new UnfinishedParseException(sdfSource.format(episode.Date.getTime()) + "!=" + testDate);
                            }
                        } else
                        if(videoParser.getName().equals("enclosure")) {
                            episode.VideoLink = videoParser.getAttributeValue(videoParser.getNamespace(),"url");
                            String length = videoParser.getAttributeValue(videoParser.getNamespace(),"length");
                            episode.VideoSize = Long.parseLong(length);
                        }
                    }

                    // At this point, we should have a full Episode object
                    episode.assertComplete();
                    EpisodesRetrieved.add(episode);
                }


            } catch(Exception e) { //!!!!! CHANGE THIS YOU TWAT
                Log.e(TAG, e.getMessage() + "!!!");
                e.printStackTrace();
            }
        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String textOfNext(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.next();
        return parser.getText();
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
    void spinUntil(String tagName) throws XmlPullParserException, IOException, UnfinishedParseException {
        //Audio
        while(! (tagName.equals(audioParser.getName()) && audioParser.getEventType()==XmlPullParser.START_TAG) )
            advanceOne(audioParser);

        if(videoParser==null)
            return;

        // Video
        while(! (tagName.equals(videoParser.getName()) && videoParser.getEventType()==XmlPullParser.START_TAG) )
            advanceOne(videoParser);
    }

    public static abstract class Callback {
        abstract void call(LinkedList<Episode> x);
    }
}

