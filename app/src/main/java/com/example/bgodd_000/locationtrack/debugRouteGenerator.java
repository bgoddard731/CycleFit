package com.example.bgodd_000.locationtrack;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by bgodd_000 on 2/17/2016.
 */

//Class that can auto generate a route summary of dummy values for use in testing.
//Change various parameters in order to change the values that are added to the dummy route if desired
public class debugRouteGenerator {
    public routeSummary genRT;
    public debugRouteSum debRT;
    private double METER = .00000899280576;
    private double startLat = 30.6360562;
    private double startLong = -96.3630399;

    private int DEFALUT_HR = 55;
    private double DEFALUT_RPM = 25;
    private double DEFAULT_INCLINE = -50;

    public debugRouteGenerator(){
        genRT = new routeSummary();
        double currLat = startLat;
        double currDistance = 0;
        double currSpeed = 1;
        int hr = DEFALUT_HR;
        double rpm = DEFALUT_RPM;
        double incline = DEFAULT_INCLINE;
        Date start = new Date();
        long currTime = start.getTime();
        genRT.start=start;
        for(int i = 0; i < 120*120; i++){
            LatLng currlatLng = new LatLng(currLat,startLong);
            genRT.points.add(new routeNode(hr, rpm, incline, currlatLng,currDistance,currSpeed,currTime));
            currLat += 2*METER
            ;
            currDistance += 2;
            currTime += 500;
            if(i%1000 == 0 && i != 0){
                currSpeed += 2;
                hr += 30;
                rpm += 15;
                incline += 30;
            }

        }
        Date end = new Date(currTime);
        genRT.end=end;
        genRT.totalDistance=currDistance;
        genRT.avgSpeed=4;
        genRT.elapsedTime= (end.getTime() - start.getTime())/1000;
    }
    public debugRouteGenerator(Date startdt){
        genRT = new routeSummary();
        double currLat = startLat;
        double currDistance = 0;
        double currSpeed = 0;
        long currTime = startdt.getTime();
        genRT.start=startdt;
        for(int i = 0; i < 7200; i++){
            LatLng currlatLng = new LatLng(currLat,startLong);
            genRT.points.add(new routeNode(DEFALUT_HR, DEFALUT_RPM, DEFAULT_INCLINE, currlatLng,currDistance,currSpeed,currTime));
            currLat += 2*METER;
            currDistance += 2;
            currTime += 500;
            currSpeed = 4;
        }
        Date end = new Date(currTime);
        genRT.end=end;
        genRT.totalDistance=currDistance;
        genRT.avgSpeed=4;
        genRT.elapsedTime= (end.getTime() - startdt.getTime())/1000;
    }
}
