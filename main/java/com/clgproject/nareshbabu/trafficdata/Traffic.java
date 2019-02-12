package com.clgproject.nareshbabu.trafficdata;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Traffic {

    private double latitude;
    private double longitude;
    private String time;

    public Traffic(double latitude, double longitude, String time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("latitude", latitude);
            obj.put("longitude", longitude);
            obj.put("time", time);
        } catch (JSONException e) {
            Log.e("Json", "DefaultListItem.toString JSONException: " + e.getMessage());
        }
        return obj;
    }


}
