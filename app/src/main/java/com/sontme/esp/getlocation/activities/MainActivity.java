package com.sontme.esp.getlocation.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.GpsStatus;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
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
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.sontme.esp.getlocation.BackgroundService;
import com.sontme.esp.getlocation.BuildConfig;
import com.sontme.esp.getlocation.CustomFormatter;
import com.sontme.esp.getlocation.R;
import com.sontme.esp.getlocation.Servers.UDP_Client;
import com.sontme.esp.getlocation.SontHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;
import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.DeviceCallback;
import me.aflak.bluetooth.interfaces.DiscoveryCallback;

public class MainActivity extends AppCompatActivity implements GpsStatus.Listener {

    //region DEFINING VARIABLES
    public PublisherAdView mPublisherAdView;
    public Context context = this;
    public static TextView alti;
    public static TextView longi;
    public static TextView lati;
    public static TextView spd;
    public static TextView c;
    public static Switch sw;
    public static Switch sw2;
    public static Switch sw3;
    public static Switch sw4;
    public static TextView dst;
    public static FloatingActionButton exitb;
    public static TextView add;
    public static TextView provider;
    public static TextView uniq;
    public static TextView servicestatus;
    public static TextView val_succ;
    public static TextView csv;
    public static TextView zip;
    public static int retry_counter_2 = 0;

    private TextView val_errors;
    WifiManager wm;
    public DevicePolicyManager mDPM;
    public ComponentName mAdminName;

    public Handler handler = new Handler();
    public Handler chart_handler = new Handler();
    public BackgroundService backgroundService;

    public static String INSERT_URL = "https://sont.sytes.net/wifilocator/wifi_insert.php";
    public static String[] myColors = {"#f857b5", "#f781bc", "#fdffdc", "#c5ecbe", "#00b8a9", "#f6416c", "#ffde7d", "#7effdb", "#b693fe", "#8c82fc", "#ff9de2", "#a8e6cf", "#dcedc1", "#ffd3b6", "#ffaaa5", "#fc5185", "#384259"};

    //endregion

    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (sw4.isChecked() == true) {
                    longi.setText(BackgroundService.longitude);
                    lati.setText(BackgroundService.latitude);
                    alti.setText(BackgroundService.altitude);
                    spd.setText(BackgroundService.speed + " km/h");
                    if (BackgroundService.distance != null) {
                        dst.setText(SontHelper.round(Double.valueOf(BackgroundService.distance), 2) + " meters");
                    }
                    add.setText(BackgroundService.address);
                    if (BackgroundService.getCount() != 0) {
                        // c.setText(SontHelper.count);
                    }
                    provider.setText(BackgroundService.provider);
                    if (BackgroundService.uniqueAPS.size() > 0) {
                        // uniq.setText(SontHelper.uniqueAPS.size());
                    }
                    WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
                    String ipv4 = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                    BackgroundService.ipaddress = ipv4;
                    TextView ip = findViewById(R.id.ip);
                    ip.setText(BackgroundService.ipaddress);
                    servicestatus.setText("Not available");

                    File f1;
                    File f2;
                    String deviceMan = android.os.Build.MANUFACTURER;
                    if (deviceMan.equalsIgnoreCase("huawei")) {
                        f1 = new File("/data/user/0/com.sontme.esp.getlocation/files/wifilocator_database.csv");
                        f2 = new File("/data/user/0/com.sontme.esp.getlocation/files/wifilocator_database.zip");
                    } else {
                        f1 = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
                        f2 = new File("/storage/emulated/0/Documents/wifilocator_database.zip");
                    }

                    TextView csv1 = findViewById(R.id.val_csv);
                    TextView zip1 = findViewById(R.id.val_zip);
                    csv1.setText((int) (f1.length()) / 1024 + " kb");
                    zip1.setText((int) (f2.length()) + " bytes");
                    csv.setText((int) (f1.length()) / 1024 + " kb");
                    zip.setText((int) (f2.length()) + " bytes");
                    val_errors = findViewById(R.id.val_error);
                    val_errors.setText(String.valueOf(BackgroundService.urlList_failed.size()));
                    val_succ = findViewById(R.id.val_succ);
                    val_succ.setText(String.valueOf(BackgroundService.urlList_successed.size()));
                }
            } catch (Exception e) {
                Log.d("TIMER_MAIN_FONTOS", e.toString());
                e.printStackTrace();
            }
            if (BackgroundService.longitude == null) {
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
                ip.setText(BackgroundService.ipaddress);

                File f1;
                File f2;
                String deviceMan = android.os.Build.MANUFACTURER;
                if (deviceMan.equalsIgnoreCase("huawei")) {
                    f1 = new File("/data/user/0/com.sontme.esp.getlocation/files/wifilocator_database.csv");
                    f2 = new File("/data/user/0/com.sontme.esp.getlocation/files/wifilocator_database.zip");
                } else {
                    f1 = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
                    f2 = new File("/storage/emulated/0/Documents/wifilocator_database.zip");
                }
                TextView csv1 = findViewById(R.id.val_csv);
                TextView zip1 = findViewById(R.id.val_zip);
                csv1.setText((int) (f1.length()) / 1024 + " kb");
                zip1.setText((int) (f2.length()) + " bytes");
                csv.setText((int) (f1.length()) / 1024 + " kb");
                zip.setText((int) (f2.length()) + " bytes");
                val_errors = findViewById(R.id.val_error);
                val_errors.setText(String.valueOf(BackgroundService.urlList_failed.size()));
                val_succ = findViewById(R.id.val_succ);
                val_succ.setText(String.valueOf(BackgroundService.urlList_successed.size()));
            }
            handler.postDelayed(this, 1000);
        }
    };
    public Runnable chart_runnable = new Runnable() {
        @Override
        public void run() {
            try {
                //Log.d("TIMER_CHART_", "LEFUTOTT");
                if (sw.isChecked() == true) {
                    getChart_timer_updated("https://sont.sytes.net/wifilocator/wifis_chart_updated.php");
                }
                if (sw2.isChecked() == true) {
                    getChart_timer_new("https://sont.sytes.net/wifilocator/wifis_chart_new.php");
                }
                if (sw3.isChecked() == true) {
                    getChart_timer_pie("https://sont.sytes.net/wifilocator/wifis_chart_2.php");
                }
                if (sw4.isChecked() == true) {
                    getStatHttp("https://sont.sytes.net/wifilocator/wifi_stats.php?source=" + BackgroundService.googleAccount);
                }
            } catch (Exception e) {
                Log.d("TIMER_CHART_", e.toString());
            }
            chart_handler.postDelayed(this, 5000);
        }
    };

    public DrawerLayout dl;
    public ActionBarDrawerToggle t;
    public NavigationView nv;

    public boolean isBLDevicePaired(BluetoothDevice device) {
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> list = ba.getBondedDevices();

        Log.d("BLUETOOTH_LIBRARY_", "Paired count: " + list.size());
        for (BluetoothDevice dev : list) {
            return device.getAddress() == dev.getAddress();
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        wm = (WifiManager) getSystemService(WIFI_SERVICE);

        UDP_Client udp = new UDP_Client("sont.sytes.net", 5000, getApplicationContext());
        udp.execute("STARTED ACT");


        Button sharebutton = findViewById(R.id.sharebutton);
        Button testbutton = findViewById(R.id.testbutton);
        testbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BluetoothDevice[] connectTo = new BluetoothDevice[1];
                SontHelper.playTone();
                SontHelper.vibrate(getApplicationContext());

                SontHelper.check_if_local(getApplicationContext());

                Bluetooth bluetooth = new Bluetooth(getApplicationContext());

                DeviceCallback deviceCallback = new DeviceCallback() {
                    @Override
                    public void onDeviceConnected(BluetoothDevice device) {
                        Log.d("BLUETOOTH_LIBRARY_", "Connected to: " + device.getAddress());
                    }

                    @Override
                    public void onDeviceDisconnected(BluetoothDevice device, String message) {
                        Log.d("BLUETOOTH_LIBRARY_", "Disconnected from: " + device.getAddress());
                    }

                    @Override
                    public void onMessage(String message) {
                        Log.d("BLUETOOTH_LIBRARY_", "message_" + message);
                    }

                    @Override
                    public void onError(int errorCode) {
                        Log.d("BLUETOOTH_LIBRARY_", "error_" + errorCode);
                    }

                    @Override
                    public void onConnectError(BluetoothDevice device, String message) {
                        Log.d("BLUETOOTH_LIBRARY_", "ConnectionError_ " + message);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "CONNECTION ERROR:\n" +
                                                device.getName() + "\n" +
                                                device.getAddress() + "\n" +
                                                message,
                                        Toast.LENGTH_SHORT
                                ).show();
                                SontHelper.double_vibrate(getApplicationContext());
                                SontHelper.vibrate(getApplicationContext());
                            }
                        });
                    }
                };
                DiscoveryCallback discoveryCallback = new DiscoveryCallback() {
                    @Override
                    public void onDiscoveryStarted() {
                        Log.d("BLUETOOTH_LIBRARY_", "Discovery Started ! " +
                                System.currentTimeMillis());
                    }

                    @Override
                    public void onDiscoveryFinished() {
                        Log.d("BLUETOOTH_LIBRARY_", "Discovery Finished ! " +
                                System.currentTimeMillis());
                        try {
                            bluetooth.onStop();
                            bluetooth.onStart();
                            if (!bluetooth.isEnabled())
                                bluetooth.enable();
                            bluetooth.startScanning();

                            //bluetooth.pair(connectTo[0]);
                            if (bluetooth != null)
                                Log.d("BLUETOOTH_LIBRARY_", "Bluetooth NOT null");
                            if (connectTo[0] != null) {
                                Log.d("BLUETOOTH_LIBRARY_", "Device NOT null");
                                bluetooth.connectToDevice(connectTo[0]);
                                Log.d("BLUETOOTH_LIBRARY_", "Connecting ! " +
                                        connectTo[0].getName() + " " +
                                        connectTo[0].getAddress());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d("BLUETOOTH_LIBRARY_", e.getMessage());
                        }
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                SontHelper.double_vibrate(getApplicationContext());
                            }
                        });
                    }

                    @Override
                    public void onDeviceFound(BluetoothDevice device) {
                        if (device.getAddress().equals("00:19:86:00:10:AE")) {
                            if (isBLDevicePaired(device) == true) {
                                Log.d("BLUETOOTH_LIBRARY_", "Found: " +
                                        device.getAddress() + " _ PAIRED");
                            } else {
                                Log.d("BLUETOOTH_LIBRARY_", "Found: " +
                                        device.getAddress() + " _ NOT PAIRED");
                            }
                        } else {
                            UDP_Client udp = new UDP_Client("sont.sytes.net", 5000, getApplicationContext());
                            udp.execute(device.getName() + "_" + device.getAddress() + "_" + BackgroundService.latitude + "_" + BackgroundService.longitude);
                        }
                    }

                    @Override
                    public void onDevicePaired(BluetoothDevice device) {
                        Log.d("BLUETOOTH_LIBRARY_", "pairing_" +
                                device.getName() + " _ " +
                                device.getAddress());
                    }

                    @Override
                    public void onDeviceUnpaired(BluetoothDevice device) {
                        Log.d("BLUETOOTH_LIBRARY_", "un_pairing_" + device.getName());
                    }

                    @Override
                    public void onError(int errorCode) {
                        Log.d("BLUETOOTH_LIBRARY_", "Error_ " + errorCode);
                    }
                };

                //bluetooth.setBluetoothCallback(bluetoothCallback);
                bluetooth.setDiscoveryCallback(discoveryCallback);
                bluetooth.setDeviceCallback(deviceCallback);

                bluetooth.onStart();
                if (!bluetooth.isEnabled())
                    bluetooth.enable();
                bluetooth.startScanning();
            }
        });
        sharebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PackageManager m = getPackageManager();
                    String app = getPackageName();
                    PackageInfo p = m.getPackageInfo(app, 0);

                    File f = new File(p.applicationInfo.sourceDir);
                    File f2 = new File(Environment.getExternalStorageDirectory(), "wifilogger_share.apk");
                    copyFile(f, f2);

                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());

                    Intent sharingIntent = new Intent();
                    Uri uri = Uri.fromFile(f);
                    sharingIntent.setType("*/*");
                    //sharingIntent.setType("application/vnd.android.package-archive");
                    sharingIntent.setAction(Intent.ACTION_SEND);
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(sharingIntent, "Share app"));
                } catch (Exception e) {
                    Log.d("APPINFO", "hiba: " + e.getMessage());
                }
            }
        });

        LinearLayout lin2 = findViewById(R.id.firstlin2);
        LinearLayout lin3 = findViewById(R.id.firstlin3);
        LinearLayout lin4 = findViewById(R.id.firstlin4);
        LinearLayout lin5 = findViewById(R.id.firstlin5);

        lin2.setVisibility(LinearLayout.GONE);
        lin3.setVisibility(LinearLayout.GONE);
        lin4.setVisibility(LinearLayout.GONE);
        lin5.setVisibility(LinearLayout.GONE);

        wifi_check();
        adminPermission_check();
        SontHelper.requestPermissions(this);
        turnGPSOn();

        // NOTIFICATION BAR COLOR RANDOMIZATOR
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        Collections.shuffle(Arrays.asList(myColors));
        window.setStatusBarColor(Color.parseColor(myColors[1]));
        window.setNavigationBarColor(Color.parseColor(myColors[1]));
        setTitleColor(Color.parseColor(myColors[1]));
        Log.d("APP_COLOR", String.valueOf(myColors[1]));

        Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                UDP_Client udp = new UDP_Client("sont.sytes.net", 5000, getApplicationContext());
                udp.execute("ERROR_" + ex.getMessage() + "\n" + ex.getStackTrace());

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
            BackgroundService.googleAccount = acc;
        } else {
            BackgroundService.googleAccount = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        }

        String ipv4 = SontHelper.getLocalIpAddress();
        BackgroundService.ipaddress = ipv4;
        TextView ip = findViewById(R.id.ip);
        ip.setText(BackgroundService.ipaddress);

        //region DRAWER
        dl = findViewById(R.id.drawler);
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
                Toast.makeText(getApplicationContext(), "AD loaded", Toast.LENGTH_SHORT).show();
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
        alti = findViewById(R.id.alti);
        add = findViewById(R.id.add);
        c = findViewById(R.id.c);
        longi = findViewById(R.id.longi);
        lati = findViewById(R.id.lati);
        spd = findViewById(R.id.spd);
        dst = findViewById(R.id.dst);
        sw = findViewById(R.id.switch1);
        sw2 = findViewById(R.id.switch2);
        sw3 = findViewById(R.id.switch3);
        sw4 = findViewById(R.id.switch4);
        exitb = findViewById(R.id.exitb);
        provider = findViewById(R.id.prov);
        uniq = findViewById(R.id.uniq);
        WebView webview = findViewById(R.id.webview);
        servicestatus = findViewById(R.id.servstatus);
        csv = findViewById(R.id.val_csv);
        zip = findViewById(R.id.val_zip);
        exitb.setBackgroundColor(Color.TRANSPARENT);

        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    LinearLayout lin2 = findViewById(R.id.firstlin2);
                    lin2.setVisibility(LinearLayout.VISIBLE);
                } else {
                    LinearLayout lin2 = findViewById(R.id.firstlin2);
                    lin2.setVisibility(LinearLayout.GONE);
                }
            }
        });
        sw2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    LinearLayout lin3 = findViewById(R.id.firstlin3);
                    lin3.setVisibility(LinearLayout.VISIBLE);
                } else {
                    LinearLayout lin3 = findViewById(R.id.firstlin3);
                    lin3.setVisibility(LinearLayout.GONE);
                }
            }
        });
        sw3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    LinearLayout lin4 = findViewById(R.id.firstlin4);
                    lin4.setVisibility(LinearLayout.VISIBLE);
                } else {
                    LinearLayout lin4 = findViewById(R.id.firstlin4);
                    lin4.setVisibility(LinearLayout.GONE);
                }
            }
        });
        sw4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    LinearLayout lin5 = findViewById(R.id.firstlin5);
                    lin5.setVisibility(LinearLayout.VISIBLE);
                } else {
                    LinearLayout lin5 = findViewById(R.id.firstlin5);
                    lin5.setVisibility(LinearLayout.GONE);

                    NotificationManager notificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(1);

                    // GET ALL NOTIFICATION
                    /*
                    StatusBarNotification[] asd = notificationManager.getActiveNotifications();
                    for(StatusBarNotification notif : asd){
                        Log.d("NOTIF_",notif.getPackageName() + "_" + notif.getId() + "_" + notif.isOngoing());
                    }
                    */
                    //notificationManager.cancelAll();
                }
            }
        });

        //endregion

        exitb.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                finish();
                finishAffinity();
                System.exit(0);
            }
        });

        // DRAWER VERSION INFO
        View hView = nv.getHeaderView(0);
        TextView tex = hView.findViewById(R.id.header_verinfo);
        String version = "Version: " + BuildConfig.VERSION_NAME + " Build: " + BuildConfig.VERSION_CODE;
        tex.setText(version);

        handler.postDelayed(runnable, 1000);
        chart_handler.postDelayed(chart_runnable, 5000);

        Thread th3 = new Thread() {
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(MainActivity.this, BackgroundService.class));
                    bindService(new Intent(MainActivity.this, BackgroundService.class), mConnection, BIND_AUTO_CREATE);
                } else {
                    startService(new Intent(MainActivity.this, BackgroundService.class));
                    bindService(new Intent(MainActivity.this, BackgroundService.class), mConnection, BIND_AUTO_CREATE);
                }
            }
        };
        th3.start();
    }

    @Override
    public void onStop() {
        UDP_Client udp = new UDP_Client("sont.sytes.net", 5000, getApplicationContext());
        udp.execute("STOPPED ACT");
        super.onStop();
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplicationContext(), "Service is disconnected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundService.LocalBinder mLocalBinder = (BackgroundService.LocalBinder) service;
            backgroundService = mLocalBinder.getServerInstance();
        }
    };

    @Override
    public void onResume() {
        UDP_Client udp = new UDP_Client("sont.sytes.net", 5000, getApplicationContext());
        udp.execute("RESUMING ACT");
        super.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        UDP_Client udp = new UDP_Client("sont.sytes.net", 5000, getApplicationContext());
        udp.execute("PAUSING ACT");
        SharedPreferences prefs = getSharedPreferences("X", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastActivity", getClass().getName());
        editor.commit();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (t.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    public void wifi_check() {
        // TURN ON WIFI
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning on WiFi", Toast.LENGTH_SHORT).show();
            wifi.setWifiEnabled(true);
        }
    }

    public void getChart_timer_updated(String path) {
        AndroidNetworking.get(path)
                .setTag("chart_auto")
                .setPriority(Priority.LOW)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        LineChart newchart = findViewById(R.id.newchart);
                        List<Entry> entries = new ArrayList<Entry>();

                        Map<Integer, Integer> hours = new HashMap<Integer, Integer>();
                        for (int i = 0; i < 24; i++) {
                            hours.put(i, 0);
                        }
                        Map<Integer, Integer> values = new HashMap<Integer, Integer>();
                        Map<Integer, Integer> combined = new HashMap<Integer, Integer>(hours);

                        String str = response;
                        String[] lines = str.trim().split("\\r?\\n");
                        try {
                            for (String line : lines) {
                                String[] words = line.trim().split("\\s+");
                                values.put(Integer.valueOf(words[0]), Integer.valueOf(words[1]));
                            }
                            combined.putAll(values);
                            for (Map.Entry<Integer, Integer> entry : combined.entrySet()) {
                                int key = entry.getKey();
                                int value = entry.getValue();
                                entries.add(new Entry(key, value));
                            }
                        } catch (Exception e) {
                            Log.d("Error_", e.getMessage());
                            e.printStackTrace();
                        }

                        LineDataSet dataSet = new LineDataSet(entries, "Stats");
                        dataSet.setLineWidth(4f);
                        dataSet.setDrawFilled(true);
                        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.color.nicered1);
                        dataSet.setFillDrawable(drawable);
                        dataSet.setDrawHighlightIndicators(true);
                        Collections.shuffle(Arrays.asList(myColors));
                        dataSet.setHighLightColor(Color.parseColor(myColors[1]));
                        dataSet.setHighlightLineWidth(3f);
                        dataSet.setDrawValues(true);
                        dataSet.setValueTextSize(13);
                        dataSet.setValueFormatter(new DefaultValueFormatter(0));
                        dataSet.setHighlightEnabled(false);
                        dataSet.setDrawHighlightIndicators(false);
                        dataSet.setColors(Color.parseColor(myColors[0]));
                        dataSet.setLineWidth(1);
                        dataSet.setValueFormatter(new CustomFormatter());
                        LineData lineData = new LineData(dataSet);
                        newchart.setData(lineData);
                        newchart.setDrawBorders(false);
                        newchart.getAxisRight().setDrawGridLines(false);
                        newchart.getAxisLeft().setDrawGridLines(false);

                        newchart.getXAxis().setDrawGridLines(false);
                        newchart.getXAxis().setDrawLabels(true);

                        newchart.getDescription().setEnabled(false);
                        newchart.getLegend().setEnabled(false);
                        newchart.setScaleEnabled(false);
                        newchart.setPinchZoom(false);
                        newchart.invalidate();
                        newchart.animateX(500);
                    }

                    @Override
                    public void onError(ANError anError) {
                    }
                });
    }
    public void getChart_timer_new(String path) {
        AndroidNetworking.get(path)
                .setTag("chart_auto")
                .setPriority(Priority.LOW)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        LineChart newchart = findViewById(R.id.newchart2);
                        List<Entry> entries = new ArrayList<Entry>();

                        Map<Integer, Integer> hours = new HashMap<Integer, Integer>();
                        for (int i = 0; i <= 24; i++) {
                            hours.put(i, 0);
                        }
                        Map<Integer, Integer> values = new HashMap<Integer, Integer>();
                        Map<Integer, Integer> combined = new HashMap<Integer, Integer>(hours);

                        String str = response;

                        String[] lines = str.trim().split("\\r?\\n");
                        try {
                            for (String line : lines) {
                                String[] words = line.trim().split("\\s+");
                                values.put(Integer.valueOf(words[0]), Integer.valueOf(words[1]));
                            }
                            combined.putAll(values);
                            for (Map.Entry<Integer, Integer> entry : combined.entrySet()) {
                                int key = entry.getKey();
                                int value = entry.getValue();
                                entries.add(new Entry(key, value));
                            }
                        } catch (Exception e) {
                        }

                        LineDataSet dataSet = new LineDataSet(entries, "Stats");
                        dataSet.setLineWidth(4f);
                        dataSet.setDrawFilled(true);
                        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.color.nicered1);
                        dataSet.setFillDrawable(drawable);
                        dataSet.setDrawHighlightIndicators(true);
                        Collections.shuffle(Arrays.asList(myColors));
                        dataSet.setHighLightColor(Color.parseColor(myColors[1]));
                        dataSet.setHighlightLineWidth(3f);
                        dataSet.setDrawValues(true);
                        dataSet.setLineWidth(1);
                        dataSet.setValueTextSize(13);
                        dataSet.setValueFormatter(new DefaultValueFormatter(0));
                        dataSet.setHighlightEnabled(false);
                        dataSet.setDrawHighlightIndicators(false);
                        dataSet.setColors(Color.parseColor(myColors[0]));
                        dataSet.setValueFormatter(new CustomFormatter());
                        LineData lineData = new LineData(dataSet);
                        newchart.setData(lineData);
                        newchart.setDrawBorders(false);
                        newchart.getAxisRight().setDrawGridLines(false);
                        newchart.getAxisLeft().setDrawGridLines(false);

                        newchart.getXAxis().setDrawGridLines(false);
                        newchart.getXAxis().setDrawLabels(true);

                        newchart.getDescription().setEnabled(false);
                        newchart.getLegend().setEnabled(false);
                        newchart.setScaleEnabled(false);
                        newchart.setPinchZoom(false);
                        newchart.invalidate();
                        newchart.animateX(500);
                    }

                    @Override
                    public void onError(ANError anError) {
                    }
                });
    }
    public void getChart_timer_pie(String path) {
        AndroidNetworking.get(path)
                .setTag("chart_auto2")
                .setPriority(Priority.LOW)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        PieChart piechart = findViewById(R.id.piechart);
                        List<PieEntry> entries = new ArrayList<>();

                        String str = response;
                        String[] lines = str.trim().split("xxx");

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
                            try {
                                entries.add(new PieEntry(Integer.valueOf(words[1]), words[0]));
                                ossz = ossz + Integer.valueOf(words[1]);
                                i++;
                            } catch (Exception e) {
                            }
                        }

                        PieDataSet set = new PieDataSet(entries, "");
                        set.setColors(Color.parseColor(myColors[0]), Color.parseColor(myColors[1]), Color.parseColor(myColors[2]), Color.parseColor(myColors[3]), Color.parseColor(myColors[4]), Color.parseColor(myColors[5]));
                        set.setValueTextColor(Color.BLACK);
                        set.setValueTextSize(12);
                        PieData data = new PieData(set);

                        Description d = new Description();
                        d.setText("");
                        piechart.setDescription(d);
                        piechart.setCenterText("Shares" + "\n(" + ossz + ")");
                        piechart.getLegend().setEnabled(true);
                        //piechart.animateXY(2000, 2000);
                        piechart.setEntryLabelColor(SontHelper.invertColor(Color.parseColor(myColors[0])));
                        piechart.setEntryLabelTextSize(12);

                        set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                        set.setSliceSpace(5);

                        piechart.setExtraBottomOffset(20f);
                        piechart.setExtraLeftOffset(20f);
                        piechart.setExtraRightOffset(20f);
                        piechart.animateY(500);
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
                    }

                    @Override
                    public void onError(ANError anError) {
                    }
                });
    }
    public void getStatHttp(String path) {
        path = path.replaceAll(Pattern.quote("+"), "");
        path = path.replaceAll(Pattern.quote(" "), "%20");
        path = path.replaceAll(Pattern.quote("%20"), "");
        Log.d("STAT_", path);
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(path, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    String str = "";
                    str = new String(responseBody, StandardCharsets.UTF_8);
                    Log.d("STAT_", str);
                    String[] vagott = str.split(Pattern.quote("/"));
                    // 0 - new me
                    // 1 - new all
                    // 2 - up me
                    // 3 - up all
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        String NOTIFICATION_CHANNEL_ID = "new";
                        String channelName = "new";
                        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_MIN);
                        chan.setLightColor(Color.BLUE);
                        chan.setShowBadge(true);
                        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        assert manager != null;
                        manager.createNotificationChannel(chan);

                        Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
                        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, intent, 0);

                        for (int i = 0; i < vagott.length; i++) {
                            vagott[i] = vagott[i].replace("\n", "").replace("\r", "");
                        }

                        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notif_lay_up);
                        contentView.setTextViewText(R.id.texttxt, "Up: " + vagott[2] + "/" + vagott[3] + " | New: " + vagott[0] + "/" + vagott[1] + " | Travelled: " + BackgroundService.sumOfTravelDistance + "m");

                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID);
                        Notification notification = notificationBuilder
                                .setOngoing(true)
                                .setSmallIcon(R.drawable.gps2)
                                .setGroup("wifi")
                                //.setContentTitle("Statistics")
                                .setContentTitle("Up: " + vagott[2] + "/" + vagott[3] + " | New: " + vagott[0] + "/" + vagott[1] + " | " + BackgroundService.sumOfTravelDistance + "m")
                                .setContent(contentView)
                                .setSubText("Searching")
                                .setContentIntent(pi)
                                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                                .setCategory(Notification.CATEGORY_SERVICE)
                                .setNumber(1)
                                .build();
                        //startForeground(3, notification);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            manager.notify(1, notification);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (retry_counter_2 < 20) {
                    getStatHttp("https://sont.sytes.net/wifilocator/wifi_stats.php?source=" + BackgroundService.googleAccount);
                    retry_counter_2++;
                }
            }
        });
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

    public void adminPermission_check() {
        try {
            if (!mDPM.isAdminActive(mAdminName)) {
                try {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "extrainfo");
                    startActivityForResult(intent, 0);
                } catch (Exception e) {
                    Log.d("Error_setting_admin_permission_", e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.d("Error_", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void copyFile(File src, File dst) {
        Thread thread = new Thread() {
            public void run() {
                try (InputStream in = new FileInputStream(src)) {
                    try (OutputStream out = new FileOutputStream(dst)) {
                        // Transfer bytes from in to out
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.d("APPINFO", "Copy Fail");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("APPINFO", "Copy Fail");
                }
                Log.d("APPINFO", "Copy Done");
            }
        };
        thread.start();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        //mStatus = mService.getGpsStatus(mStatus);
        if (event != GpsStatus.GPS_EVENT_FIRST_FIX &&
                event != GpsStatus.GPS_EVENT_SATELLITE_STATUS &&
                event != GpsStatus.GPS_EVENT_STARTED &&
                event != GpsStatus.GPS_EVENT_STOPPED) {
            //Toast.makeText(getApplicationContext(), "GPS Unknown event: " + event, Toast.LENGTH_SHORT).show();
        }
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                //Toast.makeText(getApplicationContext(), "GPS Event Started", Toast.LENGTH_SHORT).show();
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                //Toast.makeText(getApplicationContext(), "GPS Event Stopped", Toast.LENGTH_SHORT).show();
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                //Toast.makeText(getApplicationContext(), "GPS Event First FIX", Toast.LENGTH_SHORT).show();
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                //Toast.makeText(getApplicationContext(), "GPS SAT Status", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}

