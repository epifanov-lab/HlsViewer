package com.epifanov.kostya.hls_viewer.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Handler;
import android.util.Size;
import android.view.PixelCopy;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistParserFactory;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoSize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.epifanov.kostya.hls_viewer.utils.schedulers.Schedulers.MAIN_LOOPER;

public class CustomExoPlayer implements Consumer<StreamKey>, Player.Listener {

  public interface OutputFormatListener {
    public void onOutputFormatChanged(MediaFormat format);
  }

  public static final String USER_AGENT = "webka_mobile_flutter";

  private PlayerView mPlayerView;

  List<Player.Listener> mVideoListeners = new ArrayList<>();
  List<Player.EventListener> mEventListeners = new ArrayList<>();
  List<AnalyticsListener> mAnalyticsListeners = new ArrayList<>();

  CustomTrackSelector mTrackSelector;
  private final CustomBandwidthMeter mCustomBandwidthMeter;
  DefaultLoadControl mLoadControl;

  private StreamKey mCurrentStream;
  private VideoSize mCurrentVideoSize;
  private Size mCurrentSurfaceSize;
  private boolean initialPlayWhenReady = true;
  private final SurfaceTexture texture;
  private final Context context;
  private final Surface surface;
  private ExoPlayer player;
  private final OutputFormatListener outputFormatListener;

  public CustomExoPlayer(@NonNull Context context,
                         Player.Listener vl,
                         EventListener el,
                         AnalyticsListener al,
                         OutputFormatListener ofl,
                         boolean initialPlayWhenReady,
                         SurfaceTexture texture) {

    System.out.println(hashCode() + " Native: TestExoPlayer.initialize");

    this.initialPlayWhenReady = initialPlayWhenReady;
    this.context = context;
    this.texture = texture;
    this.surface = new Surface(this.texture);
    this.outputFormatListener =  ofl;

    mVideoListeners.add(vl);
    mVideoListeners.add(this);

    mEventListeners.add(el);
    mAnalyticsListeners.add(al);

    mCustomBandwidthMeter = new CustomBandwidthMeter(DefaultBandwidthMeter.getSingletonInstance(context));
    mTrackSelector = new CustomTrackSelector(context, new AdaptiveTrackSelection.Factory());
    mTrackSelector.setParameters(mTrackSelector.buildUponParameters().setViewportSize(1280, 720, true));
    mLoadControl = new DefaultLoadControl.Builder()
      .setBufferDurationsMs(DefaultLoadControl.DEFAULT_MIN_BUFFER_MS, DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
        500,500)
      .createDefaultLoadControl();

    createPlayer(context);
  }

  public void setInitialPlayWhenReady(boolean value) {
    initialPlayWhenReady = value;
  }

  private void createPlayer(Context context) {
    System.out.println(hashCode() + " Native: TestExoPlayer.createPlayer");

    SimpleExoPlayer exo =
      new SimpleExoPlayer.Builder(context, new CustomRenderersFactory(context, outputFormatListener))
      .setLooper(MAIN_LOOPER)
      .setBandwidthMeter(mCustomBandwidthMeter)
      .setTrackSelector(mTrackSelector)
      .setLoadControl(mLoadControl)
      .build();

    player = exo;

    exo.setVideoSurface(surface);

    if (mVideoListeners != null)
      mVideoListeners.forEach((listener) -> exo.addListener(listener));

    if (mEventListeners != null)
      mEventListeners.forEach(exo::addListener);

    if (mAnalyticsListeners != null)
      mAnalyticsListeners.forEach(exo::addAnalyticsListener);

    setPlayWhenReady(initialPlayWhenReady);
  }

  public void recreate() {
    System.out.println(hashCode() + " Native: TestExoPlayer.recreate");
    dispose();
    createPlayer(context);
    if (mCurrentStream != null) accept(mCurrentStream);
  }

  @Override
  public void accept(StreamKey key) {
    System.out.println(hashCode() + " Native: TestExoPlayer.accept: " + key);
    mCurrentStream = key;
    prepareMediaSource(key.url);
    setFitMode(key.fitMode);
  }

  public void dispose() {
    System.out.println(hashCode() + " Native: TestExoPlayer.dispose");
    getExoPlayer().release();
    surface.release();
  }

  public void setPlayWhenReady(Boolean b) {
    System.out.println(hashCode() + " Native: TestExoPlayer.setPlayWhenReady: " + b);
    getExoPlayer().setPlayWhenReady(b);
  }

  public void setVolume(float value) {
    System.out.println(hashCode() + " Native: TestExoPlayer.setVolume: " + value);
    getExoPlayer().setVolume(value);
  }

  public void setFitMode(FitMode fitMode) {
  }

  public void seekTo(long value) {
    System.out.println(hashCode() + " Native: TestExoPlayer.seekTo: " + value);
    getExoPlayer().seekTo(value);
  }

  public void setPlaybackRate(float value) {
    System.out.println(hashCode() + " Native: TestExoPlayer.setPlaybackRate: " + value);
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
    //System.out.println(hashCode() + " BEFORE TestExoPlayer.setVideoQuality: "
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

          if (level != -1) { // set MIN
            mCustomBandwidthMeter.overrideBandwith(getMinBitrate(formats));
            mTrackSelector.publicInvalidate();
          } else { // set AUTO
            mCustomBandwidthMeter.overrideBandwith(null);
            mTrackSelector.publicInvalidate();
          }

          //System.out.println(hashCode() + "  Native: CustomExoPlayer.trackSelector - selected " + level);
          //System.out.println(hashCode() + " AFTER TestExoPlayer.setVideoQuality: "
          //  + level + " surface:" + mCurrentSurfaceSize + " video:" + mCurrentVideoSize);
        }
      }
    }
  }

  public void calcAndSetVideoQuality(Size size) {
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

          int bitrate = getClosestBitrateFromFormatsBySize(formats, size);
          mCustomBandwidthMeter.overrideBandwith(bitrate);
          mTrackSelector.publicInvalidate();

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

  private int getClosestBitrateFromFormatsBySize(List<Format> formats, Size size) {
    Format[] sorted = new Format[formats.size()];
    formats.toArray(sorted);
    Arrays.sort(sorted, (lhs, rhs) -> Integer.compare(lhs.bitrate, rhs.bitrate));

    int s1 = size.getWidth() + size.getHeight();
    int s2 = sorted[0].width + sorted[0].height;
    int distance = Math.abs(s2 - s1);
    int idx = 0;
    for(int c = 1; c < sorted.length; c++){
      int sc = sorted[c].width + sorted[c].height;
      int cdistance = Math.abs(sc - s1);
      if(cdistance < distance){
        idx = c;
        distance = cdistance;
      }
    }
    return sorted[idx].bitrate;
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
    //System.out.println(hashCode() + " Native: TestExoPlayer.getCurrentPosition");
    final long currentPosition = getExoPlayer().getCurrentPosition();
    Timeline.Window w1 = new Timeline.Window();

    if (getExoPlayer().getCurrentTimeline().isEmpty()) {
      System.out.println(hashCode() + "  Native: CustomExoPlayer: getCurrentPosition getCurrentTimeline() isEmpty");
      throw new IllegalStateException(" Native: CustomExoPlayer: getCurrentPosition getCurrentTimeline() isEmpty");
    }

    final long windowStartTimeMs = w1.windowStartTimeMs;
    return w1.getPositionInFirstPeriodMs() + currentPosition + windowStartTimeMs;
  }

  public long getTotalPosition() {
    //System.out.println(hashCode() + " Native: TestExoPlayer.getTotalPosition");
    Timeline.Window w1 = new Timeline.Window();

    if (getExoPlayer().getCurrentTimeline().isEmpty()) {
      System.out.println(hashCode() + "  Native: CustomExoPlayer: getTotalPosition getCurrentTimeline() isEmpty");
      throw new IllegalStateException(" Native: CustomExoPlayer: getTotalPosition getCurrentTimeline() isEmpty");
    }

    int currentWindowIndex = getExoPlayer().getCurrentWindowIndex();
    getExoPlayer().getCurrentTimeline().getWindow(currentWindowIndex, w1);
    final long posMs = w1.windowStartTimeMs + getExoPlayer().getCurrentPosition();

    if (posMs < 0) {
      System.out.println(hashCode() + "  Native: CustomExoPlayer: getTotalPosition posMs < 0: " + posMs);
      throw new IllegalStateException(" Native: CustomExoPlayer: getTotalPosition posMs < 0: " + posMs);
    }

    return posMs;
  }

  public long getDuration() {
    //System.out.println(hashCode() + " Native: TestExoPlayer.getDuration");
    return getExoPlayer().getDuration();
  }

  public int[] getVideoSize() {
    System.out.println(hashCode() + " Native: TestExoPlayer.getVideoSize");
    Format format = getExoPlayer().getVideoFormat();
    assert format != null;
    return new int[]{ format.width, format.height };
  }

  public void captureFrame(Function<Bitmap, Integer> result, Handler handler) {
    System.out.println(hashCode() + " Native: TestExoPlayer.captureFrame");
    int width, height;

    Format format = getExoPlayer().getVideoFormat();
    width = format != null ? format.width : mCurrentVideoSize.width;
    height = format != null ? format.height : mCurrentVideoSize.height;

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    PixelCopy.request(surface, bitmap, copyResult -> result.apply(bitmap), handler);
  }

  public void onWidgetSizeChanged(int width, int height) {
    if (mCurrentSurfaceSize == null || (mCurrentSurfaceSize.getWidth() != width || mCurrentSurfaceSize.getHeight() != height)) {
      System.out.println(hashCode() + " Native: TestExoPlayer.onWidgetSizeChanged: " + width + " x " + height +
        " ||| cur: " + mCurrentSurfaceSize);
      if (mCurrentSurfaceSize == null) {
        new Handler().postDelayed(() ->
          setSurfaceSize(new Size(width, height)), 1500);
      } else {
        setSurfaceSize(new Size(width, height));
      }
    }
  }

  private void setSurfaceSize(Size size) {
    mCurrentSurfaceSize = size;
    calcAndSetVideoQuality(mCurrentSurfaceSize);
  }

  @Override
  public void onSurfaceSizeChanged(int width, int height) {
    System.out.println(hashCode() + " Native: TestExoPlayer.onSurfaceSizeChanged: " + width + "x" + height);
    setSurfaceSize(new Size(width, height));
  }

  @Override
  public void onVideoSizeChanged(VideoSize videoSize) {
    System.out.println(hashCode() + "Native: TestExoPlayer.onVideoSizeChanged: " + videoSize.toString());
    mCurrentVideoSize = videoSize;
  }

  @Override
  public void onRenderedFirstFrame() {
    System.out.println(hashCode() + " Native: TestExoPlayer.onRenderedFirstFrame");
  }

  private void prepareMediaSource(String url) {
    System.out.println(hashCode() + " Native: TestExoPlayer.buildMediaSource: " + url);
    MediaSource source;
    String userAgent = Util.getUserAgent(context, USER_AGENT);
    if (url.contains("m3u8")) {
      source = new HlsMediaSource.Factory(new DefaultHttpDataSource.Factory())
        .setPlaylistParserFactory(new DefaultHlsPlaylistParserFactory())
        .createMediaSource(Uri.parse(url));
      getExoPlayer().prepare(source);
    } else {
      DefaultDataSourceFactory dataSourceFactory =
        new DefaultDataSourceFactory(context, userAgent);
      source = new ProgressiveMediaSource.Factory(dataSourceFactory)
        .createMediaSource(Uri.parse(url));
    }
    getExoPlayer().prepare(source);
  }

  private ExoPlayer getExoPlayer() {
    return player;
  }

}

