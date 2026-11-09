package com.couplereels.app
import android.os.Looper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.junit.Assert.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StartupTest {
 @Test fun launchesWithoutFirebaseClientConfig() {
  val controller = Robolectric.buildActivity(MvpActivity::class.java).setup()
  shadowOf(Looper.getMainLooper()).idle()
  assertFalse(controller.get().isFinishing)
  controller.pause().stop().destroy()
 }
}
