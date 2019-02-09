package com.sontme.esp.getlocation.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sontme.esp.getlocation.BuildConfig;
import com.sontme.esp.getlocation.Global;
import com.sontme.esp.getlocation.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;

    static List<GeoPoint> geoPoints = new ArrayList<>();
    static ArrayList<OverlayItem> overlayItemArray = new ArrayList<OverlayItem>();
    static Polyline line = new Polyline();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        dl = (DrawerLayout)findViewById(R.id.drawler2);
        t = new ActionBarDrawerToggle(this, dl,R.string.Open, R.string.Close);
        dl.addDrawerListener(t);
        t.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        nv = (NavigationView)findViewById(R.id.nv2);
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

        NavigationView navigationView = (NavigationView) findViewById(R.id.nv2);
        View hView =  navigationView.getHeaderView(0);
        TextView tex = (TextView)hView.findViewById(R.id.header_verinfo);
        String version = "Version: " + String.valueOf(BuildConfig.VERSION_NAME) + " Build: " + String.valueOf(BuildConfig.VERSION_CODE);
        tex.setText(version);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        final MapView map = (MapView) findViewById(R.id.osmmap);
        IMapController mapController = map.getController();
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        mapController.setZoom(18.0);
        GeoPoint startPoint = new GeoPoint(Double.valueOf(Global.latitude), Double.valueOf(Global.longitude));
        mapController.setCenter(startPoint);
        updateMap(map);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateMap(map);
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }
    private void updateMap(MapView map) {
        GeoPoint geo = new GeoPoint(Double.valueOf(Global.latitude), Double.valueOf(Global.longitude));
        OverlayItem point = new OverlayItem("Actual", "Position", geo);
        ItemizedIconOverlay<OverlayItem> itemizedIconOverlay = new ItemizedIconOverlay<OverlayItem>(this, overlayItemArray, null);
        line.setOnClickListener(new Polyline.OnClickListener() {
            @Override
            public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                Toast.makeText(mapView.getContext(), "Clicked line! " + polyline.getPoints().size() + "pts was tapped", Toast.LENGTH_LONG).show();
                return false;
            }
        });
        if(map.getOverlays().size() < 2){
        //if(geoPoints.size() < 2) {
            geoPoints.add(geo);
            map.getOverlays().add(itemizedIconOverlay);
            overlayItemArray.add(point);
            //RGB(205, 92, 92)
            //RGB(240, 128, 128)
            //RGB(250, 128, 114)
            //RGB(233, 150, 122)
            //RGB(255, 160, 122)
            line.setColor(Color.argb(90,240,128,128));
            line.setWidth(20.0f);
            line.setPoints(geoPoints);
            map.getOverlayManager().add(line);
        }

        if(geoPoints.contains(geo) != true) {
            geoPoints.add(geo);
            map.getOverlays().add(itemizedIconOverlay);
            line.setColor(Color.argb(90,240,128,128));
            line.setWidth(20.0f);
            line.setPoints(geoPoints);
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

}
