package com.epifanov.kostya.hls_viewer.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.epifanov.kostya.hls_viewer.R;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistParserFactory;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.video.VideoSize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.epifanov.kostya.hls_viewer.utils.schedulers.Schedulers.MAIN_LOOPER;
import static com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist.PLAYLIST_TYPE_VOD;
import static com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;
import static com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM;

class CustomBandwidthMeter implements BandwidthMeter, EventListener {
  public CustomBandwidthMeter(BandwidthMeter bandwidthMeter) {
    this.bandwidthMeter = bandwidthMeter;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
  }

  public void overrideBandwith(Integer value) {
    overriden = value;
  }

  @Override
  public long getBitrateEstimate() {
    final long actual = bandwidthMeter.getBitrateEstimate();
    if (null != overriden) {
      return Math.min(overriden, actual);
    }
    return actual;
  }

  @Nullable
  @Override
  public TransferListener getTransferListener() {
    return bandwidthMeter.getTransferListener();
  }

  @Override
  public void addEventListener(Handler eventHandler, EventListener eventListener) {
    bandwidthMeter.addEventListener(eventHandler, eventListener);
  }

  @Override
  public void removeEventListener(EventListener eventListener) {
    bandwidthMeter.removeEventListener(eventListener);
  }

  Integer overriden = null;
  BandwidthMeter bandwidthMeter;
}

class CustomTrackSelector extends DefaultTrackSelector {

  public CustomTrackSelector(Context context, AdaptiveTrackSelection.Factory factory) {
    super(context, factory);
  }

  public void publicInvalidate() {
    super.invalidate();
  }

}

public class CustomExoPlayer extends FrameLayout
  implements Consumer<StreamKey>, Player.Listener {

  public PlayerView mPlayerView;

  List<Player.Listener> mVideoListeners = new ArrayList<>();
  List<Player.EventListener> mEventListeners = new ArrayList<>();
  List<AnalyticsListener> mAnalyticsListeners = new ArrayList<>();

  CustomTrackSelector mTrackSelector;
  private CustomBandwidthMeter mCustomBandwidthMeter;
  DefaultLoadControl mLoadControl;

  private StreamKey mCurrentStream;
  private VideoSize mCurrentVideoSize;
  private Size mCurrentSurfaceSize;
  private @HlsMediaPlaylist.PlaylistType int mCurrentType;
  private boolean initialPlayWhenReady = true;

  public CustomExoPlayer(@NonNull Context context) {
    this(context, null, null, null, true);
  }

  public CustomExoPlayer(@NonNull Context context, Player.Listener vl, EventListener el, AnalyticsListener al, boolean initialPlayWhenReady) {
    super(context);
    System.out.println(hashCode() + " CustomExoPlayer.initialize");

    this.initialPlayWhenReady = initialPlayWhenReady;

    mVideoListeners.add(vl);
    mVideoListeners.add(this);
    mEventListeners.add(el);
    mAnalyticsListeners.add(al);

    View inflated = LayoutInflater.from(context).inflate(R.layout.player_view, this, false);
    mPlayerView = (PlayerView) inflated.getRootView();
    mPlayerView.setUseController(false);
    mPlayerView.setShutterBackgroundColor(Color.TRANSPARENT);
    //addView(mPlayerView);

    mCustomBandwidthMeter = new CustomBandwidthMeter(DefaultBandwidthMeter.getSingletonInstance(context));
    mTrackSelector = new CustomTrackSelector(context, new AdaptiveTrackSelection.Factory());
    mTrackSelector.setParameters(mTrackSelector.buildUponParameters().setViewportSize(1280, 720, true));
    mLoadControl = new DefaultLoadControl.Builder()
      .setBufferDurationsMs(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS, DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
        1500,1500)
      .createDefaultLoadControl();

    createPlayer(context);
  }

  public void setInitialPlayWhenReady(boolean value) {
    initialPlayWhenReady = value;
  }

  private void createPlayer(Context context) {
    System.out.println(hashCode() + " CustomExoPlayer.createPlayer");

    SimpleExoPlayer exo = new SimpleExoPlayer.Builder(context)
      .setLooper(MAIN_LOOPER)
      .setBandwidthMeter(mCustomBandwidthMeter)
      .setTrackSelector(mTrackSelector)
      .setLoadControl(mLoadControl)
      .build();

    if (mVideoListeners != null)
      mVideoListeners.forEach((listener -> exo.addListener(listener)));

    if (mEventListeners != null)
      mEventListeners.forEach(exo::addListener);

    if (mAnalyticsListeners != null)
      mAnalyticsListeners.forEach(exo::addAnalyticsListener);

    mPlayerView.setPlayer(exo);
    setPlayWhenReady(initialPlayWhenReady);
  }

  public void recreate() {
    System.out.println(hashCode() + " CustomExoPlayer.recreate");
    dispose();
    Context context = mPlayerView.getContext();
    createPlayer(context);
    if (mCurrentStream != null) accept(mCurrentStream);
  }

  @Override
  public void accept(StreamKey key) {
    System.out.println(hashCode() + " CustomExoPlayer.accept: " + key);
    mCurrentStream = key;
    prepareMediaSource(key.url);
    setFitMode(key.fitMode);
  }

  public void dispose() {
    System.out.println(hashCode() + " CustomExoPlayer.dispose");
    getExoPlayer().release();
  }

  public void setPlayWhenReady(Boolean b) {
    System.out.println(hashCode() + " CustomExoPlayer.setPlayWhenReady: " + b);
    getExoPlayer().setPlayWhenReady(b);
  }

  public void setVolume(float value) {
    System.out.println(hashCode() + " CustomExoPlayer.setVolume: " + value);
    getExoPlayer().setVolume(value);
  }

  public void setFitMode(FitMode fitMode) {
    System.out.println(hashCode() + " CustomExoPlayer.setFitMode: " + fitMode);
    mPlayerView.setResizeMode(fitMode == FitMode.CONTAIN
      ? RESIZE_MODE_FIT : RESIZE_MODE_ZOOM);
  }

  public void seekTo(long value) {
    System.out.println(hashCode() + " CustomExoPlayer.seekTo: " + value);
    getExoPlayer().seekTo(value);
  }

  public void setPlaybackRate(float value) {
    System.out.println(hashCode() + " CustomExoPlayer.setPlaybackRate: " + value);
    PlaybackParameters pm = new PlaybackParameters(value);
    getExoPlayer().setPlaybackParameters(pm);
  }

  public long getTotalBufferedDuration() {
    return getExoPlayer().getTotalBufferedDuration();
  }

  public long getBufferedPosition() {
    return getExoPlayer().getBufferedPosition();
  }

  // VideoQuality.AUTO = -1
  // VideoQuality.MIN = 0
  public void setVideoQuality(int level) {
    //System.out.println(hashCode() + " BEFORE CustomExoPlayer.setVideoQuality: "
    //  + level + " surface:" + mCurrentSurfaceSize + " video:" + mCurrentVideoSize);
    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = mTrackSelector.getCurrentMappedTrackInfo();
    if (mappedTrackInfo != null) {
      for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
        if (getExoPlayer().getRendererType(i) == C.TRACK_TYPE_VIDEO) {

          TrackGroupArray videoTrackGroups = mappedTrackInfo.getTrackGroups(i);
          List<Format> formats = new ArrayList<>();
          for (int j = 0; j < videoTrackGroups.length; j++) {
            TrackGroup trackGroup = videoTrackGroups.get(j);
            for (int k = 0; k < trackGroup.length; k++) {
              Format format = trackGroup.getFormat(k);
              formats.add(format);
              //System.out.println("@@@@@ formats: " + format);
            }
          }

          int[] minmaxarr = getTrackIndexWithMinMaxBitrate(formats);
          if (level != -1) { // set MIN
            mCustomBandwidthMeter.overrideBandwith(getMinBitrate(formats));
            mTrackSelector.publicInvalidate();
          } else { // set AUTO
            mCustomBandwidthMeter.overrideBandwith(null);
            mTrackSelector.publicInvalidate();
          }

          //System.out.println(hashCode() + " Native: CustomExoPlayer.trackSelector - selected " + level);
          //System.out.println(hashCode() + " AFTER CustomExoPlayer.setVideoQuality: "
          //  + level + " surface:" + mCurrentSurfaceSize + " video:" + mCurrentVideoSize);
        }
      }
    }
  }

  private int getMinBitrate(List<Format> formats) {
    Format[] sorted = new Format[formats.size()];
    formats.toArray(sorted);
    Arrays.sort(sorted, (lhs, rhs) -> Integer.compare(lhs.bitrate, rhs.bitrate));
    return sorted[0].bitrate;
  }

  private int[] getTrackIndexWithMinMaxBitrate(List<Format> formats) {
    List<Format> sorted = new ArrayList<>(formats);
    sorted.sort((o1, o2) -> Integer.compare(o2.bitrate, o1.bitrate));
    //sorted.forEach(format -> System.out.println("@@@@@ sorted: " + format));
    return new int[]{
      formats.indexOf(sorted.get(sorted.size() - 1)),
      formats.indexOf(sorted.get(0))
    };
  }

  public Boolean hasTimeline() {
    return !getExoPlayer().getCurrentTimeline().isEmpty();
  }

  public long getCurrentPosition() {
    //System.out.println(hashCode() + " CustomExoPlayer.getCurrentPosition");
    final long currentPosition = getExoPlayer().getCurrentPosition();
    Timeline.Window w1 = new Timeline.Window();

    if (getExoPlayer().getCurrentTimeline().isEmpty()) {
      System.out.println(hashCode() + " Native: CustomExoPlayer: getCurrentPosition getCurrentTimeline() isEmpty");
      throw new IllegalStateException("Native: CustomExoPlayer: getCurrentPosition getCurrentTimeline() isEmpty");
    }

    final long windowStartTimeMs = w1.windowStartTimeMs;
    return w1.getPositionInFirstPeriodMs() + currentPosition + windowStartTimeMs;
  }

  public long getTotalPosition() {
    //System.out.println(hashCode() + " CustomExoPlayer.getTotalPosition");
    Timeline.Window w1 = new Timeline.Window();

    if (getExoPlayer().getCurrentTimeline().isEmpty()) {
      System.out.println(hashCode() + " Native: CustomExoPlayer: getTotalPosition getCurrentTimeline() isEmpty");
      throw new IllegalStateException("Native: CustomExoPlayer: getTotalPosition getCurrentTimeline() isEmpty");
    }

    int currentWindowIndex = getExoPlayer().getCurrentWindowIndex();
    getExoPlayer().getCurrentTimeline().getWindow(currentWindowIndex, w1);
    final long posMs = w1.windowStartTimeMs + getExoPlayer().getCurrentPosition();

    if (posMs < 0) {
      System.out.println(hashCode() + " Native: CustomExoPlayer: getTotalPosition posMs < 0: " + posMs);
      throw new IllegalStateException("Native: CustomExoPlayer: getTotalPosition posMs < 0: " + posMs);
    }

    return posMs;
  }

  public long getDuration() {
    //System.out.println(hashCode() + " CustomExoPlayer.getDuration");
    return getExoPlayer().getDuration();
  }

  public int[] getVideoSize() {
    System.out.println(hashCode() + " CustomExoPlayer.getVideoSize");
    Format format = getExoPlayer().getVideoFormat();
    assert format != null;
    return new int[]{ format.width, format.height };
  }

  public Bitmap captureFrame() {
    System.out.println(hashCode() + " CustomExoPlayer.captureFrame");
    int width, height;

    Format format = getExoPlayer().getVideoFormat();
    width = format != null ? format.width : mCurrentVideoSize.width;
    height = format != null ? format.height : mCurrentVideoSize.height;

    Bitmap bitmap = Bitmap.createBitmap(getResources().getDisplayMetrics(),
      width, height, Bitmap.Config.ARGB_8888);
    return ((TextureView) mPlayerView.getVideoSurfaceView()).getBitmap(bitmap);
  }

  @Override
  public void onVideoSizeChanged(VideoSize videoSize) {
    mCurrentVideoSize = videoSize;
  }

  @Override
  public void onSurfaceSizeChanged(int width, int height) {
    mCurrentSurfaceSize = new Size(width, height);
  }

  public boolean isVod() {
    return mCurrentType == PLAYLIST_TYPE_VOD;
  }

  private void prepareMediaSource(String url) {
    System.out.println(hashCode() + " CustomExoPlayer.buildMediaSource: " + url);
    HlsMediaSource source = new HlsMediaSource.Factory(new DefaultHttpDataSource.Factory())
      .setPlaylistParserFactory(new DefaultHlsPlaylistParserFactory())
      .createMediaSource(Uri.parse(url));
    getExoPlayer().prepare(source);
  }

  private SimpleExoPlayer getExoPlayer() {
    return (SimpleExoPlayer) mPlayerView.getPlayer();
  }

  private void removePlayerView() {
    this.removeView(mPlayerView);
    mPlayerView = null;
  }

}

