/*
 * Copyright (C) 2010 Draggable and Droppable ListView Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 /*
  * functions description:
  * pointToPosition(x,y) Maps a point to a position in the list.
  * 
  */
package com.andrefy.ddviewlist;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;


public class DDListView extends ListView {

    private int mDragPos;      // which item is being dragged
    private int mFirstDragPos; // where was the dragged item originally
    private int mDragPoint;    // at what offset inside the item did the user grab it
    private int mCoordOffset;  // the difference between screen coordinates and coordinates in this view
    
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private Rect mTempRect = new Rect();
    
    //dragging elements
    private Bitmap mDragBitmap;
    private ImageView mDragView;
    private int mHeight;
    private int mUpperBound;
    private int mLowerBound;
    private int mTouchSlop;

    public int mItemHeightHalf = -1;
    public int mItemHeightNormal = -1;
    public int mItemHeightExpanded = -1;
    public int mItemColor = -1;

	private DragListener mDragListener;
	private DropListener mDropListener;


    //*******************QWEEX*******************
    private int mDragControlID=-1;


	public DDListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	    Resources res = getResources();
	}

    //Called when a row is touched; called BEFORE onTouchEvent
	@Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //QWEEX DID THIS
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        int itemnum = pointToPosition(x, y);
        if (itemnum == AdapterView.INVALID_POSITION) {
            return super.onInterceptTouchEvent(ev);
        }
        ViewGroup item = (ViewGroup) getChildAt(itemnum - getFirstVisiblePosition());
        boolean insideCtrl = true;
        if(mDragControlID>-1)
        {
            try {
            View childView = item.findViewById(mDragControlID);
            int[] evXY = new int[2];
            int[] controlXY = new int[2];
            int[] controlWH = new int[2];

            evXY[0] = (int)ev.getRawX();
            evXY[1] = (int)ev.getRawY();
            childView.getLocationInWindow(controlXY);
            controlWH[0] = childView.getMeasuredWidth();
            controlWH[1] = childView.getMeasuredHeight();

                Log.d(":", evXY[0] + ">" + controlXY[0] + " && " + evXY[0] + "<" + (controlXY[0]+controlWH[0]) + "==" + controlXY[0] + "+" + controlWH[0]);
            insideCtrl = (evXY[0]>controlXY[0] && evXY[0]<(controlXY[0]+controlWH[0]));
                Log.d(":", evXY[1] + ">" + controlXY[1] + " && " + evXY[1] + "<" + (controlXY[1]+controlWH[1]) + "==" + controlXY[1] + "+" + controlWH[1]);
            insideCtrl &= (evXY[1]>controlXY[1] && evXY[1]<(controlXY[1]+controlWH[1]));
            Log.d(":", insideCtrl + "!");
            }catch(NullPointerException e){}
        }
        //END OF QWEEX


        if ((mDragListener != null || mDropListener != null) && insideCtrl) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDragPoint = y - item.getTop();
                    mCoordOffset = ((int)ev.getRawY()) - y;
                    item.setDrawingCacheEnabled(true);
                    Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
                    startDragging(bitmap, y);
                    mDragPos = itemnum;
                    mFirstDragPos = mDragPos;
                    mHeight = getHeight();
                    int touchSlop = mTouchSlop;
                    mUpperBound = Math.min(y - touchSlop, mHeight / 3);
                    mLowerBound = Math.max(y + touchSlop, mHeight * 2 /3);
                    return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    //Gets the child underneath the specified point.
	private int myPointToPosition(int x, int y) {
        if (y < 0) {
            int pos = myPointToPosition(x, y + mItemHeightNormal);
            if (pos > 0) {
                return pos - 1;
            }
        }
        Rect frame = mTempRect;
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i;
            }
        }
        return INVALID_POSITION;
    }

    //?????????????
    private int getItemForPosition(int y) {
        int adjustedy = y - mDragPoint - mItemHeightHalf;
        int pos = myPointToPosition(0, adjustedy);
        Log.d(":", "getItemForPosition: " + adjustedy + " " + pos + "<=" + mFirstDragPos);
        if (pos >= 0) {
            if (pos <= mFirstDragPos) {
                pos += 1;
            }
        } else if (adjustedy < 0) {
            pos = 0;
        }
        return pos;
    }

    //???????????????
    private void adjustScrollBounds(int y) {
        if (y >= mHeight / 3) {
            mUpperBound = mHeight / 3;
        }
        if (y <= mHeight * 2 / 3) {
            mLowerBound = mHeight * 2 / 3;
        }
    }

    private void unExpandViews(boolean deletion) {
        for (int i = 0;; i++) {
            View v = getChildAt(i);
            if (v == null) {
                if (deletion) {
                    int position = getFirstVisiblePosition();
                    int y = getChildAt(0).getTop();
                    setAdapter(getAdapter());
                    setSelectionFromTop(position, y);
                }
                layoutChildren(); 
                v = getChildAt(i);
                if (v == null) {
                    break;
                }
            }
            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = mItemHeightNormal;
            v.setLayoutParams(params);
            v.setVisibility(View.VISIBLE);
        }
    }

    private void doExpansion() {
        int childnum = mDragPos - getFirstVisiblePosition();
        if (mDragPos > mFirstDragPos) {
            childnum++; //QWEEX
        }
        Log.d(":", "doExpansion");

        View first = getChildAt(mFirstDragPos - getFirstVisiblePosition());

        for (int i = 0;; i++) {
            View vv = getChildAt(i);
            if (vv == null) {
                break;
            }
            int height = mItemHeightNormal;
            int visibility = View.VISIBLE;
            //QWEEX
            if (vv.equals(first)) {
               if (mDragPos == mFirstDragPos) {
                   visibility = View.INVISIBLE;
                } else {
                    height = 1;
                }
            } else //*/
            if (i == childnum) {
                if (mDragPos < getCount() - 1) {
                    height = mItemHeightExpanded;
                }
            }
            ViewGroup.LayoutParams params = vv.getLayoutParams();
            params.height = height;
            vv.setLayoutParams(params);
            vv.setVisibility(visibility);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if ((mDragListener != null || mDropListener != null) && mDragView != null) {
            int action = ev.getAction(); 
            switch (action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Rect r = mTempRect;
                    mDragView.getDrawingRect(r);
                    stopDragging();
                    if (mDropListener != null && mDragPos >= 0 && mDragPos < getCount()) {
                        mDropListener.drop(mFirstDragPos, mDragPos);
                     }
                     unExpandViews(false);
                    break;
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    dragView(x, y);
                    int itemnum = getItemForPosition(y);
                    if (itemnum >= 0) {
                        Log.d(":", "onTouchEvent" +  itemnum);
                        if (action == MotionEvent.ACTION_DOWN || itemnum != mDragPos) {
                            Log.d(":", "onTouchEvent(Webedraggin)" +  mDragPos);
                            if (mDragListener != null) {
                                mDragListener.drag(mDragPos, itemnum);
                            }
                            mDragPos = itemnum;
                            doExpansion();
                        }
                        int speed = 0;
                        adjustScrollBounds(y);
                        if (y > mLowerBound) {
                            // scroll the list up a bit
                            speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
                        } else if (y < mUpperBound) {
                            // scroll the list down a bit
                            speed = y < mUpperBound / 2 ? -16 : -4;
                        }
                        if (speed != 0) {
                            int ref = pointToPosition(0, mHeight / 2);
                            if (ref == AdapterView.INVALID_POSITION) {
                                //we hit a divider or an invisible view, check somewhere else
                                ref = pointToPosition(0, mHeight / 2 + getDividerHeight() + 64);
                            }
                            View v = getChildAt(ref - getFirstVisiblePosition());
                            if (v!= null) {
                                int pos = v.getTop();
                                setSelectionFromTop(ref, pos - speed);
                            }
                        }
                    }
                    break;
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }
    
    private void startDragging(Bitmap bm, int y) {
        stopDragging();

        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPoint + mCoordOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;
        
        Context context = getContext();
        ImageView v = new ImageView(context);
        int backGroundColor = mItemColor;
        v.setBackgroundColor(backGroundColor);
        v.setImageBitmap(bm);
        mDragBitmap = bm;

        mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
    }
    
    private void dragView(int x, int y) {
        mWindowParams.y = y - mDragPoint + mCoordOffset;
        mWindowManager.updateViewLayout(mDragView, mWindowParams);
    }
    
    private void stopDragging() {
        if (mDragView != null) {
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
        if (mDragBitmap != null) {
            mDragBitmap.recycle();
            mDragBitmap = null;
        }
    }



    
    public void setDragListener(DragListener l) {
        mDragListener = l;
    }

    public void setDropListener(DropListener l, int ControlID) {
        mDragControlID = ControlID;
        setDropListener(l);
    }

    public void setDropListener(DropListener l) {
        mDropListener = l;
    }
    
    public interface DragListener {
        void drag(int from, int to);
    }
    public interface DropListener {
        void drop(int from, int to);
    }
    public interface RemoveListener {
        void remove(int which);
    }
}
