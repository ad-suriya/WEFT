package com.weft.focusbridge

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the foreground and, while WEFT's blocking is active, sends the user
 * back to the home screen and opens WEFT's own "blocked" screen instead. This is the standard,
 * publicly-documented AccessibilityService technique used by app-blocking/parental-control apps —
 * no private APIs, no root, no device-owner mode.
 *
 * The service only ever consults its own SharedPreferences (written by [WeftFocusBridgeModule] from
 * JS); it has no independent notion of "should I block" beyond what WEFT explicitly told it, and it
 * NEVER blocks WEFT itself, the device's home launcher, or anything in [NEVER_BLOCK] — regardless of
 * what the persisted block list contains.
 */
class WeftBlockAccessibilityService : AccessibilityService() {

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val packageName = event?.packageName?.toString() ?: return
    if (packageName == this.packageName) return
    if (NEVER_BLOCK.contains(packageName)) return
    if (packageName == getDefaultHomePackage(this)) return

    val prefs = blockPrefs(this)
    if (!prefs.getBoolean(KEY_ENABLED, false)) return

    // Fail-safe expiry: a flag left on for longer than any real work session ever runs is treated as
    // stale (e.g. WEFT crashed before cleanup, or the phone sat untouched across a reboot) and is
    // cleared automatically rather than blocking indefinitely.
    val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
    if (startedAt <= 0L || System.currentTimeMillis() - startedAt > MAX_BLOCK_DURATION_MS) {
      prefs.edit().putBoolean(KEY_ENABLED, false).apply()
      return
    }

    val blocked = prefs.getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
    if (!blocked.contains(packageName)) return

    performGlobalAction(GLOBAL_ACTION_HOME)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("weft://blocked?app=$packageName"))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
  }

  override fun onInterrupt() {}

  companion object {
    const val PREFS_NAME = "weft_block_prefs"
    const val KEY_ENABLED = "blocking_enabled"
    const val KEY_STARTED_AT = "blocking_started_at"
    const val KEY_PACKAGES = "blocked_packages"

    /** Generous ceiling well beyond any real work session — the safety net described above. */
    const val MAX_BLOCK_DURATION_MS = 12L * 60 * 60 * 1000

    /**
     * Always exempt, no matter what's in the persisted block list: WEFT can't fix a phone it has
     * bricked. Package names vary by OEM, so this is a best-effort denylist, not exhaustive by
     * package name alone — combined with the JS-side picker excluding these same entries.
     */
    val NEVER_BLOCK = setOf(
      "com.android.systemui",
      "com.android.settings",
      "com.android.phone",
      "com.android.server.telecom",
      "com.android.dialer",
      "com.google.android.dialer",
      "com.google.android.permissioncontroller",
      "com.android.permissioncontroller",
      "com.android.emergency",
    )

    fun blockPrefs(context: Context): SharedPreferences =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultHomePackage(context: Context): String? = try {
      val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
      context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    } catch (e: Exception) { null }
  }
}
