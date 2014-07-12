package com.qweex.callisto;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.qweex.callisto.catalog.CatalogFragment;
import com.qweex.callisto.contact.ContactFragment;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.util.List;

/** Main activity for the application.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class MasterActivity extends ActionBarActivity {

    String TAG = "Callisto:MainActivity";

    /** ActionBar/Drawer variables. */
    public DrawerLayout drawerLayout;
    private ListView drawerList;
    public ActionBarDrawerToggle drawerToggle;
    private TextView navSelected;

    /** Fragments of the various types. Only 1 exists at a time. */
    private CatalogFragment catalogFragment;
    //private LiveFragment liveFragment;
    //private ChatFragment chatFragment;
    //private ScheduleFragment scheduleFragment;
    private ContactFragment contactFragment;
    //private DonateFragment donateFragment;
    //private SettingsFragment settingsFragment;

    /** Reference to the Active fragment. */
    private CallistoFragment activeFragment;

    /** Database class. */
    public DatabaseConnector databaseConnector;


    /** Inherited; called when the activity is created.
     * @param savedInstanceState [ASK_SOMEONE_SMARTER]
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String[] navbarTexts = getResources().getStringArray(R.array.navigation_drawer_items_array);
        TypedArray iconStrings = getResources().obtainTypedArray(R.array.navigation_drawer_icons_array);
        Drawable[] navbarIcons = new Drawable[iconStrings.length()];
        Resources resources = getResources();
        for(int i=0; i<iconStrings.length(); ++i)
            navbarIcons[i] = resources.getDrawable(iconStrings.getResourceId(i, -1));


        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerList.setAdapter(new NavigationAdapter(this, R.layout.nav_entry, navbarTexts, navbarIcons));
        drawerList.setOnItemClickListener(navClickListener);


        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.actionbar_drawer, R.string.app_name, R.string.update);
        drawerLayout.setDrawerListener(drawerToggle);


        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        databaseConnector = new DatabaseConnector(this);

        catalogFragment = new CatalogFragment(this);
        //liveFragment = new LiveFragment(databaseConnector);
        //chatFragment = new ChatFragment(databaseConnector);
        //scheduleFragment = new ScheduleFragment(databaseConnector);
        contactFragment = new ContactFragment(this);
        //donateFragment = new DonateFragment(databaseConnector);
        //settingsFragment = new SettingsFragment(this);

        checkForUpdates();
        checkForCrashes();
    }

    /** Inherited. [ASK_SOMEONE_SMARTER]
     * Used to ensure the drawer/up icon is correct in the ActionBar.
     * @param savedInstanceState [ASK_SOMEONE_SMARTER]
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }


    /** Inherited; called when a menu item is selected (Including the ActionBar home button.)
     * See: http://stackoverflow.com/questions/17258020/switching-between-android-navigation-drawer-image-and-up-caret-when-using-fragme
     *
     * @param item The item that was selected
     * @return If the event was handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.isDrawerIndicatorEnabled() && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            getCurrentFragment().hide();
            return getSupportFragmentManager().popBackStackImmediate();
        }

        return super.onOptionsItemSelected(item);
    }

    /** Get the current fragment that is in use.
     * @return The current fragment that is in use.
     */
    public CallistoFragment getCurrentFragment(){

        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        return (CallistoFragment) fragments.get(fragments.size()-1);
    }


    /** Used by HockeyApp */
    private void checkForCrashes() {
        CrashManager.register(this, PRIVATE.HOCKEY_APP_ID);
    }

    /** Used by HockeyApp */
    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this, PRIVATE.HOCKEY_APP_ID);
    }

    /** Called when an item is selected from the Nav spinner in the ActionBar. */
    AdapterView.OnItemClickListener navClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            CallistoFragment frag = null;
            switch (position+1) {
                case 1: //Catalog
                    frag = catalogFragment;
                    break;
                case 2: //Live
                    //frag = liveFragment
                    break;
                case 3: //Chat
                    //frag = chatFragment;
                    break;
                case 4: //Schedule
                    //frag = scheduleFragment;
                    break;
                case 5: //Contact
                    frag = contactFragment;
                    break;
                case 6: //Donate
                    //frag = donateFragment;
                    break;
                case 7: //Settings
                    //frag = settingsFragment;
                    break;
                case 8: //About
                    break;
                default:
                    //TODO: WTF
                    return;
            }
            if(frag==null) //Unnecessary safety
                return;

            if(navSelected!=null)
                navSelected.setSelected(false);
            navSelected = (TextView) view;
            navSelected.setSelected(true);


            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_fragment, frag);
            transaction.commit();

            if(activeFragment!=null)
                activeFragment.hide();
            activeFragment = frag;
            try {
                activeFragment.show();
            } catch(NullPointerException npe) {
                Log.w("Callisto", "Encountered null while trying to perform show()");
            }

            drawerLayout.closeDrawer(GravityCompat.START);
        }
    };
}
