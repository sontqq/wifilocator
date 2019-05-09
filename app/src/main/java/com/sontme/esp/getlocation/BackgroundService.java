package com.sontme.esp.getlocation;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.crashlytics.android.Crashlytics;
import com.sontme.esp.getlocation.activities.MainActivity;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.fabric.sdk.android.Fabric;

public class BackgroundService extends Service implements GpsStatus.Listener {
    //region GLOBAL / LOCAL VARIABLES
    int GpsInView;
    int GpsInUse;
    public static String accuracy;

    public static String getLatitude() {
        if (latitude == null)
            return "0";
        return latitude;
    }

    public static String getLongitude() {
        if (longitude == null)
            return "0";
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
        if (initLat == null)
            return "0";
        return initLat;
    }

    public static String getInitLong() {
        if (initLong == null)
            return "0";
        return initLong;
    }

    public static String initLat;
    public static String initLong;

    public static int getCount() {
        return count;
    }

    public static int count = 0;
    public static List<String> urlList_failed = new ArrayList<String>();
    public static List<String> urlList_successed = new ArrayList<String>();
    public static String lastSSID;
    public static String nearbyCount;
    public static List<String> uniqueAPS = new ArrayList<>();

    public static String ipaddress;
    public static String googleAccount;
    public static boolean isUploading;
    //endregion
    int totalBytesSent = 0;
    public static String HUAWEI_PATH = "/data/user/0/com.sontme.esp.getlocation/files/";
    CountDownTimer mCountDownTimer;
    int UPLOAD_SIZE_LIMIT = 10240;
    boolean UPLOAD_3G = false;
    boolean UPLOAD_NIGHT = false;
    int not_recorded = 0;
    int recorded = 0;
    int uploaded = 0;
    private LocationManager mService;
    public String DEVICE_ACCOUNT;
    public boolean isuploading = false;
    private List<String> urlList_uniq = new ArrayList<String>();
    private List<String> macList_uniq = new ArrayList<String>();
    public static Location mlocation;
    public LocationListener locationListener;
    IBinder mBinder = new LocalBinder();
    int req_count;
    public LocationManager locationManager;
    exporter cs = new exporter("wifilocator_database.csv");

    private static boolean isRunning;


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
            } else if (intent.getStringExtra("alarm") == "off") {
                //locationManager.removeUpdates(locationListener);
                Log.d("ALARM_SERVICE_", "OFF");
            }
        }
    };

    @Override
    public void onCreate() {
        try {
            showOngoing();

            Fabric.with(this, new Crashlytics());
            logUser();
            AndroidNetworking.initialize(getApplicationContext());
            turnGPSOn();
            registerReceiver(broadcastReceiver, new IntentFilter());
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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


            //region HTTP
            /*
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
                    //Toast.MakeText(getApplicationContext(), "Service: HTTP Respond sent", Toast.LENGTH_SHORT).show();
                }
            });
            server.listen(8888);
            */
            //endregion
            Calendar rightNow = Calendar.getInstance();
            int hour = rightNow.get(Calendar.HOUR_OF_DAY);
            int min = rightNow.get(Calendar.MINUTE);

            mCountDownTimer = new CountDownTimer(10000, 10000) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    if (hour > 5 && hour < 23) {
                        Log.d("TIMER_", "registered");
                        UPLOAD_NIGHT = false;

                        AndroidNetworking.get("https://sont.sytes.net/wifilocator/upload_limit.php")
                                .setTag("upload_limit")
                                .addQueryParameter("source", googleAccount)
                                .setPriority(Priority.LOW)
                                .build()
                                .getAsString(new StringRequestListener() {
                                    @Override
                                    public void onResponse(String response) {
                                        Map<String, String> config = new HashMap<String, String>();
                                        if (NumberUtils.isNumber(response.trim())) {
                                            //UPLOAD_SIZE_LIMIT = Integer.valueOf(response.trim());
                                        } else {
                                            String lines[] = response.split("\n");
                                            for (String line : lines) {
                                                String key = line.split("=")[0];
                                                String value = line.split("=")[1];
                                                config.put(key, value);
                                            }
                                            Iterator iterator = config.keySet().iterator();
                                            while (iterator.hasNext()) {
                                                String key = (String) iterator.next();
                                                String value = config.get(key);

                                                if (key.contains("upload_limit")) {
                                                    UPLOAD_SIZE_LIMIT = Integer.valueOf(value);
                                                }
                                                if (key == "upload_3g")
                                                    if (value == "yes")
                                                        UPLOAD_3G = true;
                                                if (value == "no")
                                                    UPLOAD_3G = false;
                                                if (key == "upload_night")
                                                    if (value == "yes")
                                                        UPLOAD_NIGHT = true;
                                                if (value == "no")
                                                    UPLOAD_NIGHT = false;


                                            }
                                        }
                                    }

                                    @Override
                                    public void onError(ANError anError) {
                                        Log.d("UPLOAD_LIMIT_", "HTTP_ERROR");
                                    }
                                });
                    } else {
                        Log.d("TIMER_", "removed");
                        UPLOAD_NIGHT = true;
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
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void showNotification(Context context, String title, String body, Intent intent) {

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notif_lay);
        String[] det = body.split("\\s+");
        contentView.setTextViewText(R.id.notif_ssid, "SSID / Count #" + count);
        contentView.setTextViewText(R.id.notif_time, "Time / HTTP #" + req_count);
        contentView.setTextViewText(R.id.notif_text2, det[6]);
        contentView.setTextViewText(R.id.notif_text3, lastSSID);
        contentView.setTextViewText(R.id.notif_lat, latitude);
        contentView.setTextViewText(R.id.notif_long, longitude);
        contentView.setTextViewText(R.id.notif_add, "Address: " + address);
        contentView.setTextViewText(R.id.notif_uniq, "Unique: " + String.valueOf(uniqueAPS.size()));
        contentView.setTextViewText(R.id.notif_gps, "GPS Satellites: " + String.valueOf(GpsInView) + "_" + String.valueOf(GpsInUse));

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(body);
        bigText.setBigContentTitle(title);
        bigText.setSummaryText("Current Status");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 0;
        String channelId = "0";
        String channelName = "0";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.computer_low)
                .setContent(contentView)
                .setStyle(bigText)
                .setGroup("wifi")
                .setNumber(1)
                .setWhen(System.currentTimeMillis());

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());

    }

    public void uploadProgress(int prog, long uploaded, long total) {

        Context context = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 100;
        String channelId = "100";
        String channelName = "Uploadingdatabase";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }
        String detail = String.valueOf(prog);
        if (prog >= 100 || prog < 0) {
            detail = "Complete " + total + " bytes";
            prog = 100;
        } else {
            detail = "Progress: " + String.valueOf((int) (uploaded)) + " / " + String.valueOf((int) (total));
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.cloudupload)
                .setContentTitle("Uploading database")
                .setProgress(100, prog, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setVibrate(new long[]{0L})
                .setSound(null)
                .setLights(0xFFff0000, 600, 500)
                .setDefaults(Notification.FLAG_SHOW_LIGHTS)
                .setContentText(detail);

        notificationManager.notify(notificationId, mBuilder.build());

    }

    public void uploadProgress(int prog) {

        Context context = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 100;
        String channelId = "100";
        String channelName = "Uploadingdatabase";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }
        String detail = String.valueOf(prog);
        if (prog >= 100 || prog < 0) {
            detail = "Complete";
            prog = 100;
        } else {
            detail = "Progress: " + String.valueOf(prog);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.cloudupload)
                .setContentTitle("Uploading database")
                .setProgress(100, prog, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setVibrate(new long[]{0L})
                .setSound(null)
                .setLights(0xFFff0000, 600, 500)
                .setDefaults(Notification.FLAG_SHOW_LIGHTS)
                .setContentText(detail);

        notificationManager.notify(notificationId, mBuilder.build());

    }

    private void turnGPSOn() {
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!provider.contains("gps")) { //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            sendBroadcast(poke);
        }
    }

    public void startUpdatesGPS() {

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListenerr = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Toast.makeText(getBaseContext(), "changed", Toast.LENGTH_SHORT).show();
                mlocation = location;
                //Log.d("(service) Location Changes", location.toString());
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
                Log.d("(service) Status Changed", String.valueOf(status));
//                //Toast.MakeText(getBaseContext(), "GPS Status Changed: " + status, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d("(service) Provider Enabled", provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d("(service )Provider Disabled", provider);
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
            //return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerr);
        //locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, locationListenerr);
        //Log.d("(service)", "3");
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
        //Toast.makeText(getBaseContext(), "query()", Toast.LENGTH_SHORT).show();
        //Thread thread = new Thread() {
        // public void run() {
        if (UPLOAD_NIGHT == false) {
            // CHECK IF CSV SIZE IS OVER 1 MEGABYTE YES -> Start UploadFileHTTP
            File f;
            String deviceMan = android.os.Build.MANUFACTURER;
            if (deviceMan.equalsIgnoreCase("huawei")) {
                f = new File("/data/user/0/com.sontme.esp.getlocation/files/wifilocator_database.csv");
            } else {
                f = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
            }
            Log.d("csv_", String.valueOf(f.getParent()));
            Log.d("UPLOAD_", "CHECK_" + String.valueOf(UPLOAD_3G) + String.valueOf(UPLOAD_NIGHT) + String.valueOf(UPLOAD_SIZE_LIMIT));
            if (f.length() / 1024 >= UPLOAD_SIZE_LIMIT) {
                Log.d("UPLOAD_", "SIZE OK TO UPLOAD" + String.valueOf(UPLOAD_3G) + String.valueOf(UPLOAD_NIGHT));
                if (isuploading == false) {
                    Log.d("UPLOAD_", "STATUS OK TO UPLOAD");
                    if (chk_3g_wifi() == "wifi" || UPLOAD_3G == true) {
                        Log.d("UPLOAD_", "SIZE OK TO UPLOAD");
                        //Toast.MakeText(getBaseContext(), String.valueOf("Adatbázis feltöltése"), Toast.LENGTH_SHORT).show();
                        uploadProgress(0, 0, 0);
                        isuploading = true;
                        zipFileAtPath(f.getAbsolutePath(), f.getParent() + "/wifilocator_database.zip");
                        File zip = new File(f.getParent() + "/wifilocator_database.zip");
                        AndroidNetworking.upload("https://sont.sytes.net/wifilocator/upload.php")
                                .addMultipartFile("uploaded_file", zip)
                                .addMultipartParameter("source", DEVICE_ACCOUNT) // DEVICE_ACCOUNT
                                .setTag("background_auto_upload")
                                .setPriority(Priority.IMMEDIATE)
                                .setExecutor(Executors.newSingleThreadExecutor())
                                .build()
                                .setUploadProgressListener(new UploadProgressListener() {
                                    @Override
                                    public void onProgress(long bytesUploaded, long totalBytes) {
                                        int prog = (int) ((bytesUploaded / totalBytes) * 100);
                                        Log.d("FELTOLTES_", "prog: " + String.valueOf(prog));
                                        Log.d("FELTOLTES_", "progress left: " + String.valueOf(totalBytes - bytesUploaded));
                                        if (prog == 100) {
                                            uploadProgress(100, totalBytes, totalBytes);
                                        } else {
                                            uploadProgress(prog, bytesUploaded, totalBytes);
                                        }
                                    }
                                })
                                .getAsString(new StringRequestListener() {
                                    @Override
                                    public void onResponse(String response) {
                                        isuploading = false;
                                        Log.d("FELTOLTES_resp_", String.valueOf(response));
                                        uploadProgress(100);
                                        uploaded++;

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
                                            Log.d("csv_", e.toString());
                                        }
                                    }

                                    @Override
                                    public void onError(ANError anError) {
                                        isuploading = false;
                                        uploadProgress(0);
                                        Log.d("FELTOLTES_1_", String.valueOf(anError.getResponse()));
                                        Log.d("FELTOLTES_2_", String.valueOf(anError.getErrorBody()));
                                        Log.d("FELTOLTES_3_", String.valueOf(anError.getErrorDetail()));
                                        Log.d("FELTOLTES_4_", String.valueOf(anError.getErrorCode()));
                                    }
                                });
                        isuploading = false;

                    } else {
                        // wanna upload but no wifi
                    }
                }
                isuploading = false;
            } else {
                isuploading = false;
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
                Intent in = new Intent();
                showNotification(getApplicationContext(), "DETAILS", "Count: " + String.valueOf(count) + " (Service)"
                        + "\nLast Change: " + time
                        + "\nDistance: " + distance + " meters"
                        + "\nLongitude: " + longitude
                        + "\nLatitude: " + latitude
                        + "\nAddress: " + address
                        + "\nProvider: " + provider
                        + "\nSpeed: " + String.valueOf(round(mpsTokmh(Double.valueOf(speed)), 2)) + " km/h"
                        + "\nAccuracy: " + accuracy + " meters", in);
            } catch (Exception e) {
                Log.d("NOTIF EXCEPTION: ", e.toString());
            }
        }

    }

    public void saveRecordHttp(String path) {

        Thread th2 = new Thread() {
            public void run() {
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
                                if (response.trim() == "not recorded") {
                                    not_recorded++;
                                } else if (response.trim() == "recorded") {
                                    recorded++;
                                }
                            }

                            @Override
                            public void onError(ANError anError) {
                                urlList_failed.add(path);
                                Log.d("HTTP_ERROR_", anError.toString());
                            }
                        });
            }
        };
        th2.start();

    }

    public void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(50);
        }
    }

    public void aplist(final Context context, double lati, double longi) {
//        Thread thread = new Thread() {
        //public void run() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            nearbyCount = String.valueOf(scanResults.size());
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
                if (!macList_uniq.contains(result.BSSID)) {
                    macList_uniq.add(result.BSSID);
                    vibrate();
                }
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
                String deviceMan = android.os.Build.MANUFACTURER;
                if (deviceMan.equalsIgnoreCase("huawei")) {
                    cs.writeCsv_huawei("0" + "," + result.BSSID + "," + result.SSID + "," + convertDBM(result.level) + "," + DEVICE_ACCOUNT + "_v" + versionCode + "," + enc + "," + lati + "," + longi + "," + result.frequency + "," + time);
                } else {
                    cs.writeCsv("0" + "," + result.BSSID + "," + result.SSID + "," + convertDBM(result.level) + "," + DEVICE_ACCOUNT + "_v" + versionCode + "," + enc + "," + lati + "," + longi + "," + result.frequency + "," + time);
                }
            }
        } catch (
                Exception e) {
            Log.d("APP", "ERROR " + e.getMessage());
        }
        // }
        //};
        //thread.start();
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
        mNotificationManager.createNotificationChannel(new NotificationChannel("0", Title, NotificationManager.IMPORTANCE_DEFAULT));
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
                StringBuilder strReturnedAddress = new StringBuilder();

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

    public void showOngoing() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "new";
            String channelName = "new";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setShowBadge(true);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
            bigText.setBigContentTitle("UP and Running");
            bigText.bigText("Searching for WiFi networks");
            bigText.setSummaryText("Current Status");

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder
                    .setOngoing(true)
                    //.setStyle(bigText)
                    .setSmallIcon(R.drawable.search4)
                    .setGroup("wifi")
                    .setContentTitle("Running")
                    .setSubText("Searching")
                    .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setNumber(1)
                    .build();
            manager.notify(4, notification);
            startForeground(3, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showOngoing();
        Log.d("service,location", "asd");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        Log.d("service,location", "asd2");
        Log.d("service,location", "asd3");
        isRunning = true;
        Toast.makeText(getBaseContext(), "Service started", Toast.LENGTH_SHORT).show();

        onCreate();
        return START_STICKY;
        //return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(getBaseContext(), "Service stopped_1", Toast.LENGTH_SHORT).show();
        isRunning = false;
        stopForeground(true);
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(ns);
        for (int i = 0; i <= 10; i++) {
            nMgr.cancel(i);
            nMgr.cancelAll();
            stopForeground(i);
        }
    }

    public static boolean isRunning() {
        return isRunning;
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
            //Toast.MakeText(getBaseContext(), "GPS Unknown event: " + event, Toast.LENGTH_SHORT).show();
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
                ////Toast.MakeText(getBaseContext(), "GPS SAT Status", Toast.LENGTH_SHORT).show();
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

    public static boolean checkPermissionLocation(Context c) {

        if (ContextCompat.checkSelfPermission(c, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(c, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(c, "perm error", Toast.LENGTH_LONG).show();
        }

        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
