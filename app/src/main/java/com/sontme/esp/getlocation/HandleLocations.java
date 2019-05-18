package com.sontme.esp.getlocation;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HandleLocations {
    private String lati;
    private String longi;
    private double lat1;
    private double lon1;
    private double lat2;
    private double lon2;
    private Location current;

    //region CONSTRUCTORS
    public HandleLocations(Location loc){
        this.current = loc;
    }
    public HandleLocations(String lat, String lon){
        this.lati = lat;
        this.longi = lon;
    }
    public HandleLocations(Location loc, String lat, String lon){
        this.current = loc;
        this.lati = lat;
        this.longi = lon;
    }
    public HandleLocations(String lat1, String lon1, String lat2, String lon2){
        this.lat1 = Double.valueOf(lat1);
        this.lon1 = Double.valueOf(lon1);
        this.lat2 = Double.valueOf(lat2);
        this.lon2 = Double.valueOf(lon2);
    }
    public HandleLocations(double latitude, double longitude) {
        this.lat1 = latitude;
        this.lon1 = longitude;
    }
    //endregion
    // UNUSED YET - need some rework
    public float getDistance(String la1, String lo1, String la2, String lo2){
        Location loc1 = null;
        Double lad1 = Double.valueOf(la1);
        Double lad2 = Double.valueOf(lo1);
        loc1.setLatitude(lad1);
        loc1.setLongitude(lad2);
        Location loc2 = null;
        Double lad3 = Double.valueOf(la2);
        Double lad4 = Double.valueOf(lo2);
        loc1.setLatitude(lad3);
        loc1.setLongitude(lad4);
        float distance = loc1.distanceTo(loc2);
        return distance;
    }
    public float getLDistance(Location lo1, Location lo2){
        float dist = lo1.distanceTo(lo2);
        return dist;
    }
    public float getSelfDistance(){
        float[] distance = new float[2];
        Location.distanceBetween(lat1,lon1,lat2,lon2, distance);
        return distance[0];
    }
    public Location getNearestPoint(Location point, Location[] locs){
        Location nearest = null;
        float near = 0;
        double latitude = point.getLatitude();
        double longitude = point.getLongitude();
        for (Location s: locs) {
            float d = s.distanceTo(point);
            if(d < near){
                near = d;
                nearest = s;
            }
        }
        return nearest;
        }
    public Location getNearestPoint(Location point, Map<Location, ApStrings> coll){
        return null;
    }
    public static String getCompleteAddressString(Context c,double LATITUDE, double LONGITUDE) {
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
                //Log.w("My Current loction add ", strReturnedAddress.toString());
            } else {
                //Log.w("My Current loction add ", "No Address returned!");
            }
        } catch (Exception e) {
            Log.d("APP", "ADDRESS RESOLUTION FAIL");
        }
        return strAdd;
    }
}
