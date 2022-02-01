import 'package:flutter_test/flutter_test.dart';
import 'package:hls_viewer/model/hls_resource.dart';

void main() {
  test('hls_resource_model_test #1', () {
    final Map<String, dynamic> json = {
      'title': 'TEST TITLE',
      'description': 'TEST DESCRIPTION',
      'url': 'https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8',
      'preview': 'TEST PREVIEW URL',
    };
    final HlsResourceModel model = HlsResourceModel.fromJson(json);
    expect(model.title, 'TEST TITLE');
    expect(model.description, 'TEST DESCRIPTION');
    expect(model.url, 'https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8');
    expect(model.preview, 'TEST PREVIEW URL');
  });
}