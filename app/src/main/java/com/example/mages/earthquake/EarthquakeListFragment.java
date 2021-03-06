package com.example.mages.earthquake;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class EarthquakeListFragment extends Fragment {
    private ArrayList<Earthquake> mEarthquakes = new ArrayList<Earthquake>();

    private RecyclerView mRecyclerView;

    protected EarthquakeViewModel earthquakeViewModel;

    private SwipeRefreshLayout mSwipeToRefreshView;

    private EarthquakeRecyclerViewAdapter mEarthquakeAdapter = new EarthquakeRecyclerViewAdapter(mEarthquakes);

    private OnListFragmentInteractionListener mListener;

    private int nMinimumMagnitude = 0;

    public EarthquakeListFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mListener = (OnListFragmentInteractionListener) context;
   }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_earthquake_list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mSwipeToRefreshView = view.findViewById(R.id.swiperefresh);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = view.getContext();
        mRecyclerView.setLayoutManager((new LinearLayoutManager(context)));
        mRecyclerView.setAdapter(mEarthquakeAdapter);

        mSwipeToRefreshView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateEarthquakes();
            }
        });
    }

    protected void updateEarthquakes() {
        if (mListener != null) {
            mListener.onListFragmentRefreshRequested();
        }
    }

    protected void setEarthquakes(List<Earthquake> earthquakes) {
        updateFromPreferences();

        for (Earthquake earthquake : earthquakes) {
            if (earthquake.getMagnitude() >= nMinimumMagnitude) {
                if (!mEarthquakes.contains(earthquake)) {
                    mEarthquakes.add(earthquake);
                    mEarthquakeAdapter.notifyItemInserted(mEarthquakes.indexOf(earthquake));
                }
            }
        }

        if (mEarthquakes != null && mEarthquakes.size() > 0)
            for(int i = mEarthquakes.size() - 1; i >= 0; i--) {
                if (mEarthquakes.get(i).getMagnitude() < nMinimumMagnitude) {
                    mEarthquakes.remove(i);
                    mEarthquakeAdapter.notifyItemRemoved(i);
                }
            }

        mSwipeToRefreshView.setRefreshing(false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        earthquakeViewModel = ViewModelProviders.of(getActivity()).get(EarthquakeViewModel.class);
        earthquakeViewModel.getEarthquakes().observe(this, new Observer<List<Earthquake>>() {
            @Override
            public void onChanged(@Nullable List<Earthquake> earthquakes) {
                if (earthquakes != null) {
                    setEarthquakes(earthquakes);
                }
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentRefreshRequested();
    }

    private void updateFromPreferences() {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getContext());

        nMinimumMagnitude = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_MIN_MAG, "3"));
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PreferencesActivity.PREF_MIN_MAG.equals(key)) {
                List<Earthquake> earthquakes = earthquakeViewModel.getEarthquakes().getValue();
                if (earthquakes != null)
                    setEarthquakes(earthquakes);
            }
        }
    };

}
