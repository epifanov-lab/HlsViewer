import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:hls_viewer/bloc/hls_resources/hls_resources_state.dart';


class HlsResourcesCubit extends Cubit<HlsResourcesState> {

  HlsResourcesCubit() : super(const HlsResourcesState.initial());

}
