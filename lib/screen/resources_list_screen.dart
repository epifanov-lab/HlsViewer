import 'package:flutter/material.dart';
import 'package:hls_viewer/utils/screen_transitions.dart';

class ResourcesListScreen extends StatelessWidget {
  const ResourcesListScreen({Key? key}) : super(key: key);

  static Route route() {
    return ScreenTransitions.getTransition(
        ScreenTransitionType.fadeIn,
        const RouteSettings(name: "ResourcesListScreen"),
        const ResourcesListScreen()
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container();
  }
}
