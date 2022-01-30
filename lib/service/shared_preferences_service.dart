import 'dart:async';

import 'package:shared_preferences/shared_preferences.dart';

const String prefsKeyHlsResourcesList = 'prefsKeyHlsResourcesList';

class SharedPreferencesService {
  final StreamController<String> _keys$;

  SharedPreferencesService()
      : _keys$ = StreamController.broadcast();

  Future init() async {
    await SharedPreferences.getInstance();
  }

  Stream<String?> onChange$(String key) async* {
    final instance = await SharedPreferences.getInstance();
    yield* _keys$.stream.where((s) => key == s).map((key) => instance.getString(key));
  }

  Future<String?> getString(String key) async {
    final instance = await SharedPreferences.getInstance();
    return instance.getString(key);
  }

  Future setString(String key, String value) async {
    final instance = await SharedPreferences.getInstance();
    return instance.setString(key, value).then((_) => _keys$.add(key));
  }

  Future<bool?> getBool(String key) async {
    final instance = await SharedPreferences.getInstance();
    return instance.getBool(key);
  }

  Future setBool(String key, bool value) async {
    final instance = await SharedPreferences.getInstance();
    return instance.setBool(key, value).then((_) => _keys$.add(key));
  }

  Future<bool> remove(String key) async {
    final instance = await SharedPreferences.getInstance();
    return instance.remove(key);
  }

  void logEntries() async {
    final instance = await SharedPreferences.getInstance();
    instance.getKeys().forEach((key) => print('SharedPreferencesService.log: '
        '$key : ${instance.get(key)}'));
  }

  Future clear() async {
    final instance = await SharedPreferences.getInstance();
    return instance.clear();
  }

  Future<void> dispose() async {
    final instance = await SharedPreferences.getInstance();
    _keys$.close();
    instance.clear();
  }
}
