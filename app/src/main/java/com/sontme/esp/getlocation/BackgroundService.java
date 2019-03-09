package com.sontme.esp.getlocation;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.sontme.esp.getlocation.activities.MainActivity;


import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;
import io.fabric.sdk.android.Fabric;
import okhttp3.WebSocket;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class BackgroundService extends Service implements GpsStatus.Listener {
    private LocationManager mService;
    private GpsStatus mStatus;
    private List<String> urlList = new ArrayList<String>();
    private List<String> urlList_uniq = new ArrayList<String>();
    public static Location mlocation;
    IBinder mBinder = new LocalBinder();
    int req_count;

    exporter cs = new exporter("wifilocator_database.csv");

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Fabric.with(this, new Crashlytics());
        logUser();
        Toast.makeText(getBaseContext(), "Service started_1", Toast.LENGTH_SHORT).show();
        mService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mService.addGpsStatusListener(this);

        AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account[] list = manager.getAccounts();
        String acc = "no";
        for (Account s : list) {
            acc = String.valueOf(s.name);
        }
        if (acc.length() > 3) {
            Global.googleAccount = acc;
        } else {
            Global.googleAccount = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        }
        Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Intent mStartActivity = new Intent(getBaseContext(), BackgroundService.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(getBaseContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder builder = new Notification.Builder(this, "wifilocatorservice")
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("WiFi Locator Service started")
                    .setAutoCancel(true);
            Notification notification = builder.build();
            startForeground(1, notification);
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("WiFi Locator Service started")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);
            Notification notification = builder.build();
            startForeground(1, notification);
        }
        //region HTTP
        AsyncHttpServer server = new AsyncHttpServer();
        List<WebSocket> _sockets = new ArrayList<WebSocket>();
        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String tosend = "";
                String os = System.getProperty("os.version");
                String sdk = android.os.Build.VERSION.SDK;
                String device = android.os.Build.DEVICE;
                String model = android.os.Build.MODEL;
                String prod = android.os.Build.PRODUCT;
                String serviceName = Context.TELEPHONY_SERVICE;
                TelephonyManager m_telephonyManager = (TelephonyManager) getSystemService(serviceName);
                String IMEI, IMSI;
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                IMEI = m_telephonyManager.getDeviceId();
                IMSI = m_telephonyManager.getSubscriberId();
                String osf = android.os.Build.VERSION.RELEASE;
                long RX = TrafficStats.getTotalRxBytes() / (1024 * 1024);
                long TX = TrafficStats.getTotalTxBytes() / (1024 * 1024);

                ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> runningApps = manager.getRunningAppProcesses();
                String stat = "";
                for (ActivityManager.RunningAppProcessInfo runningApp : runningApps) {
                    long received = TrafficStats.getUidRxBytes(runningApp.uid);
                    long sent = TrafficStats.getUidTxBytes(runningApp.uid);
                    stat = runningApp.uid + " Process: " + runningApp.processName + " Sent: " + sent / (1024 * 1024) + " Received: " + received / 1024 / 1024;
                    Log.d("NETSTAT", stat);
                }
                tosend = request.toString();
                tosend = tosend + "<br><br>OS: " + os + "<br>OS_2: " + osf + "<br>SDK: " + sdk + "<br>Device: " + device + "<br>Model: " + model + "<br>Product: " + prod + "<br>IMEI: " + IMEI + "<br>IMSI: " + IMSI + "<br>Service name: " + serviceName;
                tosend = tosend + "<br>ID: " + Build.ID;
                tosend = tosend + "<br>User: " + Build.USER;
                tosend = tosend + "<br>Host: " + Build.HOST;
                tosend = tosend + "<br>Fingerprint: " + Build.FINGERPRINT;
                tosend = tosend + "<br>Board: " + Build.BOARD;
                tosend = tosend + "<br>RX: " + RX + " mb TX: " + TX + " mb";
                tosend = tosend + "<br>" + stat;
                response.send(tosend);
                Toast.makeText(getApplicationContext(), "Service: HTTP Respond sent", Toast.LENGTH_SHORT).show();
            }
        });
        server.listen(8888);
        //endregion
        startUpdatesGPS();

    }

    public void startUpdatesGPS() {
        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
                Log.d("(service) Location Changes", location.toString());
                Global.GpsInView = location.getExtras().getInt("satellites");
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Iterable<GpsSatellite> satellites = locationManager.getGpsStatus(null).getSatellites();
                Iterator<GpsSatellite> satI = satellites.iterator();
                int i = 0;
                while (satI.hasNext()) {
                    GpsSatellite satellite = satI.next();
                    i++;
                }
                Global.GpsInUse = i;

                queryLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("Status Changed", String.valueOf(status));
//                Toast.makeText(getBaseContext(), "GPS Status Changed: " + status, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d("(service) Provider Enabled", provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d("Provider Disabled", provider);
            }
        };

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

        // Now create a location manager
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    public void logUser() {
        Crashlytics.setUserIdentifier("12345");
        Crashlytics.setUserEmail("sont16@gmail.com");
        Crashlytics.setUserName("wifilocatoruser");
    }

    public void queryLocation(Location LocRes) {
        if (String.valueOf(LocRes.getLongitude()) != null || String.valueOf(LocRes.getLongitude()).length() >= 1) {
            try {
                Global.accuracy = String.valueOf(LocRes.getAccuracy());
                Global.latitude = String.valueOf(LocRes.getLatitude());
                Global.longitude = String.valueOf(LocRes.getLongitude());
                Global.speed = String.valueOf(round(mpsTokmh(LocRes.getSpeed()), 2));
                Global.altitude = String.valueOf(LocRes.getAltitude());
                Global.bearing = String.valueOf(LocRes.getBearing());
                Global.time = String.valueOf(convertTime(LocRes.getTime()));
                Global.address = getCompleteAddressString(LocRes.getLatitude(), LocRes.getLongitude());
                Global.provider = LocRes.getProvider();
                Global.distance = String.valueOf(getDistance(Double.valueOf(Global.latitude), Double.valueOf(Global.initLat), Double.valueOf(Global.longitude), Double.valueOf(Global.initLong)));
            } catch (Exception e) {
                Log.d("queryLocation()_", e.toString());
            }
        }
        try {
            if (Double.valueOf(Global.latitude) != 0 && Double.valueOf(Global.longitude) != 0) {
                Global.count++;
                if (Global.count == 1) {
                    // START POSITION (Activity/Program start)
                    Global.initLat = Global.latitude;
                    Global.initLong = Global.longitude;
                }
            }
        } catch (Exception e) {
            Log.d("queryLocation()_2_", e.toString());
        }

        if (Global.latitude != null) {
            aplist(getBaseContext(), Double.valueOf(Global.latitude), Double.valueOf(Global.longitude));
        }
        try {
            showNotif("WIFI Locator", "Count: " + String.valueOf(Global.count) + " (Service)"
                    + "\nLast Change: " + Global.time
                    + "\nDistance: " + Global.distance + " meters"
                    + "\nLongitude: " + Global.longitude
                    + "\nLatitude: " + Global.latitude
                    + "\nAddress: " + Global.address
                    + "\nProvider: " + Global.provider
                    + "\nSpeed: " + String.valueOf(round(mpsTokmh(Double.valueOf(Global.speed)), 2)) + " km/h"
                    + "\nAccuracy: " + Global.accuracy + " meters");
        } catch (Exception e) {
            Log.d("NOTIF EXCEPTION: ", e.toString());
        }
    }

    public void saveRecordHttp(String path) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(path, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            }

            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                //saveRecordHttp(path);
                //Log.d("HTTP_RETRY", "Error code: " + statusCode);
            }
        });
    }

    public void aplist(final Context context, double lati, double longi) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            Global.nearbyCount = String.valueOf(scanResults.size());
            //WifiConfiguration wc = new WifiConfiguration();
            //wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            int versionCode = BuildConfig.VERSION_CODE;
            for (ScanResult result : scanResults) {
                Global.lastSSID = result.SSID + " " + convertDBM(result.level) + "%";
                if (!Global.uniqueAPS.contains(result.BSSID)) {
                    Global.uniqueAPS.add(result.BSSID);
                }
                //Log.d("WIFI_CAP_", result.capabilities);
                String enc = "notavailable";
                if (!result.capabilities.contains("WEP") || !result.capabilities.contains("WPA")) {
                    enc = "NONE";
                } else if (result.capabilities.contains("WEP")) {
                    enc = "WEP";
                } else if (result.capabilities.contains("WPA")) {
                    enc = "WPA";
                } else if (result.capabilities.contains("WPA2")) {
                    enc = "WPA2";
                }

                String url = MainActivity.INSERT_URL;
                String reqBody = "?id=0&ssid=" + result.SSID + "&add=service" + "&bssid=" + result.BSSID + "&source=" + Global.googleAccount + "_v" + versionCode + "&enc=" + enc + "&rssi=" + convertDBM(result.level) + "&long=" + longi + "&lat=" + lati + "&channel=" + result.frequency;

                if (!urlList_uniq.contains(url + reqBody)) {
                    urlList_uniq.add(url + reqBody);
                    saveRecordHttp(url + reqBody);
                    req_count++;
                } else {
                    //Log.d("HTTP_", String.valueOf(req_count) + "_ALREADY CONTAINS_" + String.valueOf(urlList_uniq.size()));
                }
                if (urlList_uniq.size() >= 5000) {
                    urlList_uniq.clear();
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String time = sdf.format(new Date());
                cs.writeCsv("0" + "," + result.BSSID + "," + result.SSID + "," + convertDBM(result.level) + "," + Global.googleAccount + "_v" + versionCode + "," + enc + "," + lati + "," + longi + "," + result.frequency + "," + time);
            }
        } catch (
                Exception e) {
            Log.d("APP", "ERROR " + e.getMessage());
        }
    }

    public int convertDBM(int dbm) {
        int quality;
        if (dbm <= -100)
            quality = 0;
        else if (dbm >= -50)
            quality = 100;
        else
            quality = 2 * (dbm + 100);
        return quality;
    }

    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public double mpsTokmh(double mps) {
        return mps * 3.6;
    }

    public void showNotif(String Title, String Text) {

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notif_lay);
        String[] det = Text.split("\\s+");
        contentView.setTextViewText(R.id.notif_ssid, "SSID / Count #" + Global.count);
        contentView.setTextViewText(R.id.notif_time, "Time / HTTP #" + req_count);
        contentView.setTextViewText(R.id.notif_text2, det[6]);
        contentView.setTextViewText(R.id.notif_text3, Global.lastSSID);
        contentView.setTextViewText(R.id.notif_lat, Global.latitude);
        contentView.setTextViewText(R.id.notif_long, Global.longitude);
        contentView.setTextViewText(R.id.notif_add, "Address: " + Global.address);
        contentView.setTextViewText(R.id.notif_uniq, "Unique: " + String.valueOf(Global.uniqueAPS.size()));
        contentView.setTextViewText(R.id.notif_gps, "GPS Satellites: " + String.valueOf(Global.GpsInView) + "_" + String.valueOf(Global.GpsInUse));

        Intent intent2 = new Intent(getBaseContext(), Receiver.class);
        Intent intent3 = new Intent(getBaseContext(), Receiver.class);
        Intent intent4 = new Intent(getBaseContext(), Receiver.class);
        intent2.setAction("exit");
        intent3.setAction("resume");
        intent4.setAction("pause");

        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(getBaseContext(), 1, intent2, PendingIntent.FLAG_ONE_SHOT);
        PendingIntent pendingIntent3 = PendingIntent.getBroadcast(getBaseContext(), 1, intent3, PendingIntent.FLAG_ONE_SHOT);
        PendingIntent pendingIntent4 = PendingIntent.getBroadcast(getBaseContext(), 1, intent4, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getBaseContext().getApplicationContext(), "0");
        Intent ii = new Intent(getBaseContext().getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, ii, 0);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(Text);
        bigText.setBigContentTitle(Title);
        bigText.setSummaryText("Current Status");

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.drawable.computer_low);
        mBuilder.setContent(contentView);
        mBuilder.setPriority(Notification.PRIORITY_DEFAULT);
        mBuilder.setStyle(bigText);
        mBuilder.setVibrate(new long[]{0L});
        mBuilder.setSound(null);
        mBuilder.setLights(0xFFff0000, 600, 500);
        mBuilder.setDefaults(Notification.FLAG_SHOW_LIGHTS);

        mBuilder.addAction(R.drawable.computer_low, "Pause", pendingIntent4);
        mBuilder.addAction(R.drawable.computer_low, "Resume", pendingIntent3);
        mBuilder.addAction(R.drawable.computer_low, "Exit", pendingIntent2);


        NotificationManager mNotificationManager =
                (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("0",
                    Title,
                    NotificationManager.IMPORTANCE_NONE);
            mNotificationManager.createNotificationChannel(channel);
        }
        mNotificationManager.notify(0, mBuilder.build());

    }

    public String convertTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");
        return format.format(date);
    }

    public double getDistance(double lat1, double lat2, double lon1, double lon2) {

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

    public String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("");
                }
                strAdd = strReturnedAddress.toString();
            } else {
            }
        } catch (Exception e) {
            Log.d("Error_", e.toString());
        }
        return strAdd;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getBaseContext(), "Service started_2", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(getBaseContext(), "Service stopped_1", Toast.LENGTH_SHORT).show();
        stopForeground(true);
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Toast.makeText(getBaseContext(), "Service started_3", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Toast.makeText(getBaseContext(), "Service stopped_2", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        //mStatus = mService.getGpsStatus(mStatus);
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                Toast.makeText(getBaseContext(), "GPS Event Started", Toast.LENGTH_SHORT).show();
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                Toast.makeText(getBaseContext(), "GPS Event Stopped", Toast.LENGTH_SHORT).show();
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                Toast.makeText(getBaseContext(), "GPS Event First FIX", Toast.LENGTH_SHORT).show();
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                //Toast.makeText(getBaseContext(), "GPS SAT Status", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public class LocalBinder extends Binder {
        public BackgroundService getServerInstance() {
            return BackgroundService.this;
        }
    }


}
