package com.couplereels.app

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.security.MessageDigest

val tagVocabulary = listOf("silver", "necklace", "minimal", "gift", "pendant", "daily", "jewelry", "925", "budget", "anniversary", "date", "fashion", "perfume", "flowers", "food", "travel", "tech", "hobby", "gold", "bold")
fun cleanTags(tags: Collection<String>): Set<String> = tags.map { it.trim().lowercase() }.filter { it in tagVocabulary }.toSet()
fun digest(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
@Serializable data class Profile(val uid: String, val email: String, val name: String, val coupleId: String? = null)
@Serializable data class Couple(val members: List<String>, val active: Boolean = true)
@Serializable data class Taste(val tags: Set<String> = emptySet(), val excludedIds: Set<String> = emptySet(), val updatedAt: Long = 0)
@Serializable data class SavedReel(val url: String, val tags: Set<String> = emptySet(), val liked: Boolean = false, val privateSource: Boolean = true)
@Serializable data class Candidate(val url: String, val tags: Set<String> = emptySet())
data class RankedReel(val candidate: Candidate, val score: Double)
data class AppData(val user: Profile? = null, val coupleId: String? = null, val members: List<String> = emptyList(), val myTaste: Taste = Taste(), val partnerTaste: Taste = Taste(), val saved: List<SavedReel> = emptyList(), val error: String? = null, val partnerReels: List<String>? = null, val partnerUpdatedAt: Long = 0)

/** Only manually assigned metadata. Empty tags mean unclassified, never inferred video content. */
object RecommendationEngine {
 val weights = mapOf("silver" to 1.4, "necklace" to 1.4, "gift" to 1.2, "anniversary" to 1.3)
 fun rank(taste: Taste, candidates: List<Candidate>, limit: Int = 10): List<RankedReel> {
  val target = cleanTags(taste.tags)
  return candidates.mapNotNull { c -> ReelUrl.normalize(c.url)?.let { c.copy(url = it, tags = cleanTags(c.tags)) } }
   .groupBy { it.url }.map { (url, entries) -> Candidate(url, entries.flatMap { it.tags }.toSet()) }
   .filter { digest(it.url) !in taste.excludedIds }
   .map { c ->
    val union = target + c.tags
    val score = if (union.isEmpty()) 0.0 else (target intersect c.tags).sumOf { weights[it] ?: 1.0 } / union.sumOf { weights[it] ?: 1.0 }
    RankedReel(c, score)
   }.sortedWith(compareByDescending<RankedReel> { it.score }.thenBy { it.candidate.url }).take(limit.coerceIn(0, 10))
 }
}
interface CoupleRepository {
 val state: StateFlow<AppData>
 suspend fun register(email: String, password: String, name: String)
 suspend fun login(email: String, password: String)
 suspend fun logout()
 suspend fun rename(name: String)
 suspend fun createCode(): String
 suspend fun join(code: String)
 suspend fun unlink()
 suspend fun publish(tags: Set<String>, url: String?, privateSource: Boolean)
 suspend fun updateCandidateCatalog(candidates: List<Candidate>)
 suspend fun saveRecommendation(url: String)
 suspend fun save(url: String, liked: Boolean)
 suspend fun removeSaved(url: String)
 fun close()
}
fun requireTags(tags: Set<String>): Set<String> = cleanTags(tags).also { require(it.isNotEmpty() && it.size == tags.size) { "목록에서 취향 태그를 선택하세요." } }
fun requireName(name: String): String = name.trim().also { require(it.length in 1..40) { "이름은 1~40자로 입력하세요." } }

/** Only public candidate URLs cross the couple boundary. Never tags, scores or source hashes. */
object RecommendationProjection {
 const val MAX_PUBLIC_RESULTS = 5
 fun urls(taste: Taste, candidates: List<Candidate>): List<String>? =
  if (taste.tags.isEmpty()) null else RecommendationEngine.rank(taste, candidates, MAX_PUBLIC_RESULTS).map { it.candidate.url }
 fun catalog(candidates: List<Candidate>): List<Candidate> = candidates.mapNotNull { c ->
  ReelUrl.normalize(c.url)?.takeIf { it in ReelUrl.candidates }?.let { Candidate(it, cleanTags(c.tags)) }
 }.groupBy { it.url }.map { (url, entries) -> Candidate(url, entries.flatMap { it.tags }.toSet()) }.take(10)
}
