package com.example.bgodd_000.locationtrack;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by bgodd_000 on 3/2/2016.
 */

//Structure to hold route summary information. COntaines overall route summary data points, the start
//and end location, but none of the midpoint data
public class smallRouteSummary {
    double totalDistance;
    double elapsedTime;
    public Date start;
    public Date end;
    public double avgSpeed;
    public double avgHR;
    public double avgIncline;
    public double avgRPM;
    public double calorieBurn;
    public LatLng startLoc;
    public LatLng endLoc;

    public smallRouteSummary(){
        totalDistance = 0;
        elapsedTime = 0;
        start = new Date();
        end = new Date();
        avgHR = 0;
        avgIncline = 0;
        avgRPM = 0;
        avgSpeed = 0;
        calorieBurn = 0;
        startLoc = new LatLng(0,0);
        endLoc = new LatLng(0,0);
    }

    public smallRouteSummary(String contents){
        String[] parts = contents.split(";", 13);
        totalDistance = Double.parseDouble(parts[0]);
        elapsedTime = Double.parseDouble(parts[1]);
        start = new Date(Long.parseLong(parts[2]));
        end = new Date(Long.parseLong(parts[3]));
        avgHR = Double.parseDouble(parts[4]);
        avgIncline = Double.parseDouble(parts[5]);
        avgSpeed = Double.parseDouble(parts[6]);
        avgRPM = Double.parseDouble(parts[7]);
        calorieBurn = Double.parseDouble(parts[8]);
        startLoc = new LatLng(Double.parseDouble(parts[9]),Double.parseDouble(parts[10]));
        endLoc = new LatLng(Double.parseDouble(parts[11]),Double.parseDouble(parts[12]));

    }
}
