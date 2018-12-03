package com.example.mages.earthquake;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EarthquakeMainActivity extends AppCompatActivity {

    private static final String TAG_LIST_FRAGMENT = "TAG_LIST_FRAGMENT";

    EarthquakeListFragment mEarthquakeListFragment;

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

        Date now = Calendar.getInstance().getTime();
        ArrayList<Earthquake> dummyQuakes = new ArrayList<Earthquake>(0);
        for (int i = 0; i< 30; i++) {
            dummyQuakes.add(new Earthquake(String.valueOf(i), now, "San Jose", null, 7.3, null));
        }
        mEarthquakeListFragment.setEarthquakes(dummyQuakes);
    }
}
