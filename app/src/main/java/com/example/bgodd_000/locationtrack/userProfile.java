package com.example.bgodd_000.locationtrack;

import android.content.Context;

import java.io.FileOutputStream;

//Structure to hold user profile information
public class userProfile {
    public String name, picPath, deviceName;
    public boolean male;
    public int month, day, year, feet, inches, weight;
    public boolean initialized;

    public userProfile(){
        name = "";
        picPath = "blank";
        male = true;
        month = 0;
        day = 0;
        year = 0;
        feet = 0;
        inches = 0;
        weight = 0;
        deviceName = "";
        initialized = false;
    }
    //create from a formated string
    public userProfile(String contents){
        String[] parts = contents.split(",");
        name = parts[0];
        male = Boolean.parseBoolean(parts[1]);
        month = Integer.parseInt(parts[2]);
        day = Integer.parseInt(parts[3]);
        year = Integer.parseInt(parts[4]);
        feet = Integer.parseInt(parts[5]);
        inches = Integer.parseInt(parts[6]);
        weight = Integer.parseInt(parts[7]);
        deviceName = parts[8];
        picPath = parts[9];
        initialized = true;
    }
    //print to a string for storage
    public String toString(){
        return name + "," + male + "," + month + "," + day + "," + year + "," + feet + "," + inches + "," + weight + "," + deviceName + "," + picPath;
    }

}