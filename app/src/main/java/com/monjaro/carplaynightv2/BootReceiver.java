package com.monjaro.carplaynightv2;

import android.content.*;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        start(c);
        WatchdogReceiver.schedule(c);
    }

    static void start(Context c) {
        try {
            Intent s=new Intent(c,ThemeWatchService.class);
            if(Build.VERSION.SDK_INT>=26) c.startForegroundService(s);
            else c.startService(s);
        } catch(Throwable ignored) {
            try { c.startService(new Intent(c,ThemeWatchService.class)); } catch(Throwable ignored2) {}
        }
    }
}
