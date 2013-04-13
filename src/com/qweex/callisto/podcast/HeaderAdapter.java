/*
Copyright (C) 2012 Qweex
This file is a part of Callisto.

Callisto is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

Callisto is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Callisto; If not, see <http://www.gnu.org/licenses/>.
*/
package com.qweex.callisto.podcast;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

//http://stackoverflow.com/questions/13590627/android-listview-headers/**

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