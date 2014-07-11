package com.qweex.callisto;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/** This fragment is a splash fragment. It exists because I don't know what to show when the app starts up.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class SplashFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.splash, container, false);
    }
}
