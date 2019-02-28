package com.sontme.esp.getlocation.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;


import com.crashlytics.android.Crashlytics;
import com.ederdoski.simpleble.interfaces.BleCallback;
import com.ederdoski.simpleble.models.BluetoothLE;
import com.ederdoski.simpleble.utils.BluetoothLEHelper;
import com.github.johnpersano.supertoasts.library.Style;
import com.github.johnpersano.supertoasts.library.SuperActivityToast;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.sontme.esp.getlocation.BackgroundService;
import com.sontme.esp.getlocation.BuildConfig;
import com.sontme.esp.getlocation.Global;
import com.sontme.esp.getlocation.R;
import com.sontme.esp.getlocation.Receiver;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import cz.msebera.android.httpclient.Header;
import io.fabric.sdk.android.Fabric;
import lecho.lib.hellocharts.formatter.SimpleLineChartValueFormatter;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.ChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.view.Chart;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PieChartView;
import okhttp3.WebSocket;

import android.support.design.widget.NavigationView;

import org.w3c.dom.Text;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;


public class MainActivity extends AppCompatActivity {
    //region DEFINING VARIABLES

    public int counter = 0;
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
    public static TextView add;
    public static TextView provider;
    public static TextView uniq;
    public static TextView servicestatus;
    public static Button startServiceBtn;
    public static Button stopServiceBtn;
    public static Button release_btn;

    //endregion

    public DevicePolicyManager mDPM;
    public ComponentName mAdminName;

    public Location mlocation;
    public LocationRequest mPlayLocationRequest;
    public Handler handler = new Handler();
    public BackgroundService backgroundService;

    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                longi.setText(Global.longitude);
                lati.setText(Global.latitude);
                alti.setText(Global.altitude);
                spd.setText(Global.speed + " km/h");
                dst.setText(String.valueOf(Global.round(Double.valueOf(Global.distance), 2) + " meters"));
                add.setText(Global.address);
                c.setText(Global.count);
                provider.setText(Global.provider);
                uniq.setText(Global.uniqueAPS.size());
                WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
                String ipv4 = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                Global.ipaddress = ipv4;
                TextView ip = findViewById(R.id.ip);
                ip.setText(Global.ipaddress);
                servicestatus.setText("Not yet available");

                queryLocation(null);
            } catch (Exception e) {
            }
            if (Global.longitude == null) {
                alti.setText("0");
                longi.setText("0");
                lati.setText("0");
                spd.setText("0.00 km/h");
                dst.setText("0.00 meter");
                add.setText("Not available");
                c.setText("0");
                provider.setText("Not available");
                uniq.setText("0");
                servicestatus.setText("Not available");
                TextView ip = findViewById(R.id.ip);
                ip.setText(Global.ipaddress);
            }
            handler.postDelayed(this, 1000);
        }
    };

    public void queryLocation(Location LocRes) {
        if (String.valueOf(LocRes.getLongitude()) != null || String.valueOf(LocRes.getLongitude()).length() >= 1) {
            try {
                WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
                String ipv4 = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                Global.accuracy = String.valueOf(LocRes.getAccuracy());
                Global.latitude = String.valueOf(LocRes.getLatitude());
                Global.longitude = String.valueOf(LocRes.getLongitude());
                Global.speed = String.valueOf(Global.round(mpsTokmh(LocRes.getSpeed()), 2));
                Global.altitude = String.valueOf(LocRes.getAltitude());
                Global.bearing = String.valueOf(LocRes.getBearing());
                Global.time = String.valueOf(Global.convertTime(getBaseContext(), LocRes.getTime()));
                Global.address = Global.getCompleteAddressString(getBaseContext(), LocRes.getLatitude(), LocRes.getLongitude());
                Global.provider = LocRes.getProvider();
                Global.distance = String.valueOf(Global.getDistance(Double.valueOf(Global.latitude), Double.valueOf(Global.initLat), Double.valueOf(Global.longitude), Double.valueOf(Global.initLong)));
                Global.ipaddress = ipv4;
            } catch (Exception e) {
            }
            Log.d("GOOGLEAPIPLAY ", LocRes.toString());
        }
        try {
            if (Double.valueOf(Global.latitude) != 0 && Double.valueOf(Global.longitude) != 0) {
                counter++;
                Global.count++;
                if (counter == 1) {
                    // START POSITION (Activity/Program start)
                    Global.initLat = Global.latitude;
                    Global.initLong = Global.longitude;
                }
            }
            Log.d("INITIAL", String.valueOf(Global.initLat) + String.valueOf(Global.initLong));
        } catch (Exception e) {
        }

        if (Global.latitude != null) {
            aplist(getBaseContext(), Double.valueOf(Global.latitude), Double.valueOf(Global.longitude));
        }
        try {
            showNotif("WIFI Locator", "Count: " + String.valueOf(counter)
                    + "\nLast Change: " + Global.time
                    + "\nDistance: " + Global.distance + " meters"
                    + "\nLongitude: " + Global.longitude
                    + "\nLatitude: " + Global.latitude
                    + "\nAddress: " + Global.address
                    + "\nProvider: " + Global.provider
                    + "\nSpeed: " + String.valueOf(Global.round(mpsTokmh(Double.valueOf(Global.speed)), 2)) + " km/h"
                    + "\nAccuracy: " + Global.accuracy + " meters");
        } catch (Exception e) {
            Log.d("NOTIF EXCEPTION: ", e.toString());
        }
    }

    public DrawerLayout dl;
    public ActionBarDrawerToggle t;
    public NavigationView nv;

    //String INSERT_URL = "https://sont.sytes.net/mcuinsert2.php";
    String INSERT_URL = "https://sont.sytes.net/wifi_insert.php";
    String myColors[] = {"#f857b5", "#f781bc", "#fdffdc", "#c5ecbe", "#00b8a9", "#f8f3d4", "#f6416c", "#ffde7d"};
    Map<String, String> BLEdevices = new HashMap<String, String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        init();
        logUser();
        adminPermission();

        String ipv4 = Global.getLocalIpAddress();
        Global.ipaddress = ipv4;
        TextView ip = findViewById(R.id.ip);
        ip.setText(Global.ipaddress);


        //region DRAWER
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
                SuperActivityToast superToast = new SuperActivityToast(MainActivity.this);
                superToast.setText("Advertisement loaded");
                superToast.setAnimations(Style.ANIMATIONS_SCALE);
                superToast.setDuration(Style.DURATION_LONG);
                superToast.setTouchToDismiss(true);
                superToast.show();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                if (errorCode == 3) {
                } else {
                    // CODE NOT OK
                }
            }
        });
        //endregion
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
        provider = findViewById(R.id.prov);
        uniq = findViewById(R.id.uniq);
        WebView webview = findViewById(R.id.webview);
        startServiceBtn = findViewById(R.id.startService);
        stopServiceBtn = findViewById(R.id.stopService);
        servicestatus = findViewById(R.id.servstatus);
        release_btn = findViewById(R.id.release_btn);
        Button blebtn1 = findViewById(R.id.blebtn1);
        Button blebtn2 = findViewById(R.id.blebtn2);

        webview.clearCache(true);
        webview.clearHistory();
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setAllowFileAccessFromFileURLs(true);
        webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webview.getSettings().setAppCacheEnabled(true);
        webview.getSettings().setLoadsImagesAutomatically(true);
        webview.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.setInitialScale(1);
        webview.setBackgroundColor(Color.argb(100, 234, 234, 234));
        webview.loadUrl("https://sont.sytes.net/osm.php");

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
                startUpdatesPlay();
            }

        });
        stop_srv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpdatesGPS();
            }
        });
        startServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), "Starting service", Toast.LENGTH_SHORT);
                //WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
                //String ipv4 = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                //ip.setText(ipv4);
                startService(new Intent(MainActivity.this, BackgroundService.class));
                /*if(!isMyServiceRunning(BackgroundService.class)) {
                    try {
                        startService(new Intent(MainActivity.this, BackgroundService.class));
                    }catch (Exception e){
                        Toast.makeText(getBaseContext(),"Service start error: " + e.getMessage(),Toast.LENGTH_LONG);
                    }
                }
                else{
                    Toast.makeText(getBaseContext(),"Service is ALREADY running",Toast.LENGTH_SHORT);
                }*/
            }
        });
        stopServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Stopping service", Toast.LENGTH_SHORT);
                Intent myService = new Intent(MainActivity.this, BackgroundService.class);
                stopService(myService);
            }
        });
        release_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                releaseQueue();
            }
        });
        blebtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothAdapter bluetoothAdapter;
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                bluetoothAdapter = bluetoothManager.getAdapter();
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 5);
                }
                bluetoothAdapter.startDiscovery();
                bluetoothAdapter.startLeScan(leScanCallback);


            }
        });
        blebtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bl_list = null;

                for (String key : BLEdevices.keySet()) {
                    System.out.println("key : " + key);
                    System.out.println("value : " + BLEdevices.get(key));
                    if (bl_list == null) {
                        bl_list = "Name: " + key + " Address: " + BLEdevices.get(key) + "\n\n";
                    } else {
                        bl_list = bl_list + "Name: " + key + " Address: " + BLEdevices.get(key) + "\n\n";
                    }
                }

                new AlertDialog.Builder(context)
                        .setTitle("BLE Devices")
                        .setMessage(bl_list)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNegativeButton("Stop BLE Listening", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BluetoothAdapter bluetoothAdapter;
                                final BluetoothManager bluetoothManager =
                                        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                                bluetoothAdapter = bluetoothManager.getAdapter();

                                bluetoothAdapter.stopLeScan(leScanCallback);
                                bluetoothAdapter.cancelDiscovery();
                                if (bluetoothAdapter.isEnabled()) {
                                    bluetoothAdapter.disable();
                                }
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
        //endregion

        View hView = nv.getHeaderView(0);
        TextView tex = (TextView) hView.findViewById(R.id.header_verinfo);
        String version = "Version: " + String.valueOf(BuildConfig.VERSION_NAME) + " Build: " + String.valueOf(BuildConfig.VERSION_CODE);
        tex.setText(version);

        turnGPSOn();
        handler.postDelayed(runnable, 1000);

        Intent mIntent = new Intent(MainActivity.this, BackgroundService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        String android_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Log.d("ANDROIDID:", android_id);
        if (android_id.equals("73bedfbd149e01de")) {
            Log.d("PHONE:", "SAJAT");
        } else /*if(android_id.equals("ae3b8f5d1877b6ec"))*/ {
            Intent fIntent = new Intent(getBaseContext(), BackgroundService.class);
            startService(fIntent);
            Toast.makeText(getBaseContext(), "Started Service Autimatically", Toast.LENGTH_SHORT).show();
        }
        getChartHttp("https://sont.sytes.net/wifis_chart.php");
        getChartHttp2("https://sont.sytes.net/wifis_chart_2.php");
    }

    @Override
    public void onStop() {
        super.onStop();
        /*
        SuperActivityToast superToast = new SuperActivityToast(MainActivity.this);
        superToast.setText("Exiting");
        superToast.setAnimations(Style.ANIMATIONS_SCALE);
        superToast.setDuration(Style.DURATION_VERY_SHORT);
        superToast.setTouchToDismiss(true);
        superToast.show();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        */
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplicationContext(), "Service is disconnected", Toast.LENGTH_SHORT).show();
            backgroundService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Toast.makeText(getApplicationContext(), "Service is connected", Toast.LENGTH_SHORT).show();
            BackgroundService.LocalBinder mLocalBinder = (BackgroundService.LocalBinder) service;
            backgroundService = mLocalBinder.getServerInstance();
        }
    };

    public void startUpdatesGPS() {
        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
                Log.d("Location Changes", location.toString());

                queryLocation(location);
                Global.provider = location.getProvider();
                provider.setText(Global.provider);
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
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    public void startUpdatesPlay() {
        mPlayLocationRequest = new LocationRequest();
        mPlayLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mPlayLocationRequest.setInterval(1000);
        mPlayLocationRequest.setFastestInterval(500);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mPlayLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(getApplicationContext());
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        getFusedLocationProviderClient(getApplicationContext()).requestLocationUpdates(mPlayLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        queryLocation(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("BLE_DEVICE_FOUND_", device.getName() + " _ " + device.getAddress() + " _ " + device.getBondState() + " _ " + device.getUuids());
                            BLEdevices.put(device.getName(), device.getAddress() + " UUID: " + device.getUuids());
                            SuperActivityToast superToast = new SuperActivityToast(MainActivity.this);
                            superToast.setText("BLE Device found: Name: " + device.getName() + " Address: " + device.getAddress() + " UUID: " + device.getUuids());
                            superToast.setAnimations(Style.ANIMATIONS_SCALE);
                            superToast.setDuration(Style.DURATION_VERY_LONG);
                            superToast.setTouchToDismiss(true);
                            superToast.show();
                        }
                    });
                }
            };

    @Override
    public void onResume() {
        super.onResume();
        SuperActivityToast superToast = new SuperActivityToast(MainActivity.this);
        superToast.setText("Resuming");
        superToast.setAnimations(Style.ANIMATIONS_SCALE);
        superToast.setDuration(Style.DURATION_VERY_SHORT);
        superToast.setTouchToDismiss(true);
        superToast.show();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (t.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    public double mpsTokmh(double mps) {
        return mps * 3.6;
    }

    public void showNotif(String Title, String Text) {

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notif_lay);
        String[] det = Text.split("\\s+");
        contentView.setTextViewText(R.id.notif_ssid, "SSID #" + counter);
        contentView.setTextViewText(R.id.notif_time, "Time #" + Global.lastNearby);
        contentView.setTextViewText(R.id.notif_text2, "" + det[4] + " " + det[5]);
        contentView.setTextViewText(R.id.notif_text3, Global.lastSSID);
        contentView.setTextViewText(R.id.notif_lat, Global.latitude);
        contentView.setTextViewText(R.id.notif_long, Global.longitude);
        contentView.setTextViewText(R.id.notif_add, Global.address);
        //contentView.setTextViewText(R.id.notif_uniq, ""+String.valueOf(Global.nearbyCount));
        contentView.setTextViewText(R.id.notif_uniq, "Unique APs found: " + String.valueOf(Global.uniqueAPS.size()));

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

    public void aplist(final Context context, double lati, double longi) {
        //Map<String, Integer> map = new HashMap<String, Integer>();

        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult result : scanResults) {
                Global.lastSSID = result.SSID + " " + Global.convertDBM(result.level) + "%";
                Global.lastNearby = String.valueOf(scanResults.size());
                Global.nearbyCount = scanResults.size();
                //map.put(result.SSID, Global.convertDBM(result.level));
                if (!Global.uniqueAPS.contains(result.BSSID)) {
                    Global.uniqueAPS.add(result.BSSID);
                }
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
                String url = INSERT_URL;
                String reqBody = "?id=0&ssid=" + result.SSID + "&bssid=" + result.BSSID + "&source=" + android_id + "_v" + versionCode + "&enc=" + enc + "&rssi=" + Global.convertDBM(result.level) + "&long=" + longi + "&lat=" + lati + "&add=" + "addition" + "&channel=" + result.frequency;
                if (!Global.queue.contains(url + reqBody)) {
                    Global.queue.add(url + reqBody);
                }
                saveRecordHttp(url + reqBody);
            }

        } catch (Exception e) {
            Log.d("APP", "ERROR " + e.getMessage());
        }
    }

    public void logUser() {
        Crashlytics.setUserIdentifier("12345");
        Crashlytics.setUserEmail("sont16@gmail.com");
        Crashlytics.setUserName("wifilocatoruser");
    }

    public void init() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // LOCATION PERMISSION CHECK IF NOT ASK FOR IT
        if (Global.checkPermissionLocation(getApplicationContext()) == false) {
            SuperActivityToast superToast = new SuperActivityToast(MainActivity.this);
            superToast.setText("Missing LOCATION PERMISSION");
            superToast.setAnimations(Style.ANIMATIONS_SCALE);
            superToast.setDuration(Style.DURATION_LONG);
            superToast.setTouchToDismiss(true);
            superToast.show();
            requestPermissionLocation();
        }
        // TURN ON WIFI
        if (!wifi.isWifiEnabled()) {
            SuperActivityToast superToast = new SuperActivityToast(MainActivity.this);
            superToast.setText("Turning on WiFi");
            superToast.setAnimations(Style.ANIMATIONS_SCALE);
            superToast.setDuration(Style.DURATION_LONG);
            superToast.setTouchToDismiss(true);
            superToast.show();
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

    public void getChartHttp(String path) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(path, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                LineChartView chart = findViewById(R.id.chart);
                List<PointValue> values = new ArrayList<PointValue>();

                String str = null;
                try {
                    str = new String(responseBody, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String lines[] = str.trim().split("\\r?\\n");
                int i = 0;
                try {
                    for (String line : lines) {
                        String[] words = line.trim().split("\\s+");
                        values.add(new PointValue(i, Integer.valueOf(words[1])).setLabel(String.valueOf(i)));
                        i++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Collections.shuffle(Arrays.asList(myColors));
                Line line = new Line(values).setColor(Color.parseColor(myColors[0]));
                line.setStrokeWidth(5);
                List<Line> liness = new ArrayList<Line>();
                liness.add(line);
                LineChartData data = new LineChartData();
                data.setLines(liness);
                chart.setOnValueTouchListener(new ValueTouchListener());
                chart.setLineChartData(data);
            }

            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                getChartHttp("https://sont.sytes.net/wifis_chart.php");
            }
        });
    }

    public void getChartHttp2(String path) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(path, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                PieChartView pieChartView = findViewById(R.id.chart2);
                List<SliceValue> values = new ArrayList<SliceValue>();

                String str = null;
                try {
                    str = new String(responseBody, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String lines[] = str.trim().split("xxx");
                try {
                    int i = 0;
                    Collections.shuffle(Arrays.asList(myColors));
                    for (String line : lines) {
                        String[] words = line.trim().split("\\s+");
                        values.add(new SliceValue(Float.valueOf(words[1])).setLabel(words[0]).setColor(Color.parseColor(myColors[i])));
                        i++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                PieChartData pieChartData = new PieChartData(values);
                pieChartView.setPieChartData(pieChartData);
                pieChartData.setCenterText1("Recorded data per Device");
                pieChartData.setHasCenterCircle(true);
                pieChartData.setHasLabels(true);
                pieChartData.setHasLabelsOnlyForSelected(true);
                pieChartData.setHasLabelsOutside(true);
                pieChartView.setValueSelectionEnabled(true);

                pieChartData.setValues(values);

            }

            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                getChartHttp2("https://sont.sytes.net/wifis_chart.php");
            }
        });
    }

    public void releaseQueue() {
        Iterator it = Global.queue.iterator();
        int i = 0;
        while (it.hasNext()) {
            String iteratorValue = (String) it.next();
            saveRecordHttp(iteratorValue);
            i++;
        }
        SuperActivityToast superToast = new SuperActivityToast(MainActivity.this);
        String txt = "Queue Released (" + i + ")";
        superToast.setText(txt);
        superToast.setAnimations(Style.ANIMATIONS_SCALE);
        superToast.setDuration(Style.DURATION_LONG);
        superToast.setTouchToDismiss(true);
        superToast.show();
    }

    public void requestPermissionLocation() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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

    public void adminPermission() {
        try {
            if (!mDPM.isAdminActive(mAdminName)) {
                try {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "extrainfo");
                    startActivityForResult(intent, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ValueTouchListener implements LineChartOnValueSelectListener {
        @Override
        public void onValueSelected(int lineIndex, int pointIndex, PointValue value) {
            Toast.makeText(getApplicationContext(), String.valueOf(value.getLabel()), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onValueDeselected() {
        }
    }
}
