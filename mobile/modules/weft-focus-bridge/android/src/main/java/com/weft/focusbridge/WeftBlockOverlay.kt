package com.weft.focusbridge

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Draws WEFT's "blocked" message directly on top of whatever is on screen — the launcher, the app
 * drawer, or the distracting app itself — via the SYSTEM_ALERT_WINDOW overlay API, instead of first
 * sending the user home and then switching them into a separate WEFT app screen. This is the
 * standard, publicly-documented technique (same permission class Meet/Messenger use for the
 * floating chat-head bubble); it only ever renders while WEFT's own Accessibility service decides
 * to show it, and only one instance exists at a time.
 */
object WeftBlockOverlay {
  private var view: View? = null

  private val QUOTES = listOf(
    "You're getting distracted. The task you chose to focus on is still waiting.",
    "This app can wait. The work you set out to do can't finish itself.",
    "Every minute here is a minute you promised to your work session.",
    "Discipline is choosing between what you want now and what you want most.",
    "You didn't block this app by accident — you blocked it because it pulls you away.",
    "Future you is counting on present you to stay on task.",
    "The urge to check this will pass. The work you skip won't do itself.",
    "You're one tap away from getting back to what actually matters right now.",
    "Distraction feels productive for a second and costs you the next twenty minutes.",
    "Small interruptions like this one are how deep work quietly dies.",
  )

  fun isShowing(): Boolean = view != null

  fun show(context: Context, appName: String, onExit: () -> Unit) {
    if (view != null) return
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val root = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER
      setBackgroundColor(Color.parseColor("#F2EFE7"))
      setPadding(72, 72, 72, 72)
      isClickable = true
      isFocusable = true
    }
    root.addView(TextView(context).apply {
      text = "$appName is blocked"
      textSize = 26f
      setTypeface(typeface, Typeface.BOLD)
      setTextColor(Color.parseColor("#141414"))
      setPadding(0, 0, 0, 40)
    })
    root.addView(TextView(context).apply {
      text = QUOTES.random()
      textSize = 18f
      setTextColor(Color.parseColor("#141414"))
      setPadding(0, 0, 0, 72)
    })
    root.addView(Button(context).apply {
      text = "EXIT"
      setTextColor(Color.WHITE)
      setBackgroundColor(Color.parseColor("#141414"))
      setPadding(48, 32, 48, 32)
      setOnClickListener { hide(context); onExit() }
    })

    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else
      @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
      type,
      0,
      PixelFormat.TRANSLUCENT,
    )
    wm.addView(root, params)
    view = root
  }

  fun hide(context: Context) {
    val v = view ?: return
    view = null
    try { (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v) } catch (_: Exception) {}
  }
}
