package com.couplereels.app

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class DemoRepositoryTest {
 @Test fun localWorkspaceNeedsNoAccountAndRestoresTasteAndSavedLinks() = runBlocking {
  var disk = ""
  val repo = DemoRepository { disk = Json.encodeToString(it) }
  assertNull(repo.state.value.user)
  assertTrue(repo.state.value.isDemo)
  repo.publish(setOf("jewelry", "silver"), null, true)
  repo.publish(setOf("gift"), ReelUrl.candidates[0] + "?igsh=test", true)
  repo.save(ReelUrl.candidates[0], true)
  val snapshot = Json.decodeFromString<DemoSnapshot>(disk)
  val restored = DemoRepository(snapshot)
  assertTrue(snapshot.onboarded)
  assertEquals(setOf("jewelry", "silver", "gift"), restored.state.value.myTaste.tags)
  assertEquals(ReelUrl.candidates[0], restored.state.value.saved.single().url)
  assertTrue(restored.state.value.saved.single().liked)
  assertNull(restored.state.value.user)
 }
 @Test fun rejectedStorageWriteNeverReportsSavedState() = runBlocking {
  val repo = DemoRepository { error("disk full") }
  try { repo.publish(setOf("gift"), ReelUrl.candidates[0], true); fail("must fail") } catch (_: IllegalStateException) { }
  assertTrue(repo.state.value.saved.isEmpty())
  assertTrue(repo.state.value.myTaste.tags.isEmpty())
 }
 @Test fun invalidUrlAndTagsCannotMutateStorage() = runBlocking {
  var writes = 0
  val repo = DemoRepository { writes++ }
  try { repo.publish(setOf("gift"), "https://example.com/reel/test", true); fail() } catch (_: IllegalStateException) { }
  try { repo.publish(setOf("unknown"), null, true); fail() } catch (_: IllegalArgumentException) { }
  assertEquals(0, writes)
 }
 @Test fun repeatedShareMergesTagsAndPreservesInterest() = runBlocking {
  val repo = DemoRepository()
  val url = ReelUrl.candidates[1]
  repo.publish(setOf("silver"), url, true)
  repo.save(url, true)
  repo.publish(setOf("gift"), "$url?utm_source=test", true)
  assertEquals(1, repo.state.value.saved.size)
  assertEquals(setOf("silver", "gift"), repo.state.value.saved.single().tags)
  assertTrue(repo.state.value.saved.single().liked)
 }
 @Test fun demoCannotAuthenticateOrCreateCoupleCodes() = runBlocking {
  val repo = DemoRepository()
  try { repo.register("demo@test.invalid", "password", "demo"); fail() } catch (_: IllegalStateException) { }
  try { repo.login("demo@test.invalid", "password"); fail() } catch (_: IllegalStateException) { }
  try { repo.createCode(); fail() } catch (_: IllegalStateException) { }
  assertNull(repo.state.value.user)
  assertNull(repo.state.value.coupleId)
 }
 @Test fun unavailableAuthenticationNeverCreatesLocalAccount() = runBlocking {
  val repo = UnavailableRepository()
  try { repo.register("demo@test.invalid", "password", "demo"); fail() } catch (_: IllegalStateException) { }
  assertNull(repo.state.value.user)
  assertFalse(repo.state.value.isDemo)
 }
 @Test fun recommendationOrderActuallyChangesWithSelectedTaste() {
  assertEquals("perfume", DemoRecommendations.rank(setOf("perfume")).first().content.id)
  assertEquals("travel", DemoRecommendations.rank(setOf("travel")).first().content.id)
  assertEquals(setOf("travel"), DemoRecommendations.rank(setOf("travel")).first().matchingTags)
  assertTrue(DemoRecommendations.rank(emptySet()).all { it.score == 0.0 })
 }
 @Test fun demoWorkspaceCannotChangeLegacyAccounts() = runBlocking {
  val backend = FakeBackend()
  val legacy = FakeRepository(backend)
  legacy.register("legacy@test.invalid", "secret", "Legacy")
  legacy.publish(setOf("fashion"), null, true)
  val before = legacy.state.value
  val demo = DemoRepository()
  demo.publish(setOf("travel"), ReelUrl.candidates[0], true)
  demo.logout()
  assertEquals(before, legacy.state.value)
  assertEquals(setOf("travel"), demo.state.value.myTaste.tags)
  legacy.close()
 }
}
