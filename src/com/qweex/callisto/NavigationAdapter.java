package com.qweex.callisto;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/** An adapter for the Navigation menu in the Drawer.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class NavigationAdapter extends ArrayAdapter<String> {
    /** Reference to activity, for getting the layout inflater. */
    Activity activity;
    /** R.layout id for the layout. Needs to be a textview. */
    int resource;
    /** Text for the menu items. */
    String[] text;
    /** Icons for the menu items. */
    Drawable[] icons;

    /** Constructor.
     * @param activity Reference to activity, for getting the layout inflater.
     * @param resource R.layout id for the layout. Needs to be a textview.
     * @param text Text for the menu items.
     * @param icons Icons for the menu items.
     */
    public NavigationAdapter(Activity activity, int resource, String[] text, Drawable[] icons) {
        super(activity, resource, text);
        this.activity = activity;
        this.resource = resource;
        this.text = text;
        this.icons = icons;
    }

    /** Inherited method; creates or modifies view per item's data.
     * @param position Position of item to get.
     * @param convertView View to be recycled, if any.
     * @param parent Parent that the view should have.
     * @return The View with all data in it.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null)
            convertView = activity.getLayoutInflater().inflate(resource, null);

        TextView convertViewAsTV = (TextView) convertView;
        convertViewAsTV.setText(text[position]);
        convertViewAsTV.setCompoundDrawablesWithIntrinsicBounds(icons[position], null, null, null);
        convertViewAsTV.setCompoundDrawablePadding(10);

        return convertView;
    }
}
