package com.sontme.esp.getlocation;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class UploadFileHTTP extends AsyncTask<String, Void, String> {

    private Context c;

    public UploadFileHTTP(Context c) {
        this.c = c;
    }

    public void uploadNow(String... strings) {
        try {
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
                    //conn.setRequestProperty("source", Global.googleAccount);
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
                        Toast.makeText(c, "HTTP Response: " + serverResponseMessage, Toast.LENGTH_LONG).show();
                        File f = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
                        f.delete();
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
            }
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
        Log.d("HTTP_UPLOAD_", "done");
    }

    @Override
    protected void onPreExecute() {
        Global.isUploading = true;
        zipFileAtPath("/storage/emulated/0/Documents/wifilocator_database.csv", "/storage/emulated/0/Documents/wifilocator_database.zip");
        Log.d("HTTP_UPLOAD_", "started");
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        //Toast.makeText(c,"Progress: "+values.toString(),Toast.LENGTH_SHORT).show();
        //Log.d("HTTP_UPLOAD_progress_",values.toString());
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
            e.printStackTrace();
            Log.d("ZIP_", "DONE_error");
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