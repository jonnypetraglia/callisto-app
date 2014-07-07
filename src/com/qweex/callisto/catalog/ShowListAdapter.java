package com.qweex.callisto.catalog;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.qweex.callisto.R;

import java.util.ArrayList;

class ShowListAdapter extends BaseAdapter {
    ArrayList<ShowInfo> array;
    private CatalogFragment catalogFragment;

    public ShowListAdapter(CatalogFragment catalogFragment, ArrayList<ShowInfo> a) {
        this.catalogFragment = catalogFragment;
        array = a;
    }

    @Override
    public int getCount() {
        return array.size();
    }

    @Override
    public Object getItem(int position) {
        return array.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private View _getView(int resid, int position, View convertView, ViewGroup parent) {
        if(convertView==null)
            convertView = catalogFragment.getActivity().getLayoutInflater().inflate(resid, null);

        ((TextView)convertView).setText( array.get(position).title );
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return _getView(R.layout.nav_entry, position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return _getView(android.R.layout.simple_spinner_item, position, convertView, parent);
    }
}
