package com.qweex.callisto.catalog;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class CatalogFragment extends CallistoFragment {

    Spinner catalogSpinner;
    ArrayList<ShowInfo> showList;
    int selectedShow;

    ListView listview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        listview = new ListView(getActivity());

        //Cursor r = StaticBlob.databaseConnector.getShow(currentShow, filter);
        listview.setAdapter(new CatalogAdapter(getActivity(), R.layout.catalog_row, null));

        catalogSpinner = (Spinner) getActivity().findViewById(R.id.nav_spinner);

        InputStream is = null;
        try {
            is = getActivity().getAssets().open("shows.min.json");
            showList = ShowInfo.readJSON(is);
            catalogSpinner.setAdapter(new ShowListAdapter(this, showList));
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
