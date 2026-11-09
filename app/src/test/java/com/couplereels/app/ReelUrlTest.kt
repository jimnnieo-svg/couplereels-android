package com.couplereels.app
import org.junit.Assert.*
import org.junit.Test
class ReelUrlTest {
 @Test fun allTenAreUniqueReels() { assertEquals(10, ReelUrl.candidates.distinct().size); ReelUrl.candidates.forEach { assertEquals(it, ReelUrl.normalize(it)) } }
 @Test fun stripsTracking() { assertEquals(ReelUrl.candidates[0], ReelUrl.normalize(ReelUrl.candidates[0] + "?igsh=abc")) }
 @Test fun rejectsNonReels() { listOf("https://www.instagram.com/name/", "https://www.instagram.com/explore/", "https://www.instagram.com/reel/abc/extra").forEach { assertNull(ReelUrl.normalize(it)) } }
 @Test fun rejectsHostSpoofing() { listOf("https://www.instagram.com.evil.org/reel/abc/", "https://evil@www.instagram.com/reel/abc/", "http://www.instagram.com/reel/abc/", "javascript:alert(1)", "https://www.instagram.com:443/reel/abc/").forEach { assertNull(ReelUrl.normalize(it)) } }
 @Test fun extractsShare() { assertEquals(ReelUrl.candidates[0], ReelUrl.fromShare("공유 링크 ${ReelUrl.candidates[0]}?igsh=x")) }
 @Test fun blocksAppAndFileNavigation() { listOf("intent://instagram", "file:///etc/passwd", "https://evil.org", "https://instagram.com.evil.org").forEach { assertFalse(ReelUrl.allowedNavigation(it)) }; assertTrue(ReelUrl.allowedNavigation("https://www.instagram.com/accounts/login/")) }
}
