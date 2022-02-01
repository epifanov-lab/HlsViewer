package com.epifanov.kostya.hls_viewer.player;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistTracker;

public class PlayerUtils {

  public static boolean isBehindLiveWindowException(ExoPlaybackException e) {
    if (e.type != ExoPlaybackException.TYPE_SOURCE) {
      return false;
    }

    Throwable cause = e.getSourceException();
    while (cause != null) {
      if (cause instanceof BehindLiveWindowException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  public static boolean isPlaylistStuckException(ExoPlaybackException e) {
    if (e.type != ExoPlaybackException.TYPE_SOURCE) {
      return false;
    }

    Throwable cause = e.getSourceException();
    while (cause != null) {
      if (cause instanceof HlsPlaylistTracker.PlaylistStuckException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  public static boolean isParserException(ExoPlaybackException e) {
    if (e.type != ExoPlaybackException.TYPE_SOURCE) {
      return false;
    }

    Throwable cause = e.getSourceException();
    while (cause != null) {
      if (cause instanceof ParserException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  public static boolean isUnexpectedIllegalArgumentException(ExoPlaybackException e) {
    /* //TODO выяснить причину.
    IGraphicBufferProducer::setBufferCount(2) returned Invalid argument
    E/ACodec  (26544): native_window_set_buffer_count failed: Invalid argument (22)
    E/ExoPlayerImplInternal(26544): Internal runtime error.
    E/ExoPlayerImplInternal(26544): java.lang.IllegalArgumentException
    E/ExoPlayerImplInternal(26544):   at android.media.MediaCodec.native_setSurface(Native Method)
    E/ExoPlayerImplInternal(26544):   at android.media.MediaCodec.setOutputSurface(MediaCodec.java:2042)
    */

    if (e.type != ExoPlaybackException.TYPE_UNEXPECTED) {
      return false;
    }

    Throwable cause = e.getUnexpectedException();
    while (cause != null) {
      if (cause instanceof IllegalArgumentException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  public static boolean isRendererIllegalStateException(ExoPlaybackException e) {
    /* //TODO выяснить причину.
    E/ExoPlayerImplInternal: Renderer error: index=0, type=video, format=Format(0, null, null, video/avc, null, 1000000, null, [360, 640, -1.0], [-1, -1]), rendererSupport=YES
    com.google.android.exoplayer2.ExoPlaybackException: java.lang.IllegalStateException
        at com.google.android.exoplayer2.BaseRenderer.createRendererException(BaseRenderer.java:359)
        at com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.render(MediaCodecRenderer.java:723)
        at com.google.android.exoplayer2.ExoPlayerImplInternal.doSomeWork(ExoPlayerImplInternal.java:599)
        at com.google.android.exoplayer2.ExoPlayerImplInternal.handleMessage(ExoPlayerImplInternal.java:329)
        at android.os.Handler.dispatchMessage(Handler.java:103)
        at android.os.Looper.loop(Looper.java:237)
        at android.os.HandlerThread.run(HandlerThread.java:67)
    */

    if (e.type != ExoPlaybackException.TYPE_RENDERER) {
      return false;
    }

    Throwable cause = e.getRendererException();
    while (cause != null) {
      if (cause instanceof IllegalStateException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

}
