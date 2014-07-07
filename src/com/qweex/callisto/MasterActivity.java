package com.qweex.callisto;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.*;

public class MasterActivity extends FragmentActivity {

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private LinearLayout navButton;
    private TextView navSelected, titlebarText;


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
    }

    AdapterView.OnItemClickListener navClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            switch (position+1) {
                case 1: //Catalog
                    break;
                case 2: //Live
                    break;
                case 3: //Chat
                    break;
                case 4: //Schedule
                    break;
                case 5: //Contact
                    break;
                case 6: //Donate
                    break;
                case 7: //Settings
                    break;
                case 8: //About
                    break;
            }
            if(navSelected!=null)
                navSelected.setSelected(false);
            navSelected = (TextView) view;
            titlebarText.setText(navSelected.getText());
            navSelected.setSelected(true);


            /*FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            MainFragment mf = new MainFragment(false);

            getFragmentManager().popBackStackImmediate();
            transaction.replace(R.id.main_fragment, mf);*/

            drawerLayout.closeDrawer(GravityCompat.START);
        }
    };

    View.OnClickListener showSettings = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d("Callisto", "Settings");
        }
    };
}
