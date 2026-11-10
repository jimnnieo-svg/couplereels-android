package com.couplereels.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val blushPink = Color(0xFFED82A2)
private val softPink = Color(0xFFFBE6EC)
private val ivory = Color(0xFFFFF9F7)
private val charcoal = Color(0xFF302B30)
private val tagLabels = mapOf(
 "silver" to "실버", "necklace" to "목걸이", "minimal" to "미니멀",
 "gift" to "선물", "pendant" to "펜던트", "daily" to "데일리",
 "jewelry" to "주얼리", "925" to "925 실버", "budget" to "가성비",
 "anniversary" to "기념일", "date" to "데이트"
)

class MvpActivity : ComponentActivity() {
 private var incoming by mutableStateOf("")
 private var shareError by mutableStateOf<String?>(null)
 private val json = Json { ignoreUnknownKeys = true }
 private val prefs by lazy { getSharedPreferences("couplereels-demo-v1", MODE_PRIVATE) }
 private val backend by lazy {
  val data = runCatching { json.decodeFromString<FakeSnapshot>(prefs.getString("db", "{}")!!) }.getOrDefault(FakeSnapshot())
  FakeBackend(data) { snapshot -> check(prefs.edit().putString("db", json.encodeToString(snapshot)).commit()) { "기기 저장 공간을 확인하세요." } }
 }
 private val firebaseAvailable by lazy { FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null }
 private val firestore by lazy { FirebaseFirestore.getInstance().apply { firestoreSettings = FirebaseFirestoreSettings.Builder().setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build() } }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); if (savedInstanceState == null) receive(intent)
  setContent { MaterialTheme(colorScheme = lightColorScheme(primary = blushPink, onPrimary = Color.White, background = ivory, onBackground = charcoal, surface = ivory, onSurface = charcoal, secondaryContainer = softPink, onSecondaryContainer = charcoal)) { Surface(Modifier.fillMaxSize()) { App() } } }
 }
 override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
 private fun receive(i: Intent) {
  if (i.action != Intent.ACTION_SEND) return
  val url = ReelIntents.sharedUrl(i)
  if (url == null) { incoming = ""; shareError = "릴스 링크를 받지 못했어요. 인스타그램 릴스의 링크 공유를 선택하거나 링크를 복사해서 붙여넣어 주세요." }
  else { shareError = null; incoming = url }
 }
 private fun play(url: String?) { startActivity(ReelIntents.playback(this, url)) }
 @Composable private fun App() {
  var real by remember { mutableStateOf(firebaseAvailable && prefs.getBoolean("useFirebase", true)) }
  val repo: CoupleRepository = remember(real) { if (real) FirebaseRepository(FirebaseAuth.getInstance(), firestore) else FakeRepository(backend, prefs.getString("session", null)) { uid -> prefs.edit().putString("session", uid).commit(); Unit } }
  DisposableEffect(repo) { onDispose { repo.close() } }
  val state by repo.state.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  var busy by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf("") }
  var page by rememberSaveable { mutableStateOf("연결") }
  var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
  var code by remember { mutableStateOf("") }
  var link by rememberSaveable { mutableStateOf(incoming) }
  var shareSheetUrl by rememberSaveable { mutableStateOf("") }
  var shareTags by remember { mutableStateOf(emptySet<String>()) }
  var shareSaveError by remember { mutableStateOf("") }
  var tags by remember { mutableStateOf(emptySet<String>()) }
  var previousOwner by remember { mutableStateOf<String?>(null) }
  val catalog = remember { MutableStateFlow(CandidateCatalog()) }
  val candidates = catalog.collectAsStateWithLifecycle().value.items
  val feedScope = remember(repo) { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
  DisposableEffect(feedScope) { onDispose { feedScope.cancel() } }
  val feedFlow = remember(repo) { PartnerFeed.observe(repo, catalog, feedScope) }
  val feed by feedFlow.collectAsStateWithLifecycle()
  fun action(onSuccess: (() -> Unit)? = null, block: suspend () -> String) { if (!busy) scope.launch { busy = true; message = ""; try { val result = withTimeout(25000) { block() }; message = result; onSuccess?.invoke() } catch (e: TimeoutCancellationException) { message = "응답 시간 초과. 연결·권한을 확인하고 상태를 확인한 후 재시도하세요." } catch (e: CancellationException) { throw e } catch (e: Exception) { message = e.userMessage() } finally { busy = false } } }
  LaunchedEffect(incoming) {
   if (incoming.isNotBlank()) {
    link = incoming; page = "취향/공유"; shareSheetUrl = incoming; shareSaveError = ""
    shareTags = prefs.getStringSet("${real}:${state.user?.uid}:last-share-tags", emptySet()).orEmpty().toSet()
    incoming = ""
   }
  }
  LaunchedEffect(shareError) { if (shareError != null) { shareSheetUrl = ""; page = "취향/공유" } }
  LaunchedEffect(repo, state.user?.uid) {
   val owner = "${real}:${state.user?.uid}"
   if (previousOwner != null && !previousOwner!!.endsWith(":null") && previousOwner != owner && shareSheetUrl.isBlank()) { link = ""; incoming = "" }
   previousOwner = owner
   password = ""; name = state.user?.name.orEmpty(); code = ""; message = ""
   tags = prefs.getStringSet("$owner:selected-tags", emptySet())!!.toSet()
   if (shareSheetUrl.isNotBlank()) shareTags = prefs.getStringSet("$owner:last-share-tags", emptySet()).orEmpty().toSet()
   val stored = runCatching { json.decodeFromString<List<Candidate>>(prefs.getString("$owner:catalog", "[]")!!) }.getOrDefault(emptyList())
   catalog.value = CandidateCatalog(state.user?.uid, ReelUrl.candidates.map { url -> stored.find { it.url == url } ?: Candidate(url) })
  }
  LaunchedEffect(state.user?.name) { if (name.isBlank()) name = state.user?.name.orEmpty() }
  Column(Modifier.fillMaxSize().systemBarsPadding().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text("CoupleReels", style = MaterialTheme.typography.headlineMedium)
   Text(if (real) "Firebase 연결 모드" else "로컬 Fake 모드 · 이 기기 안에서만 동작", color = MaterialTheme.colorScheme.primary)
   if (!real) Text("실제 비밀번호를 사용하지 마세요. 가입한 두 테스트 계정을 번갈아 로그인해 시연할 수 있습니다. 다른 폰과 동기화되지 않습니다.", style = MaterialTheme.typography.bodySmall)
   if (firebaseAvailable) TextButton(enabled = !busy, onClick = { real = !real; prefs.edit().putBoolean("useFirebase", real).apply() }) { Text(if (real) "로컬 테스트 모드로 전환" else "Firebase 모드로 전환") }
   else Text("Firebase 설정 미포함: mvp/google-services.json 추가 후 다시 빌드하세요.", style = MaterialTheme.typography.bodySmall)
   if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
   if (message.isNotEmpty()) Text(message)
   shareError?.let { Text(it) }
   state.error?.let { Text("연결 오류: $it") }
   TextButton(onClick = { play(null) }) { Text("P0-1 독립 재생 검증 열기") }
   if (state.user == null) {
    if (shareSheetUrl.isNotBlank()) {
     Text("릴스 링크를 받았어요 💗 로그인하거나 가입하면 내 취향으로 저장할 수 있어요.", color = MaterialTheme.colorScheme.primary)
     TextButton(onClick = { shareSheetUrl = "" }) { Text("공유 취소") }
    }
    OutlinedTextField(email, { email = it }, label = { Text("이메일") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("email"))
    OutlinedTextField(password, { password = it }, label = { Text("비밀번호") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("password"))
    OutlinedTextField(name, { name = it }, label = { Text("가입 이름") }, singleLine = true, modifier = Modifier.testTag("register-name"))
    Row {
     Button(enabled = !busy, onClick = { action { repo.login(email, password); "로그인 완료" } }) { Text("로그인") }
     TextButton(enabled = !busy, onClick = { action { repo.register(email, password, name); "가입 완료" } }) { Text("회원가입") }
    }
   } else {
    Text("${state.user!!.name} · ${state.user!!.email}")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("연결", "취향/공유", "피드").forEach { p -> TextButton(onClick = { page = p }) { Text(p) } } }
    Row { listOf("저장", "마이페이지").forEach { p -> TextButton(onClick = { page = p }) { Text(p) } } }
    when (page) {
     "연결" -> {
      Text(if (state.members.size == 2) "커플 연결 완료" else if (state.coupleId != null) "상대방 연결 대기" else "연결되지 않음")
      state.coupleId?.let { invite ->
       Text("연결 코드: $invite")
       TextButton(onClick = {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("CoupleReels 연결 코드", invite))
        message = "연결 코드를 복사했습니다. 상대방에게 직접 전달하세요."
       }) { Text("연결 코드 복사") }
      }
      if (state.coupleId == null) {
       Button(enabled = !busy, onClick = { action { "연결 코드: ${repo.createCode()}" } }) { Text("새 연결 코드 발급") }
       OutlinedTextField(code, { code = it }, label = { Text("상대방의 32자리 코드") }, modifier = Modifier.fillMaxWidth().testTag("couple-code"))
       Button(enabled = !busy, onClick = { action { repo.join(code); "커플 연결 완료" } }) { Text("코드로 연결") }
      }
      if (state.user!!.coupleId != null) TextButton(enabled = !busy, onClick = { action { repo.unlink(); "연결 해제 완료. 파트너 데이터 접근이 종료됩니다." } }) { Text("연결/대기 코드 해제") }
     }
     "취향/공유" -> {
      Text("취향 태그 선택")
      tagVocabulary.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { row.forEach { tag -> FilterChip(selected = tag in tags, onClick = { tags = if (tag in tags) tags - tag else tags + tag; prefs.edit().putStringSet("${real}:${state.user?.uid}:selected-tags", tags).apply() }, label = { Text(tag) }) } } }
      OutlinedTextField(link, { link = it }, label = { Text("공유 Reel URL (태그만 저장할 때 비움)") }, modifier = Modifier.fillMaxWidth().testTag("share-url"))
      Text("원본 릴스는 상대에게 공개하지 않아요.", style = MaterialTheme.typography.bodySmall)
      Button(enabled = !busy, onClick = { action { repo.publish(tags, link, true); "최신 취향과 공유 이벤트 저장 완료" } }) { Text("취향/공유 저장") }
      Text("내 최신 태그: ${state.myTaste.tags.joinToString()}")
     }
     "피드" -> {
      Text("서로의 취향을 반영한 릴스", style = MaterialTheme.typography.titleLarge)
      Text("실제 URL 10개 기반. 영상 내용은 자동 분석하지 않습니다. 후보 태그는 사용자가 직접 분류하며 이 기기에 저장됩니다.", style = MaterialTheme.typography.bodySmall)
      when (feed.status) {
       FeedStatus.LOGIN_REQUIRED -> Text("로그인이 필요합니다.")
       FeedStatus.PARTNER_REQUIRED -> Text("두 계정을 연결하면 상대방 취향을 반영합니다.")
       FeedStatus.CATALOG_LOADING -> Text("내 후보 분류를 불러오는 중입니다.")
       FeedStatus.TASTE_REQUIRED -> Text("상대방이 취향 태그를 저장하면 피드가 갱신됩니다.")
       FeedStatus.READY -> Text("추천 ${feed.reels.size}개 / 최대 10개 · 비공개 원본 제외 · 미분류 ${feed.unclassified}개")
      }
      feed.reels.forEachIndexed { index, ranked ->
       val c = ranked.candidate
       Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${index + 1}. ${c.url.substringAfter("/reel/").trimEnd('/')}")
        Text("태그: ${c.tags.joinToString().ifEmpty { "미분류" }} · 점수 %.3f".format(ranked.score))
        Row {
         TextButton(onClick = { play(c.url) }) { Text("앱 안에서 재생") }
         TextButton(enabled = !busy, onClick = { action { repo.saveRecommendation(c.url); "저장 완료" } }) { Text("저장") }
         TextButton(enabled = !busy, onClick = { action { repo.save(c.url, true); "관심 표시 완료" } }) { Text("관심") }
        }
        TextButton(onClick = {
         if (tags.isEmpty()) message = "취향/공유 화면에서 분류할 태그를 먼저 선택하세요."
         else { catalog.value = CandidateCatalog(state.user?.uid, candidates.map { if (it.url == c.url) it.copy(tags = tags) else it }); prefs.edit().putString("${real}:${state.user?.uid}:catalog", json.encodeToString(catalog.value.items)).apply(); message = "선택한 태그로 후보를 수동 분류했습니다." }
        }) { Text("선택 태그로 이 후보 수동 분류") }
       } }
      }
     }
     "저장" -> {
      if (state.saved.isEmpty()) Text("저장한 릴스가 없습니다.")
      state.saved.forEach { item ->
       Text(item.url)
       Row {
        TextButton(onClick = { play(item.url) }) { Text("재생") }
        TextButton(enabled = !busy, onClick = { action { repo.save(item.url, !item.liked); "관심 상태 변경" } }) { Text(if (item.liked) "관심 해제" else "관심") }
        TextButton(enabled = !busy, onClick = { action { repo.removeSaved(item.url); "저장 삭제" } }) { Text("삭제") }
       }
      }
     }
     "마이페이지" -> {
      OutlinedTextField(name, { name = it }, label = { Text("프로필 이름") })
      Button(enabled = !busy, onClick = { action { repo.rename(name); "프로필 저장 완료" } }) { Text("이름 저장") }
      TextButton(enabled = !busy, onClick = { action { repo.unlink(); "연결 해제 완료" } }) { Text("커플 연결 해제") }
      Button(enabled = !busy, onClick = { action { repo.logout(); "로그아웃 완료" } }) { Text("로그아웃") }
     }
    }
   }
  }
  if (shareSheetUrl.isNotBlank() && state.user != null) {
   ModalBottomSheet(onDismissRequest = { if (!busy) shareSheetUrl = "" }, containerColor = ivory) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     Text("♡ CoupleReels", color = blushPink, style = MaterialTheme.typography.titleMedium)
     Text("릴스 취향에 담기", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
     Text("이 릴스에서 어떤 점이 마음에 들었나요?", style = MaterialTheme.typography.bodyMedium)
     Card(colors = CardDefaults.cardColors(containerColor = softPink)) {
      Column(Modifier.fillMaxWidth().padding(14.dp)) {
       Text("Instagram Reel", fontWeight = FontWeight.SemiBold)
       Text(shareSheetUrl, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("share-sheet-url"))
      }
     }
     if (state.saved.any { it.url == shareSheetUrl }) Text("이미 담았던 릴스예요. 새로운 취향 태그를 추가할 수 있어요.", color = blushPink, style = MaterialTheme.typography.bodySmall)
     tagVocabulary.chunked(3).forEach { group ->
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
       group.forEach { tag ->
        FilterChip(selected = tag in shareTags, onClick = { shareTags = if (tag in shareTags) shareTags - tag else shareTags + tag },
         label = { Text(tagLabels[tag] ?: tag) })
       }
      }
     }
     Text("🔒 내가 공유한 원본 링크는 상대방에게 직접 공개되지 않아요.", style = MaterialTheme.typography.bodySmall)
     Text("태그를 하나 이상 선택하면 앞으로의 추천에 취향이 누적돼요.", style = MaterialTheme.typography.bodySmall)
     if (shareSaveError.isNotBlank()) Text(shareSaveError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("share-save-error"))
     Button(enabled = !busy && shareTags.isNotEmpty(), modifier = Modifier.fillMaxWidth().testTag("share-save"), onClick = {
      val sharedUrl = shareSheetUrl; val selected = shareTags; val owner = "${real}:${state.user?.uid}"
      if (!busy) scope.launch {
       busy = true; shareSaveError = ""
       try {
        withTimeout(25000) { repo.publish(selected, sharedUrl, true) }
        prefs.edit().putStringSet("$owner:last-share-tags", selected).apply()
        message = "릴스를 내 취향에 반영했어요 💗"
        shareSheetUrl = ""
       } catch (e: TimeoutCancellationException) {
        shareSaveError = "저장 확인이 지연됐어요. 보관함에 저장됐는지 확인해 주세요."
       } catch (e: CancellationException) { throw e }
       catch (e: Exception) { shareSaveError = e.userMessage() }
       finally { busy = false }
      }
     }) { Text(if (busy) "저장하는 중…" else "내 취향에 반영하기") }
     TextButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = { shareSheetUrl = "" }) { Text("취소") }
     Spacer(Modifier.height(12.dp))
    }
   }
  }
 }
}
