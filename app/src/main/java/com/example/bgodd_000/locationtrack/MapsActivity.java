package com.example.bgodd_000.locationtrack;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.nfc.Tag;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;


public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    //Constants to use pre integration
    int DEFALUT_HR = 80;
    double DEFALUT_RPM = 60;
    double DEFAULT_INCLINE = 0;
    int MAXROUTES = 3;


    //Object to model the map presented by the fragment
    private GoogleMap mMap;

    //Global vars for distance, speed, and time tracking
    private double distance_traveled = 0;
    private double[] speeds = new double[4];
    private double curr_speed = 0;
    private int speed_count = 0;
    private double start_time = -1;
    private double prev_time = 0;
    //Previous location used to compare locations between consecutive samples
    private Location returnLoc;
    private Location prev_loc;
    private boolean running = false;
    private boolean saveOptions = false;
    private routeSummary rt = new routeSummary();

    //Bluetooth connection variables
    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothDevice cfDevice;
    private BluetoothSocket btSocket;
    private int REQUEST_ENABLE_BT = 1;
    final byte delimiter = 10;
    int readBufferPosition = 0;
    private Handler btHandler;
    private int prevHR = 0;
    private int prevRPM = 0;
    private double prevInc = 0;

    //Connects device to Google Play services used to display location and map
    private GoogleApiClient mGoogleApiClient;
    //Class to handle the location requests to the fused API
    private LocationRequest mLocationRequest;
    //Stores the list of points along the route
    private List<LatLng> routepoints = new LinkedList<>();
    //Tag for Debugging
    public static final String TAG = MapsActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RES_REQUEST = 9000; //9 seconds to connection failure

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        //mapFragment.getMapAsync(this);
        //Connect to google play services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        //Set parameters for the location requests
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(500) //Interval of internal requests
                .setFastestInterval(500); //Fastest interval of requests app can process

        //Create button click listeners
        Button startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startButtonClick();
            }
        });

        Button stopButton = (Button) findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopButtonClick();
            }
        });

        //Connect to bluetooth device
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "This device does not support bluetooth. You will not be able to user Cycle Fit", Toast.LENGTH_SHORT).show();
            onBackPressed();
        }
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        }else{
           connectCFDevice();
        }

    }

    @Override
    protected void onResume(){
        //called after onStart or if activity is moved from background to foreground
        super.onResume();
        setUpMapIfNeeded();
        if(!mGoogleApiClient.isConnected()){
            mGoogleApiClient.connect();
        }
        if(btSocket != null){
            if(!btSocket.isConnected()){
                connectCFDevice();
            }
        }
    }

    //Connect map if necessary
    private void setUpMapIfNeeded(){
        if (mMap == null){
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
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
        //Pull last known location for map initialization
        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
       // LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        if(loc != null) {
            //If connected, zoom camera to current location
            double currentLat = loc.getLatitude();
            double currentLong = loc.getLongitude();
            LatLng latLng = new LatLng(currentLat,currentLong);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        }
    }
    //Function to process a new location point
    private void handleNewLocation(Location loc){
        //If this is first point of a route, prev_loc will be null.
        //Update to current point
        if(prev_loc == null) {
            prev_loc = loc;
        }

        //log location for debug
        //Log.d(TAG, loc.toString());
        //Pull necessary data from location object
        double currentLat = loc.getLatitude();
        double currentLong = loc.getLongitude();
        double ctime = loc.getElapsedRealtimeNanos();

        //Update Distance Traveled and speed:
        double leg = loc.distanceTo(prev_loc); //calc distance
        if(leg > 1) {//Filter out small distances
            distance_traveled += leg;
            speeds[speed_count] = leg / ((ctime - prev_time) / (1000000000));
            //Clear the previous polyline
            mMap.clear();
            //Make new lat lang from the location
            LatLng latLng = new LatLng(currentLat,currentLong);
            routepoints.add(latLng); //Add to global list
            //Update and redraw the polyline
            Polyline route = mMap.addPolyline(new PolylineOptions());
            route.setPoints(routepoints);
            //Update previous values for next location
            prev_time = ctime;
            prev_loc = loc;
            rt.addPoint(new routeNode(prevHR , prevRPM, prevInc, latLng, distance_traveled, curr_speed, ctime));
        }else {
            speeds[speed_count] = 0;
        }
        speed_count++;
        //Calculate average speed from last 4 legs
        //Not sure if this is the best implementation method
        if(speed_count == 4){
            double curr_spd = 0;
            speed_count = 0;
            for(int i = 0; i < 4; i++){
                curr_spd += speeds[i];
            }
            curr_spd /= 4;
            curr_speed = curr_spd;
        }

        //update the display
        final TextView resultText = (TextView) findViewById(R.id.result_text);
        resultText.setText(String.format("Tracking in Progress:\nCurrent Distance Traveled: %1$.2fm\nCurrent Estimated Speed: %2$.2fm/s", distance_traveled, curr_speed));
    }
    //Disconnection from service
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }
    //unable to connect
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
    //Handle new locations
    @Override
    public void onLocationChanged(Location location) {
        returnLoc = location;
        //Get sensor reading
        new Thread(new btThread()).start();
        //handleNewLocation(location);
    }

    //Start location tracking
    private void startButtonClick(){
        //If start button is being used as the discard option
        if(saveOptions) {
            saveOptions = false;
            //Discard data and reset
            Button stopButton = (Button) findViewById(R.id.stop_button);
            stopButton.setText("Stop");
            Button startButton = (Button) findViewById(R.id.start_button);
            startButton.setText("Start");
            discardRouteInfo();
        }else{
            //Start tracking a route
            mMap.clear();
            rt = new routeSummary();
            TextView resultText = (TextView) findViewById(R.id.result_text);
            resultText.setText("Tracking in Progress\nCurrent Distance Traveled: 0m\nCurrent Estimated Speed: 0m/s");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            start_time = SystemClock.elapsedRealtimeNanos();
            rt.start = new Date();
            running = true;
            //Debug
//            debugRouteGenerator test = new debugRouteGenerator();
//            rt = test.genRT;
//            for(routeNode n: rt.points){
//                routepoints.add(n.loc);
//            }
//            Polyline route = mMap.addPolyline(new PolylineOptions());
//            route.setPoints(routepoints);
        }
    }
    private void stopButtonClick(){
        double stop_time = SystemClock.elapsedRealtimeNanos();
        if(mGoogleApiClient.isConnected()){
            //Stop tracking location
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        //reset list of locations
        routepoints = new LinkedList<>();
        if(saveOptions){
            //If stop is being used as the save button
            saveOptions = false;
            Button stopButton = (Button) findViewById(R.id.stop_button);
            stopButton.setText("Stop");
            Button startButton = (Button) findViewById(R.id.start_button);
            startButton.setText("Start");
            saveRouteInfo();
        }else if(running){
            rt.end = new Date();
            //Calculate summary data
            double route_time = (stop_time - start_time)/1000000000;
            rt.elapsedTime = route_time;
            rt.totalDistance = distance_traveled;
           double avg_speed = distance_traveled / route_time;

            if(distance_traveled == 0 || route_time == 0) {
                avg_speed = 0;
            }
            rt.avgSpeed = avg_speed;
            TextView resultText = (TextView) findViewById(R.id.result_text);
            resultText.setText(String.format("Route Ended:\nTotal Distance Traveled: %1$.2fm\nTotal time: %2$.2fsec\n Average Speed: %3$.2fm/s", distance_traveled, route_time, avg_speed));
            //Log.d(TAG, rt.toString());
            //reset the global data
            distance_traveled = 0;
            start_time = 0;
            prev_time = 0;
            curr_speed = 0;
            for(int i = 0; i < 4; i++){
                speeds[i] = 0;
            }
            prev_loc = null;
            //Set up save/discard option
            running = false;
            saveOptions = true;
            Button stopButton = (Button) findViewById(R.id.stop_button);
            stopButton.setText("Save?");
            Button startButton = (Button) findViewById(R.id.start_button);
            startButton.setText("Discard?");
        }

    }
    public void saveRouteInfo(){

        int avgHR = 0;
        double avgInc = 0;
        double avgRPM = 0;
        for(routeNode n: rt.points){
            avgHR += n.hr;
            avgInc += n.incline;
            avgRPM += n.rpm;
        }
        avgHR /= rt.points.size();
        avgInc /= rt.points.size();
        avgRPM /= rt.points.size();

        rt.avgHR = avgHR;
        rt.avgIncline = avgInc;
        rt.avgRPM = avgRPM;

        rt.calorieBurn = 150;

        //save the route;
        Log.d(TAG, "Start of save: " + SystemClock.elapsedRealtimeNanos());
        String name = rt.end.getTime() + "";
        //Log.d(TAG, "Send to Prev: " + SystemClock.elapsedRealtimeNanos());
        //Asynchronosly save the route on a background thread
        new Thread(new Runnable() {
            public void run() {
                SharedPreferences prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                saveRouteToFile();
                prefs.edit().putString("" + rt.end.getTime(), rt.end.getTime()+".txt").apply();
                Map<String, ?> map = prefs.getAll();
                //Delete a route if number exceeds maximum stored
                if(map.size() > MAXROUTES + 1){
                    TreeMap<String,?> sortedMap= new TreeMap<>(map);
                    String name = sortedMap.firstKey();
                    String path = (String) sortedMap.get(name);
                    deleteRouteFile(path);
                    prefs.edit().remove(name).apply();
                }
            }
        }).start();
        //Set up a previous route summary
        Intent prevIntent = new Intent(this, PrevRouteActivity.class);
        prevIntent.putExtra("routeName", name);
        Globals.summary = rt;
        prevIntent.putExtra("fromTrack", true);
        startActivity(prevIntent);
        if(btSocket.isConnected()){
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Reset map for return from summary
        mMap.clear();
        TextView resultText = (TextView) findViewById(R.id.result_text);
        resultText.setText("New Route\nDistance Traveled: 0m/s");

    }
    private void discardRouteInfo(){
        rt = new routeSummary();
        mMap.clear();
        TextView resultText = (TextView) findViewById(R.id.result_text);
        resultText.setText("New Route\nDistance Traveled: 0m/s");
    }

    public void saveRouteToFile(){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        //Save short summary for planning
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(cw.openFileOutput(rt.end.getTime()+".txts", Context.MODE_PRIVATE));
            outputStreamWriter.write(rt.shortToString());
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        //Save full summary
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(cw.openFileOutput(rt.end.getTime()+".txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(rt.toString());
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void deleteRouteFile(String path){
        File file = new File(path);
        boolean deleted = file.delete();
    }

    private void connectCFDevice(){
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals(Globals.user.deviceName)){
                    cfDevice = device;
                }
            }
        }
        Log.d(TAG, ""+cfDevice.toString());
        btHandler = new Handler();
        new ConnectThread(cfDevice).run();
    }
    private class ConnectThread extends Thread {

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) { }
            btSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                btSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    btSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket();
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) { }
        }
    }

    private void manageConnectedSocket(){
        //new Thread(new btThread()).start();
    }

    final class btThread implements Runnable {
        private final InputStream mmInStream;
        public btThread() {
            InputStream tmpIn = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = btSocket.getInputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
        }
        public void run() {
            if(!mBluetoothAdapter.isEnabled()){
                Log.d("workerThread", "BT is not enabled for a settings worker thread!");
                return;
            }
            while(!Thread.currentThread().isInterrupted() && mBluetoothAdapter.isEnabled()) {
                int bytesAvailable;
                boolean workDone = false;

                try {
                   // Log.d("refreshThread", "attempting btConnection for receiving");
                    bytesAvailable = mmInStream.available();
                    if(bytesAvailable > 0) {

                        byte[] packetBytes = new byte[bytesAvailable];
                        Log.d("bt receive","bytes available:" + bytesAvailable);
                        byte[] readBuffer = new byte[1024];
                        mmInStream.read(packetBytes);

                        for(int i=0;i<bytesAvailable;i++) {
                            byte b = packetBytes[i];
                            if(b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;
                                Log.d(TAG, "String that was received in thread: " + data);

                                //The variable data now contains a full sensor reading
                                btHandler.post(new Runnable() {
                                    public void run() {
                                        //setTexts based upon data
                                        parseSensorData(data);
                                    }
                                });
                                workDone = true;
                                break;
                            }
                            else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                        if (workDone == true){
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    //Toast.makeText(getApplicationContext(), "Refresh error: !", Toast.LENGTH_SHORT).show();
                }
                catch (NullPointerException e) {
                    e.printStackTrace();
//                        Toast.makeText(getApplicationContext(), "Beacon error: Not connected!", Toast.LENGTH_SHORT).show();
                }
            }
            Log.d("workerThread", "Exiting worker thread");
        }
    }

    public void parseSensorData(String reading){
        Log.d(TAG, reading);
        try{
            String[] parts = reading.split(",");
            prevHR = Integer.parseInt(parts[0]);
            prevRPM = Integer.parseInt(parts[1]);
            prevInc = Double.parseDouble(parts[2]);
            Log.d(TAG, "New Sensor Reading: " + prevHR + ", " + prevRPM + ", " + prevInc);
        }catch(Exception e){
            Log.d(TAG, "ERROROROROR");
        }
        handleNewLocation(returnLoc);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                connectCFDevice();
            }
            else{
                Toast.makeText(this, "oops. Something went wrong. Try Again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onBackPressed(){
        if(btSocket.isConnected()){
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onBackPressed();
    }
    @Override
    protected void onStop(){
        if(btSocket.isConnected()){
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onStop();
    }
}
