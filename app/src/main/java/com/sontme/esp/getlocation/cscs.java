package com.sontme.esp.getlocation;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.common.util.IOUtils;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class cscs {

    private String fileName;

    public void uploadFile(String path, Context ctx) {
        //new UploadFileFTP().execute("sont.sytes.net");
    }

    public cscs(String fileName) {
        this.fileName = fileName;
    }

    public void writeCsv(String text) {
        if (Global.isUploading == false) {
            File file = null;
            try {
                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                StringBuilder sb = new StringBuilder();
                sb.append(text);
                sb.append('\n');
                writer.append(sb.toString());
                writer.flush();
                writer.close();
            } catch (Exception e) {
                Log.d("CSV_writer_error_:", e.toString());
            } finally {
                //Log.d("CSV_SIZE_", String.valueOf(file.length() / 1024) + " kb");
            }
        }
    }
}

