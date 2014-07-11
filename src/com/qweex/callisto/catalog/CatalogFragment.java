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

/**
 * This fragment displays past episodes for a show in the catalog.
 *
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class CatalogFragment extends CallistoFragment {

    String TAG = "Callisto:CatalogFragment";

    /** Adapter used for Nav spinner in the ActionBar. */
    ShowListAdapter showListAdapter;
    /** The show list as read from JSON; used in showListAdapter & rssUpdater. */
    ArrayList<ShowInfo> showList;

    /** The layout for this fragment. */
    LinearLayout layout;
    /** The ListView for this fragment */
    ListView listview;

    /** The class to make database connections easier concerning episodes. */
    DatabaseMate dbMate;
    /** The class to fetch new data from the RSS feeds inside showList. */
    RssUpdater rssUpdater;

    /** Whether or showing only new episodes is enabled. */
    boolean filter = false;

    /**
     * Inherited constructor.
     *
     * @param master Reference to MasterActivity.
     */
    public CatalogFragment(MasterActivity master) {
        super(master);
        dbMate = new DatabaseMate(master.databaseConnector);
    }

    /**
     * Inherited method; called each time the fragment is attached to a FragmentActivity.
     *
     * @param inflater Used for instantiating the fragment's view.
     * @param container [ASK_SOMEONE_SMARTER]
     * @param savedInstanceState [ASK_SOMEONE_SMARTER]
     * @return The new / recycled View to be attached.
     */
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

    /**
     * Inherited method; called automatically when the options menu is created.
     *
     * @param menu The menu object you can add items to.
     * @param inflater The inflater that can be used for XML menus.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.catalog_menu, menu);
    }

    /**
     * Inherited method; called when the user selects an item from the options menu.
     *
     * @param item The menu item selected.
     * @return Whether or not the event should be passed on to the next handler.
     */
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

    /**
     * Gets the selected show from the Nav in the ActionBar.
     *
     * @return The selected show.
     */
    ShowInfo getSelectedShow() {
        int i = master.getSupportActionBar().getSelectedNavigationIndex();
        return showList.get(i);
    }

    /**
     * Called when the user changes the show in the ActionBar nav spinner.
     */
    private android.support.v7.app.ActionBar.OnNavigationListener changeShow = new android.support.v7.app.ActionBar.OnNavigationListener() {
        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            Log.d(TAG, "Selected Show: " + showList.get(itemPosition).title);
            reloadList();
            return true;
        }
    };

    /**
     * The callback method for when the RssUpdater has finished.
     */
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
            Log.e(TAG, msg);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG);
        }
    };

    /**
     * Called when the user selects an episode.
     */
    AdapterView.OnItemClickListener selectEpisode = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Long _id = (Long) view.getTag();

            EpisodeFragment frag = new EpisodeFragment(master, CatalogFragment.this, _id);

            FragmentTransaction transaction = master.getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_fragment, frag);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    };

    /**
     * Reloads the list of episodes for the currently selected show.
     */
    void reloadList() {
        ShowInfo selectedShow = showList.get(Math.max(master.getSupportActionBar().getSelectedNavigationIndex(), 0));
        Cursor r = dbMate.getShow(selectedShow.title, filter);
        listview.setAdapter(new CatalogAdapter(getActivity(), R.layout.catalog_row, r));
    }

    /**
     * Inherited method; things to do when the fragment is shown initially.
     */
    public void show() {
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        master.getSupportActionBar().setTitle(null);
        master.getSupportActionBar().setSubtitle(null);
    }

    /**
     * Inherited method; things to do when the fragment is hidden/dismissed.
     */
    public void hide() {
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }
}
