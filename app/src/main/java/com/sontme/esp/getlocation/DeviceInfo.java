package com.sontme.esp.getlocation;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;

import java.io.Serializable;

import static android.content.Context.TELEPHONY_SERVICE;

public class DeviceInfo implements Serializable {

    String MODEL = Build.MODEL;
    String OS = System.getProperty("os.version");
    String APILEVEL = Build.VERSION.SDK;
    String DEVICE = Build.DEVICE;
    String PRODUCT = android.os.Build.PRODUCT;

    public String TEST1;
    public String TEST2;
    public String TEST3;
    public static String TEST1_s;
    public static String TEST2_s;
    public static String TEST3_s;

    public String getSoftwareVersion(Context c) {
        TelephonyManager telephonyManager = (TelephonyManager) c.getSystemService(TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        return telephonyManager.getDeviceSoftwareVersion();
    }

    public String getSIMSerialNumber(Context c) {
        TelephonyManager telephonyManager = (TelephonyManager) c.getSystemService(TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        return telephonyManager.getSimSerialNumber();
    }

    public String getVoiceMailNumber(Context c) {
        TelephonyManager telephonyManager = (TelephonyManager) c.getSystemService(TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        return telephonyManager.getVoiceMailNumber();
    }

    public String getSIMOperatorName(Context c) {
        TelephonyManager telephonyManager = (TelephonyManager) c.getSystemService(TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        return telephonyManager.getSimOperatorName();
    }

    public String getIMEI(Context c) {
        String serviceName = TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) c.getSystemService(serviceName);
        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        return m_telephonyManager.getDeviceId();
    }

    public String getIMSI(Context c) {
        String serviceName = TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) c.getSystemService(serviceName);
        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        return m_telephonyManager.getSubscriberId();
    }

    public String toString() {
        return "test1: " + TEST1;
    }

}
