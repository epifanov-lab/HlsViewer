import 'package:equatable/equatable.dart';

class HlsResourceModel extends Equatable {

  final String? title;
  final String? description;
  final String? url;
  final String? preview;

  const HlsResourceModel({
    required this.title,
    required this.description,
    required this.url,
    required this.preview,
  });

  const HlsResourceModel.empty() : this(
      title: null,
      description: null,
      url: null,
      preview: null,
  );

  HlsResourceModel.fromJson(Map<String, dynamic> json)
      : title = json['title'],
        description = json['description'],
        url = json['url'],
        preview = json['preview'];

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['title'] = title;
    data['description'] = description;
    data['url'] = url;
    data['preview'] = preview;
    return data;
  }


  @override
  List<Object?> get props => [
    title,
    description,
    url,
    preview,
  ];

  @override
  String toString() {
    return 'HlsResourceModel{'
        'title: $title, '
        'description: $description, '
        'url: $url, '
        'preview: $preview'
        '}';
  }

}