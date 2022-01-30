import 'package:equatable/equatable.dart';
import 'package:hls_viewer/model/hls_resource.dart';

class HlsResourcesState extends Equatable {

  final List<HlsResourceModel> resources;

  const HlsResourcesState._({
    required this.resources,
  });

  const HlsResourcesState.initial()
      : this._(resources: const []);

  HlsResourcesState copyWith({
    List<HlsResourceModel>? resources,
  }) {
    return HlsResourcesState._(
      resources: resources ?? this.resources,
    );
  }

  @override
  List<Object> get props => [
    resources,
  ];

}