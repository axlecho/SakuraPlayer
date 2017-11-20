package com.axlecho.sakura;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.axlecho.sakura.IjkVideoPlayer.IjkVideoView;
import com.squareup.picasso.Picasso;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by axlecho on 2017/10/19 0019.
 */

public class PlayerView extends RelativeLayout implements View.OnTouchListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "sakura_player";
    private static final int VIDEO_PROCESS_SYNC_MSG = 1;
    private static final int HIDE_CONTROLLER_MSG = 2;

    // videoView
    public IjkVideoView videoView;
    public Activity activity;

    // controller
    private ImageView controllerPlayerBtn;
    private TextView controllerCurrentTimeTextView;
    private TextView controllerEndTimeTextView;
    private SeekBar controllerSeekBar;
    private ImageView controllerRotationBtn;
    private ImageView controllerFullScreenBtn;
    private ImageView playerBtn;
    private View controllerLayout;

    // status
    private TextView statusTitleTextView;
    private View statusLoadingView;
    private ImageView statusTrumbImageView;

    private PlayerManager playerManager;
    private Timer syncVideoProcessTimer;
    private Timer hideControllerTimer;
    private GestureDetector gestureDetector;


    public ViewGroup parent;
    public ViewGroup.LayoutParams params;
    public boolean isFullScreen = false;

    public PlayerView(Context context) {
        super(context);
        this.init(context);
    }

    public PlayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.init(context);
    }

    public PlayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context);
    }

    public void init(Context context) {
        this.initView(context);
        if (isInEditMode()) {
            return;
        }

        this.gestureDetector = new GestureDetector(context, new PlayerGestureListener());
        this.setOnTouchListener(this);

        this.playerManager = new PlayerManager(context, this);
        this.syncStopStatus();

        if (context instanceof Activity) {
            this.activity = (Activity) context;
        }
    }

    public void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_player, this, true);
        controllerPlayerBtn = (ImageView) this.findViewById(R.id.player_controllbar_play_btn);
        controllerCurrentTimeTextView = (TextView) this.findViewById(R.id.player_controllbar_currenttime_textview);
        controllerEndTimeTextView = (TextView) this.findViewById(R.id.player_controllbar_endtime_textview);
        controllerSeekBar = (SeekBar) this.findViewById(R.id.player_controllbar_seekbar);
        controllerRotationBtn = (ImageView) this.findViewById(R.id.ijk_iv_rotation);
        controllerFullScreenBtn = (ImageView) this.findViewById(R.id.player_controllbar_fullscreen);
        playerBtn = (ImageView) this.findViewById(R.id.play_big_btn);

        statusTitleTextView = (TextView) this.findViewById(R.id.app_video_title);
        statusLoadingView = this.findViewById(R.id.app_video_loading);
        statusTrumbImageView = (ImageView) this.findViewById(R.id.player_trumb_imageview);
        videoView = (IjkVideoView) this.findViewById(R.id.player_video_view);
        controllerLayout = this.findViewById(R.id.player_controllbar_layout);
        controllerSeekBar.setMax(1000);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // save params for toggle full screen mode
        parent = (ViewGroup) getParent();
        params = this.getLayoutParams();
    }

    public void initAction() {
        this.playerBtn.setOnClickListener(new PlayerManager.StartPlayAction(playerManager, this));
        this.controllerPlayerBtn.setOnClickListener(new PlayerManager.StartPlayAction(playerManager, this));
        this.controllerSeekBar.setOnSeekBarChangeListener(this);
        this.controllerFullScreenBtn.setOnClickListener(new PlayerManager.ToggleFullScreenAction(activity, this));
    }

    public void stop() {
        playerManager.stop();
    }

    public void setVideoUrl(String url) {
        playerManager.setVideoUrl(url);
    }

    public void setTrumbImageUrl(String url) {
        Picasso.with(getContext()).load(url).into(statusTrumbImageView);
    }

    public void syncBuffingStatus(boolean isBuffing) {
        this.videoView.setVisibility(VISIBLE);
        if (isBuffing) {
            statusLoadingView.setVisibility(VISIBLE);
            this.syncControllerStatus(false);
        } else {
            statusLoadingView.setVisibility(GONE);
            this.syncControllerStatus(true);
        }
    }

    public void syncPlayingStatus() {
        this.statusTrumbImageView.setVisibility(GONE);
        this.playerBtn.setImageResource(R.mipmap.ic_player_center_pause);
        this.controllerPlayerBtn.setImageResource(R.mipmap.ic_player_pause_white_24dp);
        this.playerBtn.setOnClickListener(new PlayerManager.PauseAction(playerManager, this));
        this.controllerPlayerBtn.setOnClickListener(new PlayerManager.PauseAction(playerManager, this));
        this.startSendVideoProcessSyncMsg();
    }

    public void syncStopStatus() {
        Log.d(TAG, "[syncStopStatus]");
        this.videoView.setVisibility(GONE);

        this.statusLoadingView.setVisibility(GONE);
        this.statusTrumbImageView.setVisibility(VISIBLE);
        this.playerBtn.setImageResource(R.mipmap.ic_player_center_play);
        this.controllerPlayerBtn.setImageResource(R.mipmap.ic_player_play_white_24dp);
        this.stopSendVideoProcessSyncMsg();
        this.syncProgress();
        this.syncControllerStatus(true);
        this.controllerLayout.setVisibility(GONE);
        this.initAction();
    }

    public void syncPauseStatus() {
        this.playerBtn.setImageResource(R.mipmap.ic_player_center_play);
        this.controllerPlayerBtn.setImageResource(R.mipmap.ic_player_play_white_24dp);
        this.playerBtn.setOnClickListener(new PlayerManager.ResumeAction(playerManager, this));
        this.controllerPlayerBtn.setOnClickListener(new PlayerManager.ResumeAction(playerManager, this));
    }

    public void syncErrorStatus() {
    }

    private void syncProgress() {
        long position = playerManager.getCurrentPosition();
        long duration = playerManager.getDuration();

        // Log.d(TAG, "[syncProgress] current " + position + " duration " + duration);
        if (duration <= 0) {
            controllerSeekBar.setProgress(0);
            controllerSeekBar.setSecondaryProgress(0);
            controllerCurrentTimeTextView.setText(generateTime(position));
            controllerEndTimeTextView.setText(generateTime(duration));
            return;
        }

        int pos = (int) (1000 * position / duration);
        controllerSeekBar.setProgress(pos);
        int percent = videoView.getBufferPercentage();
        controllerSeekBar.setSecondaryProgress(percent * 10);
        controllerCurrentTimeTextView.setText(generateTime(position));
        controllerEndTimeTextView.setText(generateTime(duration));
    }

    public void syncControllerStatus(boolean isShow) {
        if (isShow) {
            controllerLayout.setVisibility(VISIBLE);
            playerBtn.setVisibility(VISIBLE);
            delayHideControllerMsg();
        } else {
            controllerLayout.setVisibility(GONE);
            playerBtn.setVisibility(GONE);
            cancelDelayHideControllerMsg();
        }
    }

    public void toggleControllerStatus() {
        if (controllerLayout.getVisibility() == VISIBLE) {
            syncControllerStatus(false);
        } else {
            syncControllerStatus(true);
        }
    }

    public void toggleFullScreen() {
        PlayerManager.ToggleFullScreenAction action = new PlayerManager.ToggleFullScreenAction(activity, this);
        action.excute();
    }

    private String generateTime(long time) {
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }

    private void startSendVideoProcessSyncMsg() {
        syncVideoProcessTimer = new Timer();
        syncVideoProcessTimer.schedule(new VideoProcessSyncTimeTask(), 200, 500);
    }

    private void stopSendVideoProcessSyncMsg() {
        if (syncVideoProcessTimer != null) {
            syncVideoProcessTimer.cancel();
            syncVideoProcessTimer = null;
        }
    }

    private void delayHideControllerMsg() {
        hideControllerTimer = new Timer();
        hideControllerTimer.schedule(new HideControllerTimeTask(), 4000);
    }

    private void cancelDelayHideControllerMsg() {
        if (hideControllerTimer != null) {
            hideControllerTimer.cancel();
            hideControllerTimer = null;
        }
    }

    private class VideoProcessSyncTimeTask extends TimerTask {

        @Override
        public void run() {
            handler.sendEmptyMessage(VIDEO_PROCESS_SYNC_MSG);
        }
    }

    private class HideControllerTimeTask extends TimerTask {

        @Override
        public void run() {
            handler.sendEmptyMessage(HIDE_CONTROLLER_MSG);
        }
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case VIDEO_PROCESS_SYNC_MSG:
                    syncProgress();
                    break;
                case HIDE_CONTROLLER_MSG:
                    if (playerManager.isPlaying()) {
                        syncControllerStatus(false);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!playerManager.isPlaying()) {
                return true;
            }

            toggleFullScreen();
            Log.d(TAG, "[onDoubleTap]");
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "[onDown]");
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (!playerManager.isPlaying()) {
                return true;
            }

            toggleControllerStatus();
            Log.d(TAG, "[onSingleTapUp]");
            return false;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int process, boolean fromUser) {
        if (!fromUser) {
            return;
        }

        long progress = seekBar.getProgress();
        float percent = progress / 1000f;
        long duration = playerManager.getDuration();
        long currentPosition = (long) (duration * percent);
        controllerCurrentTimeTextView.setText(generateTime(currentPosition));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeMessages(VIDEO_PROCESS_SYNC_MSG);
        this.stopSendVideoProcessSyncMsg();
        this.cancelDelayHideControllerMsg();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        long progress = seekBar.getProgress();
        float percent = progress / 1000f;
        playerManager.seekTo(percent);
        handler.removeMessages(VIDEO_PROCESS_SYNC_MSG);
        this.startSendVideoProcessSyncMsg();
        this.delayHideControllerMsg();
    }

    public PlayerManager getManager() {
        return playerManager;
    }

    public void addHeaders(String name, String value) {
        this.playerManager.addHttpHeaders(name, value);
    }
}
