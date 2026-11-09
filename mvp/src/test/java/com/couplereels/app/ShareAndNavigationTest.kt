package com.couplereels.app

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareAndNavigationTest {
 @Test fun actionSendUsesTheSameParserAsActivityAndCanonicalizesTracking() {
  val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "추천 링크 ${ReelUrl.candidates[0]}?igsh=x")
  assertEquals(ReelUrl.candidates[0], ReelIntents.sharedUrl(intent))
 }
 @Test fun profileAndWrongMimeAreNotAcceptedAsReels() {
  assertNull(ReelIntents.sharedUrl(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "https://www.instagram.com/profile/")))
  assertNull(ReelIntents.sharedUrl(Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_TEXT, ReelUrl.candidates[0])))
 }
 @Test fun recommendationOpensExplicitInternalProbeInsteadOfExternalInstagram() {
  val intent = ReelIntents.playback(ApplicationProvider.getApplicationContext(), ReelUrl.candidates[0] + "?igsh=x")
  assertEquals(MainActivity::class.java.name, intent.component!!.className)
  assertEquals("com.couplereels.mvp", intent.component!!.packageName)
  assertEquals(ReelUrl.candidates[0], intent.getStringExtra(Intent.EXTRA_TEXT))
  assertNull(intent.data)
 }
 @Test fun unsafePlaybackUrlIsRejectedBeforeNavigation() {
  assertTrue(runCatching { ReelIntents.playback(ApplicationProvider.getApplicationContext(), "intent://instagram") }.isFailure)
 }
}
