
package com.sontme.esp.getlocation;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
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
import com.sontme.esp.getlocation.activities.MainActivity;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class BackgroundService extends Service implements GpsStatus.Listener/*, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener*/ {

    //region GLOBAL / LOCAL VARIABLES
    int GpsInView;
    int GpsInUse;

    public static int getCount() {
        return count;
    }
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

    public static String accuracy;
    public static String latitude;
    public static String longitude;
    public static String speed;
    public static String altitude;
    public static String bearing;
    public static String time;
    public static String address;
    public static String provider;
    public static String distance;
    public static String initLat;
    public static String initLong;

    public static int count = 0;
    public static List<String> urlList_failed = new ArrayList<String>();
    public static List<String> urlList_successed = new ArrayList<String>();
    public static String lastSSID;
    public static String nearbyCount;
    public static List<String> uniqueAPS = new ArrayList<>();

    public static String ipaddress;
    public static String googleAccount;
    public static boolean isUploading = false;
    //endregion

    //region NEW CONSTANTS
    String CHANNEL_UPLOAD_ID = "100";
    String CHANNEL_UPLOAD_NAME = "upload";
    int CHANNEL_UPLOAD_NOTIF_ID = NotificationManager.IMPORTANCE_DEFAULT; // (3)

    int GPS_UPDATE_METERS = 1;
    int GPS_UPDATE_TIME = 1000;
    //endregion

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
    CsvExporter cs = new CsvExporter("wifilocator_database.csv");

    public Location previousLocation = new Location(LocationManager.GPS_PROVIDER);
    public static double sumOfTravelDistance = 0;

    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("BS_R", action);
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                Log.d("BLUETOOTH_", "pairing request !");
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("BLUETOOTH_", "Discovery FINISHED !");
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("BLUETOOTH_",
                        "BSERVICE_BL_NAME_" + device.getName() +
                                "_ADDRESS_" + device.getAddress());

                if (device.getAddress().equals("00:19:86:00:10:AE") && device.getName().equals("DESKTOP-0Q3JAI7")) {
                    // check if CHARGING
                    BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
                    ba.cancelDiscovery();
                    Log.d("BLUETOOTH_1", "BSERVICE_near_home ! discovery CANCELLED !");
                    Toast.makeText(getApplicationContext(), "Near home!", Toast.LENGTH_SHORT).show();
                }
            }
            if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action) ||
                    (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action))) {
                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo;
                wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) { // connection finished
                    if (wifiInfo.getSSID().contains("UPCAEDB2C3") || wifiInfo.getBSSID().contains("ac:22:05:47:39:f8")) {
                        Toast.makeText(getApplicationContext(), "Welcome home!", Toast.LENGTH_SHORT).show();
                        Log.d("WIFI__", "Welcome home!");
                    }
                } else if (wifiInfo.getSupplicantState() == SupplicantState.DISCONNECTED) {
                    Toast.makeText(getApplicationContext(), "Good bye!", Toast.LENGTH_SHORT).show();
                    Log.d("WIFI__", "Leaving home !");
                }
            }
            if (intent.getStringExtra("alarm") == "run") {
                Log.d("ALARM_", "ALARM RAN !");
            } else if (intent.getStringExtra("alarm") == "off") {
                locationManager.removeUpdates(locationListener);
            }
        }
    };

    @Override
    public void onCreate() {
        createNotifGroup("wifi", "wifi");
        SontHelper.vibrate(getApplicationContext(), 1, 50);
        try {
            AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            Account[] list = manager.getAccounts();

            String acc = "no";
            for (Account s : list) {
                acc = String.valueOf(s.name);
            }
            if (acc.length() > 3) {
                DEVICE_ACCOUNT = acc;
                DEVICE_ACCOUNT = DEVICE_ACCOUNT.replaceAll("[^0-9]", "");
            } else {
                DEVICE_ACCOUNT = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                        Settings.Secure.ANDROID_ID);
            }

            showOngoing();

            AndroidNetworking.initialize(getApplicationContext());

            // THIS IS NOT THE BROADCAST RECEIVER !!
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
            intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.EXTRA_WIFI_INFO);
            intentFilter.addAction(WifiManager.EXTRA_NETWORK_INFO);
            intentFilter.addAction(WifiManager.EXTRA_RESULTS_UPDATED);
            intentFilter.addAction(WifiManager.EXTRA_SUPPLICANT_CONNECTED);
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

            registerReceiver(broadcastReceiver, intentFilter);

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
            mService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            mService.addGpsStatusListener(this);

            // WHAT TO HAPPEN ON UNCAUGHT (try catch) EXCEPTION
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


            //region HTTP SERVER
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

            mCountDownTimer = new CountDownTimer(10000, 100000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // EVERY 10000 SECONDS
                }

                @Override
                public void onFinish() {
                    // EVERY 100000 SECONDS
                    if (hour > 5 && hour < 23) { // HA NAPPAL / IF DAYLIGHT
                        Log.d("ALARM_3", "ran...");
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
                                            String[] lines = response.split("\n");
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
                        UPLOAD_NIGHT = true;
                    }
                    mCountDownTimer.cancel();
                    mCountDownTimer.start();
                }
            };
            mCountDownTimer.start();

            // Every day 10:05 -> BroadcastReceiver.class -> "run"
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, 14);
            calendar.set(Calendar.MINUTE, 40);

            Intent intent = new Intent(getApplicationContext(), BroadcReceiver.class);
            intent.putExtra("alarm", "run");
            alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

            alarmMgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);

            turnGPSOn();
            startUpdatesGPS();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void uploadProgress(int prog, long uploaded, long total) {

        Context context = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = CHANNEL_UPLOAD_NOTIF_ID;
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        String channelId = CHANNEL_UPLOAD_ID;
        String channelName = CHANNEL_UPLOAD_NAME;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            mChannel.setImportance(NotificationManager.IMPORTANCE_MIN);
            mChannel.setShowBadge(true);

            notificationManager.createNotificationChannel(mChannel);
        }
        String detail = String.valueOf(prog);
        if (prog >= 100 || prog < 0) {
            detail = "Complete " + total + " bytes";
            prog = 100;
        } else {
            detail = "Progress: " + (int) (uploaded) + " / " + (int) (total);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.cloudupload)
                .setContentTitle("Uploading database")
                .setProgress(100, prog, false)
                .setOngoing(false)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
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

        int notificationId = CHANNEL_UPLOAD_NOTIF_ID;
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        String channelId = CHANNEL_UPLOAD_ID;
        String channelName = CHANNEL_UPLOAD_NAME;

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
            detail = "Progress: " + prog;
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.cloudupload)
                .setContentTitle("Uploading database")
                .setProgress(100, prog, false)
                .setOngoing(false)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setAutoCancel(true)
                .setVibrate(new long[]{0L})
                .setSound(null)
                .setLights(0xFFff0000, 600, 500)
                .setDefaults(Notification.FLAG_SHOW_LIGHTS)
                .setContentText(detail);

        notificationManager.notify(notificationId, mBuilder.build());

    }

    public void turnGPSOn() {
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
                mlocation = location;
                GpsInView = location.getExtras().getInt("satellites");
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                Log.d("GPS_LOCATION_CHANGE: ", location.toString());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("(service) Status Changed", String.valueOf(status));
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

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_TIME, GPS_UPDATE_METERS, locationListenerr);
    }

    public void queryLocation(Location LocRes) {
        try {
            float[] distancee = new float[1];

            Location.distanceBetween(LocRes.getLatitude(), LocRes.getLongitude(), previousLocation.getLatitude(), previousLocation.getLongitude(), distancee);
            distancee[0] = SontHelper.roundFloat(distancee[0]);

            if (distancee[0] >= 1 && distancee[0] <= 560000) {
                sumOfTravelDistance = sumOfTravelDistance + distancee[0];
            }

            previousLocation = LocRes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("NEW_QUERY_LOCATION: ", "Source: " + LocRes.getProvider());

        if (UPLOAD_NIGHT == false) {
            // CHECK IF CSV SIZE IS OVER 1 MEGABYTE YES -> Start UploadFileHTTP
            File f;
            String deviceMan = android.os.Build.MANUFACTURER;
            if (deviceMan.equalsIgnoreCase("huawei")) {
                f = new File("/data/user/0/com.sontme.esp.getlocation/files/wifilocator_database.csv");
            } else {
                f = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
            }
            if (f.length() / 1024 >= UPLOAD_SIZE_LIMIT) {
                if (isuploading == false) {
                    if (SontHelper.chk_3g_wifi(getApplicationContext()) == "wifi" || UPLOAD_3G == true) {
                        Log.d("UPLOAD_", "SIZE OK TO UPLOAD");
                        //Toast.MakeText(getBaseContext(), String.valueOf("Adatbázis feltöltése"), Toast.LENGTH_SHORT).show();
                        uploadProgress(0, 0, 0);
                        isuploading = true;
                        SontHelper.zipFileAtPath(f.getAbsolutePath(), f.getParent() + "/wifilocator_database.zip");
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
                                        Log.d("FELTOLTES_", "prog: " + prog);
                                        Log.d("FELTOLTES_", "progress left: " + (totalBytes - bytesUploaded));
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

            if (String.valueOf(LocRes.getLongitude()) != null ||
                    String.valueOf(LocRes.getLongitude()).length() >= 1) {
                try {
                    accuracy = String.valueOf(LocRes.getAccuracy());
                    latitude = String.valueOf(LocRes.getLatitude());
                    longitude = String.valueOf(LocRes.getLongitude());
                    speed = String.valueOf(SontHelper.round(SontHelper.mpsTokmh(LocRes.getSpeed()), 2));
                    altitude = String.valueOf(LocRes.getAltitude());
                    bearing = String.valueOf(LocRes.getBearing());
                    time = String.valueOf(SontHelper.convertTime(LocRes.getTime()));
                    address = SontHelper.getCompleteAddressString(getApplicationContext(), LocRes.getLatitude(), LocRes.getLongitude());
                    provider = LocRes.getProvider();
                    distance = String.valueOf(SontHelper.getDistance(Double.valueOf(latitude), Double.valueOf(initLat), Double.valueOf(longitude), Double.valueOf(initLong)));
                } catch (Exception e) {
                    Log.d("queryLocation()_", e.toString());
                    e.printStackTrace();
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
                e.printStackTrace();
            }

            if (latitude != null) {
                aplist(getApplicationContext(), Double.valueOf(latitude), Double.valueOf(longitude));
            }
            try {
                Intent in = new Intent(getApplicationContext(), MainActivity.class);
                showNotification(getApplicationContext(), "DETAILS", "Count: " + count + " (Service)"
                        + "\nLast Change: " + time
                        + "\nDistance: " + distance + " meters"
                        + "\nLongitude: " + longitude
                        + "\nLatitude: " + latitude
                        + "\nAddress: " + address
                        + "\nProvider: " + provider
                        + "\nSpeed: " + SontHelper.round(SontHelper.mpsTokmh(Double.valueOf(speed)), 2) + " km/h"
                        + "\nAccuracy: " + accuracy + " meters", in);
            } catch (Exception e) {
                Log.d("NOTIF EXCEPTION: ", e.toString());
                e.printStackTrace();
            }
        }

    }
    public void aplist(final Context context, double lati, double longi) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.isWifiEnabled() == true) {
                wifiManager.startScan(); // force scan
                List<ScanResult> scanResults = wifiManager.getScanResults();
                nearbyCount = String.valueOf(scanResults.size());
                int versionCode = BuildConfig.VERSION_CODE;
                for (ScanResult result : scanResults) {
                    lastSSID = result.SSID + " " + SontHelper.convertDBM(result.level) + "%";
                    if (!uniqueAPS.contains(result.BSSID)) {
                        uniqueAPS.add(result.BSSID);
                    }
                    String enc = "notavailable";
                    if (!result.capabilities.contains("WEP") ||
                            !result.capabilities.contains("WPA")) {
                        enc = "NONE";
                    } else if (result.capabilities.contains("WEP")) {
                        enc = "WEP";
                    } else if (result.capabilities.contains("WPA")) {
                        enc = "WPA";
                    } else if (result.capabilities.contains("WPA2")) {
                        enc = "WPA2";
                    }

                    String url = MainActivity.INSERT_URL;
                    String reqBody = "?id=0&ssid=" + result.SSID + "&add=service" + "&bssid=" + result.BSSID + "&source=" + DEVICE_ACCOUNT + "_v" + versionCode + "&enc=" + enc + "&rssi=" + SontHelper.convertDBM(result.level) + "&long=" + longi + "&lat=" + lati + "&channel=" + result.frequency;
                    if (!macList_uniq.contains(result.BSSID)) {
                        macList_uniq.add(result.BSSID);
                        SontHelper.vibrate(getApplicationContext());
                    }
                    if (!urlList_uniq.contains(url + reqBody)) {
                        urlList_uniq.add(url + reqBody);
                        saveRecordHttp(url + reqBody);
                        req_count++;
                    } else {

                    }
                    if (urlList_uniq.size() >= 5000) {
                        urlList_uniq.clear();
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String time = sdf.format(new Date());
                    String deviceMan = android.os.Build.MANUFACTURER;
                    if (deviceMan.equalsIgnoreCase("huawei")) {
                        cs.writeCsv_huawei("0" + "," + result.BSSID + "," + result.SSID + "," + SontHelper.convertDBM(result.level) + "," + DEVICE_ACCOUNT + "_v" + versionCode + "," + enc + "," + lati + "," + longi + "," + result.frequency + "," + time);
                    } else {
                        cs.writeCsv("0" + "," + result.BSSID + "," + result.SSID + "," + SontHelper.convertDBM(result.level) + "," + DEVICE_ACCOUNT + "_v" + versionCode + "," + enc + "," + lati + "," + longi + "," + result.frequency + "," + time);
                    }
                }
            }
        } catch (
                Exception e) {
            Log.d("APP", "ERROR " + e.getMessage());
            SontHelper.vibrate(getApplicationContext(), 255, 300);
            e.printStackTrace();
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
                                if (response.trim() == "not recorded") {
                                    not_recorded++;
                                } else if (response.trim() == "recorded") {
                                    recorded++;
                                }
                            }

                            @Override
                            public void onError(ANError anError) {
                                urlList_failed.add(path);
                                saveRecordHttp(path); // may REDUCE battery life
                                Log.d("HTTP_ERROR_", anError.toString());
                            }
                        });
            }
        };
        th2.start();
    }

    public void createNotifGroup(String id, String name) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        NotificationChannelGroup notificationChannelGroup =
                new NotificationChannelGroup(id, name);
        notificationManager.createNotificationChannelGroup(notificationChannelGroup);
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

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, intent, 0);

            RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notif_lay_up);
            contentView.setTextViewText(R.id.texttxt, "Running! ID: " + DEVICE_ACCOUNT);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.route)
                    .setGroup("wifi")
                    .setContentTitle("Running")
                    .setContent(contentView)
                    .setSubText("Searching")
                    .setContentIntent(pi)
                    .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setNumber(1)
                    .build();

            startForeground(35, notification);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                //manager.notify(4, notification);
            }
        }
    }
    public void showNotification(Context context, String title, String body, Intent intent) {

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notif_lay);
        String[] det = body.split("\\s+");

        contentView.setTextViewText(R.id.notif_ssid, "#" + count + " | #" + req_count + "/-" + urlList_failed.size() + " | " + lastSSID + " | " + det[6]);
        contentView.setTextViewText(R.id.notif_add, "Address: " + address);
        contentView.setTextViewText(R.id.notif_add_2, "Unique: " + uniqueAPS.size() + "/" + urlList_uniq.size() + "/" + recorded + " | GPS: " + GpsInView + "/" + GpsInUse);

        //region Unused
        //contentView.setTextViewText(R.id.notif_ssid, "Count #" + count);
        //contentView.setTextViewText(R.id.notif_time, "HTTP #" + req_count);
        //contentView.setTextViewText(R.id.notif_text2, det[6]);
        //contentView.setTextViewText(R.id.notif_text3, lastSSID);
        //contentView.setTextViewText(R.id.notif_lat, latitude);
        //contentView.setTextViewText(R.id.notif_long, longitude);
        //contentView.setTextViewText(R.id.notif_uniq, "Unique: " + uniqueAPS.size());
        //contentView.setTextViewText(R.id.notif_gps, "GPS Satellites: " + GpsInView + "_" + GpsInUse);
        //endregion

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(body);
        bigText.setBigContentTitle(title);
        bigText.setSummaryText("Current Status");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 0;
        String channelId = "0";
        String channelName = "0";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        Intent intent2 = new Intent(getBaseContext(), BroadcReceiver.class);
        Intent intent3 = new Intent(getBaseContext(), BroadcReceiver.class);
        Intent intent4 = new Intent(getBaseContext(), BroadcReceiver.class);
        intent2.setAction("exit");
        intent3.setAction("resume");
        intent4.setAction("pause");

        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(getApplicationContext(), 1, intent2, PendingIntent.FLAG_ONE_SHOT);
        PendingIntent pendingIntent3 = PendingIntent.getBroadcast(getApplicationContext(), 1, intent3, PendingIntent.FLAG_ONE_SHOT);
        PendingIntent pendingIntent4 = PendingIntent.getBroadcast(getApplicationContext(), 1, intent4, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.computer_low)
                .setContent(contentView)
                .addAction(R.drawable.computer_low_gray, "Pause", pendingIntent4)
                .addAction(R.drawable.computer_low_gray, "Resume", pendingIntent3)
                .addAction(R.drawable.computer_low_gray, "Exit", pendingIntent2)
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        SontHelper.showToast(getApplicationContext(), "Service started");
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        SontHelper.showToast(getApplicationContext(), "Service stopped_1");
        stopForeground(true);
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(ns);
        nMgr.cancelAll();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        //mStatus = mService.getGpsStatus(mStatus);
        if (event != GpsStatus.GPS_EVENT_FIRST_FIX &&
                event != GpsStatus.GPS_EVENT_SATELLITE_STATUS &&
                event != GpsStatus.GPS_EVENT_STARTED &&
                event != GpsStatus.GPS_EVENT_STOPPED) {
            Toast.makeText(getApplicationContext(), "GPS Unknown event: " + event, Toast.LENGTH_SHORT).show();
        }
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                Toast.makeText(getApplicationContext(), "GPS Connected", Toast.LENGTH_SHORT).show();
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                Toast.makeText(getApplicationContext(), "GPS Lost Signal", Toast.LENGTH_SHORT).show();
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                Toast.makeText(getApplicationContext(), "GPS FIRST Location", Toast.LENGTH_SHORT).show();
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

