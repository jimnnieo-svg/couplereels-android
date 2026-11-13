package com.couplereels.app

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class P05RegressionTest {
 @Test fun designVocabularyRemainsValidatedAndUnknownTagsAreRejected() {
  val designTags = setOf("jewelry", "fashion", "perfume", "flowers", "food", "travel", "tech", "hobby", "gold", "bold")
  assertEquals(designTags, requireTags(designTags))
  assertTrue(runCatching { requireTags(setOf("imaginary-category")) }.isFailure)
 }
 @Test fun sharedTasteFlowsBothWaysWhileOriginalsAreExcludedAndSavedStateSurvivesRestart() = runTest {
  var snapshot = FakeSnapshot()
  val backend = FakeBackend(persist = { snapshot = it })
  val a = FakeRepository(backend); val b = FakeRepository(backend)
  try {
   a.register("a@demo.test", "test1234", "A"); b.register("b@demo.test", "test1234", "B")
   val aUid = a.state.value.user!!.uid
   val bUid = b.state.value.user!!.uid
   val originalA = ReelUrl.candidates[0]; val originalB = ReelUrl.candidates[1]; val candidate = ReelUrl.candidates[2]
   a.publish(setOf("perfume", "minimal"), originalA, true)
   b.publish(setOf("flowers", "gold"), originalB, true)
   b.join(a.createCode())
   val feedA = PartnerFeed.build(a.state.value, CandidateCatalog(aUid, listOf(Candidate(originalB, setOf("flowers")), Candidate(candidate, setOf("flowers")))))
   val feedB = PartnerFeed.build(b.state.value, CandidateCatalog(bUid, listOf(Candidate(originalA, setOf("perfume")), Candidate(candidate, setOf("perfume")))))
   assertEquals(listOf(candidate), feedA.reels.map { it.candidate.url })
   assertEquals(listOf(candidate), feedB.reels.map { it.candidate.url })
   b.saveRecommendation(candidate); b.save(candidate, true); b.saveRecommendation(candidate)
   assertEquals(1, b.state.value.saved.count { it.url == candidate }); assertTrue(b.state.value.saved.first { it.url == candidate }.liked)
   val restored = FakeRepository(FakeBackend(snapshot), bUid)
   try { assertTrue(restored.state.value.saved.first { it.url == candidate }.liked) } finally { restored.close() }
   a.unlink(); assertTrue(b.state.value.partnerTaste.tags.isEmpty())
   assertEquals(FeedStatus.PARTNER_REQUIRED, PartnerFeed.build(b.state.value, CandidateCatalog(bUid)).status)
   b.logout(); assertTrue(b.state.value.saved.isEmpty()); assertNull(b.state.value.user)
   b.login("a@demo.test", "test1234"); assertFalse(b.state.value.saved.any { it.url == originalB })
  } finally { a.close(); b.close() }
 }
 @Test fun pendingCatalogFromPreviousUserCannotLeakIntoNewAccountFeed() {
  val data = AppData(user = Profile("B", "b@demo.test", "B", "code"), coupleId = "code", members = listOf("A", "B"), partnerTaste = Taste(setOf("gold")))
  val feed = PartnerFeed.build(data, CandidateCatalog("A", listOf(Candidate(ReelUrl.candidates[0], setOf("gold")))))
  assertEquals(FeedStatus.CATALOG_LOADING, feed.status); assertTrue(feed.reels.isEmpty())
 }
}
