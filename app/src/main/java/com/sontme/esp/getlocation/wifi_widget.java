package com.sontme.esp.getlocation;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.widget.RemoteViews;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class wifi_widget extends AppWidgetProvider {
    private static String REFRESH_ACTION = "android.appwidget.action.APPWIDGET_UPDATE";
    private static CountDownTimer timer;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equalsIgnoreCase("android.appwidget.action.APPWIDGET_UPDATE")) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews remoteViews;
            ComponentName watchWidget;
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.wifi_widget);
            watchWidget = new ComponentName(context, wifi_widget.class);
            String s;
            s = "Count : " + BackgroundService.getCount() + " Nearby: " + BackgroundService.nearbyCount + " Unique: " + BackgroundService.uniqueAPS.size();
            remoteViews.setTextViewText(R.id.widget_txt1, s);
            remoteViews.setTextViewText(R.id.widget_txt2, BackgroundService.time);
            appWidgetManager.updateAppWidget(watchWidget, remoteViews);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wifi_widget);

        timer = new CountDownTimer(5000, 20) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                RemoteViews remoteViews;
                ComponentName watchWidget;
                remoteViews = new RemoteViews(context.getPackageName(), R.layout.wifi_widget);
                watchWidget = new ComponentName(context, wifi_widget.class);
                String s;
                String b = "0.0";
                if (BackgroundService.distance != null) {
                    b = BackgroundService.distance;
                }
                String[] distance = b.split("\\.");
                s = distance[0] + " meters / " + BackgroundService.getLongitude() + " / " + BackgroundService.getLatitude();
                Long current = System.currentTimeMillis();

                try {
                    if (BackgroundService.time != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd. hh:mm:ss");
                        Date convertedDate = new Date();
                        convertedDate = dateFormat.parse(BackgroundService.time);
                        Timestamp t = new Timestamp(convertedDate.getTime());
                        long tl = t.getTime();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                remoteViews.setTextViewText(R.id.widget_txt1, s);
                remoteViews.setTextViewText(R.id.widget_txt2, BackgroundService.time);
                appWidgetManager.updateAppWidget(watchWidget, remoteViews);

                timer.start();
            }
        }.start();

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);

            RemoteViews remoteViews;
            ComponentName watchWidget;

            remoteViews = new RemoteViews(context.getPackageName(), R.layout.wifi_widget);
            watchWidget = new ComponentName(context, wifi_widget.class);

            remoteViews.setOnClickPendingIntent(R.id.widget_btn1,
                    getPendingSelfIntent(context, "android.appwidget.action.APPWIDGET_UPDATE"));
            appWidgetManager.updateAppWidget(watchWidget, remoteViews);

        }
    }
    @Override
    public void onEnabled(Context context) {
    }
    @Override
    public void onDisabled(Context context) {
    }
}

