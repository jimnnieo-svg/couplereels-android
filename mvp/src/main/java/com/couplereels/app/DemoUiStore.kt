package com.couplereels.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** Additive UI-only demo preferences. Never rewrites P0-6 snapshots, URLs or account data. */
data class DemoUiPreferences(val liked:Set<String> = emptySet(),val bookmarked:Set<String> = emptySet(),val hidden:Set<String> = emptySet())
class DemoUiStore(context:Context) {
 private val prefs=context.getSharedPreferences(NAME,Context.MODE_PRIVATE)
 val state=MutableStateFlow(DemoUiPreferences(read("liked"),read("bookmarked"),read("hidden")))
 private fun read(key:String)=prefs.getStringSet(key,emptySet()).orEmpty().toSet()
 private fun save(next:DemoUiPreferences) {
  check(prefs.edit().putStringSet("liked",next.liked).putStringSet("bookmarked",next.bookmarked).putStringSet("hidden",next.hidden).commit()) { "기기 저장 공간을 확인해 주세요." }
  state.value=next
 }
 private fun valid(id:String) { require(DemoRecommendations.contents.any {it.id==id}) }
 fun like(id:String) { valid(id); val s=state.value; save(s.copy(liked=if(id in s.liked) s.liked-id else s.liked+id)) }
 fun bookmark(id:String) { valid(id); val s=state.value; save(s.copy(bookmarked=if(id in s.bookmarked) s.bookmarked-id else s.bookmarked+id)) }
 fun hide(id:String) { valid(id); save(state.value.copy(hidden=state.value.hidden+id)) }
 fun restoreHidden()=save(state.value.copy(hidden=emptySet()))
 companion object { const val NAME="couplereels-demo-content-p07-v1" }
}
