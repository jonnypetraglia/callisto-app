package com.qweex.callisto.catalog;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** A Data structure to contain info about a show
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class ShowInfo {

    /** Show info */
    public String title, audioFeed, videoFeed;
    public String lastChecked;
    boolean active;

    /** Metadata */
    SharedPreferences settings;

    /** Constructor; creates object from JSON
     * @param j JSON Object containing data.
     */
    public ShowInfo(JSONObject j) throws JSONException {
        title = j.getString("title");
        audioFeed = j.getString("audio");
        try {
            videoFeed = j.getString("video");
        } catch(JSONException e) {}
        active = j.getBoolean("active");
    }

    /** Sorter for the show list */
    static class ShowComparator implements Comparator<ShowInfo> {
        @Override
        public int compare(ShowInfo s1, ShowInfo s2) {
            return s1.title.compareTo(s2.title);
        }
    }

    ////////////////// Static methods //////////////////

    /** Reads JSON from a stream.
     *
     * @param is Input Stream to read from
     * @return ArrayList of ShowInfo objects.
     */
    static public ArrayList<ShowInfo> readJSON(InputStream is) {
        JSONArray json = null;
        ArrayList<ShowInfo> json_parsed = new ArrayList<ShowInfo>();
        try {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new JSONArray(new String(buffer, "UTF-8"));

            for(int i=0; i<json.length(); ++i)
                json_parsed.add(new ShowInfo((JSONObject) json.get(i)));

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(json_parsed, new ShowInfo.ShowComparator());
        return json_parsed;
    }
}
