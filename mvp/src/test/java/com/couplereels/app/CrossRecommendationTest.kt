package com.couplereels.app

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrossRecommendationTest {
 private val urls = ReelUrl.candidates
 // Test-assigned tags exercise ranking; they are not claims about these videos' content.
 private val candidates = urls.mapIndexed { i, url -> Candidate(url, if (i % 2 == 0) setOf("silver") else setOf("date")) }
 private suspend fun connected(db: FakeBackend): Pair<FakeRepository, FakeRepository> {
  val a = FakeRepository(db); val b = FakeRepository(db)
  a.register("a@test.local", "pass123", "A"); b.register("b@test.local", "pass123", "B"); b.join(a.createCode()); a.updateCandidateCatalog(candidates); b.updateCandidateCatalog(candidates); return a to b
 }
 @Test fun shareUpdatesObservedPartnerFeedAndTasteChangeReordersIt() = runTest {
  val (a,b) = connected(FakeBackend())
  try {
   val catalog = MutableStateFlow(CandidateCatalog(b.state.value.user!!.uid, candidates))
   val feed = PartnerFeed.observe(b, catalog, backgroundScope); runCurrent()
   assertEquals(FeedStatus.TASTE_REQUIRED, feed.value.status)
   val shared = ReelUrl.fromShare("Instagram 공유 ${urls[0]}?igsh=tracking")!!
   a.publish(setOf("silver"), shared, false); runCurrent()
   assertEquals(5, feed.value.reels.size); assertEquals(setOf("silver"), feed.value.reels.first().candidate.tags)
   val first = feed.value.reels.first().candidate.url
   a.publish(setOf("date"), null, true); runCurrent()
   assertTrue(feed.value.tags.isEmpty()); assertTrue(b.state.value.partnerTaste.tags.isEmpty()); assertNotEquals(first, feed.value.reels.first().candidate.url)
   val recommended = feed.value.reels.first().candidate.url
   b.save(recommended, true); assertEquals(recommended, b.state.value.saved.single().url)
   assertTrue(b.state.value.saved.single().liked)
  } finally { a.close(); b.close() }
 }
 @Test fun privateShareProducesBoundedRealCandidatesWithoutFillingFakeTenth() = runTest {
  val (a,b) = connected(FakeBackend())
  try { a.publish(setOf("silver"), urls[0], true); val feed = PartnerFeed.build(b.state.value, CandidateCatalog(b.state.value.user!!.uid, candidates)); assertEquals(5, feed.reels.size); assertFalse(feed.reels.any { it.candidate.url == urls[0] }); assertTrue(b.state.value.saved.isEmpty()) } finally { a.close(); b.close() }
 }
 @Test fun unlinkRemovesObservedFeedImmediately() = runTest {
  val (a,b) = connected(FakeBackend())
  try { a.publish(setOf("silver"), null, true); val feed = PartnerFeed.observe(b, MutableStateFlow(CandidateCatalog(b.state.value.user!!.uid, candidates)), backgroundScope); runCurrent(); assertEquals(5, feed.value.reels.size); a.unlink(); runCurrent(); assertEquals(FeedStatus.PARTNER_REQUIRED, feed.value.status); assertTrue(feed.value.reels.isEmpty()) } finally { a.close(); b.close() }
 }
 @Test fun accountSwitchCannotUsePreviousAccountsCatalog() = runTest {
  val (a,b) = connected(FakeBackend())
  try { a.publish(setOf("silver"), null, true); val oldCatalog = CandidateCatalog(a.state.value.user!!.uid, candidates); assertEquals(FeedStatus.CATALOG_LOADING, PartnerFeed.build(b.state.value, oldCatalog).status); b.logout(); assertEquals(FeedStatus.LOGIN_REQUIRED, PartnerFeed.build(b.state.value, oldCatalog).status) } finally { a.close(); b.close() }
 }
 @Test fun invalidSharedUrlCannotChangePartnersFeed() = runTest {
  val (a,b) = connected(FakeBackend())
  try { a.publish(setOf("silver"), null, true); val prior = b.state.value.partnerReels; assertTrue(runCatching { a.publish(setOf("date"), "https://www.instagram.com/profile/", true) }.isFailure); assertEquals(prior, b.state.value.partnerReels) } finally { a.close(); b.close() }
 }
 @Test fun concurrentJoinAttemptsAdmitOnlyOnePartner() = runTest {
  val db = FakeBackend(); val a = FakeRepository(db); val b = FakeRepository(db); val c = FakeRepository(db)
  try { a.register("a@test.local", "pass123", "A"); b.register("b@test.local", "pass123", "B"); c.register("c@test.local", "pass123", "C"); val code = a.createCode(); val outcomes = listOf(b,c).map { r -> async(Dispatchers.Default) { runCatching { r.join(code) }.isSuccess } }.awaitAll(); assertEquals(1, outcomes.count { it }); assertEquals(2, a.state.value.members.size) } finally { a.close(); b.close(); c.close() }
 }
 @Test fun restartedPairRetainsLatestTasteExclusionsAndSavedRecommendation() = runTest {
  var stored = FakeSnapshot(); val db = FakeBackend(persist = { stored = it }); val (a,b) = connected(db)
  a.publish(setOf("silver"), urls[0], true); b.save(urls[1], true); val uid = b.state.value.user!!.uid; a.close(); b.close()
  val restored = FakeRepository(FakeBackend(Json.decodeFromString<FakeSnapshot>(Json.encodeToString(stored))), uid)
  try { val feed = PartnerFeed.build(restored.state.value, CandidateCatalog(uid, candidates)); assertEquals(5, feed.reels.size); assertTrue(feed.tags.isEmpty()); assertTrue(restored.state.value.partnerTaste.excludedIds.isEmpty()); assertEquals(urls[1], restored.state.value.saved.single().url) } finally { restored.close() }
 }
 @Test fun newCoupleDoesNotInheritFormerPartnersTasteOrExclusions() = runTest {
  val db = FakeBackend(); val (a,b) = connected(db); val c = FakeRepository(db)
  try { a.publish(setOf("silver"), urls[0], true); b.unlink(); c.register("c@test.local", "pass123", "C"); c.join(b.createCode()); val feed = PartnerFeed.build(b.state.value, CandidateCatalog(b.state.value.user!!.uid, candidates)); assertEquals(FeedStatus.TASTE_REQUIRED, feed.status); assertTrue(b.state.value.partnerTaste.excludedIds.isEmpty()) } finally { a.close(); b.close(); c.close() }
 }
 @Test fun savingAndResharingDoesNotRemoveExistingInterest() = runTest {
  val (a,b) = connected(FakeBackend())
  try { a.save(urls[0], true); a.saveRecommendation(urls[0] + "?igsh=tracked"); a.publish(setOf("silver"), urls[0], true); assertTrue(a.state.value.saved.single().liked); assertTrue(b.state.value.saved.isEmpty()) } finally { a.close(); b.close() }
 }
}
