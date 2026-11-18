package com.couplereels.app

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
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

/** Synthetic URLs are QA fixtures, not claimed playable or discovered Instagram content. */
@RunWith(AndroidJUnit4::class)
class P08LibraryUiTest {
 @get:Rule(order=0) val reset=object:ExternalResource(){
  override fun before(){
   check(InstrumentationRegistry.getArguments().getString("p08Disposable")=="true" && Build.HARDWARE in setOf("ranchu","goldfish")){"Disposable emulator only; never clear a user's phone"}
   val c=ApplicationProvider.getApplicationContext<Context>()
   listOf(LocalLibrary.NAME,DemoStore.NAME,DemoUiStore.NAME).forEach {c.getSharedPreferences(it,0).edit().clear().commit()}
  }
 }
 @get:Rule(order=1) val ui=createAndroidComposeRule<MvpActivity>()
 private val a="https://www.instagram.com/reel/P08_QA_A/"
 private val b="https://www.instagram.com/reel/P08_QA_B/"
 private val c="https://www.instagram.com/reel/P08_PRIVATE_C/"
 private val store get()=LocalLibrary.get(ui.activity)
 private fun click(text:String){val node=if(text in listOf("발견","취향","보관함","우리"))ui.onNodeWithTag("tab-$text") else ui.onNodeWithText(text);runCatching {node.performScrollTo()};node.performClick();ui.waitForIdle()}
 private fun tag(tag:String){val node=ui.onNodeWithTag(tag);runCatching {node.performScrollTo()};node.performClick();ui.waitForIdle()}
 private fun waitState(block:()->Boolean){ui.waitUntil(15000,block);ui.waitForIdle()}
 private fun add(url:String,expected:Int){click("보관함");click("링크 담기");ui.onNodeWithTag("share-url").performTextInput(url);click("저장");waitState {store.state.value.profile(store.state.value.current).reels.size==expected}}
 private fun capture(name:String){ui.waitForIdle();val dir=File(ui.activity.getExternalFilesDir(null),"p08-captures").apply{mkdirs()};InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let {bitmap->File(dir,"$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}}
 @Test fun completeLocalTwoPersonPrivacyAndImmediateSaveFlow(){
  click("회원가입");ui.onNodeWithTag("auth-submit").assertIsNotEnabled();click("로그인으로 돌아가기")
  click("로그인 없이 체험하기")
  ui.waitUntil(15000){ui.onAllNodesWithTag("interest-jewelry").fetchSemanticsNodes().isNotEmpty()}
  click("주얼리");click("다음");click("실버");click("취향 저장하고 시작하기")
  waitState {store.state.value.me.onboarded};ui.onNodeWithText("아직 저장한 릴스가 없어요.").assertExists();capture("01-empty")
  add(a,1);ui.onNodeWithTag("reel-P08_QA_A").assertExists();capture("02-library")
  click("발견");ui.onNodeWithTag("feed-url-P08_QA_A").assertTextEquals(a)
  assertTrue(store.state.value.partnerView(LocalPerson.PARTNER).direct.isEmpty())
  click("보관함");tag("reel-P08_QA_A");tag("sharing-DIRECT");waitState {store.state.value.partnerView(LocalPerson.PARTNER).direct.size==1};capture("03-direct")
  tag("sharing-SIMILAR");waitState {store.state.value.partnerView(LocalPerson.PARTNER).direct.isEmpty()}
  click("우리");tag("profile-PARTNER");waitState {store.state.value.current==LocalPerson.PARTNER}
  click("발견");tag("feed-partner");ui.onNodeWithTag("feed-url-P08_QA_A").assertDoesNotExist();ui.onNodeWithTag("similar-unavailable").assertExists();capture("04-hidden-source")
  click("우리");tag("sharing-DIRECT");waitState {store.state.value.partner.defaultSharing==Sharing.DIRECT}
  add(b,1)
  click("우리");tag("sharing-SIMILAR");waitState {store.state.value.partner.defaultSharing==Sharing.SIMILAR};add(c,2)
  click("우리");tag("profile-ME");waitState {store.state.value.current==LocalPerson.ME}
  click("발견");tag("feed-partner");ui.onNodeWithTag("feed-url-P08_QA_B").assertTextEquals(b);ui.onNodeWithTag("feed-url-P08_PRIVATE_C").assertDoesNotExist();capture("05-partner-direct-only")
  assertEquals(listOf(a),store.state.value.me.reels.map{it.url});assertEquals(2,store.state.value.partner.reels.size)
  ui.activityRule.scenario.recreate();ui.waitForIdle();waitState {store.state.value.me.partnerFeed};ui.onNodeWithTag("feed-url-P08_QA_B").assertExists()
  click("보관함");click("링크 담기");ui.onNodeWithTag("share-url").performTextInput("https://example.com/not-a-reel");click("저장");ui.onNodeWithText("올바른 개별 릴스 링크를 입력해 주세요.").assertExists();click("취소")
  add("$a?igsh=duplicate",1);assertEquals(1,store.state.value.me.reels.size)
  tag("reel-P08_QA_A");click("카테고리·태그 편집");ui.onAllNodesWithText("꽃").onFirst().performClick();click("저장");waitState {"flowers" in store.state.value.me.reels.single().categories}
  click("보관함");capture("06-classified-library")
  click("우리");capture("07-two-profile-settings")
 }
}
