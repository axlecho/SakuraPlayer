package com.axlecho.sakura.videoparser;

import com.axlecho.sakura.videoparser.extractor.BaseExtractors;
import com.axlecho.sakura.videoparser.extractor.BilibiliExecutor;
import com.axlecho.sakura.videoparser.extractor.YoukuExtractor;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by axlecho on 2017/11/18 0018.
 */

public class SakuraParser {

    private static SakuraParser instance;
    private BaseExtractors extractor;

    private SakuraParser() {
    }

    public static SakuraParser getInstance() {
        if (instance == null) {
            synchronized (SakuraParser.class) {
                if (instance == null) {
                    instance = new SakuraParser();
                }
            }
        }
        return instance;
    }

    public String getStreamUrl(String pageUrl) {
        this.extractor = resolveExecutor(pageUrl);
        if (extractor == null) {
            // not surpport ye
            return pageUrl;
        }
        return extractor.get(pageUrl);
    }

    private BaseExtractors resolveExecutor(String pageUrl) {
        try {
            URL url = new URL(pageUrl);
            if (url.getHost().contains(BilibiliExecutor.NAME.toLowerCase())) {
                return new BilibiliExecutor();
            } else if (url.getHost().contains(YoukuExtractor.NAME.toLowerCase())) {
                return new YoukuExtractor();
            }
        } catch (MalformedURLException e) {

        }

        return null;
    }
}
