package com.example.bgodd_000.locationtrack;

import android.app.ProgressDialog;
import android.content.Context;
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
import android.view.View;
import android.widget.Button;
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

import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

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
    private int mode;
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
        }else{
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
        if(start == null || end == null){
            Toast.makeText(getApplicationContext(), "Must Place a Start and End Markers!", Toast.LENGTH_SHORT).show();
        }else{
            String url = createDirectionsUrl();
            Log.d(TAG,url);

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
        url+="&key=AIzaSyC7oVSbp-YRTOD3gmdeoiq827GTkJkkokM";
        return url;
    }

}
