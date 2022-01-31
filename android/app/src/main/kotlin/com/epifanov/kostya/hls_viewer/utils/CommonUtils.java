package com.epifanov.kostya.hls_viewer.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Pair;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class CommonUtils {

  public static final String SHARED_PREFERENCES_NAME = "FlutterSharedPreferences";

  public static String stacktraceToString(Exception exception) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    exception.printStackTrace(pw);
    return sw.toString();
  }

  @SafeVarargs
  public static Map<String, Object> hashMapOf(Pair<String, Object>... entries) {
    final HashMap<String, Object> result = new HashMap<>();
    for (Pair<String, Object> pair: entries) {
      result.put(pair.first, pair.second);
    }
    return result;
  }

  public static void startActivity(Context context, Class activityClass) {
    startActivity(context, activityClass, null);
  }

  public static void startActivity(Context context, Class activityClass, Intent transitIntent) {
    //System.out.println("@@@@@ CommonUtils.startActivity: " + activityClass);
    Intent intent = new Intent(context, activityClass);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    //intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    for (String key : transitIntent.getExtras().keySet()) {
      //System.out.println("@@@@@ " + key + " - " + transitIntent.getStringExtra(key));
      intent.putExtra(key, transitIntent.getStringExtra(key));
    }
    context.startActivity(intent);
  }

  public static String obtainFromPreferences(Context context, String key) {
    SharedPreferences sp = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    System.out.println("Native.obtainFromPreferences: " + key + ": " + sp.getString(key, null));
    //sp.getAll().forEach((s, o) -> System.out.println("### CommonUtils.obtainFromPreferences: " + s + " - " + o));
    return sp.getString(key, null);
  }

  public static void putToPreferences(Context context, String key, String value) {
    SharedPreferences sp = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    sp.edit().putString(key, value).apply();
    sp.getAll().forEach((k, v) -> System.out.println("CommonUtils.putToPreferences: [all]" + k + " - " + v));
  }

}
