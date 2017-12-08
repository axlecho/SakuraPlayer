package com.axlecho.sakura.utils;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by axlecho on 2017/12/8 0008.
 */
public class SakuraDeviceUtils {

    private static final String TAG = "device-utils";
    private static final long MAX_LIGHT_TIME = 1000 * 60 * 60 * 3; // 3 hours
    private static SakuraDeviceUtils instance;
    private PowerManager.WakeLock wakeLock;

    private SakuraDeviceUtils(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            SakuraLogUtils.e(TAG, "cound not access to power service");
            return;
        }
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "sakura-player");
        wakeLock.setReferenceCounted(false);
    }

    public static SakuraDeviceUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (SakuraDeviceUtils.class) {
                if (instance == null) {
                    instance = new SakuraDeviceUtils(context);
                }
            }
        }
        return instance;
    }

    public void keekLight() {
        if (wakeLock != null) {
            wakeLock.acquire(MAX_LIGHT_TIME);
        }
    }

    public void release() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}