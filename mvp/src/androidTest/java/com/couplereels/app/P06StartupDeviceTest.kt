package com.couplereels.app
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class P06StartupDeviceTest {
 @Test fun launchesWithoutFirebaseClientConfig() {
  ActivityScenario.launch(MvpActivity::class.java).use { scenario -> scenario.onActivity { assertFalse(it.isFinishing) } }
 }
}
