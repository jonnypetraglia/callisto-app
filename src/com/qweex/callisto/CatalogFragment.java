package com.qweex.callisto;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class CatalogFragment extends CallistoFragment {

    View contentView;
    Spinner catalogSpinner;
    ArrayList<ShowInfo> showList;
    int selectedShow;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        contentView = super.onCreateView(inflater, container, savedInstanceState);

        catalogSpinner = (Spinner) getActivity().findViewById(R.id.nav_spinner);

        InputStream is = null;
        try {
            is = getActivity().getAssets().open("shows.json");
            showList = ShowInfo.readJSON(is);
            catalogSpinner.setAdapter(new ShowAdapter(showList));
            catalogSpinner.setOnItemSelectedListener(changeShow);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextView t = new TextView(getActivity());
        t.setText("HEY");
        show();
        return t;
        //return contentView;
    }

    class ShowAdapter extends BaseAdapter {
        ArrayList<ShowInfo> array;

        public ShowAdapter(ArrayList<ShowInfo> a) {
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
                convertView = getActivity().getLayoutInflater().inflate(resid, null);

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


    private AdapterView.OnItemSelectedListener changeShow = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.d("Callisto", "Selected Show: " + showList.get(position).title);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            Log.d("Callisto", "Nothing Selected");
        }
    };

    public void show() {
        if(catalogSpinner==null)
            return;
        catalogSpinner.setSelection(selectedShow);
        catalogSpinner.setVisibility(View.VISIBLE);
    }

    public void hide() {
        if(catalogSpinner==null)
            return;
        catalogSpinner.setVisibility(View.GONE);
    }
}
