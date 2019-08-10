package com.sontme.esp.getlocation;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import static org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import static org.opencv.android.CameraBridgeViewBase.GONE;
import static org.opencv.android.CameraBridgeViewBase.OnTouchListener;

public class opencv_realtime extends AppCompatActivity implements CvCameraViewListener2 {

    CameraBridgeViewBase mOpenCvCameraView;
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    MatOfRect faces;

    Mat grayscaleImage;
    int absoluteFaceSize;
    int absoluteEyeSize;

    CascadeClassifier cascadeClassifier_Face;
    CascadeClassifier cascadeClassifier_Eye;
    CascadeClassifier LBPcascadeClassifier_Face;

    int mCameraId = 1;

    ImageView histogram;
    Bitmap histogram_temp;

    private void swapCamera() {
        //mCameraId = mCameraId ^ 1; //bitwise not
        if (mOpenCvCameraView.getCameraIndex() == 0) {
            mOpenCvCameraView.setCameraIndex(1);
            mOpenCvCameraView.forgat = false;
            mOpenCvCameraView.setForgat(false);
        } else {
            mOpenCvCameraView.setCameraIndex(0);
            mOpenCvCameraView.forgat = true;
            mOpenCvCameraView.setForgat(true);
        }
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.enableView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_opencv_realtime);

        initOpenCVDependencies();

        faces = new MatOfRect();

        mOpenCvCameraView = findViewById(R.id.show_camera_activity_java_surface_view);
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                swapCamera();
                return false;
            }
        });

        histogram = findViewById(R.id.opencv_histogram);
        histogram.setVisibility(GONE);

    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i("open_cv", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("open_cv", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("open_cv", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);

        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

        absoluteFaceSize = (int) (height * 0.2); // screen 20%
        absoluteEyeSize = (int) (height * 0.02); // screen 2%
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Size s = new Size();
        s.height = 500;
        s.width = 500;

        Imgproc.resize(mRgba, mRgba, s);
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0, 0, 0);
        Core.flip(mRgbaF, mRgba, -1);

        Imgproc.cvtColor(mRgba, grayscaleImage, Imgproc.COLOR_RGBA2RGB);


        if (cascadeClassifier_Face != null) {
            cascadeClassifier_Face.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            Rect[] facesArray = faces.toArray();

            for (int i = 0; i < facesArray.length; i++) {
                Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), SontHelper.argbtoScalar(0, 0, 255, 0), 3);
            }
            if (facesArray.length >= 1) {
                Log.d("open_cv", "face detected !!");
            }
        }

        if (cascadeClassifier_Eye != null) {
            cascadeClassifier_Eye.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteEyeSize, absoluteEyeSize), new Size());
            Rect[] facesArray = faces.toArray();

            for (int i = 0; i < facesArray.length; i++) {
                Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), SontHelper.argbtoScalar(255, 0, 0, 0), 3);
            }
            if (facesArray.length >= 1) {
                Log.d("open_cv", "eye detected !!");
            }
        }

        if (LBPcascadeClassifier_Face != null) {
            LBPcascadeClassifier_Face.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            Rect[] facesArray = faces.toArray();

            for (int i = 0; i < facesArray.length; i++) {
                Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), SontHelper.argbtoScalar(0, 255, 0, 0), 3);
            }
            if (facesArray.length >= 1) {
                Log.d("open_cv", "LBP face detected !!");
            }
        }
        if (histogram.getVisibility() != GONE) {
            try {
                if (mRgba != null) {
                    histogram_temp = Bitmap.createBitmap(mRgba.width(), mRgba.height(), Bitmap.Config.RGB_565);
                    Utils.matToBitmap(mRgba, histogram_temp);
                    if (histogram_temp != null) {
                        Bitmap b = SontHelper.resize(histogram_temp, histogram_temp.getWidth() / 3, histogram_temp.getHeight() / 3);
                        histogram.setImageBitmap(createHistogram(b));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mRgba;
    }

    public void initOpenCVDependencies() {
        List<String> cascades = new ArrayList<String>();
        cascades.add("lbpcascade_frontalface_improved.xml");
        //cascades.add("haarcascade_frontalface_default.xml");
        cascades.add("haarcascade_eye.xml");

        for (String cascade : cascades) {
            String[] casc = cascade.split(".xml");
            int resId = -5;
            try {
                resId = getResources().getIdentifier(casc[0], "raw", getPackageName());
                InputStream is = getResources().openRawResource(resId);
                File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                File mCascadeFile = new File(cascadeDir, casc[0] + ".xml"); // .xml
                FileOutputStream os = new FileOutputStream(mCascadeFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.close();

                if (cascade.compareToIgnoreCase("haarcascade_frontalface_default.xml") != 0) {
                    cascadeClassifier_Face = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    Log.d("open_cv_2", "HAAR_Face_init_done");
                }
                if (cascade.compareToIgnoreCase("haarcascade_eye.xml") != 0) {
                    cascadeClassifier_Eye = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    Log.d("open_cv_2", "HAAR_Eye_init_done");
                }

                if (cascade.compareToIgnoreCase("lbpcascade_frontalface_improved.xml") != 0) {
                    LBPcascadeClassifier_Face = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    Log.d("open_cv_2", "LBP_Face_init_done");
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.d("open_cv_2", "fail1 - " + casc[0] + " --> " + cascade);
            }
        }


    }

    public Bitmap createHistogram(Bitmap bitmap) {
        Mat sourceMat = new Mat();
        Utils.bitmapToMat(bitmap, sourceMat);

        Size sourceSize = sourceMat.size();

        int histogramSize = 256;
        MatOfInt hisSize = new MatOfInt(histogramSize);

        List<Mat> channels = new ArrayList<>();

        MatOfFloat range = new MatOfFloat(0f, 255f);
        MatOfFloat histRange = new MatOfFloat(range);

        Core.split(sourceMat, channels);

        MatOfInt[] allChannel = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};
        Scalar[] colorScalar = new Scalar[]{new Scalar(220, 0, 0, 255), new Scalar(0, 220, 0, 255), new Scalar(0, 0, 220, 255)};

        Mat matB = new Mat(sourceSize, sourceMat.type());
        Mat matG = new Mat(sourceSize, sourceMat.type());
        Mat matR = new Mat(sourceSize, sourceMat.type());

        Imgproc.calcHist(channels, allChannel[0], new Mat(), matB, hisSize, histRange);
        Imgproc.calcHist(channels, allChannel[1], new Mat(), matG, hisSize, histRange);
        Imgproc.calcHist(channels, allChannel[2], new Mat(), matR, hisSize, histRange);


        int graphHeight = 300;
        int graphWidth = 200;
        int binWidth = 3;

        Mat graphMat = new Mat(graphHeight, graphWidth, CvType.CV_8UC3, new Scalar(0, 0, 0));

        //Normalize channel
        Core.normalize(matB, matB, graphMat.height(), 0, Core.NORM_INF);
        Core.normalize(matG, matG, graphMat.height(), 0, Core.NORM_INF);
        Core.normalize(matR, matR, graphMat.height(), 0, Core.NORM_INF);

        //convert pixel value to point and draw line with points
        for (int i = 0; i < histogramSize; i++) {
            Point bPoint1 = new Point(binWidth * (i - 1), graphHeight - Math.round(matB.get(i - 1, 0)[0]));
            Point bPoint2 = new Point(binWidth * i, graphHeight - Math.round(matB.get(i, 0)[0]));
            Imgproc.line(graphMat, bPoint1, bPoint2, new Scalar(220, 0, 0, 255), 3, 8, 0);

            Point gPoint1 = new Point(binWidth * (i - 1), graphHeight - Math.round(matG.get(i - 1, 0)[0]));
            Point gPoint2 = new Point(binWidth * i, graphHeight - Math.round(matG.get(i, 0)[0]));
            Imgproc.line(graphMat, gPoint1, gPoint2, new Scalar(0, 220, 0, 255), 3, 8, 0);

            Point rPoint1 = new Point(binWidth * (i - 1), graphHeight - Math.round(matR.get(i - 1, 0)[0]));
            Point rPoint2 = new Point(binWidth * i, graphHeight - Math.round(matR.get(i, 0)[0]));
            Imgproc.line(graphMat, rPoint1, rPoint2, new Scalar(0, 0, 220, 255), 3, 8, 0);
        }

        //convert Mat to bitmap
        Bitmap graphBitmap = Bitmap.createBitmap(graphMat.cols(), graphMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(graphMat, graphBitmap);

        // show histogram
        return graphBitmap;
    }

}
