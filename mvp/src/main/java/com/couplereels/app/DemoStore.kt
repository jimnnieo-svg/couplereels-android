package com.couplereels.app

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DemoStore(context: Context) {
 private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
 private val json = Json { ignoreUnknownKeys = true }
 val active: Boolean get() = prefs.getBoolean("active", false)
 fun setActive(value: Boolean) { check(prefs.edit().putBoolean("active", value).commit()) { "기기 저장 공간을 확인해 주세요." } }
 fun load(): DemoSnapshot = prefs.getString("snapshot", null)?.let { json.decodeFromString<DemoSnapshot>(it) } ?: DemoSnapshot()
 fun save(snapshot: DemoSnapshot) { check(prefs.edit().putString("snapshot", json.encodeToString(snapshot)).commit()) { "기기 저장 공간을 확인해 주세요." } }
 companion object { const val NAME = "couplereels-guest-p06-v1" }
}
