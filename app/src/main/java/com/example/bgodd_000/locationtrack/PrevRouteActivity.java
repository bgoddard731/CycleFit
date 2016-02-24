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

        import java.util.ArrayList;
        import java.util.Date;
        import java.util.LinkedList;
        import java.util.List;

public class PrevRouteActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    //Object to model the map presented by the fragment
    private GoogleMap mMap;
    //Connects device to Google Play services used to display location and map
    private GoogleApiClient mGoogleApiClient;
    private routeSummary rt;
    private ArrayList<LatLng> routepoints = new ArrayList<>();
    private int index;
    private int MAXINDEX = 1;
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
            String sum_name = extras.getString("routeName");
            SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            String rtString = prefs.getString(sum_name,"");
            rt = new routeSummary(rtString);
        }else{
            //rt = extras.getParcelable("routeData");
            rt = Globals.summary;
        }

        TextView sumText = (TextView) findViewById(R.id.route_sum_text);
        sumText.setText(String.format("Route Summary:\nTotal Distance Traveled: %1$.2fm\nTotal time: %2$.2fsec\n Average Speed: %3$.2fm/s", rt.totalDistance, rt.elapsedTime, rt.avgSpeed));
        for(routeNode n: rt.points){
            routepoints.add(n.loc);
        }
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
        index = 0;
        Log.d(TAG,"Post RT point iteration: "+SystemClock.currentThreadTimeMillis());
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
        Log.d(TAG, SystemClock.currentThreadTimeMillis() + "done");

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
            mMap.moveCamera(CameraUpdateFactory.newLatLng(routepoints.get(0)));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(13));
        }
        TextView sumText = (TextView) findViewById(R.id.route_sum_text);
        switch (index){
            case 0:
                if(!routepoints.isEmpty()){
                    Polyline route = mMap.addPolyline(new PolylineOptions());
                    route.setPoints(routepoints);
                }
                sumText.setText(String.format("Route Summary:\nTotal Distance Traveled: %1$.2fm\nTotal time: %2$.2fsec\n Average Speed: %3$.2fm/s", rt.totalDistance, rt.elapsedTime, rt.avgSpeed));
                break;
            case 1:
                if(!routepoints.isEmpty()){
                    initializeSpeedMap();
                }

                break;
        }
    }
    private void initializeSpeedMap(){
        double minSpeed = 99999;
        double maxSpeed = 0;
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
                curr_range = calcSpeedRange(n.speed);
            }
        }
        pointList.add(temp);
        ranges.add(curr_range);
        for(int i = 0; i < pointList.size(); i++){
            Polyline route = mMap.addPolyline(new PolylineOptions());
            route.setPoints(pointList.get(i));
            route.setColor(calcSpeedColor(ranges.get(i)));
        }
        TextView sumText = (TextView) findViewById(R.id.route_sum_text);
        sumText.setText(String.format("Speed Summary:\nAverage Speed: %1$.2fm\nMinimum Speed: %2$.2fsec\n Maximum Speed: %3$.2fm/s", rt.avgSpeed, minSpeed, maxSpeed));

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
    private int calcSpeedColor(int range){
        switch (range){
            case 0:
                return Color.BLACK;
            case 1:
                return Color.MAGENTA;
            case 2:
                return Color.BLUE;
            case 3:
                return Color.GREEN;
            case 4:
                return Color.YELLOW;
            case 5:
                return Color.RED;
            default: return Color.BLACK;
        }
    }

}
