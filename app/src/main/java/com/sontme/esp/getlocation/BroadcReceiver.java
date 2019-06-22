package com.sontme.esp.getlocation;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.sontme.esp.getlocation.activities.MainActivity;


public class BroadcReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // start SERVICE on Boot
        if (intent.getStringExtra("alarm") == "run") {
            Intent i = new Intent();
            i.putExtra("alarm", "run");
            context.sendBroadcast(i);
            Log.d("ALARM_RECEIVER_", "ON");

        } else if (intent.getStringExtra("alarm") == "off") {
            Intent i = new Intent();
            i.putExtra("alarm", "off");
            context.sendBroadcast(i);
            Log.d("ALARM_RECEIVER_", "OFF");
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, BackgroundService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
        // CATCH NOTIFICATION BUTTON PRESS
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
            Toast.makeText(context, "Pausing", Toast.LENGTH_SHORT).show();
        }
        if (intent.getAction() == "resume") {
            Toast.makeText(context, "Resuming", Toast.LENGTH_SHORT).show();
        }
        if (intent.getAction() == "btn") {
            Toast.makeText(context,"NotifButtonPressed",Toast.LENGTH_SHORT).show();
        }

        if (intent.getAction() == "widgetButton") {
            Toast.makeText(context, "WDGT", Toast.LENGTH_SHORT).show();
        }
    }

    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancelAll();
        nMgr.cancel(notifyId);
    }
}
