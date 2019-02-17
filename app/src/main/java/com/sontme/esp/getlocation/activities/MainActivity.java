package com.sontme.esp.getlocation.activities;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.sontme.esp.getlocation.BackgroundService;
import com.sontme.esp.getlocation.BuildConfig;
import com.sontme.esp.getlocation.Global;
import com.sontme.esp.getlocation.HandleLocations;
import com.sontme.esp.getlocation.R;
import com.sontme.esp.getlocation.Receiver;

import java.io.IOException;
import java.io.InputStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;
import io.fabric.sdk.android.Fabric;

import android.support.design.widget.NavigationView;

import org.osmdroid.util.LocationUtils;
import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity {
    //region DEFINING VARIABLES

    public int counter = 0;
    public double initialLong;
    public double initialLat;
    public PublisherAdView mPublisherAdView;

    public Context context = this;


    public static TextView alti;
    public static TextView longi;
    public static TextView lati;
    public static TextView spd;
    public static TextView c;
    public static TextView dst;
    public static Button start_srv;
    public static Button stop_srv;
    public static Button exitb;
    public static TextView srvStatus;
    public static TextView add;

    //endregion
    Location mlocation;
    Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            queryLocation();
            try {
                longi.setText("Longitude: " + Global.longitude);
                lati.setText("Latitude: " + Global.latitude);
                alti.setText("Altitude: " + Global.altitude);
                spd.setText("Speed: " + Global.speed + " km/h");
                dst.setText("Distance: " + round(Double.valueOf(Global.distance), 2) + " meters");
                add.setText("Address: " + Global.address);
                c.setText("Count: " + Global.count);
            } catch (Exception e) {
                Log.d("RUNEXCEPTION: ", e.toString());
            }
            handler.postDelayed(this, 2000);
        }
    };

    public void queryLocation() {
        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
                Log.d("Location Changes", location.toString());

                Global.latitude = String.valueOf(location.getLatitude());
                Global.longitude = String.valueOf(location.getLongitude());
                Global.accuracy = String.valueOf(location.getAccuracy());
                Global.altitude = String.valueOf(location.getAltitude());
                Global.bearing = String.valueOf(location.getBearing());
                Global.speed = String.valueOf(mpsTokmh(location.getSpeed()));
                Global.time = String.valueOf(location.getTime());
                Global.distance = String.valueOf(getDistance(Double.valueOf(Global.latitude), initialLat, Double.valueOf(Global.longitude), initialLong));
                Global.address = getCompleteAddressString(Double.valueOf(Global.latitude), Double.valueOf(Global.longitude));

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("Status Changed", String.valueOf(status));
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d("Provider Enabled", provider);
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
        final Looper looper = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d("Location PROVIDER: " + Global.count + " ", locationManager.getBestProvider(criteria, false));
        locationManager.requestSingleUpdate(criteria, locationListener, looper);
        try {
            if (Double.valueOf(Global.latitude) != 0 && Double.valueOf(Global.longitude) != 0) {
                counter++;
                Global.count++;
                if (counter == 1) {
                    // START POSITION (Activity/Program start)
                    initialLong = Double.valueOf(Global.longitude);
                    initialLat = Double.valueOf(Global.latitude);
                }
            }
            Log.d("INITIAL", String.valueOf(initialLat) + String.valueOf(initialLong));
        } catch (Exception e) {
        }

        if (Global.latitude != null) {
            aplist(getBaseContext(), Double.valueOf(Global.latitude), Double.valueOf(Global.longitude));
        }
        try {
            showNotif("WIFI Locator", "Count: " + String.valueOf(counter)
                    + "\nLast Change: " + convertTime(Long.valueOf(Global.time))
                    + "\nDistance: " + Global.distance + " meters"
                    + "\nLongitude: " + Global.longitude
                    + "\nLatitude: " + Global.latitude
                    + "\nAddress: " + Global.address
                    + "\nSpeed: " + String.valueOf(round(Double.valueOf(Global.speed), 2)) + " km/h"
                    + "\nAccuracy: " + Global.accuracy + " meters");
        } catch (Exception e) {
        }
    }

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        init();
        logUser();

        dl = (DrawerLayout) findViewById(R.id.drawler);
        t = new ActionBarDrawerToggle(this, dl, R.string.Open, R.string.Close);
        dl.addDrawerListener(t);
        t.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        nv = findViewById(R.id.nv);
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.main:
                        dl.closeDrawers();
                        return true;
                    case R.id.map:
                        dl.closeDrawers();
                        Intent i = new Intent(MainActivity.this, MapActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.list:
                        dl.closeDrawers();
                        Intent i2 = new Intent(MainActivity.this, ListActivity.class);
                        startActivity(i2);
                        return true;
                    case R.id.nearby:
                        dl.closeDrawers();
                        Intent i4 = new Intent(MainActivity.this, NearbyActivity.class);
                        startActivity(i4);
                        return true;
                    default:
                        dl.closeDrawers();
                        return true;
                }
            }
        });

        mPublisherAdView = findViewById(R.id.publisherAdView);
        PublisherAdRequest adRequest = new PublisherAdRequest.Builder().build();
        mPublisherAdView.loadAd(adRequest);
        mPublisherAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Toast.makeText(getBaseContext(), "Advertisement loaded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                if (errorCode == 3) {
                } else {
                    // CODE NOT OK
                }
            }
        });

        //region UI ELEMENTS
        alti = (TextView) findViewById(R.id.alti);
        add = (TextView) findViewById(R.id.add);
        c = (TextView) findViewById(R.id.c);
        longi = (TextView) findViewById(R.id.longi);
        lati = (TextView) findViewById(R.id.lati);
        spd = (TextView) findViewById(R.id.spd);
        dst = (TextView) findViewById(R.id.dst);
        start_srv = (Button) findViewById(R.id.srv);
        stop_srv = (Button) findViewById(R.id.stop_srv);
        exitb = (Button) findViewById(R.id.exitb);
        srvStatus = (TextView) findViewById(R.id.srvStatus);

        //endregion
        //region UI ELEMENT LISTENERS


        exitb.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                finish();
                finishAffinity();
                System.exit(0);
            }
        });

        start_srv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMyServiceRunning(BackgroundService.class)) {
                    Toast.makeText(getBaseContext(), "Service is already running!", Toast.LENGTH_SHORT).show();
                } else {
                    Intent serviceIntent = new Intent(getApplicationContext(), BackgroundService.class);
                    serviceIntent.putExtra("serviceinterval", String.valueOf(1000));
                    serviceIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    serviceIntent.setAction(Intent.ACTION_MAIN);
                    serviceIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                }
            }
        });
        stop_srv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMyServiceRunning(BackgroundService.class)) {
                    Toast.makeText(getBaseContext(), "No service running!", Toast.LENGTH_SHORT).show();
                } else {
                    stopService(new Intent(getBaseContext(), BackgroundService.class));
                }
            }
        });
        //endregion

        View hView = nv.getHeaderView(0);
        TextView tex = (TextView) hView.findViewById(R.id.header_verinfo);
        String version = "Version: " + String.valueOf(BuildConfig.VERSION_NAME) + " Build: " + String.valueOf(BuildConfig.VERSION_CODE);
        tex.setText(version);

        turnGPSOn();
        queryLocation();
        handler.postDelayed(runnable, 5000);
        // showNotif("WiFi Locator", "Application started!");

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (t.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    public static int ConvertDBM(int dbm) {
        int quality;
        if (dbm <= -100)
            quality = 0;
        else if (dbm >= -50)
            quality = 100;
        else
            quality = 2 * (dbm + 100);
        return quality;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public double mpsTokmh(double mps) {
        return mps * 3.6;
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public String convertTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");
        return format.format(date);
    }

    public void showNotif(String Title, String Text) {

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notif_lay);
        //contentView.setImageViewResource(R.id.notif_img, R.drawable.computer_low);
        //contentView.setTextViewText(R.id.notif_title, Title);

        String[] det = Text.split("\\s+");
        contentView.setTextViewText(R.id.notif_ssid, "SSID #" + counter);
        contentView.setTextViewText(R.id.notif_time, "Time #" + Global.lastNearby);
        contentView.setTextViewText(R.id.notif_text2, "" + det[4] + " " + det[5]);
        contentView.setTextViewText(R.id.notif_text3, Global.lastSSID);
        contentView.setTextViewText(R.id.notif_lat, Global.latitude);
        contentView.setTextViewText(R.id.notif_long, Global.longitude);
        contentView.setTextViewText(R.id.notif_add, Global.address);

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
        //mBuilder.setContentTitle(Title);
        //mBuilder.setContentText(Text);
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

    public ArrayList<String> aplist(final Context context, double lati, double longi) {
        ArrayList<String> apList = new ArrayList<String>();
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            Log.d("APLISTCOUNT: ", String.valueOf(scanResults.size()));

            for (ScanResult result : scanResults) {
                Global.lastSSID = result.SSID + " " + ConvertDBM(result.level) + "%";
                Global.lastNearby = String.valueOf(scanResults.size());
                String enc = "notavailable";
                if (!result.capabilities.contains("WEP") || !result.capabilities.contains("WPA")) {
                    enc = "NONE";
                } else if (result.capabilities.contains("WEP")) {
                    enc = "WEP";
                } else if (result.capabilities.contains("WPA")) {
                    enc = "WPA";
                }
                String android_id = Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                int versionCode = BuildConfig.VERSION_CODE;
                //String versionName = BuildConfig.VERSION_NAME;
                String url = "http://sont.sytes.net/mcuinsert2.php";
                String reqBody = "?id=0&ssid=" + result.SSID + "&bssid=" + result.BSSID + "&source=" + android_id + "_v" + versionCode + "&enc=" + enc + "&rssi=" + ConvertDBM(result.level) + "&long=" + longi + "&lat=" + lati + "&add=" + "addition" + "&channel=" + result.frequency;
                saveRecordHttp(url + reqBody);
            }
        } catch (Exception e) {
            Log.d("APP", "ERROR " + e.getMessage());
        }
        return apList;
    }

    public void logUser() {
        Crashlytics.setUserIdentifier("12345");
        Crashlytics.setUserEmail("sont16@gmail.com");
        Crashlytics.setUserName("wifilocatoruser");
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

    public void init() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // LOCATION PERMISSION CHECK IF NOT ASK FOR IT
        if (checkPermissionLocation() == false) {
            Toast.makeText(getBaseContext(), "Missing location permission!", Toast.LENGTH_SHORT).show();
            requestPermissionLocation();
        }
        // TURN ON WIFI
        if (!wifi.isWifiEnabled()) {
            Toast.makeText(getBaseContext(), "Turning on WiFi..", Toast.LENGTH_SHORT).show();
            wifi.setWifiEnabled(true);
        }
    }

    public static void saveRecordHttp(String path) {
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
                Log.d("HTTP", "Error code: " + statusCode);
            }
        });
    }

    public boolean checkPermissionLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    public void requestPermissionLocation() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
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
            e.printStackTrace();
        }
        return strAdd;
    }

    private void turnGPSOn(){
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if(!provider.contains("gps")){ //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            sendBroadcast(poke);
        }
    }
}


