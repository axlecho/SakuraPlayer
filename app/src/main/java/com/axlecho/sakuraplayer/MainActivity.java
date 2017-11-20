package com.axlecho.sakuraplayer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import com.axlecho.sakura.PlayerView;
import com.axlecho.sakura.utils.SakuraLogUtils;
import com.axlecho.sakura.videoparser.SakuraParser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "demo";
    private PlayerView player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player = (PlayerView) findViewById(R.id.player);
        // player.setTrumbImageUrl("http://webinput.nie.netease.com/img/yys/logo.png/100");
        // player.setVideoUrl("http://nie.v.netease.com/nie/yys/gw/pc/japancm.mp4");

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = SakuraParser.getInstance().getStreamUrl("https://www.bilibili.com/video/av16336903/");
                Message msg = Message.obtain();
                msg.obj = url;
                handler.sendMessage(msg);

            }
        }).start();

    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            String url = (String) msg.obj;
            SakuraLogUtils.d(TAG, url);
            player.setVideoUrl(url);
        }
    };
}
