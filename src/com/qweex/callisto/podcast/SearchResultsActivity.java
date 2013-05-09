/*
 * Copyright (C) 2012-2013 Qweex
 * This file is a part of Callisto.
 *
 * Callisto is free software; it is released under the
 * Open Software License v3.0 without warranty. The OSL is an OSI approved,
 * copyleft license, meaning you are free to redistribute
 * the source code under the terms of the OSL.
 *
 * You should have received a copy of the Open Software License
 * along with Callisto; If not, see <http://rosenlaw.com/OSL3.0-explained.htm>
 * or check OSI's website at <http://opensource.org/licenses/OSL-3.0>.
 */
package com.qweex.callisto.podcast;

import com.qweex.callisto.R;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import com.qweex.callisto.StaticBlob;

/** Shows the results of a list for episodes. Can be done for a specific show or all shows. */

public class SearchResultsActivity extends ListActivity
{
    /** The show that will be searched; is optional, null searches all shows */
    public static String searchShow;

    /** Called when the activity is first created. Creates all the crap, man.
     * @param savedInstanceState Um I don't even know. Read the Android documentation.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
        TextView empty = new TextView(this);
        empty.setText("No results found.");
        this.getListView().setEmptyView(empty);
        this.getListView().setBackgroundColor(this.getResources().getColor(R.color.backClr));
        this.getListView().setCacheColorHint(this.getResources().getColor(R.color.backClr));
    }

    /** Called when a search is requested.
     * @return true if the search was handled, false otherwise
     */
    @Override
    public boolean onSearchRequested ()
    {
        return false;
    }

    /** Um, called when a new intent is uh...new... */
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /** Actually performs the search and displays it.
     * @param intent Basically the intent of the current SearchResultsActivity
     */
    private void handleIntent(Intent intent) {

        if(Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);
            System.out.println(query);

            String[] from = {"title", "_id", "show"};
            int[] to = {R.id.text1, R.id.id1, R.id.uri};
            Cursor c = StaticBlob.databaseConnector.searchEpisodes(query, searchShow);
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.simple_spinner_item_plus_vis, c, from, to );
            this.setListAdapter(adapter);
        }
    }

    /** Called when a user presses a search result; open's the episode's page; that is an EpisodeDesc */
    @Override
    public void onListItemClick(ListView l, View v, int position, long useless_id)
    {
        Intent intent = new Intent(this, EpisodeDesc.class);
        long actual_id = Long.parseLong(((TextView)v.findViewById(R.id.id1)).getText().toString());
        intent.putExtra("id", actual_id);
        startActivity(intent);
        //finish();
    }
}