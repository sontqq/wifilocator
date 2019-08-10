package com.sontme.esp.getlocation;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.IOException;

public class activity_camera extends AppCompatActivity implements SurfaceHolder.Callback {

    Camera camera;
    SurfaceView sf;
    SurfaceHolder sh;

    private CustomView customView;

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        camera = Camera.open(1);
        camera.setDisplayOrientation(90);
        camera.startFaceDetection();
        camera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
            @Override
            public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                for (Camera.Face x : faces) {
                    try {
                        /*
                        customView.setRect(x.rect);
                        customView.setEye1(x.leftEye);
                        customView.setEye2(x.rightEye);
                        customView.setMouth(x.mouth);
                        customView.setScore(x.score);
                        customView.rajz();
                        customView.draw(new Canvas());
                        customView.invalidate();
                        */

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        if (camera != null) {
            camera.stopPreview();
            try {
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        customView = new CustomView(activity_camera.this, null);
        sf = findViewById(R.id.sf_preview);
        sf.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_UP:
                        Log.d("camera_api", "touch: " + x + "," + y);
                }
                return false;
            }
        });
        FrameLayout framelay = findViewById(R.id.framelay);
        framelay.addView(customView);
        sh = sf.getHolder();
        sh.addCallback(this);

        Camera.PictureCallback prevc_jpeg = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    if (data != null) {
                        Intent i = new Intent(activity_camera.this, activity_image.class);
                        i.putExtra("bitmap", data);
                        startActivity(i);
                    } else {
                        Log.d("camera_api", "data is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("camera_api", "error: " + e.toString());
                }
            }
        };
        Camera.ShutterCallback shutc = new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                Log.d("camera_api", "shuttered !");
            }
        };
        Camera.PictureCallback pictc_raw = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
            }
        };

        Button btn = findViewById(R.id.getimg);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("camera_api", "shuttered");
                camera.takePicture(shutc, pictc_raw, prevc_jpeg);
            }
        });

        Button opencv = findViewById(R.id.start_opencv);
        opencv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(activity_camera.this, opencv_realtime.class);
                startActivity(i);
                //finish();
            }
        });
    }

}

