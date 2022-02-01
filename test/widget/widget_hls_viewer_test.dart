import 'package:flutter/cupertino.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hls_viewer/screen/hls_viewer_screen.dart';
import 'package:hls_viewer/widget/player/hls_player_widget.dart';

void main() {

  /// flutter run -t test/widget_hls_viewer_test.dart
  /// I/flutter (30035): 00:02 +1: All tests passed!
  testWidgets('Hls Viewer test', (WidgetTester tester) async {
    await tester.pumpWidget(
      const Directionality(
        textDirection: TextDirection.ltr,
        child: HlsViewerScreen(),
      ),
    );
    var finder = find.byKey(const ValueKey('HlsPlayerWidget'));
    expect(finder, findsWidgets);
    await tester.pump(const Duration(milliseconds: 2000));
    HlsPlayerWidget widget = finder.evaluate().first.widget as HlsPlayerWidget;
    expect(widget.controller?.isFirstFrameRendered, isTrue);
  }, initialTimeout: const Duration(milliseconds: 5000));
}
