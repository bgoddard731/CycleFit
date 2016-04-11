package com.example.bgodd_000.locationtrack;


import com.google.android.gms.maps.model.LatLng;

/**
 * Created by bgodd_000 on 2/12/2016.
 */

//Structure to hold a point of data along a route
public class routeNode {
    public int hr;
    public double rpm;
    public double incline;
    public LatLng loc;
    public double distance;
    public double speed;
    public double elapsed_time;

    public routeNode(){
        hr = 0;
        rpm = 0;
        incline = 0;
        loc = new LatLng(0,0);
        distance = 0;
        speed = 0;
        elapsed_time = 0;
    }
    public routeNode(int h, double r, double i, LatLng location, double d, double s, double et){
        hr = h;
        rpm = r;
        incline = i;
        loc = location;
        distance = d;
        speed = s;
        elapsed_time = et;
    }
    public String toString(){
        return hr + "," + rpm + "," + incline + "," + loc.latitude + "," + loc.longitude + "," + distance + "," + speed + "," + elapsed_time;
    }
    public routeNode(String contents){
        String[] parts = contents.split(",");
        hr = Integer.parseInt(parts[0]);
        rpm = Double.parseDouble(parts[1]);
        incline = Double.parseDouble(parts[2]);
        loc = new LatLng(Double.parseDouble(parts[3]), Double.parseDouble(parts[4]));
        distance = Double.parseDouble(parts[5]);
        speed = Double.parseDouble(parts[6]);
        elapsed_time = Double.parseDouble(parts[7]);
    }

}
