package com.couplereels.app

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class UiIntegrationTest {
 @get:Rule(order = 0) val reset = object : ExternalResource() {
  override fun before() { ApplicationProvider.getApplicationContext<Context>().getSharedPreferences(DemoStore.NAME, Context.MODE_PRIVATE).edit().clear().commit() }
 }
 @get:Rule(order = 1) val ui = createAndroidComposeRule<MvpActivity>()
 private fun click(text: String) { val node = ui.onNodeWithText(text, useUnmergedTree = true); runCatching { node.performScrollTo() }; node.performClick(); ui.waitForIdle() }
 private fun enter() { click("로그인 없이 체험하기") }
 @Test fun onboardingLeadsToPersonalizedFeedAndAllTabs() {
  enter()
  ui.onNodeWithTag("onboarding-next").assertIsNotEnabled()
  click("향수"); click("다음"); click("실버"); click("취향 저장하고 시작하기")
  ui.onNodeWithText("내 취향 추천").assertExists()
  ui.onNodeWithText("데모 콘텐츠").assertExists()
  click("취향"); ui.onNodeWithText("내 취향").assertExists()
  click("보관함"); ui.onNodeWithText("아직 이곳에 담긴 릴스가 없어요").assertExists()
  click("우리"); click("내 취향 추천 보기")
  ui.onNodeWithText("내 취향 추천").assertExists()
 }
 @Test fun imageWithoutLinkAndCancellationNeverSave() {
  enter(); click("나중에 할게요")
  val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
  ui.runOnUiThread { instrumentation.callActivityOnNewIntent(ui.activity, Intent(Intent.ACTION_SEND).setType("image/jpeg")) }; ui.waitForIdle()
  ui.onNodeWithText("릴스 링크를 받지 못했어요. 이미지 내용은 분석하지 않아요. Instagram에서 링크를 복사해 직접 입력해 주세요.").assertExists()
  ui.runOnUiThread { instrumentation.callActivityOnNewIntent(ui.activity, Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ReelUrl.candidates[0])) }; ui.waitForIdle()
  click("실버")
  ui.activityRule.scenario.recreate(); ui.waitForIdle()
  ui.onNodeWithTag("share-sheet-url").assertTextContains(ReelUrl.candidates[0])
  ui.onNodeWithTag("share-save").assertIsEnabled()
  click("취소"); click("보관함")
  ui.onNodeWithText("아직 이곳에 담긴 릴스가 없어요").assertExists()
 }
 @Test fun registrationIsASeparateScreenAndUnavailableAuthDoesNotSucceed() {
  click("회원가입")
  ui.onNodeWithTag("register-name").assertExists()
  ui.onNodeWithTag("auth-unavailable").assertExists()
  ui.onNodeWithTag("auth-submit").assertIsNotEnabled()
  click("로그인으로 돌아가기")
  ui.onNodeWithTag("register-name").assertDoesNotExist()
  ui.onNodeWithTag("enter-demo").assertExists()
 }
}
