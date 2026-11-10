package com.couplereels.app

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable data class FakeAccount(val profile: Profile, val passwordHash: String)
@Serializable data class FakeSnapshot(val accounts: Map<String, FakeAccount> = emptyMap(), val couples: Map<String, Couple> = emptyMap(), val tastes: Map<String, Map<String, Taste>> = emptyMap(), val saved: Map<String, List<SavedReel>> = emptyMap(), val personalTastes: Map<String, Taste> = emptyMap())
class FakeBackend(initial: FakeSnapshot = FakeSnapshot(), private val persist: (FakeSnapshot) -> Unit = {}) {
 internal val data = MutableStateFlow(initial)
 private val lock = Mutex()
 internal suspend fun change(action: (FakeSnapshot) -> FakeSnapshot) = lock.withLock { val next = action(data.value); persist(next); data.value = next }
}
/** Local-only test implementation; multiple repositories can share one backend in JVM tests. */
class FakeRepository(private val backend: FakeBackend, initialUid: String? = null, private val persistSession: (String?) -> Unit = {}) : CoupleRepository {
 private val session = MutableStateFlow(initialUid?.takeIf { it in backend.data.value.accounts })
 private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
 override val state = combine(backend.data, session) { db, uid ->
  val user = db.accounts[uid]?.profile
  val c = db.couples[user?.coupleId]?.takeIf { it.active && uid in it.members }
  val cid = user?.coupleId?.takeIf { c != null }
  AppData(user, cid, c?.members.orEmpty(), db.tastes[cid]?.get(uid) ?: db.personalTastes[uid] ?: Taste(), db.tastes[cid]?.filterKeys { it != uid }?.values?.firstOrNull() ?: Taste(), db.saved[uid].orEmpty())
 }.stateIn(scope, SharingStarted.Eagerly, AppData())
 private fun uid() = session.value ?: error("로그인이 필요합니다.")
 private fun setSession(uid: String?) { persistSession(uid); session.value = uid }
 override suspend fun register(email: String, password: String, name: String) {
  val normalized = email.trim().lowercase(); require(normalized.contains('@') && password.length >= 6) { "이메일과 6자 이상 비밀번호를 입력하세요." }
  val n = requireName(name); val id = UUID.randomUUID().toString()
  backend.change { db -> require(db.accounts.values.none { it.profile.email == normalized }) { "이미 등록된 이메일입니다." }; db.copy(accounts = db.accounts + (id to FakeAccount(Profile(id, normalized, n), digest(password)))) }
  setSession(id)
 }
 override suspend fun login(email: String, password: String) {
  val a = backend.data.value.accounts.values.find { it.profile.email == email.trim().lowercase() && it.passwordHash == digest(password) } ?: error("이메일 또는 비밀번호를 확인하세요.")
  setSession(a.profile.uid)
 }
 override suspend fun logout() = setSession(null)
 override suspend fun rename(name: String) { val n = requireName(name); val id = uid(); backend.change { db -> val a = db.accounts.getValue(id); db.copy(accounts = db.accounts + (id to a.copy(profile = a.profile.copy(name = n)))) } }
 override suspend fun createCode(): String {
  val id = uid(); val code = UUID.randomUUID().toString().replace("-", "").uppercase()
  backend.change { db ->
   val a = db.accounts.getValue(id); require(db.couples[a.profile.coupleId]?.active != true) { "기존 연결을 먼저 해제하세요." }
   val ownTaste = db.personalTastes[id]
   db.copy(couples = db.couples + (code to Couple(listOf(id))), accounts = db.accounts + (id to a.copy(profile = a.profile.copy(coupleId = code))),
    tastes = if (ownTaste != null && ownTaste.tags.isNotEmpty()) db.tastes + (code to (db.tastes[code].orEmpty() + (id to ownTaste))) else db.tastes)
  }
  return code
 }
 override suspend fun join(code: String) {
  val id = uid(); val key = code.trim().uppercase()
  backend.change { db ->
   val a = db.accounts.getValue(id); require(db.couples[a.profile.coupleId]?.active != true) { "기존 연결을 먼저 해제하세요." }
   val c = db.couples[key] ?: error("연결 코드를 확인하세요.")
   require(c.active && c.members.size == 1 && id !in c.members) { "사용할 수 없는 코드입니다." }
   val ownTaste = db.personalTastes[id]
   db.copy(couples = db.couples + (key to c.copy(members = c.members + id)), accounts = db.accounts + (id to a.copy(profile = a.profile.copy(coupleId = key))),
    tastes = if (ownTaste != null && ownTaste.tags.isNotEmpty()) db.tastes + (key to (db.tastes[key].orEmpty() + (id to ownTaste))) else db.tastes)
  }
 }
 override suspend fun unlink() {
  val id = uid(); backend.change { db ->
   val a = db.accounts.getValue(id); val cid = a.profile.coupleId; val c = db.couples[cid]
   val retained = db.personalTastes[id] ?: db.tastes[cid]?.get(id)
   db.copy(accounts = db.accounts + (id to a.copy(profile = a.profile.copy(coupleId = null))),
    couples = if (c != null && cid != null) db.couples + (cid to c.copy(active = false)) else db.couples,
    personalTastes = if (retained != null) db.personalTastes + (id to retained) else db.personalTastes)
  }
 }
 override suspend fun publish(tags: Set<String>, url: String?, privateSource: Boolean) {
  val t = requireTags(tags); val normalized = url?.takeIf { it.isNotBlank() }?.let { ReelUrl.normalize(it) ?: error("개별 Reel URL을 확인하세요.") }; val id = uid()
  backend.change { db ->
   val cid = db.accounts.getValue(id).profile.coupleId?.takeIf { candidate -> db.couples[candidate]?.let { it.active && id in it.members } == true }
   val previous = db.personalTastes[id] ?: db.tastes[cid]?.get(id) ?: Taste()
   val exclude = previous.excludedIds + if (normalized != null && privateSource) setOf(digest(normalized)) else emptySet()
   require(exclude.size <= 100) { "비공개 원본 100개 제한에 도달했습니다." }
   val taste = Taste(if (normalized != null) previous.tags + t else t, exclude, System.currentTimeMillis())
   val saved = if (normalized != null) {
    val existing = db.saved[id].orEmpty().find { it.url == normalized }
    db.saved + (id to (db.saved[id].orEmpty().filterNot { it.url == normalized } + SavedReel(normalized, existing?.tags.orEmpty() + t, liked = existing?.liked ?: false, privateSource = privateSource)))
   } else db.saved
   db.copy(personalTastes = db.personalTastes + (id to taste),
    tastes = if (cid != null) db.tastes + (cid to (db.tastes[cid].orEmpty() + (id to taste))) else db.tastes, saved = saved)
  }
 }
 override suspend fun saveRecommendation(url: String) {
  val valid = ReelUrl.normalize(url) ?: error("Reel URL 오류"); val id = uid()
  backend.change { db -> val saved = db.saved[id].orEmpty(); if (saved.any { it.url == valid }) db else db.copy(saved = db.saved + (id to (saved + SavedReel(valid)))) }
 }
 override suspend fun save(url: String, liked: Boolean) { val valid = ReelUrl.normalize(url) ?: error("Reel URL 오류"); val id = uid(); backend.change { db -> val prior = db.saved[id].orEmpty(); val item = prior.find { it.url == valid } ?: SavedReel(valid); db.copy(saved = db.saved + (id to (prior.filterNot { it.url == valid } + item.copy(liked = liked)))) } }
 override suspend fun removeSaved(url: String) { val id = uid(); backend.change { it.copy(saved = it.saved + (id to it.saved[id].orEmpty().filterNot { s -> s.url == url })) } }
 override fun close() { scope.cancel() }
}
