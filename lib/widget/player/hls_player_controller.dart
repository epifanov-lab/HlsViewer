import 'package:flutter/services.dart';
import 'package:hls_viewer/model/hls_resource.dart';
import 'package:hls_viewer/widget/player/hls_player_widget.dart';

class HlsPlayerController {
  static const messengerChannelName = 'com.epifanov.kostya.hls_viewer/platform_messenger';
  static const messengerChannel = MethodChannel(messengerChannelName);

  final MethodChannel _playerChannel;
  final int _id;

  final OnViewCreatedCallback onViewCreated;
  final OnRenderedFirstFrameCallback onRenderedFirstFrame;
  final OnPlaybackEndedCallback onPlaybackEnded;
  final OnProgressChangedCallback onProgressChangedCallback;
  final OnPlayerEventCallback onPlayerEventCallback;
  final OnPlayerExceptionCallback onPlayerExceptionCallback;
  final OnVideoResolutionReceived onVideoResolutionReceived;

  HlsPlayerController(int id, {
    required this.onViewCreated,
    required this.onRenderedFirstFrame,
    required this.onPlaybackEnded,
    required this.onProgressChangedCallback,
    required this.onPlayerEventCallback,
    required this.onPlayerExceptionCallback,
    required this.onVideoResolutionReceived,
  })  : _id = id,
        _playerChannel = MethodChannel('$playerPlatformViewName$id');

  Future<void> loadHls(HlsResourceModel resource) async {
    return _playerChannel.invokeMethod('loadHls', {
      'url': resource.url
    });
  }

  void setFitMode(fitMode) {
    //TODO
  }

  void setVideoQuality(quality) {
    //TODO
  }
}