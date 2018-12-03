package com.example.mages.earthquake;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mages.earthquake.databinding.ListItemEarthquakeBinding;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EarthquakeRecyclerViewAdapter extends RecyclerView.Adapter<EarthquakeRecyclerViewAdapter.ViewHolder> {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);
    private static final NumberFormat MAGNITUDE_FORMAT = new DecimalFormat("0.0");

    private final List<Earthquake> mEarthquakes;

    public EarthquakeRecyclerViewAdapter(List<Earthquake> earthquakes) {
        this.mEarthquakes = earthquakes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItemEarthquakeBinding binding = ListItemEarthquakeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Earthquake earthquake = mEarthquakes.get(position);
        holder.binding.setEarthquake(earthquake);
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return this.mEarthquakes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ListItemEarthquakeBinding binding;


        public ViewHolder(ListItemEarthquakeBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
            binding.setTimeformat(TIME_FORMAT);
            binding.setMagnitudeformat(MAGNITUDE_FORMAT);
        }
    }
}
