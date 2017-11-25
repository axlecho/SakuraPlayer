package com.axlecho.sakura.videoparser.extractor;

import com.axlecho.sakura.utils.SakuraLogUtils;
import com.axlecho.sakura.utils.SakuraNetworkUtils;
import com.axlecho.sakura.utils.SakuraTextUtils;

import java.io.IOException;

import okhttp3.Headers;

/**
 * Created by axlecho on 2017/11/25 0025.
 */

public class YoukuExtractor extends Extractor {
    public static final String NAME = "Youku";
    private static final String TAG = NAME.toLowerCase();
    private static final String mobile_ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36";
    private static final String referer = "http://v.youku.com";

    private String url;
    private String vid;
    private String utid;
    private String ccode = "0502";

    @Override
    public String get(String pageUrl) {
        this.url = pageUrl;
        try {
            this.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String fetch_cna() throws IOException {
        Headers headers = SakuraNetworkUtils.getInstance().getForHeaders("http://log.mmstat.com/eg.js");
        String cookies = headers.get("set-cookie");
        SakuraLogUtils.d(TAG, cookies);
        for (String cookie : cookies.split(";")) {
            if (cookie.startsWith("cna")) {
                return SakuraTextUtils.urlDecode(cookie.split("=")[1]);
            }
        }
        return SakuraTextUtils.urlDecode("DOG4EdW4qzsCAbZyXbU+t7Jt");
    }

    private void youku_ups() throws IOException {
        String ts = String.valueOf(System.currentTimeMillis());
        url = String.format("https://ups.youku.com/ups/get.json?vid=%1$s&ccode=%2$s", vid, ccode);
        url += "&client_ip=192.168.1.1";
        url += "&utid=" + utid;
        url += "&client_ts=" + ts;

        Headers.Builder headersBuilder = new Headers.Builder();
        headersBuilder.add("Referer", referer);
        headersBuilder.add("User-Agent", mobile_ua);

        String json = SakuraNetworkUtils.getInstance().get(url, headersBuilder.build());
    }

    private String getVidFromUrl(String url) {
        String b64p = "([a-zA-Z0-9=]+)";
        String[] p_list = {"youku\\.com/v_show/id_" + b64p,
                "player\\.youku\\.com/player\\.php/sid/'+b64p+r'/v\\.swf",
                "loader\\.swf\\?VideoIDS=" + b64p,
                "player\\.youku\\.com/embed/" + b64p};
        for (String p : p_list) {
            vid = SakuraTextUtils.search(url, p);
            if (vid != null) {
                return vid;
            }
        }

        return vid;
    }

    private void prepare() throws IOException {
        this.vid = this.getVidFromUrl(url);
        this.utid = fetch_cna();
        this.youku_ups();
    }
}
