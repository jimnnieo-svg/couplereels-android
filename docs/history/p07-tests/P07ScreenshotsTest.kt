package com.couplereels.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import java.io.File

/** Run only on the dedicated disposable emulator. Uses explicit local QA fixtures. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class P07ScreenshotsTest {
 @get:Rule(order=0) val fixture=object:ExternalResource() {
  override fun before() {
   val c=ApplicationProvider.getApplicationContext<Context>()
   listOf(DemoStore.NAME,DemoUiStore.NAME).forEach {c.getSharedPreferences(it,0).edit().clear().commit()}
   DemoStore(c).apply {
    save(DemoSnapshot(Taste(setOf("jewelry","silver","minimal","perfume","necklace")),listOf(SavedReel(ReelUrl.candidates[0],setOf("silver","necklace")),SavedReel(ReelUrl.candidates[1],setOf("perfume"))),true))
    setActive(true)
   }
   DemoUiStore(c).apply {like("jewelry"); bookmark("food"); bookmark("travel")}
  }
 }
 @get:Rule(order=1) val ui=createAndroidComposeRule<MvpActivity>()
 private fun click(text:String) {
  val n=if(text in listOf("발견","취향","보관함","우리")) ui.onNodeWithTag("tab-$text") else ui.onNodeWithText(text)
  runCatching {n.performScrollTo()}; n.performClick(); ui.waitForIdle()
 }
 private fun capture(name:String) {
  ui.waitForIdle()
  val out=File(ui.activity.getExternalFilesDir(null),"p07-captures").apply {mkdirs()}
  // Capture the actual Android Compose window, including the modal window when present.
  // This contains native rendered controls; no mockup image is used for the UI.
  val roots=ui.onAllNodes(isRoot(),useUnmergedTree=true)
  val bitmap=roots[roots.fetchSemanticsNodes().lastIndex].captureToImage().asAndroidBitmap()
  File(out,"$name.png").outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
  // Keep a separate full-display capture for checking Android system bars/overlays.
  InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let {screen ->
   File(out,"$name-system.png").outputStream().use {screen.compress(Bitmap.CompressFormat.PNG,100,it)}
   // The modal and main Compose owners have no stable ordering; use the full
   // Android display for this shot so the actual sheet is always included.
   if(name=="06-share-sheet") File(out,"$name.png").outputStream().use {screen.compress(Bitmap.CompressFormat.PNG,100,it)}
   screen.recycle()
  }
 }
 @Test fun captureSevenInteractiveScreensAndCheckSafeArea() {
  ui.onNodeWithTag("discover-pager").assertExists(); capture("01-discover")
  click("취향")
  val top=ui.onNodeWithContentDescription("알림").fetchSemanticsNode().boundsInRoot.top
  assertTrue("Header must stay below Android status bar after leaving immersive feed",top>=20*ui.activity.resources.displayMetrics.density)
  capture("02-taste")
  ui.onNodeWithText("릴스 공유하기").performScrollTo(); capture("02-taste-bottom")
  click("취향 수정"); capture("00-onboarding-1")
  ui.onNodeWithText("나중에 할게요").performScrollTo(); capture("00-onboarding-1-bottom")
  click("다음"); capture("05-onboarding-2")
  ui.onNodeWithTag("onboarding-next").performScrollTo(); capture("05-onboarding-2-bottom")
  click("취향 저장하고 시작하기")
  click("보관함"); capture("03-saved")
  click("우리"); capture("04-us")
  ui.runOnUiThread {deliverShareForScenario(ui.activity,Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,ReelUrl.candidates[0]))}
  ui.waitForIdle(); ui.onAllNodesWithText("실버").onLast().performClick(); capture("06-share-sheet")
  click("취소"); assertEquals(2,DemoStore(ui.activity).load().saved.size)
 }
}
