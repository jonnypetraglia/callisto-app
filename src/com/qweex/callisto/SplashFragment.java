package com.qweex.callisto;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SplashFragment extends Fragment
{
    MasterActivity MasterActivityRef;
    View contentView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        MasterActivityRef = (MasterActivity) getActivity();
        contentView = inflater.inflate(R.layout.splash, container, false);
        return contentView;
    }
}
