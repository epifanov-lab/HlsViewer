class VideoPlayerException implements Exception {
  final String type;
  final String message;
  final Map<String, dynamic> data;

  VideoPlayerException({
    required this.type,
    required this.message,
    required this.data,
  });

  @override String toString() {
    return 'VideoPlayerException: $type $message data[$data]}';
  }
}