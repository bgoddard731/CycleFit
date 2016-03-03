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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class PlanActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    //Object to model the map presented by the fragment
    private GoogleMap mMap;
    //Connects device to Google Play services used to display location and map
    private GoogleApiClient mGoogleApiClient;
    private Marker start;
    private Marker end;
    private ArrayList<Marker> waypoints;
    private ArrayList<LatLng> routepoints = new ArrayList<>();
    private ArrayList<smallRouteSummary> sim_routes;
    private long distance_planned;
    private int mode;
    private String currentView;
    //Stores the list of points along the route
    //Tag for Debugging
    public static final String TAG = MapsActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RES_REQUEST = 9000; //9 seconds to connection failure

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.plan_map);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        waypoints = new ArrayList<>();
        mode = 0;
        setModeText();
        setPlanEventListeners();

    }

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
        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
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
            //Drop Start marker
            start = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Route Start")
                    .snippet(latLng.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

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

    private void setModeText(){
        TextView mode_txt = (TextView) findViewById(R.id.mode_text);
        Button start_button = (Button) findViewById(R.id.start_button);
        Button waypoint_button = (Button) findViewById(R.id.waypoint_button);
        Button end_button = (Button) findViewById(R.id.end_button);
        if(mode == 0){
            start_button.setTextColor(Color.YELLOW);
            waypoint_button.setTextColor(Color.WHITE);
            end_button.setTextColor(Color.WHITE);
            mode_txt.setText("Place Start Marker:\nClick Map to designate route beginning");
        }else if(mode == 1){
            start_button.setTextColor(Color.WHITE);
            waypoint_button.setTextColor(Color.YELLOW);
            end_button.setTextColor(Color.WHITE);
            mode_txt.setText("Place Waypoint Markers (Optional):\nClick points on map to add waypoints to route.\nClick waypoint to remove");
        }else{
            start_button.setTextColor(Color.WHITE);
            waypoint_button.setTextColor(Color.WHITE);
            end_button.setTextColor(Color.YELLOW);
            mode_txt.setText("Place End Marker:\nClick Map to designate route end");
        }
    }

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

    private void mapClick(LatLng loc){
        if(mode == 0){
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
            Marker temp = mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title("Waypoint")
                    .snippet(loc.toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            waypoints.add(temp);
        }else if(mode == 2){
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

    private boolean markerClick(Marker m){
        if(m.equals(start) || m.equals(end)){
            return false;
        }else if(mode == 1){
            if(waypoints.contains(m)){
                waypoints.remove(m);
            }
            m.remove();
            return true;
        }else{
            return false;
        }
    }

    private void goButtonClick(View v){
        Button go_button = (Button) findViewById(R.id.go_button);
        if(go_button.getText().equals("Reset")){
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
        if(start == null || end == null){
            Toast.makeText(getApplicationContext(), "Must Place a Start and End Markers!", Toast.LENGTH_SHORT).show();
        }else{
            String url = createDirectionsUrl();
            Log.d(TAG,url);
            mode = 3;
            new JSONconnection().execute(url);
        }
    }

    private String createDirectionsUrl(){
        String url = "";
        url += "https://maps.googleapis.com/maps/api/directions/json?";
        url += "origin="+start.getPosition().latitude + "," + start.getPosition().longitude;
        url += "&destination=" + end.getPosition().latitude + "," + end.getPosition().longitude;
        url += "&mode=bicycling";
        if(waypoints.size() > 0){
            String waypoint_opts = "&waypoints=optimize:true";
            for(int i = 0; i < waypoints.size(); i++){
                waypoint_opts += "|via:"+waypoints.get(i).getPosition().latitude + "," + waypoints.get(i).getPosition().longitude;
            }
            url += waypoint_opts;
        }
        url+="&key=AIzaSyB_zRbAj1yfngb_APNXfRY1DUhXLrQ6rzI";
        return url;
    }

    public class JSONconnection extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            return getJSON(params[0],10000);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Log.d(TAG,result);
            try {
                //Parse Results
                JSONObject res = new JSONObject(result);
                JSONArray routes = res.getJSONArray("routes");
                JSONObject route = routes.getJSONObject(0);
                JSONObject overviewPolylines = route.getJSONObject("overview_polyline");
                String encodedString = overviewPolylines.getString("points");
                routepoints = decodePoly(encodedString);
                Polyline dirs = mMap.addPolyline(new PolylineOptions());
                dirs.setPoints(routepoints);
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

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    return sb.toString();
            }

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

    private void showPlanResults(){
        TextView mode_txt = (TextView) findViewById(R.id.mode_text);
        Button start_button = (Button) findViewById(R.id.start_button);
        Button waypoint_button = (Button) findViewById(R.id.waypoint_button);
        Button end_button = (Button) findViewById(R.id.end_button);
        Button go_button = (Button) findViewById(R.id.go_button);

        start_button.setVisibility(View.INVISIBLE);
        end_button.setVisibility(View.INVISIBLE);
        waypoint_button.setVisibility(View.INVISIBLE);
        go_button.setText("Reset");
        calcSimilarRoutes();
        String modeText = "Planned Distance: " + distance_planned + " meters\n";
        if(sim_routes.size() == 0){
            modeText+= "Number of similar previous routes: 0\nComplete and store more routes of this length to compare in future";
        }else{
            modeText+= "Number of similar previous routes: " + sim_routes.size();
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            Button simViewButton = (Button) findViewById(R.id.simViewButton);
            simViewButton.setVisibility(View.VISIBLE);
        }
        mode_txt.setText(modeText);

    }
    private void calcSimilarRoutes(){
        sim_routes = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Map<String, ?> map = prefs.getAll();
        TreeMap<String, ?> sortedMap = new TreeMap<>(map);
        for (String name : sortedMap.descendingKeySet()) {
            if(!name.equals("user")){
                String rtPath = prefs.getString(name,"");
                smallRouteSummary temp = new smallRouteSummary(loadsmallRouteFromFile(rtPath));
                double percentage = Math.abs(temp.totalDistance - distance_planned)/temp.totalDistance;
                if(percentage < .05){
                    sim_routes.add(temp);
                }
            }
        }
        Log.d(TAG, sim_routes.size() + "");
    }

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
    public void simRouteListClick(View v){
        TextView temp = (TextView) v;
        String name = temp.getTag().toString();
        //routeSummary sum = new routeSummary(prefs.getString(name, ""));
        //Log.d(TAG,sum.toString());
        Intent prevIntent = new Intent(this, PrevRouteActivity.class);
        prevIntent.putExtra("routeName",name);
        startActivity(prevIntent);
    }

    public void simRoutesClick(){
        Globals.sL = sim_routes;
        Intent simIntent = new Intent(this, simList.class);
        startActivity(simIntent);
    }

}


