package com.couplereels.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class LocalLibraryTest {
 private val context:Context get()=ApplicationProvider.getApplicationContext()
 private val a="https://www.instagram.com/reel/LOCAL_A/"
 private val b="https://www.instagram.com/reel/LOCAL_B/"
 @Before fun resetIsolatedTestStorage(){listOf(LocalLibrary.NAME,DemoStore.NAME,"couplereels-demo-v1").forEach {context.getSharedPreferences(it,0).edit().clear().commit()}}
 @Test fun migrationPreservesOldStoreAndImportsExactlyOnce(){
  val old=DemoSnapshot(Taste(setOf("silver")),listOf(SavedReel(a,setOf("silver"))),true);DemoStore(context).save(old)
  val prefs=context.getSharedPreferences(DemoStore.NAME,0);val raw=prefs.getString("snapshot",null)
  val store=LocalLibrary(context);assertEquals(old.taste,store.state.value.me.taste);assertEquals(a,store.state.value.me.reels.single().url)
  assertEquals(Sharing.SIMILAR,store.state.value.me.reels.single().sharing);assertTrue(store.state.value.partner.reels.isEmpty())
  store.remove(LocalPerson.ME,a);assertTrue(LocalLibrary(context).state.value.me.reels.isEmpty());assertEquals(raw,prefs.getString("snapshot",null))
 }
 @Test fun saveIsDurableBeforeAnyClassification(){val store=LocalLibrary(context){123};val receipt=store.save("꽃다발 선물 $a")
  assertFalse(receipt.duplicate);val restored=LocalLibrary(context).state.value.me.reels.single();assertEquals(123L,restored.savedAt);assertEquals(Classification.PENDING,restored.classification);assertEquals(setOf("unclassified"),restored.categories)
  store.classifyPending();assertEquals(setOf("flowers","gift"),store.state.value.me.reels.single().categories)
 }
 @Test fun urlAloneNeverClassifiesFromItsShortcode(){val store=LocalLibrary(context);store.save("https://www.instagram.com/reel/gift_flower_cafe/");store.classifyPending();assertEquals(Classification.UNCLASSIFIED,store.state.value.me.reels.single().classification)}
 @Test fun similarProjectionContainsNoOriginalOrCaptionThenRevokesDirectImmediately(){
  val store=LocalLibrary(context);store.save("secret $a")
  fun projected()=store.state.value.partnerView(LocalPerson.PARTNER)
  assertTrue(projected().awaitingSimilar);assertTrue(projected().direct.isEmpty());assertFalse(projected().toString().contains("LOCAL_A"));assertFalse(projected().toString().contains("secret"))
  store.sharing(LocalPerson.ME,a,Sharing.DIRECT);assertEquals(a,projected().direct.single().url)
  store.sharing(LocalPerson.ME,a,Sharing.SIMILAR);assertTrue(projected().direct.isEmpty());assertEquals(a,store.state.value.me.reels.single().url)
  assertEquals(Sharing.SIMILAR,LocalLibrary(context).state.value.me.reels.single().sharing)
 }
 @Test fun twoProfilesMaySaveSameUrlWithoutSharingSettingsOrTaste(){
  val store=LocalLibrary(context);store.save(a,LocalPerson.ME);store.select(LocalPerson.PARTNER);store.defaultSharing(LocalPerson.PARTNER,Sharing.DIRECT);store.save(a);store.taste(LocalPerson.PARTNER,setOf("flowers"));store.taste(LocalPerson.ME,setOf("silver"))
  val s=LocalLibrary(context).state.value;assertEquals(LocalPerson.PARTNER,s.current);assertEquals(setOf("silver"),s.me.taste.tags);assertEquals(setOf("flowers"),s.partner.taste.tags);assertEquals(Sharing.SIMILAR,s.me.reels.single().sharing);assertEquals(Sharing.DIRECT,s.partner.reels.single().sharing)
 }
 @Test fun changingDefaultAffectsOnlyFutureSavesAndDuplicateDoesNotResetIt(){val store=LocalLibrary(context);store.save(a);store.defaultSharing(LocalPerson.ME,Sharing.DIRECT);store.save(b);assertTrue(store.save("$a?igsh=test").duplicate);assertEquals(Sharing.SIMILAR,store.state.value.me.reels.find {it.url==a}!!.sharing);assertEquals(Sharing.DIRECT,store.state.value.me.reels.find {it.url==b}!!.sharing)}
 @Test fun manualEditWinsOverDeferredClassificationAndDeletionCannotResurrect(){val store=LocalLibrary(context);store.save("꽃다발 $a");store.edit(LocalPerson.ME,a,setOf("travel"),setOf("travel"));store.classifyPending();assertEquals(setOf("travel"),store.state.value.me.reels.single().categories);store.save("선물 $b");store.remove(LocalPerson.ME,b);store.classifyPending();assertEquals(listOf(a),store.state.value.me.reels.map {it.url})}
 @Test fun categoryRenameKeepsIdentityAndDeletionKeepsReels(){val store=LocalLibrary(context);store.save(a);store.edit(LocalPerson.ME,a,setOf("flowers"),emptySet());store.renameCollection(LocalPerson.ME,"flowers","꽃집");assertEquals(setOf("flowers"),store.state.value.me.reels.single().categories);store.removeCollection(LocalPerson.ME,"flowers");assertEquals(setOf("unclassified"),store.state.value.me.reels.single().categories);assertEquals(a,LocalLibrary(context).state.value.me.reels.single().url)}
 @Test fun customCollectionsAreProfileLocal(){val store=LocalLibrary(context);store.addCollection(LocalPerson.ME,"데이트 후보");val c=store.state.value.me.collections.last();assertFalse(store.state.value.partner.collections.any {it.id==c.id});store.save(a);store.edit(LocalPerson.ME,a,setOf(c.id),emptySet());assertEquals(setOf(c.id),LocalLibrary(context).state.value.me.reels.single().categories)}
 @Test fun malformedUrlsDoNotMutateAndQueriesDeduplicate(){val store=LocalLibrary(context);listOf("http://www.instagram.com/reel/A/","https://instagram.com.evil/reel/A/","https://u@instagram.com/reel/A/","https://www.instagram.com/p/A/","https://www.instagram.com/reel/A%2fB/").forEach {assertThrows(IllegalArgumentException::class.java){store.save(it)}};assertTrue(store.state.value.me.reels.isEmpty());store.save("($a?igsh=abc&utm_source=test).");assertTrue(store.save(a).duplicate)}
 @Test fun concurrentShareDuplicatesCommitOneEntry(){val store=LocalLibrary(context);val pool=Executors.newFixedThreadPool(4);try {val futures=(1..20).map {pool.submit<SaveReceipt>{store.save(a)}};assertEquals(1,futures.count {!it.get().duplicate});assertEquals(1,LocalLibrary(context).state.value.me.reels.size)}finally{pool.shutdownNow()}}
 @Test fun corruptCurrentSnapshotIsNotSilentlyOverwritten(){val prefs=context.getSharedPreferences(LocalLibrary.NAME,0);prefs.edit().putString("snapshot","broken").commit();assertThrows(Exception::class.java){LocalLibrary(context)};assertEquals("broken",prefs.getString("snapshot",null))}
 @Test fun profileAndFeedSelectionSurviveRestart(){val store=LocalLibrary(context);store.select(LocalPerson.PARTNER);store.feedSelection(LocalPerson.PARTNER,true);val s=LocalLibrary(context).state.value;assertEquals(LocalPerson.PARTNER,s.current);assertTrue(s.partner.partnerFeed);assertFalse(s.me.partnerFeed)}
 @Test fun newSaveSelectsOwnFeedWithoutChangingOtherProfileSelection(){val store=LocalLibrary(context);store.feedSelection(LocalPerson.ME,true);store.feedSelection(LocalPerson.PARTNER,true);store.save(a,LocalPerson.ME);assertFalse(store.state.value.me.partnerFeed);assertTrue(store.state.value.partner.partnerFeed)}
 @Test fun legacyAccountPreferencesAreNotTouched(){val prefs=context.getSharedPreferences("couplereels-demo-v1",0);prefs.edit().putString("account","preserve").commit();val store=LocalLibrary(context);store.save(a);store.select(LocalPerson.PARTNER);store.save(b);assertEquals("preserve",prefs.getString("account",null))}
}
