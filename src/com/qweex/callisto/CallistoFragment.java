package com.qweex.callisto;

import android.support.v4.app.Fragment;

public abstract class CallistoFragment extends Fragment {

    public CallistoFragment(DatabaseConnector db) {
        mastersDatabaseConnector = db;
    }

    protected DatabaseConnector mastersDatabaseConnector;
    public abstract void show();
    public abstract void hide();
}
