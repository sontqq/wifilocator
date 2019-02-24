package com.sontme.esp.getlocation;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Global extends Application{
    // actual global variables
    public static String longitude = "0";
    public static String latitude = "0";
    public static String altitude = "0";
    public static String bearing = "0";
    public static String time;
    public static String distance = "0.00";
    public static String ipaddress = "127.0.0.1";
    public static String address = "Not available";
    public static String speed = "0.00";
    public static String accuracy = "0";
    public static String lastSSID;
    public static String lastNearby;
    public static int count = 0;
    public static String provider = "Not available";
    public static String maptype;
    public static int bytes;
    public static boolean wanarun = true;
    public static String initLat = "0";
    public static String initLong = "0";
    public static int nearbyCount;
    public static List<String> uniqueAPS = new ArrayList<>();
    public static Queue<String> queue = new LinkedList<String>();


    public static long getUsedMemorySize() {

        long freeSize = 0L;
        long totalSize = 0L;
        long usedSize = -1L;
        try {
            Runtime info = Runtime.getRuntime();
            freeSize = info.freeMemory();
            totalSize = info.totalMemory();
            usedSize = totalSize - freeSize;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return usedSize / 1024 / 1024;

    }


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

    // Static methods
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
