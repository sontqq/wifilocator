package com.sontme.esp.getlocation;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadFileHTTP extends AsyncTask<String, Void, String> {

    private Context c;

    public UploadFileHTTP(Context c) {
        this.c = c;
    }

    public void uploadNow(String... strings) {
        try {
            String sourceFileUri = "/storage/emulated/0/Documents/wifilocator_database.csv";
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File sourceFile = new File(sourceFileUri);

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
                    //conn.setRequestProperty("bill", "/asd.txt");
                    dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    //dos.writeBytes("Content-Disposition: form-data; name=\"bill\";filename=\""
                    //       + sourceFileUri + "\"" + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                            + sourceFileUri + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);
                    // create a buffer of maximum size
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];
                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math
                                .min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0,
                                bufferSize);

                    }
                    // send multipart form data necesssary after file
                    // data...
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens
                            + lineEnd);
                    // Responses from the server (code and message)
                    int serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn
                            .getResponseMessage();
                    if (serverResponseCode == 200) {
                        // DELETE file on complete
                        Toast.makeText(c, "HTTP Response: " + serverResponseMessage, Toast.LENGTH_LONG).show();
                        File f = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
                        f.delete();
                        if (f.exists()) {
                            f.getCanonicalFile().delete();
                        }
                        if (f.exists()) {
                            c.deleteFile(f.getName());
                        } else {
                            Log.d("DELETE_", "File is removed");
                        }
                    }
                    Log.d("HTTP_complete_", "HTTP Response: " + serverResponseMessage);
                    // close the streams //
                    fileInputStream.close();
                    dos.flush();
                    dos.close();
                } catch (Exception e) {
                    //new UploadFileHTTP(c).execute("https://sont.sytes.net/upload.php?");
                    Log.d("HTTP_UPLOAD_", e.toString());
                }
                // dialog.dismiss();
            } // End else block
        } catch (Exception ex) {
            // dialog.dismiss();
            Log.d("HTTP_UPLOAD_2_", ex.toString());
            //new UploadFileHTTP(c).execute("https://sont.sytes.net/upload.php?");
        }
    }

    @Override
    protected String doInBackground(String... params) {
        uploadNow(params);
        return "Executed";
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(c, "HTTP Upload done: " + result, Toast.LENGTH_LONG).show();
        Global.isUploading = false;
    }

    @Override
    protected void onPreExecute() {
        Global.isUploading = true;
        Log.d("HTTP_UPLOAD_", "started");
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        //Toast.makeText(c,"Progress: "+values.toString(),Toast.LENGTH_SHORT).show();
        //Log.d("HTTP_UPLOAD_progress_",values.toString());
    }
}