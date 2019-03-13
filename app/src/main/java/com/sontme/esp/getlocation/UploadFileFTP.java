package com.sontme.esp.getlocation;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.sontme.esp.getlocation.activities.MainActivity;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class UploadFileFTP extends AsyncTask<String, Void, String> {

    private Context c;

    public UploadFileFTP(Context c) {
        this.c = c;
    }

    @Override
    public String doInBackground(String... strings) {
        uploadNow(strings);
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(c, "FTP Upload done: " + result, Toast.LENGTH_LONG).show();
    }

    public String uploadNow(String... strings) {

        Log.d("FTP", "STARTING FTP UPLOAD");
        long time = System.currentTimeMillis();
        FTPClient con = null;
        try {
            con = new FTPClient();

            con.setDefaultTimeout(5000);
            con.setDataTimeout(5000);
            con.setConnectTimeout(5000);
            con.setControlKeepAliveTimeout(5000);
            con.setControlKeepAliveReplyTimeout(5000);

            InetAddress address;
            address = InetAddress.getByName(strings[0]);
            con.connect(address);

            if (con.login("", "")) {
                con.enterLocalPassiveMode();
                con.setFileType(FTP.BINARY_FILE_TYPE);
                String data = "/storage/emulated/0/Documents/wifilocator_database.csv";

                FileInputStream in = new FileInputStream(new File(data));
                boolean result = con.storeFile("/var/www/uploads/wifi_database_" + time + "_" + BackgroundService.googleAccount + ".csv", in);
                in.close();
                if (result) {
                    Log.d("FTP UPLOAD", "SUCCESS");
                    File f = new File("/storage/emulated/0/Documents/wifilocator_database.csv");
                    f.delete();
                }
                con.logout();
                con.disconnect();

            }
        } catch (ConnectException e) {
            Log.d("FTP_RETRY_", e.toString());
            UploadFileFTP up = new UploadFileFTP(c);
            up.doInBackground("sont.sytes.net");
        } catch (SocketTimeoutException tim) {
            Log.d("FTP_RETRY_", tim.toString());
            UploadFileFTP up = new UploadFileFTP(c);
            up.doInBackground("sont.sytes.net");
        } catch (Exception e) {
            Log.d("FTP", e.toString());
            UploadFileFTP up = new UploadFileFTP(c);
            up.doInBackground("sont.sytes.net");
            Toast.makeText(c, "FTP Upload error: " + e.toString(), Toast.LENGTH_LONG).show();
        }
        return null;
    }
}
