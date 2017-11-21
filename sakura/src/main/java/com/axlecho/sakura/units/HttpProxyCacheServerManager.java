package com.axlecho.sakura.units;

import android.content.Context;

import com.danikula.videocache.HttpProxyCacheServer;

/**
 * Created by axlecho on 2017/11/21 0021.
 */

public class HttpProxyCacheServerManager {
    private static HttpProxyCacheServerManager instance;
    private HttpProxyCacheServer proxy;

    public static HttpProxyCacheServerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HttpProxyCacheServerManager.class) {
                if (instance == null) {
                    instance = new HttpProxyCacheServerManager(context);
                }
            }
        }
        return instance;
    }

    private HttpProxyCacheServerManager(Context context) {
        proxy = new HttpProxyCacheServer(context);
    }

    public HttpProxyCacheServer getProxy() {
        return proxy;
    }
}
