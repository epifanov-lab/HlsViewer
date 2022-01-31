package com.epifanov.kostya.hls_viewer.player;

import java.util.Objects;


/**
 * @author Konstantin Epifanov
 * @since 07.02.2020
 */
public class StreamKey {
  public final String url;
  public final FitMode fitMode;

  public StreamKey(String url, FitMode fitMode) {
    this.url = url;
    this.fitMode = fitMode;
  }

  public StreamKey(String url) {
    this.url = url;
    fitMode = FitMode.COVER;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final StreamKey streamKey = (StreamKey) o;
    return Objects.equals(url, streamKey.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url);
  }
}

