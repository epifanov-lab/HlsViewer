import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:hls_viewer/model/hls_resource.dart';
import 'package:hls_viewer/widget/player/hls_player_widget.dart';
import 'package:hls_viewer/widget/player/video_events.dart';
import 'package:hls_viewer/widget/player/video_player_exceptions.dart';

const PLAYER_EVENT = "player_event";
const PLAYER_EXCEPTION = "player_exception";
const TYPE = "type", DATA = "data";

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

  bool? isFirstFrameRendered;

  HlsPlayerController(int id, {
    required this.onViewCreated,
    required this.onRenderedFirstFrame,
    required this.onPlaybackEnded,
    required this.onProgressChangedCallback,
    required this.onPlayerEventCallback,
    required this.onPlayerExceptionCallback,
    required this.onVideoResolutionReceived,
  })  : _id = id,
        _playerChannel = MethodChannel('$playerPlatformViewName$id') {
    _playerChannel.setMethodCallHandler((call) => _handleEvents(call));
  }

  Future _handleEvents(MethodCall call) {
    var arguments = call.arguments;
    switch (call.method) {
      case PLAYER_EVENT:
        print("VideoView $hashCode event: $arguments");
        final String type = arguments[TYPE];

        switch (type) {
          case VideoPlayerEvents.common_onHlsStreamEnded:
            onPlaybackEnded(this);
            break;

          case VideoPlayerEvents.common_onRenderedFirstFrame:
            isFirstFrameRendered = true;
            onRenderedFirstFrame(this);
            break;

          case VideoPlayerEvents.android_onVideoSizeChanged:
            var data = arguments[DATA];
            onVideoResolutionReceived(Size(data['width'] * 1.0, data['height'] * 1.0));
            break;

          case VideoPlayerEvents.common_onProgressChanged:
            var data = arguments[DATA];
            var currentTime = data['currentPosition'];
            onProgressChangedCallback(this, currentTime);
            break;

          default:
            onPlayerEventCallback(this, arguments);
        }
        break;

      case PLAYER_EXCEPTION:
        var type = '${arguments['type']}';
        var message = '${arguments['message']}';
        var stacktrace = '${arguments['stacktrace']}';
        _exception(type: type, message: message, stacktrace: stacktrace);
        break;

      default:
        print("VideoView $hashCode WTF ${call.method}: ${arguments}");
    }

    return Future.value();
  }

  Exception _exception({String? type, String? message, String? stacktrace}) {
    print("VideoView $hashCode exception: $type $message");
    VideoPlayerException exception = VideoPlayerException(type: type, message: message, data: null);
    onPlayerExceptionCallback(this, exception);
    return exception;
  }

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