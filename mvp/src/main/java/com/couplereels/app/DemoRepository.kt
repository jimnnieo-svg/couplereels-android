package com.couplereels.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/** A device-local workspace, not an authenticated or fabricated account. No Firebase dependency. */
@Serializable data class DemoSnapshot(val taste: Taste = Taste(), val saved: List<SavedReel> = emptyList(), val onboarded: Boolean = false)

class DemoRepository(initial: DemoSnapshot = DemoSnapshot(), private val persist: (DemoSnapshot) -> Unit = {}) : CoupleRepository {
 private var snapshot = initial
 private val lock = Mutex()
 private val mutableState = MutableStateFlow(initial.asAppData())
 override val state: StateFlow<AppData> = mutableState
 private fun DemoSnapshot.asAppData() = AppData(myTaste = taste, saved = saved, isDemo = true)
 private suspend fun change(block: (DemoSnapshot) -> DemoSnapshot) = lock.withLock {
  val next = block(snapshot)
  persist(next) // Publish only after durable storage succeeds.
  snapshot = next
  mutableState.value = next.asAppData()
 }
 suspend fun finishOnboarding() = change { it.copy(onboarded = true) }
 override suspend fun publish(tags: Set<String>, url: String?, privateSource: Boolean) {
  val cleaned = requireTags(tags)
  val canonical = url?.let { ReelUrl.normalize(it) ?: error("올바른 Instagram 릴스 링크를 입력해 주세요.") }
  change { old ->
   val prior = old.saved.find { it.url == canonical }
   val saved = if (canonical == null) old.saved else old.saved.filterNot { it.url == canonical } +
    SavedReel(canonical, prior?.tags.orEmpty() + cleaned, prior?.liked ?: false, privateSource)
   old.copy(taste = old.taste.copy(tags = if (canonical == null) cleaned else old.taste.tags + cleaned, updatedAt = System.currentTimeMillis()), saved = saved,
    onboarded = old.onboarded || canonical == null)
  }
 }
 override suspend fun saveRecommendation(url: String) = change { old ->
  val valid = ReelUrl.normalize(url) ?: error("올바른 Instagram 릴스 링크를 입력해 주세요.")
  if (old.saved.any { it.url == valid }) old else old.copy(saved = old.saved + SavedReel(valid))
 }
 override suspend fun save(url: String, liked: Boolean) = change { old ->
  val valid = ReelUrl.normalize(url) ?: error("올바른 Instagram 릴스 링크를 입력해 주세요.")
  val item = old.saved.find { it.url == valid } ?: SavedReel(valid)
  old.copy(saved = old.saved.filterNot { it.url == valid } + item.copy(liked = liked))
 }
 override suspend fun removeSaved(url: String) = change { it.copy(saved = it.saved.filterNot { item -> item.url == url }) }
 override suspend fun logout() = Unit // Exiting changes the UI mode; it never deletes the workspace.
 override suspend fun register(email: String, password: String, name: String): Unit = error("체험 모드에서는 계정을 만들지 않아요.")
 override suspend fun login(email: String, password: String): Unit = error("체험 모드에는 로그인이 필요하지 않아요.")
 override suspend fun rename(name: String): Unit = error("체험 모드에는 계정 프로필이 없어요.")
 override suspend fun createCode(): String = error("커플 연결은 계정 서비스가 필요해요.")
 override suspend fun join(code: String): Unit = error("커플 연결은 계정 서비스가 필요해요.")
 override suspend fun unlink(): Unit = error("체험 모드에는 연결된 계정이 없어요.")
 override fun close() = Unit
}

/** No local account fallback when server authentication is unavailable. */
class UnavailableRepository : CoupleRepository {
 override val state = MutableStateFlow(AppData())
 private fun unavailable(): Nothing = error("계정 서비스 준비 중이에요. 로그인 없이 체험해 주세요.")
 override suspend fun register(email: String, password: String, name: String): Unit = unavailable()
 override suspend fun login(email: String, password: String): Unit = unavailable()
 override suspend fun logout() = Unit
 override suspend fun rename(name: String): Unit = unavailable()
 override suspend fun createCode(): String = unavailable()
 override suspend fun join(code: String): Unit = unavailable()
 override suspend fun unlink(): Unit = unavailable()
 override suspend fun publish(tags: Set<String>, url: String?, privateSource: Boolean): Unit = unavailable()
 override suspend fun saveRecommendation(url: String): Unit = unavailable()
 override suspend fun save(url: String, liked: Boolean): Unit = unavailable()
 override suspend fun removeSaved(url: String): Unit = unavailable()
 override fun close() = Unit
}

data class DemoContent(val id: String, val title: String, val description: String, val symbol: String, val tags: Set<String>)
data class DemoRecommendation(val content: DemoContent, val matchingTags: Set<String>, val score: Double)
object DemoRecommendations {
 // Editorial examples have no invented Reel URL, thumbnail, partner, or playback claim.
 val contents = listOf(
  DemoContent("jewelry", "매일 함께할 작은 반짝임", "심플한 주얼리와 선물을 찾아보는 취향 예시", "◇", setOf("jewelry", "silver", "necklace", "minimal", "daily", "gift")),
  DemoContent("fashion", "둘만의 주말 스타일", "데일리 룩과 패션을 찾아보는 취향 예시", "✧", setOf("fashion", "daily", "minimal", "bold")),
  DemoContent("perfume", "기억에 남는 향기", "향수와 기념일 선물을 찾아보는 취향 예시", "♧", setOf("perfume", "anniversary", "gift")),
  DemoContent("flowers", "평범한 날의 꽃 한 송이", "꽃과 작은 선물을 찾아보는 취향 예시", "❋", setOf("flowers", "date", "gift")),
  DemoContent("food", "다음 데이트의 한 끼", "맛집과 데이트 장소를 찾아보는 취향 예시", "◉", setOf("food", "date", "budget")),
  DemoContent("travel", "함께 떠나고 싶은 곳", "여행과 기념일을 찾아보는 취향 예시", "⌁", setOf("travel", "date", "anniversary")),
  DemoContent("tech", "일상을 편하게 만드는 선물", "전자기기와 실용적인 선물을 찾아보는 취향 예시", "▣", setOf("tech", "daily", "gift")),
  DemoContent("hobby", "함께 좋아하는 시간", "취미와 새로운 데이트를 찾아보는 취향 예시", "✦", setOf("hobby", "date", "bold")),
  DemoContent("gold", "기념일을 위한 골드 포인트", "골드 주얼리와 선물을 찾아보는 취향 예시", "◈", setOf("jewelry", "gold", "bold", "pendant", "anniversary"))
 )
 fun rank(tags: Set<String>): List<DemoRecommendation> {
  val selected = cleanTags(tags)
  return contents.map { content ->
   val common = selected intersect content.tags
   val union = selected + content.tags
   DemoRecommendation(content, common, common.sumOf { RecommendationEngine.weights[it] ?: 1.0 } / union.sumOf { RecommendationEngine.weights[it] ?: 1.0 })
  }.sortedWith(compareByDescending<DemoRecommendation> { it.score }.thenBy { it.content.id })
 }
}
