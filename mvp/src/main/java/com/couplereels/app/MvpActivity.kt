package com.couplereels.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.*
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
 private val demoStore by lazy { DemoStore(this) }
 private val demoUiStore by lazy { DemoUiStore(this) }
 private val firebaseAvailable by lazy { FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null }
 private val firestore by lazy { FirebaseFirestore.getInstance().apply { firestoreSettings = FirebaseFirestoreSettings.Builder().setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build() } }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
  if (savedInstanceState == null) receive(intent) else { incoming = savedInstanceState.getString("pendingReel").orEmpty(); shareError = savedInstanceState.getString("shareError") }
  setContent { CoupleTheme { App() } }
 }
 override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingReel", incoming); outState.putString("shareError", shareError); super.onSaveInstanceState(outState) }
 override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
 private fun receive(i: Intent) {
  if (i.action != Intent.ACTION_SEND) return
  val url = ReelIntents.sharedUrl(i)
  if (url == null) { incoming = ""; shareError = "릴스 링크를 받지 못했어요. Instagram에서 개별 릴스 링크를 복사해 주세요." }
  else { shareError = null; incoming = url }
 }
 private fun external(url: String, error: (String) -> Unit) {
  val canonical = ReelUrl.normalize(url)
  if (canonical == null) { error("올바른 개별 Instagram Reel 링크가 아니에요."); return }
  runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(canonical))) }
   .onFailure { error("Instagram 링크를 열 앱이 없어요. 링크를 복사해서 브라우저에 붙여넣어 주세요.") }
 }

 @OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
 @Composable private fun App() {
  var demoMode by remember { mutableStateOf(demoStore.active) }
  val real = !demoMode && firebaseAvailable
  val repo: CoupleRepository = remember(demoMode, real) {
   if (demoMode) DemoRepository(demoStore.load(), demoStore::save)
   else if (real) FirebaseRepository(FirebaseAuth.getInstance(), firestore)
   else UnavailableRepository()
  }
  DisposableEffect(repo) { onDispose { repo.close() } }
  val state by repo.state.collectAsStateWithLifecycle()
  val demoUi by demoUiStore.state.collectAsStateWithLifecycle()
  var infoDemo by remember { mutableStateOf<DemoContent?>(null) }
  var noticeDialog by remember { mutableStateOf<String?>(null) }
  var removeUrl by remember { mutableStateOf<String?>(null) }
  var showingUrlInput by rememberSaveable { mutableStateOf(false) }
  var linkInputError by remember { mutableStateOf("") }
  var moreTags by rememberSaveable { mutableStateOf(false) }
  val active = state.isDemo || state.user != null
  val currentUid = if (state.isDemo) "guest-p06" else state.user?.uid
  val scope = rememberCoroutineScope()
  var busy by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("") }
  var page by rememberSaveable { mutableStateOf("발견") }
  var registering by rememberSaveable { mutableStateOf(false) }
  var previewUrl by rememberSaveable { mutableStateOf<String?>(null) }
  var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
  var code by remember { mutableStateOf("") }; var link by rememberSaveable { mutableStateOf("") }
  var sheet by rememberSaveable { mutableStateOf("") }; var shareTags by rememberSaveable { mutableStateOf(emptySet<String>()) }; var shareSaveError by remember { mutableStateOf("") }
  var tags by rememberSaveable { mutableStateOf(emptySet<String>()) }; var step by rememberSaveable { mutableIntStateOf(0) }
  var settings by remember { mutableStateOf(false) }; var diagnostics by remember { mutableStateOf(false) }; var unlinkConfirm by remember { mutableStateOf(false) }
  var filter by remember { mutableStateOf("all") }
  var previousUid by rememberSaveable { mutableStateOf<String?>(null) }
  var initializedOwner by rememberSaveable { mutableStateOf("") }
  var classifyUrl by remember { mutableStateOf<String?>(null) }; var classifyTags by remember { mutableStateOf(emptySet<String>()) }
  val owner = if (state.isDemo) "guest-p06" else "${real}:${state.user?.uid}"
  val contentScroll = rememberScrollState()
  LaunchedEffect(page, step, settings, previewUrl, currentUid) { contentScroll.scrollTo(0) }
  val catalog = remember { MutableStateFlow(CandidateCatalog()) }
  val feedScope = remember(repo) { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
  DisposableEffect(feedScope) { onDispose { feedScope.cancel() } }
  val feed by remember(repo) { PartnerFeed.observe(repo, catalog, feedScope) }.collectAsStateWithLifecycle()
  fun action(success: (() -> Unit)? = null, block: suspend () -> String) { if (!busy) scope.launch { busy = true; message = ""; try { message = withTimeout(25000) { block() }; success?.invoke() } catch (e: TimeoutCancellationException) { message = "저장 응답이 지연됐어요. 현재 상태를 확인하고 다시 시도해 주세요." } catch (e: CancellationException) { throw e } catch (e: Exception) { message = e.userMessage() } finally { busy = false } } }
  fun dismissShare() { sheet = ""; incoming = ""; link = ""; shareTags = emptySet(); shareSaveError = "" }
  fun copyLink(value: String) { getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(android.content.ClipData.newPlainText("CoupleReels", value)); message = "복사했어요." }
  LaunchedEffect(incoming) { if (incoming.isNotBlank() && incoming != sheet) { if (!active) previousUid = null; previewUrl = null; settings = false; sheet = incoming; link = incoming; shareTags = emptySet(); shareSaveError = ""; page = "취향" } }
  LaunchedEffect(shareError) { if (shareError != null) { dismissShare(); previewUrl = null; settings = false; page = "취향" } }
  LaunchedEffect(demoMode, currentUid) {
   val uid = currentUid
   if (uid != null && previousUid != null && previousUid != uid) { dismissShare(); link = "" }
   if (uid != null) previousUid = uid
   password = ""; email = ""; code = ""; name = state.user?.name.orEmpty(); message = ""; settings = false; filter = "all"
   if (initializedOwner != owner) {
   tags = state.myTaste.tags
   step = if (active && !(if (state.isDemo) demoStore.load().onboarded else prefs.getBoolean("$owner:onboarded", false))) 1 else 0
   initializedOwner = owner
   }
   classifyUrl = null
   val stored = runCatching { json.decodeFromString<List<Candidate>>(prefs.getString("$owner:catalog", "[]")!!) }.getOrDefault(emptyList())
   catalog.value = CandidateCatalog(uid, ReelUrl.candidates.map { u -> stored.find { it.url == u } ?: Candidate(u) })
  }
  LaunchedEffect(state.myTaste) { if (step == 0) tags = state.myTaste.tags }
  val immersive = active && step == 0 && page == "발견" && !settings && previewUrl == null
  SideEffect { androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !immersive }
  Scaffold(containerColor = cream, contentWindowInsets = WindowInsets(0,0,0,0), bottomBar = {
   if (active && step == 0) CoupleNavigation(page) { tab -> page = tab; settings = false; previewUrl = null; message = ""; shareError = null; showingUrlInput = false }
  }) { insets ->
   Column(Modifier.fillMaxSize().padding(insets).then(if (!immersive) Modifier.statusBarsPadding() else Modifier).then(if (!active || step > 0) Modifier.navigationBarsPadding() else Modifier).imePadding().background(Brush.verticalGradient(listOf(cream, Color.White, cream)))) {
    if (!immersive) Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
     Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (step > 0) Arrangement.Center else Arrangement.Start) {
      Brand(Modifier.combinedClickable(onClick = {}, onLongClick = { if (BuildConfig.DEBUG) diagnostics = true }), compact = active && step == 0)
      if (active && step == 0) {
       Spacer(Modifier.weight(1f))
       IconButton(onClick = { noticeDialog = if (state.isDemo) "체험모드에서는 서버 알림을 받지 않아요." else "새로운 알림이 없어요." }, modifier = Modifier.semantics { contentDescription = "알림" }) { Glyph("bell", Modifier.size(23.dp)) }
       IconButton(onClick = { settings = true; previewUrl = null }, modifier = Modifier.semantics { contentDescription = "프로필 및 설정" }) { Box(Modifier.size(32.dp).background(baby, CircleShape), contentAlignment = Alignment.Center) { Glyph("person", Modifier.size(20.dp), muted) } }
      }
     }
     if (state.isDemo) Text("체험 모드 · 기기에만 저장", fontSize = 11.sp, color = muted, modifier = if (step > 0) Modifier.align(Alignment.CenterHorizontally) else Modifier)
     shareError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("share-input-error")) }
     if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
     if (message.isNotBlank()) Text(message, Modifier.testTag("notice"), style = MaterialTheme.typography.bodySmall)
     state.error?.let { Text("연결을 확인해 주세요: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
    if (active && step == 0 && page == "발견" && !settings && previewUrl == null) {
     if (state.isDemo) DemoDiscovery(state.myTaste.tags, demoUi, Modifier.weight(1f),
      onLike = { id -> action { demoUiStore.like(id); "" } },
      onBookmark = { id -> action { demoUiStore.bookmark(id); "" } },
      onHide = { id -> action { demoUiStore.hide(id); "" } },
      onRestore = { action { demoUiStore.restoreHidden(); "" } }, onInfo = { infoDemo = it })
     else if (feed.status == FeedStatus.READY && feed.reels.isNotEmpty()) {
      val pager = rememberPagerState { feed.reels.size }
      VerticalPager(state = pager, key = { feed.reels[it].candidate.url }, modifier = Modifier.weight(1f).testTag("discover-pager")) { index ->
       val candidate = feed.reels[index].candidate
       Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("연인을 위한 발견 · ${index + 1}/${feed.reels.size}", fontWeight = FontWeight.Bold)
        ReelPreview(candidate.url, Modifier.weight(1f), onExternal = { external(candidate.url) { message = it } }, onCopy = { copyLink(candidate.url) })
        Text(if (candidate.tags.isEmpty()) "아직 분류되지 않은 후보" else candidate.tags.joinToString(" · ") { labels[it] ?: it }, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { classifyUrl = candidate.url; classifyTags = candidate.tags }) { Text("후보 태그 편집") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
         Button(colors = ButtonDefaults.buttonColors(containerColor = pink, contentColor = ink), enabled = !busy, onClick = { action { repo.saveRecommendation(candidate.url); "보관함에 저장했어요." } }, modifier = Modifier.weight(1f)) { Text(if (state.saved.any { it.url == candidate.url }) "저장됨" else "저장") }
         OutlinedButton(enabled = !busy, onClick = { action { repo.save(candidate.url, state.saved.none { it.url == candidate.url && it.liked }); "관심 상태를 변경했어요." } }) { Text(if (state.saved.any { it.url == candidate.url && it.liked }) "♥ 관심 해제" else "♡ 관심") }
        }
        Text("등록된 후보와 수동 태그 기반 추천이에요. Instagram 전체 검색이 아니에요.", style = MaterialTheme.typography.labelSmall, color = muted)
       }
      }
     } else Column(Modifier.weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
      Heading("발견", "서로의 취향에서 시작하는 작은 발견")
      SoftCard { Text("♡", style = MaterialTheme.typography.displayMedium); Text(when (feed.status) { FeedStatus.PARTNER_REQUIRED -> "연결하면 상대방의 취향을 반영해요"; FeedStatus.TASTE_REQUIRED -> "상대방의 취향을 기다리고 있어요"; FeedStatus.CATALOG_LOADING -> "내 후보를 불러오고 있어요"; else -> "추천할 후보가 아직 없어요" }, style = MaterialTheme.typography.titleLarge); Text("준비된 실제 링크만 보여드려요. 없는 영상을 채우지 않아요."); Primary("우리로 이동") { page = "우리" } }
     }
    } else Column(Modifier.weight(1f).verticalScroll(contentScroll).padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     if (!active) {
      Heading(if (registering) "회원가입" else "취향이 만나는 곳", if (registering) "서로의 취향을 연결할 준비" else "좋아하는 릴스에서 시작하는 우리")
      if (sheet.isNotBlank()) { Text("받은 릴스는 체험을 시작한 뒤 저장할 수 있어요."); TextButton(onClick = { dismissShare() }) { Text("공유 취소") } }
      Primary("로그인 없이 체험하기", !busy, Modifier.testTag("enter-demo")) {
       action(success = { registering = false; previousUid = null; initializedOwner = ""; page = "발견"; demoMode = true }) {
        demoStore.load(); demoStore.setActive(true); ""
       }
      }
      Text("계정 없이 시작하고, 취향은 이 기기에 저장해요.", color = muted, style = MaterialTheme.typography.bodySmall)
      SoftCard {
       if (registering) OutlinedTextField(name, { name = it }, label = { Text("이름") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("register-name"))
       OutlinedTextField(email, { email = it }, label = { Text("이메일") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("email"))
       OutlinedTextField(password, { password = it }, label = { Text("비밀번호") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("password"))
       if (!real) Text("계정 서비스 준비 중이에요. 체험하기를 이용해 주세요.", color = muted, modifier = Modifier.testTag("auth-unavailable"))
       Primary(if (registering) "계정 만들기" else "로그인", real && !busy && email.isNotBlank() && password.length >= 6 && (!registering || name.isNotBlank()), Modifier.testTag("auth-submit")) {
        action { if (registering) repo.register(email, password, name) else repo.login(email, password); "" }
       }
       TextButton(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth-switch"), onClick = { registering = !registering; password = ""; message = "" }) { Text(if (registering) "로그인으로 돌아가기" else "회원가입") }
      }
     } else if (step > 0) {
      OnboardingContent(step, tags, busy, onToggle = { tags = tags.toggle(it) }, onNext = {
       if (step == 1) step = 2 else action(success = { if (!state.isDemo) prefs.edit().putBoolean("$owner:onboarded", true).apply(); step = 0; page = "발견" }) { repo.publish(tags, null, true); "" }
      }, onSkip = { action(success = { step = 0; page = "발견" }) { if (repo is DemoRepository) repo.finishOnboarding() else prefs.edit().putBoolean("$owner:onboarded", true).apply(); "" } }, onPrevious = { step = 1 })
     } else if (previewUrl != null) {
      TextButton(onClick = { previewUrl = null }) { Text("돌아가기") }
      ReelPreview(previewUrl!!, Modifier.fillMaxWidth().height(460.dp), onExternal = { external(previewUrl!!) { message = it } }, onCopy = { copyLink(previewUrl!!) })
      state.saved.find { it.url == previewUrl }?.let { item ->
       Tags(item.tags,item.tags) { link = item.url; sheet = item.url; incoming = item.url; shareTags = item.tags }
       OutlinedButton(onClick = { link = item.url; sheet = item.url; incoming = item.url; shareTags = item.tags }, modifier = Modifier.fillMaxWidth()) { Text("태그 수정") }
       Row { TextButton(onClick = { action { repo.save(item.url,!item.liked); "관심 상태를 변경했어요." } }) { Text(if(item.liked) "좋아요 취소" else "좋아요") }; TextButton(onClick = { removeUrl = item.url }) { Text("보관함에서 삭제") } }
      }
     } else if (settings && state.isDemo) {
      Heading("체험 설정", "취향과 보관함은 이 기기에 남아요")
      Text("실제 계정 데이터와 분리되어 있고 서버에 업로드하지 않아요.")
      Primary("체험 나가기", !busy) { action(success = { dismissShare(); previousUid = null; initializedOwner = ""; demoMode = false; settings = false; step = 0 }) { demoStore.setActive(false); "" } }
      MenuRow("down", "관심 없음으로 숨긴 데모 복원") { action { demoUiStore.restoreHidden(); "숨긴 데모를 복원했어요." } }
      TextButton(onClick = { settings = false }) { Text("돌아가기") }
     } else if (settings) {
      Heading("계정 설정", state.user!!.email)
      OutlinedTextField(name, { name = it }, label = { Text("프로필 이름") }, modifier = Modifier.fillMaxWidth())
      Primary("이름 저장", !busy) { action { repo.rename(name); "프로필을 저장했어요." } }
      Primary("로그아웃", !busy) { action(success = { dismissShare(); link = ""; previousUid = null }) { repo.logout(); "로그아웃했어요." } }
      TextButton(onClick = { settings = false }) { Text("돌아가기") }
     } else when (page) {
      "취향" -> {
       TasteContent(state, onEdit = { tags = state.myTaste.tags; step = 1 }, onGifts = { tags = state.myTaste.tags; step = 2 }, onOpen = { previewUrl = it.url }, onShare = { showingUrlInput = !showingUrlInput })
      }
      "보관함" -> {
       Heading("보관함", "마음에 남은 릴스를 모아봤어요")
       val demoSaved = if (state.isDemo) DemoRecommendations.contents.filter { it.id in demoUi.bookmarked } else emptyList()
       val availableTags = state.saved.flatMap { it.tags }.toSet() + demoSaved.flatMap { it.tags }
       val filters = listOf("all") + listOf("jewelry", "date", "perfume", "flowers", "fashion", "tech", "hobby").filter { matchesSavedFilter(availableTags,it) }
       Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { filters.forEach { key -> FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(if(key == "all") "전체" else labels.getValue(key)) }, shape = CircleShape, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = roseDeep, selectedLabelColor = Color.White), modifier = Modifier.heightIn(min=48.dp).testTag("filter-$key")) } }
       val saved = state.saved.filter { matchesSavedFilter(it.tags,filter) }
       val demos = demoSaved.filter { matchesSavedFilter(it.tags,filter) }
       if (saved.isEmpty() && demos.isEmpty()) EmptyContent("아직 이곳에 담긴 릴스가 없어요", "공유하거나 발견한 콘텐츠를 저장해 보세요.")
       ReelGrid(saved, onOpen = { previewUrl = it.url }, onRemove = { removeUrl = it.url })
       if (demos.isNotEmpty()) DemoSavedGrid(demos, onOpen = { infoDemo = it }, onRemove = { c -> action { demoUiStore.bookmark(c.id); "데모 보관을 취소했어요." } })
       Text("저장한 릴스는 나만 볼 수 있어요.", color = muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
      }
      else -> {
       if (state.isDemo) {
        val unavailable = { noticeDialog = "체험모드에서는 실제 커플 연결을 지원하지 않아요. 계정 서비스 연결 후 사용할 수 있어요." }
        CoupleUnlinked(true, unavailable, unavailable, unavailable, onLater = { page = "발견" }, onSettings = { settings = true })
       } else {
       Heading("우리", "우리만의 취향 공간")
       SoftCard {
        Glyph("people", Modifier.align(Alignment.CenterHorizontally).size(60.dp), pink)
        Text(if (state.members.size == 2) "커플 연결 완료" else if (state.coupleId != null) "상대방 연결 대기" else "아직 연결된 연인이 없어요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (real) "연결하면 서로의 취향 태그가 추천 피드에 반영돼요." else "로컬 연결은 이 기기의 체험 계정끼리만 가능해요.")
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
       if (!state.isDemo) PrivacyNote()
       SoftCard { MenuRow("⚙", "계정 설정") { name = state.user!!.name; settings = true } }
       }
      }
     }
     Spacer(Modifier.height(12.dp))
    }
   }
  }
  if (sheet.isNotBlank() && active && step == 0 && (previousUid == null || previousUid == currentUid)) ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { if (!busy) dismissShare() }, containerColor = cream) {
   Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .86f).dp).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Brand(); Heading("릴스 취향에 담기", "이 릴스에서 어떤 점이 좋았나요?")
    ShareLinkSummary(sheet) { external(sheet) { shareSaveError = it } }
    if (state.saved.any { it.url == sheet }) Text("이미 담았던 릴스예요. 새로운 취향 태그를 추가할 수 있어요.")
    Tags(if (moreTags) tagVocabulary else (listOf("silver", "necklace", "minimal", "bold", "gift") + shareTags).distinct(), shareTags, !busy) { shareTags = shareTags.toggle(it) }
    TextButton(onClick = { moreTags = !moreTags }) { Text(if(moreTags) "태그 접기" else "태그 더 보기") }
    PrivacyNote()
    Text("비슷한 콘텐츠 추천에 취향이 반영돼요.", color = muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
    if (shareSaveError.isNotBlank()) Text(shareSaveError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("share-save-error"))
    Primary(if (busy) "저장하는 중…" else "내 취향에 반영하기", !busy && shareTags.isNotEmpty(), Modifier.testTag("share-save")) {
     val url = sheet; val selected = shareTags
     scope.launch { busy = true; shareSaveError = ""; try { withTimeout(25000) { repo.publish(selected, url, true) }; message = "릴스를 보관함에 저장했어요."; if (!state.isDemo) prefs.edit().putBoolean("$owner:onboarded", true).apply(); step = 0; page = "보관함"; if (sheet == url) dismissShare() } catch (e: TimeoutCancellationException) { shareSaveError = "저장 확인이 지연됐어요. 보관함에서 확인한 후 재시도해 주세요." } catch (e: CancellationException) { throw e } catch (e: Exception) { shareSaveError = e.userMessage() } finally { busy = false } }
    }
    TextButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = { dismissShare() }) { Text("취소") }
   }
  }
  if (showingUrlInput) AlertDialog(onDismissRequest = { showingUrlInput = false; linkInputError = "" }, title = { Text("릴스 링크 담기") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
   OutlinedTextField(link, { link = it; linkInputError = "" }, label = { Text("Instagram Reel 링크") }, singleLine = true, isError = linkInputError.isNotEmpty(), modifier = Modifier.fillMaxWidth().testTag("share-url"))
   if(linkInputError.isNotEmpty()) Text(linkInputError,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
  } }, confirmButton = { TextButton(enabled=!busy,onClick = { val valid = ReelUrl.normalize(link); if(valid==null) linkInputError="올바른 개별 Instagram Reel 링크를 입력해 주세요." else { sheet=valid; incoming=valid; shareTags=emptySet(); shareSaveError=""; showingUrlInput=false; linkInputError="" } }) { Text("링크 확인") } }, dismissButton = { TextButton(onClick = { showingUrlInput=false; linkInputError="" }) { Text("취소") } })
  infoDemo?.let { c -> AlertDialog(onDismissRequest = { infoDemo = null }, title = { Text(c.title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("데모 콘텐츠 · 영상 미제공", color = roseDeep, fontWeight = FontWeight.Bold); Text(c.description); Text("제공된 UI 시안의 예시 사진이에요. 실제 Instagram 영상이나 검색 결과가 아니에요."); Text("내 취향과 맞는 이유: " + (state.myTaste.tags intersect c.tags).joinToString(" · ") { labels[it] ?: it }.ifEmpty { "새로운 취향 탐색" }) } }, confirmButton = { TextButton(onClick = { infoDemo = null; page = "취향"; showingUrlInput = true }) { Text("실제 릴스 링크 담기") } }, dismissButton = { TextButton(onClick = { infoDemo = null }) { Text("닫기") } }) }
  noticeDialog?.let { notice -> AlertDialog(onDismissRequest = { noticeDialog = null }, title = { Text("안내") }, text = { Text(notice) }, confirmButton = { TextButton(onClick = { noticeDialog = null }) { Text("확인") } }) }
  removeUrl?.let { url -> AlertDialog(onDismissRequest = { removeUrl = null }, title = { Text("보관함에서 삭제할까요?") }, text = { Text("저장한 링크를 보관함에서 지워요. 취향 선택은 유지돼요.") }, confirmButton = { TextButton(onClick = { removeUrl = null; action(success = { if(previewUrl == url) previewUrl = null }) { repo.removeSaved(url); "보관함에서 삭제했어요." } }) { Text("삭제") } }, dismissButton = { TextButton(onClick = { removeUrl = null }) { Text("취소") } }) }
  classifyUrl?.let { target -> AlertDialog(onDismissRequest = { classifyUrl = null }, title = { Text("후보 태그 편집") }, text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text("영상 내용을 직접 확인하고 분류해 주세요. 이 기기에만 저장돼요."); Text(target, style = MaterialTheme.typography.bodySmall); Tags(tagVocabulary, classifyTags) { classifyTags = classifyTags.toggle(it) } } }, confirmButton = { TextButton(onClick = { catalog.value = CandidateCatalog(state.user?.uid, catalog.value.items.map { if (it.url == target) it.copy(tags = classifyTags) else it }); prefs.edit().putString("$owner:catalog", json.encodeToString(catalog.value.items)).apply(); classifyUrl = null; message = "후보 태그를 저장했어요." }) { Text("분류 저장") } }, dismissButton = { TextButton(onClick = { classifyUrl = null }) { Text("취소") } }) }
  if (unlinkConfirm) AlertDialog(onDismissRequest = { unlinkConfirm = false }, title = { Text("커플 연결을 해제할까요?") }, text = { Text("서로의 추천 정보 접근이 종료돼요. 내 취향과 보관함은 유지돼요.") }, confirmButton = { TextButton(enabled = !busy, onClick = { unlinkConfirm = false; action { repo.unlink(); "연결을 해제했어요." } }) { Text("연결 해제") } }, dismissButton = { TextButton(onClick = { unlinkConfirm = false }) { Text("취소") } })
  if (diagnostics && BuildConfig.DEBUG) AlertDialog(onDismissRequest = { diagnostics = false }, title = { Text("재생 검증 도구") }, text = { Text("P0-1의 독립 재생 검증 화면을 열어요.") }, confirmButton = { TextButton(onClick = { diagnostics = false; startActivity(ReelIntents.playback(this@MvpActivity, null)) }) { Text("검증 화면 열기") } }, dismissButton = { TextButton(onClick = { diagnostics = false }) { Text("닫기") } })
  BackHandler(enabled = sheet.isBlank() && !showingUrlInput && !diagnostics && classifyUrl == null && !unlinkConfirm && infoDemo == null && noticeDialog == null && removeUrl == null && (registering || settings || previewUrl != null || step > 0 || (active && page != "발견"))) {
   when {
    previewUrl != null -> previewUrl = null
    settings -> settings = false
    registering -> registering = false
    step == 2 -> step = 1
    step == 1 -> { step = 0; page = "발견" }
    else -> page = "발견"
   }
  }

 }
}
private fun Set<String>.toggle(tag: String) = if (tag in this) this - tag else this + tag
