package com.sontme.esp.getlocation;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.sontme.esp.getlocation.activities.MainActivity;


public class Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // start SERVICE on Boot
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, BackgroundService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
        // CATCH NOTIFICATION BUTTON PRESS
        String code = intent.getStringExtra("code");
        if (intent.getAction() == "exit") {
            Toast.makeText(context, "EXIT PRESSED", Toast.LENGTH_SHORT).show();
            Log.d("RECEIVER", "NOTIFICATION BUTTON PRESSED");
            cancelNotification(context, 0);
            context.stopService(intent);
            context.stopService(new Intent(context, BackgroundService.class));
            context.stopService(new Intent(context, MainActivity.class));
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(2);
        }
        if (intent.getAction() == "pause") {
            Global.wanarun = false;
            Toast.makeText(context, "Pausing", Toast.LENGTH_SHORT).show();
        }
        if (intent.getAction() == "resume") {
            Toast.makeText(context, "Resuming", Toast.LENGTH_SHORT).show();
            Global.wanarun = true;
        }
        if (intent.getAction() == "btn") {
            Toast.makeText(context,"NotifButtonPressed",Toast.LENGTH_SHORT).show();
            Log.d("NOTIF","NOTIFPRESSED");
        }
    }

    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancelAll();
        nMgr.cancel(notifyId);
    }
}
