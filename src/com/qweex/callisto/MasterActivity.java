package com.qweex.callisto;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.*;
import com.qweex.callisto.catalog.CatalogFragment;
import com.qweex.callisto.contact.ContactFragment;
import com.qweex.callisto.settings.SettingsFragment;

public class MasterActivity extends FragmentActivity {

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private LinearLayout navButton;
    private TextView navSelected, titlebarText;

    private CallistoFragment activeFragment;


    private CatalogFragment catalogFragment;
    //private LiveFragment liveFragment;
    //private ChatFragment chatFragment;
    //private ScheduleFragment scheduleFragment;
    private ContactFragment contactFragment;
    //private DonateFragment donateFragment;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        titlebarText = (TextView) findViewById(R.id.titlebar_title);


        navButton = (LinearLayout) findViewById(R.id.nav_button);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(drawerLayout.isDrawerOpen(GravityCompat.START))
                    drawerLayout.closeDrawer(GravityCompat.START);
                else
                    drawerLayout.openDrawer(GravityCompat.START);
            }
        });


        String[] navbarEntries = getResources().getStringArray(R.array.navigation_drawer_items_array);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.nav_entry, navbarEntries));
        drawerList.setOnItemClickListener(navClickListener);


        catalogFragment = new CatalogFragment();
        //liveFragment = new LiveFragment();
        //chatFragment = new ChatFragment();
        //scheduleFragment = new ScheduleFragment();
        contactFragment = new ContactFragment();
        //donateFragment = new DonateFragment();
        settingsFragment = new SettingsFragment();

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
            titlebarText.setText(navSelected.getText());
            navSelected.setSelected(true);


            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main_fragment, frag);
            transaction.addToBackStack(null);
            transaction.commit();

            if(activeFragment!=null)
                activeFragment.hide();
            activeFragment = frag;
            activeFragment.show();

            drawerLayout.closeDrawer(GravityCompat.START);
        }
    };
}
