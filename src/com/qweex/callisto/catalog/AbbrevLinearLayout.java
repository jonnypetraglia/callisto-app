package com.qweex.callisto.catalog;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This is a specialized LinearLayout that is designed to hold buttons.
 *
 * When horizontal (collapsed), the buttons will contain images.
 * You can then call the expand() function to flip to vertical.
 * When vertical (expanded), the buttons will contain images as well as text.
 *
 * Right now I'm just going to say that children have to be created programatically via
 * 'setData()' followed by 'addItem()'.
 *
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class AbbrevLinearLayout extends LinearLayout {

    public enum STATE {EXPANDED, COLLAPSED, BOTH}
    STATE currentState;
    // Resources to be used for each item
    int textResource, itemBgResource;

    // Layout Params for each of the states;
    LayoutParams lp_expand, lp_collapse;


    /**
     * Default constructor needed by XML
     *
     * @param context The Context
     */
    public AbbrevLinearLayout(Context context) {
        super(context);
    }

    /**
     * Default constructor needed by XML
     *
     * @param context The Context
     * @param attrs The Attributes specified in XML
     */
    public AbbrevLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Default constructor needed by XML
     *
     * @param context The Context
     * @param attrs The Attributes specified in XML
     * @param defStyle The style specified in the XML
     */
    public AbbrevLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets data for the AbbrevLinearLayout. This MUST be called before adding any items.
     *
     * @param textResource Text R.layout id that will be inflated for each button.
     * @param itemBgResource The R.drawable id that will
     * @param startingState The starting state. CANNOT be 'BOTH'.
     * @return 'this'. Useful for chaining together with other functions (like addItem)
     */
    public AbbrevLinearLayout setData(int textResource, int itemBgResource, STATE startingState) {
        this.textResource = textResource;
        this.itemBgResource = itemBgResource;
        this.currentState = startingState;
        if(startingState!=STATE.EXPANDED && startingState!=STATE.COLLAPSED)
            throw new RuntimeException("Illegal starting state.");

        // Init the LayoutParams for the children. FOR THE CHILDREN!
        lp_expand = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);
        lp_collapse = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
        lp_collapse.setMargins(20, 0, 20, 0);

        setGravity(Gravity.CENTER);

        return this;
    }

    /**
     * Adds an item (i.e. a button) to the layout.
     *
     * @param text The R.string id to be displayed on the new item when it is expanded.
     * @param drawable The R.drawable id that will
     * @return 'this'. Useful for chaining together with other functions
     */
    public AbbrevLinearLayout addItem(int text, int drawable) {
        return addItem(text, drawable, null, STATE.BOTH);
    }

    /**
     * Adds an item (i.e. a button) to the layout.
     *
     * @param text The R.string id to be displayed on the new item when it is expanded.
     * @param drawable The R.drawable id that will be on the item.
     * @param click The OnClickListener for the item.
     * @return 'this'. Useful for chaining together with other functions
     */
    public AbbrevLinearLayout addItem(Integer text, Integer drawable, OnClickListener click) {
        return addItem(text, drawable, click, STATE.BOTH);
    }

    /**
     * Adds an item (i.e. a button) to the layout.
     *
     * @param text The R.string id to be displayed on the new item when it is expanded.
     * @param drawable The R.drawable id that will be on the item.
     * @param showState The STATE to be show this item. It will be hidden otherwise.
     * @return 'this'. Useful for chaining together with other functions
     */
    public AbbrevLinearLayout addItem(Integer text, Integer drawable, STATE showState) {
        return addItem(text, drawable, null, showState);
    }

    /**
     * Adds an item (i.e. a button) to the layout.
     *
     * @param text The R.string id to be displayed on the new item when it is expanded.
     * @param drawable The R.drawable id that will be on the item.
     * @param click The OnClickListener for the item.
     * @param showState The STATE to be show this item. It will be hidden otherwise.
     * @return 'this'. Useful for chaining together with other functions
     */
    public AbbrevLinearLayout addItem(Integer text, Integer drawable, OnClickListener click, STATE showState) {

        if(currentState!=STATE.EXPANDED && currentState!=STATE.COLLAPSED)
            throw new RuntimeException();

        Item i = new Item(getContext());
        i.setData(text, drawable, showState);
        i.setOnClickListener(click);

        addView(i);

        return this;
    }

    /////////////// Toggle State ///////////////

    /**
     * Expands the Layout to be vertical and all of its children to show their text
     */
    public void expand() {
        currentState = STATE.EXPANDED;
        setOrientation(LinearLayout.VERTICAL);
        refreshChildren();
    }

    /**
     * Collapsed the Layout to be horizontal and all of its children to hide their text
     */
    public void collapse() {
        currentState = STATE.COLLAPSED;
        setOrientation(LinearLayout.HORIZONTAL);
        refreshChildren();
    }

    /////////////// Internal Functions ///////////////

    /**
     * Refresh ALL the children!
     */
    private void refreshChildren() {
        for(int i=0; i<getChildCount(); ++i)
            if(currentState==STATE.EXPANDED)
                ((Item)getChildAt(i)).expand();
            else if(currentState==STATE.COLLAPSED)
                ((Item)getChildAt(i)).collapse();
    }



    /**
     * This is an internal class. It's essentially a button with an icon and text.
     *
     * @author      Jon Petraglia <notbryant@gmail.com>
     */
    class Item extends LinearLayout {
        STATE showInState;
        TextView text;
        ImageView image;

        /**
         * Default constructor needed by Android
         * (I include all 3 just for safety's sake.)
         *
         * @param context The Context
         */
        public Item(Context context) {
            super(context);
            init();
        }

        /**
         * Default constructor needed by Android
         * (I include all 3 just for safety's sake.)
         *
         * @param context The Context
         * @param attrs The Attributes
         */
        public Item(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        /**
         * Default constructor needed by Android
         * (I include all 3 just for safety's sake.)
         *
         * @param context The Context
         * @param attrs The Attributes specified in XML
         * @param defStyle The style specified in the XML
         */
        public Item(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            init();
        }

        /**
         * Internal function to instantiate class.
         */
        void init() {
            setOrientation(LinearLayout.HORIZONTAL);
            setClickable(true);
            setBackgroundResource(itemBgResource);
            setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            setPadding(20,20,20,20);
            setGravity(Gravity.CENTER);
            invalidate();
        }

        /**
         * Sets the data for this, that is, actually adds the text & image & sets the state in which show the widget.
         *
         * @param textResId The R.layout id for the text portion. MUST contain only a TextView.
         * @param drawableResId The R.drawable id for the image.
         * @param stateShown The STATE to show this widget for.
         */
        public void setData(Integer textResId, Integer drawableResId, STATE stateShown) {
            showInState = stateShown;

            if(drawableResId!=null) {
                image = new ImageView(getContext());
                image.setImageResource(drawableResId);
                addView(image);
            }

            if(textResId!=null) {
                text = (TextView) LayoutInflater.from(getContext()).inflate(textResource, null);
                text.setText(textResId);
                addView(text);
                if(currentState==STATE.COLLAPSED)
                    text.setVisibility(View.GONE);
            }

            if(currentState==STATE.EXPANDED)
                expand();
            else if(currentState==STATE.COLLAPSED)
                collapse();
            else
                throw new RuntimeException("Invalid current state for Item");
        }

        /**
         * Shows the text or possibly hides the widget depending on Item settings.
         */
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

        /**
         * Hides the text or possibly hides the widget depending on Item settings.
         */
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
