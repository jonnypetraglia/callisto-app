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
package com.qweex.utils;

import com.qweex.callisto.R;	//TODO: Fix so that this is not needed
import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.Gravity;

//IDEA: Extend it so that it can work with longs, doubles, ect. Might have to use templates?

/** A custom view to select a number via a field with + and - buttons
 * @author MrQweex */
public class NumberPicker extends LinearLayout
{
	private String prefix = "", suffix = "";
	private int current = 0;
	private EditText edit;
	private int inc = 1;
	private int min=0, max=100;
	
	/** Constructor for the class. Really meaningless, init() does all the work.
	 * @param ctx The context for the NumberPicker */
    public NumberPicker(Context ctx) {
        super(ctx);
        init();
    }
    
    /** Constructor for the class. Really meaningless, init() does all the work.
	 * @param ctx The context for the NumberPicker
	 * @param attrs The attributes for the NumberPicker */
    public NumberPicker(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }
    
    /** Gets the current value of the NumberPicker.
     * @return The current value in the edit field
     */
    public int getValue()
    {
    	return current;
    }
    
    /** Sets the value of the NumberPicker. Must be a valid integer that is within the set bounds.
     * @param val The new value to set
     * @throws Exception Thrown when the number is not within the set bounds of min and max
     */
    public void setValue(int val) throws Exception
    {
    	if(val<min || val>max)
    		throw(new Exception()); //TODO: Create Exception for NumberPicker
    	current = val;
    	refresh();
    }
    
    /** Sets the string prefix that will appear before the number.
     * @param p The new prefix
     */
    public void setPrefix(String p)
    {
    	this.prefix = p;
    	refresh();
    }
    
    /** Sets the string suffix that will appear after the number.
     * @param s The new suffix
     */
    public void setSuffix(String s)
    {
    	this.suffix = s;
    	refresh();
    }
    
    /** Sets the value that the NumberPicker will increment/decrement when the user presses a button.
     * @param i The new increment value
     */
    public void setIncrement(int i)
    {
    	this.inc = i;
    }
    
    /** Sets the range for the NumberPicker.
     * @param _min The minimum
     * @param _max The maximum
     */
    public void setRange(int _min, int _max)
    {
    	this.min = _min;
    	this.max = _max;
    }  //TODO: Error checking for current value
    
    /**************************************************************/
    
    /** Initiates the view */
    private void init()
    {
    	this.setOrientation(LinearLayout.VERTICAL);
    	LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	    Button up = new Button(getContext());
	    up.setBackgroundDrawable(getResources().getDrawable(R.drawable.number_picker_up));
	    up.setOnClickListener(increase);
	    edit = new EditText(getContext());
	    edit.setGravity(Gravity.CENTER_HORIZONTAL);
	    edit.setRawInputType(InputType.TYPE_CLASS_NUMBER);
	    edit.setFilters(new InputFilter[] {new InputFilter() {

			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend)
			//FIXME: Does not allow for keyboard input.
			{
				try {
					if(source.length()==0)
						return null;
					String result = dest.toString();
					String s = (String) source.subSequence(prefix.length(), source.length()-suffix.length());
					result = result.substring(0,dstart) + s.subSequence(start-prefix.length(), end-suffix.length()) + result.substring(dend);
					int i = Integer.parseInt(result);
					if(i<min || i>max)
						return "";
					//Integer.parseInt((String)source);
					current = i;
					//I would do a refresh() here, but it causes this filter to be called, creating an endless loop
					return null;
				} catch(Exception e)
				{
					e.printStackTrace();
					return "";
				}
			}
	    	
	    }});
	    Button down = new Button(getContext());
    	down.setBackgroundDrawable(getResources().getDrawable(R.drawable.number_picker_down));
    	down.setOnClickListener(decrease);
	    addView(up, ll);
	    addView(edit, ll);
	    addView(down, ll);
	    refresh();
    }
    
    /** Refreshes the NumberPicker to the current value with the prefix and suffix. */
    private void refresh()
    {
    	edit.setText(prefix + Integer.toString(current) + suffix);
    }
    
    /** Listener for the "-" button. Decreases the value by the increment value and updates the value. */
    private OnClickListener decrease = new OnClickListener()
    {
		@Override
		public void onClick(View v)
		{
			if(current-inc<min)
				return;
			current-=inc;
			refresh();
		}
    };
    
    /** Listener for the "+" button. Increases the value by the increment value and updates the value. */
    private OnClickListener increase = new OnClickListener()
    {
		@Override
		public void onClick(View v)
		{
			if(current+inc>max)
				return;
			current+=inc;
			refresh();
		}
    };
}
