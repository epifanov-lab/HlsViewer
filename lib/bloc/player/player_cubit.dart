import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:hls_viewer/bloc/hls_resources/hls_resources_state.dart';


class PlayerCubit extends Cubit<HlsResourcesState> {

  PlayerCubit() : super(const HlsResourcesState.initial());

}
