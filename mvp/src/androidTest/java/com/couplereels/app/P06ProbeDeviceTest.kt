package com.couplereels.app
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class P06ProbeDeviceTest {
 @get:Rule val ui = createAndroidComposeRule<MainActivity>()
 @Test fun unverifiedPlaybackExportsAsUnverifiedAndDoesNotSendExternally() {
  Intents.init()
  try {
   Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
   ui.onNodeWithText("결과 기록").performClick()
   ui.onNodeWithText("미확인").performScrollTo().performClick()
   ui.onNodeWithText("기록 내보내기").performClick()
   val chooser = Intents.getIntents().last { it.action == Intent.ACTION_CHOOSER }
   @Suppress("DEPRECATION") val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
   assertEquals("application/json", send.type)
   val items = JSONArray(send.getStringExtra(Intent.EXTRA_TEXT)); val last = items.getJSONObject(items.length() - 1)
   assertEquals("미확인", last.getString("result")); assertTrue(last.has("device")); assertTrue(last.has("webview"))
   assertNotNull(ReelUrl.normalize(last.getString("url")))
  } finally { Intents.release() }
 }
}
