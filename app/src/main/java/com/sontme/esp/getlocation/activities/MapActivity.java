package com.sontme.esp.getlocation.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sontme.esp.getlocation.BackgroundService;
import com.sontme.esp.getlocation.BuildConfig;
import com.sontme.esp.getlocation.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//import com.github.johnpersano.supertoasts.library.Style;
//import com.github.johnpersano.supertoasts.library.SuperActivityToast;

public class MapActivity extends AppCompatActivity implements GpsStatus.Listener {

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;
    private Button export;
    private LocationManager mService;

    public static List<GeoPoint> geoPoints;
    public static ArrayList<OverlayItem> overlayItemArray;
    public static Polyline line;
    public BackgroundService backgroundService;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mService.addGpsStatusListener(this);

        Intent mIntent = new Intent(MapActivity.this, BackgroundService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        line = new Polyline();
        geoPoints = new ArrayList<>();
        overlayItemArray = new ArrayList<OverlayItem>();

        dl = findViewById(R.id.drawler2);
        t = new ActionBarDrawerToggle(this, dl,R.string.Open, R.string.Close);
        dl.addDrawerListener(t);
        t.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        nv = findViewById(R.id.nv2);
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch(id)
                {
                    case R.id.main:
                        dl.closeDrawers();
                        Intent i1 = new Intent(MapActivity.this,MainActivity.class);
                        startActivity(i1);
                        return true;
                    case R.id.map:
                        dl.closeDrawers();
                        return true;
                    case R.id.list:
                        dl.closeDrawers();
                        Intent i3 = new Intent(MapActivity.this,ListActivity.class);
                        startActivity(i3);
                        return true;
                    case R.id.nearby:
                        dl.closeDrawers();
                        Intent i4 = new Intent(MapActivity.this,NearbyActivity.class);
                        startActivity(i4);
                        return true;
                    default:
                        dl.closeDrawers();
                        return true;
                }
            }
        });
        export = findViewById(R.id.btn_export);
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = findViewById(R.id.osmmap);
                Bitmap bitmap = viewToBitmap(view);
                try {
                    FileOutputStream output = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/map.png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                    output.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                galleryAddPic();
                Toast.makeText(getApplicationContext(), "Image saved", Toast.LENGTH_SHORT).show();
            }
        });
        NavigationView navigationView = findViewById(R.id.nv2);
        View hView =  navigationView.getHeaderView(0);
        TextView tex = hView.findViewById(R.id.header_verinfo);
        String version = "Version: " + String.valueOf(BuildConfig.VERSION_NAME) + " Build: " + String.valueOf(BuildConfig.VERSION_CODE);
        tex.setText(version);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        final MapView map = findViewById(R.id.osmmap);
        IMapController mapController = map.getController();
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        mapController.setZoom(18.0);
        GeoPoint startPoint = null;
        if (BackgroundService.getLatitude() != null) {
            startPoint = new GeoPoint(Double.valueOf(BackgroundService.getLatitude()), Double.valueOf(BackgroundService.getLongitude()));
        }
        else{
            startPoint = new GeoPoint(47.935900, 20.367770);
        }
        mapController.setCenter(startPoint);


        updateMap(map);
        drawPoint(map);


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateMap(map);
                handler.postDelayed(this, 1000);
            }
        }, 1000);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public Bitmap viewToBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/map.png");
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
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

    private void updateMap(MapView map) {
        GeoPoint geo = null;
        if (BackgroundService.getLatitude() != null) {
            geo = new GeoPoint(Double.valueOf(BackgroundService.getLatitude()), Double.valueOf(BackgroundService.getLongitude()));
        }
        else{
            geo = new GeoPoint(47.935900, 20.367770);
        }
        OverlayItem point = new OverlayItem("Actual", "Position", geo);
        ItemizedIconOverlay<OverlayItem> itemizedIconOverlay = new ItemizedIconOverlay<OverlayItem>(this, overlayItemArray, null);
        line.setOnClickListener(new Polyline.OnClickListener() {
            @Override
            public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {

                return false;
            }
        });

        if(geoPoints.contains(geo) != true) {
            geoPoints.add(geo);
            map.getOverlays().add(itemizedIconOverlay);
            line.setColor(Color.argb(90,240,128,128));
            line.setWidth(20.0f);
            line.getPaint().setStrokeJoin(Paint.Join.ROUND);
            try {
                line.setPoints(geoPoints);
            }catch (Exception e){}
            map.getOverlayManager().add(line);
        }
        map.invalidate();

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(t.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    public void drawPoint(MapView map){
        map.getOverlays().clear();
        map.invalidate();
        Drawable pin = getResources().getDrawable(R.drawable.wifi5);
        GeoPoint geo = null;
        if (BackgroundService.getLatitude() != null) {
            geo = new GeoPoint(Double.valueOf(BackgroundService.getLatitude()), Double.valueOf(BackgroundService.getLongitude()));
        }
        else{
            geo = new GeoPoint(47.935900, 20.367770);
        }
        Marker m = new Marker(map);
        m.setTitle("Start Point");
        m.setSubDescription("The location where you started");
        m.setIcon(resize(pin,100));
        m.setPosition(geo);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        m.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                Toast.makeText(getApplicationContext(), "Marker count: " + mapView.getOverlays().size(), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        map.getOverlays().add(m);
        map.invalidate();
    }

    private Drawable resize(Drawable image, Integer size) {
        Bitmap b = ((BitmapDrawable)image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, size, size, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }

}
