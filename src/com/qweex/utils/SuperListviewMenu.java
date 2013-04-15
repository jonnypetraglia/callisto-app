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

package com.qweex.utils;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: notbryant
 * Date: 4/15/13
 * Time: 12:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class SuperListviewMenu extends ListView
{
    float selectedSize = 0, normalSize = 0;
    View oldSelection, currentSelection;
    int oldSelectionPosition = -1, currentSelectionPosition = -1;
    SpecialArrayAdapter ssa;
    Context ctext;

    public SuperListviewMenu(Context context)
    {
        super(context);
        init(context);
    }
    public SuperListviewMenu(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }
    public SuperListviewMenu(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(context);
    }

    protected void init(Context context)
    {
        ctext = context;
        setDivider(null);
        setBackgroundColor(0x00000000);
        setCacheColorHint(0x00000000);
    }

    public void setData(List<String> list)
    {
        ssa =  new SpecialArrayAdapter(ctext, com.qweex.callisto.R.layout.tablet_row, new ArrayList(list));
        setAdapter(ssa);
        setOnItemClickListener(anyItemClicked);
        setOnScrollListener(sl);
    }

    OnScrollListener sl = new OnScrollListener()
    {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {}

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
        {
            System.out.println("DERP:" + getFirstVisiblePosition() + "->" + getLastVisiblePosition() + " = " + getAdapter().getCount());
            if(getFirstVisiblePosition()==0 && getLastVisiblePosition()==(getAdapter().getCount()-1))
                return;

            visibleItemCount--;
            if(currentSelectionPosition== (visibleItemCount / 2 + firstVisibleItem))
                return;
            oldSelection = currentSelection;
            oldSelectionPosition = currentSelectionPosition;
            currentSelection = getChildAt(visibleItemCount / 2);
            currentSelectionPosition = visibleItemCount / 2 + firstVisibleItem;
            if(oldSelection!=null)
            {
                ((TextView) oldSelection.findViewById(R.id.text1)).setTextSize(TypedValue.COMPLEX_UNIT_SP, normalSize);
                invalidateViews();
            }
        }
    };

    public void setSelectedSize(float sel)
    {
        selectedSize = sel;
    }
    //TODO: Getter
    //TODO: Text colors/effects

    void initiateSizes(float nor)
    {
        normalSize=nor;
        if(selectedSize==0)
            selectedSize = normalSize*2;
    }

    class SpecialArrayAdapter extends ArrayAdapter
    {
        int resId;
        List<String> objects;

        public SpecialArrayAdapter(Context context, int textViewResourceId, List objects)
        {
            super(context, textViewResourceId, objects);
            resId=textViewResourceId;
            this.objects=objects;
        }

        @Override
        public View getView(int position, View v, ViewGroup vg)
        {
            v = super.getView(position, v, vg);
            //if(v==null)// || v==oldSelection)//true)
            {
                LayoutInflater inflater= (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(resId, vg, false);
            }
            if(normalSize==0)
                initiateSizes(((TextView)v.findViewById(R.id.text1)).getTextSize());

            if(position==currentSelectionPosition)
                ((TextView)v.findViewById(R.id.text1)).setTextSize(TypedValue.COMPLEX_UNIT_SP, selectedSize);
            else
                ((TextView)v.findViewById(R.id.text1)).setTextSize(TypedValue.COMPLEX_UNIT_SP, normalSize);


            ((TextView)v.findViewById(R.id.text1)).setText(objects.get(position));
            v.setFocusable(false);
            return v;
        }

        /*
        @Override
        public int getCount()
        {
            return Integer.MAX_VALUE;
        }

        @Override
        public String getItem(int position)
        {
            return objects[position % objects.length]
        }
        */
    }

    public void setOnMainItemClickListener(OnMainItemClickListener l)
    {
        whatDo=l;
    }
    public void removeOnMainItemClickListener()
    {
        whatDo=null;
    }

    OnMainItemClickListener whatDo;
    public interface OnMainItemClickListener
    {
        public void onMainItemClick(View v, int position);
    }

    private OnItemClickListener anyItemClicked = new OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            System.out.println("DERP:" + getFirstVisiblePosition() + "->" + getLastVisiblePosition() + " = " + getAdapter().getCount());
            if(position==currentSelectionPosition)
            {
                if(whatDo!=null)
                    whatDo.onMainItemClick(view, position);
                return;
            }

            if(getFirstVisiblePosition()==0 && getLastVisiblePosition()==(getAdapter().getCount()-1))
            {
                oldSelection = currentSelection;
                oldSelectionPosition = currentSelectionPosition;
                currentSelection = view;
                currentSelectionPosition = position;
                if(oldSelection!=null)
                {
                    ((TextView) oldSelection.findViewById(R.id.text1)).setTextSize(TypedValue.COMPLEX_UNIT_SP, normalSize);
                    invalidateViews();
                }
                return;
            }

            if(position<currentSelectionPosition)
                smoothScrollToPosition(getFirstVisiblePosition()-(currentSelectionPosition-position) + 1);
            else
                smoothScrollToPosition(getLastVisiblePosition()+(position-currentSelectionPosition) - 1);
            System.out.println("Derpina: " + ((TextView)view.findViewById(R.id.text1)).getText().toString() + " | ");
        }
    };
}
