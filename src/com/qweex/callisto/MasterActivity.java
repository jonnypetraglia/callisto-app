package com.qweex.callisto;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.*;
import com.qweex.callisto.catalog.CatalogFragment;
import com.qweex.callisto.contact.ContactFragment;
import com.qweex.callisto.settings.SettingsFragment;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

public class MasterActivity extends ActionBarActivity {

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private TextView navSelected;

    private CallistoFragment activeFragment;


    private CatalogFragment catalogFragment;
    //private LiveFragment liveFragment;
    //private ChatFragment chatFragment;
    //private ScheduleFragment scheduleFragment;
    private ContactFragment contactFragment;
    //private DonateFragment donateFragment;
    private SettingsFragment settingsFragment;

    public DatabaseConnector databaseConnector;

    ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String[] navbarEntries = getResources().getStringArray(R.array.navigation_drawer_items_array);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.nav_entry, navbarEntries));
        drawerList.setOnItemClickListener(navClickListener);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.actionbar_drawer, R.string.app_name,R.string.update);
        drawerLayout.setDrawerListener(drawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        databaseConnector = new DatabaseConnector(this);

        catalogFragment = new CatalogFragment(databaseConnector);
        //liveFragment = new LiveFragment(databaseConnector);
        //chatFragment = new ChatFragment(databaseConnector);
        //scheduleFragment = new ScheduleFragment(databaseConnector);
        contactFragment = new ContactFragment(databaseConnector);
        //donateFragment = new DonateFragment(databaseConnector);
        settingsFragment = new SettingsFragment(databaseConnector);

        checkForUpdates();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    private void checkForCrashes() {
        CrashManager.register(this, PRIVATE.HOCKEY_APP_ID);
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this, PRIVATE.HOCKEY_APP_ID);
    }

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
                    frag = settingsFragment;
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
            getActionBar().setTitle(navSelected.getText());


            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_fragment, frag);
            //transaction.addToBackStack(null);
            transaction.commit();

            if(activeFragment!=null)
                activeFragment.hide();
            activeFragment = frag;
            activeFragment.show();

            drawerLayout.closeDrawer(GravityCompat.START);
        }
    };


    public static boolean hasMenuKey(Activity a) {
        return Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 &&
                ViewConfiguration.get(a).hasPermanentMenuKey());
    }
}
