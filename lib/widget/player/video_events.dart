
class VideoPlayerEvents {
  static const String common_onHlsStreamEnded = "hlsStreamEnded";
  static const String common_onRenderedFirstFrame = "onRenderedFirstFrame";   /// Called when a frame is rendered for the first time.
  static const String common_onProgressChanged = "onProgressChanged";         /// Player progress changed. data: currentPosition, (only IOS) currentPositionPercent.

  /// Hls playlist parser meets required MS tag, packaged it to Map<String, Object> and send it as event.
  ///   key: start_offset, value: int (Long) #EXT-X-START-OFFSET tag value
  ///   key: fragment_duration, value: int (Long) #EXT-X-DATERANGE duration subtag value
  static const String common_onHlsMediaPlaylistParserEvent = "onHlsMediaPlaylistParserEvent";

  static const String ios_endTime = "endTime";                                /// Player has ended playing item
  static const String ios_playBackStalled = "playBackStalled";                /// Player loading new data. please wait a bit
  static const String ios_itemTimeJumped = "itemTimeJumped";                  /// Some lags happens. no problems
  static const String ios_failedToPlayToEndTime = "failedToPlayToEndTime";    /// Player seems broken
  static const String ios_assetDurationDidChange = "assetDurationDidChange";  /// Appended some new data
  static const String ios_newErrorLogEntry = "newErrorLogEntry";              /// Some error happened

  static const String android_onVideoSizeChanged = "onVideoSizeChanged";                    /// Change in the size of the video being rendered.
  static const String android_onSurfaceSizeChanged = "onSurfaceSizeChanged";                /// Change in the size of the surface onto which the video is being rendered.
  static const String android_onPlayerError = "onPlayerError";                              /// Called when an error occurs.
  static const String android_onPlayerStateChanged = "onPlayerStateChanged";                /// Called when the value returned from either #getPlayWhenReady() or #getPlaybackState() changes.
  static const String android_onLoadingChanged = "onLoadingChanged";                        /// Called when the player starts or stops loading the source.
  static const String android_onTimelineChanged = "onTimelineChanged";                      /// Called when the timeline and/or manifest has been refreshed.
  static const String android_onTracksChanged = "onTracksChanged";                          /// Called when the available or selected tracks change.
  static const String android_onIsPlayingChanged = "onIsPlayingChanged";                    /// Called when the value of #isPlaying() changes.
  static const String android_onRepeatModeChanged = "onRepeatModeChanged";                  /// Called when the value of #getRepeatMode() changes.
  static const String android_onSeekProcessed = "onSeekProcessed";                          /// Called when all pending seek requests have been processed by the player.
  static const String android_onPositionDiscontinuity = "onPositionDiscontinuity";          /// Called when a position discontinuity occurs without a change to the timeline.
  static const String android_onShuffleModeEnabledChanged = "onShuffleModeEnabledChanged";  /// Called when the value of #getShuffleModeEnabled() changes.
  static const String android_onPlaybackParametersChanged = "onPlaybackParametersChanged";  /// Called when the current playback parameters change.
  static const String android_onPlaybackSuppressionReasonChanged = "onPlaybackSuppressionReasonChanged"; /// Called when the value returned from #getPlaybackSuppressionReason() changes.
}