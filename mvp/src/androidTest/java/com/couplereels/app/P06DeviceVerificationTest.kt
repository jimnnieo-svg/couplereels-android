package com.couplereels.app

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/** Real installed app interactions. Does not clear user data or seed app state. */
@RunWith(AndroidJUnit4::class)
class P06DeviceVerificationTest {
 @get:Rule val ui = createAndroidComposeRule<MvpActivity>()
 @org.junit.Before fun rememberScenarioIntent() { scenarioLaunchIntent = Intent(ui.activity.intent) }
 private lateinit var scenarioLaunchIntent: Intent
 @org.junit.After fun restoreScenarioIntentMetadata() {
  if (::scenarioLaunchIntent.isInitialized) ui.runOnUiThread { ui.activity.intent = scenarioLaunchIntent }
 }

 private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
 private val output get() = File(ui.activity.getExternalFilesDir(null), InstrumentationRegistry.getArguments().getString("captureDir") ?: "p06-captures").apply { mkdirs() }
 private fun click(label: String) {
  P06SystemDialogs.waitForSystemUiIfPresent()
  val node = if (label in listOf("발견", "취향", "보관함", "우리")) ui.onNodeWithTag("tab-$label") else ui.onNodeWithText(label, useUnmergedTree = true)
  runCatching { node.performScrollTo() }
  node.performClick(); ui.waitForIdle()
 }
 private fun capture(name: String) {
  P06SystemDialogs.waitForSystemUiIfPresent()
  ui.waitForIdle()
  val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("Real device capture unavailable")
  File(output, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
  File(output, "events.txt").appendText("CAPTURE $name\n")
  instrumentation.sendStatus(2, android.os.Bundle().apply { putString("stream", "\nCAPTURE $name\n") })
 }
 private fun register(email: String, name: String) {
  P06SystemDialogs.waitForSystemUiIfPresent()
  ui.onNodeWithTag("email").performScrollTo().performTextReplacement(email)
  ui.onNodeWithTag("password").performScrollTo().performTextReplacement("demo1234")
  ui.onNodeWithTag("register-name").performScrollTo().performTextReplacement(name)
  click("회원가입")
 }
 @Test fun actualSevenScreensAndLocalCoupleJourney() {
  androidx.test.espresso.IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS)
  androidx.test.espresso.IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS)
  P06SystemDialogs.waitForSystemUiIfPresent()
  ui.onNodeWithText("이 기기에만 저장 · 기기 간 동기화 없음").assertExists()
  if (ui.onAllNodesWithTag("share-sheet-url").fetchSemanticsNodes().isNotEmpty()) click("취소")
  val suffix = System.currentTimeMillis().toString()
  if (ui.onAllNodesWithTag("email").fetchSemanticsNodes().isEmpty()) {
   click("우리"); click("계정 설정"); click("로그아웃")
  }
  register("p06-a-$suffix@demo.test", "발표 A")
  ui.onNodeWithTag("onboarding-next").assertIsNotEnabled()
  capture("01-onboarding-interests")
  click("주얼리"); click("다음")
  capture("02-onboarding-style")
  click("실버"); click("목걸이"); click("취향 저장하고 시작하기")
  ui.onNodeWithText("내 취향").assertExists()
  capture("03-my-taste")
  click("우리")
  ui.onNodeWithText("아직 연결된 연인이 없어요").assertExists()
  capture("04-us-unlinked")
  val source = ReelUrl.candidates[0]
  ui.runOnUiThread { instrumentation.callActivityOnNewIntent(ui.activity, Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_TEXT, source + "?igsh=p06")) }
  ui.waitForIdle()
  ui.onNodeWithTag("share-sheet-url").assertTextContains(source)
  ui.onNodeWithTag("share-save").assertIsNotEnabled()
  ui.onNodeWithTag("share-necklace").performScrollTo().performClick(); ui.waitForIdle()
  ui.onNodeWithText("릴스 취향에 담기").performScrollTo()
  capture("05-share-bottom-sheet")
  click("내 취향에 반영하기")
  ui.onNodeWithText("릴스를 내 취향에 반영했어요 💗").assertExists()
  click("보관함")
  ui.onNodeWithText(source).assertExists()
  capture("06-saved")
  click("우리"); click("새 연결 코드 발급"); click("연결 코드 복사")
  var code = ""
  ui.runOnUiThread { code = ui.activity.getSystemService(android.content.ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString() }
  assertEquals(32, code.length)
  click("계정 설정"); click("로그아웃")
  register("p06-b-$suffix@demo.test", "발표 B")
  click("나중에 할게요"); click("우리")
  ui.onNodeWithTag("couple-code").performScrollTo().performTextReplacement(code)
  click("코드로 연결")
  ui.onAllNodesWithText("커플 연결 완료")[0].assertExists()
  click("발견")
  ui.onNodeWithTag("discover-pager").assertExists()
  ui.onNodeWithText(source).assertDoesNotExist()
  capture("07-discover")
  click("공식 임베드 열기")
  android.os.SystemClock.sleep(16000)
  capture("08-playback-diagnostic-not-a-pass")
  click("닫기")
  click("저장"); click("보관함")
  ui.onAllNodesWithText("Instagram Reel")[0].assertExists()
  click("우리"); click("커플 연결 해제"); click("연결 해제")
  click("발견")
  ui.onNodeWithText("연결하면 상대방의 취향을 반영해요").assertExists()
  File(output, "events.txt").appendText("PASS onboarding, image-caption share, private save, pairing, partner projection, save and unlink\nPlayback and production Firebase were not asserted.\n")
 }
}
