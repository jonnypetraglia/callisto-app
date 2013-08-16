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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

//IDEA: Extend it so that it can work with longs, doubles, ect. Might have to use templates?

/** A custom view to select a number via a field with + and - buttons
 * @author MrQweex */
public class ImgTxtButton extends LinearLayout
{
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL, VERTICAL = LinearLayout.VERTICAL;
	private String prefix = "", suffix = "";
	private ImageView img;
    private TextView txt;

    public ImgTxtButton(Context ctx) {
        super(ctx);
        init();
    }

    public ImgTxtButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
        try {
            txt.setText(getResources().getString(attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "text", 0)));
        } catch(Exception e)
        {
            txt.setText(attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "text"));
        }
        try {
            String x = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "textSize");
            int type;
            if(x.endsWith("px"))
                type = TypedValue.COMPLEX_UNIT_PX;
            else if(x.endsWith("dp") || x.endsWith("dip"))
                type = TypedValue.COMPLEX_UNIT_DIP;
            else if(x.endsWith("sp"))
                type = TypedValue.COMPLEX_UNIT_SP;
            else if(x.endsWith("mm"))
                type = TypedValue.COMPLEX_UNIT_MM;
            else if(x.endsWith("pt"))
                type = TypedValue.COMPLEX_UNIT_PT;
            else if(x.endsWith("in"))
                type = TypedValue.COMPLEX_UNIT_IN;
            else
                throw(new Exception());

            if(x.endsWith("dip"))
                x = x.substring(0, x.length()-3);
            else
                x = x.substring(0, x.length()-2);
            txt.setTextSize(type, Float.parseFloat(x));
        } catch(Exception e){}

        txt.setTextColor(attrs.getAttributeUnsignedIntValue("http://schemas.android.com/apk/res/android", "textColor", 0xff000000));
        setOrientation(attrs.getAttributeUnsignedIntValue("http://schemas.android.com/apk/res/android", "orientation", VERTICAL));
        txt.setGravity(attrs.getAttributeUnsignedIntValue("http://schemas.android.com/apk/res/android", "gravity", Gravity.CENTER));
        img.setImageDrawable(getResources().getDrawable(attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "drawable", 0)));
        //*/
    }

    /*
    @TargetApi(11)
    public ImgTxtButton(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }
    */

    public String getText()
    {
    	return txt.getText().toString();
    }

    public void setText(String val)
    {
        txt.setText(val);
    }

    public Drawable getDrawable()
    {
        return img.getDrawable();
    }

    public void setDrawable(Drawable d)
    {
        img.setImageDrawable(d);
    }

    @Override
    public void setOrientation(int d)
    {
        if(d==VERTICAL)
        {
            super.setOrientation(VERTICAL);
            txt.setGravity(Gravity.CENTER);
        }
        else
        {
            super.setOrientation(HORIZONTAL);
            txt.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            txt.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 1f));
            img.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 0f));
        }
    }

    public void hideText()
    {
        txt.setVisibility(View.GONE);
    }

    public void showText()
    {
        txt.setVisibility(View.VISIBLE);
    }

    public void hideImage()
    {
        img.setVisibility(View.GONE);
    }

    public void showImage()
    {
        img.setVisibility(View.VISIBLE);
    }

    /**************************************************************/

    /** Initiates the view */
    private void init()
    {
        txt = new TextView(getContext());
        img = new ImageView(getContext());
        setOrientation(VERTICAL);
        txt.setGravity(Gravity.CENTER);
        addView(img);
        addView(txt);
    }

}
