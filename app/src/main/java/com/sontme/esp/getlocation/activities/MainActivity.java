package com.sontme.esp.getlocation.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.javiersantos.materialstyleddialogs.enums.Duration;
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
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
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
import com.sontme.esp.getlocation.R;
import com.sontme.esp.getlocation.SontHelper;
import com.sontme.esp.getlocation.UploadFileFTP;
import com.sontme.esp.getlocation.UploadFileHTTP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

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
    public static Switch sw;
    public static Switch sw2;
    public static Switch sw3;
    public static Switch sw4;

    public static TextView dst;
    public static Button start_srv;
    public static Button stop_srv;
    public static FloatingActionButton exitb;
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

    public static int csvSize;
    public static int zipSize;
    //endregion

    private TextView val_errors;
    private Button btn_upload_http;
    WifiManager wm;
    public DevicePolicyManager mDPM;
    public ComponentName mAdminName;

    public Location mlocation;
    public LocationRequest mPlayLocationRequest;
    public Handler handler = new Handler();
    public Handler chart_handler = new Handler();
    public BackgroundService backgroundService;

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

    public void queryLocation(Location LocRes) {

        if (String.valueOf(LocRes.getLongitude()) != null || String.valueOf(LocRes.getLongitude()).length() >= 1) {
            try {
                String ipv4 = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                BackgroundService.accuracy = String.valueOf(LocRes.getAccuracy());
                BackgroundService.latitude = String.valueOf(LocRes.getLatitude());
                BackgroundService.longitude = String.valueOf(LocRes.getLongitude());
                BackgroundService.speed = String.valueOf(SontHelper.round(SontHelper.mpsTokmh(LocRes.getSpeed()), 2));
                BackgroundService.altitude = String.valueOf(LocRes.getAltitude());
                BackgroundService.bearing = String.valueOf(LocRes.getBearing());
                BackgroundService.time = String.valueOf(SontHelper.convertTime(LocRes.getTime()));
                BackgroundService.address = SontHelper.getCompleteAddressString(getApplicationContext(), LocRes.getLatitude(), LocRes.getLongitude());
                BackgroundService.provider = LocRes.getProvider();
                BackgroundService.distance = String.valueOf(SontHelper.getDistance(Double.valueOf(BackgroundService.latitude), Double.valueOf(BackgroundService.initLat), Double.valueOf(BackgroundService.longitude), Double.valueOf(BackgroundService.initLong)));
                BackgroundService.ipaddress = ipv4;

            } catch (Exception e) {
                Log.d("SIZE TIMER CSV ZIP _ ", e.toString());
            }
        }
        try {
            if (Double.valueOf(BackgroundService.latitude) != 0 && Double.valueOf(BackgroundService.longitude) != 0) {
                counter++;
                BackgroundService.count++;
                if (counter == 1) {
                    // START POSITION (Activity/Program start)
                    BackgroundService.initLat = BackgroundService.latitude;
                    BackgroundService.initLong = BackgroundService.longitude;
                }
            }
            Log.d("INITIAL", BackgroundService.initLat + BackgroundService.initLong);
        } catch (Exception e) {
        }

        if (BackgroundService.latitude != null) {
            aplist(getApplicationContext(), Double.valueOf(BackgroundService.latitude), Double.valueOf(BackgroundService.longitude));
        }
    }

    public DrawerLayout dl;
    public ActionBarDrawerToggle t;
    public NavigationView nv;

    //String INSERT_URL = "https://sont.sytes.net/mcuinsert2.php";
    public static String INSERT_URL = "https://sont.sytes.net/wifilocator/wifi_insert.php";
    public static String[] myColors = {"#f857b5", "#f781bc", "#fdffdc", "#c5ecbe", "#00b8a9", "#f8f3d4", "#f6416c", "#ffde7d", "#7effdb", "#b693fe", "#8c82fc", "#ff9de2", "#a8e6cf", "#dcedc1", "#ffd3b6", "#ffaaa5", "#fc5185", "#384259"};
    public static Map<String, String> BLEdevices = new HashMap<String, String>();

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

    static String CAMERA_IMAGE_BUCKET_NAME =
            Environment.getExternalStorageDirectory().toString()
                    + "/DCIM/Camera";
    static String CAMERA_IMAGE_BUCKET_ID =
            getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    public static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase().hashCode());
    }

    public static List<String> getCameraImages(Context context) {
        final String[] projection = {MediaStore.Images.Media.DATA};
        final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        final String[] selectionArgs = {CAMERA_IMAGE_BUCKET_ID};
        final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);
        ArrayList<String> result = new ArrayList<String>(cursor.getCount());
        if (cursor.moveToFirst()) {
            final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            do {
                final String data = cursor.getString(dataColumn);
                result.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button sharebutton = findViewById(R.id.sharebutton);
        Button testbutton = findViewById(R.id.testbutton);
        testbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

                List<String> list = new ArrayList<String>();
                list = getCameraImages(getApplicationContext());
                long allsize = 0;
                for (String s : list) {
                    File f = new File(s);
                    allsize = allsize + f.length();
                }
                Log.d("LIST_", "allsize: " + (int) (allsize) / 1024 / 1024 + " mb" + " _ " + "(" + list.size() + ")");
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

        init();
        logUser();
        adminPermission();
        requestAppPermissions();


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
            BackgroundService.googleAccount = acc;
        } else {
            BackgroundService.googleAccount = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        }
        Log.d("ANDROID_ID_", "Android_id:_" + BackgroundService.googleAccount);
        String ipv4 = SontHelper.getLocalIpAddress();
        BackgroundService.ipaddress = ipv4;
        TextView ip = findViewById(R.id.ip);
        ip.setText(BackgroundService.ipaddress);

        wm = (WifiManager) getSystemService(WIFI_SERVICE);
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
        start_srv = findViewById(R.id.srv);
        stop_srv = findViewById(R.id.stop_srv);
        exitb = findViewById(R.id.exitb);
        provider = findViewById(R.id.prov);
        uniq = findViewById(R.id.uniq);
        WebView webview = findViewById(R.id.webview);
        startServiceBtn = findViewById(R.id.startService);
        stopServiceBtn = findViewById(R.id.stopService);
        servicestatus = findViewById(R.id.servstatus);
        release_btn = findViewById(R.id.release_btn);
        Button blebtn1 = findViewById(R.id.blebtn1);
        Button blebtn2 = findViewById(R.id.blebtn2);
        // txt_stat1 = findViewById(R.id.txt_stat1);
        btn_upload_http = findViewById(R.id.btn_up_http);

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
        //webview.getSettings().setUseWideViewPort(false);
        //webview.setInitialScale(1);
        webview.setBackgroundColor(Color.argb(100, 234, 234, 234));
        //webview.loadUrl("https://sont.sytes.net/wifilocator/osm.php");
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
                //startUpdatesPlay();
            }

        });
        stop_srv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startUpdatesGPS();
            }
        });
        startServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(), "Starting service", Toast.LENGTH_SHORT);
                startService(new Intent(MainActivity.this, BackgroundService.class));
            }
        });
        stopServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(), "Stopping service", Toast.LENGTH_SHORT);
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
                if (SontHelper.chk_3g_wifi(getApplicationContext()) == "3g") {
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
                                    http.execute("http://192.168.0.43/wifilocator/upload.php?");
                                    Toast.makeText(getApplicationContext(), "Uploading database over HTTP", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                } else {
                    UploadFileHTTP http = new UploadFileHTTP(MainActivity.this);
                    http.execute("http://192.168.0.43/wifilocator/upload.php?");
                    Toast.makeText(getApplicationContext(), "Uploading database over HTTP", Toast.LENGTH_SHORT).show();
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
        TextView tex = hView.findViewById(R.id.header_verinfo);
        String version = "Version: " + BuildConfig.VERSION_NAME + " Build: " + BuildConfig.VERSION_CODE;
        tex.setText(version);

        turnGPSOn();


        Thread th2 = new Thread() {
            public void run() {
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
            }
        };
        th2.start();

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
        super.onStop();
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //Toast.makeText(getApplicationContext(), "Service is disconnected", Toast.LENGTH_SHORT).show();
            // SontHelper = null;
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
            //Toast.makeText(getApplicationContext(), "onRestore: null", Toast.LENGTH_LONG).show();
        } else {
            //Toast.makeText(getApplicationContext(), "Saved state onResume: " + stateSaved, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle onState) {
        super.onSaveInstanceState(onState);
        String stateSaved = onState.getString("save_state");
        if (stateSaved == null) {
            //Toast.makeText(getApplicationContext(), "onSave null", Toast.LENGTH_LONG).show();
        } else {
            //Toast.makeText(getApplicationContext(), "Saved state onSave: " + stateSaved, Toast.LENGTH_LONG).show();
        }
    }

    public void startUpdatesGPS() {
        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
                Log.d("Location Changes", location.toString());
                if (BackgroundService.isUploading == false) {
                    queryLocation(location);
                }
                BackgroundService.provider = location.getProvider();
                provider.setText(BackgroundService.provider);
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
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, locationListener);
    }
    public void startUpdatesPlay() {
        mPlayLocationRequest = new LocationRequest();
        mPlayLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mPlayLocationRequest.setInterval(1000);
        mPlayLocationRequest.setSmallestDisplacement(1);
        mPlayLocationRequest.setFastestInterval(1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mPlayLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(getApplicationContext());
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    public void aplist(final Context context, double lati, double longi) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult result : scanResults) {
                BackgroundService.lastSSID = result.SSID + " " + SontHelper.convertDBM(result.level) + "%";

                if (!BackgroundService.uniqueAPS.contains(result.BSSID)) {
                    BackgroundService.uniqueAPS.add(result.BSSID);
                }
                String enc = "notavailable";
                if (!result.capabilities.contains("WEP") || !result.capabilities.contains("WPA")) {
                    enc = "NONE";
                } else if (result.capabilities.contains("WEP")) {
                    enc = "WEP";
                } else if (result.capabilities.contains("WPA")) {
                    enc = "WPA";
                }
                Log.d("WIFI_DEBUG_", result.SSID + " _ " + result.capabilities);
                int versionCode = BuildConfig.VERSION_CODE;
                String url = INSERT_URL;
                String reqBody = "?id=0&ssid=" + result.SSID + "&bssid=" + result.BSSID + "&source=" + BackgroundService.googleAccount + "_v" + versionCode + "&enc=" + enc + "&rssi=" + SontHelper.convertDBM(result.level) + "&long=" + longi + "&lat=" + lati + "&channel=" + result.frequency;
                saveRecordHttp(url + reqBody);
            }
            BackgroundService.nearbyCount = String.valueOf(scanResults.size());

        } catch (Exception e) {
            Log.d("APP", "ERROR " + e.getMessage());
        }
    }

    public void logUser() {
        //Crashlytics.setUserIdentifier("12345");
        //Crashlytics.setUserEmail("sont16@gmail.com");
        //Crashlytics.setUserName("wifilocatoruser");
    }

    public void init() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // LOCATION PERMISSION CHECK IF NOT ASK FOR IT
        if (BackgroundService.checkPermissionLocation(getApplicationContext()) == false) {
            Toast.makeText(getApplicationContext(), "Missing location permission", Toast.LENGTH_SHORT).show();
            requestPermissionLocation();
        }
        // TURN ON WIFI
        if (!wifi.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning on WiFi", Toast.LENGTH_SHORT).show();
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
                    //TextView stat = findViewById(R.id.txt_stat1);
                    //stat.setText("HTTP Error");
                    getStatHttp("https://sont.sytes.net/wifilocator/wifi_stats.php?source=" + BackgroundService.googleAccount);
                    retry_counter_2++;
                }
            }
        });
    }

    private void requestAppPermissions() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }


        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        }
        if (hasReadPermissions() && hasWritePermissions()) {
        }
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, 1); // your request code
    }

    private boolean hasReadPermissions() {
        return (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasWritePermissions() {
        return (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public void releaseQueue() {

        //ConnectionManager connectionManager = new ConnectionManager(context);
        //connectionManager.requestWIFIConnection("VENDEG","");
        //connectionManager.requestWIFIConnection("UPC Wi-Free","");

        //Toast.makeText(getApplicationContext(),"Connecting to: VENDEG",Toast.LENGTH_SHORT).show();

        new UploadFileFTP(getApplicationContext()).execute("192.168.0.43");
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

class CustomFormatter implements IValueFormatter {

    private DecimalFormat mFormat;

    public CustomFormatter() {
        mFormat = new DecimalFormat("###,###,##0");
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {

        if (value > 0) {
            return mFormat.format(value);
        } else {
            return "";
        }
    }
}