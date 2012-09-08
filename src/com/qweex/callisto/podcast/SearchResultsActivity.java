/*
Copyright (C) 2012 Qweex
This file is a part of Callisto.

Callisto is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

Callisto is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Callisto; If not, see <http://www.gnu.org/licenses/>.
*/
package com.qweex.callisto.podcast;

import com.qweex.callisto.Callisto;
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

public class SearchResultsActivity extends ListActivity
{
	
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
        this.getListView().setBackgroundColor(Callisto.RESOURCES.getColor(R.color.backClr));
        this.getListView().setCacheColorHint(Callisto.RESOURCES.getColor(R.color.backClr));
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

        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);
            System.out.println(query);
            
            String[] from = {"title", "_id", "show"};
            int[] to = {R.id.text1, R.id.id1, R.id.uri};
            Cursor c = Callisto.databaseConnector.searchEpisodes(query, searchShow);
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.simple_spinner_item_plus_vis, c, from, to );
            this.setListAdapter(adapter);
        }
    }
    
    /** Called when a user presses a search result; open's the episode's page. */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		Intent intent = new Intent(this, EpisodeDesc.class);
		long id1 = Long.parseLong(((TextView)v.findViewById(R.id.id1)).getText().toString());
		intent.putExtra("id", id1);
		startActivity(intent);
		//finish();
	}
}