package com.example.mages.earthquake;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class EarthquakeUpdateWorker extends Worker {
    private static final String TAG = "EarthquakeUpdateWorker";
    private static final String UPDATE_JOB_TAG = "update_job";
    private static final String NOTIFICATION_CHANNEL = "earthquake";
    public static final int NOTIFICATION_ID = 1;

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

            scheduleNextUpdate(earthquakes);

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

    private void scheduleNextUpdate(List<Earthquake> earthquakes) {
        Earthquake largestNewEarthquake = findLargestNewEarthquake(earthquakes);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        int updateFreq = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_UPDATE_FREQ, "60"));
        boolean autoUpdateChecked = prefs.getBoolean(PreferencesActivity.PREF_AUTO_UPDATE, false);
        int minimumMagnitude = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_MIN_MAG, "3"));

        if (largestNewEarthquake != null && largestNewEarthquake.getMagnitude() >= minimumMagnitude) {
            broadcastNotification(largestNewEarthquake);
        }

        if (autoUpdateChecked) {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(EarthquakeUpdateWorker.class)
                    .setConstraints(constraints)
                    .setInitialDelay(10, TimeUnit.SECONDS)
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getApplicationContext().getString(R.string.earthquake_channel_name);

            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    name,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void broadcastNotification(Earthquake earthquake) {
        createNotificationChannel();

        Intent startActivityIntent = new Intent(getApplicationContext(), EarthquakeMainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(getApplicationContext(), 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder earthquakeNotificationBuilder = new NotificationCompat.Builder(getApplicationContext(),  NOTIFICATION_CHANNEL);

        earthquakeNotificationBuilder
                .setSmallIcon(R.drawable.ic_notification_24dp)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(launchIntent)
                .setAutoCancel(true)
                .setShowWhen(true);

        earthquakeNotificationBuilder
                .setWhen(earthquake.getDate().getTime())
                .setContentTitle("M:" + earthquake.getMagnitude())
                .setContentText(earthquake.getDetails())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(earthquake.getDetails()));

        NotificationManagerCompat notificationManager = NotificationManagerCompat .from(getApplicationContext());
        notificationManager.notify(NOTIFICATION_ID, earthquakeNotificationBuilder.build());
    }

    private Earthquake findLargestNewEarthquake(List<Earthquake> newEarthquakes) {
        List<Earthquake> earthquakes = EarthquakeDatabaseAccessor.getInstance(getApplicationContext()).earthquakeDAO().loadAllEarthquakesSync();

        Earthquake largestNewEarthquake = null;
        for (Earthquake earthquake : newEarthquakes) {
            if (earthquakes.contains(earthquake)) {
                continue;
            }

            if (largestNewEarthquake == null || earthquake.getMagnitude() > largestNewEarthquake.getMagnitude()) {
                largestNewEarthquake = earthquake;
            }
        }

        return largestNewEarthquake;
    }
}
