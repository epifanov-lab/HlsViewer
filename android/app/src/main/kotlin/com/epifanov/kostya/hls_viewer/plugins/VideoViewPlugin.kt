package com.webka.app

import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class VideoViewPlugin : FlutterPlugin, ActivityAware {
    private val VIEW_TYPE = "VideoView"
    private var pluginBinding: FlutterPluginBinding? = null
    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        println("Native: VideoViewPlugin: onAttachedToEngine: $binding")
        pluginBinding = binding
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        println("Native: VideoViewPlugin: onDetachedFromEngine: $binding")
        pluginBinding = null
    }

    override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
        println("Native: VideoViewPlugin: onAttachedToActivity: $activityPluginBinding")
        pluginBinding!!.platformViewRegistry
                .registerViewFactory(VIEW_TYPE,
                        VideoViewFactory(pluginBinding!!.binaryMessenger))
    }

    override fun onDetachedFromActivityForConfigChanges() {
        println("Native: VideoViewPlugin: onDetachedFromActivityForConfigChanges")
        this.onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
        println("Native: VideoViewPlugin: onReattachedToActivityForConfigChanges: $activityPluginBinding")
        // TODO: your plugin is now attached to a new Activity
        // after a configuration change.
    }

    override fun onDetachedFromActivity() {
        println("Native: VideoViewPlugin: onDetachedFromActivity")
        // TODO: your plugin is no longer associated with an Activity.
        // Clean up references.
    }

    fun registerWith(registrar: Registrar) {
        println("Native: VideoViewPlugin: registerWith: $registrar")
        registrar.platformViewRegistry()
                .registerViewFactory(
                        VIEW_TYPE, VideoViewFactory(registrar.messenger()))
    }

    companion object {
        private val VIEW_TYPE = "VideoView"
        fun registerWith(registrar: Registrar) {
            if (registrar.activity() == null) {
                // When a background flutter view tries to register the plugin, the registrar has no activity.
                // We stop the registration process as this plugin is foreground only.
                return
            }

            //registrar.activity().application.registerActivityLifecycleCallbacks(plugin)
            registrar.platformViewRegistry()
                    .registerViewFactory(
                            VIEW_TYPE,
                            VideoViewFactory(registrar.messenger()))
        }
    }
}

class VideoViewFactory(private val messenger: BinaryMessenger) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, id: Int, o: Any?): PlatformView {
        println("Native: VideoViewFactory: create: $id $messenger")
        // return PlayerPlatformView(context, messenger, id)
        throw Exception("not implemented")
    }
}

