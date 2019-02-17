package com.sontme.esp.getlocation;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
    public static String aplist;

    public void setCount(){
        this.count++;
    }

    public String getVersionInfo(Activity a, int element){
        return "Version: " + String.valueOf(BuildConfig.VERSION_NAME)
                + " Build: " + String.valueOf(BuildConfig.VERSION_CODE);

    }
}
