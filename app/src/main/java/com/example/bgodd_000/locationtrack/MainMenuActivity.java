package com.example.bgodd_000.locationtrack;

import android.app.ActionBar;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.Tag;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ActionMenuView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;


public class MainMenuActivity extends AppCompatActivity {
    //Stores user info (height, weight, age, etc)
    private userProfile user;
    private String currentView;
    private SharedPreferences prefs;

    public static final String TAG = MapsActivity.class.getSimpleName();

    //Used to override the back button press for staying within the same activity
    //on return from the list of previous routes or user profile pages (All contained within same activity)
    @Override
    public void setContentView(int layoutResID) {
        View view = getLayoutInflater().inflate(layoutResID, null);
        //tag is needed for pressing back button to go back to splash screen
        currentView = (String) view.getTag();
        super.setContentView(view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        setMainMenuEventListeners();
        //Load user info
        uploadUser();
        if(!user.picPath.equals("blank")){
            ImageView img = (ImageView) findViewById(R.id.userPicMain);
            loadImageFromStorage(img, user.picPath);
        }
    }

    //Called when the track route opiton selected.
    //Navigates to the track route page
    private void trackRouteClick(View v){
        Log.d(TAG, "Track Route Click");
        if(user.initialized){
            //User must be initialized to track a route
            Intent mapintent = new Intent(this, MapsActivity.class);
            startActivity(mapintent);
            //Debug - nonBTversion of routeTracking:
//            Intent mapNBTintent = new Intent(this, MapsNBTActivity.class);
//            startActivity(mapNBTintent);
            Globals.user = user;


        }else{
            Toast.makeText(getApplicationContext(), "User Must be Initialized for Route Tracking", Toast.LENGTH_SHORT).show();
        }
        //Debug - used to load routes into memory, change i to set number
//        debugRouteGenerator test = new debugRouteGenerator();
//        routeSummary rt = test.genRT;
//        Log.d(TAG, "Created");
//        Log.d(TAG, "Start of Storage: " + SystemClock.currentThreadTimeMillis());
//        JsonSerializer<Date> ser = new JsonSerializer<Date>() {
//            @Override
//            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext
//                    context) {
//                return src == null ? null : new JsonPrimitive(src.getTime());
//            }
//        };
//
//        Gson gson = new GsonBuilder()
//                .registerTypeAdapter(Date.class, ser).create();
//        String jsonRT = gson.toJson(rt);
//        smallRouteSummary smallRT = new smallRouteSummary(rt.shortToString());
//        String smallJsonRT = gson.toJson(smallRT);
//        for(int i = 0; i < 20; i++){
//            ContextWrapper cw = new ContextWrapper(getApplicationContext());
//            try {
//                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(cw.openFileOutput((test.genRT.end.getTime() + i)+".rt", Context.MODE_PRIVATE));
//                outputStreamWriter.write(jsonRT);
//                outputStreamWriter.close();
//                outputStreamWriter = new OutputStreamWriter(cw.openFileOutput((test.genRT.end.getTime() + i)+".rts", Context.MODE_PRIVATE));
//                outputStreamWriter.write(smallJsonRT);
//                outputStreamWriter.close();
//            }
//            catch (IOException e) {
//                Log.e("Exception", "File write failed: " + e.toString());
//            }
//            prefs.edit().putString((test.genRT.end.getTime() + i) + "", (test.genRT.end.getTime() + i) + ".rt").commit();
//            Log.d(TAG, "After Storage: " + i);
//        }

    }
    //Used to navigate to a list of previous routes stored in memory to allow the user to view a previous route
    private void prevRouteClick(View v){
        //Log.d(TAG, "Prev Route Click");
        //Log.d(TAG, "Start of List: " + SystemClock.elapsedRealtimeNanos());
        //Create the list of previous routes
        Map<String, ?> map = prefs.getAll();
        TreeMap<String, ?> sortedMap = new TreeMap<>(map);
        //Update the view
        setContentView(R.layout.stored_route_list);
        for (String name : sortedMap.descendingKeySet()) {
            if(!name.equals("user")){
                //Add a new element to the list of previous routes
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                TextView entry = new TextView(this);
                entry.setLayoutParams(lp);
                Date temp = new Date(Long.parseLong(name));
                entry.setText(temp.toString());
                entry.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                entry.setTag(name);
                LinearLayout ll = (LinearLayout) findViewById(R.id.route_list);
                ll.addView(entry);
                entry.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        routeListClick(v);
                    }
                });
            }
        }
        //Debug log time tracking
        //Log.d(TAG, "End of List: " + SystemClock.elapsedRealtimeNanos());
        //Log.d(TAG, SystemClock.currentThreadTimeMillis()+"");
        //String temp = (String) map.get("test");
        //routeSummary testRT = new routeSummary(temp);
        //Log.d(TAG, testRT.toString());
    }
    //navigate to the plan route page
    private void planRouteClick(View v){
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if(!netInfo.isConnected()){
            Toast.makeText(getApplicationContext(), "An internet connection is required to use this feature.", Toast.LENGTH_SHORT).show();
        }else{
            Intent plan_intent = new Intent(this, PlanActivity.class);
            startActivity(plan_intent);
        }
        //Log.d(TAG, "Plan Route Click");

    }

    //Used when the user navigates to the user profile page, sets up the new view
    //and adds in all the event listners
    private void userProfMainClick(View v){
        Log.d(TAG, "User Prof Main Click");
        setContentView(R.layout.user_profile_menu);
        setUserProfMenuEventListeners();
        //load user values
        if(user.initialized){
            fillTextFields();
        }
        //load profile picture if it exists
        if(!user.picPath.equals("blank")){
            ImageView img = (ImageView) findViewById(R.id.user_prof_pic);
            loadImageFromStorage(img, user.picPath);
        }


    }
    //Return to main menu from user profile page
    //must have all fields completed (will be validated
    private void userProfBackToMainClick(){
        Log.d(TAG, "User Prof Back to Main Click");
        //validate the form
        boolean valid = validateForm();
        //return and reset event listners if valid
        if(valid){
            setContentView(R.layout.activity_main_menu);
            saveUser();
            setMainMenuEventListeners();
            if(!user.picPath.equals("blank")){
                ImageView img = (ImageView) findViewById(R.id.userPicMain);
                loadImageFromStorage(img, user.picPath);
            }
        }
    }
    //Adds the event listeners for the objects of the main menu
    private void setMainMenuEventListeners(){
        TextView trackRoute = (TextView) findViewById(R.id.track_option);
        trackRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                trackRouteClick(v);
            }
        });

        TextView prevRoute = (TextView) findViewById(R.id.prev_routes_options);
        prevRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prevRouteClick(v);
            }
        });

        TextView planRoute = (TextView) findViewById(R.id.plan_route_option);
        planRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                planRouteClick(v);
            }
        });

        ImageView userProfMain = (ImageView) findViewById(R.id.userPicMain);
        userProfMain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                userProfMainClick(v);
            }
        });
    }
    //Adds the event listeners for the objects of the user profile menu
    private void setUserProfMenuEventListeners(){
        ImageView userProfBackToMain = (ImageView) findViewById(R.id.user_prof_back_arrow);
        userProfBackToMain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                userProfBackToMainClick();
            }
        });
        ImageView profPic = (ImageView) findViewById(R.id.user_prof_pic);
        profPic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setUserPic();
            }
        });



    }
    //Get the user shared Preferences
    private void uploadUser(){
        prefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        if(prefs.contains("user")){
            user = new userProfile(prefs.getString("user","Name,true,0,0,0,0,0,0,"));
            Globals.user = user;
        }else{
            user = new userProfile();
        }
    }
    //Save the user info into shared preferences
    private void saveUser(){
            prefs.edit().putString("user",user.toString()).commit();
    }

    //If user is already initialized, add the user info to the page when the user navigates to
    //user profile page
    private void fillTextFields(){
        //Fill the text fields from the user profile
        EditText nametxt = (EditText) findViewById(R.id.username);
        nametxt.setText(user.name);

        EditText monthtxt = (EditText) findViewById(R.id.month);
        monthtxt.setText(user.month + "");

        EditText daytxt = (EditText) findViewById(R.id.day);
        daytxt.setText(user.day + "");

        EditText yeartxt = (EditText) findViewById(R.id.year);
        yeartxt.setText(user.year + "");

        EditText fttxt = (EditText) findViewById(R.id.ft);
        fttxt.setText(user.feet + "");

        EditText intxt = (EditText) findViewById(R.id.in);
        intxt.setText(user.inches + "");

        EditText lbstxt = (EditText) findViewById(R.id.lbs);
        lbstxt.setText(user.weight + "");

        EditText devTxt = (EditText) findViewById(R.id.deviceNameTxt);
        devTxt.setText(user.deviceName);

        RadioButton maleButton = (RadioButton) findViewById(R.id.male_button);
        RadioButton femaleButton = (RadioButton) findViewById(R.id.female_button);
        if(user.male){
            maleButton.setChecked(true);
            femaleButton.setChecked(false);
        }else{
            femaleButton.setChecked(true);
            maleButton.setChecked(false);
        }
    }
    //Validate the userprofile form
    private boolean validateForm(){
        EditText nametxt = (EditText) findViewById(R.id.username);
        String name = nametxt.getText().toString();
        //Name must be filled
        if(name.equals("")){
            Toast.makeText(getApplicationContext(), "Must enter a name", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.name = name;
        }
        //Validate month
        EditText monthtxt = (EditText) findViewById(R.id.month);
        int month = Integer.parseInt(monthtxt.getText().toString());
        if(month < 1 || month > 12){
            Toast.makeText(getApplicationContext(), "Invalid Month", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.month = month;
        }
        //Validate day field
        EditText daytxt = (EditText) findViewById(R.id.day);
        int day = Integer.parseInt(daytxt.getText().toString());
        boolean dayvalid = true;
        if(day < 0){
            dayvalid = false;
        }
        switch (user.month){
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                if(day > 31){
                    dayvalid = false;
                }
                break;
            case 2:
                if(day > 29){
                    dayvalid = false;
                }
            default:
                if(day > 30){
                    dayvalid = false;
                }
        }
        if(!dayvalid){
            Toast.makeText(getApplicationContext(), "Invalid Day", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.day = day;
        }
        //validate year field
        EditText yeartxt = (EditText) findViewById(R.id.year);
        int year = Integer.parseInt(yeartxt.getText().toString());
        if(year < 1900 || year > 2016){
            Toast.makeText(getApplicationContext(), "Invalid year", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.year = year;
        }
        //validate feet field
        EditText fttxt = (EditText) findViewById(R.id.ft);
        int feet = Integer.parseInt(fttxt.getText().toString());
        if(feet < 0 || feet > 10){
            Toast.makeText(getApplicationContext(), "Invalid ft field", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.feet = feet;
        }

        //validate inches field
        EditText intxt = (EditText) findViewById(R.id.in);
        int inches = Integer.parseInt(intxt.getText().toString());
        if(inches < 0 || inches > 11){
            Toast.makeText(getApplicationContext(), "Invalid inches field", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.inches = inches;
        }

        //validate weight field
        EditText weighttxt = (EditText) findViewById(R.id.lbs);
        int weight = Integer.parseInt(weighttxt.getText().toString());
        if(weight < 0 || weight > 1000){
            Toast.makeText(getApplicationContext(), "Invalid weight", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.weight = weight;
        }

        //validate male/female buttons
        RadioButton maleButton = (RadioButton) findViewById(R.id.male_button);
        RadioButton femaleButton = (RadioButton) findViewById(R.id.female_button);

        if(!maleButton.isChecked() && !femaleButton.isChecked()){
            Toast.makeText(getApplicationContext(), "Select a gender", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            if(maleButton.isChecked()){
                user.male = true;
            }else{
                user.male = false;
            }
        }

        //Validate device name field
        EditText devTxt = (EditText) findViewById(R.id.deviceNameTxt);
        String devName = devTxt.getText().toString();
        if(devName.equals("")){
            Toast.makeText(getApplicationContext(), "Must enter the name of the device to initialize App", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.deviceName = devName;
        }
        user.initialized = true;
        Globals.user = user;
        return true;
    }

    //Get picture from gallery
    private void setUserPic(){
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    //Put the new user profile picture into the picture field on return from the gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Log.d(TAG, String.valueOf(bitmap));

                ImageView profPic = (ImageView) findViewById(R.id.user_prof_pic);
                profPic.setImageBitmap(bitmap);
                user.picPath = saveToInternalStorage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Store Image to internal storage
    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return directory.getAbsolutePath();
    }

    //Get an image from local storage to add to Imageview field
    private void loadImageFromStorage(ImageView img, String path) {

        try {
            File f=new File(path, "profile.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            img.setImageBitmap(b);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

    }

    //Pull up the route summary for the route clicked from the list of previous routes
    public void routeListClick(View v){
        TextView temp = (TextView) v;
        String name = temp.getTag().toString();
        //routeSummary sum = new routeSummary(prefs.getString(name, ""));
        //Log.d(TAG,sum.toString());
        Log.d(TAG, "Start of Load: " + SystemClock.elapsedRealtimeNanos());
        Intent prevIntent = new Intent(this, PrevRouteActivity.class);
        prevIntent.putExtra("routeName",name);
        startActivity(prevIntent);
    }

    //If not on the main menu page, override the back button to return to main menu and not kill activity
    @Override
    public void onBackPressed(){
        if(currentView.equals("user")){
            userProfBackToMainClick();
        }else if(currentView.equals("route_list")) {
            setContentView(R.layout.activity_main_menu);
            setMainMenuEventListeners();
            if(!user.picPath.equals("blank")){
                ImageView img = (ImageView) findViewById(R.id.userPicMain);
                loadImageFromStorage(img, user.picPath);
            }
        }else{
            super.onBackPressed();
        }
    }
}
