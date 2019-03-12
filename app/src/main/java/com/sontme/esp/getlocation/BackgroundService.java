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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.crashlytics.android.Crashlytics;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.sontme.esp.getlocation.activities.MainActivity;


import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cz.msebera.android.httpclient.Header;
import io.fabric.sdk.android.Fabric;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class BackgroundService extends Service implements GpsStatus.Listener {
    //region GLOBAL / LOCAL VARIABLES
    int GpsInView;
    int GpsInUse;
    public static String accuracy;

    public static String getLatitude() {
        return latitude;
    }

    public static String getLongitude() {
        return longitude;
    }

    public static String latitude;
    public static String longitude;
    public static String speed;
    public static String altitude;
    public static String bearing;
    public static String time;
    public static String address;
    public static String provider;
    public static String distance;

    public static String getInitLat() {
        return initLat;
    }

    public static String getInitLong() {
        return initLong;
    }

    public static String initLat;
    public static String initLong;
    public static int count;
    public static List<String> urlList_failed = new ArrayList<String>();
    public static List<String> urlList_successed = new ArrayList<String>();
    public static String lastSSID;
    public static String nearbyCount;
    public static List<String> uniqueAPS = new ArrayList<>();

    //endregion
    int totalBytesSent = 0;
    CountDownTimer mCountDownTimer;
    boolean night_mode = false;
    private LocationManager mService;
    public String DEVICE_ACCOUNT;
    public boolean isuploading = false;
    private List<String> urlList_uniq = new ArrayList<String>();
    public static Location mlocation;
    public LocationListener locationListener;
    IBinder mBinder = new LocalBinder();
    int req_count;
    public LocationManager locationManager;
    exporter cs = new exporter("wifilocator_database.csv");

    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("alarm") == "run") {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, locationListener);
                Log.d("ALARM_SERVICE_", "ON");
            } else if (intent.getStringExtra("alarm") == "run") {
                locationManager.removeUpdates(locationListener);
                Log.d("ALARM_SERVICE_", "OFF");
            }
        }
    };

    @Override
    public void onCreate() {
        Fabric.with(this, new Crashlytics());
        logUser();
        AndroidNetworking.initialize(getApplicationContext());
        Toast.makeText(getBaseContext(), "Service started_1", Toast.LENGTH_SHORT).show();

        registerReceiver(broadcastReceiver, new IntentFilter());

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
            DEVICE_ACCOUNT = acc;
        } else {
            DEVICE_ACCOUNT = Settings.Secure.getString(getApplicationContext().getContentResolver(),
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
        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int min = rightNow.get(Calendar.MINUTE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mCountDownTimer = new CountDownTimer(5000, 5000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (hour > 5 && hour < 23) {
                    Log.d("TIMER_", "registered");
                    night_mode = false;
                } else {
                    Log.d("TIMER_", "removed");
                    night_mode = true;
                }
                mCountDownTimer.cancel();
                mCountDownTimer.start();
            }
        };
        mCountDownTimer.start();

        alarmMgr = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, Receiver.class);
        intent.putExtra("alarm", "run");
        alarmIntent = PendingIntent.getBroadcast(getBaseContext(), 0, intent, 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 06);
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);
        startUpdatesGPS();

    }

    public void uploadProgress(int prog) {
        Log.d("NOTIF_UPLOAD_PROGRESS_", "lol_" + String.valueOf(prog));
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), "100");
        mBuilder.setOngoing(true);
        mBuilder.setProgress(100, prog, false);
        mBuilder.setAutoCancel(true);
        Intent ii = new Intent(getBaseContext().getApplicationContext(), BackgroundService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 100, ii, 0);

        NotificationManager mNotificationManager =
                (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("100",
                    "Downloading",
                    NotificationManager.IMPORTANCE_NONE);
            mNotificationManager.createNotificationChannel(channel);
        }
        mNotificationManager.notify(100, mBuilder.build());
    }

    public void startUpdatesGPS() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
                Log.d("(service) Location Changes", location.toString());
                GpsInView = location.getExtras().getInt("satellites");
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
                GpsInUse = i;

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

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, locationListener);
    }

    public void logUser() {
        Crashlytics.setUserIdentifier("12345");
        Crashlytics.setUserEmail("sont16@gmail.com");
        Crashlytics.setUserName("wifilocatoruser");
    }

    public boolean zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;

        File sourceFile = new File(sourcePath);
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {
                zipSubFolder(out, sourceFile, sourceFile.getParent().length());
            } else {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourcePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
                entry.setTime(sourceFile.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
            out.close();
        } catch (Exception e) {
            //e.printStackTrace();
            Log.d("ZIP_", "DONE_error" + e.getMessage());
            return false;
        }
        Log.d("ZIP_", "DONE");
        return true;
    }

    private void zipSubFolder(ZipOutputStream out, File folder,
                              int basePathLength) throws IOException {

        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte data[] = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                entry.setTime(file.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    public String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        return lastPathComponent;
    }

    public void queryLocation(Location LocRes) {
        if (night_mode == false && chk_3g_wifi() == "wifi") {
            // CHECK IF CSV SIZE IS OVER 1 MEGABYTE YES -> Start UploadFileHTTP
            File f = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
            if (f.length() / 1024 >= 100) {
                if (isuploading == false) {
                    Toast.makeText(getBaseContext(), String.valueOf("Adatbázis feltöltése"), Toast.LENGTH_SHORT).show();
                    isuploading = true;
                    zipFileAtPath("/storage/emulated/0/Documents/wifilocator_database.csv", "/storage/emulated/0/Documents/wifilocator_database.zip");
                    File zip = new File("/storage/emulated/0/Documents/wifilocator_database.zip");
                    AndroidNetworking.upload("https://sont.sytes.net/upload.php")
                            .addMultipartFile("uploaded_file", zip)
                            .addMultipartParameter("source", DEVICE_ACCOUNT)
                            .setTag("background_auto_upload")
                            .setPriority(Priority.HIGH)
                            .setExecutor(Executors.newSingleThreadExecutor())
                            .build()

                            .setUploadProgressListener(new UploadProgressListener() {
                                @Override
                                public void onProgress(long bytesUploaded, long totalBytes) {
                                    Log.d("NOTIF_UPLOAD_PROGRESS_", String.valueOf("asd"));
                                    int prog = (int) ((bytesUploaded / totalBytes) * 100);
                                    Log.d("NOTIF_UPLOAD_PROGRESS_", String.valueOf(prog));
                                    uploadProgress(prog);
                                }
                            })
                            .getAsString(new StringRequestListener() {
                                @Override
                                public void onResponse(String response) {
                                    isuploading = false;
                                    Log.d("FELTOLTES_", String.valueOf(response));
                                }

                                @Override
                                public void onError(ANError anError) {
                                    isuploading = false;
                                    Log.d("FELTOLTES_", String.valueOf(anError.toString()));
                                }
                            });
                    isuploading = false;
                    try {
                        File f2 = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
                        f2.delete();
                        deleteFile("/storage/emulated/0/Documents/wifilocator_database.csv");
                        if (f2.exists()) {
                            f2.getCanonicalFile().delete();
                        }
                        if (f2.exists()) {
                            getBaseContext().deleteFile(f2.getName());
                            getApplicationContext().deleteFile(f2.getName());
                        }
                        if (f2.exists()) {
                            f2.getAbsoluteFile().delete();
                        }
                        if (f2.exists()) {
                            Log.d("DELETE_", "Could not delete file");
                        } else {
                            Log.d("DELETE_", "File is removed or couldnt remove");
                        }
                    } catch (Exception e) {
                    }
                }
            } else {
            }

            if (String.valueOf(LocRes.getLongitude()) != null || String.valueOf(LocRes.getLongitude()).length() >= 1) {
                try {
                    accuracy = String.valueOf(LocRes.getAccuracy());
                    latitude = String.valueOf(LocRes.getLatitude());
                    longitude = String.valueOf(LocRes.getLongitude());
                    speed = String.valueOf(round(mpsTokmh(LocRes.getSpeed()), 2));
                    altitude = String.valueOf(LocRes.getAltitude());
                    bearing = String.valueOf(LocRes.getBearing());
                    time = String.valueOf(convertTime(LocRes.getTime()));
                    address = getCompleteAddressString(LocRes.getLatitude(), LocRes.getLongitude());
                    provider = LocRes.getProvider();
                    distance = String.valueOf(getDistance(Double.valueOf(latitude), Double.valueOf(initLat), Double.valueOf(longitude), Double.valueOf(initLong)));
                } catch (Exception e) {
                    Log.d("queryLocation()_", e.toString());
                }
            }
            try {
                if (Double.valueOf(latitude) != 0 && Double.valueOf(longitude) != 0) {
                    count++;
                    if (count == 1) {
                        // START POSITION (Activity/Program start)
                        initLat = latitude;
                        initLong = longitude;
                    }
                }
            } catch (Exception e) {
                Log.d("queryLocation()_2_", e.toString());
            }

            if (latitude != null) {
                aplist(getBaseContext(), Double.valueOf(latitude), Double.valueOf(longitude));
            }
            try {
                showNotif("WIFI Locator", "Count: " + String.valueOf(count) + " (Service)"
                        + "\nLast Change: " + time
                        + "\nDistance: " + distance + " meters"
                        + "\nLongitude: " + longitude
                        + "\nLatitude: " + latitude
                        + "\nAddress: " + address
                        + "\nProvider: " + provider
                        + "\nSpeed: " + String.valueOf(round(mpsTokmh(Double.valueOf(speed)), 2)) + " km/h"
                        + "\nAccuracy: " + accuracy + " meters");
            } catch (Exception e) {
                Log.d("NOTIF EXCEPTION: ", e.toString());
            }
        }
    }

    public void saveRecordHttp(String path) {

        AndroidNetworking.get(path)
                .setUserAgent("sont_wifilocator")
                .setTag("save_record_http")
                .setPriority(Priority.LOW)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        urlList_successed.add(path);
                        Log.d("HTTP_RESPONSE_", response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        urlList_failed.add(path);
                        Log.d("HTTP_ERROR_", anError.toString());
                    }
                });


    }

    public void aplist(final Context context, double lati, double longi) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            nearbyCount = String.valueOf(scanResults.size());
            //WifiConfiguration wc = new WifiConfiguration();
            //wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            int versionCode = BuildConfig.VERSION_CODE;
            for (ScanResult result : scanResults) {
                lastSSID = result.SSID + " " + convertDBM(result.level) + "%";
                if (!uniqueAPS.contains(result.BSSID)) {
                    uniqueAPS.add(result.BSSID);
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
                String reqBody = "?id=0&ssid=" + result.SSID + "&add=service" + "&bssid=" + result.BSSID + "&source=" + DEVICE_ACCOUNT + "_v" + versionCode + "&enc=" + enc + "&rssi=" + convertDBM(result.level) + "&long=" + longi + "&lat=" + lati + "&channel=" + result.frequency;

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
                cs.writeCsv("0" + "," + result.BSSID + "," + result.SSID + "," + convertDBM(result.level) + "," + DEVICE_ACCOUNT + "_v" + versionCode + "," + enc + "," + lati + "," + longi + "," + result.frequency + "," + time);
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
        contentView.setTextViewText(R.id.notif_ssid, "SSID / Count #" + count);
        contentView.setTextViewText(R.id.notif_time, "Time / HTTP #" + req_count);
        contentView.setTextViewText(R.id.notif_text2, det[6]);
        contentView.setTextViewText(R.id.notif_text3, lastSSID);
        contentView.setTextViewText(R.id.notif_lat, latitude);
        contentView.setTextViewText(R.id.notif_long, longitude);
        contentView.setTextViewText(R.id.notif_add, "Address: " + address);
        contentView.setTextViewText(R.id.notif_uniq, "Unique: " + String.valueOf(uniqueAPS.size()));
        contentView.setTextViewText(R.id.notif_gps, "GPS Satellites: " + String.valueOf(GpsInView) + "_" + String.valueOf(GpsInUse));

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
        if (event != GpsStatus.GPS_EVENT_FIRST_FIX &&
                event != GpsStatus.GPS_EVENT_SATELLITE_STATUS &&
                event != GpsStatus.GPS_EVENT_STARTED &&
                event != GpsStatus.GPS_EVENT_STOPPED) {
            Toast.makeText(getBaseContext(), "GPS Unknown event: " + event, Toast.LENGTH_SHORT).show();
        }
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

    public String chk_3g_wifi() {
        final ConnectivityManager connMgr = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi.isConnectedOrConnecting()) {
            return "wifi";
        } else if (mobile.isConnectedOrConnecting()) {
            return "3g";
        } else {
            return "no";
        }
    }

}
