package com.axlecho.sakura.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by axlecho on 2017/11/18 0018.
 */

public class SakuraTextUtils {
    private static final String TAG = "text-utils";

    public static String search(String data, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            String found = matcher.group(1);
            SakuraLogUtils.d(TAG, found);
            return found;
        }
        return null;
    }

    public static String urlDecode(String url) {
        try {
            return URLDecoder.decode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            SakuraLogUtils.w(TAG, "decode failed " + url);
        }
        return null;
    }

    public static String generateTime(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ? String.format(Locale.CHINA, "%02d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.CHINA, "%02d:%02d", minutes, seconds);
    }
}
