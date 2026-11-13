package com.couplereels.app

import android.content.ClipboardManager
import android.content.Intent
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CoupleUiTest {
 @get:Rule(order = 0) val reset = object : ExternalResource() {
  override fun before() { ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("couplereels-demo-v1", Context.MODE_PRIVATE).edit().clear().commit() }
 }
 @get:Rule(order = 1) val ui = createAndroidComposeRule<MvpActivity>()
 private fun click(label: String) { val node = ui.onNodeWithText(label, useUnmergedTree = true); runCatching { node.performScrollTo() }; node.performClick(); ui.waitForIdle() }
 private fun register(email: String, name: String) {
  ui.onNodeWithTag("email").performScrollTo().performTextReplacement(email)
  ui.onNodeWithTag("password").performScrollTo().performTextInput("test1234")
  ui.onNodeWithTag("register-name").performScrollTo().performTextReplacement(name)
  click("회원가입")
  if (ui.onAllNodesWithText("나중에 할게요").fetchSemanticsNodes().isNotEmpty()) click("나중에 할게요")
 }
 @Test fun twoAccountsConnectAndRepeatedShareSurvivesLogin() {
  register("a@demo.test", "A")
  click("우리")
  click("새 연결 코드 발급"); click("연결 코드 복사")
  val clipboard = ui.activity.getSystemService(ClipboardManager::class.java)
  val code = clipboard.primaryClip!!.getItemAt(0).text.toString()
  assertEquals(32, code.length)
  click("계정 설정"); click("로그아웃")
  val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ReelUrl.candidates[0])
  ui.runOnUiThread { androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(ui.activity, share) }; ui.waitForIdle()
  register("b@demo.test", "B")
  ui.onNodeWithTag("share-sheet-url").assertTextContains(ReelUrl.candidates[0])
  click("실버"); click("내 취향에 반영하기")
  click("우리")
  ui.onNodeWithTag("couple-code").performScrollTo().performTextInput(code)
  click("코드로 연결")
  ui.onAllNodesWithText("커플 연결 완료", useUnmergedTree = true)[0].assertExists()
  click("발견")
  ui.runOnUiThread { androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(ui.activity, share) }; ui.waitForIdle()
  ui.onNodeWithTag("share-sheet-url").assertTextContains(ReelUrl.candidates[0])
  ui.onNodeWithText("이미 담았던 릴스예요. 새로운 취향 태그를 추가할 수 있어요.").assertExists()
 }
 @Test fun unpairedAccountCanReceiveShareAndSavePrivateTaste() {
  register("solo@demo.test", "Solo")
  val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ReelUrl.candidates[1] + "?igsh=abc")
  ui.runOnUiThread { androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(ui.activity, share) }; ui.waitForIdle()
  ui.onNodeWithTag("share-sheet-url").assertTextContains(ReelUrl.candidates[1])
  ui.onNodeWithTag("share-save").assertIsNotEnabled()
  click("실버"); click("내 취향에 반영하기")
  ui.onNodeWithText("릴스를 내 취향에 반영했어요 💗").assertExists()
  click("보관함")
  ui.onNodeWithText(ReelUrl.candidates[1]).assertExists()
 }
}
