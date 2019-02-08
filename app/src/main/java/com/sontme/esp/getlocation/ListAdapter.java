package com.sontme.esp.getlocation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ListAdapter extends ArrayAdapter<ApStrings> {
    private int resourceLayout;
    private Context mContext;

    public ListAdapter(Context context, int resource, List<ApStrings> aps) {
        super(context, resource);
        this.resourceLayout = resource;
        this.mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(resourceLayout, null);
        }
        if (position % 2 == 1) {
        } else {
            view.setBackgroundColor(Color.argb(85,190,190,190));
        }

        ApStrings aps = getItem(position);
        if (aps != null) {
            TextView tt1 = (TextView) view.findViewById(R.id.time);
            TextView tt2 = (TextView) view.findViewById(R.id.ssid);
            //TextView tt3 = (TextView) view.findViewById(R.id.mac);
            TextView tt4 = (TextView) view.findViewById(R.id.source);

            TextView ttlati = (TextView) view.findViewById(R.id.txtLati);
            TextView ttlongi = (TextView) view.findViewById(R.id.txtLongi);

            tt1.setText(aps.getTime());
            tt2.setText(aps.getSsid());
            //tt3.setText(aps.getMac());
            String android_id = Settings.Secure.getString(getContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            String source_ = "adapter error";
            if(aps.getSource().contains(android_id)){
                String[] a = aps.getSource().split("_");
                source_ =  "THIS_" + a[1];
                tt4.setText(source_);
            }
            else{
                tt4.setText(aps.getSource());
            }
            ttlati.setText(aps.getLati());
            ttlongi.setText(aps.getLongi());
        }
        return view;
    }

}
