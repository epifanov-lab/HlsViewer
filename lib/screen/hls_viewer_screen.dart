import 'package:flutter/material.dart';
import 'package:hls_viewer/utils/screen_transitions.dart';

class HlsViewerScreen extends StatelessWidget {
  const HlsViewerScreen({Key? key}) : super(key: key);

  static Route route() {
    return ScreenTransitions.getTransition(
        ScreenTransitionType.fadeIn,
        const RouteSettings(name: "HlsViewerScreen"),
        const HlsViewerScreen()
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container();
  }
}
