package com.couplereels.app

import android.content.Context
import android.content.Intent

object ReelIntents {
 fun sharedUrl(intent: Intent): String? = if (intent.action == Intent.ACTION_SEND && (intent.type == "text/plain" || intent.type?.startsWith("image/") == true)) ReelUrl.fromShare(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()) else null
 fun playback(context: Context, url: String?): Intent = Intent(context, MainActivity::class.java).apply {
  if (url != null) {
   val canonical = ReelUrl.normalize(url) ?: error("올바른 개별 Reel URL이 필요합니다.")
   action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, canonical)
  }
 }
}
