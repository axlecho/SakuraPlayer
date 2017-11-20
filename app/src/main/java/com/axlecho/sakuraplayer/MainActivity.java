package com.axlecho.sakuraplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.axlecho.sakura.PlayerView;

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
        player.setVideoUrl("https://www.bilibili.com/video/av14661594/");
    }
}
