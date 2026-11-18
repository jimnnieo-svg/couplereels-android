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
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
@Config(sdk = [35])
class ProbeExportUiTest {
 @get:Rule(order = 0) val reset = object : ExternalResource() {
  override fun before() { ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("probe", Context.MODE_PRIVATE).edit().clear().commit() }
 }
 @get:Rule(order = 1) val ui = createAndroidComposeRule<MainActivity>()
 @Test fun manuallyUnverifiedPlaybackResultCanBeExportedWithoutClaimingSuccess() {
  ui.onNodeWithText("결과 기록").performClick()
  ui.onNodeWithText("미확인").performScrollTo().performClick()
  ui.onNodeWithText("기록 내보내기").performClick()
  val chooser = shadowOf(ui.activity).nextStartedActivity
  assertEquals(Intent.ACTION_CHOOSER, chooser.action)
  @Suppress("DEPRECATION") val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
  assertEquals("application/json", send.type)
  val item = JSONArray(send.getStringExtra(Intent.EXTRA_TEXT)).getJSONObject(0)
  assertEquals("미확인", item.getString("result"))
  assertEquals(ReelUrl.candidates[0], item.getString("url"))
  assertEquals("webpage", item.getString("mode"))
  assertTrue(item.has("device")); assertTrue(item.has("webview"))
 }
}
