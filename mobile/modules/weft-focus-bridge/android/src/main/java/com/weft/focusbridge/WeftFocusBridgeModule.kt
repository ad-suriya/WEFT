package com.weft.focusbridge

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class WeftFocusBridgeModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("WeftFocusBridge")
    AsyncFunction("hasPolicyAccess") {
      val context = appContext.reactContext ?: return@AsyncFunction false
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.isNotificationPolicyAccessGranted
    }
    AsyncFunction("requestPolicyAccess") {
      val context = appContext.reactContext ?: return@AsyncFunction false
      val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      true
    }
    AsyncFunction("setFocusEnabled") { enabled: Boolean ->
      val context = appContext.reactContext ?: return@AsyncFunction false
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (!manager.isNotificationPolicyAccessGranted) return@AsyncFunction false
      manager.setInterruptionFilter(if (enabled) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL)
      true
    }
  }
}
