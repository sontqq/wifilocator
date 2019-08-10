package com.sontme.esp.getlocation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class activity_image extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        Bundle b = getIntent().getExtras();
        byte[] data = null;
        if (b != null) {
            data = b.getByteArray("bitmap");
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        Matrix matrix = new Matrix();
        matrix.postRotate(270);
        Matrix m = new Matrix();
        m.preScale(-1, 1);

        Bitmap rotated_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        Bitmap mirrored_rotated_bitmap = Bitmap.createBitmap(rotated_bitmap, 0, 0, rotated_bitmap.getWidth(), rotated_bitmap.getHeight(), m, true);
        Bitmap reduced = SontHelper.reduceBitmapQuality(mirrored_rotated_bitmap, 100);
        Bitmap reduced2 = findFace(reduced, 10);

        CustomImageView customimg = findViewById(R.id.customimg);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        customimg.setBitmap(resize(reduced2, width, height));


        if (OpenCVLoader.initDebug()) {
            try {
                // region LOAD HAAR
                Log.d("open_cv", "init done");
                InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml"); // .xml
                FileOutputStream os = new FileOutputStream(mCascadeFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.close();

                InputStream is2 = getResources().openRawResource(R.raw.haarcascade_eye);
                File cascadeDir2 = getDir("cascade", Context.MODE_PRIVATE);
                File mCascadeFile2 = new File(cascadeDir2, "haarcascade_eye.xml"); // .xml
                FileOutputStream os2 = new FileOutputStream(mCascadeFile2);

                byte[] buffer2 = new byte[4096];
                int bytesRead2;
                while ((bytesRead2 = is2.read(buffer2)) != -1) {
                    os2.write(buffer2, 0, bytesRead2);
                }
                os2.close();
                // endregion

                CascadeClassifier faceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                CascadeClassifier eyeDetector = new CascadeClassifier(mCascadeFile2.getAbsolutePath());
                MatOfRect faceDetections = new MatOfRect();
                Mat mat = new Mat();
                Bitmap bmp32 = reduced2.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bmp32, mat);
                faceDetector.detectMultiScale(mat, faceDetections);
                java.util.List<org.opencv.core.MatOfRect> rectlista = new java.util.ArrayList<org.opencv.core.MatOfRect>();
                faceDetections.toList(rectlista);

                Rect region = new Rect();

                for (org.opencv.core.Rect rect : faceDetections.toArray()) {
                    Log.d("open_cv", "rect real: " + rect.toString());
                    Log.d("open_cv", "rect real x: " + rect.x);
                    Log.d("open_cv", "rect real y: " + rect.y);
                    Log.d("open_cv", "rect real h: " + rect.height);
                    Log.d("open_cv", "rect real w: " + rect.width);
                    Log.d("open_cv", "tempbitmap size: " + reduced.getWidth() + "," + reduced.getHeight());

                    region.left = rect.x;
                    region.top = rect.y;
                    region.bottom = rect.width;
                    region.right = rect.height;
                }

                Bitmap tempBitmap = reduced.copy(Bitmap.Config.RGB_565, true);
                Canvas tempCanvas = new Canvas(tempBitmap);
                Paint rectPaint = new Paint();
                rectPaint.setColor(Color.argb(100, 0, 0, 255));
                rectPaint.setStrokeWidth(10);

                tempCanvas.drawRect(region, rectPaint);
                tempCanvas.drawCircle(region.left, region.right, 50, rectPaint);

                customimg.setBitmap(resize(tempBitmap, width, height));

                Log.d("open_cv", "reclista: " + rectlista.size());
                Log.d("open_cv", "faceDetections: " + faceDetections.toArray().length);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d("open_cv", "init fail");
        }
    }

    public File copyHaar(String fileXML, int file) {
        try {
            InputStream is = getResources().openRawResource(file);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, fileXML); // .xml
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            return mCascadeFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Bitmap resize(Bitmap source, int w, int h) {

        float imageRatio = (float) source.getWidth() / (float) source.getHeight();

        int imageViewWidth = w;
        int imageRealHeight = (int) (imageViewWidth / imageRatio);

        Bitmap imageToShow = Bitmap.createScaledBitmap(source, imageViewWidth, imageRealHeight, true);
        return imageToShow;
    }

    public static Bitmap findFace(Bitmap bitmap, int maxfaces) {
        Bitmap tempBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
        Canvas tempCanvas = new Canvas(tempBitmap);
        Log.d("camera_api", "findface()");

        FaceDetector.Face[] faces = new FaceDetector.Face[maxfaces];
        FaceDetector fd = new FaceDetector(tempBitmap.getWidth(), tempBitmap.getHeight(), maxfaces);
        int facesfound = fd.findFaces(tempBitmap, faces);
        for (FaceDetector.Face f : faces) {
            try {
                PointF p = new PointF();
                f.getMidPoint(p);
                Log.d("camera_api", "faces found: " + facesfound + " w: " + p.x + " h: " + p.y);

                Paint.FontMetrics fm = new Paint.FontMetrics();
                Paint paint = new Paint();
                paint.setColor(Color.argb(100, 255, 0, 0));
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeWidth(15f);
                paint.setTextSize(40);

                Paint circlePaint = new Paint();
                circlePaint.setColor(Color.argb(100, 0, 255, 0));
                circlePaint.setStrokeWidth(10);

                paint.getFontMetrics(fm);

                tempCanvas.drawText(String.valueOf(f.confidence()),
                        tempBitmap.getWidth() / 2,
                        tempBitmap.getHeight() / 2 + -(fm.ascent + fm.descent) / 2, paint);
                tempCanvas.drawCircle(p.x, p.y, 200, circlePaint);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tempBitmap;
    }
}