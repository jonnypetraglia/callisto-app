package com.qweex.callisto.catalog;

import android.app.ActionBar;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class CatalogFragment extends CallistoFragment {

    public static SimpleDateFormat sdfRaw = new SimpleDateFormat("yyyyMMddHHmmss");

    ShowListAdapter showListAdapter;
    ArrayList<ShowInfo> showList;
    LinearLayout layout;
    ListView listview;
    DatabaseMate dbMate;
    RssUpdater rssUpdater;
    boolean filter = false;

    public CatalogFragment(MasterActivity m) {
        super(m);
        dbMate = new DatabaseMate(m.databaseConnector);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        if(layout==null) {
            layout = (LinearLayout) inflater.inflate(R.layout.catalog, null);
            listview = (ListView) layout.findViewById(android.R.id.list);
            listview.setEmptyView(layout.findViewById(android.R.id.empty));
            listview.setDivider(new ColorDrawable(0xff999999));
            listview.setDividerHeight(1);
            listview.setOnItemClickListener(selectEpisode);

            InputStream is = null;
            try {
                is = getActivity().getAssets().open("shows.min.json");
                showList = ShowInfo.readJSON(is);
                showListAdapter = new ShowListAdapter(master, showList);
                master.getSupportActionBar().setListNavigationCallbacks(showListAdapter, changeShow);

                reloadList();
            } catch (IOException e) {
                e.printStackTrace();
            }

            setHasOptionsMenu(true);
        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        show();
        return layout;
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
                    rssUpdater = new RssUpdater(dbMate, processRssResults);
                    rssUpdater.execute(getSelectedShow());
                }
                return true;
            case R.id.update_all:
                if(rssUpdater!=null && rssUpdater.isRunning())
                    rssUpdater.addItems(showList);
                else {
                    rssUpdater = new RssUpdater(dbMate, processRssResults);
                    rssUpdater.execute(showList.toArray(new ShowInfo[showList.size()]));
                }
                return true;
            case R.id.filter :
                filter = !filter;
                if(filter)
                    item.setTitle(R.string.show_watched);
                else
                    item.setTitle(R.string.hide_watched);
                reloadList();
                return true;
            case R.id.mark:
                //TODO
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    ShowInfo getSelectedShow() {
        int i = master.getSupportActionBar().getSelectedNavigationIndex();
        Log.i("Callisto", "Updating show " + i);
        return showList.get(i);
    }


    private android.support.v7.app.ActionBar.OnNavigationListener changeShow = new android.support.v7.app.ActionBar.OnNavigationListener() {
        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            Log.d("Callisto", "Selected Show: " + showList.get(itemPosition).id);
            reloadList();
            return true;
        }
    };


    RssUpdater.Callback processRssResults = new RssUpdater.Callback() {
        @Override
        void call(LinkedList<Episode> results, LinkedList<ShowInfo> failures) {
            while(!results.isEmpty())
                dbMate.insertEpisode(results.pop());
            if(failures.size()==0)
                return;
            String[] errorStrings = new String[failures.size()];
            for(int i=0; i<failures.size(); ++i) {
                errorStrings[i] = failures.get(i).title;
            }
            String msg = "Errors Occured with:" + TextUtils.join("\n", errorStrings);
            Log.e("Callisto", msg);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG);
        }
    };

    AdapterView.OnItemClickListener selectEpisode = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Long _id = (Long) view.getTag();

            EpisodeFragment frag = new EpisodeFragment(master, dbMate.getOneEpisode(_id));

            FragmentTransaction transaction = master.getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_fragment, frag);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    };

    void reloadList() {
        ShowInfo selectedShow = showList.get(Math.max(master.getSupportActionBar().getSelectedNavigationIndex(), 0));
        Cursor r = dbMate.getShow(selectedShow.id, filter);
        listview.setAdapter(new CatalogAdapter(getActivity(), R.layout.catalog_row, r));
    }

    public void show() {
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        master.getSupportActionBar().setTitle(null);
        master.getSupportActionBar().setSubtitle(null);
    }

    public void hide() {
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }
}
