package com.weft.focusbridge

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class WeftFocusBridgeModule : Module() {
  private fun hasBlockingServiceEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
      it.resolveInfo.serviceInfo.packageName == context.packageName &&
        it.resolveInfo.serviceInfo.name == WeftBlockAccessibilityService::class.java.name
    }
  }

  override fun definition() = ModuleDefinition {
    Name("WeftFocusBridge")

    // --- Do Not Disturb (Study Focus) — unchanged ---
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

    // --- App blocking (new) ---
    AsyncFunction("hasAccessibilityAccess") {
      val context = appContext.reactContext ?: return@AsyncFunction false
      hasBlockingServiceEnabled(context)
    }
    AsyncFunction("requestAccessibilityAccess") {
      val context = appContext.reactContext ?: return@AsyncFunction false
      val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      true
    }
    AsyncFunction("listInstalledApps") {
      val context = appContext.reactContext ?: return@AsyncFunction emptyList<Map<String, String>>()
      val pm = context.packageManager
      val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
      val ownPackage = context.packageName
      val homePackage = WeftBlockAccessibilityService.getDefaultHomePackage(context)
      pm.queryIntentActivities(launcherIntent, 0)
        .mapNotNull { it.activityInfo?.packageName }
        .distinct()
        .filter { it != ownPackage && it != homePackage && !WeftBlockAccessibilityService.NEVER_BLOCK.contains(it) }
        .mapNotNull { pkg ->
          try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            mapOf("packageName" to pkg, "appName" to pm.getApplicationLabel(appInfo).toString())
          } catch (e: Exception) { null }
        }
        .sortedBy { it["appName"] }
    }
    AsyncFunction("setBlockedPackages") { packages: List<String> ->
      val context = appContext.reactContext ?: return@AsyncFunction false
      // Belt-and-suspenders: even if a bad value somehow reached here, WEFT itself and the never-block
      // set can never end up in the persisted list the background service enforces.
      val safe = packages.filter { it != context.packageName && !WeftBlockAccessibilityService.NEVER_BLOCK.contains(it) }
      WeftBlockAccessibilityService.blockPrefs(context).edit()
        .putStringSet(WeftBlockAccessibilityService.KEY_PACKAGES, safe.toSet())
        .apply()
      true
    }
    AsyncFunction("setBlockingEnabled") { enabled: Boolean ->
      val context = appContext.reactContext ?: return@AsyncFunction false
      if (enabled && !hasBlockingServiceEnabled(context)) return@AsyncFunction false
      val editor = WeftBlockAccessibilityService.blockPrefs(context).edit()
      editor.putBoolean(WeftBlockAccessibilityService.KEY_ENABLED, enabled)
      if (enabled) editor.putLong(WeftBlockAccessibilityService.KEY_STARTED_AT, System.currentTimeMillis())
      editor.apply()
      true
    }
  }
}
