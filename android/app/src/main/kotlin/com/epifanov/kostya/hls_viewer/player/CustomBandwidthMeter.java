package com.epifanov.kostya.hls_viewer.player;

import android.os.Handler;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.TransferListener;

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
