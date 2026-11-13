package com.couplereels.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore, private val catalogLoader: (String) -> List<Candidate> = { ReelUrl.candidates.map(::Candidate) }) : CoupleRepository {
 private val mutable = MutableStateFlow(AppData())
 override val state = mutable.asStateFlow()
 private var profileListener: ListenerRegistration? = null
 private var ownTasteListener: ListenerRegistration? = null
 private var legacyOwnListener: ListenerRegistration? = null
 private var ownedPreferenceExists = false
 private var coupleListener: ListenerRegistration? = null
 private var tasteListener: ListenerRegistration? = null
 private var savedListener: ListenerRegistration? = null
 private var generation = 0
 private var coupleGeneration = 0
 private var tasteGeneration = 0
 private var ownTaste = Taste()
 private var catalogOverride: Pair<String, List<Candidate>>? = null
 private fun privateCatalog(id: String) = catalogOverride?.takeIf { it.first == id }?.second ?: RecommendationProjection.catalog(catalogLoader(id))
 private val authListener = FirebaseAuth.AuthStateListener { observeUser() }
 init { auth.addAuthStateListener(authListener) }
 private fun uid() = auth.currentUser?.uid ?: error("다시 로그인하세요.")
 private fun userRef(id: String = uid()) = db.collection("users").document(id)
 private fun coupleRef(id: String) = db.collection("couples").document(id)
 private fun err(e: Exception?) { if (e != null) mutable.value = mutable.value.copy(error = e.userMessage()) }
 private fun observeUser() {
  generation++; val token = generation
  catalogOverride = null; ownedPreferenceExists = false
  profileListener?.remove(); savedListener?.remove(); ownTasteListener?.remove(); ownTaste = Taste(); stopCouple()
  mutable.value = AppData()
  val user = auth.currentUser ?: return
  mutable.value = AppData(user = Profile(user.uid, user.email.orEmpty(), ""))
  profileListener = userRef(user.uid).addSnapshotListener { snap, e ->
   if (token != generation) return@addSnapshotListener
   err(e)
   if (snap?.exists() == true) {
    val profile = Profile(user.uid, user.email.orEmpty(), snap.getString("name").orEmpty(), snap.getString("coupleId"))
    val prior = mutable.value.user?.coupleId
    mutable.value = mutable.value.copy(user = profile)
    if (prior != profile.coupleId || coupleListener == null) observeCouple(profile.coupleId, token)
   }
  }
  savedListener = userRef(user.uid).collection("saved").addSnapshotListener { snap, e ->
   if (token != generation) return@addSnapshotListener
   err(e); if (snap != null) mutable.value = mutable.value.copy(saved = snap.documents.mapNotNull { d -> d.getString("url")?.let { SavedReel(it, strings(d.get("tags")), d.getBoolean("liked") ?: false, d.getBoolean("privateSource") ?: true) } })
  }
  ownTasteListener = userRef(user.uid).collection("preferences").document("taste").addSnapshotListener { snap, e ->
   if (token != generation) return@addSnapshotListener
   err(e)
   if (snap != null) {
    ownedPreferenceExists = snap.exists()
    if (ownedPreferenceExists) { ownTaste = taste(snap); mutable.value = mutable.value.copy(myTaste = ownTaste) }
   }
  }
 }
 private fun stopCouple() { legacyOwnListener?.remove(); legacyOwnListener = null; coupleGeneration++; tasteGeneration++; coupleListener?.remove(); coupleListener = null; tasteListener?.remove(); tasteListener = null; mutable.value = mutable.value.copy(coupleId = null, members = emptyList(), myTaste = ownTaste, partnerTaste = Taste(), partnerReels = null, partnerUpdatedAt = 0) }
 private fun observeCouple(cid: String?, token: Int) {
  stopCouple(); if (cid == null) return
  val ct = coupleGeneration
  val ownerId = uid()
  legacyOwnListener = coupleRef(cid).collection("tastes").document(ownerId).addSnapshotListener { legacy, error ->
   if (token != generation || ct != coupleGeneration) return@addSnapshotListener
   if (error != null) { err(error); return@addSnapshotListener }
   if (!ownedPreferenceExists && legacy?.exists() == true) { ownTaste = taste(legacy); mutable.value = mutable.value.copy(myTaste = ownTaste) }
  }
  coupleListener = coupleRef(cid).addSnapshotListener { snap, e ->
   if (token != generation || ct != coupleGeneration) return@addSnapshotListener
   if (e != null || snap?.getBoolean("active") != true || uid() !in strings(snap.get("members"))) { tasteGeneration++; tasteListener?.remove(); tasteListener = null; mutable.value = mutable.value.copy(coupleId = null, members = emptyList(), myTaste = ownTaste, partnerTaste = Taste(), partnerReels = null, partnerUpdatedAt = 0); err(e); return@addSnapshotListener }
   mutable.value = mutable.value.copy(coupleId = cid, members = strings(snap.get("members")).toList())
   if (tasteListener == null) {
    val tt = tasteGeneration
    tasteListener = coupleRef(cid).collection("recommendations").addSnapshotListener projection@{ data, error ->
     if (token != generation || ct != coupleGeneration || tt != tasteGeneration) return@projection
     if (error != null) { mutable.value = mutable.value.copy(partnerTaste = Taste(), partnerReels = null, partnerUpdatedAt = 0); err(error); return@projection }
     if (data != null) {
      val partner = data.documents.firstOrNull { it.id != uid() && it.id in mutable.value.members }
      val urls = partner?.let { strings(it.get("urls")).mapNotNull(ReelUrl::normalize).filter { u -> u in ReelUrl.candidates }.take(RecommendationProjection.MAX_PUBLIC_RESULTS) }
      mutable.value = mutable.value.copy(myTaste = ownTaste, partnerTaste = Taste(), partnerReels = urls, partnerUpdatedAt = partner?.getTimestamp("updatedAt")?.toDate()?.time ?: 0, error = null)
     }

   }
   }
  }
 }
 override suspend fun register(email: String, password: String, name: String) {
  val n = requireName(name); require(password.length >= 6) { "비밀번호는 6자 이상입니다." }
  val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
  val user = result.user ?: error("가입 실패")
  userRef(user.uid).set(mapOf("uid" to user.uid, "email" to user.email.orEmpty(), "name" to n, "coupleId" to null)).await()
 }
 override suspend fun login(email: String, password: String) { auth.signInWithEmailAndPassword(email.trim(), password).await() }
 override suspend fun logout() { auth.signOut() }
 // Also repairs a profile if Auth succeeded but the initial Firestore write failed.
 override suspend fun rename(name: String) {
  val n = requireName(name); val user = auth.currentUser ?: error("로그인이 필요합니다.")
  db.runTransaction { tx -> val ref = userRef(user.uid); val old = tx.get(ref); if (old.exists()) tx.update(ref, "name", n) else tx.set(ref, mapOf("uid" to user.uid, "email" to user.email.orEmpty(), "name" to n, "coupleId" to null)) }.await()
 }
 private fun ensureFree(tx: Transaction, profile: DocumentSnapshot) {
  require(profile.exists()) { "프로필 이름을 먼저 저장하세요." }
  val old = profile.getString("coupleId")
  if (old != null) require(tx.get(coupleRef(old)).getBoolean("active") != true) { "기존 연결을 먼저 해제하세요." }
 }
 private fun privateTasteForMigration(tx: Transaction, profile: DocumentSnapshot, id: String): Taste {
  val owned = tx.get(userRef(id).collection("preferences").document("taste"))
  if (owned.exists()) return taste(owned)
  val old = profile.getString("coupleId") ?: return Taste()
  return taste(tx.get(coupleRef(old).collection("tastes").document(id)))
 }
 private fun retainOwnTaste(tx: Transaction, id: String, source: Taste) {
  if (source.tags.isNotEmpty()) tx.set(userRef(id).collection("preferences").document("taste"), mapOf("tags" to source.tags.toList(), "excludedIds" to source.excludedIds.toList(), "updatedAt" to FieldValue.serverTimestamp()))
 }
 override suspend fun createCode(): String {
  val id = uid(); val code = UUID.randomUUID().toString().replace("-", "").uppercase()
  db.runTransaction { tx ->
   val profile = tx.get(userRef(id)); ensureFree(tx, profile)
   val source = privateTasteForMigration(tx, profile, id)
   tx.set(coupleRef(code), mapOf("members" to listOf(id), "active" to true))
   tx.update(userRef(id), "coupleId", code); retainOwnTaste(tx, id, source)
  }.await()
  try { syncRecommendationsToCouple() } catch (e: Exception) { err(e) }
  return code
 }
 override suspend fun join(code: String) {
  val key = code.trim().uppercase(); require(Regex("[A-F0-9]{32}").matches(key)) { "32자리 연결 코드를 확인하세요." }; val id = uid()
  db.runTransaction { tx ->
   val profile = tx.get(userRef(id)); val couple = tx.get(coupleRef(key)); ensureFree(tx, profile)
   val members = strings(couple.get("members")); require(couple.getBoolean("active") == true && members.size == 1 && id !in members) { "만료되었거나 사용 중인 연결 코드입니다." }
   val source = privateTasteForMigration(tx, profile, id)
   tx.update(coupleRef(key), "members", members.toList() + id)
   tx.update(userRef(id), "coupleId", key); retainOwnTaste(tx, id, source)
  }.await()
  try { syncRecommendationsToCouple() } catch (e: Exception) { err(e) }
 }
 override suspend fun unlink() {
  val id = uid()
  db.runTransaction { tx ->
   val ref = userRef(id); val p = tx.get(ref); val cid = p.getString("coupleId"); val c = cid?.let { tx.get(coupleRef(it)) }
   val shared = if (cid != null && c?.getBoolean("active") == true) tx.get(coupleRef(cid).collection("tastes").document(id)) else null
   val ownedRef = userRef(id).collection("preferences").document("taste")
   val owned = if (cid != null) tx.get(ownedRef) else null
   if (owned?.exists() == false && shared?.exists() == true) {
    val old = taste(shared)
    if (old.tags.isNotEmpty()) tx.set(ownedRef, mapOf("tags" to old.tags.toList(), "excludedIds" to old.excludedIds.toList(), "updatedAt" to FieldValue.serverTimestamp()))
   }
   if (cid != null && c?.getBoolean("active") == true) tx.update(coupleRef(cid), "active", false)
   tx.update(ref, "coupleId", null)
  }.await()
 }
 /** The author's device computes the outgoing list; no private inputs are sent to the partner. */
 private suspend fun syncRecommendationsToCouple(candidates: List<Candidate> = privateCatalog(uid())) {
  val id = uid()
  db.runTransaction { tx ->
   val profile = tx.get(userRef(id)); val cid = profile.getString("coupleId") ?: return@runTransaction
   val couple = tx.get(coupleRef(cid))
   if (couple.getBoolean("active") != true || id !in strings(couple.get("members"))) return@runTransaction
   val owned = tx.get(userRef(id).collection("preferences").document("taste"))
   val legacy = if (!owned.exists()) tx.get(coupleRef(cid).collection("tastes").document(id)) else null
   val source = if (owned.exists()) taste(owned) else taste(legacy)
   val urls = RecommendationProjection.urls(source, candidates) ?: return@runTransaction
   tx.set(coupleRef(cid).collection("recommendations").document(id), mapOf("urls" to urls, "updatedAt" to FieldValue.serverTimestamp()))
   Unit
  }.await()
 }
 override suspend fun updateCandidateCatalog(candidates: List<Candidate>) {
  val id = uid(); val safe = RecommendationProjection.catalog(candidates)
  syncRecommendationsToCouple(safe)
  check(auth.currentUser?.uid == id) { "계정이 변경됐어요. 다시 시도하세요." }
  catalogOverride = id to safe
 }
 override suspend fun publish(tags: Set<String>, url: String?, privateSource: Boolean) {
  val t = requireTags(tags); val link = url?.takeIf { it.isNotBlank() }?.let { ReelUrl.normalize(it) ?: error("Reel URL을 확인하세요.") }; val id = uid()
  db.runTransaction { tx ->
   val p = tx.get(userRef(id)); val cid = p.getString("coupleId")
   val couple = cid?.let { tx.get(coupleRef(it)) }
   val linked = cid != null && (couple?.let { it.getBoolean("active") == true && id in strings(it.get("members")) } == true)
   val ownedRef = userRef(id).collection("preferences").document("taste")
   val previousOwn = tx.get(ownedRef)
   val ref = if (linked) coupleRef(cid!!).collection("tastes").document(id) else null
   val previousShared = ref?.let { tx.get(it) }
   val excluded = strings(previousOwn.get("excludedIds")) + strings(previousShared?.get("excludedIds")) + if (link != null && privateSource) setOf(digest(link)) else emptySet()
   require(excluded.size <= 100) { "비공개 원본 100개 제한입니다." }
   val savedRef = link?.let { userRef(id).collection("saved").document(digest(it)) }
   val savedBefore = savedRef?.let { tx.get(it) }
   val cumulativeTags = if (link != null) strings(previousOwn.get("tags")) + strings(previousShared?.get("tags")) + t else t
   val body = mapOf("tags" to cumulativeTags.toList(), "excludedIds" to excluded.toList(), "updatedAt" to FieldValue.serverTimestamp())
   tx.set(ownedRef, body)
   if (ref != null && cid != null) {
    tx.set(coupleRef(cid).collection("recommendations").document(id), mapOf("urls" to RecommendationProjection.urls(Taste(cumulativeTags, excluded), privateCatalog(id)).orEmpty(), "updatedAt" to FieldValue.serverTimestamp()))
   }
   if (link != null && savedRef != null) tx.set(savedRef, mapOf("url" to link, "tags" to (strings(savedBefore?.get("tags")) + t).toList(), "liked" to (savedBefore?.getBoolean("liked") ?: false), "privateSource" to privateSource))
  }.await()
 }
 override suspend fun saveRecommendation(url: String) {
  val id = uid()
  val link = ReelUrl.normalize(url) ?: error("Reel URL 오류")
  db.runTransaction { tx -> val ref = userRef(id).collection("saved").document(digest(link)); if (!tx.get(ref).exists()) tx.set(ref, mapOf("url" to link, "tags" to emptyList<String>(), "liked" to false, "privateSource" to true)) }.await()
 }
 override suspend fun save(url: String, liked: Boolean) {
  val id = uid()
  val link = ReelUrl.normalize(url) ?: error("Reel URL 오류")
  db.runTransaction { tx -> val ref = userRef(id).collection("saved").document(digest(link)); val old = tx.get(ref); if (old.exists()) tx.update(ref, "liked", liked) else tx.set(ref, mapOf("url" to link, "tags" to emptyList<String>(), "liked" to liked, "privateSource" to true)) }.await()
 }
 override suspend fun removeSaved(url: String) { val id = uid(); val ref = userRef(id).collection("saved").document(digest(url)); db.runTransaction { tx -> tx.get(ref); tx.delete(ref) }.await() }
 override fun close() { auth.removeAuthStateListener(authListener); profileListener?.remove(); savedListener?.remove(); ownTasteListener?.remove(); stopCouple(); generation++ }
 companion object {
  private fun strings(value: Any?): Set<String> = (value as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
  private fun taste(doc: DocumentSnapshot?): Taste = if (doc == null) Taste() else Taste(strings(doc.get("tags")), strings(doc.get("excludedIds")), doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0)
 }
}
