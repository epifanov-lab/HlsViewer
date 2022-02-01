package com.epifanov.kostya.hls_viewer.plugins;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class PlatformMessengerPlugin implements FlutterPlugin {

  interface MessengerListener {
    void onMessage(Object arg);
  }

  private final String CHANNEL_COMMON_EVENTS = "com.epifanov.kostya.hls_viewer/platform_messenger";

  private MessengerListener listener;
  private MethodChannel mChannel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    System.out.println("Native: PlatformMessengerPlugin.onAttachedToEngine");
    mChannel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL_COMMON_EVENTS);
    mChannel.setMethodCallHandler((call, result) -> {
      switch (call.method) {
        case "message": onMessage(call, result); break;
        default: result.notImplemented();
      }
    });
  }

  public void setListener(MessengerListener listener) {
    this.listener = listener;
  }

  private void onMessage(MethodCall call, MethodChannel.Result result) {
    System.out.println("Native: PlatformMessengerPlugin.message: " + call.arguments);
    if (listener != null) listener.onMessage(call.arguments);
    // sendEvent("Answer: " + call.arguments.toString()); // test echo
  }

  public void sendEvent(Object arguments) {
    mChannel.invokeMethod("event", arguments);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    System.out.println("Native: PlatformMessengerPlugin.onDetachedFromEngine");
  }

}