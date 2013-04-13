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
package com.qweex.callisto.irc;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
		super.onCreate(savedInstanceState);
		setTitle("Nick List");
		this.setListAdapter(new NickListAdapter(this, android.R.layout.simple_list_item_1, IRCChat.nickList));
		setResult(-1);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		setResult(position);
		finish();
	}

    class NickListAdapter extends ArrayAdapter<String>
    {
        int resID;
        List<String> content;
        public NickListAdapter(Context context, int textViewResourceId, List<String> objects) {
            super(context, textViewResourceId, objects);
            resID = textViewResourceId;
            content = objects;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent){
            View v = convertView;
            if(v==null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v=vi.inflate(resID, null);
            }
            String pre = "";
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
            return v;
        }
    }
	
}
