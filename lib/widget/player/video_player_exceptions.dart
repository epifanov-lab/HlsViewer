class VideoPlayerException implements Exception {
  final String? type;
  final String? message;
  final Map<String, dynamic>? data;

  VideoPlayerException({
    this.type,
    this.message,
    this.data,
  });

  @override String toString() {
    return 'VideoPlayerException: $type $message data[$data]}';
  }
}