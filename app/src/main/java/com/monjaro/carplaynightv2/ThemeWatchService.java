package com.monjaro.carplaynightv2;

import android.app.*;
import android.content.*;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;

public class ThemeWatchService extends Service {
    private static final int NOTIF_ID = 3107;
    private static final String CHANNEL_ID = "monjaro_night_guard";
    private ContentObserver observer;
    private int lastRaw = Integer.MIN_VALUE;
    private PowerManager.WakeLock wakeLock;

    @Override public void onCreate() {
        super.onCreate();
        promoteToForeground();

        try {
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MonjaroNight:ThemeWatch");
            wakeLock.setReferenceCounted(false);
        } catch(Throwable ignored) {}

        Handler h = new Handler(Looper.getMainLooper());
        observer = new ContentObserver(h) {
            @Override public void onChange(boolean self, Uri uri) { sync(); }
        };
        getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, observer);

        WatchdogReceiver.schedule(this);
        sync();
    }

    private void promoteToForeground() {
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel ch=new NotificationChannel(
                    CHANNEL_ID, "Monjaro CarPlay Night",
                    NotificationManager.IMPORTANCE_MIN);
            ch.setDescription("Фоновое переключение дневного/ночного режима CarPlay");
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }

        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(
                this,0,open,
                Build.VERSION.SDK_INT>=23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification.Builder b = Build.VERSION.SDK_INT>=26
                ? new Notification.Builder(this,CHANNEL_ID)
                : new Notification.Builder(this);

        Notification n=b
                .setContentTitle("Monjaro CarPlay Night V3")
                .setContentText("Автопереключение день/ночь активно")
                .setSmallIcon(android.R.drawable.ic_menu_day)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pi)
                .build();

        startForeground(NOTIF_ID,n);
    }

    private void sync() {
        if (!Prefs.enabled(this)) return;
        try {
            if(wakeLock!=null && !wakeLock.isHeld()) wakeLock.acquire(5000);
        } catch(Throwable ignored) {}

        int raw = Settings.System.getInt(
                getContentResolver(), "realThemeDaynightMode", -1);

        if (raw < 0 || raw == lastRaw) return;
        lastRaw = raw;

        boolean night = raw == 1;
        if (Prefs.invert(this)) night = !night;
        UsbNightController.setNightMode(this, night);
    }

    @Override public int onStartCommand(Intent i, int flags, int id) {
        promoteToForeground();
        WatchdogReceiver.schedule(this);
        sync();
        return START_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        WatchdogReceiver.scheduleQuickRestart(this);
        super.onTaskRemoved(rootIntent);
    }

    @Override public android.os.IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        try {
            if (observer != null) getContentResolver().unregisterContentObserver(observer);
        } catch(Throwable ignored) {}
        try {
            if(wakeLock!=null && wakeLock.isHeld()) wakeLock.release();
        } catch(Throwable ignored) {}
        WatchdogReceiver.scheduleQuickRestart(this);
        super.onDestroy();
    }
}
