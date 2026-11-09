package com.couplereels.app

import java.net.URI

object ReelUrl {
    val ids = listOf("DUnumljCZlg", "DR3TZi2CWO8", "DRhhirTETMB", "DRhok9lifwC", "De9clQliESh", "De4TkzwiTcr", "DejsrpGCZ1O", "DbyBqWBiLjA", "DdRTGfjibIj", "DdeLa8Uiuck")
    val candidates = ids.map { "https://www.instagram.com/reel/$it/" }
    fun normalize(value: String): String? = runCatching {
        val u = URI(value.trim())
        require(u.scheme == "https" && u.host in setOf("instagram.com", "www.instagram.com") && u.userInfo == null && u.port == -1)
        val id = Regex("^/reel/([A-Za-z0-9_-]+)/?$").matchEntire(u.path)?.groupValues?.get(1) ?: return null
        "https://www.instagram.com/reel/$id/"
    }.getOrNull()
    fun fromShare(text: String): String? = Regex("https://[^\\s<>]+").findAll(text).mapNotNull { normalize(it.value) }.firstOrNull()
    fun allowedNavigation(value: String): Boolean = runCatching {
        val u = URI(value)
        u.scheme == "https" && u.host in setOf("www.instagram.com", "instagram.com") && u.userInfo == null && u.port == -1
    }.getOrDefault(false)
}
