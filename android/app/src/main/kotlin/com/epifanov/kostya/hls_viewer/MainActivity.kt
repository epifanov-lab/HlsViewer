package com.epifanov.kostya.hls_viewer

import com.epifanov.kostya.hls_viewer.plugins.PlatformMessengerPlugin
import com.epifanov.kostya.hls_viewer.plugins.VideoViewPlugin
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {

    private var sPlatformMessengerPlugin: PlatformMessengerPlugin? = null
    private var mVideoViewPlugin: VideoViewPlugin? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        sPlatformMessengerPlugin = PlatformMessengerPlugin()
        mVideoViewPlugin = VideoViewPlugin()
        flutterEngine.plugins.add(sPlatformMessengerPlugin!!)
        flutterEngine.plugins.add(mVideoViewPlugin!!)
        super.configureFlutterEngine(flutterEngine)
    }

}
