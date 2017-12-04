package com.axlecho.sakura.utils;

import android.content.Context;

import com.danikula.videocache.HttpProxyCacheServer;

/**
 * Created by axlecho on 2017/11/21 0021.
 */

public class SakuraHttpProxyCacheServerManager {
    private static SakuraHttpProxyCacheServerManager instance;
    private HttpProxyCacheServer proxy;

    public static SakuraHttpProxyCacheServerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SakuraHttpProxyCacheServerManager.class) {
                if (instance == null) {
                    instance = new SakuraHttpProxyCacheServerManager(context);
                }
            }
        }
        return instance;
    }

    private SakuraHttpProxyCacheServerManager(Context context) {
        proxy = new HttpProxyCacheServer(context);
    }

    public HttpProxyCacheServer getProxy() {
        return proxy;
    }
}
