package com.sontme.esp.getlocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.akexorcist.roundcornerprogressbar.TextRoundCornerProgressBar;
import com.sontme.esp.getlocation.activities.MainActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class UploadFileHTTP extends AsyncTask<String, Integer, String> {

    private Context c;
    private Activity a;
    CustomDialog di;

    public UploadFileHTTP(Activity a) {
        this.a = a;
    }

    public void uploadNow(String... strings) {
        if (haveNetworkConnection() == true) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentT = dateFormat.format(new Date());
                Long tsLong = System.currentTimeMillis();
                String ts = tsLong.toString();

                File f1 = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
                File f2 = new File("/storage/emulated/0/Documents/wifilocator_database.zip");
                String sourceFileUri;
                if (f2.exists()) {
                    sourceFileUri = "/storage/emulated/0/Documents/wifilocator_database.zip";
                } else {
                    sourceFileUri = "/storage/emulated/0/Documents/wifilocator_database.csv";
                }

                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                //int maxBufferSize = 10 * 1024 * 1024;
                int maxBufferSize = 2;
                File sourceFile = new File(sourceFileUri);
                if (sourceFile.isFile()) {
                    try {
                        String upLoadServerUri = strings[0];
                        FileInputStream fileInputStream = new FileInputStream(
                                sourceFile);
                        URL url = new URL(upLoadServerUri);
                        // Open a HTTP connection to the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE",
                                "multipart/form-data");
                        conn.setRequestProperty("Content-Type",
                                "multipart/form-data;boundary=" + boundary);
                        //conn.setRequestProperty("bill", sourceFileUri);
                        conn.setRequestProperty("uploaded_file", sourceFileUri);
                        conn.setRequestProperty("source", Global.googleAccount);
                        conn.setRequestProperty("date", currentT);

                        dos = new DataOutputStream(conn.getOutputStream());
                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                + sourceFileUri + "\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        // create a buffer of maximum size
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];
                        // read file and write it into form...
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        int length = (int) (f2.length());
                        int ossz = 0;
                        //Log.d("bytes_",String.valueOf(bytesRead));
                        //int i = 0;
                        Log.d("UPLOADING_", "Size: " + length + " bytes");
                        while (bytesRead > 0) {
                            Log.d("BYTESREAD_1_", String.valueOf(String.valueOf(bytesRead).length()));
                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math
                                    .min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0,
                                    bufferSize);
                            ossz = ossz + (bytesRead);
                            int percent = (int) (ossz * 100 / length + 0.5);
                            int maradt = length - ossz;
                            if (maradt > 1024 * 1024) {
                                di.setLeft(String.valueOf((length - ossz) / 1024 / 1024) + " / " + length / 1024 / 1024 + " MB");
                            } else {
                                di.setLeft(String.valueOf((length - ossz) / 1024) + " / " + length / 1024 + " KB");
                            }
                            if (Math.round(maradt) == 0) {
                                publishProgress(100);
                            } else {
                                publishProgress(percent);
                            }
                            Log.d("BYTESREAD_2_", String.valueOf(String.valueOf(bytesRead).length()));
                        }
                        // send multipart form data necesssary after file data
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens
                                + lineEnd);
                        // Responses from the server (code and message)
                        int serverResponseCode = conn.getResponseCode();
                        String serverResponseMessage = conn
                                .getResponseMessage();
                        if (serverResponseCode == 200) {
                            // DELETE file on complete
                            File f = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
                            f.delete();
                        /*FileChannel outChan = new FileOutputStream(f, true).getChannel();
                        outChan.truncate(0);
                        outChan.close();*/
                            c.deleteFile("/storage/emulated/0/Documents/wifilocator_database.csv");
                            if (f.exists()) {
                                f.getCanonicalFile().delete();
                            }
                            if (f.exists()) {
                                c.deleteFile(f.getName());
                            }
                            if (f.exists()) {
                                f.getAbsoluteFile().delete();
                            }
                            if (f.exists()) {
                                Log.d("DELETE_", "Could not delete file");
                            } else {
                                Log.d("DELETE_", "File is removed or couldnt remove");
                            }
                        }
                        if (serverResponseMessage.contains("uploaded") || serverResponseMessage.equals("uploaded")) {
                            Log.d("HTTP_UPLOAD_SUCCESFUL !!! _", serverResponseMessage);
                        }
                        Log.d("HTTP_complete_", "HTTP Response: " + serverResponseMessage);
                        // close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();
                        publishProgress(100);
                    } catch (Exception e) {
                        Log.d("HTTP_UPLOAD_FAIL_", e.toString());
                        try {
                            Toast.makeText(a, "Error: " + e.toString(), Toast.LENGTH_LONG).show();
                        } catch (Exception ex) {
                        }
                    }
                }
            } catch (Exception ex) {
                // dialog.dismiss();
                Log.d("HTTP_UPLOAD_2_FAIL_", ex.toString());
                //new UploadFileHTTP(c).execute("https://sont.sytes.net/upload.php?");
//            Toast.makeText(c,"Upload failed: " + ex.getMessage(),Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected String doInBackground(String... params) {
        if (haveNetworkConnection() == true) {
            uploadNow(params);
        } else {
            try {
                Toast.makeText(a, "No internet connection", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
            }
        }
        return "Executed";
    }

    @Override
    protected void onPostExecute(String result) {
        //Toast.makeText(a, "HTTP Upload complete", Toast.LENGTH_LONG).show();
        di.setProgressVal(100);
        try {
            TextView t = a.findViewById(R.id.currentstatus);
            t.setText("Done");
        } catch (Exception e) {
        }
        Global.isUploading = false;
        Log.d("HTTP_UPLOAD_", "done");
        //dialog.dismiss();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        di = new CustomDialog(a);
        di.show();

        Global.isUploading = true;
        zipFileAtPath("/storage/emulated/0/Documents/wifilocator_database.csv", "/storage/emulated/0/Documents/wifilocator_database.zip");
        Log.d("HTTP_UPLOAD_", "started");
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        //dialog.setProgress(Integer.valueOf(values.toString()));
        di.setProgressVal(values[0]);
        for (int i : values) {
            Log.d("Progress: _ ", String.valueOf(i));
        }

    }

    public boolean zipFileAtPath(String sourcePath, String toLocation) {
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
                byte data[] = new byte[BUFFER];
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

    private void zipSubFolder(ZipOutputStream out, File folder,
                              int basePathLength) throws IOException {

        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte data[] = new byte[BUFFER];
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

    public String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        return lastPathComponent;
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;
        ConnectivityManager cm;
        try {
            cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        } catch (Exception e) {
            cm = (ConnectivityManager) a.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}

class CustomDialog extends Dialog implements android.view.View.OnClickListener {

    public Activity c;
    public Dialog d;
    TextRoundCornerProgressBar newpb;
    Button inter;
    TextView t;
    String myColors[] = {"#00adb5", "#eaffd0", "#dcedc2", "#f38181", "#95e1d3", "#f857b5", "#f781bc", "#fdffdc", "#c5ecbe", "#00b8a9", "#f8f3d4", "#f6416c", "#ffde7d", "#7effdb", "#b693fe", "#8c82fc", "#ff9de2", "#a8e6cf", "#dcedc1", "#ffd3b6", "#ffaaa5", "#fc5185", "#384259"};

    public CustomDialog(Activity a) {
        super(a);
        this.c = a;
    }

    public void setProgressVal(int i) {
        newpb = findViewById(R.id.newprogressbar);
        String x = i + " %";
        newpb.setProgressText(x);
        newpb.setTextProgressColor(Color.WHITE);
        inter = findViewById(R.id.interrupt);
        //t = findViewById(R.id.txt_percentage);
        TextView cur = findViewById(R.id.currentstatus);
        Log.d("INTEGER_", String.valueOf(i));
        if (i >= 99) {
            cur.setText("Done!");
            cur.setTypeface(null, Typeface.BOLD);
            inter.setText("Close");
        }
        //t.setText(i + " %");
        newpb.setProgress((float) (i));
    }

    public void setLeft(String str) {
        TextView left = findViewById(R.id.currentstatus_left);
        left.setText(str);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog);
        Collections.shuffle(Arrays.asList(myColors));
        newpb = findViewById(R.id.newprogressbar);
        inter = findViewById(R.id.interrupt);
        t = findViewById(R.id.currentstatus);
        newpb.setProgressColor(Color.parseColor(myColors[0]));
        newpb.setRadius(40);
        //newpb.setProgress(0);
        inter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    cancel();
                    d.cancel();
                } catch (Exception e) {
                }
            }
        });

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                // dismiss();
                break;
        }
        // dismiss();
    }
}