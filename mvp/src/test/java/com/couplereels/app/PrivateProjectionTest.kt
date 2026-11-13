package com.couplereels.app

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PrivateProjectionTest {
 @Test fun partnerOnlyReceivesPublicUrlsAndCannotSeePrivateInputs() = runTest {
  val db = FakeBackend(); val a = FakeRepository(db); val b = FakeRepository(db)
  try {
   a.register("a@test.local", "demo123", "A"); b.register("b@test.local", "demo123", "B"); b.join(a.createCode())
   val original = ReelUrl.candidates[0]; val candidate = ReelUrl.candidates[1]
   a.updateCandidateCatalog(listOf(Candidate(original, setOf("silver")), Candidate(candidate, setOf("silver"))))
   a.publish(setOf("silver", "anniversary", "gift"), original, true)
   assertEquals(setOf("silver", "anniversary", "gift"), a.state.value.myTaste.tags)
   assertEquals(listOf(candidate), b.state.value.partnerReels)
   assertTrue(b.state.value.partnerTaste.tags.isEmpty()); assertTrue(b.state.value.partnerTaste.excludedIds.isEmpty())
   assertTrue(b.state.value.saved.isEmpty()); assertTrue(b.state.value.myTaste.tags.isEmpty())
   a.unlink(); assertNull(b.state.value.partnerReels)
  } finally { a.close(); b.close() }
 }
 @Test fun catalogProjectionRejectsArbitraryUrlsAndMergesDuplicateMetadata() {
  val url = ReelUrl.candidates[0]
  val safe = RecommendationProjection.catalog(listOf(Candidate(url, setOf("silver")), Candidate(url + "?igsh=x", setOf("gift")), Candidate("https://www.instagram.com/reel/not-in-public-pool/", setOf("silver"))))
  assertEquals(listOf(Candidate(url, setOf("silver", "gift"))), safe)
  val outgoing = RecommendationProjection.urls(Taste(setOf("silver"), setOf(digest(url))), ReelUrl.candidates.map { Candidate(it, setOf("silver")) })!!
  assertEquals(5, outgoing.size)
  assertFalse(outgoing.contains(url))
  assertTrue((ReelUrl.candidates.toSet() - outgoing.toSet()).size >= 5)
 }
 @Test fun privateTasteChangesOutputOrderWithoutPublishingReasonOrScore() {
  val a = ReelUrl.candidates[0]; val b = ReelUrl.candidates[1]
  val catalog = listOf(Candidate(a, setOf("silver")), Candidate(b, setOf("date")))
  assertEquals(a, RecommendationProjection.urls(Taste(setOf("silver")), catalog)!!.first())
  assertEquals(b, RecommendationProjection.urls(Taste(setOf("date")), catalog)!!.first())
  assertNull(RecommendationProjection.urls(Taste(), catalog))
 }
 @Test fun legacyTasteSurvivesPartnerUnlinkAndNewCoupleCreation() = runTest {
  val oldCode = "A".repeat(32); val original = ReelUrl.candidates[0]
  val snapshot = FakeSnapshot(accounts = mapOf(
   "a" to FakeAccount(Profile("a", "a@test.local", "A", oldCode), digest("demo123")),
   "b" to FakeAccount(Profile("b", "b@test.local", "B", oldCode), digest("demo123"))),
   couples = mapOf(oldCode to Couple(listOf("a", "b"))),
   tastes = mapOf(oldCode to mapOf("a" to Taste(setOf("silver"), setOf(digest(original))))))
  val backend = FakeBackend(snapshot); val a = FakeRepository(backend, "a"); val b = FakeRepository(backend, "b")
  try {
   b.unlink(); a.createCode()
   assertEquals(setOf("silver"), a.state.value.myTaste.tags)
   assertEquals(setOf(digest(original)), a.state.value.myTaste.excludedIds)
   assertTrue(a.state.value.partnerTaste.tags.isEmpty())
  } finally { a.close(); b.close() }
 }

 @Test fun legacyPartnerPayloadCannotDriveOrExposeTheFeed() {
  val data = AppData(user = Profile("b", "b@test.local", "B", "code"), coupleId = "code", members = listOf("a", "b"), partnerTaste = Taste(setOf("silver"), setOf("private-hash")))
  val feed = PartnerFeed.build(data, CandidateCatalog("b", listOf(Candidate(ReelUrl.candidates[0], setOf("silver")))))
  assertEquals(FeedStatus.TASTE_REQUIRED, feed.status)
  assertTrue(feed.tags.isEmpty()); assertTrue(feed.reels.isEmpty())
 }

}
