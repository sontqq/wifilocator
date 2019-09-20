package com.sontme.esp.getlocation.activities;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.sontme.esp.getlocation.ApStrings;
import com.sontme.esp.getlocation.BuildConfig;
import com.sontme.esp.getlocation.ListAdapter;
import com.sontme.esp.getlocation.R;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class ListActivity extends AppCompatActivity {

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv3;
    static String content = null;

    ArrayList<ApStrings> aplist = new ArrayList<ApStrings>();
    ListAdapter customAdapter;
    List<String> uniqueMacList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        final ListView yourListView = findViewById(R.id.mainListView);

        customAdapter = new ListAdapter(this, R.layout.row, aplist);
        yourListView.setAdapter(customAdapter);
        customAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                yourListView.setSelection(customAdapter.getCount() - 1);
            }
        });

        dl = findViewById(R.id.drawler3);
        t = new ActionBarDrawerToggle(this, dl, R.string.Open, R.string.Close);
        dl.addDrawerListener(t);
        t.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        nv3 = findViewById(R.id.nv3);
        nv3.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.main:
                        dl.closeDrawers();
                        Intent i1 = new Intent(ListActivity.this, MainActivity.class);
                        startActivity(i1);
                        return true;
                    case R.id.map:
                        dl.closeDrawers();
                        Intent i2 = new Intent(ListActivity.this, MapActivity.class);
                        startActivity(i2);
                        return true;
                    case R.id.list:
                        dl.closeDrawers();
                        return true;
                    case R.id.nearby:
                        dl.closeDrawers();
                        Intent i4 = new Intent(ListActivity.this, NearbyActivity.class);
                        startActivity(i4);
                        return true;
                    case R.id.more:
                        dl.closeDrawers();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sont.sytes.net/moreapps"));
                        startActivity(browserIntent);
                        return true;
                    default:
                        dl.closeDrawers();
                        return true;
                }
            }
        });

        NavigationView navigationView = findViewById(R.id.nv3);
        View hView = navigationView.getHeaderView(0);
        TextView tex = hView.findViewById(R.id.header_verinfo);
        String version = "Version: " + BuildConfig.VERSION_NAME + " Build: " + BuildConfig.VERSION_CODE;
        tex.setText(version);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Download list
                getList(getBaseContext(), "https://sont.sytes.net/wifilocator/wifi_list.php");
                handler.postDelayed(this, 5000);
            }
        }, 5000);

        yourListView.setSelection(customAdapter.getCount() - 1);
    }

    public void getList(final Context context, String url) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                content = new String(response, StandardCharsets.UTF_8);
                String stripped = html2text(content);
                String[] lines = stripped.split("\\r?\\n");
                for (String s : lines) {
                    String[] splittedStr = s.split("OVER");
                    List<ApStrings> aps = new ArrayList<>();
                    String time = splittedStr[1];
                    String ssid = splittedStr[2];
                    String mac = splittedStr[3];
                    String source = splittedStr[8];
                    String str = splittedStr[4];
                    String lat = splittedStr[7];
                    String lon = splittedStr[6];
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Date convertedDate = new Date();
                    try {
                        convertedDate = dateFormat.parse(time);
                    } catch (ParseException e) {
                    }
                    //String d = dateDiff(convertedDate);
                    String d = String.valueOf(convertedDate);
                    ApStrings one_row = new ApStrings(lat, lon, d, ssid, mac, str, source);
                    if (uniqueMacList.contains(mac) == false) {
                        uniqueMacList.add(mac);
                        customAdapter.add(one_row);
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
            }

            @Override
            public void onRetry(int retryNo) {
            }
        });
    }

    public static String html2text(String html) {
        return android.text.Html.fromHtml(html).toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (t.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final ListView yourListView = findViewById(R.id.mainListView);
        customAdapter = new ListAdapter(this, R.layout.row, aplist);
        yourListView.setAdapter(customAdapter);
        customAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                yourListView.setSelection(customAdapter.getCount() - 1);
            }
        });
        yourListView.setSelection(customAdapter.getCount() - 1);
        //customAdapter.clear();
        getList(getBaseContext(), "https://sont.sytes.net/wifilocator/wifi_list.php");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
