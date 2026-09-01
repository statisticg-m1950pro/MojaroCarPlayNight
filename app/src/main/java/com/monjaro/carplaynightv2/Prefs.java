package com.monjaro.carplaynightv2;
import android.content.Context;

public final class Prefs {
    private static final String P = "prefs";
    static boolean enabled(Context c) {
        return c.getSharedPreferences(P,0).getBoolean("enabled", true);
    }
    static boolean invert(Context c) {
        return c.getSharedPreferences(P,0).getBoolean("invert", false);
    }
    static void setEnabled(Context c, boolean v) {
        c.getSharedPreferences(P,0).edit().putBoolean("enabled", v).apply();
    }
    static void setInvert(Context c, boolean v) {
        c.getSharedPreferences(P,0).edit().putBoolean("invert", v).apply();
    }
}
