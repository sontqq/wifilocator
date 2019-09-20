package com.sontme.esp.getlocation.activities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.sontme.esp.getlocation.R;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.view.MapView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class mapsforge extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapsforge);

        AndroidGraphicFactory.createInstance(this.getApplication());

        MapView map = findViewById(R.id.forgeview);

        File f = new File(Environment.getExternalStorageDirectory() + File.separator + "maps/", "mapsforge.zip");

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            AndroidNetworking.download("https://openmaps.eu/dltosm.php?dl=hungary_openmaps_eu_europe.map.zip", f.getAbsolutePath(), f.getName())
                    .setTag("zip")
                    .setPriority(Priority.HIGH)
                    .build()
                    .startDownload(new DownloadListener() {
                        @Override
                        public void onDownloadComplete() {
                            File unzipped;

                            Log.d("mapsforge_zip", "download done:" + f.length() + "_" + f.getTotalSpace() + "_" + f.getFreeSpace() + "_" + f.getUsableSpace());
                            if (isValid(f)) {
                                Log.d("mapsforge_zip", "valid");
                            } else {
                                Log.d("mapsforge_zip", "not valid");
                            }
                            try {
                                ZipInputStream zis = new ZipInputStream(new FileInputStream(f.getPath() + File.separator + f.getName()));
                                ZipEntry temp = null;
                                while ((temp = zis.getNextEntry()) != null) {
                                    if (!temp.isDirectory()) {
                                        Path currentPath = Paths.get(temp.getName());
                                        Log.d("mapsforge_zip", "found: " + f.getPath());
                                        Log.d("mapsforge_zip", "found: " + f.getAbsolutePath());
                                        Log.d("mapsforge_zip", "found: " + f.getParent());
                                        Log.d("mapsforge_zip", "found: " + currentPath);
                                        Log.d("mapsforge_zip", "found: " + currentPath.getFileName());

                                        FileOutputStream fout =
                                                new FileOutputStream(f.getParent() + File.separator + currentPath.getFileName());

                                        byte[] buffer = new byte[8192];
                                        int len;
                                        while ((len = zis.read(buffer)) != -1) {
                                            fout.write(buffer, 0, len);
                                        }
                                        fout.close();
                                        unzipped = new File(f.getParent() + File.separator + currentPath.getFileName());


                                    }
                                }
                            } catch (Exception e) {
                                Log.d("mapsforge_zip", "error opening zip -> " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(ANError anError) {
                            Log.d("mapsforge_zip", "error");
                        }
                    });
        } else {
            Toast.makeText(getApplicationContext(), "Please connect to WiFi!", Toast.LENGTH_LONG).show();
        }

        final LatLong localLatLong = new LatLong(20.1, 47.2);
        TappableMarker positionmarker = new TappableMarker(R.drawable.pin1, localLatLong);

        map.getLayerManager().getLayers().add(positionmarker);
    }

    private class TappableMarker extends Marker {
        public TappableMarker(int icon, LatLong localLatLong) {
            super(localLatLong, AndroidGraphicFactory.convertToBitmap(getApplicationContext().getResources().getDrawable(icon)),
                    1 * (AndroidGraphicFactory.convertToBitmap(getApplicationContext().getResources().getDrawable(icon)).getWidth()) / 2,
                    -1 * (AndroidGraphicFactory.convertToBitmap(getApplicationContext().getResources().getDrawable(icon)).getHeight()) / 2);
        }
    }

    public static void unzip(String zipFile, String location) throws IOException {
        Log.d("mapsforge_zip", "zipping");
        Log.d("mapsforge_zip", "from: " + zipFile);
        Log.d("mapsforge_zip", "to: " + location);
        try {
            File f = new File(location);
            if (!f.isDirectory()) {
                f.mkdirs();
            }
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
            try {
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = location + ze.getName();

                    if (ze.isDirectory()) {
                        File unzipFile = new File(path);
                        if (!unzipFile.isDirectory()) {
                            unzipFile.mkdirs();
                        }
                    } else {
                        FileOutputStream fout = new FileOutputStream(path, false);
                        try {
                            for (int c = zin.read(); c != -1; c = zin.read()) {
                                fout.write(c);
                            }
                            zin.closeEntry();
                        } finally {
                            fout.close();
                        }
                    }
                }
            } finally {
                zin.close();
            }
        } catch (Exception e) {
            Log.d("mapsforge_zip", "Unzip exception", e);
        }
        Log.d("mapsforge_zip", "zipping finished");
    }

    public void extractFile(Path zipFile, String fileName, Path outputFile) throws IOException {
        try {
            FileSystem fileSystem = FileSystems.newFileSystem(zipFile, null);
            Path fileToExtract = fileSystem.getPath(fileName);
            Files.copy(fileToExtract, outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean isValid(final File file) {
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(file);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                    zipfile = null;
                }
            } catch (IOException e) {
            }
        }
    }
}
