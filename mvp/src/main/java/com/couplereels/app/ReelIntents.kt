package com.couplereels.app

import android.content.Context
import android.content.Intent

object ReelIntents {
 fun sharedText(intent: Intent): String = listOfNotNull(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),intent.getStringExtra(Intent.EXTRA_SUBJECT),intent.clipData?.let {clip->(0 until clip.itemCount.coerceAtMost(8)).mapNotNull {clip.getItemAt(it).text?.toString()}.joinToString("\n")}).joinToString("\n").take(16000)
 fun sharedUrl(intent: Intent): String? = if (intent.action == Intent.ACTION_SEND && (intent.type == "text/plain" || intent.type?.startsWith("image/") == true)) ReelUrl.fromShare(sharedText(intent)) else null
 fun playback(context: Context, url: String?): Intent = Intent(context, MainActivity::class.java).apply {
  if (url != null) {
   val canonical = ReelUrl.normalize(url) ?: error("올바른 개별 Reel URL이 필요합니다.")
   action = Intent.ACTION_SEND; type = "text/plain"; putExtra(Intent.EXTRA_TEXT, canonical)
  }
 }
}
