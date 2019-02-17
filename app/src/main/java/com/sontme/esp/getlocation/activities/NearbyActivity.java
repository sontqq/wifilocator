package com.sontme.esp.getlocation.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.Circle;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions;
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

import com.sontme.esp.getlocation.ApStrings;
import com.sontme.esp.getlocation.BuildConfig;
import com.sontme.esp.getlocation.Global;
import com.sontme.esp.getlocation.HandleLocations;
import com.sontme.esp.getlocation.R;


public class NearbyActivity extends AppCompatActivity {

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;
    private MapView map;
    private Button btn;
    String content = null;

    static Map<Location, ApStrings> loc_ssid2 = new HashMap<Location, ApStrings>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        org.osmdroid.config.IConfigurationProvider osmConf = org.osmdroid.config.Configuration.getInstance();
        File basePath = new File(getCacheDir().getAbsolutePath(), "osmdroid");
        osmConf.setOsmdroidBasePath(basePath);
        File tileCache = new File(osmConf.getOsmdroidBasePath().getAbsolutePath(), "tile");
        osmConf.setOsmdroidTileCache(tileCache);

        setContentView(R.layout.activity_nearby);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        map = (MapView) findViewById(R.id.osmmap2);
        btn = (Button)findViewById(R.id.button6);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getList(getBaseContext(), "https://sont.sytes.net/wifis_stripped_open.php");
            }
        });

        IMapController mapController = map.getController();
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        mapController.setZoom(17.0);
        GeoPoint startPoint = new GeoPoint(Double.valueOf(Global.latitude), Double.valueOf(Global.longitude));
        mapController.setCenter(startPoint);

        dl = (DrawerLayout)findViewById(R.id.drawler4);
        t = new ActionBarDrawerToggle(this, dl,R.string.Open, R.string.Close);
        dl.addDrawerListener(t);
        t.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        nv = (NavigationView)findViewById(R.id.nv4);
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch(id)
                {
                    case R.id.main:
                        dl.closeDrawers();
                        Intent i0 = new Intent(NearbyActivity.this,MainActivity.class);
                        startActivity(i0);
                        return true;
                    case R.id.map:
                        dl.closeDrawers();
                        Intent i = new Intent(NearbyActivity.this,MapActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.list:
                        dl.closeDrawers();
                        Intent i2 = new Intent(NearbyActivity.this,ListActivity.class);
                        startActivity(i2);
                        return true;
                    case R.id.nearby:
                        dl.closeDrawers();
                        return true;
                    default:
                        dl.closeDrawers();
                        return true;
                }
            }
        });
        NavigationView navigationView = (NavigationView) findViewById(R.id.nv4);
        View hView =  navigationView.getHeaderView(0);
        TextView tex = (TextView)hView.findViewById(R.id.header_verinfo);
        String version = "Version: " + String.valueOf(BuildConfig.VERSION_NAME) + " Build: " + String.valueOf(BuildConfig.VERSION_CODE);
        tex.setText(version);
        getList(getBaseContext(), "https://sont.sytes.net/wifis_stripped_open.php");


    }

    private Drawable resize(Drawable image, Integer size) {
        Bitmap b = ((BitmapDrawable)image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, size, size, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }

    protected void drawCircle(MapView map){
        Polygon oPolygon = new Circlee(map);
        final double radius = 150;
        Double lat = Double.valueOf(Global.latitude);
        Double lon = Double.valueOf(Global.longitude);
        ArrayList<GeoPoint> circlePoints = new ArrayList<GeoPoint>();
        GeoPoint p = new GeoPoint(lat,lon);
        circlePoints.add(p);
        for (float f = 0; f < 360; f++){
            circlePoints.add(new GeoPoint(lat, lon).destinationPoint(radius, f));
        }
        oPolygon.setPoints(circlePoints);
        oPolygon.setStrokeWidth(20.0f);
        final InfoWindow pop = new PopUpWin(R.layout.popup, map);
        oPolygon.setTitle("karika title");
        oPolygon.setSubDescription("karika subdest");
        oPolygon.setInfoWindow(pop);
        oPolygon.setFillColor(Color.argb(60,233,150,122));
        oPolygon.setStrokeColor(Color.argb(85,255,0,255));
        oPolygon.setOnClickListener(new Polygon.OnClickListener() {
            @Override
            public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {
                return false;
            }
        });
        map.getOverlays().add(oPolygon);

    }

    protected void drawMarkers(final MapView map, Map<Location, ApStrings> loc_ssid) {
        //final RadiusMarkerClusterer clusterer = new RadiusMarkerClusterer(this);
        final RadiusMarkerClusterer clusterer = new CustomCluster(this);
        final List<Overlay> overlays = map.getOverlays();
        MapController mapController = (MapController) map.getController();

        overlays.clear();

        Bitmap icon = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.wifi4_cluster_25);

        //clusterer.setIcon(getResizedBitmap(icon,100,100));
        clusterer.setIcon(icon);
        clusterer.setRadius(85);
        clusterer.mTextAnchorU = 0.70f;
        clusterer.mTextAnchorV = 0.27f;
        clusterer.getTextPaint().setTextSize(20.0f);
        

        map.getOverlays().clear();
        map.invalidate();
        int counter = 0;
        Drawable pin = getResources().getDrawable(R.drawable.wifi4_25);
        for (Map.Entry<Location, ApStrings> entry : loc_ssid.entrySet()) {
            Location coords = entry.getKey();

            String time = entry.getValue().getTime();
            String ssid = entry.getValue().getSsid();
            String bssid = entry.getValue().getMac();
            String source = entry.getValue().getSource();
            String str = entry.getValue().getStr();
            String la = entry.getValue().getLati();
            String lo = entry.getValue().getLongi();

            String description = "Time: " + time + "\n" + "MAC: " + bssid;
            String snippet = "Source: " + source;

            GeoPoint geo = new GeoPoint(coords.getLatitude(), coords.getLongitude());
            Marker m = new Marker(map);
            m.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    if(marker.isInfoWindowShown()){
                        marker.closeInfoWindow();
                    }
                    else{
                        marker.showInfoWindow();
                    }
                    return true;
                }
            });
            m.setTitle(ssid);
            m.setSnippet(snippet);
            m.setSubDescription(description);
            final InfoWindow pop = new PopUpWin(R.layout.popup, map);
            m.setInfoWindow(pop);
            //m.setIcon(resize(pin,100));
            m.setIcon(pin);
            m.setPosition(geo);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            //map.getOverlays().add(m);
            clusterer.add(m);
            //map.invalidate();
            counter++;
        }
        overlays.add(clusterer);

        if(map.getMaxZoomLevel() <= 17){
            mapController.setZoom(18);
        }
        //mapController.setZoom(map.getZoomLevel()-0.01);

        map.invalidate();
        NearbyActivity.loc_ssid2.clear();
        Toast.makeText(getBaseContext(),counter + "aps found",Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(t.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    public void getList(final Context context, String url) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    content = new String(response, "UTF-8");
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                }
                String stripped = html2text(content);
                String lines[] = stripped.split("\\r?\\n");
                for (String s : lines) {
                    String[] splittedStr = s.split("OVER");
                    String recordTime = splittedStr[1];
                    String ssid = splittedStr[2];
                    String bssid = splittedStr[3];
                    String str = splittedStr[4];
                    String source;
                    try{
                         source = splittedStr[8];
                    }
                    catch(Exception e){
                        source = "No data";
                    }
                    try {
                        double longi = ParseDouble(splittedStr[6]);
                        double lati = ParseDouble(splittedStr[7]);
                        Location x = new Location("");
                        x.setLatitude(lati);
                        x.setLongitude(longi);
                        ApStrings desc = new ApStrings(recordTime, ssid, bssid, str, source);
                        loc_ssid2.put(x, desc);
                    }
                    catch (Exception e){}
                }
                drawMarkers(map,loc_ssid2);
                drawCircle(map);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Toast.makeText(getBaseContext(), "Failed to retreive AP list", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRetry(int retryNo) {
            }
        });
    }

    public static String html2text(String html) {
        return android.text.Html.fromHtml(html).toString();
    }

    static double ParseDouble(String strNumber) {
        if (strNumber != null && strNumber.length() > 0) {
            try {
                return Double.parseDouble(strNumber);
            } catch(Exception e) {
                return -1;
            }
        }
        else return 0;
    }

    public Context getContext() {
        return getApplicationContext();
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }
}

class Circlee extends Polygon {
    private MapView map;

    public Circlee(MapView map){
        this.map = map;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
        /*if (e.getAction() == MotionEvent.ACTION_UP && contains(e)) {
            return true;
        }
        return super.onSingleTapUp(e, mapView);*/
        return false;
    }

    @Override
    public boolean onLongPress(MotionEvent e, MapView mapView){
        Toast.makeText(mapView.getContext(),"Items: " + mapView.getOverlays().size(),Toast.LENGTH_SHORT).show();
        Log.d("TAPI","LONGTAPTAPTAPTAPTAP_" + e.toString());
        return super.onLongPress(e, mapView);
    }
}

class PopUpWin extends InfoWindow {

    private int layoutID;
    private MapView map;


    public PopUpWin(int layoutResId, MapView map) {
        super(layoutResId, map);
        this.layoutID = layoutID;
        this.map = map;
    }

    @Override
    public void onOpen(Object item) {
        InfoWindow.closeAllInfoWindowsOn(map);
        PopUpWin.closeAllInfoWindowsOn(map);
        String title;
        String desc;
        String snip;

        LinearLayout layout = (LinearLayout) mView.findViewById(R.id.plinlay);
        Button btn = (Button) mView.findViewById(R.id.pbtn);
        TextView ssid = (TextView) mView.findViewById(R.id.pssid);
        TextView descr = (TextView) mView.findViewById(R.id.pdesc);
        TextView psnip = (TextView) mView.findViewById(R.id.psnip);
        ImageView img = (ImageView) mView.findViewById(R.id.pimg);
        TextView pstat = (TextView) mView.findViewById(R.id.pstat);

        if(item instanceof Marker) {
            final Marker marker = (Marker)item;

            title = marker.getTitle();
            desc = marker.getSubDescription();
            snip = marker.getSnippet();

            String android_id = Settings.Secure.getString(map.getContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            String source_;
            if(snip.contains(android_id)){
                String[] a = snip.split("_");
                source_ =  "Source: THIS_" + a[1];
                psnip.setText(source_);
            }
            else{
                psnip.setText(snip);
            }
            ssid.setText("SSID: " + title);
            descr.setText(desc);

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("CLICKED BUTTON", "title: " + marker.getTitle());
                    Log.d("CLICKED BUTTON", "desc: " + marker.getSubDescription());
                    //map.invalidate();
                    marker.remove(map);
                    marker.closeInfoWindow();
                    map.invalidate();
                }
            });
        }
    }

    @Override
    public void onClose() {

    }
}

class CustomCluster extends RadiusMarkerClusterer {

    private Context ctx;
    private static int counter;

    public CustomCluster(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        counter++;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
        Log.d("CLUSTER","CLUSTER TAPPED: " + e.getX() + ", " + e.getY());
        return super.onSingleTapUp(e, mapView);
    }

    public static int getNumOfInstance() {
        return counter;
    }
}
