package com.sontme.esp.getlocation;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class exporter {

    public String fileName;
    public List<String> csv_list_uniq = new ArrayList<String>();

    public exporter(String fileName) {
        this.fileName = fileName;
    }

    public void writeCsv(String text) throws IOException {
        if (BackgroundService.isUploading == false) {
            File file = null;
            try {
                if (csv_list_uniq.contains(text) == false) {
                    file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
                    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                    StringBuilder sb = new StringBuilder();
                    sb.append(text);
                    sb.append('\n');
                    writer.append(sb.toString());
                    writer.flush();
                    writer.close();
                } else {
                    Log.d("CSV_WRITER_", "Already contains");
                }
            } catch (Exception e) {
                Log.d("CSV_writer_error_:", e.toString());
            } finally {
                Log.d("csv_", "LEFUTOTT");
            }
        }
    }

    /*
    public void writeCsv_huawei(Context context, String text) throws IOException {
        if (BackgroundService.isUploading == false) {
            try {
                if (csv_list_uniq.contains(text) == false) {
                    ContextWrapper cw = new ContextWrapper(context);
                    // path to /data/data/yourapp/app_data/PrivateFiles
                    File directory = cw.getDir("database", Context.MODE_PRIVATE);
                    File file = new File(directory, fileName);
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        fileOutputStream.write(text.getBytes());
                        fileOutputStream.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d("CSV_WRITER_", "Already contains");
                }
            } catch (Exception e) {
                Log.d("CSV_writer_error_:", e.toString());
            } finally {

            }
        }
    }
    */
}

