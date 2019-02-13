package com.example.mages.earthquake;

import android.app.SearchManager;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.content.Intent;
import android.net.Uri;
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

    private LiveData<List<Earthquake>> searchResults;

    private MutableLiveData<String> selectedSearchSuggestionId;

    private LiveData<Earthquake> selectedSearchSuggestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquake_search_result);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recycleView = findViewById(R.id.search_result_list);
        recycleView.setLayoutManager(new LinearLayoutManager(this));
        recycleView.setAdapter(mEarthquakeAdapter);

        // Initialize the search query Live Data.
        searchQuery = new MutableLiveData<>();
        searchQuery.setValue(null);

        searchResults = Transformations.switchMap(searchQuery,
                query -> EarthquakeDatabaseAccessor.getInstance(getApplicationContext()).earthquakeDAO().searchEarthquakes("%" + query + "%"));

        searchResults.observe(this, searchQueryResultObserver);

        // Initialize the selected search suggestion Id Live Data.
        selectedSearchSuggestionId = new MutableLiveData<>();
        selectedSearchSuggestionId.setValue(null);

        selectedSearchSuggestion = Transformations.switchMap(selectedSearchSuggestionId,
                id -> EarthquakeDatabaseAccessor.getInstance(getApplicationContext()).earthquakeDAO().getEarthquake(id));

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            selectedSearchSuggestion.observe(this, selectedSearchSuggestionObserver);
            setSelectedSearchSuggestion(getIntent().getData());
        } else {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            setSearchQuery(query);
        }
        setSearchQueryFromIntent();
    }

    private void setSearchQueryFromIntent() {
        String query = getIntent().getStringExtra(SearchManager.QUERY);
        setSearchQuery(query);
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            setSelectedSearchSuggestion(getIntent().getData());
        } else {
            setSearchQueryFromIntent();
        }
    }

    private void setSelectedSearchSuggestion(Uri dataString) {
        String id = dataString.getPathSegments().get(1);
        selectedSearchSuggestionId.setValue(id);
    }

    private final Observer<Earthquake> selectedSearchSuggestionObserver = selectedSearchSuggestion -> {
        if (selectedSearchSuggestion != null) {
            setSearchQuery(selectedSearchSuggestion.getDetails());
        }
    };
}
