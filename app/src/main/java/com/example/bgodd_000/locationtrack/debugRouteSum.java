package com.example.bgodd_000.locationtrack;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by bgodd_000 on 2/12/2016.
 */
public class debugRouteSum {
    double totalDistance;
    double elapsedTime;
    public Date start;
    public Date end;
    public double avgSpeed;
    public double avgHR;
    public double avgIncline;
    public double avgRPM;
    public double calorieBurn;
    public ArrayList<LatLng> points;

    public debugRouteSum(){
        totalDistance = 0;
        elapsedTime = 0;
        start = new Date();
        end = new Date();
        avgHR = 0;
        avgIncline = 0;
        avgRPM = 0;
        avgSpeed = 0;
        calorieBurn = 0;
        points = new ArrayList<>();
    }

    public debugRouteSum(double tD, double eT, Date s, Date e, double hr, double ic, double speed, double rpm, double burn, ArrayList<LatLng> pts){
        totalDistance = tD;
        elapsedTime = eT;
        start = s;
        end = e;
        avgHR = hr;
        avgIncline = ic;
        avgSpeed = speed;
        avgRPM = rpm;
        calorieBurn = burn;
        points = pts;
    }

    public void addPoint(LatLng pt){
        points.add(pt);
    }

    public String toString(){
        String print = "";
        print += totalDistance + ";";
        print += elapsedTime+ ";";
        print += start.getTime()+ ";";
        print += end.getTime()+ ";";
        print += avgHR+ ";";
        print += avgIncline+ ";";
        print += avgSpeed+ ";";
        print += avgRPM+ ";";
        print += calorieBurn+ ";";
        for(LatLng n: points){
            print += n.latitude + "," + n.longitude +";";
        }
        return print;
    }
    public debugRouteSum(String contents){
        points = new ArrayList<>();
        String[] parts = contents.split(";");
        totalDistance = Double.parseDouble(parts[0]);
        elapsedTime = Double.parseDouble(parts[1]);
        start = new Date(Long.parseLong(parts[2]));
        end = new Date(Long.parseLong(parts[3]));
        avgHR = Double.parseDouble(parts[4]);
        avgIncline = Double.parseDouble(parts[5]);
        avgSpeed = Double.parseDouble(parts[6]);
        avgRPM = Double.parseDouble(parts[7]);
        calorieBurn = Double.parseDouble(parts[8]);
        for(int i = 9; i < parts.length; i++){
           String[] temp = parts[i].split(",");
           points.add(new LatLng(Double.parseDouble(temp[0]),Double.parseDouble(temp[1])));
        }
    }

}
