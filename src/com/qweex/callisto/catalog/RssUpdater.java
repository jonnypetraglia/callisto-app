package com.qweex.callisto.catalog;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** A class to fetch and parse RSS feeds SPECIFICALLY for Callisto.
 *
 * It will actually work in general, but specifically, it can parse two nearly-identical feeds simultaneously.
 * For Callisto, this is for parsing audio and video.
 *
 * @author      Jon Petraglia <notbryant@gmail.com>
 */

public class RssUpdater extends AsyncTask<Void, Object, Void>
{
    /** Date format used by RSS standard. */
    static SimpleDateFormat sdfSource = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    /** A list of episodes received which will be returned upon completion. */
    LinkedList<Episode> EpisodesRetrieved = new LinkedList<Episode>();
    /** A list of feeds that failed. */
    LinkedList<ShowInfo> FailedFeeds = new LinkedList<ShowInfo>();
    /** The parsers that do the actual work. */
    XmlPullParser audioParser, videoParser = null;
    /** A list of shows (containing feeds) to check. */
    ArrayList<ShowInfo> items = new ArrayList<ShowInfo>();
    /** The callback to call after finishing with the data. */
    Callback callback;
    /** Wehther or not to check the video feeds. Also set automatically for each feed if no feed exists. */
    boolean doVideo = false;    //This means that it will not do video, no matter what.

    /** Constructor.
     * @param show A single show to update.
     * @param c The callback.
     */
    public RssUpdater(ShowInfo show, Callback c) {
        this.callback = c;
        addItem(show);
    }

    /** Constructor.
     * @param shows A single show to update.
     * @param c The callback.
     */
    public RssUpdater(ArrayList<ShowInfo> shows, Callback c) {
        this.callback = c;
        addItems(shows);
    }

    /** Checks if task is running.
     * @return If task is running.
     */
    public boolean isRunning() { return items!=null; }

    /** Adds a show to be checked.
     * @param show The show. To be checked.
     */
    public void addItem(ShowInfo show) {
        items.add(show);
    }

    /** Adds shows to be checked.
     * @param shows The shows. To be checked.
     */
    public void addItems(ArrayList<ShowInfo> shows) {
        for(ShowInfo s : shows)
            items.add(s);
    }

    /** Checks if the task is updating a certain show
     * @param show The show to check for
     * @return True if the show is in the list to be updated, false otherwise.
     */
    public boolean isUpdating(ShowInfo show) {
        return items!=null && items.indexOf(show)>-1;
    }

    /** Inherited method; the task actually run asyncronously.
     */
    @Override
    protected Void doInBackground(Void... notused) {
        String TAG = "Callisto:RssUpdater";

        Episode episode = null;

        ShowInfo current;
        for(int i=0; i<items.size(); i++) {
            current = items.get(i);
            Log.i(TAG, "Beginning update: " + current.title + " " + current.audioFeed + " " + current.videoFeed);


            LinkedList<Episode> tempEpisodesRetrieved = new LinkedList<Episode>();
            String errorReason = null;

            doVideo = doVideo && current.videoFeed!=null;

            //current.lastChecked = current.settings.getString("last_checked", null);

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

                    try {
                        // Seek to first <item>
                        try {
                            spinUntil("item");
                        } catch(UnfinishedParseException upe) { //It's cool, we weren't in the middle of an item. This SHOULD be thrown.
                            break;
                        }

                        episode = new Episode(current.title);

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
                            } else
                            if(episode.Image ==null && (audioParser.getName().equals("thumbnail")
                                )) //|| audioParser.getName().equals("content")))
                                episode.Image = audioParser.getAttributeValue(null, "url");
                            //else
                            //if(audioParser.equals("image"))
                            //    episode.Image = textOfNext(audioParser);
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
                        tempEpisodesRetrieved.add(episode);

                        // Catch the episode INSIDE the update for its show.
                    } catch(UnfinishedParseException e) {
                        if(!episode.Title.endsWith("[del.icio.us]"))
                            throw e;
                    }

                }

            // Catch OUTSIDE the loop for the show; anything that lands here means that the episodes retrieved thus far are discarded
            } catch(UnfinishedParseException e) {
                errorReason = e.getMessage();
            } catch (ParseException e) {
                errorReason = e.getMessage();
            } catch (XmlPullParserException e) {
                errorReason = e.getMessage();
            } catch (MalformedURLException e) {
                errorReason = e.getMessage();
            } catch (IOException e) {
                errorReason = e.getMessage();
            }

            // Call the callback for this show (on the main thread)
            if(errorReason!=null) {
                tempEpisodesRetrieved = null;
                FailedFeeds.add(current);
            }
            publishProgress(current, tempEpisodesRetrieved, errorReason);

        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /** Inherited method; called after async part is finished. */
    @Override
    protected void onPostExecute(Void v) {
        items = null;
    }

    @Override
    protected void onProgressUpdate(Object... data) {
        items.remove(data[0]);
        callback.call((ShowInfo)data[0], (LinkedList<Episode>) data[1], (String)data[2]);
    }

    public void executePlz(ShowInfo... params) throws ExecutionException, InterruptedException {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            execute(params);
    }


    ///////////////////// Private methods /////////////////////

    /** Shortcut function for readability; advanced parser one step + returns text */
    String textOfNext(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.next();
        return parser.getText();
    }

    /** Assert that two strings are the same
     * @param s1 String A.
     * @param s2 String B
     * @throws UnfinishedParseException If the strings are not equal.
     */
    void assertSame(String s1, String s2) throws UnfinishedParseException {
        if(!s1.equals(s2))
            throw new UnfinishedParseException(s1 + "!=" + s2);
    }

    /** Advances a parser one step & checks to make sure it's not the end of the document.
     * @param parser The parser to advance.
     * @throws UnfinishedParseException
     * @throws IOException
     * @throws XmlPullParserException
     */
    // Advances 1 .... uh, whatever XmlPullParser's unit of analysis is
    void advanceOne(XmlPullParser parser) throws UnfinishedParseException, IOException, XmlPullParserException {
        if(parser.next()==XmlPullParser.END_DOCUMENT)
            throw new UnfinishedParseException("???");
    }

    /** "Spins" by tossing through XML elements until the specified tag is encountered or the document ends.
     * @param tagName The tagname to stop at.
     * @throws XmlPullParserException
     * @throws IOException
     * @throws UnfinishedParseException
     */
    void spinUntil(String tagName) throws XmlPullParserException, IOException, UnfinishedParseException {
        //Audio
        while(! (tagName.equals(audioParser.getName()) && audioParser.getEventType()==XmlPullParser.START_TAG) )
            advanceOne(audioParser);

        // Video
        if(doVideo)
            while(! (tagName.equals(videoParser.getName()) && videoParser.getEventType()==XmlPullParser.START_TAG) )
                advanceOne(videoParser);
    }

    ///////////////////// Classes /////////////////////

    /** Simple interface for callback. */
    public static abstract class Callback {
        /**
         * Called when all the feeds are finished updating.
         * @param show The show that the episodes belong to.
         * @param episodes The episodes that were successfully fetched. null if there was an error
         * @param error The cause of the error, if there is one. Otherwise, is null.
         */
        abstract void call(ShowInfo show, LinkedList<Episode> episodes, String error);
    }
}

