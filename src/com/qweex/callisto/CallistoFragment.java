package com.qweex.callisto;

import android.support.v4.app.Fragment;

public abstract class CallistoFragment extends Fragment {

    public CallistoFragment(MasterActivity master) {
        this.master = master;
    }

    protected MasterActivity master;
    public abstract void show();
    public abstract void hide();
}
