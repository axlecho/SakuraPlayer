package com.axlecho.sakura;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.axlecho.sakura.IjkVideoPlayer.IRenderView;
import com.axlecho.sakura.IjkVideoPlayer.IjkVideoView;
import com.axlecho.sakura.IjkVideoPlayer.TextureRenderView;
import com.axlecho.sakura.utils.SakuraHttpProxyCacheServerManager;
import com.axlecho.sakura.utils.SakuraLogUtils;
import com.axlecho.sakura.videoparser.SakuraParser;
import com.danikula.videocache.HttpProxyCacheServer;

import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Headers;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


/**
 * Created by tcking on 15/10/27.
 */
public class SakuraPlayerManager implements IMediaPlayer.OnCompletionListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnPreparedListener {
    public static final String TAG = "sakura_player";

    public static final String SCALETYPE_FITPARENT = "fitParent";
    public static final String SCALETYPE_FILLPARENT = "fillParent";
    public static final String SCALETYPE_WRAPCONTENT = "wrapContent";
    public static final String SCALETYPE_FITXY = "fitXY";
    public static final String SCALETYPE_16_9 = "16:9";
    public static final String SCALETYPE_4_3 = "4:3";

    private final Context context;
    private final IjkVideoView videoView;
    private final SakuraPlayerView sakuraPlayerView;
    private final AudioManager audioManager;

    private final int mMaxVolume;
    private boolean playerSupport;
    private int volume = -1;
    private long newPosition = -1;
    private String url;
    private boolean autoPlay = false;

    public SakuraPlayerManager(Context context, SakuraPlayerView sakuraPlayerView) {
        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
            playerSupport = true;
        } catch (Throwable e) {
            Log.e(TAG, "loadLibraries error", e);
        }

        this.context = context;
        this.sakuraPlayerView = sakuraPlayerView;
        this.videoView = sakuraPlayerView.videoView;

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
        sakuraPlayerView.syncBuffingStatus(true);
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
            sakuraPlayerView.syncStopStatus();
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
    }

    public SakuraPlayerManager toggleAspectRatio() {
        if (videoView != null) {
            videoView.toggleAspectRatio();
        }
        return null;
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

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        Log.d(TAG, "[onCompletion]");
        this.stop();
    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
        Log.d(TAG, "[onError] what " + what + " extra " + extra);

        String errorString = context.getResources().getString(R.string.error_tip);
        String msg = String.format(Locale.CHINA, errorString, what);
        sakuraPlayerView.syncErrorStatus(msg);
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int what, int extra) {
        Log.d(TAG, "[onInfo] what " + what + " extra " + extra);
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (videoView.getCurrentState() == IjkVideoView.STATE_PLAYING) {
                    sakuraPlayerView.syncBuffingStatus(true);
                }
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                sakuraPlayerView.syncBuffingStatus(false);
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                // TODO 显示下载速度
                // Toaster.show("download rate:" + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                sakuraPlayerView.syncPlayingStatus();
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        Log.d(TAG, "[onPrepared] state " + videoView.getCurrentState());
        sakuraPlayerView.syncBuffingStatus(false);
    }



    public static abstract class BaseAction implements View.OnClickListener, Consumer {
        protected abstract void excute();

        @Override
        public void onClick(View view) {
            this.excute();
        }

        @Override
        public void accept(Object o) throws Exception {
            excute();
        }
    }

    public static class StartPlayAction extends BaseAction {
        private SakuraPlayerManager manager;
        private SakuraPlayerView view;

        public StartPlayAction(SakuraPlayerManager manager, SakuraPlayerView view) {
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
        private SakuraPlayerManager manager;
        private SakuraPlayerView view;

        public ResumeAction(SakuraPlayerManager manager, SakuraPlayerView view) {
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
        private SakuraPlayerManager manager;
        private SakuraPlayerView view;

        public PauseAction(SakuraPlayerManager manager, SakuraPlayerView view) {
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
        private SakuraPlayerView player;

        public ToggleFullScreenAction(Activity activity, SakuraPlayerView player) {
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
        HttpProxyCacheServer proxy = SakuraHttpProxyCacheServerManager.getInstance(context).getProxy();
        return proxy.getProxyUrl(url);
    }

    public void addHeaders(String headers) {
        videoView.setHeader(headers);
    }

    public void clear() {
        if (handler != null && !handler.isDisposed()) {
            handler.dispose();
        }

        if (videoView.getCurrentState() != IjkVideoView.STATE_IDLE) {
            videoView.release(true);
        }
    }

    private void parserUrl() {
        handler = src.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(parserObserver, errorHandler);
    }

    private Disposable handler;
    private Observable<String> src = Observable.create(new ObservableOnSubscribe<String>() {
        @Override
        public void subscribe(ObservableEmitter<String> e) throws Exception {
            String ret = SakuraParser.getInstance().getStreamUrl(url);
            e.onNext(ret);
            e.onComplete();
        }
    });

    private Consumer<Throwable> errorHandler = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable throwable) throws Exception {
            sakuraPlayerView.syncErrorStatus(throwable.getMessage());
        }
    };

    private Consumer<String> parserObserver = new Consumer<String>() {
        @Override
        public void accept(String realUrl) throws Exception {
            SakuraLogUtils.d(TAG, "[onNext]");
            if (TextUtils.isEmpty(realUrl)) {
                SakuraLogUtils.e(TAG, "prase failed");
                return;
            }

            SakuraLogUtils.d(TAG, "the real url :" + realUrl);
            Headers headers = new Headers.Builder()
                    .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:51.0) Gecko/20100101 Firefox/51.0")
                    .add("Referer", url)
                    .build();
            addHeaders(headers.toString());
            url = realUrl;
            sakuraPlayerView.syncBuffingStatus(false);
            if (autoPlay) {
                play();
            }
        }
    };

    public void processErrorWithDefaultAction() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        context.startActivity(intent);
    }
}
