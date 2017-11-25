package com.axlecho.sakura.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
}
