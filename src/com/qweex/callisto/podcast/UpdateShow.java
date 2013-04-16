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
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
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

/** Tools to update a show. */
public class UpdateShow
{
    String epDate = null, epTitle = null, epDesc = null, epLink = null, epAudioLink = null, epVideoLink = null;
    long epAudioSize, epVideoSize;
    XmlPullParser xpp, xpp_vid;
    String lastChecked, newLastChecked;
    int currentShow,
        eventType, eventType2;

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
        boolean showErrorToast = false;
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
            URL url = new URL(AllShows.SHOW_LIST_AUDIO[currentShow]);
            URL url2 = new URL(AllShows.SHOW_LIST_VIDEO[currentShow]);
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
                eventType2 = xpp_vid.next();
            }
            eventType = xpp.next();
            eventType2 = xpp_vid.next();

            String imgurl = null, imgurl2 = null;
            //Download the image
            while(!("title".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG) && !("thumbnail".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
            {
                //Handles if there is an <image> tag
                if("image".equals(xpp.getName()))
                {
                    if(xpp.getAttributeCount()>0)
                    {
                        imgurl = xpp.getAttributeValue(null, "href");
                        eventType = xpp.next();
                        eventType = xpp.next();
                        eventType2 = xpp_vid.next();
                        eventType2 = xpp_vid.next();
                    }
                    else
                    {
                        eventType = xpp.next();
                        eventType2 = xpp_vid.next();
                        while(!(("image".equals(xpp.getName()) || ("url".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))))
                        {
                            eventType = xpp.next();
                            eventType2 = xpp_vid.next();
                        }
                        if(!("image".equals(xpp.getName())))
                        {
                            eventType = xpp.next();
                            eventType2 = xpp_vid.next();
                            imgurl = xpp.getText();
                            while(!("image".equals(xpp.getName())))
                            {
                                eventType = xpp.next();
                                eventType2 = xpp_vid.next();
                            }
                        }
                    }
                }

                eventType = xpp.next();
                eventType2 = xpp_vid.next();
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
                new downloadImage().execute(imgurl, AllShows.SHOW_LIST[currentShow]);
                //downloadImage(imgurl, AllShows.SHOW_LIST[currentShow]);
                Log.v("*:updateShow", "Parser is downloading image for " + AllShows.SHOW_LIST[currentShow] + ":" + imgurl);
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
                extractData();
            }

        } catch (XmlPullParserException e) {
            Log.e("*:update:XmlPullParserException", "Parser error");
            //TODO EXCEPTION: XmlPullParserExceptionToast.makeText(this, "There is currently no external storage to write to.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            showErrorToast = true;
        } catch (MalformedURLException e) {
            Log.e("*:update:MalformedURLException", "Malformed URL? That should never happen.");
            e.printStackTrace();
            showErrorToast = true;
        } catch (UnknownHostException e)
        {
            Log.e("*:update:UnknownHostException", "Unable to initiate a connection");
            showErrorToast = true;
        }  catch (IOException e) {
            //FIXME: EXCEPTION: IOException
            Log.e("*:update:IOException", "IO is a moon");
            e.printStackTrace();
            showErrorToast = true;
        } catch (ParseException e) {
            //FIXME: EXCEPTION: ParseException
            Log.e("*:update:ParseException", "Date Parser error: |" + epDate + "|");
            showErrorToast = true;
        } catch (UnfinishedParseException e) {
            Log.w("*:update:UnfinishedParseException",e.toString());
            showErrorToast = true;
        }

        if(showErrorToast)
            return null;

        Message m = new Message();
        if(newLastChecked==null)
        {
            Log.v("*:updateShow", "Not updating lastChecked: " + newLastChecked);
            m.arg1=0;
        }
        else
        {
            Log.v("*:updateShow", "Updating lastChecked for:" + AllShows.SHOW_LIST[currentShow] + "| " + newLastChecked);
            SharedPreferences.Editor editor = showSettings.edit();
            editor.putString("last_checked", newLastChecked);
            editor.commit();
            m.arg1=1;
        }
        Log.i("*:updateShow", "Finished update");
        return m;
    }

    public void extractData() throws UnfinishedParseException, XmlPullParserException, IOException, ParseException
    {
        epTitle = epLink = epDesc = epDate = null;
        int numOfDone = 0;
        Log.v("*:updateShow", "Starting new episode");
        xpp.next();
        while(eventType!=XmlPullParser.END_DOCUMENT)
        {
            if("title".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                epTitle = xpp.getText();
                if(epTitle==null)
                    throw(new UnfinishedParseException("Title"));
                if(epTitle.contains("[del.icio.us]")) //endswith should also work
                    return;
                if(epTitle.indexOf("|")>0)
                    epTitle = epTitle.substring(0, epTitle.indexOf("|")).trim();
                Log.d("*:updateShow", "Title: " + epTitle);
                if((numOfDone & 0x1)==0x1)
                    throw(new UnfinishedParseException("Title 2times"));
                numOfDone |= 0x1;
            }
            else if("link".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                epLink = xpp.getText();
                if(epLink==null)
                    throw(new UnfinishedParseException("Link"));
                Log.d("*:updateShow", "Link: " + epLink);
                if((numOfDone & 0x2)==0x2)
                    throw(new UnfinishedParseException("Link 2times"));
                numOfDone |= 0x2;
            }
            else if("description".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                epDesc = xpp.getText();
                //if(epDesc==null)
                    //throw(new UnfinishedParseException("Description"));
                Log.d("*:updateShow", "Desc: " + epDesc + " (from <description>)");
                if((numOfDone & 0x4)==0x4)
                    throw(new UnfinishedParseException("Description 2times"));
                if(epDesc!=null)
                    numOfDone |= 0x4;
            }
            /*
            else if(epDesc==null && ("itunes:summary".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG))
            {
                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                epDesc = xpp.getText();
                Log.d("*:updateShow", "Desc: " + epDesc + " (from <itunes:summary>)");
                if((numOfDone & 0x4)==0x4)
                    throw(new UnfinishedParseException("Description 2times"));
                if(epDesc!=null)
                    numOfDone |= 0x4;
            }
            //*/
            else if("pubDate".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                eventType = xpp.next();
                eventType2 = xpp_vid.next();
                epDate = xpp.getText();
                Log.d("*:updateShow", "Date: " + epDate);
                Log.e("*:updateShow", "Date: " + xpp_vid.getText());
                if(epDate==null)
                    throw(new UnfinishedParseException("Date"));
                if(lastChecked!=null && !StaticBlob.sdfSource.parse(epDate).after(StaticBlob.sdfSource.parse(lastChecked)))
                {
                    eventType=XmlPullParser.END_DOCUMENT;
                    return;
                }
                if(newLastChecked==null)
                    newLastChecked = epDate;
                if((numOfDone & 0x8)==0x8)
                    throw(new UnfinishedParseException("Date 2times"));
                numOfDone |= 0x8;
            }
            else if("enclosure".equals(xpp.getName()) && eventType == XmlPullParser.START_TAG)
            {
                epAudioLink = xpp.getAttributeValue(xpp.getNamespace(),"url");
                epVideoLink = xpp_vid.getAttributeValue(xpp.getNamespace(),"url");
                Log.d("*:updateShow", "A Link: " + epAudioLink);
                Log.d("*:updateShow", "V Link: " + epVideoLink);
                if(epAudioLink==null)
                    throw(new UnfinishedParseException("AudioLink"));
                //Sizes
                String temp = xpp.getAttributeValue(xpp.getNamespace(),"length");
                String temp2 = xpp_vid.getAttributeValue(xpp_vid.getNamespace(),"length");
                if(temp==null)
                    throw(new UnfinishedParseException("MediaSize"));
                epAudioSize = Long.parseLong(temp);
                if(temp2!=null) //TODO: BAD
                    epVideoSize = Long.parseLong(temp2);

                Log.d("*:updateShow", "A Size: " + epAudioSize);
                Log.d("*:updateShow", "V Size: " + epVideoSize);
                if((numOfDone & 0x10)==0x10)
                    throw(new UnfinishedParseException("Media 2times"));
                numOfDone |= 0x10;
            }

            //Done
            if("item".equals(xpp.getName()) && eventType == XmlPullParser.END_TAG)
            {
                int want = (0x1 | 0x4 | 0x8 | 0x10);
                if((numOfDone & want)==want) //Not 0x2 because some items miss <link>
                {
                    epDate = StaticBlob.sdfRaw.format(StaticBlob.sdfSource.parse(epDate));
                    //if(!Callisto.databaseConnector.updateMedia(AllShows.SHOW_LIST[currentShow], epTitle,
                    //isVideo, epMediaLink, epMediaSize))
                    StaticBlob.databaseConnector.insertEpisode(AllShows.SHOW_LIST[currentShow], epTitle, epDate, epDesc, epLink, epAudioLink, epAudioSize, epVideoLink, epVideoSize);
                    Log.v("*:updateShow", "Inserting episode: " + epTitle);
                    xpp.next();
                    return;
                }
                else
                    throw(new UnfinishedParseException("Malformed Item, missing info"));
            }
            eventType = xpp.next();
            eventType2 = xpp_vid.next();
            if(xpp.getName()!=null)
                Log.v("*:updateShow", "Bollocks: [" + xpp.getName());
        } //eventType == END_DOCUMENT
        throw(new UnfinishedParseException("Reached end of file before </item>"));
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
                File f = new File(Environment.getExternalStorageDirectory() + File.separator +
                        StaticBlob.storage_path + File.separator +
                        show + EpisodeDesc.getExtension(img_url));
                System.out.println(f.getAbsolutePath());
                if(f.exists())
                    return null;
                (new File(Environment.getExternalStorageDirectory() + File.separator +
                        StaticBlob.storage_path)).mkdirs();
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
