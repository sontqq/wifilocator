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

public class CsvExporter {

    public String fileName;
    public List<String> csv_list_uniq = new ArrayList<String>();

    public CsvExporter(String fileName) {
        this.fileName = fileName;
    }

    public void writeCsv(String text) throws IOException {
        Thread thread = new Thread() {
            public void run() {
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
                            Log.d("CSV_EXPORTER_", "Already contains");
                        }
                    } catch (Exception e) {
                        Log.d("CSV_EXPORTER_ERROR_", e.toString());
                        e.printStackTrace();
                    } finally {
                        Log.d("CSV_EXPORTER_", "LEFUTOTT_" + file.length() / 1024 + " kilobytes");
                    }
                }
            }
        };
        thread.start();
    }

    public void writeCsv_huawei(String text) {
        Thread thread = new Thread() {
            public void run() {
                if (BackgroundService.isUploading == false) {
                    File file = null;
                    try {
                        if (csv_list_uniq.contains(text) == false) {
                            file = new File("/data/user/0/com.sontme.esp.getlocation/files/", fileName);
                            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                            StringBuilder sb = new StringBuilder();
                            sb.append(text);
                            sb.append('\n');
                            writer.append(sb.toString());
                            writer.flush();
                            writer.close();
                        } else {
                            Log.d("CSV_EXPORTER_HUAW", "Already contains");
                        }
                    } catch (Exception e) {
                        Log.d("CSV_EXPORTER_ERROR_HUAW_", e.toString());
                        e.printStackTrace();
                    } finally {
                        Log.d("CSV_EXPORTER_HUAW", "LEFUTOTT_" + file.length() / 1024 + " kilobytes");
                    }
                }
            }
        };
        thread.start();
    }


}

