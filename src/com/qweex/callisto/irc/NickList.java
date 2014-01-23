/*
 * Copyright (C) 2012-2014 Qweex
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
package com.qweex.callisto.irc;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.qweex.callisto.R;
import com.qweex.callisto.StaticBlob;

import java.util.HashSet;
import java.util.List;

/** A dialog to display the people (nicks) in the IRC. Used in StartActivityForResult, so result is important; requires IRCChat.nickList */
public class NickList extends ListActivity
{
    public static HashSet<String> Owners = new HashSet<String>(),         //~ == +q
                                  Admins = new HashSet<String>(),         //& == +a
                                  Operators = new HashSet<String>(), //@ == +o
                                  HalfOperators = new HashSet<String>(), //% == +h
                                  Voices = new HashSet<String>();         //+ == +v

	/** Called when the activity is first created. Sets up the view, mostly, especially if the user is not yet logged in.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
        String TAG = StaticBlob.TAG();
        if(android.os.Build.VERSION.SDK_INT >= 11)
            setTheme(R.style.Default_New);
		super.onCreate(savedInstanceState);
		setTitle(R.string.nicklist);
        Log.d(TAG, "begin");
        getListView().setBackgroundResource(R.color.backClr);
		this.setListAdapter(new NickListAdapter(this, android.R.layout.simple_list_item_1, IRCChat.nickList));
		setResult(-1);
        Log.d(TAG, "end");
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		setResult(position);
		finish();
	}

    /** An adapter for the nicks */
    class NickListAdapter extends ArrayAdapter<String>
    {
        private int layoutResourceID;
        private List<String> content;
        public NickListAdapter(Context context, int textViewResourceId, List<String> objects)
        {
            super(context, textViewResourceId, objects);
            layoutResourceID = textViewResourceId;
            content = objects;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent)
        {
            View v = convertView;
            if(v==null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v=vi.inflate(layoutResourceID, null);
            }
            v.setBackgroundColor(android.R.color.transparent);
            String pre = "";
            //Special characters for special people
            if(Owners.contains(content.get(pos).toLowerCase()))
                pre = "~";
            else if(Admins.contains(content.get(pos).toLowerCase()))
                pre = "&";
            else if(Operators.contains(content.get(pos).toLowerCase()))
                pre = "@";
            else if(HalfOperators.contains(content.get(pos).toLowerCase()))
                pre = "%";
            else if(Voices.contains(content.get(pos).toLowerCase()))
                pre = "+";
            ((TextView)v.findViewById(android.R.id.text1)).setText(pre + content.get(pos));
            ((TextView)v.findViewById(android.R.id.text1)).setTextColor(v.getContext().getResources().getColor(R.color.txtClr));
            return v;
        }
    }
	
}
