package com.couplereels.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.couplereels.mvp.BuildConfig
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MvpActivity : ComponentActivity() {
 private var incoming by mutableStateOf("")
 private var shareError by mutableStateOf<String?>(null)
 private val json = Json { ignoreUnknownKeys = true }
 private val prefs by lazy { getSharedPreferences("couplereels-demo-v1", MODE_PRIVATE) }
 private val backend by lazy {
  val data = runCatching { json.decodeFromString<FakeSnapshot>(prefs.getString("db", "{}")!!) }.getOrDefault(FakeSnapshot())
  FakeBackend(data) { check(prefs.edit().putString("db", json.encodeToString(it)).commit()) { "기기 저장 공간을 확인하세요." } }
 }
 private val firebaseAvailable by lazy { FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null }
 private val firestore by lazy { FirebaseFirestore.getInstance().apply { firestoreSettings = FirebaseFirestoreSettings.Builder().setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build() } }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
  if (savedInstanceState == null) receive(intent) else { incoming = savedInstanceState.getString("pendingReel").orEmpty(); shareError = savedInstanceState.getString("shareError") }
  setContent { CoupleTheme { App() } }
 }
 override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingReel", incoming); outState.putString("shareError", shareError); super.onSaveInstanceState(outState) }
 override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
 private fun receive(i: Intent) {
  if (i.action != Intent.ACTION_SEND) return
  val url = ReelIntents.sharedUrl(i)
  if (url == null) { incoming = ""; shareError = "릴스 링크를 받지 못했어요. 이미지 내용은 분석하지 않아요. Instagram에서 링크를 복사해 직접 입력해 주세요." }
  else { shareError = null; incoming = url }
 }
 private fun external(url: String, error: (String) -> Unit) {
  val canonical = ReelUrl.normalize(url)
  if (canonical == null) { error("올바른 개별 Instagram Reel 링크가 아니에요."); return }
  runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(canonical))) }
   .onFailure { error("Instagram 링크를 열 앱이 없어요. 링크를 복사해서 브라우저에 붙여넣어 주세요.") }
 }

 @OptIn(ExperimentalMaterial3Api::class)
 @Composable private fun App() {
  var real by remember { mutableStateOf(firebaseAvailable && prefs.getBoolean("useFirebase", true)) }
  val repo: CoupleRepository = remember(real) { if (real) FirebaseRepository(FirebaseAuth.getInstance(), firestore) { uid ->
   runCatching { json.decodeFromString<List<Candidate>>(prefs.getString("true:$uid:catalog", "[]")!!) }.getOrDefault(emptyList()).ifEmpty { ReelUrl.candidates.map(::Candidate) }
  } else FakeRepository(backend, prefs.getString("session", null), catalogLoader = { uid ->
   runCatching { json.decodeFromString<List<Candidate>>(prefs.getString("false:$uid:catalog", "[]")!!) }.getOrDefault(emptyList()).ifEmpty { ReelUrl.candidates.map(::Candidate) }
  }) { check(prefs.edit().putString("session", it).commit()) { "로그인 상태를 저장하지 못했어요." } } }
  DisposableEffect(repo) { onDispose { repo.close() } }
  val state by repo.state.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  var busy by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("") }
  var page by rememberSaveable { mutableStateOf("우리") }
  var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
  var code by remember { mutableStateOf("") }; var link by rememberSaveable { mutableStateOf("") }
  var sheet by rememberSaveable { mutableStateOf("") }; var shareTags by rememberSaveable { mutableStateOf(emptySet<String>()) }; var shareSaveError by remember { mutableStateOf("") }
  var tags by rememberSaveable { mutableStateOf(emptySet<String>()) }; var step by rememberSaveable { mutableIntStateOf(0) }
  var settings by remember { mutableStateOf(false) }; var diagnostics by remember { mutableStateOf(false) }; var unlinkConfirm by remember { mutableStateOf(false) }
  var filter by remember { mutableStateOf("all") }
  var previousUid by rememberSaveable { mutableStateOf<String?>(null) }
  var initializedOwner by rememberSaveable { mutableStateOf("") }
  var classifyUrl by remember { mutableStateOf<String?>(null) }; var classifyTags by remember { mutableStateOf(emptySet<String>()) }
  val owner = "${real}:${state.user?.uid}"
  val contentScroll = rememberScrollState()
  LaunchedEffect(page, step, settings, state.user?.uid) { contentScroll.scrollTo(0) }
  val catalog = remember { MutableStateFlow(CandidateCatalog()) }
  val feedScope = remember(repo) { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
  DisposableEffect(feedScope) { onDispose { feedScope.cancel() } }
  val feed by remember(repo) { PartnerFeed.observe(repo, catalog, feedScope) }.collectAsStateWithLifecycle()
  fun action(success: (() -> Unit)? = null, block: suspend () -> String) { if (!busy) scope.launch { busy = true; message = ""; try { message = withTimeout(25000) { block() }; success?.invoke() } catch (e: TimeoutCancellationException) { message = "저장 응답이 지연됐어요. 현재 상태를 확인하고 다시 시도해 주세요." } catch (e: CancellationException) { throw e } catch (e: Exception) { message = e.userMessage() } finally { busy = false } } }
  fun dismissShare() { sheet = ""; incoming = ""; link = ""; shareTags = emptySet(); shareSaveError = "" }
  fun copyLink(value: String) { getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(android.content.ClipData.newPlainText("CoupleReels", value)); message = "복사했어요." }
  LaunchedEffect(incoming) { if (incoming.isNotBlank() && incoming != sheet) { if (state.user == null) previousUid = null; sheet = incoming; link = incoming; shareTags = emptySet(); shareSaveError = ""; page = "취향" } }
  LaunchedEffect(shareError) { if (shareError != null) { dismissShare(); page = "취향" } }
  LaunchedEffect(real, state.user?.uid) {
   val uid = state.user?.uid
   if (uid != null && previousUid != null && previousUid != uid) { dismissShare(); link = "" }
   if (uid != null) previousUid = uid
   password = ""; email = ""; code = ""; name = state.user?.name.orEmpty(); message = ""; settings = false; filter = "all"
   if (initializedOwner != owner) {
   tags = state.myTaste.tags
   step = if (uid != null && !prefs.getBoolean("$owner:onboarded", false) && sheet.isBlank()) 1 else 0
   initializedOwner = owner
   }
   classifyUrl = null
   val stored = runCatching { json.decodeFromString<List<Candidate>>(prefs.getString("$owner:catalog", "[]")!!) }.getOrDefault(emptyList())
   catalog.value = CandidateCatalog(uid, ReelUrl.candidates.map { u -> stored.find { it.url == u } ?: Candidate(u) })
  }
  LaunchedEffect(state.myTaste) { if (step == 0) tags = state.myTaste.tags }
  Scaffold(containerColor = burgundyBackground, bottomBar = {
   if (state.user != null && step == 0) NavigationBar(containerColor = burgundyBackground) {
    listOf("발견" to "⌂", "취향" to "♡", "보관함" to "▱", "우리" to "♧").forEach { (tab, icon) -> NavigationBarItem(modifier = Modifier.testTag("tab-$tab"), selected = page == tab, onClick = { page = tab; settings = false }, icon = { Text(icon, style = MaterialTheme.typography.headlineSmall) }, label = { Text(tab) }, colors = NavigationBarItemDefaults.colors(indicatorColor = burgundySurface)) }
   }
  }) { insets ->
   Column(Modifier.fillMaxSize().padding(insets).imePadding().background(Brush.verticalGradient(listOf(burgundyBackground, burgundySurface.copy(alpha = .3f))))) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
     Brand()
     if (!real) Text("이 기기에만 저장 · 기기 간 동기화 없음", style = MaterialTheme.typography.labelMedium, color = muted)
     if (state.user == null) shareError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
     if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
     if (message.isNotBlank()) Text(message, Modifier.testTag("notice"), style = MaterialTheme.typography.bodySmall)
     state.error?.let { Text("연결을 확인해 주세요: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
    if (state.user != null && step == 0 && page == "발견" && !settings) {
     if (feed.status == FeedStatus.READY && feed.reels.isNotEmpty()) {
      val pager = rememberPagerState { feed.reels.size }
      VerticalPager(state = pager, key = { feed.reels[it].candidate.url }, modifier = Modifier.weight(1f).testTag("discover-pager")) { index ->
       val candidate = feed.reels[index].candidate
       Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("연인을 위한 발견 · ${index + 1}/${feed.reels.size}", fontWeight = FontWeight.Bold)
        ReelPreview(candidate.url, Modifier.weight(1f), onExternal = { external(candidate.url) { message = it } }, onCopy = { copyLink(candidate.url) })
        Text(if (candidate.tags.isEmpty()) "아직 분류되지 않은 후보" else candidate.tags.joinToString(" · ") { labels[it] ?: it }, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { classifyUrl = candidate.url; classifyTags = candidate.tags }) { Text("후보 태그 편집") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
         Button(colors = ButtonDefaults.buttonColors(containerColor = goldAccent, contentColor = burgundyBackground), enabled = !busy, onClick = { action { repo.saveRecommendation(candidate.url); "보관함에 저장했어요." } }, modifier = Modifier.weight(1f)) { Text(if (state.saved.any { it.url == candidate.url }) "저장됨" else "저장") }
         OutlinedButton(enabled = !busy, onClick = { action { repo.save(candidate.url, state.saved.none { it.url == candidate.url && it.liked }); "관심 상태를 변경했어요." } }) { Text(if (state.saved.any { it.url == candidate.url && it.liked }) "♥ 관심 해제" else "♡ 관심") }
        }
        Text("등록된 후보와 수동 태그 기반 추천이에요. Instagram 전체 검색이 아니에요.", style = MaterialTheme.typography.labelSmall, color = muted)
       }
      }
     } else Column(Modifier.weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
      Heading("발견", "서로의 취향에서 시작하는 작은 발견")
      SoftCard { Text("♡", style = MaterialTheme.typography.displayMedium); Text(when (feed.status) { FeedStatus.PARTNER_REQUIRED -> "연결하면 상대방의 취향을 반영해요"; FeedStatus.TASTE_REQUIRED -> "상대방의 추천 준비를 기다리고 있어요"; FeedStatus.CATALOG_LOADING -> "내 후보를 불러오고 있어요"; else -> "추천할 후보가 아직 없어요" }, style = MaterialTheme.typography.titleLarge); Text("준비된 실제 링크만 보여드려요. 없는 영상을 채우지 않아요."); Primary("우리로 이동") { page = "우리" } }
     }
    } else Column(Modifier.weight(1f).verticalScroll(contentScroll).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
     if (state.user == null) {
      Heading("취향이 만나는 곳", "좋아하는 릴스를 담고, 서로를 더 알아가요")
      if (sheet.isNotBlank()) { Text("릴스 링크를 받았어요. 로그인하면 이어서 저장할 수 있어요."); TextButton(onClick = { dismissShare() }) { Text("공유 취소") } }
      if (!real) Text("로컬 체험 계정이에요. 실제 비밀번호를 사용하지 마세요.", style = MaterialTheme.typography.bodySmall)
      SoftCard {
       OutlinedTextField(email, { email = it }, label = { Text("이메일") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("email"))
       OutlinedTextField(password, { password = it }, label = { Text("비밀번호") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("password"))
       OutlinedTextField(name, { name = it }, label = { Text("가입 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("register-name"))
       Primary("로그인", !busy) { action { repo.login(email, password); "로그인했어요." } }
       TextButton(enabled = !busy, onClick = { action { repo.register(email, password, name); "가입했어요." } }) { Text("회원가입") }
      }
     } else if (step > 0) {
      Text("취향 알아보기 $step/2", color = muted)
      LinearProgressIndicator(progress = { step / 2f }, modifier = Modifier.fillMaxWidth(), color = goldAccent, trackColor = burgundySurface)
      Heading(if (step == 1) "어떤 걸 좋아하세요?" else "어떤 스타일이 좋아요?", if (step == 1) "관심 있는 주제를 골라주세요" else "세부 취향과 받고 싶은 선물을 알려주세요")
      if (step == 1) { ChoiceGrid(interests, tags) { tags = tags.toggle(it) } }
      else { Text("선호 스타일", fontWeight = FontWeight.Bold); ChoiceGrid(styles, tags) { tags = tags.toggle(it) }; Text("선물로 받고 싶은 것 (선택)", fontWeight = FontWeight.Bold); Tags(gifts, tags) { tags = tags.toggle(it) } }
      PrivacyNote()
      Primary(if (step == 1) "다음" else "취향 저장하고 시작하기", !busy && tags.isNotEmpty(), Modifier.testTag("onboarding-next")) {
       if (step == 1) step = 2 else action(success = { prefs.edit().putBoolean("$owner:onboarded", true).apply(); step = 0; page = "취향" }) { repo.publish(tags, null, true); "취향을 저장했어요." }
      }
      TextButton(enabled = !busy, onClick = { prefs.edit().putBoolean("$owner:onboarded", true).apply(); step = 0; page = "취향" }) { Text("나중에 할게요") }
      if (step == 2) TextButton(onClick = { step = 1 }) { Text("이전") }
     } else if (settings) {
      Heading("계정 설정", state.user!!.email)
      OutlinedTextField(name, { name = it }, label = { Text("프로필 이름") }, modifier = Modifier.fillMaxWidth())
      Primary("이름 저장", !busy) { action { repo.rename(name); "프로필을 저장했어요." } }
      Primary("로그아웃", !busy) { action(success = { dismissShare(); link = ""; previousUid = null }) { repo.logout(); "로그아웃했어요." } }
      if (BuildConfig.DEBUG) TextButton(onClick = { diagnostics = true }) { Text("개발 진단") }
      TextButton(onClick = { settings = false }) { Text("돌아가기") }
     } else when (page) {
      "취향" -> {
       Heading("내 취향", "좋아하는 것을 조금씩 알려주세요")
       SoftCard { Text("♡ 내가 좋아하는 것", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(state.myTaste.tags.joinToString(" · ") { labels[it] ?: it }.ifEmpty { "아직 선택한 취향이 없어요" }); TextButton(onClick = { tags = state.myTaste.tags; step = 1 }) { Text("취향 수정") } }
       SoftCard { Text("♧ 선물로 받고 싶은 것", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text((state.myTaste.tags intersect gifts.toSet()).joinToString(" · ") { labels[it] ?: it }.ifEmpty { "취향 수정에서 선물을 골라주세요" }) }
       Text("내가 공유한 릴스", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
       if (state.saved.none { it.tags.isNotEmpty() }) Text("Instagram에서 좋아하는 릴스를 공유해 보세요.")
       state.saved.filter { it.tags.isNotEmpty() }.forEach { item -> SavedCard(item, !busy, onOpen = { link = item.url; sheet = item.url; incoming = item.url; shareTags = item.tags }, onLike = { action { repo.save(item.url, !item.liked); "관심 상태 변경" } }, onRemove = { action { repo.removeSaved(item.url); "보관함에서 삭제했어요." } }) }
       PrivacyNote()
       shareError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
       OutlinedTextField(link, { link = it; shareError = null }, label = { Text("Instagram Reel 링크 직접 입력") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("share-url"))
       Primary("릴스 공유하기", !busy) { val valid = ReelUrl.normalize(link); if (valid == null) message = "올바른 개별 Instagram Reel 링크를 입력해 주세요." else { sheet = valid; incoming = valid; shareTags = emptySet(); shareSaveError = "" } }
      }
      "보관함" -> {
       Heading("보관함", "마음에 남은 릴스를 모아봤어요")
       Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("all" to "전체", "jewelry" to "주얼리", "date" to "데이트").forEach { (key, label) -> FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) }) } }
       val saved = state.saved.filter { filter == "all" || filter in it.tags }
       if (saved.isEmpty()) SoftCard { Text("아직 이곳에 담긴 릴스가 없어요"); Text("공유하거나 발견한 링크를 저장해 보세요.") }
       saved.forEach { item -> SavedCard(item, !busy, onOpen = { external(item.url) { message = it } }, onLike = { action { repo.save(item.url, !item.liked); "관심 상태를 변경했어요." } }, onRemove = { action { repo.removeSaved(item.url); "보관함에서 삭제했어요." } }) }
       Text("저장한 릴스와 원본 링크는 나만 볼 수 있어요.", color = muted, style = MaterialTheme.typography.bodySmall)
      }
      else -> {
       Heading("우리", "우리만의 취향 공간")
       SoftCard {
        Text("◯  ♡  ◯", Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.displaySmall)
        Text(if (state.members.size == 2) "커플 연결 완료" else if (state.coupleId != null) "상대방 연결 대기" else "아직 연결된 연인이 없어요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (real) "연결하면 취향 태그 공개 없이 계산된 후보 링크를 서로 전달해요." else "로컬 연결은 이 기기의 체험 계정끼리만 가능해요.")
        TextButton(onClick = { page = "취향" }) { Text("나중에 연결할게요") }
       }
       SoftCard {
        if (state.coupleId == null) {
         Primary("새 연결 코드 발급", !busy) { action { "연결 코드: ${repo.createCode()}" } }
         OutlinedTextField(code, { code = it }, label = { Text("상대방의 32자리 코드") }, modifier = Modifier.fillMaxWidth().testTag("couple-code"))
         Primary("코드로 연결", !busy && code.isNotBlank()) { action { repo.join(code); "커플 연결 완료" } }
        } else {
         Text("연결 코드", fontWeight = FontWeight.Bold); Text(state.coupleId!!)
         TextButton(onClick = { copyLink(state.coupleId!!) }) { Text("연결 코드 복사") }
         TextButton(enabled = !busy, onClick = { unlinkConfirm = true }) { Text("커플 연결 해제") }
        }
       }
       PrivacyNote()
       SoftCard { MenuRow("⚙", "계정 설정") { name = state.user!!.name; settings = true } }
      }
     }
     Spacer(Modifier.height(12.dp))
    }
   }
  }
  if (sheet.isNotBlank() && state.user != null && (previousUid == null || previousUid == state.user?.uid)) ModalBottomSheet(onDismissRequest = { if (!busy) dismissShare() }, containerColor = burgundyBackground, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
   DarkSheetSystemBars()
   Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Brand(); Heading("릴스 취향에 담기", "이 릴스에서 어떤 점이 좋았나요?")
    SoftCard { Text("Instagram Reel", fontWeight = FontWeight.Bold); Text(sheet, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("share-sheet-url")) }
    if (state.saved.any { it.url == sheet }) Text("이미 담았던 릴스예요. 새로운 취향 태그를 추가할 수 있어요.")
    Tags(tagVocabulary, shareTags, !busy, testTagPrefix = "share") { shareTags = shareTags.toggle(it) }
    PrivacyNote()
    if (shareSaveError.isNotBlank()) Text(shareSaveError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("share-save-error"))
    Primary(if (busy) "저장하는 중…" else "내 취향에 반영하기", !busy && shareTags.isNotEmpty(), Modifier.testTag("share-save")) {
     val url = sheet; val selected = shareTags
     scope.launch { busy = true; shareSaveError = ""; try { withTimeout(25000) { repo.publish(selected, url, true) }; message = "릴스를 내 취향에 반영했어요 💗"; prefs.edit().putBoolean("$owner:onboarded", true).apply(); step = 0; if (sheet == url) dismissShare() } catch (e: TimeoutCancellationException) { shareSaveError = "저장 확인이 지연됐어요. 보관함에서 확인한 후 재시도해 주세요." } catch (e: CancellationException) { throw e } catch (e: Exception) { shareSaveError = e.userMessage() } finally { busy = false } }
    }
    TextButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = { dismissShare() }) { Text("취소") }
   }
  }
  classifyUrl?.let { target -> AlertDialog(onDismissRequest = { classifyUrl = null }, title = { Text("후보 태그 편집") }, text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text("직접 확인한 후보를 분류하면 내 취향에 맞춰 연인에게 보낼 추천이 갱신돼요."); Text(target, style = MaterialTheme.typography.bodySmall); Tags(tagVocabulary, classifyTags) { classifyTags = classifyTags.toggle(it) } } }, confirmButton = { TextButton(onClick = { val updated = catalog.value.items.map { if (it.url == target) it.copy(tags = classifyTags) else it }
    action(success = { catalog.value = CandidateCatalog(state.user?.uid, updated); prefs.edit().putString("$owner:catalog", json.encodeToString(updated)).apply(); classifyUrl = null }) { repo.updateCandidateCatalog(updated); "내 후보 분류와 연인에게 보낼 추천을 저장했어요." } }) { Text("분류 저장") } }, dismissButton = { TextButton(onClick = { classifyUrl = null }) { Text("취소") } }) }
  if (unlinkConfirm) AlertDialog(onDismissRequest = { unlinkConfirm = false }, title = { Text("커플 연결을 해제할까요?") }, text = { Text("서로의 추천 정보 접근이 종료돼요. 내 취향과 보관함은 유지돼요.") }, confirmButton = { TextButton(enabled = !busy, onClick = { unlinkConfirm = false; action { repo.unlink(); "연결을 해제했어요." } }) { Text("연결 해제") } }, dismissButton = { TextButton(onClick = { unlinkConfirm = false }) { Text("취소") } })
  if (diagnostics && BuildConfig.DEBUG) AlertDialog(onDismissRequest = { diagnostics = false }, title = { Text("개발 진단") }, text = { Column { Text(if (real) "Firebase 선택됨 · 실제 동기화 검증 별도 필요" else "로컬 체험 · 서버 동기화 없음"); if (firebaseAvailable) TextButton(enabled = !busy, onClick = { diagnostics = false; dismissShare(); real = !real; prefs.edit().putBoolean("useFirebase", real).apply() }) { Text("저장 모드 전환") }; TextButton(onClick = { startActivity(ReelIntents.playback(this@MvpActivity, null)) }) { Text("P0-1 독립 재생 검증 열기") } } }, confirmButton = { TextButton(onClick = { diagnostics = false }) { Text("닫기") } })
 }
}
private fun Set<String>.toggle(tag: String) = if (tag in this) this - tag else this + tag
@Composable private fun ChoiceGrid(values: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
 values.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { tag -> OutlinedButton(onClick = { onToggle(tag) }, modifier = Modifier.weight(1f).heightIn(min = 86.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (tag in selected) burgundySurface else burgundyBackground, contentColor = ivoryText)) { Text((if (tag in selected) "✓  " else "") + labels.getValue(tag), fontWeight = FontWeight.SemiBold) } } } }
}
@Composable private fun SavedCard(item: SavedReel, enabled: Boolean, onOpen: () -> Unit, onLike: () -> Unit, onRemove: () -> Unit) {
 SoftCard {
  Text("Instagram Reel", fontWeight = FontWeight.Bold); Text(item.url, style = MaterialTheme.typography.bodySmall)
  if (item.tags.isNotEmpty()) Text(item.tags.joinToString(" · ") { labels[it] ?: it }, color = muted, style = MaterialTheme.typography.bodySmall)
  TextButton(onClick = onOpen) { Text("링크 열기") }
  Row { TextButton(enabled = enabled, onClick = onLike) { Text(if (item.liked) "♥ 관심 해제" else "♡ 관심") }; TextButton(enabled = enabled, onClick = onRemove) { Text("삭제") } }
 }
}
