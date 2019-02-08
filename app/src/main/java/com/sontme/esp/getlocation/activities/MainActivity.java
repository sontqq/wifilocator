package com.sontme.esp.getlocation.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
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
import es.dmoral.toasty.Toasty;
import io.fabric.sdk.android.Fabric;

import android.support.design.widget.NavigationView;

//import static com.sontme.esp.getlocation.BackgroundService.timer;

public class MainActivity extends AppCompatActivity {
    //region DEFINING VARIABLES
    public static final int REQUEST_PERMISSION_LOCATION = 255;
    public int counter = 0;
    public double initialLong;
    public double initialLat;
    public int serviceRefreshInterval = 1000;
    public boolean runUI = false;
    public PublisherAdView mPublisherAdView;

    public Context context = this;
    static Runnable runnable = null;
    public static int zoomval = 17;

    static Timer timer = new Timer();

    public static TextView alti;
    public static TextView longi;
    public static TextView lati;
    public static TextView spd;
    public static TextView c;
    public static TextView dst;
    public static Button start_srv;
    public static Button stop_srv;
    public static Button exitb;
    public static Switch sw;
    public static SeekBar sb;
    public static TextView seekVal;
    public static TextView srvStatus;
    public static ImageView mapimg;
    public static SeekBar mapzoom;
    public static TextView zoomvalue;
    public static Button button1;
    public static Button button2;
    public static Button button3;
    public static Button button4;
    public static Button button5;
    public static TextView add;

    //endregion

    public void queryLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String provider = locationManager.GPS_PROVIDER;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
        } else {
        ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION },
                1);
    }
        Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        //region DEFINING VARIABLES
        double latitude = myLocation.getLatitude();
        double longitude = myLocation.getLongitude();
        double speed = mpsTokmh(myLocation.getSpeed());
        String rSpeed = String.format("%.2f", speed); // 2 decimal accurate
        String rSpeed2 = rSpeed.replace(",", "."); // replace , with .
        double bearing = myLocation.getBearing();
        double altitude = myLocation.getAltitude();
        double accuracy = myLocation.getAccuracy();
        double distance = getDistance(latitude, initialLat, longitude, initialLong);
        double roundedDist = Math.round(distance * 100.0) / 100.0;
        long down = getDownloadAmount();
        long up = getUploadAmount();
        Long time = myLocation.getTime(); // GPS time
        String device = myLocation.getProvider();
        String address = HandleLocations.getCompleteAddressString(getBaseContext(),latitude, longitude);
        Global.latitude = String.valueOf(latitude);
        Global.longitude = String.valueOf(longitude);
        Global.altitude = String.valueOf(altitude);
        Global.speed = String.valueOf(speed);
        Global.distance = String.valueOf(roundedDist);
        Global.accuracy = String.valueOf(accuracy);
        Global.bearing = String.valueOf(bearing);
        Global.address = address;
        SimpleDateFormat niceTime = new SimpleDateFormat("mm:ss");
        long current = System.currentTimeMillis();
        long asd = current - (time);
        String nc = niceTime.format(asd);
        //endregion
        if (latitude != 0 && longitude != 0) {
            counter++;
            Global.count++;
            if (counter == 1) {
                // START POSITION (Activity/Program start)
                initialLong = longitude;
                initialLat = latitude;
            }
        }
        if (nc.contains("00:00") || nc.contains("59:59") || nc.contains("59:58") || nc.contains("00:59:58")) {
            nc = "Now";
        } else {
            nc = nc + " ago";
        }
        aplist(getBaseContext(), latitude, longitude);
        showNotif("WIFI Locator", "Count: " + String.valueOf(counter)
                + "\nLast Change: " + nc
                + "\nDistance: " + roundedDist + " meters"
                + "\nLongitude: " + longitude
                + "\nLatitude: " + latitude
                + "\nAddress: " + address
                + "\nSpeed: " + rSpeed2 + " km/h"
                + "\nAccuracy: " + accuracy + " meters"
                + "\nProvider: " + provider + " | Bandwidth: " + down + "/" + up + " mb");
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

        dl = (DrawerLayout)findViewById(R.id.drawler);
        t = new ActionBarDrawerToggle(this, dl,R.string.Open, R.string.Close);
        dl.addDrawerListener(t);
        t.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        nv = (NavigationView)findViewById(R.id.nv);
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch(id)
                {
                    case R.id.main:
                        dl.closeDrawers();
                        return true;
                    case R.id.map:
                        dl.closeDrawers();
                        Intent i = new Intent(MainActivity.this,MapActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.list:
                        dl.closeDrawers();
                        Intent i2 = new Intent(MainActivity.this,ListActivity.class);
                        startActivity(i2);
                        return true;
                    case R.id.nearby:
                        dl.closeDrawers();
                        Intent i4 = new Intent(MainActivity.this,NearbyActivity.class);
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
                Toasty.info(getBaseContext(),"Advertisement loaded!",Toast.LENGTH_SHORT,false).show();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                if (errorCode == 3) {
                } else {
                    // CODE NOT OK
                }
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                Log.w("APPERROR", "UNHANDLED EXCEPTION! RESTARTING!" + e.toString());
                writeLog(getBaseContext(), e.getMessage() + "\n\r");
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                PendingIntent pendingintent = PendingIntent.getActivity(getBaseContext(), 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingintent);
                //startActivity(intent);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(2);
            }
        });

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (runUI == true) {
                            //queryLocation();
                        }
                        if (isMyServiceRunning(BackgroundService.class) == true) {
                            srvStatus.setText("Service status: On");
                        } else {
                            srvStatus.setText("Service status: Off");
                        }
                        if (Global.maptype == null) {
                            Global.maptype = "map";
                        }
                        String url = "https://static-maps.yandex.ru/1.x/?lang=en_US" +
                                "&size=300,450" +
                                "&l=" + Global.maptype +
                                "&z=" + zoomval +
                                "&ll=" + Global.longitude + "," + Global.latitude + "" +
                                "&pt=" + Global.longitude + "," + Global.latitude + ",round";
                        try {
                            if (Global.wanarun) { // and also isNetworkAvailable()
                                SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                                String currentDateandTime = dateformat.format(new Date());
                                writeLog(getBaseContext(), currentDateandTime + "\n\r");
                                //new DownloadImageTask((ImageView) ((map)context).findViewById(R.id.mapimagee)).execute(url);
                                if(runUI == true) {
                                    new DownloadImageTask((ImageView) findViewById(R.id.mapimg)).execute(url);
                                }
                            }
                        } catch (Exception e) {
                            Log.d("APP", "HTTP ERROR");
                        }
                        longi.setText("Longitude: " + Global.longitude);
                        lati.setText("Latitude: " + Global.latitude);
                        alti.setText("Altitude: " + Global.altitude);
                        spd.setText("Speed: " + Global.speed + " km/h");
                        dst.setText("Distance: " + Global.distance + " meters");
                        add.setText("Address: " + Global.address);
                        c.setText("Count: " + Global.count);
                    }
                });
            }
        }, 0, 3000);

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
        sw = (Switch) findViewById(R.id.sw);
        sb = (SeekBar) findViewById(R.id.seekBar);
        seekVal = (TextView) findViewById(R.id.seekVal);
        srvStatus = (TextView) findViewById(R.id.srvStatus);
        mapimg = (ImageView) findViewById(R.id.mapimg);

        mapzoom = (SeekBar) findViewById(R.id.mapzoom);
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        button5 = (Button) findViewById(R.id.button5);
        zoomvalue = (TextView) findViewById(R.id.zoomvalue);
        seekVal.setText(String.valueOf(serviceRefreshInterval / 1000) + " secs");
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.maptype = "map";
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.maptype = "map,trf,skl";
            }
        });
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.maptype = "sat";
            }
        });
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.maptype = "sat,skl";
            }
        });
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Global.maptype = "map,trf,skl";
            }
        });
        //endregion
        //region UI ELEMENT LISTENERS
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    runUI = true;
                } else {
                    runUI = false;
                }
            }
        });

        exitb.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                finish();
                finishAffinity();
                System.exit(0);
            }
        });
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress > 999) {
                    serviceRefreshInterval = progress;
                    seekVal.setText(String.valueOf(progress / 1000) + " secs");
                } else {
                    serviceRefreshInterval = 1000;
                    seekBar.setProgress(1000);
                    seekVal.setText(String.valueOf(serviceRefreshInterval / 1000) + " secs");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mapzoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                zoomval = progress;
                zoomvalue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        start_srv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMyServiceRunning(BackgroundService.class)) {
                    Toasty.warning(getBaseContext(), "Service is already running!", Toast.LENGTH_SHORT, false).show();
                } else {
                    Intent serviceIntent = new Intent(getApplicationContext(), BackgroundService.class);
                    serviceIntent.putExtra("serviceinterval", String.valueOf(serviceRefreshInterval));
                    serviceIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    serviceIntent.setAction(Intent.ACTION_MAIN);
                    serviceIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                }
                timer.cancel();
                timer.purge();
                timer.scheduleAtFixedRate(new mainTask(), 0, serviceRefreshInterval);
            }
        });
        stop_srv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMyServiceRunning(BackgroundService.class)) {
                    Toasty.warning(getBaseContext(), "No service running!", Toast.LENGTH_SHORT, false).show();
                } else {
                    stopService(new Intent(getBaseContext(), BackgroundService.class));
                }
                timer.cancel();
            }
        });
        //endregion
        //region START SERVICE
        //endregion


        NavigationView navigationView = (NavigationView) findViewById(R.id.nv);
        View hView =  navigationView.getHeaderView(0);
        TextView tex = (TextView)hView.findViewById(R.id.header_verinfo);
        String version = "Version: " + String.valueOf(BuildConfig.VERSION_NAME) + " Build: " + String.valueOf(BuildConfig.VERSION_CODE);
        tex.setText(version);
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
        if(t.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    public static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.d("HTTP ERROR", e.getMessage());
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
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
    public double mpsTokmh(double mps) {
        return mps * 3.6;
    }
    public boolean areWeLocal() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 192.168.0.43");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
    public LocationListener mListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d("APP", "LOCATION CHANGED !!!!");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };
    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public void showNotif(String Title, String Text) {
        Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);

        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0,
                notificationIntent, 0);

        Intent intent2 = new Intent(getBaseContext(), Receiver.class);
        Intent intent3 = new Intent(getBaseContext(), Receiver.class);
        Intent intent4 = new Intent(getBaseContext(), Receiver.class);
        intent2.setAction("exit");
        intent3.setAction("resume");
        intent4.setAction("pause");

        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(getBaseContext(), 1, intent2, PendingIntent.FLAG_ONE_SHOT);
        PendingIntent pendingIntent3 = PendingIntent.getBroadcast(getBaseContext(), 1, intent3, PendingIntent.FLAG_ONE_SHOT);
        PendingIntent pendingIntent4 = PendingIntent.getBroadcast(getBaseContext(), 1, intent4, PendingIntent.FLAG_ONE_SHOT);

        //sendBroadcast(intent2);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context);
        Notification notification2 = builder.setContentIntent(intent)
                .addAction(R.drawable.gpsicon, "Pause", pendingIntent4)
                .addAction(R.drawable.gpsicon, "Resume", pendingIntent3)
                .addAction(R.drawable.gpsicon, "Exit", pendingIntent2)
                .setSmallIcon(R.drawable.computer).setTicker(Text).setWhen(0)
                .setAutoCancel(true)
                .setContentTitle(Title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(Text))
                .setContentText(Text).build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification2);

    }
    public static ArrayList<String> aplist(final Context context, double lati, double longi) {
        ArrayList<String> apList = new ArrayList<String>();
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult result : scanResults) {
                String enc = "notavailable";
                if(!result.capabilities.contains("WEP") || !result.capabilities.contains("WPA")){
                    enc = "NONE";
                }
                else if(result.capabilities.contains("WEP")){
                    enc = "WEP";
                }
                else if(result.capabilities.contains("WPA")){
                    enc = "WPA";
                }
                String android_id = Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                int versionCode = BuildConfig.VERSION_CODE;
                //String versionName = BuildConfig.VERSION_NAME;
                String url = "http://sont.sytes.net/mcuinsert2.php";
                String reqBody = "?id=0&ssid=" + result.SSID + "&bssid=" + result.BSSID + "&source=" + android_id + "_v" + versionCode + "&enc=" + enc + "&rssi=" + ConvertDBM(result.level) + "&long=" + longi + "&lat=" + lati + "&add=" + "addition" + "&channel=" + result.frequency;
                saveRecordHttp(url+reqBody);
            }
        } catch (Exception e) {
            Log.d("APP", "ERROR " + e.getMessage());
        }
        return apList;
    }
    public void logUser() {
        // TODO: Use the current user's information
        // You can call any combination of these three methods
        Crashlytics.setUserIdentifier("12345");
        Crashlytics.setUserEmail("sont16@gmail.com");
        Crashlytics.setUserName("wifilocatoruser");
    }
    public long getDownloadAmount() {
        long dl = TrafficStats.getUidRxBytes(this.getApplicationInfo().uid) / 1024 / 1024;
        return dl;
    }
    public long getUploadAmount() {
        long ul = TrafficStats.getUidTxBytes(this.getApplicationInfo().uid) / 1024 / 1024;
        return ul;
    }
    public String readFile() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String name = preferences.getString("wifilog", "");
        return name;
    }
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    public void writeLog(Context c, String data) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("wifilog", readFile() + data);
        editor.apply();
    }
    public String convertLongTimeWithTimeZome(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(time);
        return (cal.get(Calendar.YEAR) + " " + (cal.get(Calendar.MONTH) + 1) + " "
                + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR_OF_DAY) + ":"
                + cal.get(Calendar.MINUTE));

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
    public class mainTask extends TimerTask {
        public void run() {
            Message message = mHandler.obtainMessage(0);
            message.sendToTarget();
        }
    }
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    // to stop the timer
                    //mHandler.removeCallbacksAndMessages(runnable);
                    break;
                default:
                    break;
            }
            if (Global.wanarun) { // and also isNetworkAvailable()
                queryLocation();
            }
        }
    };
    public void init() {
        int interval = 1000;
        timer.scheduleAtFixedRate(new mainTask(), 0, interval);
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // LOCATION PERMISSION CHECK IF NOT ASK FOR IT
        if(checkPermissionLocation() == false) {
            Toasty.error(getBaseContext(), "Missing location permission!", Toast.LENGTH_SHORT, false).show();
            requestPermissionLocation();
        }
        // TURN ON WIFI
        if (!wifi.isWifiEnabled()) {
            Toasty.info(getBaseContext(),"Turning on WiFi..",Toast.LENGTH_SHORT).show();
            wifi.setWifiEnabled(true);
        }
    }
    public static void saveRecordHttp(String path){
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(path, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                //Toasty.success(context,"HTTP SUCCESS",Toast.LENGTH_SHORT,false).show();
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
    public boolean checkPermissionLocation(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        else{
            return true;
        }
    }
    public void requestPermissionLocation(){
        // requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_LOCATION);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }
    private void openFragment(final Fragment fragment, int element){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(element, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}

