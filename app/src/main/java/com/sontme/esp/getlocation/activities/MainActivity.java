package com.sontme.esp.getlocation.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
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
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.javiersantos.materialstyleddialogs.enums.Duration;
import com.github.johnpersano.supertoasts.library.Style;
import com.github.johnpersano.supertoasts.library.SuperActivityToast;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.sontme.esp.getlocation.BackgroundService;
import com.sontme.esp.getlocation.BuildConfig;
import com.sontme.esp.getlocation.Global;
import com.sontme.esp.getlocation.R;
import com.sontme.esp.getlocation.Receiver;

import com.sontme.esp.getlocation.UploadFileFTP;
import com.sontme.esp.getlocation.UploadFileHTTP;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import io.fabric.sdk.android.Fabric;

import android.support.design.widget.NavigationView;

import org.json.JSONObject;
import org.w3c.dom.Text;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MainActivity extends AppCompatActivity implements GpsStatus.Listener {
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
    public static TextView txt_stat1;
    public static TextView val_succ;
    public static TextView csv;
    public static TextView zip;

    public static int retry_counter_1 = 0;
    public static int retry_counter_2 = 0;
    public static int retry_counter_3 = 0;
    //endregion

    private TextView val_errors;
    private Button btn_upload_http;
    WifiManager wm;
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
//                c.setText(Global.count);
                provider.setText(Global.provider);
//                uniq.setText(Global.uniqueAPS.size());
                WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
                String ipv4 = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                Global.ipaddress = ipv4;
                TextView ip = findViewById(R.id.ip);
                ip.setText(Global.ipaddress);
                servicestatus.setText("Not yet available");

                File f1 = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
                File f2 = new File("/storage/emulated/0/Documents/wifilocator_database.zip");
                TextView csv1 = findViewById(R.id.val_csv);
                TextView zip1 = findViewById(R.id.val_zip);
                csv1.setText(String.valueOf((int) (f1.length()) / 1024) + " kb");
                zip1.setText(String.valueOf((int) (f2.length()) / 1024) + " kb");
                val_errors = findViewById(R.id.val_error);
                val_errors.setText(String.valueOf(Global.urlList_failed.size()));
                val_succ = findViewById(R.id.val_succ);
                val_succ.setText(String.valueOf(Global.urlList_successed.size()));

                csv.setText(Global.csvSize);
                zip.setText(Global.zipSize);

                //queryLocation(null);
            } catch (Exception e) {
                Log.d("FONTOS", e.toString());
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
                ;
                Log.d("SIZE TIMER CSV ZIP _ ", e.toString());
            }

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
    public static String INSERT_URL = "https://sont.sytes.net/wifi_insert.php";
    public static String myColors[] = {"#f857b5", "#f781bc", "#fdffdc", "#c5ecbe", "#00b8a9", "#f8f3d4", "#f6416c", "#ffde7d", "#7effdb", "#b693fe", "#8c82fc", "#ff9de2", "#a8e6cf", "#dcedc1", "#ffd3b6", "#ffaaa5", "#fc5185", "#384259"};
    public static Map<String, String> BLEdevices = new HashMap<String, String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        init();
        logUser();
        adminPermission();
        requestAppPermissions();

        Intent mIntent = new Intent(MainActivity.this, BackgroundService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        Collections.shuffle(Arrays.asList(myColors));
        window.setStatusBarColor(Color.parseColor(myColors[0]));
        window.setNavigationBarColor(Color.parseColor(myColors[1]));
        setTitleColor(Color.parseColor(myColors[2]));

        Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Intent mStartActivity = new Intent(context, MainActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);

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
        Log.d("ANDROID_ID_", "Android_id:_" + Global.googleAccount);
        String ipv4 = Global.getLocalIpAddress();
        Global.ipaddress = ipv4;
        TextView ip = findViewById(R.id.ip);
        ip.setText(Global.ipaddress);

        wm = (WifiManager) getSystemService(WIFI_SERVICE);
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
        txt_stat1 = findViewById(R.id.txt_stat1);
        btn_upload_http = findViewById(R.id.btn_up_http);

        csv = findViewById(R.id.val_csv);
        zip = findViewById(R.id.val_zip);

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
        //webview.getSettings().setLoadWithOverviewMode(true);
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
                startService(new Intent(MainActivity.this, BackgroundService.class));
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
        btn_upload_http.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chk_3g_wifi() == "3g") {
                    new MaterialStyledDialog.Builder(MainActivity.this)
                            .setTitle("You are on 3G/4G")
                            .setStyle(com.github.javiersantos.materialstyleddialogs.enums.Style.HEADER_WITH_TITLE)
                            .setHeaderColor(R.color.nicered1)
                            .withDarkerOverlay(true)
                            .setDescription("Data charges may apply\nDo you want to upload?")
                            .withDialogAnimation(true, Duration.FAST)
                            .setPositiveText("Upload")
                            .setNegativeText("Cancel")
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                }
                            })
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    UploadFileHTTP http = new UploadFileHTTP(MainActivity.this);
                                    http.execute("http://192.168.0.43/upload.php?");
                                    Toast.makeText(getBaseContext(), "Uploading database over HTTP", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                } else {
                    UploadFileHTTP http = new UploadFileHTTP(MainActivity.this);
                    http.execute("http://192.168.0.43/upload.php?");
                    Toast.makeText(getBaseContext(), "Uploading database over HTTP", Toast.LENGTH_SHORT).show();
                }
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
                String bl_list = "";

                for (String key : BLEdevices.keySet()) {
                    System.out.println("key : " + key);
                    System.out.println("value : " + BLEdevices.get(key));
                    if (bl_list == "") {
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

        getChartHttp("https://sont.sytes.net/wifis_chart.php");
        getChartHttp2("https://sont.sytes.net/wifis_chart_2.php");
        getStatHttp("https://sont.sytes.net/wifi_stats.php?source=" + Global.googleAccount);

        handler.postDelayed(runnable, 1000);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean result = false;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            result = true;
        switch (requestCode) {
            case 101:
                if (result) Log.d("GOOGLE_X", "Permission GET_ACCOUNTS granted");
                break;
        }
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplicationContext(), "Service is disconnected", Toast.LENGTH_SHORT).show();
            // backgroundService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundService.LocalBinder mLocalBinder = (BackgroundService.LocalBinder) service;
            backgroundService = mLocalBinder.getServerInstance();
        }
    };

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String stateSaved = savedInstanceState.getString("save_state");
        if (stateSaved == null) {
            //Toast.makeText(getBaseContext(), "onRestore: null", Toast.LENGTH_LONG).show();
        } else {
            //Toast.makeText(getBaseContext(), "Saved state onResume: " + stateSaved, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle onState) {
        super.onSaveInstanceState(onState);
        String stateSaved = onState.getString("save_state");
        if (stateSaved == null) {
            //Toast.makeText(getBaseContext(), "onSave null", Toast.LENGTH_LONG).show();
        } else {
            //Toast.makeText(getBaseContext(), "Saved state onSave: " + stateSaved, Toast.LENGTH_LONG).show();
        }
    }

    public void startUpdatesGPS() {
        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
                Log.d("Location Changes", location.toString());
                if (Global.isUploading == false) {
                    queryLocation(location);
                }
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, locationListener);
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
        contentView.setTextViewText(R.id.notif_time, "Time #" + Global.nearbyCount);
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
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult result : scanResults) {
                Global.lastSSID = result.SSID + " " + Global.convertDBM(result.level) + "%";

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

                int versionCode = BuildConfig.VERSION_CODE;
                String url = INSERT_URL;
                String reqBody = "?id=0&ssid=" + result.SSID + "&bssid=" + result.BSSID + "&source=" + Global.googleAccount + "_v" + versionCode + "&enc=" + enc + "&rssi=" + Global.convertDBM(result.level) + "&long=" + longi + "&lat=" + lati + "&channel=" + result.frequency;
                saveRecordHttp(url + reqBody);
            }
            Global.nearbyCount = String.valueOf(scanResults.size());

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
                LineChart newchart = (LineChart) findViewById(R.id.newchart);
                List<Entry> entries = new ArrayList<Entry>();

                String str = "";
                try {
                    str = new String(responseBody, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.d("Error_", e.getMessage());
                }
                String lines[] = str.trim().split("\\r?\\n");
                int i = 0;
                try {
                    for (String line : lines) {
                        String[] words = line.trim().split("\\s+");
                        entries.add(new Entry(i, Integer.valueOf(words[1])));
                        i++;
                    }
                } catch (Exception e) {
                    Log.d("Error_", e.getMessage());
                }

                LineDataSet dataSet = new LineDataSet(entries, "Stats");
                dataSet.setLineWidth(5);
                dataSet.setDrawFilled(true);
                Drawable drawable = ContextCompat.getDrawable(getBaseContext(), R.color.nicered1);
                dataSet.setFillDrawable(drawable);
                dataSet.setDrawHighlightIndicators(true);
                Collections.shuffle(Arrays.asList(myColors));
                dataSet.setHighLightColor(Color.parseColor(myColors[1]));
                dataSet.setHighlightLineWidth(4);
                dataSet.setValueTextSize(13);
                dataSet.setValueFormatter(new DefaultValueFormatter(0));
                dataSet.setHighlightEnabled(true);
                dataSet.setDrawHighlightIndicators(true);
                dataSet.setColors(Color.parseColor(myColors[0]));
                LineData lineData = new LineData(dataSet);
                newchart.setData(lineData);
                newchart.getAxisRight().setDrawGridLines(false);
                newchart.getAxisLeft().setDrawGridLines(false);
                newchart.getXAxis().setDrawGridLines(false);
                newchart.getXAxis().setDrawLabels(false);
                newchart.getDescription().setEnabled(false);
                newchart.getLegend().setEnabled(false);
                newchart.invalidate();
                newchart.animateXY(2000, 2000);
                startService(new Intent(getBaseContext(), BackgroundService.class));
            }

            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (retry_counter_1 < 20) {
                    getChartHttp("https://sont.sytes.net/wifis_chart.php");
                    retry_counter_1++;
                }
            }
        });
    }

    public void getChartHttp2(String path) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(path, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                PieChart piechart = findViewById(R.id.piechart);
                List<PieEntry> entries = new ArrayList<>();

                String str = "";
                try {
                    str = new String(responseBody, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.d("Error_", e.getMessage());
                }
                String lines[] = str.trim().split("xxx");
                try {
                    int i = 0;
                    Collections.shuffle(Arrays.asList(myColors));
                    int ossz = 0;
                    for (String line : lines) {
                        String[] words = line.trim().split("\\s+");
                        if ((words[0] == "73bedfbd149e01de") || (words[0].equals("73bedfbd149e01de"))) {
                            words[0] = "Sajat";
                        } else if ((words[0] == "4d32dfcf42ebf336") || (words[0].equals("4d32dfcf42ebf336"))) {
                            words[0] = "Anya";
                        } else if ((words[0] == "4dddd08a27a4e4cd") || (words[0].equals("4dddd08a27a4e4cd"))) {
                            words[0] = "Fater";
                        }
                        entries.add(new PieEntry(Integer.valueOf(words[1]), words[0]));
                        ossz = ossz + Integer.valueOf(words[1]);
                        i++;
                    }

                    PieDataSet set = new PieDataSet(entries, "");
                    set.setColors(new int[]{Color.parseColor(myColors[0]), Color.parseColor(myColors[1]), Color.parseColor(myColors[2]), Color.parseColor(myColors[3]), Color.parseColor(myColors[4]), Color.parseColor(myColors[5])});
                    set.setValueTextColor(Color.BLACK);
                    set.setValueTextSize(12);
                    PieData data = new PieData(set);

                    Description d = new Description();
                    d.setText("");
                    piechart.setDescription(d);
                    piechart.setCenterText("Shares" + "\n(" + ossz + ")");
                    piechart.getLegend().setEnabled(true);
                    piechart.animateXY(2000, 2000);
                    piechart.setEntryLabelColor(invertColor(Color.parseColor(myColors[0])));
                    piechart.setEntryLabelTextSize(12);

                    set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                    set.setSliceSpace(5);

                    piechart.setExtraBottomOffset(20f);
                    piechart.setExtraLeftOffset(20f);
                    piechart.setExtraRightOffset(20f);

                    set.setValueLinePart1OffsetPercentage(10.f);
                    set.setValueLinePart1Length(0.43f);
                    set.setValueLinePart2Length(.1f);
                    piechart.getLegend().setWordWrapEnabled(true);
                    Legend l = piechart.getLegend();
                    l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
                    l.setXEntrySpace(7f);
                    l.setYEntrySpace(0f);
                    l.setYOffset(0f);
                    l.setDirection(Legend.LegendDirection.LEFT_TO_RIGHT);
                    l.setWordWrapEnabled(true);

                    piechart.setData(data);
                    piechart.invalidate();
                } catch (Exception e) {
                    Log.d("Error_", e.getMessage());
                }
            }

            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (retry_counter_3 < 20) {
                    getChartHttp2("https://sont.sytes.net/wifis_chart.php");
                    retry_counter_3++;
                }
            }
        });
    }

    public void getStatHttp(String path) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(path, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String str = "";
                try {
                    str = new String(responseBody, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.d("Error_", e.getMessage());
                }
                TextView stat = findViewById(R.id.txt_stat1);
                stat.setText(str);
            }

            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (retry_counter_2 < 20) {
                    TextView stat = findViewById(R.id.txt_stat1);
                    stat.setText("HTTP Error");
                    getStatHttp("https://sont.sytes.net/wifi_stats.php?source=" + Global.googleAccount);
                    retry_counter_2++;
                }
            }
        });
    }

    private void requestAppPermissions() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        if (hasReadPermissions() && hasWritePermissions()) {
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 1); // your request code
    }

    private boolean hasReadPermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasWritePermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public void releaseQueue() {

        //ConnectionManager connectionManager = new ConnectionManager(context);
        //connectionManager.requestWIFIConnection("VENDEG","");
        //connectionManager.requestWIFIConnection("UPC Wi-Free","");

        //Toast.makeText(getBaseContext(),"Connecting to: VENDEG",Toast.LENGTH_SHORT).show();

        new UploadFileFTP(getBaseContext()).execute("192.168.0.43");
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
                    Log.d("Error_", e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.d("Error_", e.getMessage());
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

    int invertColor(int color) {
        return color ^ 0x00ffffff;
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
}