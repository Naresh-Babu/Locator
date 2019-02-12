package com.clgproject.nareshbabu.trafficdata;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GpsService extends Service {

    private static final String TAG = "GPSservice";
    private static final float LOCATION_DISTANCE = 0;
    public static String sampleString = "HI";
    private static long LOCATION_INTERVAL = 0;
    private static Timer timer;
    public ArrayList<Traffic> mTrafficList = new ArrayList<>();
    public long first_time;
    SharedPreferences.Editor editor;
    Location location;
    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };
    private SimpleDateFormat simpleDateFormat;
    private LocationManager mLocationManager = null;
    private SharedPreferences preferences;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        LOCATION_INTERVAL = intent.getLongExtra("interval", 121);

        Log.e(TAG, "onStartCommand");
        updater();
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {

        preferences = getApplicationContext().getSharedPreferences("preferences", Activity.MODE_PRIVATE);


        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        timer = new Timer();

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }


    }

    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return bestLocation;
            }
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }

        return bestLocation;
    }

    @Override
    public void onDestroy() {

        JSONArray jsonArray = new JSONArray();

//        if(mTrafficList.size()>0) {

            for (int i = 0; i < mTrafficList.size(); i++) {
                jsonArray.put(mTrafficList.get(i).getJSONObject());
            }
            Log.e("jsonArray", jsonArray.length() + jsonArray.toString());
            String filename = "jsondata.txt";
            File directory;
            if (filename.isEmpty()) {
                directory = getFilesDir();
            } else {
                directory = getDir(filename, MODE_PRIVATE);
            }
            FileOutputStream fos = null;
            try {
                fos = openFileOutput(filename, Context.MODE_APPEND);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                String sToAdd;
                if(mTrafficList.size()>0) {
                sToAdd = jsonArray.toString().substring(1, jsonArray.toString().length() - 1) + ",";
                }
                else
                    sToAdd = "";
                fos.write((sToAdd).getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
//        }

        Log.e(TAG, "onDestroy");
        super.onDestroy();
        Log.e(TAG, sampleString);
        timer.cancel();
        if (mLocationManager != null) {
            for (LocationListener mLocationListener : mLocationListeners) {
                try {
                    mLocationManager.removeUpdates(mLocationListener);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }

    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    //Foreground conversion
    private void updater() {
        Log.e("Gate", "onStartCommand: " + LOCATION_INTERVAL);
        location = null;
        showNotification();

        location = getLastKnownLocation();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        location = getLastKnownLocation();
                        if (location == null)
                            Toast.makeText(GpsService.this, "Check location", Toast.LENGTH_SHORT).show();
                    }
                };
                mainHandler.post(myRunnable);

                if (location != null) {
                    String format = simpleDateFormat.format(new Date());
                    Log.e("Location", location.getLatitude() + " " + location.getLongitude());
                    mTrafficList.add(new Traffic(location.getLatitude(), location.getLongitude(), format));
                    new PostDataAsyncTask().execute();
                }
            }

        }, 0, LOCATION_INTERVAL*1000);

    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        Log.e(TAG, "Inside");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = null;
        Bitmap icon = null;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N)
            icon = BitmapFactory.decodeResource(getResources(),
                    R.mipmap.traffic);


        NotificationManager notification_manager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder notification_builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String chanel_id = "Naresh";
            CharSequence name = "Channel Name";
            String description = "Chanel Description";
            NotificationChannel mChannel = new NotificationChannel(chanel_id, name, NotificationManager.IMPORTANCE_DEFAULT);
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.setLightColor(R.color.colorPrimary);

            if (notification_manager != null) {
                notification_manager.createNotificationChannel(mChannel);
            }

            Log.e(TAG, "channel NULL " + mChannel.toString());

            notification_builder = new Notification.Builder(this, chanel_id);
        } else {
            notification_builder = new Notification.Builder(this);
        }
        notification_builder
                .setContentTitle("Locator")
                .setTicker("Location Collection")
                .setContentText("Running")
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N)
            notification_builder.setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            notification_builder.setSmallIcon(R.mipmap.traffic);
            notification_builder.setColor(getResources().getColor(R.color.colorPrimary));
        }
        if (Build.VERSION.SDK_INT >= 26) {
            notification_builder.setColorized(true);
        }
        notification = notification_builder.build();

        startForeground(101, notification);
//        }
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            //Log.e(TAG, "onLocationChanged: " + location);
            //Toast.makeText(GpsService.this, location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show();
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    //Sending
    public class PostDataAsyncTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
            // do stuff before posting data
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                String TAG = "update";
                String postReceiverUrl = "http://kalamsdream.000webhostapp.com/location_receiver.php";
                Log.v(TAG, "postURL: " + postReceiverUrl);

                // HttpClient
                HttpClient httpClient = new DefaultHttpClient();

                // post header
                HttpPost httpPost = new HttpPost(postReceiverUrl);

                // add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                JSONObject jsonParam = new JSONObject();

                jsonParam.put("id", Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID));
                jsonParam.put("lat", location.getLatitude());
                jsonParam.put("lon", location.getLongitude());



                nameValuePairs.add(new BasicNameValuePair("json", jsonParam.toString()));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // execute HTTP post request
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity resEntity = response.getEntity();

                if (resEntity != null) {

                    String responseStr = EntityUtils.toString(resEntity).trim();
                    Log.v(TAG, "Response: " +  responseStr);

                    // you can add an if statement here and do other actions based on the response
                }
                Log.v(TAG, "Response: "+jsonParam.toString());


            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return  null;
        }

        @Override
        protected void onPostExecute(String lenghtOfFile) {
            // do stuff after posting data
        }
    }
}