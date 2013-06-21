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
package com.qweex.callisto.moar;

import android.app.ListActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.qweex.callisto.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

public class twit extends ListActivity
{
    public static String user = "ChrisLAS";
    int count = 50;
    boolean includeRetweets = true;
    public Bitmap profilePic;
    View headerView;
    TwitterJSONAdapter twitterAdapter;

    @Override
    public void onCreate(Bundle savedInstances)
    {
        super.onCreate(savedInstances);
        headerView = getLayoutInflater().inflate(R.layout.twit_hdr, null, false);
        getListView().setBackgroundColor(0xffffffff);
        getListView().setCacheColorHint(0xffffffff);
        new downloadImage().execute(this);
    }

    public static class downloadImage extends AsyncTask<twit, Void, String>
    {
        View headerView;
        twit thisTwit;
        @Override
        protected void onPreExecute()
        {
        }

        @Override
        protected void onPostExecute(String errorMsg)
        {
            if(errorMsg!=null)
            {
                TextView derp = new TextView(thisTwit);
                derp.setText("Sorry, an error occurred: " + errorMsg);
                thisTwit.getListView().setEmptyView(derp);
                Log.e("Error", errorMsg);
                //thisTwit.finish();
            }
            else
            {
                ((ImageView)headerView.findViewById(R.id.profile_pic)).setImageBitmap(thisTwit.profilePic);
                ((TextView)headerView.findViewById(R.id.twit_user)).setText("@" + thisTwit.user);
                thisTwit.getListView().addHeaderView(headerView);
            }
        }

        @Override
        protected String doInBackground(twit... twis)
        {
            thisTwit = twis[0];
            headerView = thisTwit.headerView;

            //Get the profile pic
            try {
                URL url = new URL("https://api.twitter.com/1/users/profile_image?screen_name=" + user + "&size=normal");
                thisTwit.profilePic = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch(MalformedURLException m) {} catch(IOException i) {}

            //Get the
            try {
                String jsonString = callURL("https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=" + thisTwit.user + "&count=" + thisTwit.count + "&include_rts=" + (thisTwit.includeRetweets?1:0));
                JSONArray jsonArray = new JSONArray(jsonString);
                thisTwit.twitterAdapter = thisTwit.new TwitterJSONAdapter(jsonArray, R.layout.twit);
                thisTwit.getListView().setAdapter(thisTwit.twitterAdapter);

            }catch(Exception e)
            {
                return e.getMessage();
            }
            return null;
        }
    }


    public static String callURL(String myURL) {
        System.out.println("Requested URL:" + myURL);
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(myURL);
            urlConn = url.openConnection();
            if (urlConn != null)
                urlConn.setReadTimeout(60 * 1000);
            if (urlConn != null && urlConn.getInputStream() != null) {
                in = new InputStreamReader(urlConn.getInputStream(),
                        Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(in);
                if (bufferedReader != null) {
                    int cp;
                    while ((cp = bufferedReader.read()) != -1) {
                        sb.append((char) cp);
                    }
                    bufferedReader.close();
                }
            }
            in.close();
        } catch (Exception e) {
            throw new RuntimeException("Exception while calling URL:"+ myURL, e);
        }

        return sb.toString();
    }

    public class TwitterJSONAdapter extends BaseAdapter
    {
        JSONArray data;
        int layout_id;

        public TwitterJSONAdapter(JSONArray data, int layout_id)
        {
            super();
            this.data = data;
            this.layout_id = layout_id;
        }

        @Override
        public int getCount() {
            return data.length();
        }

        @Override
        public Object getItem(int i) {
            try {
                return data.get(i);
            } catch(JSONException e)
            {
                return null;
            }
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view==null)
            {
                LayoutInflater inflater=getLayoutInflater();
                view = inflater.inflate(R.layout.twit, viewGroup, false);
            }
            try {
                JSONObject currData = (JSONObject) data.get(i);

                String created_at = currData.getString("created_at");
                int id = currData.getInt("id");
                String text = currData.getString("text");
                int favorite_count = currData.getInt("favorite_count");
                int retweet_count = currData.getInt("retweet_count");
                String in_reply_to_screen_name = currData.getString("in_reply_to_screen_name");

                String retweeted_user = null;
                try {
                    JSONObject retweeted_status = (JSONObject) currData.getJSONObject("retweeted_status");
                    if(retweeted_status!=null)
                        retweeted_user = "↻" + retweeted_status.getJSONObject("user").getString("screen_name");
                }catch(JSONException e)
                {}

                ((TextView)view.findViewById(R.id.text)).setText(text);
                ((TextView)view.findViewById(R.id.date)).setText(formatDate(created_at));
                ((TextView)view.findViewById(R.id.retweeted_user)).setText(retweeted_user);

            } catch(JSONException j)
            {
                Log.e("DERP", "!" + j.getClass());
                return view;
            }

            return view;
        }
    }

    SimpleDateFormat sf = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy",java.util.Locale.ENGLISH);
    String[] months = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    String formatDate(String created_at)
    {
        try {
            Date x = sf.parse(created_at);
            long diff = new Date().getTime() - x.getTime();

            long millisInMinute = (long)1000*60;
            long millisInHour = millisInMinute*60;
            long millisInDay = millisInHour*24;
            long millisInYear = millisInDay*365;
            long r;
            if(diff >= millisInYear) //MillisecondsInYear
            {
                r = diff/millisInYear;
                return r + " year" + (r>1?"s":"");
            }
            else if(diff >= millisInDay)
            {
                r = diff/millisInDay;
                //if(r>1)
                    return r + " day" + (r>1?"s":"");
                //else
                  //  return "yesterday";
                //return months[x.getMonth()] + " " + x.getDate();
            }
            else if(diff > millisInHour)
            {
                r = diff/millisInHour;
                return r + " hour" + (r>1?"s":"");
            }
            else
            {
                r = diff/millisInMinute;
                return r + " min";
            }

            //X minutes

            //1 hour

            //X hours

            //yesterday

            //X days

            //1 year ago

            //X years ago
        } catch(Exception e)
        {
            return null;
        }
    }
}
