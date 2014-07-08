package com.qweex.callisto.catalog;

import android.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ListView;
import android.widget.Toast;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.R;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class CatalogFragment extends CallistoFragment {

    public static SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss");

    ShowListAdapter showListAdapter;
    ArrayList<ShowInfo> showList;
    ListView listview;
    RssUpdater rssUpdater;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        if(listview==null) {
            listview = new ListView(getActivity());

            //Cursor r = StaticBlob.databaseConnector.getShow(currentShow, filter);
            listview.setAdapter(new CatalogAdapter(getActivity(), R.layout.catalog_row, null));

            //catalogSpinner = (Spinner) getActivity().findViewById(R.id.nav_spinner);

            InputStream is = null;
            try {
                is = getActivity().getAssets().open("shows.min.json");
                showList = ShowInfo.readJSON(is);
                showListAdapter = new ShowListAdapter(this, showList);
            } catch (IOException e) {
                e.printStackTrace();
            }

            show();
            setHasOptionsMenu(true);
        }
        return listview;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.catalog_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.update:   //Refresh the SQL database from the RSS feed
                if(rssUpdater!=null && rssUpdater.isRunning())
                    rssUpdater.addItem(getSelectedShow());
                else {
                    rssUpdater = new RssUpdater(null);
                    rssUpdater.execute(getSelectedShow());
                }
                return true;
            case R.id.update_all:
                if(rssUpdater!=null && rssUpdater.isRunning())
                    rssUpdater.addItems(showList);
                else {
                    rssUpdater = new RssUpdater(null);
                    rssUpdater.execute((ShowInfo[]) showList.toArray());
                }
        }
        return super.onOptionsItemSelected(item);
    }

    ShowInfo getSelectedShow() {
        int i = getActivity().getActionBar().getSelectedNavigationIndex();
        return showList.get(i);
    }


    private ActionBar.OnNavigationListener changeShow = new ActionBar.OnNavigationListener() {
        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            Log.d("Callisto", "Selected Show: " + showList.get(itemPosition).title);
            return true;
        }
    };

    public void show() {
        if(showListAdapter!=null)
            getActivity().getActionBar().setListNavigationCallbacks(showListAdapter, changeShow);
    }

    public void hide() {
        getActivity().getActionBar().setListNavigationCallbacks(null, null);
    }
}
