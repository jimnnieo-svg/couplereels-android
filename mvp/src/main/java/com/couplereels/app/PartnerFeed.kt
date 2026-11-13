package com.couplereels.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

data class CandidateCatalog(val ownerUid: String? = null, val items: List<Candidate> = emptyList())
enum class FeedStatus { LOGIN_REQUIRED, PARTNER_REQUIRED, CATALOG_LOADING, TASTE_REQUIRED, READY }
data class PartnerFeedState(val status: FeedStatus = FeedStatus.LOGIN_REQUIRED, val reels: List<RankedReel> = emptyList(), val tags: Set<String> = emptySet(), val updatedAt: Long = 0, val unclassified: Int = 0)

/** Same composition is used by Compose and the two-account integration tests. */
object PartnerFeed {
 fun build(data: AppData, catalog: CandidateCatalog): PartnerFeedState {
  val user = data.user ?: return PartnerFeedState()
  if (data.coupleId == null || data.members.distinct().size != 2 || user.uid !in data.members) return PartnerFeedState(FeedStatus.PARTNER_REQUIRED)
  // Do not briefly render the previous account's annotations during an account switch.
  if (catalog.ownerUid != user.uid) return PartnerFeedState(FeedStatus.CATALOG_LOADING)
  val urls = data.partnerReels ?: return PartnerFeedState(FeedStatus.TASTE_REQUIRED)
  val reels = urls.mapNotNull(ReelUrl::normalize).distinct().filter { it in ReelUrl.candidates }.take(RecommendationProjection.MAX_PUBLIC_RESULTS).map { url ->
   RankedReel(Candidate(url, catalog.items.find { it.url == url }?.tags.orEmpty()), 0.0)
  }
  return PartnerFeedState(FeedStatus.READY, reels, emptySet(), data.partnerUpdatedAt, reels.count { it.candidate.tags.isEmpty() })
 }
 fun observe(repository: CoupleRepository, catalog: StateFlow<CandidateCatalog>, scope: CoroutineScope): StateFlow<PartnerFeedState> =
  combine(repository.state, catalog, ::build).stateIn(scope, SharingStarted.Eagerly, PartnerFeedState())
}
