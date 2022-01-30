import 'package:equatable/equatable.dart';
import 'package:hls_viewer/model/hls_resource.dart';

class PlayerState extends Equatable {

  final HlsResourceModel activeHlsResource;

  const PlayerState._({
    required this.activeHlsResource,
  });

  const PlayerState.initial()
      : this._(activeHlsResource: const HlsResourceModel.empty());

  PlayerState copyWith({
    HlsResourceModel? activeHlsResource,
  }) {
    return PlayerState._(
      activeHlsResource: activeHlsResource ?? this.activeHlsResource,
    );
  }

  @override
  List<Object> get props => [
    activeHlsResource,
  ];

}