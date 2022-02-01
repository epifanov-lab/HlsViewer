import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:hls_viewer/widget/player/hls_player_controller.dart';
import 'package:hls_viewer/widget/player/video_player_exceptions.dart';

const playerPlatformViewName = 'VideoView';

typedef OnViewCreatedCallback = void Function(HlsPlayerController controller);
typedef OnRenderedFirstFrameCallback = void Function(HlsPlayerController controller);
typedef OnPlaybackEndedCallback = void Function(HlsPlayerController controller);
typedef OnProgressChangedCallback = void Function(HlsPlayerController controller, int currentTime);
typedef OnPlayerEventCallback = void Function(HlsPlayerController controller, dynamic data);
typedef OnPlayerExceptionCallback = void Function(HlsPlayerController controller, VideoPlayerException exception);
typedef OnVideoResolutionReceived = void Function(Size resolution);

class HlsPlayerWidget extends StatelessWidget {

  final OnViewCreatedCallback onViewCreated;
  final OnRenderedFirstFrameCallback onRenderedFirstFrame;
  final OnPlaybackEndedCallback onPlaybackEnded;
  final OnProgressChangedCallback onProgressChangedCallback;
  final OnPlayerEventCallback onPlayerEventCallback;
  final OnPlayerExceptionCallback onPlayerExceptionCallback;
  final OnVideoResolutionReceived onVideoResolutionReceived;

  HlsPlayerController? _controller;

  HlsPlayerWidget({
    Key? key,
    required this.onViewCreated,
    required this.onRenderedFirstFrame,
    required this.onPlaybackEnded,
    required this.onProgressChangedCallback,
    required this.onPlayerEventCallback,
    required this.onPlayerExceptionCallback,
    required this.onVideoResolutionReceived,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return _playerPlatformView();
  }

  Widget _playerPlatformView() {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        viewType: playerPlatformViewName,
        onPlatformViewCreated: (int id) => _onPlatformViewCreated(id),
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return Text('$defaultTargetPlatform is not yet supported by the video view plugin');
      /*return UiKitView(
        viewType: playerPlatformViewName,
        onPlatformViewCreated: (int id) => _onPlatformViewCreated(id),
      );*/
    } else {
      return Text('$defaultTargetPlatform is not yet supported by the video view plugin');
    }
  }

  void _onPlatformViewCreated(int id) {

    _controller = HlsPlayerController(
        id,
        onViewCreated: onViewCreated,
        onRenderedFirstFrame: onRenderedFirstFrame,
        onPlaybackEnded: onPlaybackEnded,
        onProgressChangedCallback: onProgressChangedCallback,
        onPlayerEventCallback: onPlayerEventCallback,
        onPlayerExceptionCallback: onPlayerExceptionCallback,
        onVideoResolutionReceived: onVideoResolutionReceived,
    );

    //_controller?.setFitMode(fitMode);
    //_controller?.setVideoQuality(quality);

    onViewCreated.call(_controller!);
  }

}
