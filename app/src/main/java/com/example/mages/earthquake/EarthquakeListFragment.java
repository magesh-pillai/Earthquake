package com.example.mages.earthquake;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class EarthquakeListFragment extends Fragment {
    private ArrayList<Earthquake> mEarthquakes = new ArrayList<Earthquake>();

    private RecyclerView mRecyclerView;

    public void setEarthquakes(ArrayList<Earthquake> earthquakes) {
        for (Earthquake earthquake : earthquakes) {
            if (!mEarthquakes.contains(earthquake)) {
                mEarthquakes.add(earthquake);
                mEarthquakeAdapter.notifyItemInserted(mEarthquakes.indexOf(earthquake));
            }
        }
    }

    private EarthquakeRecyclerViewAdapter mEarthquakeAdapter = new EarthquakeRecyclerViewAdapter(mEarthquakes);

    public EarthquakeListFragment() {
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
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = view.getContext();
        mRecyclerView.setLayoutManager((new LinearLayoutManager(context)));
        mRecyclerView.setAdapter(mEarthquakeAdapter);
    }


}
