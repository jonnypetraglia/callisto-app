package com.qweex.callisto.catalog;

import android.app.ActionBar;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** This fragment displays past episodes for a show in the catalog.
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
    /** The view to show when the feed is reloading */
    LinearLayout headerView;

    /** The class to make database connections easier concerning episodes. */
    DatabaseMate dbMate;
    /** The class to fetch new data from the RSS feeds inside showList. */
    RssUpdater rssUpdater;

    /** Whether or showing only new episodes is enabled. */
    boolean filter = false;

    /** Holds a list of error causes per feed to be displayed in the header. */
    HashMap<ShowInfo, String> updateErrors = new HashMap<ShowInfo, String>();

    /** Inherited constructor.
     * @param master Reference to MasterActivity.
     */
    public CatalogFragment(MasterActivity master) {
        super(master);
        dbMate = new DatabaseMate(master.databaseConnector);
    }

    /** Inherited method; called each time the fragment is attached to a FragmentActivity.
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

            headerView = (LinearLayout) layout.findViewById(R.id.header);

            InputStream is = null;
            try {
                is = getActivity().getAssets().open("shows.min.json");
                showList = ShowInfo.readJSON(is);
                showListAdapter = new ShowListAdapter(master, showList);
                master.getSupportActionBar().setListNavigationCallbacks(showListAdapter, changeShow);

                updateListAndHeader.run();
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

    /** Inherited method; called automatically when the options menu is created.
     * @param menu The menu object you can add items to.
     * @param inflater The inflater that can be used for XML menus.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.catalog_menu, menu);
    }

    /** Inherited method; called when the user selects an item from the options menu.
     * @param item The menu item selected.
     * @return Whether or not the event should be passed on to the next handler.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.update:   //Refresh the SQL database from the RSS feed
                    dbMate.clearShow(getSelectedShow());
                    if(rssUpdater!=null && rssUpdater.isRunning())
                        rssUpdater.addItem(getSelectedShow());
                    else {
                        rssUpdater = new RssUpdater(getSelectedShow(), processRssResults);
                        rssUpdater.executePlz();
                    }
                    updateHeader();
                    return true;
                case R.id.update_all:
                    dbMate.clearAllShows();
                    if(rssUpdater!=null && rssUpdater.isRunning())
                        rssUpdater.addItems(showList);
                    else {
                        rssUpdater = new RssUpdater(showList, processRssResults);
                        rssUpdater.executePlz();
                    }
                    updateHeader();
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
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        return super.onOptionsItemSelected(item);
    }

    /** Gets the selected show from the Nav in the ActionBar.
     * @return The selected show.
     */
    ShowInfo getSelectedShow() {
        int i = master.getSupportActionBar().getSelectedNavigationIndex();
        return showList.get(i);
    }

    /** Called when the user changes the show in the ActionBar nav spinner. */
    private android.support.v7.app.ActionBar.OnNavigationListener changeShow = new android.support.v7.app.ActionBar.OnNavigationListener() {
        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            ShowInfo selectedShow = showList.get(itemPosition);
            Log.d(TAG, "Selected Show: " + selectedShow.title);
            updateListAndHeader.run();
            return true;
        }
    };

    /** The callback method for when the RssUpdater has finished. */
    RssUpdater.Callback processRssResults = new RssUpdater.Callback() {
        @Override
        void call(ShowInfo show, final LinkedList<Episode> results, String error) {
            if(results==null || error!=null) {
            // Errors occurred
                updateErrors.put(show, error);
                updateHeader();
            } else {
            // No errors
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dbMate.insertEpisodes(results);
                        CatalogFragment.this.master.runOnUiThread(updateListAndHeader);
                    }
                }).start();
            }
        }
    };

    Runnable updateListAndHeader = new Runnable() {
        @Override
        public void run() {
            reloadList();
            updateHeader();
        }
    };

    /** Reloads the list of episodes for the currently selected show. */
    void reloadList() {
        ShowInfo selectedShow = showList.get(Math.max(master.getSupportActionBar().getSelectedNavigationIndex(), 0));
        Cursor r = dbMate.getShow(selectedShow, filter);
        CatalogAdapter catalogAdapter = new CatalogAdapter(getActivity(), R.layout.catalog_row, r);
        listview.setAdapter(catalogAdapter);
    }

    /** Updates the header without force showing it. */
    void updateHeader() {
        updateHeader(false);
    }

    /** Updates the header based on if the current show is running or has an error.
     * @param forceUpdating Force "Updating" to be shown.
     */
    void updateHeader(boolean forceUpdating) {
        ShowInfo selectedShow = showList.get(Math.max(master.getSupportActionBar().getSelectedNavigationIndex(), 0));

        Log.d(TAG, "Updating the header");

        if(forceUpdating || (rssUpdater!=null && rssUpdater.isUpdating(selectedShow))) {
            Log.d(TAG, "Updating the header: Showing");
            headerView.setVisibility(View.VISIBLE);
            headerView.findViewById(R.id.textView).setVisibility(View.VISIBLE);
            headerView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            headerView.findViewById(R.id.error).setVisibility(View.GONE);

            ((TextView)headerView.findViewById(R.id.textView)).setText(R.string.reloading);
        } else if(updateErrors.containsKey(selectedShow)) {
            Log.d(TAG, "Updating the header: Error");
            headerView.setVisibility(View.VISIBLE);
            headerView.findViewById(R.id.textView).setVisibility(View.VISIBLE);
            headerView.findViewById(R.id.progressBar).setVisibility(View.GONE);
            headerView.findViewById(R.id.error).setVisibility(View.VISIBLE);

            ((TextView)headerView.findViewById(R.id.textView)).setText(updateErrors.get(selectedShow));
        } else {
            Log.d(TAG, "Updating the header: Hiding");
            headerView.setVisibility(View.GONE);
        }
    }


    /** Called when the user selects an episode. Create an EpisodeFragment. */
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

    /** Inherited method; things to do when the fragment is shown initially. */
    @Override
    public void show() {
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        master.getSupportActionBar().setTitle(null);
        master.getSupportActionBar().setSubtitle(null);
    }

    /** Inherited method; things to do when the fragment is hidden/dismissed. */
    @Override
    public void hide() {
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }
}
