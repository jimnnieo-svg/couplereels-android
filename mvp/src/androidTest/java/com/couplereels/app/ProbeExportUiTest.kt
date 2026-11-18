package com.couplereels.app

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ProbeExportUiTest {
 @get:Rule(order = 0) val reset = object : ExternalResource() {
  override fun before() {
   check(androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("p08Disposable")=="true" && android.os.Build.HARDWARE in setOf("ranchu","goldfish")) {"Disposable emulator only"}
   ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("probe", Context.MODE_PRIVATE).edit().clear().commit()
  }
 }
 @get:Rule(order = 1) val ui = createAndroidComposeRule<MainActivity>()
 @Test fun manuallyUnverifiedPlaybackResultCanBeExportedWithoutClaimingSuccess() {
  val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
  val captured = java.util.concurrent.atomic.AtomicReference<Intent>()
  val monitor = object : android.app.Instrumentation.ActivityMonitor() {
   override fun onStartActivity(intent: Intent): android.app.Instrumentation.ActivityResult? {
    if (intent.action == Intent.ACTION_CHOOSER) { captured.set(intent); return android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null) }
    return null
   }
  }
  instrumentation.addMonitor(monitor)
  try {
  ui.onNodeWithText("결과 기록").performClick()
  ui.onNodeWithText("미확인").performScrollTo().performClick()
  ui.onNodeWithText("기록 내보내기").performClick()
  val chooser = checkNotNull(captured.get())
  assertEquals(Intent.ACTION_CHOOSER, chooser.action)
  @Suppress("DEPRECATION") val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
  assertEquals("application/json", send.type)
  val item = JSONArray(send.getStringExtra(Intent.EXTRA_TEXT)).getJSONObject(0)
  assertEquals("미확인", item.getString("result"))
  assertEquals(ReelUrl.candidates[0], item.getString("url"))
  assertEquals("webpage", item.getString("mode"))
  assertTrue(item.has("device")); assertTrue(item.has("webview"))
  } finally { instrumentation.removeMonitor(monitor) }
 }
}
