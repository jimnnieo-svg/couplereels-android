package com.couplereels.app

import android.content.Context
import android.content.Intent
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
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class CoupleUiTest {
 @get:Rule(order = 0) val reset = object : ExternalResource() {
  override fun before() { ApplicationProvider.getApplicationContext<Context>().getSharedPreferences(DemoStore.NAME, Context.MODE_PRIVATE).edit().clear().commit() }
 }
 @get:Rule(order = 1) val ui = createAndroidComposeRule<MvpActivity>()
 private fun click(text: String) { val n=ui.onNodeWithText(text, useUnmergedTree=true); runCatching { n.performScrollTo() }; n.performClick(); ui.waitForIdle() }
 private fun share() { ui.runOnUiThread { androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(ui.activity, Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ReelUrl.candidates[0])) }; ui.waitForIdle() }
 @Test fun shareReceivedBeforeDemoWaitsForOnboardingAndSurvivesRecreation() {
  share(); click("로그인 없이 체험하기")
  ui.onNodeWithTag("share-save").assertDoesNotExist()
  click("향수"); click("다음"); click("취향 저장하고 시작하기")
  ui.onNodeWithTag("share-save").assertIsNotEnabled()
  click("선물"); click("내 취향에 반영하기")
  ui.onNodeWithText(ReelUrl.candidates[0]).assertExists()
  ui.activityRule.scenario.recreate(); ui.waitForIdle()
  click("보관함"); ui.onNodeWithText(ReelUrl.candidates[0]).assertExists()
  val snapshot=DemoStore(ui.activity).load()
  assertEquals(setOf("perfume", "gift"),snapshot.taste.tags)
  assertEquals(1,snapshot.saved.size)
 }
 @Test fun exitDemoKeepsDataAndDoesNotOverwriteLegacyPreferences() {
  val legacy=ui.activity.getSharedPreferences("couplereels-demo-v1",Context.MODE_PRIVATE)
  legacy.edit().putString("db","legacy sentinel").commit()
  click("로그인 없이 체험하기"); click("나중에 할게요")
  share(); click("실버"); click("내 취향에 반영하기")
  click("우리"); click("체험 설정"); click("체험 나가기")
  ui.onNodeWithTag("enter-demo").assertExists()
  click("로그인 없이 체험하기"); click("보관함")
  ui.onNodeWithText(ReelUrl.candidates[0]).assertExists()
  assertEquals("legacy sentinel",legacy.getString("db",null))
  assertNull(DemoRepository(DemoStore(ui.activity).load()).state.value.user)
 }
}
