package com.axlecho.sakura.videoparser.extractor;

import com.axlecho.sakura.utils.SakuraEncryptUtils;
import com.axlecho.sakura.utils.SakuraLogUtils;
import com.axlecho.sakura.utils.SakuraNetworkUtils;
import com.axlecho.sakura.utils.SakuraTextUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by axlecho on 2017/11/18 0018.
 */

public class BilibiliExtractor extends Extractor {
    public static final String NAME = "Bilibili";

    private static final String TAG = NAME.toLowerCase();
    private static final String LIVE_API = "http://live.bilibili.com/api/playurl?cid={}&otype=json";
    private static final String API_URL = "http://interface.bilibili.com/playurl?";
    private static final String BANGUMI_API_URL = "http://bangumi.bilibili.com/player/web_api/playurl?";

    private static final String SEC1 = "1c15888dc316e05a15fdd0a02ed6584f";
    private static final String SEC2 = "9b288147e5474dd2aa67085f716c560d";

    private static final List<String> STREAM_TYPES = new ArrayList<>();

    static {
        STREAM_TYPES.add("hdflv");
        STREAM_TYPES.add("flv");
        STREAM_TYPES.add("hdmp4");
        STREAM_TYPES.add("mp4");
        STREAM_TYPES.add("live");
        STREAM_TYPES.add("vc");
    }


    private String url;
    private String page;
    private String title;
    private String realUrl;

    @Override
    public String get(String pageUrl) {
        this.url = pageUrl;
        try {
            this.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return realUrl;
    }

    public String apiReq(String cid, String quality, boolean bangumi, boolean bangumiMovie) throws IOException {
        String ts = String.valueOf(System.currentTimeMillis());
        String paramsStr = String.format("cid=%s&player=1&quality=%s&ts=%s", cid, quality, ts);
        String chkSum = SakuraEncryptUtils.md5sum(paramsStr + SEC1);
        String apiUrl = API_URL + paramsStr + "&sign=" + chkSum;
        String xmlStr = SakuraNetworkUtils.getInstance().get(apiUrl);
        return xmlStr;
    }

    private void prepare() throws IOException {
        if (url.contains("watchlater")) {
            String aid = SakuraTextUtils.search(url, "av(\\d+)");
            url = String.format("http://www.bilibili.com/video/av%s/", aid);
        }

        page = SakuraNetworkUtils.getInstance().get(url);

        title = SakuraTextUtils.search(page, "<h1\\s*title=\"([^\"]+)\"");
        if (title == null) {
            title = SakuraTextUtils.search(page, "<meta property=\"og:title\" content=\"([^\"]+)\">");
        }

        if (url.contains("bangumi.bilibili.com/movie")) {
            this.movieEntry();
        } else if (url.contains("bangumi.bilibili.com")) {
            this.bangumiEntry();
        } else if (url.contains("live.bilibili.com")) {
            this.liveEntry();
        } else if (url.contains("vc.bilibili.com")) {
            this.vcEntry();
        } else {
            this.entry();
        }
    }

    private void movieEntry() {

    }

    private void bangumiEntry() {

    }

    private void liveEntry() {

    }

    private void vcEntry() {

    }

    private void entry() throws IOException {
        // tencent player
        String tcFlashVars = SakuraTextUtils.search(page, "\"bili-cid=\\d+&bili-aid=\\d+&vid=([^\"]+)\"");
        if (tcFlashVars != null) {
            // TODO apply tencent source
            SakuraLogUtils.w(TAG, "unsupported tencent source here yet!!");
            return;
        }

        // bilibili player
        String cid = SakuraTextUtils.search(page, "cid=(\\d+)");
        if (cid != null) {
            this.downloadByVid(cid, false);
        } else {
            // other player
            String flashVars = SakuraTextUtils.search(page, "flashvars=\"([^\"]+)\"");
            if (flashVars == null) {
                SakuraLogUtils.e(TAG, "unsupported page " + url);
                return;
            }

            SakuraLogUtils.w(TAG, "unsupported other source yet!!");
        }
    }

    private void downloadByVid(String cid, boolean bangumi) throws IOException {
        String quality = bangumi ? STREAM_TYPES.get(0) : STREAM_TYPES.get(1);
        String apiXml = apiReq(cid, quality, bangumi, false);
        try {
            this.parseBiliXml(apiXml);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void parseBiliXml(String apiXml) throws ParserConfigurationException, IOException, SAXException {
        SakuraLogUtils.d(TAG, apiXml);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(apiXml)));
        NodeList durls = document.getElementsByTagName("durl");


        for (int i = 0; i < durls.getLength(); ++i) {
            Element durl = (Element) durls.item(i);
            String size = durl.getElementsByTagName("size").item(0).getTextContent();
            String url = durl.getElementsByTagName("url").item(0).getTextContent();

            SakuraLogUtils.d(TAG, "size " + size + " url " + url);
            this.realUrl = url;
        }

        // this.testDownload();
    }

    private void testDownload() {
        try {
            SakuraNetworkUtils.getInstance().get(realUrl,url);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
