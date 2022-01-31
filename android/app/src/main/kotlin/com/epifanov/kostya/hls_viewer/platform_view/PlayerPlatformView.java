package com.epifanov.kostya.hls_viewer.platform_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Pair;

import com.epifanov.kostya.hls_viewer.player.CustomExoPlayer;
import com.epifanov.kostya.hls_viewer.player.EventListener;
import com.epifanov.kostya.hls_viewer.player.FitMode;
import com.epifanov.kostya.hls_viewer.player.StreamKey;
import com.epifanov.kostya.hls_viewer.utils.CommonUtils;
import com.epifanov.kostya.hls_viewer.utils.schedulers.Schedulers;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
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
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.function.Supplier;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import reactor.core.publisher.Mono;

import static com.epifanov.kostya.hls_viewer.player.PlayerUtils.isBehindLiveWindowException;
import static com.epifanov.kostya.hls_viewer.player.PlayerUtils.isPlaylistStuckException;
import static com.epifanov.kostya.hls_viewer.player.PlayerUtils.isRendererIllegalStateException;
import static com.epifanov.kostya.hls_viewer.player.PlayerUtils.isUnexpectedIllegalArgumentException;
import static com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED;
import static com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE;

/**
 * @author Konstantin Epifanov
 * @since 18.02.2020
 */
public class PlayerPlatformView
  implements MethodChannel.MethodCallHandler, Player.Listener,
    EventListener, AnalyticsListener, CustomExoPlayer.OutputFormatListener {

  private final String PLAYER_EVENT = "player_event";
  private final String PLAYER_EXCEPTION = "player_exception";
  private final String TYPE = "type";
  private final String MESSAGE = "message";
  private final String DATA = "data";
  private final String STACKTRACE = "stacktrace";
  private final String NOT_IMPLEMENTED = "not_implemented";

  private final String VIEW_TYPE = "VideoView";
  private final MethodChannel mChannel;

  private final CustomExoPlayer mTestPlayer;

  private final Handler mMainHandler = new Handler(Looper.getMainLooper());
  private HandlerThread captureFrameThread = new HandlerThread("captureFrameThread");
  private StreamKey mStreamKey;
  private boolean mIsFirstFrameRendered = false;
  private Timer mVideoProgressTimer;
  private long duration = 0;
  private boolean pauseOnPause = false;

  final Context context;
  final BinaryMessenger messenger;
  final long id;
  final SurfaceTexture texture;

  public PlayerPlatformView(Context context, BinaryMessenger messenger, long id, SurfaceTexture texture) {
    this.context = context;
    this.messenger = messenger;
    this.id = id;
    this.texture = texture;
    captureFrameThread.start();

    mTestPlayer = new CustomExoPlayer(context, this, this, this, this, true, texture);
    mChannel = new MethodChannel(messenger, VIEW_TYPE + id);
    mChannel.setMethodCallHandler(this);
  }

  public void dispose() {
    System.out.println(hashCode() + " Native: PlayerPlatformView.dispose (internal)");
    runVideoProgressEmitter(false);
    mTestPlayer.dispose();
  }

  private void dispose(MethodChannel.Result result) {
    System.out.println(hashCode() + " Native: PlayerPlatformView.dispose (external)");
    dispose();
    result.success(null);
  }

  private void pauseOnPause(MethodCall call) {
    boolean value = (boolean)call.arguments;
    pauseOnPause = value;
    mTestPlayer.setInitialPlayWhenReady(!value);
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
      case "captureFrame": captureFrame(call, result);break;
      case "dispose": dispose(result); break;
      case "pauseOnPause": pauseOnPause(call); break;
      case "onWidgetSizeChanged": onWidgetSizeChanged(call); break;
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
        mTestPlayer.accept(mStreamKey);
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
        mTestPlayer.accept(mStreamKey);
        result.success(null);
      } else result.error("0", hashCode() + " Native: PlayerPlatformView.loadWss: some args are wrong: " + map.toString(), null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.loadWss: wrong arg type! " + call.arguments, null);
  }*/

  private void setPlayback(MethodCall call, MethodChannel.Result result) {
    System.out.println(hashCode() + " Native: PlayerPlatformView.setPlayback: call = " + call.arguments);
    if (call.arguments instanceof Boolean) {
      Boolean b = (Boolean) call.arguments;
      mTestPlayer.setPlayWhenReady(b);
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.setPlayback: wrong args: " + call.arguments, null);
  }

  private void setVolume(MethodCall call, MethodChannel.Result result) {
    System.out.println(hashCode() + " Native: PlayerPlatformView.setVolume: call = " + call.arguments);
    if (call.arguments instanceof Double) {
      Double volume = (Double) call.arguments;
      mTestPlayer.setVolume(volume.floatValue());
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
      mTestPlayer.setFitMode(fitMode);
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.setFitMode: wrong args: " + call.arguments, null);
  }

  private void onWidgetSizeChanged(MethodCall call) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.onWidgetSizeChanged: call = " + call.arguments);
    if (call.arguments instanceof Map) {
      int width = (int) Math.round((double) ((Map) call.arguments).get("width"));
      int height = (int) Math.round((double) ((Map) call.arguments).get("height"));
      mTestPlayer.onWidgetSizeChanged(width, height);
    }
  }

  private void seekTo(MethodCall call, MethodChannel.Result result) {
    if (call.arguments instanceof Number) {
      Number posMs = (Number) call.arguments;
      mTestPlayer.seekTo(posMs.longValue());
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.seekTo: wrong args: " + call.arguments, null);
  }

  private void setPlaybackRate(MethodCall call, MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.setPlaybackRate: call = " + call.arguments);
    if (call.arguments instanceof Number) {
      Number posMs = (Number) call.arguments;
      mTestPlayer.setPlaybackRate(posMs.floatValue());
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
      mTestPlayer.setVideoQuality(level.intValue());
      result.success(null);
    } else result.error("0", hashCode() + " Native: PlayerPlatformView.setVideoQuality: wrong args: " + call.arguments, null);
  }

  private void getCurrentPosition(MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.getCurrentPosition: result = " + result);
    getThreadSafePlayerOperation(mTestPlayer::getCurrentPosition)
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
    getThreadSafePlayerOperation(mTestPlayer::getTotalPosition)
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
    getThreadSafePlayerOperation(mTestPlayer::getDuration)
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
    getThreadSafePlayerOperation(mTestPlayer::getVideoSize)
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

  private void reportSuccessFromMain(MethodChannel.Result result, @Nullable Object value) {
    mMainHandler.post(new Runnable() {
      @Override
      public void run() {
        result.success(value);
      }
    });
  }

  private void reportErrorFromMain(MethodChannel.Result result, String errorCode,
                                   @androidx.annotation.Nullable String errorMessage,
                                   @androidx.annotation.Nullable Object errorDetails) {
    mMainHandler.post(new Runnable() {
      @Override
      public void run() {
        result.error(errorCode, errorMessage, errorDetails);
      }
    });
  }

  private void captureFrame(MethodCall call, MethodChannel.Result result) {
    //System.out.println(hashCode() + " Native: PlayerPlatformView.captureFrame: call = " + call.arguments);
    if (call.arguments instanceof String) {
      try {
        mTestPlayer.captureFrame(new Function<Bitmap, Integer>() {
          @Override
          public Integer apply(Bitmap b) {
            if (b == null) {
              String err = hashCode() + " Native: PlayerPlatformView.captureFrame: view error! mSizes is null.";
              reportErrorFromMain(result, "0", err, null);
              return 1;
            }
            File file = new File(call.arguments.toString());
            try {
              OutputStream stream = new FileOutputStream(file);
              b.compress(Bitmap.CompressFormat.PNG, 100, stream);
              reportSuccessFromMain(result, null);
              b.recycle();
            } catch (Exception e) {
              reportErrorFromMain(result, e.getClass().getSimpleName(), e.getMessage(), null);
            }
            return 0;
          }
        }, new Handler(captureFrameThread.getLooper()));

      } catch (Exception e) {
        reportErrorFromMain(result, e.getClass().getSimpleName(), e.getMessage(), null);
      }
    } else {
      reportErrorFromMain(result, "0", hashCode() + " Native: PlayerPlatformView.captureFrame: wrong args: "
              + call.arguments, null);
    }
  }

  //region TestExoPlayer.OutputFormatListener

  private static final String KEY_CROP_LEFT = "crop-left";
  private static final String KEY_CROP_RIGHT = "crop-right";
  private static final String KEY_CROP_BOTTOM = "crop-bottom";
  private static final String KEY_CROP_TOP = "crop-top";

  @Override
  public void onOutputFormatChanged(MediaFormat format) {
    System.out.println(hashCode() + " TestExoPlayer.onOutputFormatChanged: " + format);
    final int width = format.getInteger(MediaFormat.KEY_WIDTH);
    final int height = format.getInteger(MediaFormat.KEY_HEIGHT);

    boolean hasCrop =
            format.containsKey(KEY_CROP_RIGHT)
                    && format.containsKey(KEY_CROP_LEFT)
                    && format.containsKey(KEY_CROP_BOTTOM)
                    && format.containsKey(KEY_CROP_TOP);
    final int croppedWidth =
            hasCrop
                    ? format.getInteger(KEY_CROP_RIGHT)
                    - format.getInteger(KEY_CROP_LEFT)
                    + 1
                    : width;
    final int croppedHeight =
            hasCrop
                    ? format.getInteger(KEY_CROP_BOTTOM)
                    - format.getInteger(KEY_CROP_TOP)
                    + 1
                    : height;
    mMainHandler.post(() -> {
      mChannel.invokeMethod(PLAYER_EVENT,
        CommonUtils.hashMapOf(
          new Pair<>(TYPE, "onCropData"),
          new Pair<>(DATA, CommonUtils.hashMapOf(
            new Pair<>("width", width),
            new Pair<>("height", height),
            new Pair<>("croppedWidth", croppedWidth),
            new Pair<>("croppedHeight", croppedHeight)))
        ));
    });
  }

  //endregion TestExoPlayer.OutputFormatListener
  //region VideoListener
  @Override
  public void onVideoSizeChanged(VideoSize videoSize) {
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
  public void onSurfaceSizeChanged(int width, int height) {
    System.out.println(hashCode() + " Native: PlayerPlatformView, VideoListener.onSurfaceSizeChanged: w" + width + " h" + height);
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT, CommonUtils.hashMapOf(
      new Pair<>(TYPE, "onSurfaceSizeChanged"),
      new Pair<>(DATA, CommonUtils.hashMapOf(
        new Pair<>("width", width),
        new Pair<>("height", height))))));
  }

  @Override
  public void onRenderedFirstFrame() {
    System.out.println("PlayerPlatformView.onRenderedFirstFrame");
    mMainHandler.post(this::sendOnRenderedFirstFrameEvent);
  }

  private void sendOnRenderedFirstFrameEvent() {
    System.out.println(hashCode() + " Native: PlayerPlatformView.sendOnRenderedFirstFrameEvent");
    mIsFirstFrameRendered = true;
    mChannel.invokeMethod(PLAYER_EVENT, CommonUtils.hashMapOf(new Pair<>(TYPE, "onRenderedFirstFrame")));
  }
  //endregion VideoListener


  @Override
  public void onPlayerError(PlaybackException error) {
    System.out.println(hashCode() + " Native: PlayerPlatformView, EventListener.onPlayerError: " + error.getMessage());

    ExoPlaybackException exoError = (ExoPlaybackException) error;

    if ( isBehindLiveWindowException(exoError)
      || isPlaylistStuckException(exoError)
      || isUnexpectedIllegalArgumentException(exoError)
      || isRendererIllegalStateException(exoError)
      /*|| isParserException(error)*/) {
      mTestPlayer.recreate();
    }

    Map<String, Object> args = CommonUtils.hashMapOf(new Pair<>("code", (exoError).type));
    args.put(STACKTRACE, CommonUtils.stacktraceToString(error));

    String type = null;
    switch ((exoError).type) {
      case ExoPlaybackException.TYPE_SOURCE:
        type = "TYPE_SOURCE";
        args.put(MESSAGE, (exoError).getSourceException().getClass().getSimpleName());
        break;

      case ExoPlaybackException.TYPE_RENDERER:
        type = "TYPE_RENDERER";
        args.put(MESSAGE, (exoError).getRendererException().getClass().getSimpleName());
        break;

      case ExoPlaybackException.TYPE_UNEXPECTED:
        type = "TYPE_UNEXPECTED";
        args.put(MESSAGE, (exoError).getUnexpectedException().getClass().getSimpleName());
        break;

      case ExoPlaybackException.TYPE_REMOTE:
        type = "TYPE_REMOTE";
        break;
    }

    type += ' ' + error.getClass().getSimpleName();
    args.put(TYPE, type);
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EXCEPTION, args));
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
    final long newDuration = mTestPlayer.getDuration();
    if (newDuration != C.TIME_UNSET && duration == newDuration) {
      return;
    }
    duration = newDuration;
    mMainHandler.post(() -> mChannel.invokeMethod("onDuration", mTestPlayer.getDuration()));
  }

  //region EventListener
  @Override
  public void onTimelineChanged(@NotNull Timeline timeline, int reason) {
    mMainHandler.post(() -> {
      Map<String, Object> arg = CommonUtils.hashMapOf(
        new Pair<>("reason", reason),
        new Pair<>("other_data", NOT_IMPLEMENTED)
      );

      switch (reason) {
        case TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED:
          arg.put("reason_desc", "TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED");
          break;

        case TIMELINE_CHANGE_REASON_SOURCE_UPDATE:
          arg.put("reason_desc", "TIMELINE_CHANGE_REASON_SOURCE_UPDATE");
          break;

      }

      mChannel.invokeMethod(PLAYER_EVENT, CommonUtils.hashMapOf(new Pair<>(TYPE, "onTimelineChanged"),
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
        mTestPlayer.setPlayWhenReady(false);
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
    final long currentPosition = mTestPlayer.getCurrentPosition();
    final long bufferedEnd = mTestPlayer.getTotalBufferedDuration() + currentPosition;
    final ArrayList<long[]> range = new ArrayList<long[]>(Collections.singleton(new long[]{currentPosition, bufferedEnd}));
    mMainHandler.post(() -> mChannel.invokeMethod("onLoadedRanges", range));
    mMainHandler.post(() -> mChannel.invokeMethod("onCurrentTime", currentPosition));
    mMainHandler.post(() -> mChannel.invokeMethod("onSeek", mTestPlayer.getCurrentPosition()));
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT, CommonUtils.hashMapOf(new Pair<>(TYPE, "onSeekProcessed"))));
    mChannel.invokeMethod(PLAYER_EVENT,
            CommonUtils.hashMapOf(new Pair<>(TYPE, "onProgressChanged"),
                    new Pair<>(DATA, CommonUtils.hashMapOf(new Pair<>("currentPosition", currentPosition)))));
  }

  @Override
  public void onHlsMediaPlaylistParserEvent(Map<String, Object> map) {
    Map<String, Object> arg = CommonUtils.hashMapOf(new Pair<>(TYPE, "onHlsMediaPlaylistParserEvent"));
    map.forEach(arg::put);
    mMainHandler.post(() -> mChannel.invokeMethod(PLAYER_EVENT, arg));
  }
  //endregion

  //region AnalyticsListener
  @Override
  public void onLoadCompleted(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
    if (mediaLoadData.dataType != C.DATA_TYPE_MEDIA) {
      return;
    }
    if (!mTestPlayer.hasTimeline()) {
      return;
    }
    final long currentPosition = mTestPlayer.getCurrentPosition();
    final long bufferedEnd = mTestPlayer.getTotalBufferedDuration() + currentPosition;
    final ArrayList<long[]> range = new ArrayList<long[]>(Collections.singleton(new long[]{currentPosition, bufferedEnd}));
    mMainHandler.post(() -> mChannel.invokeMethod("onLoadedRanges", range));
  }
  //endregion

  private void runVideoProgressEmitter(boolean start) {
    System.out.println(hashCode() + " Native: PlayerPlatformView.runVideoProgressEmitter: start = " + start);
    if (!start && mVideoProgressTimer == null) return;
    else if (mVideoProgressTimer == null) mVideoProgressTimer = new Timer();
    if (start) {
      mVideoProgressTimer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            getThreadSafePlayerOperation(mTestPlayer::getCurrentPosition)
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
    }
  }

}
