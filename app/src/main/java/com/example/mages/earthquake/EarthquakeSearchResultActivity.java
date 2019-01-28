package com.example.mages.earthquake;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.v7.app.AlertController;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EarthquakeSearchResultActivity extends AppCompatActivity {

    private ArrayList<Earthquake> mEarthquakes = new ArrayList<>();

    private EarthquakeRecyclerViewAdapter mEarthquakeAdapter = new EarthquakeRecyclerViewAdapter(mEarthquakes);

    private MutableLiveData<String> searchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquake_search_result);

        RecyclerView recycleView = findViewById(R.id.search_result_list);
        recycleView.setLayoutManager(new LinearLayoutManager(this));
        recycleView.setAdapter(mEarthquakeAdapter);
    }

    private void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    private final Observer<List<Earthquake>> searchQueryResultObserver = updatedEarthquakes -> {
        // Update the UI with the updated search query results
        mEarthquakes.clear();
        if (updatedEarthquakes != null)
            mEarthquakes.addAll(updatedEarthquakes);
        mEarthquakeAdapter.notifyDataSetChanged();
    };
}
