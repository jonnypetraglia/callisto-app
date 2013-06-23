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
import android.os.Environment;
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
import java.util.LinkedList;

/** Tools to update a show. */
public class UpdateShow
{
    XmlPullParser xpp, xpp_vid;
    String lastChecked, newLastChecked;
    int currentShow,
        eventType, eventType2;
    LinkedList<EpisodeFromRSS> EpisodesRetrieved = new LinkedList<EpisodeFromRSS>();

    /** Updates a show by checking to see if there are any new episodes available.
     *
     * @param currentShow The number of the current show in relation to the AllShows.SHOW_LIST array
     * @param showSettings The associated SharedPreferences with that show
     * @return A Message object with arg1 being 1 if the show found new episodes, 0 otherwise.
     */
    public Message doUpdate(int currentShow, SharedPreferences showSettings)
    {
        Log.i("*:updateShow", "Beginning update");
        lastChecked = showSettings.getString("last_checked", null);
        String showErrorToast = null;
        this.currentShow=currentShow;
        try
        {
            //Prepare the parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            xpp = factory.newPullParser();
            XmlPullParserFactory factory2 = XmlPullParserFactory.newInstance();
            factory2.setNamespaceAware(true);
            xpp_vid = factory2.newPullParser();
            //URL url = new URL(isVideo ? AllShows.SHOW_LIST_VIDEO[currentShow] : AllShows.SHOW_LIST_AUDIO[currentShow]);
            URL url = new URL(StaticBlob.SHOW_LIST_AUDIO[currentShow]);
            URL url2 = new URL(StaticBlob.SHOW_LIST_VIDEO[currentShow]);
            Log.v("*:updateShow", "URL: " + url + " | " + url2);
            InputStream input = url.openConnection().getInputStream();
            InputStream input2 = url2.openConnection().getInputStream();
            xpp.setInput(input, null);
            xpp_vid.setInput(input2, null);

            Log.v("*:updateShow", "Parser is prepared");
            eventType = xpp.getEventType();
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
                new downloadImage().execute(imgurl, StaticBlob.SHOW_LIST[currentShow]);
                //downloadImage(imgurl, AllShows.SHOW_LIST[currentShow]);
                Log.v("*:updateShow", "Parser is downloading image for " + StaticBlob.SHOW_LIST[currentShow] + ":" + imgurl);
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
                while(!("item".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG))
                {
                    eventType2 = xpp_vid.next();
                    if(eventType2==XmlPullParser.END_DOCUMENT)
                        break;
                }
                if(eventType2==XmlPullParser.END_DOCUMENT)
                    break;
                extractData();
            }

        } catch (XmlPullParserException e) {
            Log.e("*:update:XmlPullParserException", "Parser error");
            e.printStackTrace();
            showErrorToast = "XMLPullParser";
        } catch (MalformedURLException e) {
            Log.e("*:update:MalformedURLException", "Malformed URL? That should never happen.");
            e.printStackTrace();
            showErrorToast = "Bad URL";
        } catch (UnknownHostException e)
        {
            Log.e("*:update:UnknownHostException", "Unable to initiate a connection");
            showErrorToast = "UnknownHost: " + e.getMessage();
        }  catch (IOException e) {
            //FIXME: EXCEPTION: IOException
            Log.e("*:update:IOException", "IO is a moon");
            e.printStackTrace();
            showErrorToast = "I/O";
        } catch (ParseException e) {
            //FIXME: EXCEPTION: ParseException
            Log.e("*:update:ParseException", "Date Parser error");
            showErrorToast = "Date Parse";
        } catch (UnfinishedParseException e) {
            Log.w("*:update:UnfinishedParseException",e.toString());
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
                Log.v("*:updateShow", "Not updating lastChecked: " + newLastChecked);
                m.arg1=0;
            }
            else
            {
                Log.v("*:updateShow", "Updating lastChecked for:" + StaticBlob.SHOW_LIST[currentShow] + "| " + newLastChecked);
                SharedPreferences.Editor editor = showSettings.edit();
                editor.putString("last_checked", newLastChecked);
                editor.commit();
                m.arg1=1;
            }
        }
        Log.i("*:updateShow", "Finished update");
        return m;
    }

    //XmlPullParser.START_TAG==2
    //XmlPullParser.END_TAG==3
    public void extractData() throws UnfinishedParseException, XmlPullParserException, IOException, ParseException
    {
        EpisodeFromRSS ep = new EpisodeFromRSS();
        int numOfDone = 0, numOfDoneVid=0;
        Log.v("*:updateShow", "Starting new episode");
        String titleConfirm = null, dateConfirm = null;
        boolean delicious = false;

        //<START> Block to read in 3 variables to check video feed to audio feed: title, date, and vidlink
        eventType2 = xpp_vid.next();
        while(eventType2!=XmlPullParser.END_DOCUMENT)
        {
            Log.e("VIDEO? " + eventType2 + "==" + XmlPullParser.START_TAG + "/" + XmlPullParser.END_TAG, "?" + xpp_vid.getName() + " *" + "item".equals(xpp_vid.getName()));
            if("title".equals(xpp_vid.getName()) && eventType2 == XmlPullParser.START_TAG)
            {
                eventType2 = xpp_vid.next();
                titleConfirm = xpp_vid.getText();
                Log.d("*:updateShow", "CONFIRM(1): " + titleConfirm);
                if(titleConfirm==null)
                    throw(new UnfinishedParseException("Confirm Title"));
                //!!!!Special case!!!!!!
                //For the weird delicious links, spin until they are out of the system.
                //I hate the idea of this.
                if(titleConfirm.contains("[del.icio.us]"))
                {
                    Log.d("*:updateShow", "Delicious detected; prepare to spin");
                    numOfDoneVid = 0;
                    while(eventType2!=XmlPullParser.END_DOCUMENT && !"item".equals(xpp_vid.getName())
                            && eventType2 != XmlPullParser.END_TAG)
                    {
                        Log.d("*:updateShow", "Spinning..." +  xpp_vid.getName());
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
                Log.d("*:updateShow", "CONFIRM(2): " + dateConfirm);
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
                Log.d("*:updateShow", "   V Link: " + ep.VideoLink);
                String temp = xpp_vid.getAttributeValue(xpp_vid.getNamespace(),"length");
                if(temp==null)
                    throw(new UnfinishedParseException("VideoSize"));
                ep.VideoSize = Long.parseLong(temp);
                Log.d("*:updateShow", "   V Size: " + ep.VideoSize);
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
                Log.d("*:updateShow", "FINISHED W/ VIDEO");
                eventType2 = xpp_vid.next();
                break;
            }
            eventType2 = xpp_vid.next();
        }
        //</END>

        //<START> Read audio info
        eventType = xpp.next();
        while(eventType!=XmlPullParser.END_DOCUMENT)
        {
            Log.e("AUDIO? " + eventType + "==" + XmlPullParser.START_TAG, "?" + xpp.getName() );
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
                    Log.d("*:updateShow", "Delicious detected; prepare to spin " + xpp.getName());
                    numOfDone = 0;
                    while(eventType!=XmlPullParser.END_DOCUMENT
                            && !("item".equals(xpp.getName())
                            && (eventType != XmlPullParser.END_TAG)))
                    {
                        Log.d("*:updateShow", "Spinning..." +  xpp.getName());
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
                Log.d("*:updateShow", "Title: " + ep.Title);
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
                Log.d("*:updateShow", "  Link: " + ep.Link);
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
                Log.d("*:updateShow", "  Desc: " + ep.Desc);
                if((numOfDone & 0x4)==0x4)
                    throw(new UnfinishedParseException("Description 2times"));
                if(ep.Desc!=null)
                    numOfDone |= 0x4;
            }
            else if("pubDate".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                eventType = xpp.next();
                ep.Date = xpp.getText();
                Log.d("*:updateShow", "Date: " + ep.Date);
                if(ep.Date==null)
                    throw(new UnfinishedParseException("Date"));
                if(lastChecked!=null && !StaticBlob.sdfSource.parse(ep.Date).after(StaticBlob.sdfSource.parse(lastChecked)))
                {
                    eventType=XmlPullParser.END_DOCUMENT;
                    return;
                }
                if(newLastChecked==null)
                    newLastChecked = ep.Date;
                if((numOfDone & 0x8)==0x8)
                    throw(new UnfinishedParseException("Date 2times"));
                numOfDone |= 0x8;
            }
            else if("enclosure".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                ep.AudioLink = xpp.getAttributeValue(xpp.getNamespace(),"url");
                Log.d("*:updateShow", "  A Link: " + ep.AudioLink);
                if(ep.AudioLink==null)
                    throw(new UnfinishedParseException("AudioLink"));
                //Sizes
                String temp = xpp.getAttributeValue(xpp.getNamespace(),"length");
                if(temp==null)
                    throw(new UnfinishedParseException("MediaSize"));
                ep.AudioSize = Long.parseLong(temp);
                Log.d("*:updateShow", "A Size: " + ep.AudioSize);
                if((numOfDone & 0x10)==0x10)
                    throw(new UnfinishedParseException("Media 2times"));
                numOfDone |= 0x10;
            }

            //Done
            else if("item".equals(xpp.getName()) && eventType == XmlPullParser.END_TAG)
            {
                int want = (0x1 | 0x4 | 0x8 | 0x10);
                if((numOfDone & want)==want) //Not 0x2 because some items miss <link>
                {
                    Log.d("*:updateShow", "CONFIRMING(1a) " + ep.Title);
                    Log.d("*:updateShow", "CONFIRMING(1a) " + titleConfirm);
                    Log.d("*:updateShow", "CONFIRMING(2a) " + ep.Date.substring(0,16));
                    Log.d("*:updateShow", "CONFIRMING(2b) " + dateConfirm.substring(0, 16));
                    //if(!epTitle.equals(titleConfirm))
                    //    throw(new UnfinishedParseException("Video does not match audio: " + epTitle + "==" +titleConfirm));
                    //if(!epDate.substring(0,16).equals(dateConfirm.substring(0,16)))
                    //    throw(new UnfinishedParseException("Video does not match audio: " + epDate.substring(0,16) + "==" + dateConfirm.substring(0,16)));
                    ep.Date = StaticBlob.sdfRaw.format(StaticBlob.sdfSource.parse(ep.Date));
                    //if(!Callisto.databaseConnector.updateMedia(AllShows.SHOW_LIST[currentShow], epTitle,
                    //isVideo, epMediaLink, epMediaSize))
                    Log.v("*:updateShow", "Inserting episode: " + ep.Title);
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
        public String Date = null, Title = null, Desc = null, Link = null, AudioLink = null, VideoLink = null;
        public long AudioSize, VideoSize;

        public void insert()
        {
            StaticBlob.databaseConnector.insertEpisode(StaticBlob.SHOW_LIST[currentShow], Title, Date, Desc, Link, AudioLink, AudioSize, VideoLink, VideoSize);
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
                Log.i("UpdateShow:downloadImage", "Download Path is: " + f.toString() + ", " + f.exists());
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
                Log.v("*:updateShow", "Failed to download image");
            }
            return null;
        }
    }
}
