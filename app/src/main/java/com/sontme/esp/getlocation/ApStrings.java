package com.sontme.esp.getlocation;

import android.provider.Settings;
import android.support.annotation.NonNull;

import java.util.Comparator;
import java.util.Date;

public class ApStrings {
    private String time;
    private String ssid;
    private String mac;
    private String source;
    private String str;
    private String longi;
    private String lati;

    public ApStrings(String time, String ssid, String mac, String str, String source){
        this.time = time;
        this.ssid = ssid;
        this.mac = mac;
        this.source = source;
        this.str = str;
    }
    public ApStrings(String lati, String longi, String time, String ssid, String mac, String str, String source){
        this.time = time;
        this.ssid = ssid;
        this.mac = mac;
        this.source = source;
        this.str = str;
        this.lati = lati;
        this.longi = longi;
    }

    public String getTime(){ return time; }
    public String getSsid(){ return ssid; }
    public String getMac(){ return mac; }
    public String getSource(){ return source; }
    public String getStr(){ return str; }
    public String getLati(){ return lati; }
    public String getLongi(){ return longi; }

}
