package com.couplereels.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

/** Saving never opens a WebView or requires classification/tags. Finishing returns to the caller. */
class ShareReceiverActivity:ComponentActivity() {
 private var error by mutableStateOf<String?>(null)
 private var saving=false
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  setContent {CoupleTheme { if(error!=null) AlertDialog(onDismissRequest={finish()},title={Text("저장하지 못했어요")},text={Text(error!!)},confirmButton={TextButton(onClick={save()}){Text("다시 시도")}},dismissButton={TextButton(onClick={finish()}){Text("닫기")}}) else Box(Modifier.fillMaxWidth().padding(24.dp)){Text("보관함에 저장하는 중…")} }}
  save()
 }
 private fun save() {
  if(saving)return
  saving=true;error=null
  lifecycleScope.launch {
   try {
    require(ReelIntents.sharedUrl(intent)!=null){"개별 Instagram 릴스 링크가 필요해요. 링크를 복사해 다시 공유해 주세요."}
    val receipt=withContext(Dispatchers.IO) {LocalLibrary.get(this@ShareReceiverActivity).save(ReelIntents.sharedText(intent))}
    Toast.makeText(this@ShareReceiverActivity,if(receipt.duplicate) "이미 보관함에 있어요 ♡" else "보관함에 저장했어요 ♡",Toast.LENGTH_SHORT).show()
    // Classification is deliberately independent of both persistence and the completion UI.
    LocalLibrary.classifyLater(LocalLibrary.get(this@ShareReceiverActivity))
    finish()
   }catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"저장 공간을 확인하고 다시 시도해 주세요."}finally{saving=false}
  }
 }
}
