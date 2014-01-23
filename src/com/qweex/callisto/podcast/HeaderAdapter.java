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
package com.qweex.callisto.podcast;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;


/** An adapter for listviews with two types of views: header and row (data).
 * http://stackoverflow.com/questions/13590627/android-listview-headers
 * */

public class HeaderAdapter extends ArrayAdapter<HeaderAdapter.Item>
{
    List<Item> items;
    public enum RowType {  LIST_ITEM, HEADER_ITEM;  }

    public HeaderAdapter(Context context, List<Item> items)
    {
        super(context, 0, items);
        this.items = items;
    }

    @Override
    public int getViewTypeCount()
    {
        return RowType.values().length;
    }

    @Override
    public int getItemViewType(int position)
    {
        return items.get(position).getViewType();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        return items.get(position).getView(position, convertView, parent);
    }


    public interface Item
    {
        public int getViewType();
        public View getView(int position, View convertView, ViewGroup parent);
    }
}