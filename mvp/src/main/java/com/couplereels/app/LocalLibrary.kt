package com.couplereels.app

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable enum class LocalPerson(val title: String) { ME("나"), PARTNER("체험 상대"); fun other() = if(this == ME) PARTNER else ME }
@Serializable enum class Sharing(val title: String) { SIMILAR("비슷한 콘텐츠 보여주기"), DIRECT("이 릴스 직접 보여주기") }
@Serializable enum class Classification { PENDING, UNCLASSIFIED, RULES, MANUAL, LEGACY }
@Serializable data class ReelCollection(val id: String, val name: String)
fun initialCollections() = listOf("flowers" to "꽃", "gift" to "선물", "food" to "맛집", "travel" to "여행", "fashion" to "패션", "anniversary" to "기념일", "other" to "기타", "unclassified" to "미분류").map { ReelCollection(it.first,it.second) }
@Serializable data class LocalReel(
 val url: String, val owner: LocalPerson, val savedAt: Long,
 val categories: Set<String> = setOf("unclassified"), val tags: Set<String> = emptySet(),
 val sharing: Sharing = Sharing.SIMILAR, val classification: Classification = Classification.PENDING,
 val caption: String = "", val knownSaveTime: Boolean = true
)
@Serializable data class LocalProfile(val taste: Taste = Taste(), val onboarded: Boolean = false,
 val reels: List<LocalReel> = emptyList(), val collections: List<ReelCollection> = initialCollections(),
 val defaultSharing: Sharing = Sharing.SIMILAR, val partnerFeed: Boolean = false)
@Serializable data class LibrarySnapshot(val version: Int = 8, val current: LocalPerson = LocalPerson.ME,
 val me: LocalProfile = LocalProfile(), val partner: LocalProfile = LocalProfile(onboarded=true)) {
 fun profile(person: LocalPerson) = if(person == LocalPerson.ME) me else partner
 fun withProfile(person: LocalPerson, profile: LocalProfile) = if(person == LocalPerson.ME) copy(me=profile) else copy(partner=profile)
}
data class SaveReceipt(val reel: LocalReel, val duplicate: Boolean)
/** Explicit privacy projection: a SIMILAR source never reaches a partner rendering/WebView model. */
data class PartnerLibrary(val direct: List<LocalReel>, val awaitingSimilar: Boolean)
fun LibrarySnapshot.partnerView(viewer: LocalPerson): PartnerLibrary {
 val source=profile(viewer.other()).reels
 return PartnerLibrary(source.filter { it.sharing == Sharing.DIRECT }.sortedByDescending { it.savedAt }, source.any {it.sharing == Sharing.SIMILAR})
}

/** Classifies only an actual shared caption. URL text/shortcodes cannot describe video content. */
object CaptionRules {
 private val rules = mapOf(
  "flowers" to listOf("꽃다발","장미","꽃집","플라워","bouquet","florist"),
  "gift" to listOf("선물","gift"), "food" to listOf("음식","식당","카페","맛집","restaurant","cafe"),
  "travel" to listOf("여행","숙소","travel","hotel"), "fashion" to listOf("의류","스타일링","코디","fashion"),
  "anniversary" to listOf("기념일","anniversary"))
 fun caption(shared: String) = shared.take(16000).replace(Regex("https?://[^\\s<>]+"), " ").trim().take(2000)
 fun classify(caption: String): Set<String> { val text=caption(caption).lowercase(); return rules.filterValues { words -> words.any { it in text } }.keys }
}

/** Single durable document, isolated from account/Firebase and legacy preferences.
 * All mutations commit before publishing state. The migration runs exactly once.
 */
class LocalLibrary(context: Context, private val clock: () -> Long = System::currentTimeMillis) {
 private val prefs=context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
 private val json=Json {ignoreUnknownKeys=true; encodeDefaults=true}
 private val mutable: MutableStateFlow<LibrarySnapshot>
 val state: StateFlow<LibrarySnapshot> get()=mutable
 init {
  val raw=prefs.getString("snapshot",null)
  val initial=if(raw!=null) json.decodeFromString<LibrarySnapshot>(raw).also {check(it.version==8){"저장 데이터 버전을 확인해 주세요."}} else {
   val old=DemoStore(context).load()
   val imported=old.saved.mapNotNull { saved -> ReelUrl.normalize(saved.url)?.let { url ->
    LocalReel(url,LocalPerson.ME,0,tags=saved.tags,classification=Classification.LEGACY,knownSaveTime=false)
   }}.distinctBy {it.url}
   LibrarySnapshot(me=LocalProfile(taste=old.taste,onboarded=old.onboarded,reels=imported)).also(::persist)
  }
  mutable=MutableStateFlow(initial)
 }
 private fun persist(next: LibrarySnapshot) {check(prefs.edit().putString("snapshot",json.encodeToString(next)).commit()) {"기기 저장 공간을 확인하고 다시 시도해 주세요."}}
 @Synchronized private fun update(block: (LibrarySnapshot)->LibrarySnapshot) { val next=block(mutable.value); persist(next); mutable.value=next }
 fun select(person:LocalPerson)=update {it.copy(current=person)}
 fun taste(person:LocalPerson,tags:Set<String>,finish:Boolean=true)=update { s->val p=s.profile(person);s.withProfile(person,p.copy(taste=p.taste.copy(tags=cleanTags(tags),updatedAt=clock()),onboarded=p.onboarded||finish)) }
 fun defaultSharing(person:LocalPerson,value:Sharing)=update {s->s.withProfile(person,s.profile(person).copy(defaultSharing=value))}
 fun feedSelection(person:LocalPerson,partner:Boolean)=update {s->s.withProfile(person,s.profile(person).copy(partnerFeed=partner))}
 @Synchronized fun save(shared: String, person:LocalPerson=state.value.current):SaveReceipt {
  val url=ReelUrl.fromShare(shared) ?: throw IllegalArgumentException("올바른 Instagram 릴스 링크를 공유해 주세요.")
  val profile=state.value.profile(person)
  profile.reels.find {it.url==url}?.let {return SaveReceipt(it,true)}
  val reel=LocalReel(url,person,clock(),sharing=profile.defaultSharing,caption=CaptionRules.caption(shared))
  update {s->s.withProfile(person,s.profile(person).copy(reels=listOf(reel)+s.profile(person).reels,partnerFeed=false))}
  return SaveReceipt(reel,false)
 }
 /** A separate, retryable pass after durable save; manual edits/deletes win races. */
 fun classifyPending()=update {s->
  LocalPerson.entries.fold(s) {acc,person -> val p=acc.profile(person); acc.withProfile(person,p.copy(reels=p.reels.map {r->
   if(r.classification!=Classification.PENDING) r else {
    val found=CaptionRules.classify(r.caption).filter {id->p.collections.any {it.id==id}}.toSet()
    r.copy(categories=found.ifEmpty {setOf("unclassified")},tags=found,classification=if(found.isEmpty()) Classification.UNCLASSIFIED else Classification.RULES)
   }
  }))}
 }
 fun sharing(person:LocalPerson,url:String,value:Sharing)=update {s->val p=s.profile(person);s.withProfile(person,p.copy(reels=p.reels.map {if(it.url==url) it.copy(sharing=value) else it}))}
 fun edit(person:LocalPerson,url:String,categories:Set<String>,tags:Set<String>)=update {s->
  val p=s.profile(person);val valid=categories.filter {id->p.collections.any {it.id==id}}.toSet().ifEmpty {setOf("unclassified")}
  s.withProfile(person,p.copy(reels=p.reels.map {if(it.url==url)it.copy(categories=valid,tags=cleanTags(tags),classification=Classification.MANUAL)else it}))
 }
 fun remove(person:LocalPerson,url:String)=update {s->val p=s.profile(person);s.withProfile(person,p.copy(reels=p.reels.filterNot {it.url==url}))}
 fun addCollection(person:LocalPerson,name:String)=update {s->
  val p=s.profile(person); val clean=validName(name); require(p.collections.none {it.name.equals(clean,true)}) {"이미 있는 컬렉션 이름이에요."}
  s.withProfile(person,p.copy(collections=p.collections+ReelCollection(UUID.randomUUID().toString(),clean)))
 }
 fun renameCollection(person:LocalPerson,id:String,name:String)=update {s->
  val p=s.profile(person);val clean=validName(name);require(id!="unclassified");require(p.collections.none {it.id!=id&&it.name.equals(clean,true)}) {"이미 있는 컬렉션 이름이에요."}
  s.withProfile(person,p.copy(collections=p.collections.map {if(it.id==id)it.copy(name=clean)else it}))
 }
 fun removeCollection(person:LocalPerson,id:String)=update {s->
  require(id!="unclassified");val p=s.profile(person)
  s.withProfile(person,p.copy(collections=p.collections.filterNot {it.id==id},reels=p.reels.map {r->r.copy(categories=(r.categories-id).ifEmpty {setOf("unclassified")} )}))
 }
 private fun validName(value:String)=value.trim().also {require(it.length in 1..20){"이름은 1~20자로 입력해 주세요."}}
 companion object {
  const val NAME="couplereels-library-p08-v1"
  @Volatile private var instance:LocalLibrary?=null
  fun get(context:Context):LocalLibrary=instance?:synchronized(this){instance?:LocalLibrary(context).also {instance=it}}
  private val worker=CoroutineScope(SupervisorJob()+Dispatchers.IO)
  fun classifyLater(store:LocalLibrary) {worker.launch {runCatching {store.classifyPending()}}}
 }
}
