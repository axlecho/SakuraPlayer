package com.axlecho.sakuraplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.axlecho.sakura.SakuraPlayerView;
import com.axlecho.sakura.utils.SakuraLogUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "demo";
    private SakuraPlayerView player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player = (SakuraPlayerView) findViewById(R.id.player);
        // player.setTrumbImageUrl("http://webinput.nie.netease.com/img/yys/logo.png/100");
        // player.setVideoUrl("http://nie.v.netease.com/nie/yys/gw/pc/japancm.mp4");
        // player.setVideoUrl("https://www.bilibili.com/video/av14661594/");
        // player.setVideoUrl("https://www.bilibili.com/video/av15560010/");

        player.setVideoUrl("adaadf");
        player.setAutoPlay(true);
        // player.setVideoUrl("http://v.youku.com/v_show/id_XMzE4MzY3NjM0OA==.html");

        // player.setVideoUrl("http://www.xiami.com/song/1796759297");

        //        new Thread(new Runnable() {
        //            @Override
        //            public void run() {
        //                String realUrl = SakuraParser.getInstance().getStreamUrl("http://www.xiami.com/song/1796759297");
        //                Log.d(TAG, "realUrl " + realUrl);
        //            }
        //        }).start();

        // player.setVideoUrl("http://v.youku.com/v_show/id_XMzE4MzY3NjM0OA==.html");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.clear();
    }

    @Override
    public void onBackPressed() {
        if (player.isFullScreen()) {
            player.toggleFullScreen();
            SakuraLogUtils.d(TAG, "to normal mode");
            return;
        }

        super.onBackPressed();
    }
}
