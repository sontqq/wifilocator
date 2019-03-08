package com.sontme.esp.getlocation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class UploadFileHTTP extends AsyncTask<String, Integer, String> {

    private Context c;
    private Activity a;
    //private ProgressDialog dialog;
    CustomDialog di;

    public UploadFileHTTP(Activity a) {
        this.a = a;
        //this.a = a;
        //dialog = new ProgressDialog(c);
        //this.di = new CustomDialog(a);
    }

    public void uploadNow(String... strings) {
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
            int maxBufferSize = 1 * 1024 * 1024;
            File sourceFile = new File(sourceFileUri);
            //int contentLength = conn.getContentLength();
            if (sourceFile.isFile()) {
                try {

                    String upLoadServerUri = strings[0];
                    // open a URL connection to the Servlet
                    FileInputStream fileInputStream = new FileInputStream(
                            sourceFile);
                    URL url = new URL(upLoadServerUri);
                    // Open a HTTP connection to the URL
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setDoInput(true); // Allow Inputs
                    //conn.setUseCaches(false);
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
                    long length = f2.length();
                    //Log.d("bytes_",String.valueOf(bytesRead));
                    //int i = 0;
                    while (bytesRead > 0) {
                        long left = (length / Long.valueOf(bytesRead)) * 100; // szimulálni
                        Log.d("PROGRESS_inner_", String.valueOf((int) (length)));
                        Log.d("PROGRESS_inner_", String.valueOf((int) (bytesRead)));
                        Log.d("PROGRESS_inner_", String.valueOf((int) (left)));
                        //int x = (bytesRead/contentLength)*100;
                        //Log.wtf("progress","asd"+String.valueOf(bytesRead));
                        //Log.wtf("progress","asdd"+String.valueOf(contentLength));
                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math
                                .min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0,
                                bufferSize);

                        publishProgress((int) (left));
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
                        FileChannel outChan = new FileOutputStream(f, true).getChannel();
                        outChan.truncate(0);
                        outChan.close();
                        c.deleteFile("/storage/emulated/0/Documents/wifilocator_database.csv");
                        if (f.exists()) {
                            f.getCanonicalFile().delete();
                        }
                        if (f.exists()) {
                            c.deleteFile(f.getName());
                        }
                        if (f.exists()) {
                            f.getAbsoluteFile().delete();
                        } else {
                            Log.d("DELETE_", "File is removed or couldnt remove");
                        }
                    }
                    if (serverResponseMessage.contains("uploaded") || serverResponseMessage.equals("uploaded")) {
                        Log.d("HTTP UPLOAD SUCCESFUL !!! _", serverResponseMessage);
                    }
                    Log.d("HTTP_complete_", "HTTP Response: " + serverResponseMessage);
                    // close the streams //
                    fileInputStream.close();
                    dos.flush();
                    dos.close();
                } catch (Exception e) {
                    //new UploadFileHTTP(c).execute("https://sont.sytes.net/upload.php?");
                    //Toast.makeText(c,"Upload probably failed: " + e.getMessage(),Toast.LENGTH_LONG).show();
                    Log.d("HTTP_UPLOAD_FAIL_", e.toString());
                }
                // dialog.dismiss();
            }
        } catch (Exception ex) {
            // dialog.dismiss();
            Log.d("HTTP_UPLOAD_2_FAIL_", ex.toString());
            //new UploadFileHTTP(c).execute("https://sont.sytes.net/upload.php?");
//            Toast.makeText(c,"Upload failed: " + ex.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected String doInBackground(String... params) {
        uploadNow(params);
        return "Executed";
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(a, "HTTP Upload complete", Toast.LENGTH_LONG).show();
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
        /*
        dialog.setMax(100);
        dialog.setMessage("Uploading, please wait.");
        dialog.show();
        */
        zipFileAtPath("/storage/emulated/0/Documents/wifilocator_database.csv", "/storage/emulated/0/Documents/wifilocator_database.zip");
        Log.d("HTTP_UPLOAD_", "started");
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        Log.d("PROGRESS: ", "Update: " + values.toString());
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
}

class CustomDialog extends Dialog implements android.view.View.OnClickListener {

    public Activity c;
    public Dialog d;
    ProgressBar pb;


    public CustomDialog(Activity a) {
        super(a);
        this.c = a;
        pb = findViewById(R.id.upload_progressbar);
    }

    public void setProgressVal(int i) {
        pb = findViewById(R.id.upload_progressbar);
        pb.setProgress(i);
        Log.d("progress_", "Progress: " + String.valueOf(i));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog);

        pb = findViewById(R.id.upload_progressbar);
        pb.setMax(100);
        pb.setProgress(0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                // dismiss();
                break;
        }
        dismiss();
    }
}