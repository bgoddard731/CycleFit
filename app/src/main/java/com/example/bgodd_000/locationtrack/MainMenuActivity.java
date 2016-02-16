package com.example.bgodd_000.locationtrack;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;


public class MainMenuActivity extends AppCompatActivity {

    private userProfile user;
    private String currentView;
    private SharedPreferences prefs;

    public static final String TAG = MainMenuActivity.class.getSimpleName();

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

    private void trackRouteClick(View v){
        Log.d(TAG, "Track Route Click");
        if(user.initialized){
            Intent mapintent = new Intent(this, MapsActivity.class);
            startActivity(mapintent);
        }else{
            Toast.makeText(getApplicationContext(), "User Must be Initialized for Route Tracking", Toast.LENGTH_SHORT).show();
        }

    }

    private void prevRouteClick(View v){
        Log.d(TAG, "Prev Route Click");
        Map<String, ?> map = prefs.getAll();
        Log.d(TAG, map.toString());
        //String temp = (String) map.get("test");
        //routeSummary testRT = new routeSummary(temp);
        //Log.d(TAG, testRT.toString());
    }

    private void planRouteClick(View v){
        Log.d(TAG, "Plan Route Click");
    }


    private void userProfMainClick(View v){
        Log.d(TAG, "User Prof Main Click");
        setContentView(R.layout.user_profile_menu);
        setUserProfMenuEventListeners();
        //load user values
        if(user.initialized){
            fillTextFields();
        }
        //load profile picture
        if(!user.picPath.equals("blank")){
            ImageView img = (ImageView) findViewById(R.id.user_prof_pic);
            loadImageFromStorage(img, user.picPath);
        }


    }
    //Return to main menu
    private void userProfBackToMainClick(){
        Log.d(TAG, "User Prof Back to Main Click");
        boolean valid = validateForm();
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
        }else{
            user = new userProfile();
        }
    }

    private void saveUser(){
            prefs.edit().putString("user",user.toString()).commit();
    }

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
        if(name.equals("")){
            Toast.makeText(getApplicationContext(), "Must enter a name", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.name = name;
        }
        EditText monthtxt = (EditText) findViewById(R.id.month);
        int month = Integer.parseInt(monthtxt.getText().toString());
        if(month < 1 || month > 12){
            Toast.makeText(getApplicationContext(), "Invalid Month", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.month = month;
        }

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

        EditText yeartxt = (EditText) findViewById(R.id.year);
        int year = Integer.parseInt(yeartxt.getText().toString());
        if(year < 1900 || year > 2016){
            Toast.makeText(getApplicationContext(), "Invalid year", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.year = year;
        }

        EditText fttxt = (EditText) findViewById(R.id.ft);
        int feet = Integer.parseInt(fttxt.getText().toString());
        if(feet < 0 || feet > 10){
            Toast.makeText(getApplicationContext(), "Invalid ft field", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.feet = feet;
        }

        EditText intxt = (EditText) findViewById(R.id.in);
        int inches = Integer.parseInt(intxt.getText().toString());
        if(inches < 0 || inches > 11){
            Toast.makeText(getApplicationContext(), "Invalid inches field", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.inches = inches;
        }

        EditText weighttxt = (EditText) findViewById(R.id.lbs);
        int weight = Integer.parseInt(weighttxt.getText().toString());
        if(weight < 0 || weight > 1000){
            Toast.makeText(getApplicationContext(), "Invalid weight", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            user.weight = weight;
        }

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
        user.initialized = true;
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
    //Put it in the picture
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

    @Override
    public void onBackPressed(){
        if(currentView.equals("user")){
            userProfBackToMainClick();
        }else{
            super.onBackPressed();
        }
    }
}
