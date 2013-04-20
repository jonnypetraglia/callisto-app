/*
Copyright (C) 2012 Qweex

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.qweex.utils;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

//TODO: Keyboard support; keep selection on middle one but move up and down

/**
 * Created with IntelliJ IDEA.
 * User: notbryant
 * Date: 4/15/13
 * Time: 12:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class XBMCStyleListViewMenu extends ListView
{
    /** Font sizes */
    float selectedSize = 0, normalSize = 0;
    /** Font colors */
    int selectedColor = -1, normalColor = -1;
    /** Views of the what was the last selection and current */
    View oldSelection, currentSelection;
    /** Numbers of the position of the views of the same name */
    int oldSelectionPosition = -1, currentSelectionPosition = -1;
    /** Array adapter for the listview */
    SpecialArrayAdapter ssa;
    /** Context, needed for creating the special adapter */
    Context ctext;
    /** Used to count how many children there are when first drawing.
     *  When it is actually drawn, add blanks to the data to make it where the top and bottom items are selectable.
     *  Then it is set to -1, signifying that the work has been done. */
    int numOfVisibleChildren = 0;

    public XBMCStyleListViewMenu(Context context)
    {
        super(context);
        init(context);
    }
    public XBMCStyleListViewMenu(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }
    public XBMCStyleListViewMenu(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(context);
    }

    /** Initialize the view */
    protected void init(Context context)
    {
        ctext = context;
    }

    /** Used to pad the data with blanks so that the top and bottom choices are selectable. */
    @Override
    protected void onDraw(Canvas canvas)
    {
        if(numOfVisibleChildren<0)
            return;
        if(getChildCount()>numOfVisibleChildren)
            numOfVisibleChildren = getChildCount();
        else
        {
            Log.i("SuperListViewMenu:onDraw", "Adding blanks: " + getChildCount()/2);
            numOfVisibleChildren = -1;
            ssa.addBlanks(getLastVisiblePosition()-getFirstVisiblePosition());
            this.smoothScrollByOffset((getLastVisiblePosition()-getFirstVisiblePosition())/2 - 1);
            //this.smoothScrollToPosition((int)(getChildCount()*1.5));
        }
    }

    /** Used to set the data for the adapter */
    public void setData(List<String> list)
    {
        ArrayList al = new ArrayList(list);
        ssa =  new SpecialArrayAdapter(ctext, com.qweex.callisto.R.layout.tablet_row, al);
        setAdapter(ssa);
        setOnItemClickListener(anyItemClicked);
        setOnScrollListener(scrollListener);
    }

    /** Handles reforming the current and old views. */
    private OnScrollListener scrollListener = new OnScrollListener()
    {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {}

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
        {
            //If they are literally display all of the items there is no need to scroll
            if(getFirstVisiblePosition()==0 && getLastVisiblePosition()==(getAdapter().getCount()-1))
                return;

            visibleItemCount--; //ensures that it will choose more toward the top of there is an odd number of items
            if(currentSelectionPosition== (visibleItemCount / 2 + firstVisibleItem))    //If the current has not changed, stop
                return;
            //Update the variables
            oldSelection = currentSelection;
            oldSelectionPosition = currentSelectionPosition;
            currentSelection = getChildAt(visibleItemCount / 2);
            currentSelectionPosition = visibleItemCount / 2 + firstVisibleItem;
            invalidateViews();
        }
    };

    /** Setter */
    public void setSelectedSize(float sel)
    {
        selectedSize = sel;
    }
    /** Getter */
    public float getSelectedSize()
    {
        return selectedSize;
    }
    /** Setter */
    public void setNormalSize(float nor)
    {
        normalSize=nor;
        if(selectedSize==0)
            selectedSize = normalSize*2;
    }
    /** Getter */
    public float getNormalSize()
    {
        return normalSize;
    }
    /** Setter */
    public void setSelectedColor(int s)
    {
        selectedColor = s;
    }
    public int getSelectedColor()
    {
        return selectedColor;
    }
    public void setNormalColor(int n)
    {
        normalColor=n;
        if(selectedColor==-1)
            selectedColor=normalColor;
    }


    class SpecialArrayAdapter extends ArrayAdapter
    {
        int resId;
        List<String> objects;
        protected int numOfBlanks;

        public SpecialArrayAdapter(Context context, int textViewResourceId, List objects)
        {
            super(context, textViewResourceId, objects);
            resId=textViewResourceId;
            this.objects=objects;
        }

        @Override
        public View getView(int position, View v, ViewGroup vg)
        {
            //v = super.getView(position, v, vg);
            //*if(v==null)// || v==oldSelection)//true)
            {
                LayoutInflater inflater= (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(resId, vg, false);
            }//*/
            if(normalSize==0)
                setNormalSize(((TextView)v.findViewById(R.id.text1)).getTextSize());
            if(normalColor==-1)
                setNormalColor(((TextView)v.findViewById(R.id.text1)).getCurrentTextColor());

            if(position==currentSelectionPosition)
            {
                ((TextView)v.findViewById(R.id.text1)).setTextColor(selectedColor);
                ((TextView)v.findViewById(R.id.text1)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, selectedSize);
            }
            else
            {
                ((TextView)v.findViewById(R.id.text1)).setTextColor(normalColor);
                ((TextView)v.findViewById(R.id.text1)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, normalSize);
            }


            ((TextView)v.findViewById(R.id.text1)).setText(objects.get(position));
            v.setFocusable(false);
            return v;
        }

        public void addBlanks(int childCount)
        {
            numOfBlanks=0;
            System.out.println("Blanks: " + numOfBlanks + "<=" + ((childCount-1)/2));
            while(numOfBlanks<=((childCount-1)/2))
            {
                objects.add(0,"");
                objects.add("");
                System.out.println("AddBlanks: " + numOfBlanks + "<" + childCount);
                numOfBlanks++;
            }
            notifyDataSetChanged();
            if(true==true)
                return;

            numOfBlanks = childCount-1;
            for(int i=0; i<numOfBlanks; i++)
            {
                objects.add(0,"");
                objects.add("");
            }
            objects.add(0,"");
            objects.add("");
            notifyDataSetChanged();
        }
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
                    whatDo.onMainItemClick(view, position-ssa.numOfBlanks);
                return;
            }

            if(getFirstVisiblePosition()==0 && getLastVisiblePosition()==(getAdapter().getCount()-1))
            {
                oldSelection = currentSelection;
                oldSelectionPosition = currentSelectionPosition;
                currentSelection = view;
                currentSelectionPosition = position;
                invalidateViews();
                return;
            }

            smoothScrollByOffset(position-currentSelectionPosition);
            System.out.println("Derpina: " + ((TextView)view.findViewById(R.id.text1)).getText().toString() + " | ");
        }
    };
}
