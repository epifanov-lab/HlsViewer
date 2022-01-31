package com.epifanov.kostya.hls_viewer.player;

public enum FitMode {
  COVER, CONTAIN;

  public static FitMode obtain(String str) {
    if (CONTAIN.name().equalsIgnoreCase(str)) return CONTAIN;
    else return COVER;
  }
}
