package com.clgproject.nareshbabu.trafficdata;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DataLoader extends AsyncTaskLoader<Void> {

    private SharedPreferences sharedPreferences;
    private Context mContext;

    public DataLoader(Context context) {
        super(context);
        mContext = context;
        sharedPreferences = context.getApplicationContext().getSharedPreferences("preferences", Activity.MODE_PRIVATE);
    }

    @Override
    protected void onStartLoading() {

        forceLoad();
    }

    @SuppressLint("HardwareIds")
    @Override
    public Void loadInBackground() {


        try {
            String urlAdress = "http://traffic-analyser.000webhostapp.com/index.php";
            URL url = new URL(urlAdress);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);


            StringBuilder text = new StringBuilder();
            try {


                FileInputStream fIS = mContext.getApplicationContext().openFileInput("jsondata.txt");
                InputStreamReader isr = new InputStreamReader(fIS, "UTF-8");
                BufferedReader br = new BufferedReader(isr);

                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line + '\n');
                }
                br.close();

            } catch (IOException e) {
                Log.e("Error!", e.getMessage());

            }
            Log.e("json", "loadInBackground: "+text.toString() );
            if(text.toString().length()==0) text.append("{}");

            JSONArray locationData = new JSONArray("["+text.toString().substring(0,text.toString().length()-2)+"]");

            Log.i("JSON", locationData.toString()+locationData.toString().length());

            JSONObject jsonParam = new JSONObject();
            jsonParam.put("id",Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID)+locationData.getJSONObject(0).getString("time"));
            jsonParam.put("user", Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));
            jsonParam.put("startLocation", "start");
            jsonParam.put("endLocation", "end");
            jsonParam.put("vehicle", sharedPreferences.getString("vehicle", "vehicle"));
            jsonParam.put("locationData", locationData);
            if(locationData.length()>0) {
                Geocoder gc = new Geocoder(mContext);
                String start,end;
                if(gc.isPresent()){
                    JSONObject o = (JSONObject)locationData.get(0);
                    List<Address> list = gc.getFromLocation(o.optDouble("latitude"), o.optDouble("longitude"),1);
                    Address address = list.get(0);
                    String[] parts = address.getAddressLine(0).split(",");
                    if(parts.length>3)
                        start=parts[parts.length-4]+","+parts[parts.length-3];
                    else
                        start = address.getLocality();

                    o = (JSONObject)locationData.get(locationData.length()-1);
                    list = gc.getFromLocation(o.optDouble("latitude"), o.optDouble("longitude"),1);
                    address = list.get(0);
                    //end = address.toString();
                    parts = address.getAddressLine(0).split(",");
                    if(parts.length>3)
                        end=parts[parts.length-4]+","+parts[parts.length-3];
                    else
                        end = address.getLocality();

                    jsonParam.put("startLocation", start);
                    jsonParam.put("endLocation", end);
                    Log.e("geocoder", start+" "+end+o.optDouble("latitude"));
                }
            }
            Log.e("not paasing connect","latitude");

            conn.connect();
            Log.e("not paasing any","latitude");

            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
            os.writeBytes(jsonParam.toString());
            Log.e("Full json",jsonParam.toString());
            os.flush();
            os.close();

            Log.i("STATUS", String.valueOf(conn.getResponseCode()));
            Log.i("MSG", conn.getResponseMessage());
            sharedPreferences.edit().putInt("responsecode", conn.getResponseCode()).commit();
            if(conn.getResponseCode()==200) {
                String filename = "jsondata.txt";
                File file = new File(mContext.getApplicationContext().getFilesDir() + "/" + filename);
                if (file.exists())
                    file.delete();
            }


            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }
}
