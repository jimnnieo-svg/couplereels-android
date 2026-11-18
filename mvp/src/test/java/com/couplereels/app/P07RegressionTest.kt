package com.couplereels.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class P07RegressionTest {
 private val context:Context get()=ApplicationProvider.getApplicationContext()
 @Before fun resetTestOnlyStorage() { context.getSharedPreferences(DemoUiStore.NAME,0).edit().clear().commit(); context.getSharedPreferences(DemoStore.NAME,0).edit().clear().commit() }
 @Test fun existingP06SnapshotAndGuestActivationRemainUntouched() {
  val prior=DemoSnapshot(Taste(setOf("silver","perfume"),updatedAt=123),listOf(SavedReel(ReelUrl.candidates[0],setOf("silver"),true)),true)
  val old=DemoStore(context); old.save(prior); old.setActive(true)
  val ui=DemoUiStore(context); ui.like("jewelry"); ui.bookmark("jewelry"); ui.hide("food")
  assertEquals(prior,DemoStore(context).load()); assertTrue(DemoStore(context).active)
 }
 @Test fun demoReactionsRestoreWithoutFabricatingSavedReelUrls() {
  val ui=DemoUiStore(context); ui.like("jewelry"); ui.bookmark("perfume"); ui.hide("food")
  val restored=DemoUiStore(context).state.value
  assertEquals(setOf("jewelry"),restored.liked); assertEquals(setOf("perfume"),restored.bookmarked); assertEquals(setOf("food"),restored.hidden)
  assertTrue(DemoStore(context).load().saved.isEmpty())
  ui.like("jewelry"); ui.bookmark("perfume"); ui.restoreHidden()
  assertEquals(DemoUiPreferences(),DemoUiStore(context).state.value)
 }
 @Test fun rejectsUnknownDemoIdWithoutMutation() {
  val ui=DemoUiStore(context)
  assertThrows(IllegalArgumentException::class.java) {ui.bookmark("https://example.com/fake")}
  assertEquals(DemoUiPreferences(),DemoUiStore(context).state.value)
 }
 @Test fun filtersUseSavedTagsAndJewelrySubcategories() {
  assertTrue(matchesSavedFilter(setOf("silver","necklace"),"jewelry"))
  assertTrue(matchesSavedFilter(setOf("food"),"date"))
  assertFalse(matchesSavedFilter(setOf("perfume"),"jewelry"))
  assertTrue(matchesSavedFilter(emptySet(),"all"))
 }
 @Test fun everyInterestHasOnlyExistingCompatibleStyleTags() {
  val sections=styleSections(interests.toSet())
  assertEquals(8,sections.size)
  assertTrue(sections.all {it.values.isNotEmpty() && it.values.all {v->v in tagVocabulary}})
  assertEquals(listOf("silver","gold","minimal","bold"),styleSections(setOf("jewelry")).single().values)
 }
}
