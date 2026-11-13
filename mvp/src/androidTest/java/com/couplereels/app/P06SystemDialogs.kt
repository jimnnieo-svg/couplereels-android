package com.couplereels.app

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry

/** Recover only the emulator's SystemUI dialog, never an application ANR. Keep evidence. */
internal object P06SystemDialogs {
 fun waitForSystemUiIfPresent() {
  val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
  val root = automation.rootInActiveWindow ?: return
  val titles = root.findAccessibilityNodeInfosByText("System UI isn't responding")
  if (titles.isEmpty()) return
  var wait = root.findAccessibilityNodeInfosByText("Wait").firstOrNull() ?: error("SystemUI ANR has no Wait action")
  while (!wait.isClickable && wait.parent != null) wait = wait.parent
  check(wait.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { "Could not select SystemUI Wait" }
  InstrumentationRegistry.getInstrumentation().sendStatus(2, android.os.Bundle().apply { putString("stream", "\nENVIRONMENT: SystemUI ANR; selected Wait (not an app PASS)\n") })
  val deadline = SystemClock.uptimeMillis() + 20000
  while (SystemClock.uptimeMillis() < deadline) {
   SystemClock.sleep(500)
   val remaining = automation.rootInActiveWindow?.findAccessibilityNodeInfosByText("System UI isn't responding").orEmpty()
   if (remaining.isEmpty()) return
  }
  error("SystemUI still blocks the real screen capture")
 }
}
