package com.axlecho.sakura.utils;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by axlecho on 2017/11/18 0018.
 */

public class SakuraNetworkUtils {
    private static final String TAG = "network";
    private OkHttpClient client;

    private SakuraNetworkUtils() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
    }

    private static SakuraNetworkUtils instance;

    public static SakuraNetworkUtils getInstance() {
        if (instance == null) {
            synchronized (SakuraNetworkUtils.class) {
                if (instance == null) {
                    instance = new SakuraNetworkUtils();
                }
            }
        }
        return instance;
    }

    public Headers buildFakeHeaders() {
        return new Headers.Builder()
                .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .add("Accept-Charset", "UTF-8,*;q=0.5")
                .add("Accept-Encoding", "gzip,deflate,sdch")
                .add("Accept-Language", "en-US,en;q=0.8")
                .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0")
                .build();

    }


    public String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .headers(this.buildFakeHeaders())
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }


}
