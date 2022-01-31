package com.epifanov.kostya.hls_viewer

import com.webka.app.VideoViewPlugin
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {

    private var mVideoViewPlugin: VideoViewPlugin? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        mVideoViewPlugin = VideoViewPlugin()
        flutterEngine.plugins.add(mVideoViewPlugin!!)
        super.configureFlutterEngine(flutterEngine)
    }

}
