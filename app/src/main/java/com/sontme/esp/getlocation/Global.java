package com.sontme.esp.getlocation;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

public class Global extends Application{
    // actual global variables
    public static String longitude;
    public static String latitude;
    public static String altitude;
    public static String bearing;
    public static String time;
    public static String distance;
    public static String address;
    public static String speed;
    public static String accuracy;
    public static String lastSSID;
    public static String lastNearby;
    public static int count = 0;
    public static String provider;
    public static String maptype;
    public static int bytes;
    public static boolean wanarun = true;
    public static String initLat;
    public static String initLong;
    public static int nearbyCount;
    public static List<String> uniqueAPS = new ArrayList<>();


    public void setCount(){
        this.count++;
    }

    public String getVersionInfo(Activity a, int element){
        return "Version: " + String.valueOf(BuildConfig.VERSION_NAME)
                + " Build: " + String.valueOf(BuildConfig.VERSION_CODE);

    }

    public String getInitLat(){
        if(initLat != null) {
            return initLat;
        }
        else{
            return "47.936291";
        }
    }
    public String getInitLong(){
        if(initLong != null) {
            return initLong;
        }
        else{
            return "20.367531";
        }
    }
    public String getLat(){
        if(latitude != null) {
            return latitude;
        }
        else{
            return "47.936291";
        }
    }
    public String getLong()
    {
        if(longitude != null) {
            return longitude;
        }
        else{
            return "20.367531";
        }
    }

    ///


    public static String getInitLat_(){
        if(initLat != null) {
            return initLat;
        }
        else{
            return "47.936291";
        }
    }
    public static String getInitLong_(){
        if(initLong != null) {
            return initLong;
        }
        else{
            return "20.367531";
        }
    }
    public static String getLat_(){
        if(latitude != null) {
            return latitude;
        }
        else{
            return "47.936291";
        }
    }
    public static String getLong_()
    {
        if(longitude != null) {
            return longitude;
        }
        else{
            return "20.367531";
        }
    }

}
