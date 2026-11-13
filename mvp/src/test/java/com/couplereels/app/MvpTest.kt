package com.couplereels.app

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class MvpTest {
 private val urls = ReelUrl.candidates
 @Test fun weightedSimilarityRanksMatchingCandidate() {
  val list = listOf(Candidate(urls[0], setOf("silver", "necklace")), Candidate(urls[1], setOf("date")))
  assertEquals(urls[0], RecommendationEngine.rank(Taste(setOf("silver")), list).first().candidate.url)
 }
 @Test fun changedTasteChangesRanking() {
  val list = listOf(Candidate(urls[0], setOf("silver")), Candidate(urls[1], setOf("date")))
  assertNotEquals(RecommendationEngine.rank(Taste(setOf("silver")), list).first().candidate.url, RecommendationEngine.rank(Taste(setOf("date")), list).first().candidate.url)
 }
 @Test fun deduplicatesTrackingAndMergesMetadata() {
  val ranked = RecommendationEngine.rank(Taste(setOf("silver")), listOf(Candidate(urls[0], setOf("silver")), Candidate(urls[0] + "?igsh=x", setOf("gift"))))
  assertEquals(1, ranked.size); assertEquals(setOf("silver", "gift"), ranked.first().candidate.tags)
 }
 @Test fun excludesPrivateOriginalByHash() { assertTrue(RecommendationEngine.rank(Taste(excludedIds = setOf(digest(urls[0]))), listOf(Candidate(urls[0]))).isEmpty()) }
 @Test fun boundsTopTenAndRejectsProfileUrls() {
  val items = urls.map { Candidate(it) } + Candidate("https://www.instagram.com/profile/")
  assertEquals(10, RecommendationEngine.rank(Taste(), items, 100).size)
  assertEquals(0, RecommendationEngine.rank(Taste(), items, -1).size)
 }
 @Test fun unknownTagsDoNotImplyVideoAnalysis() { assertEquals(0.0, RecommendationEngine.rank(Taste(setOf("silver")), listOf(Candidate(urls[0], setOf("imaginary")))).first().score, 0.0) }
 @Test fun rankingIsDeterministicWithUnclassifiedCandidates() { val list = urls.map { Candidate(it) }; assertEquals(RecommendationEngine.rank(Taste(), list), RecommendationEngine.rank(Taste(), list.reversed())) }
 @Test fun accountsLoginAndProfile() = runTest {
  val b = FakeBackend(); val a = FakeRepository(b)
  try { a.register("A@test.local", "pass123", "Alice"); a.rename("A new"); val uid = a.state.value.user!!.uid; a.logout(); assertNull(a.state.value.user); a.login("a@test.local", "pass123"); assertEquals(uid, a.state.value.user!!.uid); assertEquals("A new", a.state.value.user!!.name) } finally { a.close() }
 }
 @Test fun rejectsWrongPasswordAndDuplicateAccount() = runTest {
  val r = FakeRepository(FakeBackend()); try { r.register("a@test.local", "pass123", "A"); r.logout(); assertTrue(runCatching { r.login("a@test.local", "wrong") }.isFailure); assertNull(r.state.value.user); assertTrue(runCatching { r.register("a@test.local", "pass123", "Other") }.isFailure) } finally { r.close() }
 }
 @Test fun twoAccountsReceiveLatestPartnerTasteWithoutOriginal() = runTest {
  val db = FakeBackend(); val a = FakeRepository(db); val b = FakeRepository(db)
  try { a.register("a@test.local", "pass123", "A"); b.register("b@test.local", "pass123", "B"); b.join(a.createCode()); a.publish(setOf("silver"), urls[0], true)
   assertTrue(b.state.value.partnerTaste.tags.isEmpty()); assertTrue(b.state.value.partnerTaste.excludedIds.isEmpty()); assertFalse(b.state.value.partnerReels!!.contains(urls[0])); assertTrue(b.state.value.saved.isEmpty())
   a.publish(setOf("date"), null, true); assertTrue(b.state.value.partnerTaste.tags.isEmpty()); assertNotNull(b.state.value.partnerReels)
  } finally { a.close(); b.close() }
 }
 @Test fun fullCoupleCannotAcceptThirdAndLinkedUserCannotCreateAnother() = runTest {
  val db = FakeBackend(); val repos = List(3) { FakeRepository(db) }
  try { repos.forEachIndexed { i, r -> r.register("$i@test.local", "pass123", "$i") }; val code = repos[0].createCode(); repos[1].join(code); assertTrue(runCatching { repos[2].join(code) }.isFailure); assertTrue(runCatching { repos[0].createCode() }.isFailure) } finally { repos.forEach { it.close() } }
 }
 @Test fun unlinkClearsBothFeedsAndOldCodeCannotBeUsed() = runTest {
  val db = FakeBackend(); val a = FakeRepository(db); val b = FakeRepository(db)
  try { a.register("a@test.local", "pass123", "A"); b.register("b@test.local", "pass123", "B"); val code = a.createCode(); b.join(code); a.publish(setOf("gift"), null, true); b.unlink(); assertNull(a.state.value.coupleId); assertTrue(a.state.value.partnerTaste.tags.isEmpty()); a.publish(setOf("date"), null, true); assertEquals(setOf("date"), a.state.value.myTaste.tags); assertTrue(b.state.value.partnerTaste.tags.isEmpty()); assertTrue(runCatching { b.join(code) }.isFailure); assertNotEquals(code, a.createCode()) } finally { a.close(); b.close() }
 }
 @Test fun unpairedSharesAreSavedPrivatelyAndAppearAfterLinking() = runTest {
  val db = FakeBackend(); val a = FakeRepository(db); val b = FakeRepository(db)
  try {
   a.register("a@test.local", "pass123", "A")
   a.publish(setOf("silver", "necklace"), urls[0], true)
   a.publish(setOf("minimal"), urls[0] + "?igsh=tracking", true)
   assertEquals(setOf("silver", "necklace", "minimal"), a.state.value.myTaste.tags)
   assertEquals(1, a.state.value.saved.size)
   assertEquals(setOf("silver", "necklace", "minimal"), a.state.value.saved.first().tags)
   assertTrue(a.state.value.saved.first().privateSource)
   assertNull(a.state.value.coupleId)
   b.register("b@test.local", "pass123", "B")
   assertTrue(b.state.value.partnerTaste.tags.isEmpty())
   b.join(a.createCode())
   assertTrue(b.state.value.partnerTaste.tags.isEmpty())
   assertTrue(b.state.value.partnerTaste.excludedIds.isEmpty())
   assertTrue(b.state.value.saved.isEmpty())
   assertFalse(b.state.value.partnerReels!!.contains(urls[0])); assertEquals(5, b.state.value.partnerReels!!.size)
  } finally { a.close(); b.close() }
 }
 @Test fun oldCoupleTasteIsRetainedInPersonalStorageOnUnlink() = runTest {
  val id = "legacy-user"; val oldCode = "A".repeat(32)
  val snapshot = FakeSnapshot(
   accounts = mapOf(id to FakeAccount(Profile(id, "legacy@test.local", "Legacy", oldCode), digest("pass123"))),
   couples = mapOf(oldCode to Couple(listOf(id))),
   tastes = mapOf(oldCode to mapOf(id to Taste(setOf("necklace"), setOf(digest(urls[0])))))
  )
  val repo = FakeRepository(FakeBackend(snapshot), id)
  try {
   repo.unlink()
   assertNull(repo.state.value.coupleId)
   assertEquals(setOf("necklace"), repo.state.value.myTaste.tags)
   assertEquals(setOf(digest(urls[0])), repo.state.value.myTaste.excludedIds)
  } finally { repo.close() }
 }
 @Test fun savedReelsArePerAccountAndDeduplicated() = runTest {
  val db = FakeBackend(); val a = FakeRepository(db); val b = FakeRepository(db)
  try { a.register("a@test.local", "pass123", "A"); b.register("b@test.local", "pass123", "B"); a.save(urls[0], false); a.save(urls[0] + "?igsh=x", true); assertEquals(1, a.state.value.saved.size); assertTrue(a.state.value.saved.first().liked); assertTrue(b.state.value.saved.isEmpty()); a.removeSaved(urls[0]); assertTrue(a.state.value.saved.isEmpty()) } finally { a.close(); b.close() }
 }
 @Test fun persistedDatabaseAndSessionRestoreAfterRepositoryRestart() = runTest {
  var snapshot = FakeSnapshot(); var session: String? = null
  val a = FakeRepository(FakeBackend(persist = { snapshot = it }), persistSession = { session = it })
  a.register("a@test.local", "pass123", "A"); a.save(urls[0], true); a.close()
  val restored = Json.decodeFromString<FakeSnapshot>(Json.encodeToString(snapshot)); val r = FakeRepository(FakeBackend(restored), session)
  try { assertEquals("A", r.state.value.user!!.name); assertEquals(urls[0], r.state.value.saved.first().url) } finally { r.close() }
 }
 @Test fun separateCouplesDoNotReceiveOtherCoupleTastes() = runTest {
  val db = FakeBackend(); val rs = List(4) { FakeRepository(db) }
  try { rs.forEachIndexed { i,r -> r.register("$i@test.local", "pass123", "$i") }; rs[1].join(rs[0].createCode()); rs[3].join(rs[2].createCode()); rs[0].publish(setOf("silver"), urls[0], true); assertTrue(rs[2].state.value.partnerTaste.tags.isEmpty()); assertTrue(rs[3].state.value.partnerTaste.tags.isEmpty()) } finally { rs.forEach { it.close() } }
 }
 @Test fun failedPersistenceDoesNotReportSuccessfulRegistration() = runTest { val r = FakeRepository(FakeBackend(persist = { error("disk full") })); try { assertTrue(runCatching { r.register("a@test.local", "pass123", "A") }.isFailure); assertNull(r.state.value.user) } finally { r.close() } }
}
