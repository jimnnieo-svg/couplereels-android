package com.couplereels.app

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry

/** ActivityScenario matches lifecycle events against its original launch intent.
 * The production receiver legitimately replaces that intent with ACTION_SEND.
 * Keep the scenario identity after delivery; pending URL/selection restoration is
 * still exercised through the Activity's real saved-instance-state implementation.
 */
internal fun deliverShareForScenario(activity: MvpActivity, share: Intent) {
 val launch = Intent(activity.intent)
 InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(activity, share)
 activity.intent = launch
}
