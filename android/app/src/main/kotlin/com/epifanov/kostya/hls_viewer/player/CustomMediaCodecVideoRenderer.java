package com.epifanov.kostya.hls_viewer.player;

import android.content.Context;
import android.media.MediaFormat;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

class CustomMediaCodecVideoRenderer extends MediaCodecVideoRenderer {

  CustomExoPlayer.OutputFormatListener outputFormatListener;

  public CustomMediaCodecVideoRenderer(CustomExoPlayer.OutputFormatListener outputFormatListener,
                                       Context context,
                                       MediaCodecSelector mediaCodecSelector,
                                       long allowedJoiningTimeMs,
                                       boolean enableDecoderFallback,
                                       @Nullable Handler eventHandler,
                                       @Nullable VideoRendererEventListener eventListener,
                                       int maxDroppedFramesToNotify) {
    super(context, mediaCodecSelector, allowedJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, maxDroppedFramesToNotify);
    this.outputFormatListener = outputFormatListener;
  }

  @Override
  protected void onOutputFormatChanged(Format format, MediaFormat outputMediaFormat) {
    outputFormatListener.onOutputFormatChanged(outputMediaFormat);
    super.onOutputFormatChanged(format, outputMediaFormat);
  }

}
