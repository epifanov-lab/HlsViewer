import 'package:get_it/get_it.dart';
import 'package:hls_viewer/bloc/hls_resources/hls_resources_cubit.dart';
import 'package:hls_viewer/bloc/player/player_cubit.dart';
import 'package:hls_viewer/service/shared_preferences_service.dart';

final GetIt locator = GetIt.instance;

Future setupLocator() async {
  await locator.reset();
  _setupServices();
  _setupBlocs();
  return true;
}

void _setupServices() {
  locator.registerLazySingleton(() => SharedPreferencesService()..init());
}

void _setupBlocs() {
  locator.registerLazySingleton(() => HlsResourcesCubit());
  locator.registerLazySingleton(() => PlayerCubit());
}

void resetLocator() {
  locator.resetLazySingleton<SharedPreferencesService>();
}