package com.example.bgodd_000.locationtrack;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class PlanActivity2 extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    //Object to model the map presented by the fragment
    private GoogleMap mMap;
    //Connects device to Google Play services used to display location and map
    private GoogleApiClient mGoogleApiClient;
    //Globals to track markers placed on the map by user
    private Marker start;
    private Marker end;
    private ArrayList<Marker> waypoints;
    //List of points to be returned by directions request
    private ArrayList<LatLng> routepoints = new ArrayList<>();
    //Lists that store routes that match the planned route
    private ArrayList<smallRouteSummary> sim_routes;
    private ArrayList<smallRouteSummary> rev_sim_routes;
    private boolean use_rev = false;
    private long distance_planned;

    //Tracks whether the user is placing start, end, waypoints, or viewing a summary
    private int mode;

    //Tag for Debugging
    public static final String TAG = MapsActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RES_REQUEST = 9000; //9 seconds to connection failure

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);
        //Set up map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.plan_map);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        waypoints = new ArrayList<>();
        mode = 0; //Start placements
        //Initialize
        setModeText();
        setPlanEventListeners();

    }
    //Add all the on click listeners
    public void setPlanEventListeners(){
        Button start_button = (Button) findViewById(R.id.start_button);
        start_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startButtonClick();
            }
        });
        Button waypoint_button = (Button) findViewById(R.id.waypoint_button);
        waypoint_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                waypointButtonClick();
            }
        });
        Button end_button = (Button) findViewById(R.id.end_button);
        end_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                endButtonClick();
            }
        });
        Button go_button = (Button) findViewById(R.id.go_button);
        go_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goButtonClick(v);
            }
        });
        Button simViewButton = (Button) findViewById(R.id.simViewButton);
        simViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simRoutesClick();
            }
        });
    }


    @Override
    protected void onResume() {
        //called after onStart or if app is moved from background to foreground
        super.onResume();
        setUpMapIfNeeded();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    //Connect map if necessary
    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.plan_map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    //Function to handle any initialization of Map objects, only called when map created
    private void setUpMap() {
        //Turn on the my location layer for the map
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(Bundle bundle) {
        //Center camera on current location, and place start marker there by default
        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //set map and marker click listeners
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mapClick(latLng);
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return markerClick(marker);
            }
        });
        if(loc != null) {
            //If connected, zoom camera to current location
            double currentLat = loc.getLatitude();
            double currentLong = loc.getLongitude();
            LatLng latLng = new LatLng(currentLat,currentLong);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
            //Drop Start marker at last known location
            start = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Route Start")
                    .snippet(latLng.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
    }

    //Connection no work...
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    //COuldnt connect to map service
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RES_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services failed with code: " + connectionResult.getErrorCode());
        }
    }

    //Initialize the user prompt based upon the mode
    private void setModeText(){
        TextView mode_txt = (TextView) findViewById(R.id.mode_text);
        Button start_button = (Button) findViewById(R.id.start_button);
        Button waypoint_button = (Button) findViewById(R.id.waypoint_button);
        Button end_button = (Button) findViewById(R.id.end_button);
        if(mode == 0){ //Start marker placement
            start_button.setTextColor(Color.YELLOW);
            waypoint_button.setTextColor(Color.WHITE);
            end_button.setTextColor(Color.WHITE);
            mode_txt.setText("Place Start Marker:\nClick Map to designate route beginning");
        }else if(mode == 1){//Waypoint marker placements
            start_button.setTextColor(Color.WHITE);
            waypoint_button.setTextColor(Color.YELLOW);
            end_button.setTextColor(Color.WHITE);
            mode_txt.setText("Place Waypoint Markers (Optional):\nClick points on map to add waypoints to route.\nClick waypoint to remove");
        }else{//End marker placement
            start_button.setTextColor(Color.WHITE);
            waypoint_button.setTextColor(Color.WHITE);
            end_button.setTextColor(Color.YELLOW);
            mode_txt.setText("Place End Marker:\nClick Map to designate route end");
        }
    }
    //Change modes based on option button clicks
    //And re initialize prompt
    private void startButtonClick(){
        mode = 0;
        setModeText();
    }

    private void waypointButtonClick(){
        mode = 1;
        setModeText();
    }

    private void endButtonClick(){
        mode = 2;
        setModeText();
    }

    //map click listener, used to drop markers for route planning
    private void mapClick(LatLng loc){
        if(mode == 0){
            //Place a start marker where the click occurred
            if(start != null){
                start.setPosition(loc);
                start.setSnippet(loc.toString());
            }else{
                start = mMap.addMarker(new MarkerOptions()
                        .position(loc)
                        .title("Route Start")
                        .snippet(loc.toString())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }
        }else if(mode == 1){
            //Place a waypoint marker where the click occurred
            Marker temp = mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title("Waypoint")
                    .snippet(loc.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            waypoints.add(temp);
        }else if(mode == 2){
            //Place end marker on the click
            if(end != null){
                end.setPosition(loc);
                end.setSnippet(loc.toString());
            }else {
                end = mMap.addMarker(new MarkerOptions()
                        .position(loc)
                        .title("Route End")
                        .snippet(loc.toString())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }
        }
    }

    //If a waypoint marker is clicked during waypoint edit mode, remove the marker
    private boolean markerClick(Marker m){
        if(m.equals(start) || m.equals(end)){
            return false;
        }else if(mode == 1){
            //Delete a waypoint marker if waypoint mode, and a waypoint marker is clicked
            if(waypoints.contains(m)){
                waypoints.remove(m);
            }
            m.remove();
            return true;
        }else{
            return false;
        }
    }

    //Get the directions from google direcitons
    //Go button becomes reset button after go is pressed, so reset re-initializes the map
    private void goButtonClick(View v){
        Button go_button = (Button) findViewById(R.id.go_button);
        if(go_button.getText().equals("Reset")){
            //Reset the map and re-initialize the options
            Log.d(TAG,"RESET");
            Button start_button = (Button) findViewById(R.id.start_button);
            Button waypoint_button = (Button) findViewById(R.id.waypoint_button);
            Button end_button = (Button) findViewById(R.id.end_button);
            Button simViewButton = (Button) findViewById(R.id.simViewButton);
            simViewButton.setVisibility(View.INVISIBLE);
            start_button.setVisibility(View.VISIBLE);
            end_button.setVisibility(View.VISIBLE);
            waypoint_button.setVisibility(View.VISIBLE);
            go_button.setText("Go");
            mMap.clear();
            mode = 0;
            start = null;
            end = null;
            waypoints = new ArrayList<>();
            setModeText();
            return;
        }
        //Not in reset mode: get the directions
        if(start == null || end == null){
            //Must have start and end to get directions
            Toast.makeText(getApplicationContext(), "Must Place a Start and End Markers!", Toast.LENGTH_SHORT).show();
        }else{
            //Get the directions from a GET server request
            Log.d(TAG, "Start of request: " + SystemClock.elapsedRealtimeNanos());
            //Get directions and update the modes
            String url = createDirectionsUrl();
            //Log.d(TAG,url);
            mode = 3;
            new JSONconnection().execute(url);
        }
    }

    //Put together the url request string to send to the API for direcitons
    private String createDirectionsUrl(){
        //Build the JSON request to get directions from Google
        String url = "";
        url += "https://maps.googleapis.com/maps/api/directions/json?";
        //Start and end
        url += "origin="+start.getPosition().latitude + "," + start.getPosition().longitude;
        url += "&destination=" + end.getPosition().latitude + "," + end.getPosition().longitude;
        url += "&mode=bicycling"; //Get a bicycle route
        //Add waypoints if they exist
        if(waypoints.size() > 0){
            String waypoint_opts = "&waypoints=optimize:true";
            for(int i = 0; i < waypoints.size(); i++){
                waypoint_opts += "|via:"+waypoints.get(i).getPosition().latitude + "," + waypoints.get(i).getPosition().longitude;
            }
            url += waypoint_opts;
        }
        // add the Directions API Key
        url+="&key=AIzaSyB_zRbAj1yfngb_APNXfRY1DUhXLrQ6rzI";
        return url;
    }

    //Execute the request for direcitons asynchronosly
    public class JSONconnection extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            return getJSON(params[0],10000);
        }

        // onPostExecute displays the results of the AsyncTask.
        //The format for the return value for the request found at: https://developers.google.com/maps/documentation/directions/intro#Introduction
        @Override
        protected void onPostExecute(String result) {
            // Log.d(TAG,result);
            try {
                //Parse Results
                //Get the routepoint list
                JSONObject res = new JSONObject(result);
                JSONArray routes = res.getJSONArray("routes");
                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolylines = route.getJSONObject("overview_polyline");
                String encodedString = overviewPolylines.getString("points");
                routepoints = decodePoly(encodedString);

                //Add route to map
                Polyline dirs = mMap.addPolyline(new PolylineOptions());
                dirs.setPoints(routepoints);

                //Get distance from route
                JSONArray legs = route.getJSONArray("legs");
                JSONObject leg = legs.getJSONObject(0);
                JSONObject distance = leg.getJSONObject("distance");
                String distanceval = distance.getString("value");
                distance_planned = Long.parseLong(distanceval);
                showPlanResults();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    //Perform the url GET Request to the server
    public String getJSON(String url, int timeout) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();
            //Parse the result
            switch (status) {
                case 200:
                case 201:
                    //Good results, parse the bytes into a string to send to onPostExecute
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    return sb.toString();
            }
            //Otherwise its bad and catch the exceptions
        } catch (MalformedURLException ex) {
            Log.d(TAG, ex.toString());
        } catch (IOException ex) {
            Log.d(TAG, ex.toString());
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    Log.d(TAG, ex.toString());
                }
            }
        }
        return null;
    }
    //Optained from Google, decodes a polyline from encoded mode
    private ArrayList<LatLng> decodePoly(String encoded) {

        ArrayList<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    //Update to show results of the plan request and comparison to stored routes
    private void showPlanResults(){
        TextView mode_txt = (TextView) findViewById(R.id.mode_text);
        Button start_button = (Button) findViewById(R.id.start_button);
        Button waypoint_button = (Button) findViewById(R.id.waypoint_button);
        Button end_button = (Button) findViewById(R.id.end_button);
        Button go_button = (Button) findViewById(R.id.go_button);
        //Update visibility
        start_button.setVisibility(View.INVISIBLE);
        end_button.setVisibility(View.INVISIBLE);
        waypoint_button.setVisibility(View.INVISIBLE);
        go_button.setText("Reset");
        calcSimilarRoutes();//calculate the routes that match
        String modeText = "";

        //Route not traveled before
        if(sim_routes.size() == 0 && rev_sim_routes.size()==0){
            modeText+= "This route has not been traveled before.\nComplete and store this route more times to compare in the future.";
            //Route traveled before
        }else if(sim_routes.size() > 0){
            modeText += calcPredText(false);
            Button simViewButton = (Button) findViewById(R.id.simViewButton);
            simViewButton.setVisibility(View.VISIBLE);
            //Route traveled in reverse before
        }else{
            modeText += calcPredText(true);
            Button simViewButton = (Button) findViewById(R.id.simViewButton);
            simViewButton.setVisibility(View.VISIBLE);
            use_rev = true;
        }
        mode_txt.setText(modeText);
        Log.d(TAG, "End of planning: " + SystemClock.elapsedRealtimeNanos());
    }

    //Calc all similar forward and reverse routes that match the returned route
    private void calcSimilarRoutes(){
        sim_routes = new ArrayList<>();
        rev_sim_routes = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Map<String, ?> map = prefs.getAll();
        TreeMap<String, ?> sortedMap = new TreeMap<>(map);
        for (String name : sortedMap.descendingKeySet()) {
            if(!name.equals("user")){
                String rtPath = prefs.getString(name,"");
                smallRouteSummary temp = new smallRouteSummary();
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
                String smallJson = loadsmallRouteFromFile(rtPath);
                temp = gson.fromJson(smallJson,temp.getClass());
                double percentage = Math.abs(temp.totalDistance - distance_planned)/temp.totalDistance;
                //start point difference
                float[] res = new float[4];
                Location.distanceBetween(start.getPosition().latitude,start.getPosition().longitude,temp.startLoc.latitude,temp.startLoc.longitude,res);
                double start_diff = res[0];
                //calc end point diff
                res = new float[4];
                Location.distanceBetween(end.getPosition().latitude,end.getPosition().longitude,temp.endLoc.latitude,temp.endLoc.longitude,res);
                double end_diff = res[0];
                //Calc diff between start and planned end
                res = new float[4];
                Location.distanceBetween(start.getPosition().latitude,start.getPosition().longitude,temp.endLoc.latitude,temp.endLoc.longitude,res);
                double ps_re_diff = res[0];
                //Calc diff between start and planned end
                res = new float[4];
                Location.distanceBetween(end.getPosition().latitude,end.getPosition().longitude,temp.startLoc.latitude,temp.startLoc.longitude,res);
                double pe_rs_diff = res[0];
                if((percentage < .15) && (end_diff < 40) && (start_diff<40)){
                    sim_routes.add(temp);
                }else if((percentage < .15) && (ps_re_diff < 40) && (pe_rs_diff<40)){
                    rev_sim_routes.add(temp);
                }
            }
        }
        Log.d(TAG, sim_routes.size() + "");
    }

    //Get route summary from file (only contains averages and start and end, no mid points)
    private String loadsmallRouteFromFile(String path){
        String ret = "";

        try {
            InputStream inputStream = openFileInput(path + "s");

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

        return ret;
    }
    //    public void simRouteListClick(View v){
//        TextView temp = (TextView) v;
//        String name = temp.getTag().toString();
//        //routeSummary sum = new routeSummary(prefs.getString(name, ""));
//        //Log.d(TAG,sum.toString());
//        Intent prevIntent = new Intent(this, PrevRouteActivity.class);
//        prevIntent.putExtra("routeName",name);
//        startActivity(prevIntent);
//    }
    //Create list of similar routes to view
    public void simRoutesClick(){
        if(use_rev){
            Globals.sL = rev_sim_routes;
        }else{
            Globals.sL = sim_routes;
        }
        use_rev = false;
        Intent simIntent = new Intent(this, simList.class);
        startActivity(simIntent);
    }

    //Calculate fitness predicitons from previous routes
    //If use_reverse == true, then pull all of the routes from the reverse sim routes list
    public String calcPredText(boolean use_reverse){
        String ret = "";
        if(use_reverse){
            //Display the average of the previous calorie burns and elapsed time
            ret += "Route not traveled in this direction.\nNumber of times this route traveled in reverse: " + rev_sim_routes.size() + "\n";
            double avgCalBurn = 0;
            double avgTime = 0;
            for(smallRouteSummary s : rev_sim_routes){
                avgCalBurn += s.calorieBurn;
                avgTime += s.elapsedTime;
            }
            avgCalBurn /= rev_sim_routes.size();
            avgTime /= rev_sim_routes.size();
            ret += String.format("Average Time Elapsed: %1$.2f sec\nAverage Calorie Burn: %2$.2f cal", avgTime, avgCalBurn);
        }else{
            //Use the forward directions, display average calorie burns and route times
            ret += "Number of times this route stored: " + sim_routes.size() + "\n";
            double avgCalBurn = 0;
            double avgTime = 0;
            for(smallRouteSummary s : sim_routes){
                avgCalBurn += s.calorieBurn;
                avgTime += s.elapsedTime;
            }
            avgCalBurn /= sim_routes.size();
            avgTime /= sim_routes.size();
            int hours = (int) avgTime / 3600;
            double hr_rem = avgTime % 3600;
            int min = (int) hr_rem / 60;
            double sec = hr_rem % 60;
            ret += String.format("Average Time Elapsed: %d hr %d min %.2f sec\nAverage Calorie Burn: %.2f cal", hours, min, sec, avgCalBurn);
        }
        return ret;
    }

}


