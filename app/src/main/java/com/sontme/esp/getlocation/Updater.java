package com.sontme.esp.getlocation;

/*
public class Updater  {

    public void checkUpdate(final Context context, String url, final Activity a) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                String content = null;
                try {
                    content = new String(response, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.d("APP", e.toString());
                }
                content = content.replaceAll("[^0-9.]", "");
                int currentVersion = BuildConfig.VERSION_CODE;
                int remoteVersion = Integer.parseInt(content);
                Log.d("VERSION", "CURRENT VERSION: " + currentVersion);
                if (remoteVersion > currentVersion) {
                    Toast.makeText(context, "New version available!", Toast.LENGTH_LONG).show();
                    AlertDialog.Builder alert = new AlertDialog.Builder(a);
                    alert.setTitle("Do you want to update now?");
                    alert.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // DOWNLOAD AND INSTALL
                            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                downloadUpdate8_http(a);
                            } else {
                                downloadUpdate8_http(a);
                            }
                        }
                    });
                    alert.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            });
                    alert.show();

                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Toast.makeText(context, "Failed to retreive version information!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onRetry(int retryNo) {
            }
        });
    }

    public void downloadUpdate8_http(Activity a) {
        final File file = new File(Environment.getExternalStorageDirectory(), "tauri_queue_watcher.apk");
        final DownloadTask downloadTask = new DownloadTask(a,file,"Updating...");
        downloadTask.execute("https://sont.sytes.net/tauri_queue_watcher.apk");
    }
}

private Activity a;

public Updater(){

}

public class DownloadTask extends AsyncTask<String, Integer, String> {
    private ProgressDialog mPDialog;
    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private File mTargetFile;

    public DownloadTask(Context context,File targetFile,String dialogMessage) {
        this.mContext = context;
        this.mTargetFile = targetFile;
        mPDialog = new ProgressDialog(context);

        mPDialog.setMessage(dialogMessage);
        mPDialog.setIndeterminate(true);
        mPDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mPDialog.setCancelable(true);
        final DownloadTask me = this;
        mPDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                me.cancel(true);
            }
        });
        Log.i("DownloadTask","Constructor done");
    }

    @Override
    protected String doInBackground(String... sUrl) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(sUrl[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }
            Log.i("DownloadTask","Response " + connection.getResponseCode());

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();
            output = new FileOutputStream(mTargetFile,false);

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    Log.i("DownloadTask","Cancelled");
                    input.close();
                    return null;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) // only if total length is known
                    publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return null;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        mWakeLock.acquire();

        mPDialog.show();

    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        // if we get here, length is known, now set indeterminate to false
        mPDialog.setIndeterminate(false);
        mPDialog.setMax(100);
        mPDialog.setProgress(progress[0]);

    }

    @Override
    protected void onPostExecute(String result) {
        Log.i("DownloadTask", "Work Done! PostExecute");
        mWakeLock.release();
        mPDialog.dismiss();
        if (result != null)
            Toast.makeText(mContext,"Download error: "+result, Toast.LENGTH_LONG).show();
        else{
            installAPK();
        }
    }
    public void installAPK(Context context) {
        final File file = new File(Environment.getExternalStorageDirectory(), "tauri_queue_watcher.apk");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri downloaded_apk = getFileUri(context,file);
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(downloaded_apk,
                    "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ContextCompat.startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ContextCompat.startActivity(intent);
        }
    }

    public Uri getFileUri(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".com.sontme.esp.tauriqueuewatcher.provider", file);
    }
}
*/