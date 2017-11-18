package com.axlecho.sakura.utils;

import android.util.Log;

/**
 * Created by axlecho on 2017/11/18 0018.
 */

public class SakuraLogUtils {

    public static int d(String tag, String msg) {
        return Log.d(tag, msg);
    }

    public static int e(String tag, String msg) {
        return Log.e(tag, msg);
    }

    public static int w(String tag, String msg) {
        return Log.w(tag, msg);
    }

    public static int i(String tag, String msg) {
        return Log.i(tag, msg);
    }
}
