package com.sontme.esp.getlocation.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.sontme.esp.getlocation.BackgroundService;
import com.sontme.esp.getlocation.R;

public class RealTimeChart extends AppCompatActivity {

    private final Handler mHandler = new Handler();
    private Runnable mTimer1;
    private LineGraphSeries<DataPoint> mSeries1;
    private double graph2LastXValue = 5d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_chart);


        GraphView graph = findViewById(R.id.rlgraph);
        mSeries1 = new LineGraphSeries<>(generateData());
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);
        graph.addSeries(mSeries1);

        mTimer1 = new Runnable() {
            @Override
            public void run() {
                //mSeries1.resetData(generateData());
                graph2LastXValue += 1d;
                mSeries1.appendData(new DataPoint(graph2LastXValue, getWifiC()), true, 40);
                mHandler.postDelayed(this, 100);
            }
        };
        mHandler.postDelayed(mTimer1, 0);
    }

    private DataPoint[] generateData() {
        DataPoint[] values = new DataPoint[1];
        DataPoint v = new DataPoint(0, 5);
        values[0] = v;
        return values;
    }

    private double getWifiC() {
        double r = 0;
        try {
            r = BackgroundService.scanResults_forchart.size();
        } catch (Exception e) {
            r = 0;
        }
        return r;
    }

}
