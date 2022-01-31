package com.epifanov.kostya.hls_viewer.player;

import com.google.android.exoplayer2.Player;

import java.util.Map;

public interface EventListener extends Player.EventListener {

  default void onHlsMediaPlaylistParserEvent(Map<String, Object> map) {}

}
