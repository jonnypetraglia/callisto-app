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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

public class ScrollViewWithBottom extends ScrollView
{

    public ScrollViewWithBottom(Context context) {
        super(context);
    }

    public ScrollViewWithBottom(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollViewWithBottom(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    boolean isAtBottom;

    public boolean atBottom()
    {
        //System.out.println("AtBottom:==" + isAtBottom);
        //return isAtBottom;
        return true;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt)
    {
        View view = (View) getChildAt(getChildCount() - 1);
        int diff = (view.getBottom() - (getHeight()+getScrollY()));
        isAtBottom = diff<=0;
        System.out.println("AtBottom:DIFF: " + diff +  "== " + isAtBottom);
        super.onScrollChanged(l, t, oldl, oldt);
    }

    //http://stackoverflow.com/questions/12884572/scrollview-scrollto-doesnt-work
    // Properties
    private int desiredScrollX = -1;
    private int desiredScrollY = -1;
    private ViewTreeObserver.OnGlobalLayoutListener gol;

    public void scrollToWithGuarantees(int __x, int __y)
    {
        // REALLY Scrolls to a position
        // When adding items to a scrollView, you can't immediately scroll to it - it takes a while
        // for the new addition to cycle back and update the scrollView's max scroll... so we have
        // to wait and re-set as necessary

        scrollTo(__x, __y);

        desiredScrollX = -1;
        desiredScrollY = -1;

        System.out.println("AtBottom: SCROLLING " + __x + " " + __y);
        if (getScrollX() != __x || getScrollY() != __y) {
            // Didn't scroll properly: will create an event to try scrolling again later

            if (getScrollX() != __x) desiredScrollX = __x;
            if (getScrollY() != __y) desiredScrollY = __y;

            if (gol == null) {
                gol = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int nx = desiredScrollX == -1 ? getScrollX() : desiredScrollX;
                        int ny = desiredScrollY == -1 ? getScrollY() : desiredScrollY;
                        desiredScrollX = -1;
                        desiredScrollY = -1;
                        scrollTo(nx, ny);
                        System.out.println("AtBottom: SCROLLING2 " + getScrollX() + " " + getScrollY());
                    }
                };

                getViewTreeObserver().addOnGlobalLayoutListener(gol);
            }
        }
    }
}
