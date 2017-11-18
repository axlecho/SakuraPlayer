package com.axlecho.sakura.videoparser;

import com.axlecho.sakura.videoparser.extractor.BilibiliExtractor;
import com.axlecho.sakura.videoparser.extractor.Extractor;

/**
 * Created by axlecho on 2017/11/18 0018.
 */

public class SakuraParser {

    private static SakuraParser instance;
    private Extractor extractor;

    private SakuraParser() {
        this.extractor = new BilibiliExtractor();
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
        return extractor.get(pageUrl);
    }
}
