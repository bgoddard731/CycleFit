package com.example.bgodd_000.locationtrack;

import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PrevRouteActivity2 extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    //Object to model the map presented by the fragment
    private GoogleMap mMap;
    //Connects device to Google Play services used to display location and map
    private GoogleApiClient mGoogleApiClient;
    private routeSummary rt;
    private ArrayList<LatLng> routepoints = new ArrayList<>();
    private int index;
    private int MAXINDEX = 4;
    //Stores the list of points along the route
    //Tag for Debugging
    public static final String TAG = MapsActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RES_REQUEST = 9000; //9 seconds to connection failure

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prev_route);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.prev_map);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Bundle extras = getIntent().getExtras();
        boolean track = extras.getBoolean("fromTrack");
        if(!track){
            //This activity was called from main menu, must load route from memory
            String sum_name = extras.getString("routeName");
            SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            String rtPath = prefs.getString(sum_name,"");
            loadRouteFromFile(rtPath);
        }else{
            //activity called from route tracking, must load result from global value
            //rt = extras.getParcelable("routeData");
            rt = Globals.summary;
        }

        //Add the route to map, update summary text box
        TextView sumText = (TextView) findViewById(R.id.route_sum_text);
        int hours = (int) rt.elapsedTime / 3600;
        double hr_rem = rt.elapsedTime % 3600;
        int min = (int) hr_rem / 60;
        double sec = hr_rem % 60;
        sumText.setText(String.format("Route Summary:\nTotal Distance Traveled: %.2f m\nTime: %d hr %d min %.2f sec\n Average Speed: %.2f m/s\nAverage Incline: %.2f deg\nAverage Pedal RPM: %.2f rpm\nAverage Heart Rate: %.2f bpm\nCalories Burned: %.1f cal", rt.totalDistance, hours, min, sec, rt.avgSpeed, rt.avgIncline, rt.avgRPM, rt.avgHR, rt.calorieBurn));
        for(routeNode n: rt.points){
            routepoints.add(n.loc);
        }
        //Set event listeners
        Button nextButton = (Button) findViewById(R.id.next_sum_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                nextClick(v);
            }
        });
        Button prevButton = (Button) findViewById(R.id.prev_sum_button);
        prevButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prevClick(v);
            }
        });
        //current page index = 0
        index = 0;
        //Log.d(TAG,"Post on Create: "+SystemClock.elapsedRealtimeNanos());
    }
    @Override
    protected void onResume(){
        //called after onStart or if app is moved from background to foreground
        super.onResume();
        setUpMapIfNeeded();
        if(!mGoogleApiClient.isConnected()){
            mGoogleApiClient.connect();
        }
    }
    //Connect map if necessary
    private void setUpMapIfNeeded(){
        if (mMap == null){
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.prev_map)).getMap();
            if (mMap != null){
                setUpMap();
            }
        }
    }
    //Function to handle any initialization of Map objects, only called when map created
    private void setUpMap(){
        //Turn on the my location layer for the map
        mMap.setMyLocationEnabled(true);
    }
    @Override
    public void onConnected(Bundle bundle) {
        //Add polyline of route to map, add markers for start and end
        if(!routepoints.isEmpty()){
            Polyline route = mMap.addPolyline(new PolylineOptions());
            route.setPoints(routepoints);
            mMap.addMarker(new MarkerOptions()
                    .position(routepoints.get(0))
                    .title("Route Start")
                    .snippet(rt.start.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.addMarker(new MarkerOptions()
                    .position(routepoints.get(routepoints.size() - 1))
                    .title("Route End")
                    .snippet(rt.end.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(routepoints.get(0)));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(13));
        }
        Log.d(TAG, "End of Load: " + SystemClock.elapsedRealtimeNanos());
        //Debug - print the route summary for comparison
//        new Thread(new Runnable() {
//            public void run() {
//                Log.d(TAG, rt.toString());
//            }
//        }).start();


    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()){
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RES_REQUEST);
            }catch (IntentSender.SendIntentException e){
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services failed with code: " + connectionResult.getErrorCode());
        }
    }
    //Get the route summary from memory
    private void loadRouteFromFile(String path){
        String ret = "";

        try {
            InputStream inputStream = openFileInput(path);
            //parse the bytes into a string
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        //Allow gson to handle longs for dates
        JsonDeserializer<Date> deser = new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonElement json, Type typeOfT,
                                    JsonDeserializationContext context) throws JsonParseException {
                return json == null ? null : new Date(json.getAsLong());
            }
        };

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, deser).create();
        rt = new routeSummary();
        rt = gson.fromJson(ret,rt.getClass());
    }
    //Update the screen as the user navigates through the route summary
    private void nextClick(View v){
        if(index == MAXINDEX){
            index = 0;
        }else{
            index++;
        }
        initializeScreen();
    }
    private void prevClick(View v){
        if(index == 0){
            index = MAXINDEX;
        }else{
            index--;
        }
        initializeScreen();
    }

    //Update the map based on which data point summary we are viewing
    //Overall summary = 0;
    //Speed = 1;
    //HR = 2;
    //RPM = 3;
    //Incline = 4;
    private void initializeScreen(){
        mMap.clear();
        if(!routepoints.isEmpty()){
            mMap.addMarker(new MarkerOptions()
                    .position(routepoints.get(0))
                    .title("Route Start")
                    .snippet(rt.start.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.addMarker(new MarkerOptions()
                    .position(routepoints.get(routepoints.size() - 1))
                    .title("Route End")
                    .snippet(rt.end.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(routepoints.get(0)));
//            mMap.moveCamera(CameraUpdateFactory.zoomTo(13));
        }
        TextView sumText = (TextView) findViewById(R.id.route_sum_text);
        ImageView scale = (ImageView) findViewById(R.id.scale);
        switch (index){
            case 0:
                if(!routepoints.isEmpty()){
                    Polyline route = mMap.addPolyline(new PolylineOptions());
                    route.setPoints(routepoints);
                }
                scale.setImageResource(R.drawable.bpm_color_scale);
                scale.setVisibility(View.INVISIBLE);
                int hours = (int) rt.elapsedTime / 3600;
                double hr_rem = rt.elapsedTime % 3600;
                int min = (int) hr_rem / 60;
                double sec = hr_rem % 60;
                sumText.setText(String.format("Route Summary:\nTotal Distance Traveled: %.2f m\nTime: %d hr %d min %.2f sec\n Average Speed: %.2f m/s\nAverage Incline: %.2f deg\nAverage Pedal RPM: %.2f rpm\nAverage Heart Rate: %.2f bpm\nCalories Burned: %.1f cal", rt.totalDistance, hours, min, sec, rt.avgSpeed, rt.avgIncline, rt.avgRPM, rt.avgHR, rt.calorieBurn));
                break;
            case 1:
                if(!routepoints.isEmpty()){
                    initializeSpeedMap();
                }
                scale.setImageResource(R.drawable.speed_color_scale);
                scale.setVisibility(View.VISIBLE);
                break;
            case 2:
                if(!routepoints.isEmpty()){
                    initializeHRMAP();
                }
                scale.setImageResource(R.drawable.bpm_color_scale);
                scale.setVisibility(View.VISIBLE);
                break;
            case 3:
                if(!routepoints.isEmpty()){
                    initializeRPMMap();
                }
                scale.setImageResource(R.drawable.rpm_color_scale);
                scale.setVisibility(View.VISIBLE);
                break;
            case 4:
                if(!routepoints.isEmpty()){
                    initializeInclineMap();
                }
                scale.setImageResource(R.drawable.incline_color_scale);
                scale.setVisibility(View.VISIBLE);
                break;
        }
    }
    //Set up the speed summary
    private void initializeSpeedMap(){
        double minSpeed = 99999;
        double maxSpeed = 0;
        //Add the color coded polylines representing the range of speeds. Keep adding points to line
        //until in new range, then make a new line and add previous line to map
        ArrayList<ArrayList<LatLng>> pointList = new ArrayList<>();
        ArrayList<LatLng> temp = new ArrayList<>();
        ArrayList<Integer> ranges = new ArrayList<>();
        int curr_range = calcSpeedRange(rt.points.get(0).speed);
        for(routeNode n: rt.points){
            if(n.speed < minSpeed){
                minSpeed = n.speed;
            }
            if(n.speed > maxSpeed){
                maxSpeed = n.speed;
            }
            if(calcSpeedRange(n.speed) == curr_range){
                temp.add(n.loc);
            }else{
                temp.add(n.loc);
                pointList.add(temp);
                ranges.add(curr_range);
                temp = new ArrayList<>();
                temp.add(n.loc);
                curr_range = calcSpeedRange(n.speed);
            }
        }
        pointList.add(temp);
        ranges.add(curr_range);
        for(int i = 0; i < pointList.size(); i++){
            Polyline route = mMap.addPolyline(new PolylineOptions());
            route.setPoints(pointList.get(i));
            route.setColor(calcRangeColor(ranges.get(i)));
        }
        TextView sumText = (TextView) findViewById(R.id.route_sum_text);
        sumText.setText(String.format("Speed Summary:\nAverage Speed: %1.2fm/s\nMinimum Speed: %2.2fm/s\n Maximum Speed: %3.2fm/s", rt.avgSpeed, minSpeed, maxSpeed));

    }

    private int calcSpeedRange(double speed){
        if(speed < 2){
            return 0;
        }else if(speed >= 2 && speed < 4){
            return 1;
        }else if(speed >= 4 && speed < 6){
            return 2;
        }else if(speed >= 6 && speed < 8){
            return 3;
        }else if(speed >= 8 && speed < 10) {
            return 4;
        }else{
            return 5;
        }
    }

    //Put the HR polylines on the map
    private void initializeHRMAP(){
        int minHR = 99999;
        int maxHR = 0;
        //Add the color coded polylines representing the range of speeds. Keep adding points to line
        //until in new range, then make a new line and add previous line to map
        ArrayList<ArrayList<LatLng>> pointList = new ArrayList<>();
        ArrayList<LatLng> temp = new ArrayList<>();
        ArrayList<Integer> ranges = new ArrayList<>();
        int curr_range = calcHRRange(rt.points.get(0).hr);
        for(routeNode n: rt.points){
            if(n.hr < minHR){
                minHR = n.hr;
            }
            if(n.hr > maxHR){
                maxHR = n.hr;
            }
            if(calcHRRange(n.hr) == curr_range){
                temp.add(n.loc);
            }else{
                temp.add(n.loc);
                pointList.add(temp);
                ranges.add(curr_range);
                temp = new ArrayList<>();
                temp.add(n.loc);
                curr_range = calcHRRange(n.hr);
            }
        }
        pointList.add(temp);
        ranges.add(curr_range);
        for(int i = 0; i < pointList.size(); i++){
            Polyline route = mMap.addPolyline(new PolylineOptions());
            route.setPoints(pointList.get(i));
            route.setColor(calcRangeColor(ranges.get(i)));
        }
        TextView sumText = (TextView) findViewById(R.id.route_sum_text);
        sumText.setText(String.format("Heart Rate Summary:\nAverage Heart Rate: %.2f bpm\nMinimum Heart Rate: %d bpm\n Maximum Heart Rate: %d bpm", rt.avgHR, minHR, maxHR));
    }
    //Determines the range of hr
    private int calcHRRange(int hr){
        if(hr < 60){
            return 0;
        }else if(hr >= 60 && hr < 90){
            return 1;
        }else if(hr >= 90 && hr < 120){
            return 2;
        }else if(hr >= 120 && hr < 150){
            return 3;
        }else if(hr >= 150 && hr < 180) {
            return 4;
        }else{
            return 5;
        }
    }

    //Put up the RPM summary map
    private void initializeRPMMap(){
        double minRPM = 99999;
        double maxRPM = 0;
        //Add the color coded polylines representing the range of speeds. Keep adding points to line
        //until in new range, then make a new line and add previous line to map
        ArrayList<ArrayList<LatLng>> pointList = new ArrayList<>();
        ArrayList<LatLng> temp = new ArrayList<>();
        ArrayList<Integer> ranges = new ArrayList<>();
        int curr_range = calcRPMRange(rt.points.get(0).rpm);
        for(routeNode n: rt.points){
            if(n.rpm < minRPM){
                minRPM = n.rpm;
            }
            if(n.rpm > maxRPM){
                maxRPM = n.rpm;
            }
            if(calcRPMRange(n.rpm) == curr_range){
                temp.add(n.loc);
            }else{
                temp.add(n.loc);
                pointList.add(temp);
                ranges.add(curr_range);
                temp = new ArrayList<>();
                temp.add(n.loc);
                curr_range = calcRPMRange(n.rpm);
            }
        }
        pointList.add(temp);
        ranges.add(curr_range);
        for(int i = 0; i < pointList.size(); i++){
            Polyline route = mMap.addPolyline(new PolylineOptions());
            route.setPoints(pointList.get(i));
            route.setColor(calcRangeColor(ranges.get(i)));
        }
        TextView sumText = (TextView) findViewById(R.id.route_sum_text);
        sumText.setText(String.format("Pedal RPM Summary:\nAverage RPM: %.2f rpm\nMinimum RPM: %.2f rpm\n Maximum RPM: %.2f rpm", rt.avgRPM, minRPM, maxRPM));
    }
    //Calc the RPM ranges
    private int calcRPMRange(double rpm){
        if(rpm < 20){
            return 0;
        }else if(rpm >= 20 && rpm < 40){
            return 1;
        }else if(rpm >= 40 && rpm < 60){
            return 2;
        }else if(rpm >= 60 && rpm < 80){
            return 3;
        }else if(rpm >= 80 && rpm < 100) {
            return 4;
        }else{
            return 5;
        }
    }

    //Initialize Incline summary map
    private void initializeInclineMap(){
        double minIncline = 999;
        double maxIncline = -999;
        //Add the color coded polylines representing the range of speeds. Keep adding points to line
        //until in new range, then make a new line and add previous line to map
        ArrayList<ArrayList<LatLng>> pointList = new ArrayList<>();
        ArrayList<LatLng> temp = new ArrayList<>();
        ArrayList<Integer> ranges = new ArrayList<>();
        int curr_range = calcInclineRange(rt.points.get(0).incline);
        for(routeNode n: rt.points){
            if(n.incline < minIncline){
                minIncline = n.incline;
            }
            if(n.incline > maxIncline){
                maxIncline = n.incline;
            }
            if(calcInclineRange(n.incline) == curr_range){
                temp.add(n.loc);
            }else{
                temp.add(n.loc);
                pointList.add(temp);
                ranges.add(curr_range);
                temp = new ArrayList<>();
                temp.add(n.loc);
                curr_range = calcInclineRange(n.incline);
            }
        }
        pointList.add(temp);
        ranges.add(curr_range);
        for(int i = 0; i < pointList.size(); i++){
            Polyline route = mMap.addPolyline(new PolylineOptions());
            route.setPoints(pointList.get(i));
            route.setColor(calcRangeColor(ranges.get(i)));
        }
        TextView sumText = (TextView) findViewById(R.id.route_sum_text);
        sumText.setText(String.format("Incline Summary:\nAverage Incline: %1.2f deg\nMinimum Incline: %2.2f deg\n Maximum Incline: %3.2f deg", rt.avgIncline, minIncline, maxIncline));

    }

    //calc the incline point ranges for the polylines
    private int calcInclineRange(double incline){
        if(incline < -30){
            return 0;
        }else if(incline >= -30 && incline < -15){
            return 1;
        }else if(incline >= -15 && incline < 0){
            return 2;
        }else if(incline >= 0 && incline < 15){
            return 3;
        }else if(incline >= 15 && incline < 30) {
            return 4;
        }else{
            return 5;
        }
    }

    //Assign a color to the range for each of the summary polylines
    private int calcRangeColor(int range){
        switch (range){
            case 0:
                return Color.BLACK;
            case 1:
                return Color.rgb(0,0,255);
            case 2:
                return Color.rgb(0,255,0);
            case 3:
                return Color.rgb(255,255,0);
            case 4:
                return Color.rgb(241,90,36);
            case 5:
                return Color.rgb(255,0,0);
            default: return Color.BLACK;
        }
    }


}
