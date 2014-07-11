package com.qweex.callisto.catalog;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;

import java.util.ArrayList;

/** An adapter for the Nav spinner in the ActionBar
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
class ShowListAdapter extends BaseAdapter {

    /** Show Info to fetch info from. */
    ArrayList<ShowInfo> array;
    /** Reference to MasterActivity */
    private MasterActivity activity;

    /** Constructor
     * @param master Reference to MasterActivity.
     * @param a The data.
     */
    public ShowListAdapter(MasterActivity master, ArrayList<ShowInfo> a) {
        activity = master;
        array = a;
    }

    /** Inherited method; gets number of items.
     * @return Number of items.
     */
    @Override
    public int getCount() {
        return array.size();
    }

    /** Inherited method; gets an item.
     * @return Item. (ShowInfo)
     */
    @Override
    public Object getItem(int position) {
        return array.get(position);
    }

    /** [not used] */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /** Inherited method; Called to retrieve the view for the dropdown, i.e. popup menu.
     * @param position Position of item to get.
     * @param convertView View to be recycled, if any.
     * @param parent Parent that the view should have.
     * @return The View with all data in it.
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return _getView(R.layout.nav_entry, position, convertView, parent);
    }

    /** Inherited method; Called to retrieve the view for the spinner, i.e. the one shown.
     * @param position Position of item to get.
     * @param convertView View to be recycled, if any.
     * @param parent Parent that the view should have.
     * @return The View with all data in it.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return _getView(android.R.layout.simple_spinner_item, position, convertView, parent);
    }

    ////////////////// Private methods //////////////////

    /** Called by both 'getView' and 'getDropDownView' cause they do the same thing, just with different layouts.
     * @param resid R.layout id for items.
     * @param position Position to get.
     * @param convertView View to be recycled, if any.
     * @param parent Parent that the view should have.
     * @return The View with all data in it.
     */
    private View _getView(int resid, int position, View convertView, ViewGroup parent) {
        if(convertView==null)
            convertView = activity.getLayoutInflater().inflate(resid, null);

        ((TextView)convertView).setText( array.get(position).title );
        return convertView;
    }
}
