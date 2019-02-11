package com.example.mages.earthquake;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class EarthquakeUpdateWorker extends Worker {
    private static final String TAG = "EarthquakeUpdateWorker";
    private static final String UPDATE_JOB_TAG = "update_job";
    private static final String PERIODIC_JOB_TAG = "periodic_job";

    public EarthquakeUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ArrayList<Earthquake> earthquakes = new ArrayList<>(0);

        URL url;
        try {
            String quakeFeed = getApplicationContext().getString(R.string.earthquake_feed);
            url = new URL(quakeFeed);

            URLConnection connection;
            connection = url.openConnection();

            HttpURLConnection httpConnection = (HttpURLConnection)connection;
            int responseCode = httpConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();

                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();

                NodeList nl = docEle.getElementsByTagName("entry");
                if (nl != null && nl.getLength() > 0) {
                    for (int i=0; i< nl.getLength(); i++) {

                        Element entry = (Element)nl.item(i);
                        Element id = (Element)entry.getElementsByTagName("id").item(0);
                        Element title = (Element)entry.getElementsByTagName("title").item(0);
                        Element g = (Element)entry.getElementsByTagName("georss:point").item(0);
                        Element when = (Element)entry.getElementsByTagName("updated").item(0);
                        Element link = (Element)entry.getElementsByTagName("link").item(0);

                        String idString = id.getFirstChild().getNodeValue();
                        String details = title.getFirstChild().getNodeValue();
                        String hostname = "http://earthquake.usgs.gov";
                        String linkString = hostname + link.getAttribute("href");
                        String point = g.getFirstChild().getNodeValue();
                        String dt = when.getFirstChild().getNodeValue();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
                        Date qdate = new GregorianCalendar(0,0,0).getTime();
                        try {
                            qdate = sdf.parse(dt);
                        } catch (ParseException e) {
                            Log.e(TAG, "Date parsing exception.", e);
                        }

                        String[] location = point.split(" ");
                        Location l = new Location("dummyGPS");
                        l.setLatitude(Double.parseDouble(location[0]));
                        l.setLongitude(Double.parseDouble(location[1]));

                        String magnitudeString = details.split(" ")[1];
                        int end = magnitudeString.length()-1;
                        double magnitude = Double.parseDouble(magnitudeString.substring(0, end));

                        if (details.contains("-"))
                            details = details.split("-")[1].trim();
                        else
                            details = "";

                        final Earthquake earthquake = new Earthquake(idString, qdate, details, l, magnitude, linkString);
                        earthquakes.add(earthquake);
                    }
                }
            }
            httpConnection.disconnect();

            EarthquakeDatabaseAccessor.getInstance(getApplicationContext()).earthquakeDAO().insertEarthquakes(earthquakes);

            scheduleNextUpdate();

            return Result.success();
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException", e);
            return Result.failure();
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            return Result.retry();
        } catch (ParserConfigurationException e) {
            Log.e(TAG, "ParserConfigurationException", e);
            return Result.failure();
        } catch (SAXException e) {
            Log.e(TAG, "SAXException", e);
            return Result.failure();
        }
    }

    private void scheduleNextUpdate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        int updateFreq = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_UPDATE_FREQ, "60"));
        boolean autoUpdateChecked = prefs.getBoolean(PreferencesActivity.PREF_AUTO_UPDATE, false);

        if (autoUpdateChecked) {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
/*
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(EarthquakeUpdateWorker.class, updateFreq*60 / 2, TimeUnit.SECONDS)
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance().enqueueUniquePeriodicWork(PERIODIC_JOB_TAG, ExistingPeriodicWorkPolicy.REPLACE, request);
*/
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(EarthquakeUpdateWorker.class)
                    .setConstraints(constraints)
                    .setInitialDelay(updateFreq * 60 / 2, TimeUnit.SECONDS)
                    .build();

            WorkManager.getInstance().enqueueUniqueWork(UPDATE_JOB_TAG, ExistingWorkPolicy.REPLACE, request);
        }
    }

    public static void scheduleInitialUpdate() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(EarthquakeUpdateWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance().enqueueUniqueWork(UPDATE_JOB_TAG, ExistingWorkPolicy.REPLACE, request);
    }
}
