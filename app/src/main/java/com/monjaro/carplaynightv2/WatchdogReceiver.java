package com.monjaro.carplaynightv2;

import android.app.*;
import android.content.*;
import android.os.*;

public class WatchdogReceiver extends BroadcastReceiver {
    private static final int REQ=31337;

    @Override public void onReceive(Context c, Intent i) {
        BootReceiver.start(c);
        schedule(c);
    }

    public static void schedule(Context c) {
        try {
            AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi=pending(c);
            long first=System.currentTimeMillis()+5*60*1000L;
            am.cancel(pi);
            am.setRepeating(AlarmManager.RTC_WAKEUP, first, 5*60*1000L, pi);
        } catch(Throwable ignored) {}
    }

    public static void scheduleQuickRestart(Context c) {
        try {
            AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi=pending(c);
            long when=System.currentTimeMillis()+15*1000L;
            if(Build.VERSION.SDK_INT>=23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
            else am.set(AlarmManager.RTC_WAKEUP,when,pi);
        } catch(Throwable ignored) {}
    }

    private static PendingIntent pending(Context c) {
        Intent i=new Intent(c,WatchdogReceiver.class);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23) flags|=PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c,REQ,i,flags);
    }
}
