package com.qweex.callisto;

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

public class NumberPicker extends LinearLayout
{
	private String prefix = "", suffix = "";
	private int current = 0;
	private EditText edit;
	private int inc = 1;
	private int min=0, max=100;
	
    public NumberPicker(Context ctx) {
        super(ctx);
        init();
    }
    
    public NumberPicker(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }
    
    public int getValue()
    {
    	return current;
    }
    
    public void setValue(int i) throws Exception
    {
    	if(i<min || i>max)
    		throw(new Exception()); //TODO: Create Exception for NumberPicker
    	current = i;
    	refresh();
    }
    
    public void setPrefix(String p)
    {
    	this.prefix = p;
    	refresh();
    }
    
    public void setSuffix(String s)
    {
    	this.suffix = s;
    	refresh();
    }
    
    public void setIncrement(int i)
    {
    	this.inc = i;
    }
    
    public void setRange(int _min, int _max)
    {
    	this.min = _min;
    	this.max = _max;
    }
    
    /**************************************************************/
    
    private void init()
    {
    	this.setOrientation(LinearLayout.VERTICAL);
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
	    addView(up);
	    addView(edit);
	    addView(down);
	    refresh();
    }
    
    private void refresh()
    {
    	edit.setText(prefix + Integer.toString(current) + suffix);
    }
    
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
