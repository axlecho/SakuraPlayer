package com.axlecho.sakura;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.axlecho.sakura.IjkVideoPlayer.IRenderView;
import com.axlecho.sakura.IjkVideoPlayer.IjkVideoView;
import com.axlecho.sakura.IjkVideoPlayer.TextureRenderView;
import com.axlecho.sakura.units.HttpProxyCacheServerManager;
import com.axlecho.sakura.utils.SakuraLogUtils;
import com.axlecho.sakura.videoparser.SakuraParser;
import com.danikula.videocache.HttpProxyCacheServer;

import okhttp3.Headers;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


/**
 * Created by tcking on 15/10/27.
 */
public class PlayerManager implements IMediaPlayer.OnCompletionListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnPreparedListener {
    public static final String TAG = "sakura_player";

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
            Log.e(TAG, "播放器不支持此设备");
        }
    }

    public void play() {
        if (playerSupport) {
            this.videoView.setVideoPath(url);
            this.videoView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW);
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
        this.parserUrl();
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
        int pos = (int) (duration * percent);
        videoView.seekTo(pos);
        // Log.d(TAG, "[seekTo] percent " + percent + " duration " + duration + " seek to " + pos);
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

        Log.d(TAG, "onVolumeSlide:" + s);
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
            Log.d(TAG, "onProgressSlide:" + text);
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
            Log.d(TAG, "[StartPlayAction]");
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
            Log.d(TAG, "[ResumeAction]");
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
            Log.d(TAG, "[PauseAction]");
            manager.pause();
            view.syncPauseStatus();
        }
    }

    public static class ToggleFullScreenAction extends BaseAction {
        private static final int FULL_SCREEN_LAYOUT_ID = 5201314;
        private Activity activity;
        private PlayerView player;

        public ToggleFullScreenAction(Activity activity, PlayerView player) {
            this.activity = activity;
            this.player = player;
        }

        private void processToggleToFullScreenMode() {
            // change activity to landscape orientation and set activity to full screen
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                View decorView = activity.getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);
            }

            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }

            // prepare a framelayout as fullscreen mode container
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            ViewGroup root = (ViewGroup) activity.findViewById(android.R.id.content);
            FrameLayout frameLayout = new FrameLayout(activity);
            frameLayout.setId(FULL_SCREEN_LAYOUT_ID);
            frameLayout.setBackgroundColor(Color.BLACK);
            root.addView(frameLayout);
            frameLayout.setLayoutParams(params);

            // resize the texture render view to full screen
            TextureRenderView renderView = new TextureRenderView(activity);
            renderView.setLayoutParams(params);
            player.videoView.setRenderView(renderView);

            // save params for back to normal mode
            player.parent = (ViewGroup) player.getParent();
            player.params = player.getLayoutParams();

            // remove player view from parent and add it to full screen container
            ((ViewGroup) player.getParent()).removeView(player);
            frameLayout.addView(player);
            player.setLayoutParams(params);

            player.isFullScreen = true;
        }

        private void processToggleToNormalMode() {
            // change activity to portrait orientation and add the decorview
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            ActionBar actionBar = activity.getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            View decorView = activity.getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);


            ViewGroup root = (ViewGroup) activity.findViewById(android.R.id.content);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            // resize render view
            TextureRenderView renderView = new TextureRenderView(activity);
            renderView.setLayoutParams(params);

            // remove player view from parent
            ((ViewGroup) player.getParent()).removeView(player);

            // set to preview params
            player.parent.addView(player);
            player.setLayoutParams(player.params);

            // remove full screen container from root
            FrameLayout frameLayout = (FrameLayout) root.findViewById(FULL_SCREEN_LAYOUT_ID);
            root.removeView(frameLayout);

            player.isFullScreen = false;
        }

        @Override
        public void excute() {
            Log.d(TAG, "[ToggleFullScreenAction] ");
            if (!player.isFullScreen) {
                processToggleToFullScreenMode();
            } else {
                processToggleToNormalMode();
            }
        }
    }


    private String getCacheVideoPath(String url) {
        HttpProxyCacheServer proxy = HttpProxyCacheServerManager.getInstance(context).getProxy();
        return proxy.getProxyUrl(url);
    }

    public void addHeaders(String headers) {
        videoView.setHeader(headers);
    }

    private void parserUrl() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String realUrl = SakuraParser.getInstance().getStreamUrl(url);
                Message msg = Message.obtain();
                msg.obj = realUrl;
                handler.sendMessage(msg);
            }
        }).start();
    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            String realUrl = (String) msg.obj;
            SakuraLogUtils.d(TAG, realUrl);

            Headers headers = new Headers.Builder()
                    .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0")
                    .add("Referer", url)
                    .build();
            addHeaders(headers.toString());
            url = realUrl;
        }
    };
}
