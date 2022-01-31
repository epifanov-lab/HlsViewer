package com.epifanov.kostya.hls_viewer.player;

import android.content.Context;

import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;

class CustomTrackSelector extends DefaultTrackSelector {

  public CustomTrackSelector(Context context, AdaptiveTrackSelection.Factory factory) {
    super(context, factory);
  }

  public void publicInvalidate() {
    super.invalidate();
  }
}
