package com.qweex.callisto.catalog;

import android.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ListView;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.DatabaseConnector;
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
    DatabaseMate dbMate;
    RssUpdater rssUpdater;

    public CatalogFragment(DatabaseConnector db) {
        super(db);
        dbMate = new DatabaseMate(db);
    }

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
                getActivity().getActionBar().setListNavigationCallbacks(showListAdapter, changeShow);
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
                    rssUpdater = new RssUpdater(dbMate);
                    rssUpdater.execute(getSelectedShow());
                }
                return true;
            case R.id.update_all:
                if(rssUpdater!=null && rssUpdater.isRunning())
                    rssUpdater.addItems(showList);
                else {
                    rssUpdater = new RssUpdater(dbMate);
                    rssUpdater.execute((ShowInfo[]) showList.toArray());
                }
        }
        return super.onOptionsItemSelected(item);
    }

    ShowInfo getSelectedShow() {
        int i = getActivity().getActionBar().getSelectedNavigationIndex();
        Log.i("Callisto", "Updating show " + i);
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
        try {
            getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            getActivity().getActionBar().setTitle(null);
        } catch(NullPointerException npe) {
            Log.w("Callisto", "Encountered null while trying to perform show()");
        }
    }

    public void hide() {
        getActivity().getActionBar().setListNavigationCallbacks(null, null);
    }
}
