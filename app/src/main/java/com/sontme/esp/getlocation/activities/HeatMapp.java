package com.sontme.esp.getlocation.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sontme.esp.getlocation.R;
import com.sontme.esp.getlocation.SontHelper;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import ca.hss.heatmaplib.HeatMap;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HeatMapp extends AppCompatActivity implements SurfaceHolder.Callback {

    int width;
    int height;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float xrf = event.getRawX();
        float yrf = event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                HeatMap heatMap = findViewById(R.id.heatmap);
                heatMap.setMinimum(0.0);
                heatMap.setMaximum(10000.0);
                heatMap.setMaxDrawingHeight(10000);
                heatMap.setMaxDrawingWidth(10000);
                HeatMap.DataPoint point = new HeatMap.DataPoint(xrf, yrf, xrf);
                Log.d("HEATMAP_", "values: " + yrf + "_" + xrf);
                heatMap.addData(point);
                //heatMap.forceRefresh();
                break;
        }
        return false;
    }

    public void request_heatmap(String data, String url) {
        try {
            SeekBar sb1 = findViewById(R.id.seekBar);
            SeekBar sb2 = findViewById(R.id.seekBar2);

            url = url + "?row=" + sb1.getProgress() + "&col=" + sb2.getProgress();

            Log.d("POST_ok_", url);
            Log.d("POST_ok_", "sending: " + data.length() + " bytes");
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            OkHttpClient client = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .callTimeout(600, TimeUnit.SECONDS)
                    .connectTimeout(600, TimeUnit.SECONDS)
                    .writeTimeout(600, TimeUnit.SECONDS)
                    .readTimeout(600, TimeUnit.SECONDS)
                    .build();

            RequestBody body = RequestBody
                    .create(data, JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Origin", url)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();

            final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (response.isSuccessful() == true) {
                        try {
                            Log.d("POST_ok_", "success" + body.contentLength());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    ImageView img = findViewById(R.id.resp_bitmap);
                    img.setImageBitmap(bitmap);
                    Log.d("POST_ok_", "resp bytes: " + bitmap.getByteCount());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("POST_ok_", "error: " + e.getMessage() + "_" + e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heat_map);

        Button btn = findViewById(R.id.btn_post);
        SeekBar s1 = findViewById(R.id.seekBar);
        SeekBar s2 = findViewById(R.id.seekBar2);
        TextView txt = findViewById(R.id.actual);
        s1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt.setText(s1.getProgress() + " / " + s2.getProgress());
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (SontHelper.check_if_local(getApplicationContext()) == true) {
                            request_heatmap(generateHeatMapData(s1.getProgress(), s2.getProgress()), "http://192.168.0.43:8000");
                        } else {
                            request_heatmap(generateHeatMapData(s1.getProgress(), s2.getProgress()), "http://sont.sytes.net:8000");
                        }
                    }
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        s2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txt.setText(s1.getProgress() + " / " + s2.getProgress());
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (SontHelper.check_if_local(getApplicationContext()) == true) {
                            request_heatmap(generateHeatMapData(s1.getProgress(), s2.getProgress()), "http://192.168.0.43:8000");
                        } else {
                            request_heatmap(generateHeatMapData(s1.getProgress(), s2.getProgress()), "http://sont.sytes.net:8000");
                        }
                    }
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (SontHelper.check_if_local(getApplicationContext()) == true) {
                            request_heatmap(generateHeatMapData(s1.getProgress(), s2.getProgress()), "http://192.168.0.43:8000");
                        } else {
                            request_heatmap(generateHeatMapData(s1.getProgress(), s2.getProgress()), "http://sont.sytes.net:8000");
                        }
                    }
                });
            }
        });
        btn.setText("Get HEATMAP");

        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        Log.d("HEATMAP_", "Screen Size: " + width + "," + height);
        HeatMap heatMap = findViewById(R.id.heatmap);
        heatMap.setVisibility(View.GONE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (SontHelper.check_if_local(getApplicationContext()) == true) {
                    request_heatmap(generateHeatMapData(50, 10), "http://192.168.0.43:8000");
                } else {
                    request_heatmap(generateHeatMapData(50, 10), "http://sont.sytes.net:8000");
                }
            }
        });

        // END OF ONCREATE()
    }


    public String generateHeatMapData(int lines, int columns) {

        Random rnd = new Random();
        String csv2 = "";
        String csv3 = "";
        Log.d("POST_ok_", "generating csv_1: " + csv2.length() + " bytes" + csv2.length() / 1024 + " kilobytes");
        /*for(int i = 0; i < lines; i++) {
            for(int j = 0; j < columns; j++) {
                csv2 = csv2 + (rnd.nextInt(100-1)+100) + ",";
            }
            csv2 = csv2 + "\n";
            Log.d("POST_ok_", "generating csv_1_2: " + csv2.length() + " bytes" + csv2.length()/1024 + " kilobytes");
        }
        Log.d("POST_ok_", "generating csv_2: " + csv2.length() + " bytes" + csv2.length()/1024 + " kilobytes");
        BufferedReader bufReader = new BufferedReader(new StringReader(csv2));
        String line = null;
        try {
            while ((line = bufReader.readLine()) != null) {
                int endIndex = line.lastIndexOf(",");
                String newString = line.substring(0, endIndex);
                csv3 = csv3 + newString + "\n";
            }
            Log.d("POST_ok_", "generating csv_3: " + csv3.length() + " bytes" + csv3.length()/1024 + " kilobytes");
        }catch (Exception e){
            e.printStackTrace();
        }
        */
        return csv3;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}