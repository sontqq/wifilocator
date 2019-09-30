package com.sontme.esp.getlocation;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.content.ContextCompat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Global extends Application{
    // actual global variables
    /*
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
    public static String nearbyCount;
    public static int GpsInView = 0;
    public static int GpsInUse = 0;
    public static int count = 0;
    public static String provider = "Not available";
    public static String maptype;
    public static int bytes;
    public static boolean wanarun = true;
    public static String initLat = "0";
    public static String initLong = "0";
    public static boolean isUploading = false;
    public static List<String> uniqueAPS = new ArrayList<>();
    public static Queue<String> queue = new LinkedList<String>();
    public static String csvSize = "0";
    public static String zipSize = "0";
    public static String googleAccount;

    public static List<String> urlList_failed = new ArrayList<String>();
    public static List<String> urlList_successed = new ArrayList<String>();
    */

    public static String getCompleteAddressString(Context c, double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(c, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder();

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("");
                }
                strAdd = strReturnedAddress.toString();
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strAdd;
    }

    public static String convertTime(Context c, long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");
        return format.format(date);
    }

    public static String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (isIPv4)
                            return sAddr;
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static int convertDBM(int dbm) {
        int quality;
        if (dbm <= -100)
            quality = 0;
        else if (dbm >= -50)
            quality = 100;
        else
            quality = 2 * (dbm + 100);
        return quality;
    }

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

    public static double getDistance(double lat1, double lat2, double lon1, double lon2) {

        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        double el1 = 0;
        double el2 = 0;
        double height = el1 - el2;
        distance = Math.pow(distance, 2) + Math.pow(height, 2);
        return Math.sqrt(distance);
    }

    public static boolean checkPermissionLocation(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    /*
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
    */
}
