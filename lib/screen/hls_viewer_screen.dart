import 'package:flutter/material.dart';
import 'package:hls_viewer/model/hls_resource.dart';
import 'package:hls_viewer/utils/screen_transitions.dart';
import 'package:hls_viewer/widget/player/hls_player_widget.dart';

class HlsViewerScreen extends StatelessWidget {
  const HlsViewerScreen({Key? key}) : super(key: key);

  final HlsResourceModel testResource = const HlsResourceModel(
    title: 'TEST TITLE',
    description: 'TEST TITLE',
    url: 'https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8',
    preview: 'TEST PREVIEW URL',
  );

  static Route route() {
    return ScreenTransitions.getTransition(
        ScreenTransitionType.fadeIn,
        const RouteSettings(name: "HlsViewerScreen"),
        const HlsViewerScreen()
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      height: double.infinity,
      child: HlsPlayerWidget(
        key: const ValueKey('HlsPlayerWidget'),
        onViewCreated: (controller) {
          print('HlsPlayerWidget.onViewCreated');
          controller.loadHls(testResource);
        },
        onRenderedFirstFrame: (controller) {
          print('HlsPlayerWidget.onRenderedFirstFrame');
        },
        onPlaybackEnded: (controller) {
          print('HlsPlayerWidget.onPlaybackEnded');
        },
        onProgressChangedCallback: (controller, currentTime) {
          print('HlsPlayerWidget.onProgressChangedCallback: $currentTime');
        },
        onPlayerEventCallback: (controller, data) {
          print('HlsPlayerWidget.onPlayerEventCallback: $data');
        },
        onPlayerExceptionCallback: (controller, exception) {
          print('HlsPlayerWidget.onPlayerExceptionCallback: $exception');
        },
        onVideoResolutionReceived: (controller) {
          print('HlsPlayerWidget.onVideoResolutionReceived');
        },
      ),
    );
  }
}
