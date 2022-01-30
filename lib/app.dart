import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hls_viewer/core/global_navigator.dart';
import 'package:hls_viewer/core/locator.dart';
import 'package:hls_viewer/screen/hls_viewer_screen.dart';
import 'package:hls_viewer/themes.dart';
import 'package:hls_viewer/utils/no_glow_scroll_begavior.dart';

final RouteObserver<PageRoute> routeObserver = RouteObserver<PageRoute>();

class App extends StatefulWidget {
  const App({Key? key}) : super(key: key);

  @override
  _AppState createState() => _AppState();
}

class _AppState extends State<App> {

  final GlobalKey<NavigatorState> _navigatorKey = GlobalKey();

  @override
  void initState() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.manual, overlays: []);
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final globalNavigator = GlobalNavigator(navigatorKey: _navigatorKey);


    Future locatorFuture = setupLocator();

    return FutureBuilder(
      future: locatorFuture,
      builder: (BuildContext context, snapshot) {
        return AppView(navigatorKey: _navigatorKey,);
      },
    );
  }
}

class AppView extends StatefulWidget {

  final GlobalKey<NavigatorState> navigatorKey;

  const AppView({Key? key, required this.navigatorKey}) : super(key: key);

  @override
  _AppViewState createState() => _AppViewState();
}

class _AppViewState extends State<AppView> with WidgetsBindingObserver {

  @override
  void initState() {
    WidgetsBinding.instance?.addObserver(this);
    super.initState();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    //ignore.
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      useInheritedMediaQuery: true,
      debugShowCheckedModeBanner: false,
      navigatorKey: widget.navigatorKey,
      navigatorObservers: [routeObserver],
      scrollBehavior: NoGlowScrollBehavior(),
      onGenerateRoute: (_) => HlsViewerScreen.route(),
      theme: ThemeData(
        inputDecorationTheme: const InputDecorationTheme(
          labelStyle: TextStyle(color: Themes.colorLightGrey),
          hintStyle: TextStyle(color: Themes.colorLightGrey),
          disabledBorder: UnderlineInputBorder(
            borderSide: BorderSide(color: Themes.colorLightGrey),
          ),
          focusedBorder: UnderlineInputBorder(
            borderSide: BorderSide(color: Themes.colorLightGrey),
          ),
        ),
      ),
      builder: (context, child) {
        return Scaffold(
          body: Stack(
            children: [
              child ?? Container(),
            ],
          ),
        );
      },
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance?.removeObserver(this);
    super.dispose();
  }
}