package com.epifanov.kostya.hls_viewer.platform_view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;

import com.epifanov.kostya.hls_viewer.player.CustomExoPlayer;
import com.epifanov.kostya.hls_viewer.player.EventListener;
import com.epifanov.kostya.hls_viewer.player.FitMode;
import com.epifanov.kostya.hls_viewer.player.StreamKey;
import com.epifanov.kostya.hls_viewer.utils.CommonUtils;
import com.epifanov.kostya.hls_viewer.utils.schedulers.Schedulers;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.video.VideoSize;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.function.Supplier;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;
import reactor.core.publisher.Mono;

/**
 * @author Konstantin Epifanov
 * @since 18.02.2020
 */
public class PlayerPlatformView
  implements PlatformView, MethodChannel.MethodCallHandler,
  Player.Listener, EventListener, AnalyticsListener {

  private final String PLAYER_EVENT = "player_event";
  private final String PLAYER_EXCEPTION = "player_exception";
  private final String TYPE = "type";
  private final String MESSAGE = "message";
  private final String DATA = "data";
  private final String STACKTRACE = "stacktrace";
  private final String NOT_IMPLEMENTED = "not_implemented";

  private final String VIEW_TYPE = "VideoView";
  private final MethodChannel mChannel;

  private final CustomExoPlayer mCustomPlayer;

  private final Handler mMainHandler = new Handler(Looper.getMainLooper());

  private StreamKey mStreamKey;
  private boolean mIsFirstFrameRendered = false;
  private Timer mVideoProgressTimer;
  private long duration = 0;
  private boolean pauseOnPause = false;

  final Context context;
  final BinaryMessenger messenger;
  final int id;

  public PlayerPlatformView(Context context, BinaryMessenger messenger, Integer id) {
    this.context = context;
    this.messenger = messenger;
    this.id = id;

    mCustomPlayer = new CustomExoPlayer(context, this, this, this, true);
    mChannel = new MethodChannel(messenger, VIEW_TYPE + id);
    mChannel.setMethodCallHandler(this);
  }

  @Override
  public View getView() {
    return mCustomPlayer.mPlayerView;
  }

  @Override
  public void dispose() {
    System.out.println(hashCode() + " Native: PlayerPlatformView.dispose (internal)");
    runVideoProgressEmitter(false);
    mCustomPlayer.dispose();
  }

  private void dispose(MethodChannel.Result result) {
    System.out.println(hashCode() + " Native: PlayerPlatformView.dispose (external)");
    dispose();
    result.success(null);
  }

  private void pauseOnPause(MethodCall call) {
    boolean value = (boolean)call.arguments;
    pauseOnPause = value;
    mCustomPlayer.setInitialPlayWhenReady(!value);
  }

  @Override
  public void onMethodCall(MethodCall call, MethodChannel.Result result) {
    // System.out.println(hashCode() + " Native: PlayerPlatformView: onMethodCall " + call.method + " " + call.arguments);
    switch (call.method) {
      case "setPlayback": setPlayback(call, result); break;
      case "loadHls": loadHls(call, result); break;
      //case "loadWss": loadWss(call, result); break;
      case "setVolume": setVolume(call, result); break;
      case "setFitMode": setFitMode(call, result); break;
      case "seekTo": seekTo(call, result); break;
      case "setPlaybackRate": setPlaybackRate(call, result); break;
      case "setVideoQuality": setVideoQuality(call, result); break;
      case "getCurrentPosition": getCurrentPosition(result); break;
      case "getTotalPosition": getTotalPosition(result); break;
      case "getDuration": getDuration(result); break;
      case "getVideoSize": getVideoSize(result); break;
      case "captureFrame": captureFrame(call, result); break;
      case "dispose": dispose(result); break;
      case "pauseOnPause": pauseOnPause(call); break;
      default: result.notImplemented();
    }
  }

  private void loadHls(MethodCall call, MethodChannel.Result result) {
    //System.out.println(hashCode() + " TRY TO Native: PlayerPlatformView.loadHls: call = " + call.arguments);
    if (call.arguments instanceof Map) {
      Map map = (Map) call.arguments;
      String url = (String) map.get("url");
      if (url != null) {
        if (mStreamKey != null && mStreamKey.url.equals(url)) {
          result.success(null);
          return;
        }

        System.out.println(hashCode() + "Native: PlayerPlatformView.loadHls: call = " + call.arguments);
        FitMode fitMode = FitMode.obtain((String) map.get("fitMode"));
        mStreamKey = new StreamKey(url, fitMode); // TODO load hls with quality/track
        mCustomPlayer.accept(mStreamKey);
        result.success(null);
      } else result.error("0", hashCode() + " Native: PlayerPlatformView.loadHls: wrong args: " + map, null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.loadHls: wrong arg type! " + call.arguments, null);
  }

  /*private void loadWss(MethodCall call, MethodChannel.Result result) {
    // System.out.println(hashCode() + " Native: PlayerPlatformView.loadWss: call = " + call.arguments);
    if (call.arguments instanceof Map) {
      Map map = (Map) call.arguments;
      if (map.get("url") != null && map.get("token") != null && map.get("mediaId") != null) {
        //System.out.println(hashCode() + " Native: PlayerPlatformView: loadWss: $map");
        String url = (String) map.get("url");
        String token = (String) map.get("token");
        Number mediaId = (Number) map.get("mediaId");
        FitMode fitMode = FitMode.obtain((String) map.get("fitMode"));
        mStreamKey = new StreamKeyWss(url, token, mediaId, fitMode);
        mCustomPlayer.accept(mStreamKey);
        result.success(null);
      } else result.error("0", hashCode() + " Native: PlayerPlatformView.loadWss: some args are wrong: " + map.toString(), null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.loadWss: wrong arg type! " + call.arguments, null);
  }*/

  private void setPlayback(MethodCall call, MethodChannel.Result result) {
    System.out.println(hashCode() + " Native: PlayerPlatformView.setPlayback: call = " + call.arguments);
    if (call.arguments instanceof Boolean) {
      Boolean b = (Boolean) call.arguments;
      mCustomPlayer.setPlayWhenReady(b);
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.setPlayback: wrong args: " + call.arguments, null);
  }

  private void setVolume(MethodCall call, MethodChannel.Result result) {
    System.out.println(hashCode() + " Native: PlayerPlatformView.setVolume: call = " + call.arguments);
    if (call.arguments instanceof Double) {
      Double volume = (Double) call.arguments;
      mCustomPlayer.setVolume(volume.floatValue());
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.setVolume: wrong args: " + call.arguments, null);
  }

  private void setFitMode(MethodCall call, MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.setFitMode: call = " + call.arguments);
    if (!mIsFirstFrameRendered) {
      //result.error("0", hashCode() + " Native: PlayerPlatformView.setFitMode: setFitMode when first frame not rendered!", null);
      result.success(null);
    } else if (call.arguments instanceof String) {
      FitMode fitMode = FitMode.obtain((String) call.arguments);
      mCustomPlayer.setFitMode(fitMode);
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.setFitMode: wrong args: " + call.arguments, null);
  }

  private void seekTo(MethodCall call, MethodChannel.Result result) {
    if (call.arguments instanceof Number) {
      Number posMs = (Number) call.arguments;
      mCustomPlayer.seekTo(posMs.longValue());
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.seekTo: wrong args: " + call.arguments, null);
  }

  private void setPlaybackRate(MethodCall call, MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.setPlaybackRate: call = " + call.arguments);
    if (call.arguments instanceof Number) {
      Number posMs = (Number) call.arguments;
      mCustomPlayer.setPlaybackRate(posMs.floatValue());
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.seekTo: wrong args: " + call.arguments, null);
  }

  private void setVideoQuality(MethodCall call, MethodChannel.Result result) {
    //System.out.println(hashCode() + " TRY TO Native: PlayerPlatformView.setVideoQuality: call = " + call.arguments);
    if (!mIsFirstFrameRendered) {
      //result.error("0", hashCode() + " Native: PlayerPlatformView.setVideoQuality: setVideoQuality when first frame not rendered!", null);
      result.success(null);
    } else if (call.arguments instanceof Number) {
      Number level = (Number) call.arguments;
      mCustomPlayer.setVideoQuality(level.intValue());
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.setVideoQuality: wrong args: " + call.arguments, null);
  }

  private void getCurrentPosition(MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.getCurrentPosition: result = " + result);
    getThreadSafePlayerOperation(mCustomPlayer::getCurrentPosition)
      .subscribe(result::success, (e) -> result.error("-1", e.getMessage(), null));

    /*try {
      long pos = mTestPlayer.getCurrentPosition();
      result.success(pos);
    } catch (Exception e) {
      result.error("-1", e.getMessage(), null);
    }*/
  }

  private void getTotalPosition(MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.getTotalPosition: result = " + result);
    getThreadSafePlayerOperation(mCustomPlayer::getTotalPosition)
      .subscribe(result::success, (e) -> result.error("-1", e.getMessage(), null));

    /*try {
      long pos = mTestPlayer.getTotalPosition();
      result.success(pos);
    } catch (Exception e) {
      result.error("-1", e.getMessage(), null);
    }*/
  }

  private void getDuration(MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.getDuration: result = " + result);
    getThreadSafePlayerOperation(mCustomPlayer::getDuration)
      .subscribe(result::success, (e) -> result.error("-1", e.getMessage(), null));

    /*try {
      long duration = mTestPlayer.getDuration();
      result.success(duration);
    } catch (Exception e) {
      result.error("-1", e.getMessage(), null);
    }*/
  }

  private void getVideoSize(MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.getVideoSize: result = " + result);
    getThreadSafePlayerOperation(mCustomPlayer::getVideoSize)
      .map((o) -> (Integer[]) o)
      .map(sizeArray -> CommonUtils.hashMapOf(new Pair<>("width", sizeArray[0]), new Pair<>("height", sizeArray[1])))
      .subscribe(result::success);

    /*try {
      int[] sizes = mTestPlayer.getVideoSize();
      Map map = CommonUtils.hashMapOf(new Pair<>("width", sizes[0]), new Pair<>("height", sizes[1]));
      result.success(map);
    } catch (Exception e) {
      result.error("-1", e.getMessage(), null);
    }*/
  }

  @NotNull
  private Mono<Object> getThreadSafePlayerOperation(Supplier<Object> operation) {
    return Mono.just(new Object())
      .subscribeOn(Schedulers.MAIN_SCHEDULER)
      //.transform(Schedulers::work_main)
      .map((o) -> operation.get());
      //.transform(Schedulers::work_main);
  }

  private void captureFrame(MethodCall call, MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.captureFrame: call = " + call.arguments);
    /*if (call.arguments instanceof String) {
      try {
        Bitmap b = mCustomPlayer.captureFrame();
        if (b == null) {
          String err = hashCode() + " Native: PlayerPlatformView.captureFrame: view error! mSizes is null.";
          result.error("0", err, null); return;
        }
        File file = new File(call.arguments.toString());
        OutputStream stream = new FileOutputStream(file);
        b.compress(Bitmap.CompressFormat.PNG, 100, stream);
        result.success(null);
        b.recycle();
      } catch (Exception e) {
        result.error(e.getClass().getSimpleName(), e.getMessage(), null);
      }
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.captureFrame: wrong args: "
        + call.arguments, null);*/
  }

  //region VideoListener
  @Override
  public void onVideoSizeChanged(EventTime eventTime, VideoSize videoSize) {
    System.out.println(hashCode() + " Native: PlayerPlatformView, onVideoSizeChanged " +
      "width = " + videoSize.width + ", height = " + videoSize.height);

    mMainHandler.post(() -> {
      mChannel.invokeMethod(PLAYER_EVENT,
        CommonUtils.hashMapOf(
          new Pair<>(TYPE, "onVideoSizeChanged"),
          new Pair<>(DATA, CommonUtils.hashMapOf(
            new Pair<>("width", videoSize.width),
            new Pair<>("height", videoSize.height)
          ))));
    });
  }

  @Override
  public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
    System.out.println(hashCode() + " Native: PlayerPlatformView, VideoListener.onSurfaceSizeChanged: w" + width + " h" + height);
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT, CommonUtils.hashMapOf(
      new Pair<>(TYPE, "onSurfaceSizeChanged"),
      new Pair<>(DATA, CommonUtils.hashMapOf(
        new Pair<>("width", width),
        new Pair<>("height", height))))));
  }

  @Override
  public void onRenderedFirstFrame(EventTime eventTime, Object output, long renderTimeMs) {
    System.out.println("PlayerPlatformView.onRenderedFirstFrame");
    mMainHandler.post(this::sendOnRenderedFirstFrameEvent);
  }

  private void sendOnRenderedFirstFrameEvent() {
    System.out.println(hashCode() + " Native: PlayerPlatformView.sendOnRenderedFirstFrameEvent");
    if (!mIsFirstFrameRendered) {
      mIsFirstFrameRendered = true;
      mChannel.invokeMethod(PLAYER_EVENT, CommonUtils.hashMapOf(new Pair<>(TYPE, "onRenderedFirstFrame")));
    }
  }
  //endregion VideoListener


  @Override
  public void onPlayerError(EventTime eventTime, PlaybackException error) {
    System.out.println(hashCode() + " Native: PlayerPlatformView, EventListener.onPlayerError: " + error.getMessage());
    /*
    if ( isBehindLiveWindowException(error)
      || isPlaylistStuckException(error)
      || isUnexpectedIllegalArgumentException(error)
      || isRendererIllegalStateException(error)
      *//*|| isParserException(error)*//*) {
      mCustomPlayer.recreate();
    }

    Map<String, Object> args = CommonUtils.hashMapOf(new Pair<>("code", error.type));
    args.put(STACKTRACE, CommonUtils.stacktraceToString(error));

    String type = null;
    switch (error.type) {
      case ExoPlaybackException.TYPE_SOURCE:
        type = "TYPE_SOURCE";
        args.put(MESSAGE, error.getSourceException().getClass().getSimpleName());
        break;

      case ExoPlaybackException.TYPE_RENDERER:
        type = "TYPE_RENDERER";
        args.put(MESSAGE, error.getRendererException().getClass().getSimpleName());
        break;

      case ExoPlaybackException.TYPE_UNEXPECTED:
        type = "TYPE_UNEXPECTED";
        args.put(MESSAGE, error.getUnexpectedException().getClass().getSimpleName());
        break;

      case ExoPlaybackException.TYPE_REMOTE:
        type = "TYPE_REMOTE";
        break;
    }

    type += ' ' + error.getClass().getSimpleName();
    args.put(TYPE, type);
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EXCEPTION, args));
    */
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    Map<String, Object> args = CommonUtils.hashMapOf(new Pair<>("playWhenReady", playWhenReady),
      new Pair<>("state", playbackState));

    switch (playbackState) {
      case ExoPlayer.STATE_IDLE:
        args.put("state", "STATE_IDLE");
        break;

      case ExoPlayer.STATE_BUFFERING:
        if (playWhenReady) {
          mMainHandler.post(() -> mChannel.invokeMethod("onStalled", null));
        }
        args.put("state", "STATE_BUFFERING");
        break;

      case ExoPlayer.STATE_READY:
        notifyDuration();
        args.put("state", "STATE_READY");
        break;

      case ExoPlayer.STATE_ENDED:
        mMainHandler.post(() -> mChannel.invokeMethod("onEnd", null));
        mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT,
          CommonUtils.hashMapOf(new Pair<>(TYPE, "hlsStreamEnded"))));

        args.put("state", "STATE_ENDED");
        break;
    }

    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT,
      CommonUtils.hashMapOf(new Pair<>(TYPE, "onPlayerStateChanged"),
        new Pair<>(DATA, args))));
  }

  @Override
  public void onLoadingChanged(boolean isLoading) {
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT,
      CommonUtils.hashMapOf(new Pair<>(TYPE, "onLoadingChanged"),
        new Pair<>(DATA, CommonUtils.hashMapOf(new Pair<>("isLoading", isLoading)))
      )));
  }

  private void notifyDuration() {
    final long newDuration = mCustomPlayer.getDuration();
    if (newDuration != C.TIME_UNSET && duration == newDuration) {
      return;
    }
    duration = newDuration;
    mMainHandler.post(() -> mChannel.invokeMethod("onDuration", mCustomPlayer.getDuration()));
  }

  //region EventListener
  @Override
  public void onTimelineChanged(Timeline timeline, int reason) {
    mMainHandler.post(() -> {
      Map<String, Object> arg = CommonUtils.hashMapOf(
        new Pair<>("reason", reason),
        new Pair<>("other_data", NOT_IMPLEMENTED)
      );

      /*switch (reason) {
        case Player.TIMELINE_CHANGE_REASON_PREPARED:
          arg.put("reason_desc", "TIMELINE_CHANGE_REASON_PREPARED");
          break;

        case Player.TIMELINE_CHANGE_REASON_RESET:
          arg.put("reason_desc", "TIMELINE_CHANGE_REASON_RESET");
          break;

        case Player.TIMELINE_CHANGE_REASON_DYNAMIC:
          arg.put("reason_desc", "TIMELINE_CHANGE_REASON_DYNAMIC");
          break;

      }*/

      mChannel.invokeMethod(PLAYER_EVENT,
        CommonUtils.hashMapOf(new Pair<>(TYPE, "onTimelineChanged"),
        new Pair<>(DATA, arg)));
    });
  }

  @Override
  public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT, CommonUtils.hashMapOf(new Pair<>(TYPE, "onTracksChanged"),
      new Pair<>(DATA, NOT_IMPLEMENTED))));
  }

  @Override
  public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
    mMainHandler.post(() ->
      mChannel.invokeMethod(PLAYER_EVENT,
        CommonUtils.hashMapOf(new Pair<>(TYPE, "onPlaybackSuppressionReasonChanged"),
          new Pair<>(DATA, CommonUtils.hashMapOf(new Pair<>("reason", playbackSuppressionReason))))));
  }

  @Override
  public void onIsPlayingChanged(boolean isPlaying) {

    if (isPlaying) {
      // Active playback.
      mMainHandler.post(() -> mChannel.invokeMethod("onPlay", null));
    } else {
      if (pauseOnPause) {
        mCustomPlayer.setPlayWhenReady(false);
      }
      mMainHandler.post(() -> mChannel.invokeMethod("onPause", null));
      // Not playing because playback is paused, ended, suppressed, or the player
      // is buffering, stopped or failed.
    }

    runVideoProgressEmitter(isPlaying);

    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT,
      CommonUtils.hashMapOf(new Pair<>(TYPE, "onIsPlayingChanged"),
        new Pair<>(DATA, CommonUtils.hashMapOf(new Pair<>("isPlaying", isPlaying))))));
  }

  @Override
  public void onRepeatModeChanged(int repeatMode) {
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT,
      CommonUtils.hashMapOf(new Pair<>(TYPE, "onRepeatModeChanged"),
        new Pair<>(DATA, CommonUtils.hashMapOf(new Pair<>("repeatMode", repeatMode))))));
  }

  @Override
  public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT,
      CommonUtils.hashMapOf(new Pair<>(TYPE, "onShuffleModeEnabledChanged"),
        new Pair<>(DATA, CommonUtils.hashMapOf(new Pair<>("shuffleModeEnabled", shuffleModeEnabled))))));
  }

  @Override
  public void onPositionDiscontinuity(int reason) {
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT,
      CommonUtils.hashMapOf(new Pair<>(TYPE, "onPositionDiscontinuity"),
        new Pair<>(DATA, CommonUtils.hashMapOf(new Pair<>("reason", reason))))));
  }

  @Override
  public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT,
      CommonUtils.hashMapOf(new Pair<>(TYPE, "onPlaybackParametersChanged"),
        new Pair<>(DATA, CommonUtils.hashMapOf(
          new Pair<>("speed", playbackParameters.speed),
          new Pair<>("pitch", playbackParameters.pitch)
          //new Pair<>("skipSilence", playbackParameters.skipSilence)
        )))));
  }

  @Override
  public void onSeekProcessed() {
    final long currentPosition = mCustomPlayer.getCurrentPosition();
    final long bufferedEnd = mCustomPlayer.getTotalBufferedDuration() + currentPosition;
    final ArrayList<long[]> range = new ArrayList<long[]>(Collections.singleton(new long[]{currentPosition, bufferedEnd}));
    mMainHandler.post(() -> mChannel.invokeMethod("onLoadedRanges", range));
    mMainHandler.post(() -> mChannel.invokeMethod("onCurrentTime", currentPosition));
    mMainHandler.post(() -> mChannel.invokeMethod("onSeek", mCustomPlayer.getCurrentPosition()));
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT, CommonUtils.hashMapOf(new Pair<>(TYPE, "onSeekProcessed"))));
    mChannel.invokeMethod(PLAYER_EVENT,
            CommonUtils.hashMapOf(new Pair<>(TYPE, "onProgressChanged"),
                    new Pair<>(DATA, CommonUtils.hashMapOf(new Pair<>("currentPosition", currentPosition)))));
  }

  /*@Override
  public void onHlsMediaPlaylistParserEvent(Map<String, Object> map) {
    Map<String, Object> arg = CommonUtils.hashMapOf(new Pair<>(TYPE, "onHlsMediaPlaylistParserEvent"));
    map.forEach(arg::put);
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT, arg));
  }*/
  //endregion

  //region AnalyticsListener
  @Override
  public void onLoadCompleted(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
    if (mediaLoadData.dataType != C.DATA_TYPE_MEDIA) {
      return;
    }
    if (!mCustomPlayer.hasTimeline()) {
      return;
    }
    final long currentPosition = mCustomPlayer.getCurrentPosition();
    final long bufferedEnd = mCustomPlayer.getTotalBufferedDuration() + currentPosition;
    final ArrayList<long[]> range = new ArrayList<long[]>(Collections.singleton(new long[]{currentPosition, bufferedEnd}));
    mMainHandler.post(() -> mChannel.invokeMethod("onLoadedRanges", range));
  }

  //endregion

  private void runVideoProgressEmitter(boolean start) {
    System.out.println(hashCode() + " Native: PlayerPlatformView.runVideoProgressEmitter: start = " + start);
   /* if (!start && mVideoProgressTimer == null) return;
    else if (start && !mCustomPlayer.isVod()) return;
    else if (mVideoProgressTimer == null) mVideoProgressTimer = new Timer();
    if (start) {
      mVideoProgressTimer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            getThreadSafePlayerOperation(mCustomPlayer::getCurrentPosition)
              .map((o) -> (Long) o)
              .filter(pos -> pos > 0).subscribe((currentPosition) -> {
              mChannel.invokeMethod("onCurrentTime", currentPosition);
              mChannel.invokeMethod(PLAYER_EVENT,
                      CommonUtils.hashMapOf(new Pair<>(TYPE, "onProgressChanged"),
                              new Pair<>(DATA, CommonUtils.hashMapOf(new Pair<>("currentPosition", currentPosition)))));
            });
          }
        }, 0, 500);
    } else {
      mVideoProgressTimer.cancel();
      mVideoProgressTimer = null;
    }*/
  }

}
