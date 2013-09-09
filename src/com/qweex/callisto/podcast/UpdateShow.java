/*
 * Copyright (C) 2012-2013 Qweex
 * This file is a part of Callisto.
 *
 * Callisto is free software; it is released under the
 * Open Software License v3.0 without warranty. The OSL is an OSI approved,
 * copyleft license, meaning you are free to redistribute
 * the source code under the terms of the OSL.
 *
 * You should have received a copy of the Open Software License
 * along with Callisto; If not, see <http://rosenlaw.com/OSL3.0-explained.htm>
 * or check OSI's website at <http://opensource.org/licenses/OSL-3.0>.
 */
package com.qweex.callisto.podcast;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import com.qweex.callisto.StaticBlob;
import com.qweex.utils.UnfinishedParseException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;

/** Tools to update a show. */
public class UpdateShow
{
    XmlPullParser xpp
            ,xpp_vid
            ;
    String lastChecked, newLastChecked;
    String currentShow;
    int eventType, eventType2;
    LinkedList<EpisodeFromRSS> EpisodesRetrieved = new LinkedList<EpisodeFromRSS>();

    /** Updates a show by checking to see if there are any new episodes available.
     *
     * @param currentShow The number of the current show in relation to the AllShows.SHOW_LIST array
     * @param showSettings The associated SharedPreferences with that show
     * @return A Message object with arg1 being 1 if the show found new episodes, 0 otherwise.
     */
    public Message doUpdate(String currentShow, SharedPreferences showSettings, String audio_url, String video_url)
    {
        String TAG = StaticBlob.TAG();
        Log.i(TAG, "Beginning update: " + currentShow + " " + audio_url + " " + video_url);
        lastChecked = showSettings.getString("last_checked", null);
        String showErrorToast = null;
        this.currentShow=currentShow;
        try
        {
            //Prepare the parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            xpp = factory.newPullParser();
            URL url = new URL(audio_url);
            InputStream input = url.openConnection().getInputStream();
            xpp.setInput(input, null);

            if(video_url!=null)
            {
                XmlPullParserFactory factory2 = XmlPullParserFactory.newInstance();
                factory2.setNamespaceAware(true);
                xpp_vid = factory2.newPullParser();
                URL url2 = new URL(video_url);
                InputStream input2 = url2.openConnection().getInputStream();
                xpp_vid.setInput(input2, null);
            }

            Log.v(TAG, "Parser is prepared");
            eventType = xpp.getEventType();
            if(xpp_vid!=null)
                eventType2 = xpp_vid.getEventType();

            while(!("title".equals(xpp.getName()) && eventType == XmlPullParser.END_TAG))
            {
                eventType = xpp.next();
            }
            eventType = xpp.next();

            String imgurl = null, imgurl2 = null;
            //Download the image
            while(!("item".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG) && !("thumbnail".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
            {
                //Handles if there is an <image> tag
                if("image".equals(xpp.getName()))
                {
                    if(xpp.getAttributeCount()>0)
                    {
                        imgurl = xpp.getAttributeValue(null, "href");
                        eventType = xpp.next();
                        eventType = xpp.next();
                    }
                    else
                    {
                        eventType = xpp.next();
                        while(!(("image".equals(xpp.getName()) || ("url".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))))
                        {
                            eventType = xpp.next();
                        }
                        if(!("image".equals(xpp.getName())))
                        {
                            eventType = xpp.next();
                            imgurl = xpp.getText();
                            while(!("image".equals(xpp.getName())))
                            {
                                eventType = xpp.next();
                            }
                        }
                    }
                }

                eventType = xpp.next();
                if(eventType==XmlPullParser.END_DOCUMENT)
                    throw(new UnfinishedParseException("Thumbnail"));
            }
            //Handles if no <image> tag was found, falls back to <media:thumbnail>
            if(("thumbnail".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
                imgurl2 = xpp.getAttributeValue(null, xpp.getAttributeName(0));

            if(imgurl2!=null)
                imgurl = imgurl2;
            if(imgurl!=null)
            {
                if(imgurl.startsWith("http://linuxactionshow.com") || imgurl.startsWith("www.linuxactionshow.com") || imgurl.startsWith("http://www.linuxactionshow.com"))
                    imgurl = "http://www.jupiterbroadcasting.com/images/LASBadge-Audio144.jpg";
                new downloadImage().execute(imgurl, currentShow);
                //downloadImage(imgurl, AllShows.SHOW_LIST[currentShow]);
                Log.v(TAG, "Parser is downloading image for " + currentShow + ":" + imgurl);
            }

//----------------------------------------------------------------------------------------------------------------------
            while(eventType!=XmlPullParser.END_DOCUMENT)
            {
                //Find the next <item> tag
                while(!("item".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
                {
                    eventType = xpp.next();
                    if(eventType==XmlPullParser.END_DOCUMENT)
                        break;
                }
                if(eventType==XmlPullParser.END_DOCUMENT)
                    break;
                if(xpp_vid!=null)
                {
                    while(!("item".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG))
                    {
                        eventType2 = xpp_vid.next();
                        if(eventType2==XmlPullParser.END_DOCUMENT)
                            break;
                    }
                    if(eventType2==XmlPullParser.END_DOCUMENT)
                        break;
                }
                extractData();
            }

        } catch (XmlPullParserException e) {
            Log.e(TAG+":XmlPullParserException", "Parser error");
            e.printStackTrace();
            showErrorToast = "XMLPullParser";
        } catch (MalformedURLException e) {
            Log.e(TAG+":MalformedURLException", "Malformed URL? That should never happen.");
            e.printStackTrace();
            showErrorToast = "Bad URL";
        } catch (UnknownHostException e)
        {
            Log.e(TAG+":UnknownHostException", "Unable to initiate a connection");
            showErrorToast = "UnknownHost: " + e.getMessage();
        }  catch (IOException e) {
            //FIXME: EXCEPTION: IOException
            Log.e(TAG+":IOException", "IO is a moon");
            e.printStackTrace();
            showErrorToast = "I/O";
        } catch (ParseException e) {
            //FIXME: EXCEPTION: ParseException
            Log.e(TAG+":ParseException", "Date Parser error");
            showErrorToast = "Date Parse";
        } catch (UnfinishedParseException e) {
            Log.w(TAG+":UnfinishedParseException",e.toString());
            showErrorToast = "UnfinishedParse";
        }

        Message m = new Message();
        if(showErrorToast!=null)
        {
            Bundle b = new Bundle();
            b.putString("ERROR",showErrorToast);
            m.arg1=-1;
            m.setData(b);
        }
        else
        {
            while(!EpisodesRetrieved.isEmpty())
            {
                EpisodesRetrieved.pop().insert();
            }
            if(newLastChecked==null)
            {
                Log.v(TAG, "Not updating lastChecked: " + newLastChecked);
                m.arg1=0;
            }
            else
            {
                Log.v(TAG, "Updating lastChecked for:" + currentShow + "| " + newLastChecked);
                SharedPreferences.Editor editor = showSettings.edit();
                editor.putString("last_checked", newLastChecked);
                editor.commit();
                m.arg1=1;
            }
        }
        Log.i(TAG, "Finished update");
        return m;
    }

    //XmlPullParser.START_TAG==2
    //XmlPullParser.END_TAG==3
    public void extractData() throws UnfinishedParseException, XmlPullParserException, IOException, ParseException
    {
        String TAG = StaticBlob.TAG();
        EpisodeFromRSS ep = new EpisodeFromRSS();
        int numOfDone = 0, numOfDoneVid=0;
        Log.v(TAG, "Starting new episode");
        String titleConfirm = null, dateConfirm = null;

        //<START> Block to read in 3 variables to check video feed to audio feed: title, date, and vidlink
        if(xpp_vid!=null)
        {
            eventType2 = xpp_vid.next();
            while(eventType2!=XmlPullParser.END_DOCUMENT)
            {
                if("title".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG)
                {
                    eventType2 = xpp_vid.next();
                    titleConfirm = xpp_vid.getText();
                    Log.d(TAG, "CONFIRM(1): " + titleConfirm);
                    if(titleConfirm==null)
                        throw(new UnfinishedParseException("Confirm Title"));
                    //!!!!Special case!!!!!!
                    //For the weird delicious links, spin until they are out of the system.
                    //I hate the idea of this.
                    if(titleConfirm.contains("[del.icio.us]"))
                    {
                        Log.d(TAG, "Delicious detected; prepare to spin");
                        numOfDoneVid = 0;
                        while(eventType2!=XmlPullParser.END_DOCUMENT && !"item".equals(xpp_vid.getName())
                                && eventType2 != XmlPullParser.END_TAG)
                        {
                            Log.d(TAG, "Spinning..." +  xpp_vid.getName());
                            //Wait for </item>
                            eventType2 = xpp_vid.next();
                        }
                        /*while(eventType2!=XmlPullParser.END_DOCUMENT && !"item".equals(xpp_vid.getName())
                                && eventType2 != XmlPullParser.START_TAG)
                        {
                            //Wait for next <item>
                            eventType2 = xpp_vid.next();
                        }//*/
                        continue;
                    }
                    if(titleConfirm.indexOf("|")>0)
                        titleConfirm = titleConfirm.substring(0, titleConfirm.indexOf("|")).trim();
                    if((numOfDoneVid & 0x1)==0x1)
                        throw(new UnfinishedParseException("Confirm Title 2times"));
                    numOfDoneVid |= 0x1;
                }
                else if("pubDate".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG)
                {
                    eventType2 = xpp_vid.next();
                    dateConfirm = xpp_vid.getText();
                    Log.d(TAG, "CONFIRM(2): " + dateConfirm);
                    if(dateConfirm==null)
                        throw(new UnfinishedParseException("Confirm Date"));
                    if((numOfDoneVid & 0x8)==0x8)
                        throw(new UnfinishedParseException("ConfirmDate 2times"));
                    numOfDoneVid |= 0x8;
                }
                else if("enclosure".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG)
                {
                    ep.VideoLink = xpp_vid.getAttributeValue(xpp_vid.getNamespace(),"url");
                    if(ep.VideoLink==null)
                        throw(new UnfinishedParseException("VideoLink"));
                    Log.d(TAG, "   V Link: " + ep.VideoLink);
                    String temp = xpp_vid.getAttributeValue(xpp_vid.getNamespace(),"length");
                    if(temp==null)
                        throw(new UnfinishedParseException("VideoSize"));
                    ep.VideoSize = Long.parseLong(temp);
                    Log.d(TAG, "   V Size: " + ep.VideoSize);
                    if((numOfDoneVid & 0x10)==0x10)
                        throw(new UnfinishedParseException("Video 2times"));
                    numOfDoneVid |= 0x10;
                }
                //Done
                else if("item".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.END_TAG)
                {
                    int want = (0x1 | 0x8 | 0x10);
                    if((numOfDoneVid & want)!=want) //Not 0x2 because some items miss <link>
                        throw(new UnfinishedParseException("</item> before finished video"));
                    Log.d(TAG, "FINISHED W/ VIDEO");
                    eventType2 = xpp_vid.next();
                    break;
                }
                eventType2 = xpp_vid.next();
            }
        }
        //</END>

        //<START> Read audio info
        eventType = xpp.next();
        while(eventType!=XmlPullParser.END_DOCUMENT)
        {
            if("title".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                eventType = xpp.next();
                ep.Title = xpp.getText();
                if(ep.Title==null)
                    throw(new UnfinishedParseException("Title"));
                //!!!!SPECIAL CASE!!!!!!
                //Again, spin until they are out of the system
                if(ep.Title.contains("[del.icio.us]"))
                {
                    Log.d(TAG, "Delicious detected; prepare to spin " + xpp.getName());
                    numOfDone = 0;
                    while(eventType!=XmlPullParser.END_DOCUMENT
                            && !("item".equals(xpp.getName())
                            && (eventType != XmlPullParser.END_TAG)))
                    {
                        Log.d(TAG, "Spinning..." +  xpp.getName());
                        //Wait for </item>
                        eventType = xpp.next();
                    }
                    /*
                    while(&& eventType!=XmlPullParser.END_DOCUMENT && !"item".equals(xpp.getName())
                            && eventType != XmlPullParser.START_TAG)
                    {
                        //Wait for next <item>
                        eventType = xpp.next();
                    }//*/
                    continue;
                }
                if(ep.Title.indexOf("|")>0)
                    ep.Title = ep.Title.substring(0, ep.Title.indexOf("|")).trim();
                Log.d(TAG, "Title: " + ep.Title);
                if((numOfDone & 0x1)==0x1)
                    throw(new UnfinishedParseException("Title 2times"));
                numOfDone |= 0x1;
            }
            else if("link".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                eventType = xpp.next();;
                ep.Link = xpp.getText();
                if(ep.Link==null)
                    throw(new UnfinishedParseException("Link"));
                Log.d(TAG, "  Link: " + ep.Link);
                if((numOfDone & 0x2)==0x2)
                    throw(new UnfinishedParseException("Link 2times"));
                numOfDone |= 0x2;
            }
            else if("description".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                eventType = xpp.next();
                ep.Desc = xpp.getText();
                //if(epDesc==null)
                    //throw(new UnfinishedParseException("Description"));
                Log.d(TAG, "  Desc: " + ep.Desc);
                if((numOfDone & 0x4)==0x4)
                    throw(new UnfinishedParseException("Description 2times"));
                if(ep.Desc!=null)
                    numOfDone |= 0x4;
            }
            else if("pubDate".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                eventType = xpp.next();
                String tempDate = xpp.getText();
                Log.d(TAG, "Date: " + tempDate);
                if(tempDate==null)
                    throw(new UnfinishedParseException("Date"));

                try {
                    ep.Date = StaticBlob.sdfSource.parse(tempDate);
                } catch(ParseException pe)
                {
                    Log.d(TAG, "Date parse failed. Trying backup.");
                    try {
                        ep.Date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").parse(tempDate);
                    } catch(ParseException pe2) {
                        Log.d(TAG, "Date parse failed AGAIN. Trying backup #2. Stupid PDT.");
                        ep.Date = StaticBlob.sdfSource.parse(tempDate.replace(" PDT", " PST8PDT"));
                        //If this fails to parse THEN it will throw an exception
                    }
                }
                if(lastChecked!=null && !ep.Date.after(StaticBlob.sdfSource.parse(lastChecked)))
                {
                    eventType=XmlPullParser.END_DOCUMENT;
                    return;
                }
                if(newLastChecked==null)
                    newLastChecked = StaticBlob.sdfSource.format(ep.Date);
                if((numOfDone & 0x8)==0x8)
                    throw(new UnfinishedParseException("Date 2times"));
                numOfDone |= 0x8;
            }
            else if("enclosure".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                //TODO: IS THIS NAUGHTY
                if((numOfDone & 0x10)!=0x10)
                {
                    ep.AudioLink = xpp.getAttributeValue(xpp.getNamespace(),"url");
                    Log.d(TAG, "  A Link: " + ep.AudioLink);
                    if(ep.AudioLink==null)
                        throw(new UnfinishedParseException("AudioLink"));
                    //Sizes
                    String temp = xpp.getAttributeValue(xpp.getNamespace(),"length");
                    if(temp==null)
                        throw(new UnfinishedParseException("MediaSize"));
                    ep.AudioSize = Long.parseLong(temp);
                    Log.d(TAG, "A Size: " + ep.AudioSize);
                    if((numOfDone & 0x10)==0x10)
                        throw(new UnfinishedParseException("Media 2times"));
                    numOfDone |= 0x10;
                }
            }

            //Done
            else if("item".equals(xpp.getName()) && eventType == XmlPullParser.END_TAG)
            {
                int want = (0x1 | 0x4 | 0x8 | 0x10);
                if((numOfDone & want)==want) //Not 0x2 because some items miss <link>
                {
                    Log.d(TAG, "CONFIRMING(1a) " + ep.Title);
                    Log.d(TAG, "CONFIRMING(1a) " + titleConfirm);
                    //Log.d(TAG, "CONFIRMING(2a) " + ep.Date.substring(0,16));
                    //Log.d(TAG, "CONFIRMING(2b) " + dateConfirm);
                    //if(!epTitle.equals(titleConfirm))
                    //    throw(new UnfinishedParseException("Video does not match audio: " + epTitle + "==" +titleConfirm));
                    //if(!epDate.substring(0,16).equals(dateConfirm.substring(0,16)))
                    //    throw(new UnfinishedParseException("Video does not match audio: " + epDate.substring(0,16) + "==" + dateConfirm.substring(0,16)));
                    //if(!Callisto.databaseConnector.updateMedia(AllShows.SHOW_LIST[currentShow], epTitle,
                    //isVideo, epMediaLink, epMediaSize))
                    Log.v(TAG, "Inserting episode: " + ep.Title);
                    EpisodesRetrieved.push(ep);
                    xpp.next();
                    return;
                }
                else
                    throw(new UnfinishedParseException("Malformed Item, missing info: " + numOfDone));
            }
            eventType = xpp.next();
        } //eventType == END_DOCUMENT
        throw(new UnfinishedParseException("Reached end of file before </item>"));
    }


    private class EpisodeFromRSS
    {
        public String Title = null, Desc = null, Link = null, AudioLink = null, VideoLink = null;
        public long AudioSize, VideoSize;
        public Date Date;

        public void insert()
        {
            StaticBlob.databaseConnector.insertEpisode(currentShow, Title, StaticBlob.sdfRaw.format(Date), Desc, Link, AudioLink, AudioSize, VideoLink, VideoSize);
        }
    }



    /** Downloads and resizes a show's logo image.
     * @throws IOException
     * @throws NullPointerException
     */
    public static class downloadImage extends AsyncTask<String, Void, Void>
    {
        /** Do the thing stuff
         * @param s The values, split up into img_url and show. img_url is the image to download, show is the name of the show (to calculate the path)
         */
        @Override
        protected Void doInBackground(String... s)
        {
            String TAG = StaticBlob.TAG();
            //public static void downloadImage(String img_url, String show) throws IOException, NullPointerException
            //{
            try {
                String img_url = s[0], show = s[1];
                System.out.println(img_url);
                if(img_url==null)
                    throw(new NullPointerException());
                File f = new File(
                        StaticBlob.storage_path + File.separator +
                        show + EpisodeDesc.getExtension(img_url));
                Log.i(TAG, "Download Path is: " + f.toString() + ", " + f.exists());
                if(f.exists())
                    return null;
                (new File(StaticBlob.storage_path)).mkdirs();
                URL url = new URL (img_url);
                InputStream input = url.openStream();
                try {
                    OutputStream output = new FileOutputStream(f.getPath());
                    try {
                        byte[] buffer = new byte[5 * 1024];
                        int bytesRead = 0;
                        while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                            output.write(buffer, 0, bytesRead);
                        }
                    } finally {
                        output.close();
                    }
                } finally {
                    input.close();
                }
                //Resize the image
                Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
                Bitmap scale  = Bitmap.createScaledBitmap(bitmap, (int)(60* StaticBlob.DP), (int)(60* StaticBlob.DP), true);
                OutputStream fOut = new FileOutputStream(f);
                scale.compress(Bitmap.CompressFormat.JPEG, 85, fOut);//*/
            } catch(Exception e) {
                Log.v(TAG, "Failed to download image");
            }
            return null;
        }
    }
}
