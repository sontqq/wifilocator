package com.sontme.esp.getlocation;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.FaceDetector;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.ParcelUuid;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.net.util.Base64;
import org.opencv.core.Scalar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static android.util.Base64.NO_WRAP;
import static android.util.Base64.decode;
import static android.util.Base64.encodeToString;
import static org.apache.commons.net.util.Base64.encodeBase64String;

/*
   Frequently used methods to keep other classes clear
*/

public class SontHelper extends Application {

    public static class AudioTools {
        public static void play(short[] audio) {
            AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 8000 * 10, AudioTrack.MODE_STREAM);
            track.write(audio, 0, audio.length);
            track.stop();
            track.release();

        }

        public static void recordAndPlay(Context ctx) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            AudioRecord recorder = null;
            AudioTrack track = null;
            short[][] buffers = new short[256][160];
            int ix = 0;
            String audio = null;

            try {
                int N = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N * 10);
                track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N * 10, AudioTrack.MODE_STREAM);
                recorder.startRecording();
                track.play();
                while (true) {
                    short[] buffer = buffers[ix++ % buffers.length];
                    N = recorder.read(buffer, 0, buffer.length);
                    track.write(buffer, 0, buffer.length);
                    for (int i = 0; i < buffers.length; i++) {
                        for (int j = 0; j < buffers[i].length; j++) {
                            //Log.d("audio_stream","buffers _ " + buffers.length + " - > " + buffers[i][j]);
                            audio = audio + buffers[i][j];

                        }
                    }
                    play(buffer);
                    //if(audio.length() >= 8000)
                    //play(audio.toCharArray());
                    //Log.d("audio_stream","file size: " + mp3.length()/1024 + " kb");

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                recorder.stop();
                recorder.release();
                track.stop();
                track.release();

            }
        }

        public static void record_play(Activity ctx) {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(ctx, new String[]{Manifest.permission.RECORD_AUDIO}, 2);
            }
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.CAPTURE_AUDIO_OUTPUT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(ctx, new String[]{Manifest.permission.CAPTURE_AUDIO_OUTPUT}, 1);
            }
            Thread streamThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                        ParcelFileDescriptor[] descriptors = ParcelFileDescriptor.createPipe();
                        ParcelFileDescriptor parcelRead = new ParcelFileDescriptor(descriptors[0]);
                        ParcelFileDescriptor parcelWrite = new ParcelFileDescriptor(descriptors[1]);

                        InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelRead);

                        MediaRecorder recorder = new MediaRecorder();
                        // recorder.release();
                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        recorder.setOutputFile(parcelWrite.getFileDescriptor());
                        recorder.prepare();
                        recorder.start();

                        int read;
                        byte[] data = new byte[8000];
                        while ((read = inputStream.read(data, 0, data.length)) != -1) {
                            Log.d("audio_stream", "stream: " + read + " -> " + data.length / 1024 + " kb");
                            byteArrayOutputStream.write(data, 0, read);
                        }

                        byte[] sound = byteArrayOutputStream.toByteArray();

                        File path = new File(ctx.getCacheDir() + "/wifisound.mp3");

                        FileOutputStream fos = new FileOutputStream(path);
                        fos.write(sound);
                        fos.close();

                        MediaPlayer mediaPlayer = new MediaPlayer();

                        FileInputStream fis = new FileInputStream(path);
                        mediaPlayer.setDataSource(ctx.getCacheDir() + "/wifisound.mp3");

                        mediaPlayer.prepare();
                        mediaPlayer.start();

                        byteArrayOutputStream.flush();
                    } catch (Exception e) {
                        Log.d("audio_stream", "error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            streamThread.start();
        }

    }

    public static class ImageTools {
        /**
         * For STREAMING
         *
         * @param bitmap
         * @return
         */
        public static ByteArrayInputStream BitmapToByteArrayInputStream(Bitmap bitmap) {
            int byteSize = bitmap.getRowBytes() * bitmap.getHeight();
            ByteBuffer byteBuffer = ByteBuffer.allocate(byteSize);
            bitmap.copyPixelsToBuffer(byteBuffer);

            byte[] byteArray = byteBuffer.array();
            return new ByteArrayInputStream(byteArray);
        }

        public static byte[] BitmapToByteArrayInputStream_2(Bitmap bitmap) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return baos.toByteArray();
        }

        public static Bitmap byteArrayToBitmap(byte[] imagedata) {
            return BitmapFactory.decodeByteArray(imagedata, 0, imagedata.length);
        }
    }

    public static class Crypt {

        public final static String TOKEN_KEY = "fqJfdzGDvfwbedsKSUGty3VZ9taXxMVw";

        public static String encrypt(String plain) {
            try {
                byte[] iv = new byte[16];
                new SecureRandom().nextBytes(iv);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(TOKEN_KEY.getBytes(StandardCharsets.UTF_8), "AES"), new IvParameterSpec(iv));
                byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
                byte[] ivAndCipherText = getCombinedArray(iv, cipherText);
                return encodeToString(ivAndCipherText, NO_WRAP);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static String decrypt(String encoded) {
            try {
                byte[] ivAndCipherText = decode(encoded, NO_WRAP);
                byte[] iv = Arrays.copyOfRange(ivAndCipherText, 0, 16);
                byte[] cipherText = Arrays.copyOfRange(ivAndCipherText, 16, ivAndCipherText.length);

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(TOKEN_KEY.getBytes(StandardCharsets.UTF_8), "AES"), new IvParameterSpec(iv));
                return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private static byte[] getCombinedArray(byte[] one, byte[] two) {
            byte[] combined = new byte[one.length + two.length];
            for (int i = 0; i < combined.length; ++i) {
                combined[i] = i < one.length ? one[i] : two[i - one.length];
            }
            return combined;
        }

    }

    public static class BluetoothFunctions {
        public static boolean isBLDevicePaired(BluetoothDevice device) {
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> list = ba.getBondedDevices();
            Log.d("BLUETOOTH_LIBRARY_", "Paired count: " + list.size());
            for (BluetoothDevice dev : list) {
                return device.getAddress() == dev.getAddress();
            }
            return false;
        }

        public static Set<BluetoothDevice> getPaired() {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            return mBluetoothAdapter.getBondedDevices();
        }

        public static void native_BL() {
            BluetoothSocket socket = null;
            try {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                //bluetoothAdapter.startDiscovery();
                bluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        try {
                            Log.d("BL_NA", "onLeScan -> name: " + device.getName() + "_ uuid: " + device.getUuids()[0] + "_ rssi: " + rssi + "_ len: " + scanRecord.length);
                        } catch (Exception e) {
                        }
                    }
                });
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    //Log.d("BL_NATIVE", "debug0_" + device.getName() + " - " + device.getAddress());
                    Method getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
                    ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(bluetoothAdapter, null);

                    //socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                    //socket.connect();
                    //InputStream is = socket.getInputStream();
                    //OutputStream os = socket.getOutputStream();
                    //String debug = null;
                    //String a = IOUtils.toString(is);
                    //Log.d("BL_NATIVE", "debug1_" + debug);
                    //Log.d("BL_NATIVE", "debug2_" + a);
                }
            } catch (Exception e) {
                Log.d("BL_NATIVE", "error " + e.getMessage());
                try {
                    socket.close();
                    native_BL();
                } catch (Exception e2) {
                    Log.d("BL_NATIVE", "error_2 " + e.getMessage());
                    e2.printStackTrace();
                }
                e.printStackTrace();
            }
        }

    }

    public static byte[] compress_GZIP(String str) {
        ByteArrayOutputStream obj;
        try {
            if (str == null || str.length() == 0) {
                return null;
            }
            Log.d("Compress_", "output str length: " + str.length());
            obj = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(obj);
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
            gzip.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return obj.toByteArray();
    }

    public static String decompress_GZIP(byte[] str) throws Exception {
        if (str == null) {
            return null;
        }
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(str));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
        String outStr = "";
        String line;
        while ((line = bf.readLine()) != null) {
            outStr += line;
        }
        Log.d("Compress_", "output str length: " + outStr.length());
        return outStr;
    }

    public static String decompress_GZIP_string(String str) throws Exception {
        if (str == null) {
            return null;
        }
        //byte[] strr = str.getBytes();
        byte[] strr = Base64.decodeBase64(str);
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(strr));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8));
        String outStr = "";
        String line;
        while ((line = bf.readLine()) != null) {
            outStr += line;
        }
        Log.d("Compress_", "output str length: " + outStr.length());
        return outStr;
    }

    public static String byteArrayToString(byte[] barr) {
        return encodeBase64String(barr);
        //return new String(barr);
    }

    public static boolean isWifiConnected(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    public static boolean isWifiConnecting(Context ctx) {
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mMob = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean connecting;
        return (!mWifi.isConnected()) && (mWifi.isConnectedOrConnecting());
    }

    public static void requestPermissions(Activity act) {
        ActivityCompat.requestPermissions(act,
                new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_PRIVILEGED,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, 1);
    }

    static String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStorageDirectory().toString()
            + "/DCIM/Camera";
    static String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    public static String getDomainFromURL(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public static boolean isValidURL(String url) {
        Pattern p = Pattern.compile("^(https?:\\/\\/)?([\\w\\Q$-_+!*'(),%\\E]+\\.)+(\\w{2,63})(:\\d{1,4})?([\\w\\Q/$-_+!*'(),%\\E]+\\.?[\\w])*\\/?$");
        Matcher m = p.matcher(url);
        return m.find();

    }

    public static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase().hashCode());
    }

    public static List<String> getCameraImages(Context context) {
        final String[] projection = {MediaStore.Images.Media.DATA};
        final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        final String[] selectionArgs = {CAMERA_IMAGE_BUCKET_ID};
        final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);
        ArrayList<String> result = new ArrayList<String>(cursor.getCount());
        if (cursor.moveToFirst()) {
            final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            do {
                final String data = cursor.getString(dataColumn);
                result.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    public static List<String> getGallery(Context ctx) {
        List<String> list = new ArrayList<String>();
        list = getCameraImages(ctx);
        long allsize = 0;
        for (String s : list) {
            File f = new File(s);
            allsize = allsize + f.length();
        }
        return list;
    }

    public static void playTone() {
        Thread thread = new Thread() {
            public void run() {
                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
            }
        };
        thread.start();
    }

    public static void vibrate(Context ctx) {
        Thread thread = new Thread() {
            public void run() {
                Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(40, -1));
                } else {
                    v.vibrate(40);
                }
            }
        };
        thread.start();
    }

    public static void vibrate(Context ctx, int amplitude, int time) {
        Thread thread = new Thread() {
            public void run() {
                Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(time, amplitude));
                } else {
                    v.vibrate(50);
                }
            }
        };
        thread.start();
    }

    public static void double_vibrate(Context ctx) {
        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {50, 0, 50, 0};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, 1));
        } else {
            v.vibrate(40);
            v.vibrate(VibrationEffect.createWaveform(pattern, 1));
        }
    }

    public static boolean zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;

        File sourceFile = new File(sourcePath);
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {
                zipSubFolder(out, sourceFile, sourceFile.getParent().length());
            } else {
                byte[] data = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourcePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
                entry.setTime(sourceFile.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
            out.close();
        } catch (Exception e) {
            //e.printStackTrace();
            Log.d("ZIP_", "DONE_error" + e.getMessage());
            return false;
        }
        Log.d("ZIP_", "DONE");
        return true;
    }

    private static void zipSubFolder(ZipOutputStream out, File folder,
                                     int basePathLength) throws IOException {

        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte[] data = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                entry.setTime(file.lastModified()); // to keep modification time after unzipping
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    public static String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        return lastPathComponent;
    }

    public static int roundFloat(float f) {
        int c = (int) ((f) + 0.5f);
        float n = f + 0.5f;
        return (n - c) % 2 == 0 ? (int) f : c;
    }

    public static int convertDBM(int dbm) {
        int quality;
        if (dbm <= -100)
            quality = 0;
        else if (dbm >= -50)
            quality = 100;
        else
            quality = 2 * (dbm + 100);
        return quality;
    }

    public static int invertColor(int color) {
        return color ^ 0x00ffffff;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static double mpsTokmh(double mps) {
        return mps * 3.6;
    }

    public static String convertTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");
        return format.format(date);
    }

    public static double getDistance(double lat1, double lat2, double lon1, double lon2) {

        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        double el1 = 0;
        double el2 = 0;
        double height = el1 - el2;
        distance = Math.pow(distance, 2) + Math.pow(height, 2);
        return Math.sqrt(distance);
    }

    public static String getCompleteAddressString(Context ctx, double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder();

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("");
                }
                strAdd = strReturnedAddress.toString();
            } else {
            }
        } catch (Exception e) {
            Log.d("Error_", e.toString());
        }
        return strAdd;
    }

    public static void showToast(Context ctx, String text) {
        if (ctx == null) {
            ctx = ctx;
        }
        String id = Settings.Secure.getString(ctx.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        if (id.equals("73bedfbd149e01de")) {
            Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
        }

    }

    public static String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (isIPv4)
                            return sAddr;
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    public static String chk_3g_wifi(Context ctx) {
        final ConnectivityManager connMgr = (ConnectivityManager)
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi.isConnectedOrConnecting()) {
            return "wifi";
        } else if (mobile.isConnectedOrConnecting()) {
            return "3g";
        } else {
            return "no";
        }
    }

    public static Bitmap reduceBitmapQuality(Bitmap bitmap, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //bitmap.compress(Bitmap.CompressFormat.PNG, quality, out);
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        Bitmap decoded = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
        return decoded;
    }

    public static boolean check_if_local(Context ctx) {
        Log.d("LAN_", String.valueOf(System.currentTimeMillis()));
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = info.getSSID();
        if (ssid.contains("UPCAED")) {
            Log.d("LAN_", String.valueOf(System.currentTimeMillis()));
            return true;
        } else {
            Log.d("LAN_", String.valueOf(System.currentTimeMillis()));
            return false;
        }
    }

    public static boolean isNetworkAvailable(Context c) {
        Log.d("NETWORK_", "_" + System.currentTimeMillis());
        ConnectivityManager connectivityManager
                = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        Log.d("NETWORK_", "_" + System.currentTimeMillis());
        return activeNetworkInfo != null &&
                activeNetworkInfo.isConnected() &&
                activeNetworkInfo.isConnectedOrConnecting();
    }

    /**
     * WARNING !! NOT WORKING YET ! UNUSED
     *
     * @param c
     * @return
     */
    public static boolean isNetworkAvailable_2(Context c) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    // NEED TO FIX RETURN VALUE (0)
    public static int stepCounter(Context c) {
        final int[] count = {0};
        SensorManager sensorManager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Log.d("STEP_COUNTER_", "STEP: " + sensor.toString());
        SensorEventListener sel = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                count[0] = (int) event.values[0];
                Log.d("STEP_COUNTER_", "CHANGE: " + "[" + event.values.length + "] " + event.values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(sel, sensor, 1000);
        return count[0];
    }

    public static boolean isBatteryCharging(Context context) {
        // Check battery sticky broadcast
        final Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return (batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING);
    }

    public static String getCurrentWifiName(Context c) {
        WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        return info.getSSID();
    }

    public static void getUsbDevices(Context c) {
        UsbManager manager = (UsbManager) c.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        Log.d("USB_LIST", "dev list_ " + deviceList.size());
        if (deviceList.size() >= 1) {
            //Toast.makeText(c,"USB device found: " + deviceList.size(),Toast.LENGTH_LONG).show();
        }
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            Toast.makeText(c, "USB Found: " + device.toString(), Toast.LENGTH_LONG).show();
            Log.d("TEST_", "device_" + device.toString());
        }
        //return deviceList;
    }

    public static Bitmap getScreenBitmap(Context c, View view) {

        view.setDrawingCacheEnabled(true);
        view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        view.buildDrawingCache();

        if (view.getDrawingCache() == null) {
            return null;
        }

        Bitmap snapshot = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        view.destroyDrawingCache();

        return snapshot;
    }

    public static Scalar argbtoScalar(int r, int g, int b, int a) {
        Scalar s = new Scalar(r, b, g, a);
        return s;
    }

    public static Bitmap convertViewToBitmap(View v) {
        Bitmap b = Bitmap.createBitmap(v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }

    public static Bitmap resize(Bitmap source, int w, int h) {

        float imageRatio = (float) source.getWidth() / (float) source.getHeight();

        int imageViewWidth = w;
        int imageRealHeight = (int) (imageViewWidth / imageRatio);

        Bitmap imageToShow = Bitmap.createScaledBitmap(source, imageViewWidth, imageRealHeight, true);
        return imageToShow;
    }

    public static void turnGPSOn(Context ctx) {
        String provider = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!provider.contains("gps")) { //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            ctx.sendBroadcast(poke);
        }
    }

    public static void openLocationSettings(Context ctx) {
        Intent intent = new Intent(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        ctx.startActivity(intent);
    }

    public static boolean isLocationServicesEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }

    public static void requestCameraPermission(Context ctx) {
        if (ctx.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) ctx, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    public static boolean checkLowWifiSignalStr(Context ctx) {
        // ha a jelenleg kapcsolodott wifi jelerosseg NEM kisebb mint x% VAGY NEM KAPCSOLODOTT WIFIHEZ akkor -> TRUE
        // tehát mehet a hálózati forgalom
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        if (mWifi.isConnected()) {
            int conn_quality = SontHelper.convertDBM(wifiManager.getConnectionInfo().getRssi());
            return conn_quality >= 30;
        }
        return true;
    }

    public static ArrayList<String> getAllImagesPath(Activity activity) {
        Uri uri;
        ArrayList<String> listOfAllImages = new ArrayList<String>();
        Cursor cursor;
        int column_index_data, column_index_folder_name;
        String PathOfImage = null;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

        cursor = activity.getContentResolver().query(uri, projection, null,
                null, null);

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        column_index_folder_name = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
        while (cursor.moveToNext()) {
            PathOfImage = cursor.getString(column_index_data);

            listOfAllImages.add(PathOfImage);
        }
        return listOfAllImages;
    }

    public Bitmap cropBitmapRect(Bitmap bmp, Rect r) {
        return Bitmap.createBitmap(bmp, r.left, r.right, r.width(), r.height());
    }

    public static Bitmap findFaceDrawRectROI(Bitmap bitmap, int maxfaces) {
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

    public static Bitmap getCircledBitmap(Bitmap bitmap, PointF point) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(point.x, point.y, bitmap.getWidth() / 3, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static Bitmap findFaceCropROI(Bitmap bitmap, int maxfaces) {
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
                tempBitmap = SontHelper.getCircledBitmap(tempBitmap, p);
                Log.d("camera_api", "faces found: " + facesfound + " w: " + p.x + " h: " + p.y);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return tempBitmap;
    }

    public static void pickImage(Activity act) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        act.startActivityForResult(photoPickerIntent, 999);
    }

    public static void wifi_check_enabled(Context ctx) {
        // TURN ON WIFI
        WifiManager wifi = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
            Toast.makeText(ctx, "Turning on WiFi", Toast.LENGTH_SHORT).show();
            wifi.setWifiEnabled(true);
        }
    }

    public static void adminPermission_check(Context ctx, Activity a) {
        DevicePolicyManager mDPM = null;
        ComponentName mAdminName = null;
        try {
            if (!mDPM.isAdminActive(mAdminName)) {
                try {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "extrainfo");
                    a.startActivityForResult(intent, 0);
                } catch (Exception e) {
                    Log.d("Error_setting_admin_permission_", e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.d("Error_", e.getMessage());
            e.printStackTrace();
        }
    }

    public static String generateGFX(List<Location> points) {
        String name = "gfxfile.gfx";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), name);
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n";
        name = "<name>" + name + "</name><trkseg>\n";

        String segments = "";
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        for (Location location : points) {
            segments += "<trkpt lat=\"" + location.getLatitude() + "\" lon=\"" + location.getLongitude() + "\"><time>" + df.format(new Date(location.getTime())) + "</time></trkpt>\n";
        }

        String footer = "</trkseg></trk></gpx>";

        try {
            FileWriter writer = new FileWriter(file, false);
            writer.append(header);
            writer.append(name);
            writer.append(segments);
            writer.append(footer);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file.getAbsolutePath();
    }

    public static String generateKML(List<Location> points) {
        String fullKML = "";
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n <kml xmlns=\"http://earth.google.com/kml/2.2\">\n";
        String segments = "";
        String footer = "</kml>";

        for (Location location : points) {
            segments += "<Placemark>\n" +
                    "<name>" + "asd" + "</name>\n" +
                    "<description>desc</description>\n" +
                    "<Point>\n" +
                    "<coordinates>" + location.getLatitude() + "," + location.getLongitude() + "</coordinates>\n" +
                    "</Placemark>\n";
        }

        fullKML = header + segments + footer;
        File file = null;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "wifiloc.kml");
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            StringBuilder sb = new StringBuilder();
            sb.append(fullKML);
            writer.append(sb.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            Log.d("KML_location", file.toString());
        }
        return file.getAbsolutePath();
    }

    public static List<NeighboringCellInfo> getCellTowers(Context ctx) {
        // Please note that this information may not available on your device.
        // Most of the Samsung devices does not support this method.
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        List<NeighboringCellInfo> neighbors = tm.getNeighboringCellInfo();
        return neighbors;
    }
}
