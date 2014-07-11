package com.qweex.callisto.catalog;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
/*
    This is a specialized LinearLayout that is designed to hold buttons.

    When horizontal (collapsed), the buttons will contain images.
    You can then call the expand() function to flip to vertical
    When vertical (expanded), the buttons will contain images as well as text.

    Right now I'm just going to say that children have to be created programatically.
 */

public class AbbrevLinearLayout extends LinearLayout{
    public AbbrevLinearLayout(Context context) {
        super(context);
    }

    public AbbrevLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbbrevLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public enum STATE {EXPANDED, COLLAPSED, BOTH}
    STATE currentState;
    int textResource, itemBgResource;

    LayoutParams lp_expand;
    LayoutParams lp_collapse;

    public AbbrevLinearLayout setData(int textResource, int itemBgResource, STATE s) {
        this.textResource = textResource;
        this.itemBgResource = itemBgResource;
        this.currentState = s;

        lp_expand = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);

        lp_collapse = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
        lp_collapse.setMargins(20, 0, 20, 0);


        setGravity(Gravity.CENTER);

        return this;
    }

    public AbbrevLinearLayout addItem(int text, int drawable) {
        return addItem(text, drawable, null, STATE.BOTH);
    }

    public AbbrevLinearLayout addItem(Integer text, Integer drawable, OnClickListener click) {
        return addItem(text, drawable, click, STATE.BOTH);
    }

    public AbbrevLinearLayout addItem(Integer text, Integer drawable, STATE s) {
        return addItem(text, drawable, null, s);
    }

    public AbbrevLinearLayout addItem(Integer text, Integer drawable, OnClickListener click, STATE s) {

        if(currentState!=STATE.EXPANDED && currentState!=STATE.COLLAPSED)
            throw new RuntimeException();

        Item i = new Item(getContext());
        i.setData(text, drawable, s);
        i.setOnClickListener(click);

        addView(i);

        return this;
    }

    public void expand() {
        currentState = STATE.EXPANDED;
        setOrientation(LinearLayout.VERTICAL);
        refresh();
    }

    public void collapse() {
        currentState = STATE.COLLAPSED;
        setOrientation(LinearLayout.HORIZONTAL);
        refresh();
    }

    private void refresh() {
        for(int i=0; i<getChildCount(); ++i)
            if(currentState==STATE.EXPANDED)
                ((Item)getChildAt(i)).expand();
            else if(currentState==STATE.COLLAPSED)
                ((Item)getChildAt(i)).collapse();
    }




    class Item extends LinearLayout {
        STATE showInState;
        TextView text;
        ImageView image;

        void init() {
            setOrientation(LinearLayout.HORIZONTAL);
            setClickable(true);
            setBackgroundResource(itemBgResource);
            setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            setPadding(20,20,20,20);
            setGravity(Gravity.CENTER);
            invalidate();
        }

        public Item(Context context) {
            super(context);
            init();
        }

        public Item(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public Item(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            init();
        }

        public void setData(Integer t, Integer d, STATE s) {
            showInState = s;

            if(d!=null) {
                image = new ImageView(getContext());
                image.setImageResource(d);
                addView(image);
            }

            if(t!=null) {
                text = (TextView) LayoutInflater.from(getContext()).inflate(textResource, null);
                text.setText(t);
                addView(text);
                if(currentState==STATE.COLLAPSED)
                    text.setVisibility(View.GONE);
            }

            if(currentState==STATE.EXPANDED)
                expand();
            else if(currentState==STATE.COLLAPSED)
                collapse();
            else
                throw new RuntimeException();
        }

        public void expand() {
            Log.d("Callisto", "Expand");
            if(showInState==STATE.EXPANDED || showInState==STATE.BOTH) {
                setVisibility(VISIBLE);
                setLayoutParams(lp_expand);
                if(text!=null)
                    text.setVisibility(View.VISIBLE);
            } else
                setVisibility(GONE);
        }

        public void collapse() {
            if(showInState==STATE.COLLAPSED || showInState==STATE.BOTH) {
                setVisibility(VISIBLE);
                setLayoutParams(lp_collapse);
                if(text!=null)
                    text.setVisibility(View.GONE);
            } else
                setVisibility(GONE);
        }
    }
}
