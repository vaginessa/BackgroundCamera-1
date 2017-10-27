package com.nepdeveloper.backgroundcamera.Utility;

public class Log {
    private static final boolean debuggable = false;

    public static void i(String tag, String msg) {
        if (debuggable) {
            android.util.Log.i(tag, msg);
        }
    }

    public static void v(String tag, String msg) {
        if (debuggable) {
            android.util.Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (debuggable) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (debuggable) {
            android.util.Log.e(tag, msg);
        }
    }

    public static void wtf(String tag, String msg) {
        if (debuggable) {
            android.util.Log.wtf(tag, msg);
        }
    }
}
