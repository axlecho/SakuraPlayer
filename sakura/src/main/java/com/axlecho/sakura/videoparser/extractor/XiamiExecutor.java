package com.axlecho.sakura.videoparser.extractor;

import com.axlecho.sakura.utils.SakuraLogUtils;
import com.axlecho.sakura.utils.SakuraNetworkUtils;
import com.axlecho.sakura.utils.SakuraTextUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by axlecho on 2017/11/18 0018.
 */

public class XiamiExecutor extends BaseExtractors {
    public static final String NAME = "Xiami";
    private static final String TAG = NAME.toLowerCase();
    private static final String API_URL = "http://www.xiami.com/widget/xml-single/sid/";

    private String url;
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

    public String apiReq(String sid) throws IOException {
        String apiUrl = API_URL + sid;
        String xmlStr = SakuraNetworkUtils.getInstance().get(apiUrl);
        return xmlStr;
    }

    private void prepare() throws IOException {
        String sid = SakuraTextUtils.search(url, "song/(\\d+)");
        SakuraLogUtils.d(TAG,"sid " + sid);
        downloadBySid(sid);
    }


    private void downloadBySid(String sid) throws IOException {
        String apiXml = apiReq(sid);
        try {
            this.parseXiamiXml(apiXml);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void parseXiamiXml(String apiXml) throws ParserConfigurationException, IOException, SAXException {
        SakuraLogUtils.d(TAG, apiXml);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(apiXml)));
        Element track = (Element) document.getElementsByTagName("track").item(0);
        String artist = track.getElementsByTagName("artist_name").item(0).getTextContent();
        String song_title = track.getElementsByTagName("album_name").item(0).getTextContent();
        realUrl = xiamidecode(track.getElementsByTagName("location").item(0).getTextContent());
        // this.testDownload();
    }

    private static String xiamidecode(String location) throws UnsupportedEncodingException {
        int _local10;
        int _local2 = Integer.parseInt(location.substring(0, 1));
        String _local3 = location.substring(1, location.length());
        double _local4 = Math.floor(_local3.length() / _local2);
        int _local5 = _local3.length() % _local2;
        String[] _local6 = new String[_local2];
        int _local7 = 0;
        while (_local7 < _local5) {
            if (_local6[_local7] == null) {
                _local6[_local7] = "";
            }
            _local6[_local7] = _local3.substring((((int) _local4 + 1) * _local7),
                    (((int) _local4 + 1) * _local7) + ((int) _local4 + 1));
            _local7++;
        }
        _local7 = _local5;
        while (_local7 < _local2) {
            _local6[_local7] = _local3
                    .substring((((int) _local4 * (_local7 - _local5)) + (((int) _local4 + 1) * _local5)),
                            (((int) _local4 * (_local7 - _local5)) + (((int) _local4 + 1) * _local5)) + (int) _local4);
            _local7++;
        }
        String _local8 = "";
        _local7 = 0;
        while (_local7 < ((String) _local6[0]).length()) {
            _local10 = 0;
            while (_local10 < _local6.length) {
                if (_local7 >= _local6[_local10].length()) {
                    break;
                }
                _local8 = (_local8 + _local6[_local10].charAt(_local7));
                _local10++;
            }
            _local7++;
        }
        _local8 = URLDecoder.decode(_local8, "utf8");
        String _local9 = "";
        _local7 = 0;
        while (_local7 < _local8.length()) {
            if (_local8.charAt(_local7) == '^') {
                _local9 = (_local9 + "0");
            } else {
                _local9 = (_local9 + _local8.charAt(_local7));
            }
            ;
            _local7++;
        }
        _local9 = _local9.replace("+", " ");
        return _local9;
    }

}
