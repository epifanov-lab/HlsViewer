package com.epifanov.kostya.hls_viewer.player;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;

class CustomRenderersFactory extends DefaultRenderersFactory {

  CustomExoPlayer.OutputFormatListener outputFormatListener;

  public CustomRenderersFactory(Context context, CustomExoPlayer.OutputFormatListener outputFormatListener) {
    super(context);
    this.outputFormatListener = outputFormatListener;
  }

  @Override
  protected void buildVideoRenderers(Context context,
                                     int extensionRendererMode,
                                     MediaCodecSelector mediaCodecSelector,
                                     boolean enableDecoderFallback,
                                     Handler eventHandler,
                                     VideoRendererEventListener eventListener,
                                     long allowedVideoJoiningTimeMs,
                                     ArrayList<Renderer> out) {
    out.add(
            new CustomMediaCodecVideoRenderer(
                    outputFormatListener,
                    context,
                    mediaCodecSelector,
                    allowedVideoJoiningTimeMs,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY));
  }

}
