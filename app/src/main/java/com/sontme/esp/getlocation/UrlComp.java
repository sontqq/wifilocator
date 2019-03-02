package com.sontme.esp.getlocation;

import android.location.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UrlComp {

    private Location location;
    private String BSSID;
    private String RSSI;
    private String URL;
    private List<String> urlList = new ArrayList<String>();

    public UrlComp(Location location, String BSSID, String RSSI, String URL) {
        this.location = location;
        this.BSSID = BSSID;
        this.RSSI = RSSI;
        this.URL = URL;
    }


    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getBSSID() {
        return BSSID;
    }

    public void setBSSID(String BSSID) {
        this.BSSID = BSSID;
    }

    public String getRSSI() {
        return RSSI;
    }

    public void setRSSI(String RSSI) {
        this.RSSI = RSSI;
    }

    public List<String> getUrlList() {
        return urlList;
    }

    public void setUrlList(List<String> urlList) {
        this.urlList = urlList;
    }
}
