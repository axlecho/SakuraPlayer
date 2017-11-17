package com.axlecho.sakura;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static com.axlecho.sakura.IjkVideoView.RENDER_NONE;


/**
 * Created by tcking on 15/10/27.
 */
public class PlayerManager implements IMediaPlayer.OnCompletionListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnPreparedListener {
    public static final String TAG = "player";

    public static final String SCALETYPE_FITPARENT = "fitParent";
    public static final String SCALETYPE_FILLPARENT = "fillParent";
    public static final String SCALETYPE_WRAPCONTENT = "wrapContent";
    public static final String SCALETYPE_FITXY = "fitXY";
    public static final String SCALETYPE_16_9 = "16:9";
    public static final String SCALETYPE_4_3 = "4:3";

    private final Context context;
    private final IjkVideoView videoView;
    private final PlayerView playerView;
    private final AudioManager audioManager;

    private final int mMaxVolume;
    private boolean playerSupport;
    private int STATUS_ERROR = -1;
    private int STATUS_IDLE = 0;
    private int STATUS_LOADING = 1;
    private int STATUS_PLAYING = 2;
    private int STATUS_PAUSE = 3;
    private int STATUS_COMPLETED = 4;
    private long pauseTime;
    private int status = STATUS_IDLE;
    private boolean isLive = false;//是否为直播
    private OrientationEventListener orientationEventListener;
    private int defaultTimeout = 3000;
    private int screenWidthPixels;


    private boolean isShowing;
    private boolean portrait;
    private float brightness = -1;
    private int volume = -1;
    private long newPosition = -1;
    private long defaultRetryTime = 5000;

    private int currentPosition;
    private boolean fullScreenOnly;

    private long duration;
    private boolean instantSeeking;
    private boolean isDragging;

    private String url;

    public PlayerManager(Context context, PlayerView playerView) {
        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
            playerSupport = true;
        } catch (Throwable e) {
            Log.e(TAG, "loadLibraries error", e);
        }

        this.context = context;
        this.screenWidthPixels = this.context.getResources().getDisplayMetrics().widthPixels;
        this.playerView = playerView;
        this.videoView = playerView.videoView;

        this.videoView.setOnCompletionListener(this);
        this.videoView.setOnErrorListener(this);
        this.videoView.setOnInfoListener(this);
        this.videoView.setOnPreparedListener(this);

        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.mMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);


        if (!playerSupport) {
            DebugLog.e("播放器不支持此设备");
        }
    }

    public void play() {
        if (playerSupport) {
            this.videoView.setVideoPath(url);
            this.resume();
        }
        playerView.syncBuffingStatus(true);
    }

    public void resume() {
        videoView.start();
    }

    public void pause() {
        videoView.pause();
    }

    public void setVideoUrl(String videoUrl) {
        this.url = videoUrl;
    }

    public void stop() {
        Log.d(TAG, "[stop] state " + videoView.getCurrentState());
        if (videoView.getCurrentState() != IjkVideoView.STATE_IDLE) {
            videoView.release(true);
            videoView.setVideoURI(null);
            playerView.syncStopStatus();
        }
    }

    public boolean isPlayerSupport() {
        return playerSupport;
    }

    public boolean isPlaying() {
        return videoView != null ? videoView.isPlaying() : false;
    }

    public int getDuration() {
        return videoView.getDuration();
    }

    public int getCurrentPosition() {
        return videoView.getCurrentPosition();
    }

    public void seekTo(float percent) {
        long duration = videoView.getDuration();
        videoView.seekTo((int) (duration * percent));
        Log.d(TAG, "[seekTo] percent " + percent + " duration " + duration);
    }

    public PlayerManager toggleAspectRatio() {
        if (videoView != null) {
            videoView.toggleAspectRatio();
        }
        return null;
    }

    public void setDefaultRetryTime(long defaultRetryTime) {
        this.defaultRetryTime = defaultRetryTime;
    }

    public void onPause() {
        pauseTime = System.currentTimeMillis();
        if (status == STATUS_PLAYING) {
            videoView.pause();
            if (!isLive) {
                currentPosition = videoView.getCurrentPosition();
            }
        }
    }

    public void onResume() {
        pauseTime = 0;
        if (status == STATUS_PLAYING) {
            if (isLive) {
                videoView.seekTo(0);
            } else {
                if (currentPosition > 0) {
                    videoView.seekTo(currentPosition);
                }
            }
            videoView.start();
        }
    }

    public void onDestroy() {
        // orientationEventListener.disable();
        videoView.stopPlayback();
    }


    public int getCurrentState() {
        return videoView.getCurrentState();
    }

    private void onVolumeSlide(float percent) {
        if (volume == -1) {
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume < 0)
                volume = 0;
        }
        int index = (int) (percent * mMaxVolume) + volume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;

        // 变更声音
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

        // 变更进度条
        int i = (int) (index * 1.0 / mMaxVolume * 100);
        String s = i + "%";
        if (i == 0) {
            s = "off";
        }

        DebugLog.d("onVolumeSlide:" + s);
    }

    private void onProgressSlide(float percent) {
        long position = videoView.getCurrentPosition();
        long duration = videoView.getDuration();
        long deltaMax = Math.min(100 * 1000, duration - position);
        long delta = (long) (deltaMax * percent);


        newPosition = delta + position;
        if (newPosition > duration) {
            newPosition = duration;
        } else if (newPosition <= 0) {
            newPosition = 0;
            delta = -position;
        }
        int showDelta = (int) delta / 1000;
        if (showDelta != 0) {
            String text = showDelta > 0 ? ("+" + showDelta) : "" + showDelta;
            DebugLog.d("onProgressSlide:" + text);
        }
    }

    public void setScaleType(String scaleType) {
        if (SCALETYPE_FITPARENT.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT);
        } else if (SCALETYPE_FILLPARENT.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_ASPECT_FILL_PARENT);
        } else if (SCALETYPE_WRAPCONTENT.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_ASPECT_WRAP_CONTENT);
        } else if (SCALETYPE_FITXY.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_MATCH_PARENT);
        } else if (SCALETYPE_16_9.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_16_9_FIT_PARENT);
        } else if (SCALETYPE_4_3.equals(scaleType)) {
            videoView.setAspectRatio(IRenderView.AR_4_3_FIT_PARENT);
        }
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        Log.d(TAG, "[onCompletion]");
        this.stop();
    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
        Log.d(TAG, "[onError] what " + what + " extra " + extra);
        return false;
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int what, int extra) {
        Log.d(TAG, "[onInfo] what " + what + " extra " + extra);
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (videoView.getCurrentState() == IjkVideoView.STATE_PLAYING) {
                    playerView.syncBuffingStatus(true);
                }
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                playerView.syncBuffingStatus(false);
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                // TODO 显示下载速度
                // Toaster.show("download rate:" + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                playerView.syncPlayingStatus();
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        Log.d(TAG, "[onPrepared] state " + videoView.getCurrentState());
        playerView.syncBuffingStatus(false);
    }

    public static abstract class BaseAction implements View.OnClickListener {
        protected abstract void excute();

        @Override
        public void onClick(View view) {
            this.excute();
        }
    }

    public static class StartPlayAction extends BaseAction {
        private PlayerManager manager;
        private PlayerView view;

        public StartPlayAction(PlayerManager manager, PlayerView view) {
            this.manager = manager;
            this.view = view;
        }

        @Override
        protected void excute() {
            DebugLog.d("[StartPlayAction]");
            manager.play();
        }
    }

    public static class ResumeAction extends BaseAction {
        private PlayerManager manager;
        private PlayerView view;

        public ResumeAction(PlayerManager manager, PlayerView view) {
            this.manager = manager;
            this.view = view;
        }

        @Override
        protected void excute() {
            DebugLog.d("[ResumeAction]");
            manager.resume();

            // ijkPlayer没有提供播放事件的回调
            if (manager.getCurrentState() == IjkVideoView.STATE_PLAYING) {
                view.syncPlayingStatus();
            }
        }
    }

    public static class PauseAction extends BaseAction {
        private PlayerManager manager;
        private PlayerView view;

        public PauseAction(PlayerManager manager, PlayerView view) {
            this.manager = manager;
            this.view = view;
        }

        @Override
        protected void excute() {
            DebugLog.d("[PauseAction]");
            manager.pause();
            view.syncPauseStatus();
        }
    }

    public static class ToggleFullScreenAction extends BaseAction {
        private static final int FULL_SCREEN_LAYOUT_ID = 5201314;

        private Activity activity;
        private PlayerManager manager;
        private PlayerView player;

        public ToggleFullScreenAction(Activity activity, PlayerManager manager, PlayerView player) {
            this.activity = activity;
            this.manager = manager;
            this.player = player;
        }

        @Override
        public void excute() {
            Log.d(TAG, "[ToggleFullScreenAction] ");
            if (!player.isFullScreen) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                this.manager.videoView.setRender(RENDER_NONE);

                ViewGroup group = (ViewGroup) activity.findViewById(android.R.id.content);
                WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
                final int w = wm.getDefaultDisplay().getWidth();
                final int h = wm.getDefaultDisplay().getHeight();
                FrameLayout.LayoutParams lpParent = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                FrameLayout frameLayout = new FrameLayout(activity);
                frameLayout.setId(FULL_SCREEN_LAYOUT_ID);
                frameLayout.setBackgroundColor(Color.BLACK);
                group.addView(frameLayout);
                frameLayout.setLayoutParams(lpParent);

                TextureRenderView renderView = new TextureRenderView(activity);
                renderView.setLayoutParams(lpParent);
                renderView.setVideoSize(w, h);
                player.videoView.setRenderView(renderView);

                player.parent = (ViewGroup)player.getParent();
                player.params = player.getLayoutParams();

                ((ViewGroup) player.getParent()).removeView(player);
                frameLayout.addView(player);
                player.setLayoutParams(lpParent);
                player.isFullScreen = true;
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                ViewGroup group = (ViewGroup) activity.findViewById(android.R.id.content);

                WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
                final int w = wm.getDefaultDisplay().getWidth();
                final int h = wm.getDefaultDisplay().getHeight();
                FrameLayout.LayoutParams lpParent = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                TextureRenderView renderView = new TextureRenderView(activity);
                renderView.setLayoutParams(lpParent);
                renderView.setVideoSize(w, h);

                ((ViewGroup) player.getParent()).removeView(player);
                player.parent.addView(player);
                player.setLayoutParams(player.params);
                player.isFullScreen = false;

                FrameLayout frameLayout = (FrameLayout) group.findViewById(FULL_SCREEN_LAYOUT_ID);
                group.removeView(frameLayout);


            }
        }
    }

    public interface PlayerStateListener {
        void onComplete();

        void onError();

        void onLoading();

        void onPlay();
    }
}
