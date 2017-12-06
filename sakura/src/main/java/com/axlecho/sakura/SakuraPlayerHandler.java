package com.axlecho.sakura;

import android.os.Handler;
import android.os.Message;

import com.axlecho.sakura.utils.SakuraLogUtils;

/**
 * Created by axlecho on 2017/12/6 0006.
 */

public class SakuraPlayerHandler extends Handler {
    private static final String TAG = "sakura_player";
    public static final int VIDEO_PROCESS_SYNC_MSG = 1;
    public static final int HIDE_CONTROLLER_MSG = 2;

    private SakuraPlayerView player;

    public SakuraPlayerHandler(SakuraPlayerView player) {
        this.player = player;
    }

    @Override
    public void handleMessage(Message msg) {
        SakuraLogUtils.d(TAG, "recivie massage " + msg.what);
        switch (msg.what) {
            case VIDEO_PROCESS_SYNC_MSG:
                player.syncProgress();
                break;
            case HIDE_CONTROLLER_MSG:
                if (player.getManager().isPlaying()) {
                    player.syncControllerStatus(false);
                }
                break;
        }
        super.handleMessage(msg);
    }
}