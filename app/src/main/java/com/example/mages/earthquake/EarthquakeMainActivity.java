package com.example.mages.earthquake;

import android.arch.lifecycle.ViewModelProviders;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EarthquakeMainActivity extends AppCompatActivity implements EarthquakeListFragment.OnListFragmentInteractionListener {

    private static final String TAG_LIST_FRAGMENT = "TAG_LIST_FRAGMENT";

    EarthquakeListFragment mEarthquakeListFragment;

    EarthquakeViewModel earthquakeViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquake_main);

        FragmentManager fm = this.getSupportFragmentManager();

        if (savedInstanceState == null) {
            FragmentTransaction ft = fm.beginTransaction();

            this.mEarthquakeListFragment = new EarthquakeListFragment();
            ft.add(R.id.main_activity_frame, mEarthquakeListFragment, TAG_LIST_FRAGMENT);
            ft.commitNow();
        } else {
            mEarthquakeListFragment = (EarthquakeListFragment) fm.findFragmentByTag(TAG_LIST_FRAGMENT);
        }

        earthquakeViewModel = ViewModelProviders.of(this).get(EarthquakeViewModel.class);
    }

    @Override
    public void onListFragmentRefreshRequested() {
        updateEarthquakes();
    }

    private void updateEarthquakes() {
        earthquakeViewModel.loadEarthquakes();
    }
}
