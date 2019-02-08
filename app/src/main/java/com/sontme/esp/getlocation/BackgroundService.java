package com.sontme.esp.getlocation;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BackgroundService extends Service {

    /*
    public Context context = this;
    public static Runnable runnable = null;
    private static final int REQUEST_PERMISSION_LOCATION = 255;
    public int counter = 0;
    public double initialLong = 0;
    public double initialLat = 0;
    */

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onStart(Intent intent, int startid) {
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
    }
}
