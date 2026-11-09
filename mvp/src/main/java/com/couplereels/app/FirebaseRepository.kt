package com.couplereels.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore) : CoupleRepository {
 private val mutable = MutableStateFlow(AppData())
 override val state = mutable.asStateFlow()
 private var profileListener: ListenerRegistration? = null
 private var coupleListener: ListenerRegistration? = null
 private var tasteListener: ListenerRegistration? = null
 private var savedListener: ListenerRegistration? = null
 private var generation = 0
 private var coupleGeneration = 0
 private var tasteGeneration = 0
 private val authListener = FirebaseAuth.AuthStateListener { observeUser() }
 init { auth.addAuthStateListener(authListener) }
 private fun uid() = auth.currentUser?.uid ?: error("다시 로그인하세요.")
 private fun userRef(id: String = uid()) = db.collection("users").document(id)
 private fun coupleRef(id: String) = db.collection("couples").document(id)
 private fun err(e: Exception?) { if (e != null) mutable.value = mutable.value.copy(error = e.userMessage()) }
 private fun observeUser() {
  generation++; val token = generation
  profileListener?.remove(); savedListener?.remove(); stopCouple()
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
 }
 private fun stopCouple() { coupleGeneration++; tasteGeneration++; coupleListener?.remove(); coupleListener = null; tasteListener?.remove(); tasteListener = null; mutable.value = mutable.value.copy(coupleId = null, members = emptyList(), myTaste = Taste(), partnerTaste = Taste()) }
 private fun observeCouple(cid: String?, token: Int) {
  stopCouple(); if (cid == null) return
  val ct = coupleGeneration
  coupleListener = coupleRef(cid).addSnapshotListener { snap, e ->
   if (token != generation || ct != coupleGeneration) return@addSnapshotListener
   if (e != null || snap?.getBoolean("active") != true || uid() !in strings(snap.get("members"))) { tasteGeneration++; tasteListener?.remove(); tasteListener = null; mutable.value = mutable.value.copy(coupleId = null, members = emptyList(), myTaste = Taste(), partnerTaste = Taste()); err(e); return@addSnapshotListener }
   mutable.value = mutable.value.copy(coupleId = cid, members = strings(snap.get("members")).toList())
   if (tasteListener == null) {
    val tt = tasteGeneration
    tasteListener = coupleRef(cid).collection("tastes").addSnapshotListener taste@{ data, error ->
    if (token != generation || ct != coupleGeneration || tt != tasteGeneration) return@taste
    if (error != null) { mutable.value = mutable.value.copy(myTaste = Taste(), partnerTaste = Taste()); err(error); return@taste }
    if (data != null) {
     val mine = data.documents.firstOrNull { it.id == uid() }
     val partner = data.documents.firstOrNull { it.id != uid() }
     mutable.value = mutable.value.copy(myTaste = taste(mine), partnerTaste = taste(partner), error = null)
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
  db.runTransaction { tx -> val ref = userRef(); val old = tx.get(ref); if (old.exists()) tx.update(ref, "name", n) else tx.set(ref, mapOf("uid" to user.uid, "email" to user.email.orEmpty(), "name" to n, "coupleId" to null)) }.await()
 }
 private fun ensureFree(tx: Transaction, profile: DocumentSnapshot) {
  require(profile.exists()) { "프로필 이름을 먼저 저장하세요." }
  val old = profile.getString("coupleId")
  if (old != null) require(tx.get(coupleRef(old)).getBoolean("active") != true) { "기존 연결을 먼저 해제하세요." }
 }
 override suspend fun createCode(): String {
  val id = uid(); val code = UUID.randomUUID().toString().replace("-", "").uppercase()
  db.runTransaction { tx -> val profile = tx.get(userRef()); ensureFree(tx, profile); tx.set(coupleRef(code), mapOf("members" to listOf(id), "active" to true)); tx.update(userRef(), "coupleId", code) }.await()
  return code
 }
 override suspend fun join(code: String) {
  val key = code.trim().uppercase(); require(Regex("[A-F0-9]{32}").matches(key)) { "32자리 연결 코드를 확인하세요." }; val id = uid()
  db.runTransaction { tx ->
   val profile = tx.get(userRef()); val couple = tx.get(coupleRef(key)); ensureFree(tx, profile)
   val members = strings(couple.get("members")); require(couple.getBoolean("active") == true && members.size == 1 && id !in members) { "만료되었거나 사용 중인 연결 코드입니다." }
   tx.update(coupleRef(key), "members", members.toList() + id); tx.update(userRef(), "coupleId", key)
  }.await()
 }
 override suspend fun unlink() {
  db.runTransaction { tx -> val ref = userRef(); val p = tx.get(ref); val cid = p.getString("coupleId"); val c = cid?.let { tx.get(coupleRef(it)) }; if (cid != null && c?.getBoolean("active") == true) tx.update(coupleRef(cid), "active", false); tx.update(ref, "coupleId", null) }.await()
 }
 override suspend fun publish(tags: Set<String>, url: String?, privateSource: Boolean) {
  val t = requireTags(tags); val link = url?.takeIf { it.isNotBlank() }?.let { ReelUrl.normalize(it) ?: error("Reel URL을 확인하세요.") }; val id = uid()
  db.runTransaction { tx ->
   val p = tx.get(userRef()); val cid = p.getString("coupleId") ?: error("커플 연결이 필요합니다.")
   val c = tx.get(coupleRef(cid)); require(c.getBoolean("active") == true && id in strings(c.get("members"))) { "연결이 해제되었습니다." }
   val ref = coupleRef(cid).collection("tastes").document(id); val old = tx.get(ref)
   val excluded = strings(old.get("excludedIds")) + if (link != null && privateSource) setOf(digest(link)) else emptySet()
   require(excluded.size <= 100) { "비공개 원본 100개 제한입니다." }
   val savedRef = link?.let { userRef().collection("saved").document(digest(it)) }
   val savedBefore = savedRef?.let { tx.get(it) }
   val body = mapOf("tags" to t.toList(), "excludedIds" to excluded.toList(), "updatedAt" to FieldValue.serverTimestamp())
   tx.set(ref, body)
   tx.set(coupleRef(cid).collection("events").document(), mapOf("authorId" to id, "tags" to t.toList(), "updatedAt" to FieldValue.serverTimestamp()))
   if (link != null && savedRef != null) tx.set(savedRef, mapOf("url" to link, "tags" to t.toList(), "liked" to (savedBefore?.getBoolean("liked") ?: false), "privateSource" to privateSource))
  }.await()
 }
 override suspend fun saveRecommendation(url: String) {
  val link = ReelUrl.normalize(url) ?: error("Reel URL 오류")
  db.runTransaction { tx -> val ref = userRef().collection("saved").document(digest(link)); if (!tx.get(ref).exists()) tx.set(ref, mapOf("url" to link, "tags" to emptyList<String>(), "liked" to false, "privateSource" to true)) }.await()
 }
 override suspend fun save(url: String, liked: Boolean) {
  val link = ReelUrl.normalize(url) ?: error("Reel URL 오류")
  db.runTransaction { tx -> val ref = userRef().collection("saved").document(digest(link)); val old = tx.get(ref); if (old.exists()) tx.update(ref, "liked", liked) else tx.set(ref, mapOf("url" to link, "tags" to emptyList<String>(), "liked" to liked, "privateSource" to true)) }.await()
 }
 override suspend fun removeSaved(url: String) { val ref = userRef().collection("saved").document(digest(url)); db.runTransaction { tx -> tx.get(ref); tx.delete(ref) }.await() }
 override fun close() { auth.removeAuthStateListener(authListener); profileListener?.remove(); savedListener?.remove(); stopCouple(); generation++ }
 companion object {
  private fun strings(value: Any?): Set<String> = (value as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
  private fun taste(doc: DocumentSnapshot?): Taste = if (doc == null) Taste() else Taste(strings(doc.get("tags")), strings(doc.get("excludedIds")), doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0)
 }
}
