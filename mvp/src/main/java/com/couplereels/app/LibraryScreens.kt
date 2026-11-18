package com.couplereels.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable internal fun P08Root(onExit:()->Unit,onProbe:()->Unit) {
 val context=LocalContext.current
 var store by remember {mutableStateOf<LocalLibrary?>(null)}
 var error by remember {mutableStateOf<String?>(null)}
 var retry by remember {mutableIntStateOf(0)}
 var probe by remember {mutableStateOf(false)}
 LaunchedEffect(retry) {try {store=withContext(Dispatchers.IO){LocalLibrary.get(context)};LocalLibrary.classifyLater(store!!)}catch(e:Exception){error="기존 데이터를 읽지 못했어요. 데이터를 지우지 말고 다시 시도해 주세요."}}
 if(store==null) Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),contentAlignment=Alignment.Center) {
  Column {Text(error?:"보관함을 불러오는 중…");if(error!=null) TextButton(onClick={error=null;retry++}){Text("다시 시도")}}
 } else P08LibraryApp(store!!,onExit,{probe=true})
 if(probe)AlertDialog(onDismissRequest={probe=false},title={Text("재생 검증 도구")},text={Text("P0-1의 독립 재생 검증 화면을 열어요.")},confirmButton={TextButton(onClick={probe=false;onProbe()}){Text("검증 화면 열기")}},dismissButton={TextButton(onClick={probe=false}){Text("닫기")}})
}

@OptIn(ExperimentalLayoutApi::class,ExperimentalFoundationApi::class)
@Composable internal fun P08LibraryApp(store:LocalLibrary,onExit:()->Unit,onProbe:()->Unit) {
 val s by store.state.collectAsStateWithLifecycle()
 val person=s.current;val profile=s.profile(person)
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 var page by rememberSaveable {mutableStateOf("발견")}
 var detail by rememberSaveable(person) {mutableStateOf<String?>(null)}
 var step by rememberSaveable(person) {mutableIntStateOf(if(profile.onboarded)0 else 1)}
 var taste by remember(person,step) {mutableStateOf(profile.taste.tags)}
 var settings by rememberSaveable {mutableStateOf(false)}
 var busy by remember {mutableStateOf(false)}
 var notice by remember {mutableStateOf("")}
 var input by rememberSaveable {mutableStateOf<String?>(null)}
 var inputError by remember {mutableStateOf("")}
 var editing by remember(person) {mutableStateOf<LocalReel?>(null)}
 var removing by remember(person) {mutableStateOf<LocalReel?>(null)}
 var collectionEditor by remember(person) {mutableStateOf(false)}
 val feedPartner=profile.partnerFeed
 fun runJob(done:()->Unit={},work:()->Unit) {if(busy)return;scope.launch {busy=true;try {withContext(Dispatchers.IO){work()};done()}catch(e:Exception){notice=e.message?:"저장하지 못했어요. 다시 시도해 주세요."}finally{busy=false}}}
 fun open(url:String) {runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}.onFailure {notice="Instagram을 열 수 없어요. 링크를 복사해 주세요."}}
 fun copy(url:String) {(context.getSystemService(ClipboardManager::class.java)).setPrimaryClip(ClipData.newPlainText("Instagram Reel",url));notice="링크를 복사했어요."}
 val currentReel=profile.reels.find {it.url==detail}
 LaunchedEffect(currentReel,detail) {if(detail!=null&&currentReel==null)detail=null}
 LaunchedEffect(notice){if(notice.isNotEmpty()){delay(4000);notice=""}}
 Scaffold(containerColor=cream,contentWindowInsets=WindowInsets(0,0,0,0),bottomBar={if(step==0)CoupleNavigation(page){page=it;detail=null;settings=false;notice=""}}) {pad->
  Column(Modifier.fillMaxSize().padding(pad).statusBarsPadding().padding(bottom=if(step>0)WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp).imePadding()) {
   Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
    Brand(Modifier.combinedClickable(onClick={},onLongClick=onProbe),compact=true)
    Spacer(Modifier.weight(1f))
    Text(person.title,color=roseDeep,fontSize=12.sp,fontWeight=FontWeight.Bold)
   }
   Text("로컬 체험 · 실제 계정/커플 연결 아님",Modifier.padding(horizontal=16.dp),color=muted,fontSize=11.sp)
   if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
   if(notice.isNotBlank())Text(notice,Modifier.padding(horizontal=16.dp).testTag("notice"),color=roseDeep,fontSize=13.sp)
   if(step>0) Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
    OnboardingContent(step,taste,busy,{taste=if(it in taste)taste-it else taste+it},onNext={if(step==1)step=2 else runJob({step=0;page="발견"}){store.taste(person,taste)}},onSkip={runJob({step=0}){store.taste(person,taste)}},onPrevious={step=1})
   } else if(currentReel!=null) Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    TextButton(onClick={detail=null}){Text("보관함으로")}
    Text("저장한 릴스",style=MaterialTheme.typography.titleLarge)
    Text(savedDate(currentReel),color=muted,fontSize=12.sp)
    SharingChoices(currentReel.sharing,!busy){value->runJob {store.sharing(person,currentReel.url,value)}}
    Text(if(currentReel.sharing==Sharing.SIMILAR)"상대 화면에는 원본 링크를 공개하지 않아요." else "상대 체험 화면에 원본 링크를 공개해요.",fontSize=12.sp,color=muted)
    Row {TextButton(onClick={editing=currentReel}){Text("카테고리·태그 편집")};TextButton(onClick={removing=currentReel}){Text("삭제")}}
    Text(classificationLabel(currentReel),fontSize=12.sp,color=muted)
    ReelPreview(currentReel.url,Modifier.fillMaxWidth().height(420.dp),{open(currentReel.url)},{copy(currentReel.url)},autoAttempt=true)
   } else if(page=="발견") {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     FilterChip(!feedPartner,{runJob {store.feedSelection(person,false)}},label={Text("내 취향")},modifier=Modifier.weight(1f).testTag("feed-mine"))
     FilterChip(feedPartner,{runJob {store.feedSelection(person,true)}},label={Text("상대 취향")},modifier=Modifier.weight(1f).testTag("feed-partner"))
    }
    val other=s.partnerView(person)
    val reels=if(feedPartner)other.direct else profile.reels.sortedByDescending {it.savedAt}
    Text(if(feedPartner)"${person.other().title} · 로컬 체험 데이터" else "저장한 릴스 · ${person.title}",Modifier.padding(horizontal=16.dp,vertical=4.dp),color=muted,fontSize=12.sp)
    if(feedPartner&&other.awaitingSimilar)Text("아직 비슷한 릴스를 찾지 못했어요",Modifier.padding(horizontal=16.dp,vertical=4.dp).testTag("similar-unavailable"),color=muted,fontSize=12.sp)
    if(reels.isEmpty()) Column(Modifier.weight(1f).padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
     Glyph("bookmark",Modifier.size(48.dp),pink)
     Text(if(feedPartner)"공개된 릴스가 아직 없어요." else "아직 저장한 릴스가 없어요.",fontWeight=FontWeight.Bold)
     Text(if(feedPartner)"우리에서 체험 프로필을 바꾸어 릴스를 담아보세요." else "인스타그램에서 마음에 드는 릴스를 공유해 보세요 ♡",color=muted)
     TextButton(onClick={if(feedPartner){page="우리";settings=true}else input=""}){Text(if(feedPartner)"체험 설정" else "릴스 링크 담기")}
    } else key(person,feedPartner) {
     val pager=rememberPagerState {reels.size}
     LaunchedEffect(reels.map {it.url}) {pager.scrollToPage(0)}
     VerticalPager(pager,modifier=Modifier.weight(1f).testTag("discover-pager"),key={reels[it].url}) {index->
      val reel=reels[index]
      Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("${index+1} / ${reels.size}",color=muted);Text(if(feedPartner)"직접 공유한 릴스" else reel.sharing.title,color=roseDeep,fontSize=12.sp)}
       if(index==pager.currentPage)ReelPreview(reel.url,Modifier.weight(1f).fillMaxWidth(),{open(reel.url)},{copy(reel.url)},autoAttempt=true)
       else ReelPlaceholder(reel,Modifier.weight(1f).fillMaxWidth())
       Text(reel.categories.mapNotNull {id->s.profile(reel.owner).collections.find {it.id==id}?.name}.joinToString(" · "),color=muted,fontSize=12.sp)
       if(!feedPartner)Row {TextButton(onClick={detail=reel.url}){Text("공유 방식·상세")};TextButton(onClick={editing=reel}){Text("분류 변경")};TextButton(onClick={removing=reel}){Text("삭제")}}
      }
     }
    }
   } else if(page=="보관함") {
    LibraryGrid(profile,Modifier.weight(1f),onOpen={detail=it.url},onAdd={collectionEditor=true},onInput={input=""})
   } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    if(page=="취향") {
     Heading("내 취향","${person.title}의 취향을 모아봐요")
     SoftCard {SectionTitle("heart","내가 좋아하는 것");Tags(profile.taste.tags,profile.taste.tags){};TextButton(onClick={taste=profile.taste.tags;step=1}){Text("취향 수정")}}
     Text("저장한 릴스 ${profile.reels.size}개",fontWeight=FontWeight.Bold)
     Text("로컬 취향 매칭",style=MaterialTheme.typography.titleMedium)
     Text("내 저장 릴스를 태그 점수로 정렬해요. 새 릴스 검색 결과가 아니에요.",fontSize=12.sp,color=muted)
     val ranked=RecommendationEngine.rank(profile.taste,profile.reels.map {Candidate(it.url,it.tags)},10)
     if(ranked.isEmpty())Text("릴스를 저장하면 여기에서 확인할 수 있어요.",color=muted)
     ranked.forEach {item->OutlinedCard(onClick={detail=item.candidate.url},modifier=Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp)){Text("Instagram Reel · ${item.candidate.url.substringAfter("/reel/").trimEnd('/')}",maxLines=1,overflow=TextOverflow.Ellipsis);Text("태그 매칭 ${(item.score*100).toInt()}%",fontSize=12.sp,color=muted)}}}
     Primary("릴스 링크 담기"){input=""}
    } else {
     Heading(if(settings)"체험 설정" else "우리","한 기기에서 나누는 두 사람의 취향")
     SoftCard {
      SectionTitle("people","현재 체험 프로필")
      LocalPerson.entries.forEach {value->FilterChip(person==value,onClick={runJob({detail=null;step=0;notice="${value.title} 프로필로 전환했어요."}){store.select(value)}},label={Text(value.title)},enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("profile-${value.name}"))}
      Text("선택한 프로필의 보관함에 저장해요. 실제 상대방 계정과 연결되지 않아요.",color=muted,fontSize=12.sp)
     }
     SoftCard {SectionTitle("heart","공유 기본 설정");SharingChoices(profile.defaultSharing,!busy){value->runJob {store.defaultSharing(person,value)}};Text("앞으로 저장할 릴스에만 적용해요.",color=muted,fontSize=12.sp)}
     Text("실제 커플 연결은 아직 지원하지 않아요. 체험 프로필 간 DIRECT 릴스만 원본을 볼 수 있어요.",color=muted,fontSize=12.sp)
     TextButton(onClick={settings=!settings}){Text(if(settings)"우리로 돌아가기" else "체험 설정")}
     TextButton(onClick={runJob(onExit){DemoStore(context).setActive(false)}}){Text("체험 나가기")}
    }
   }
  }
 }
 if(input!=null) AlertDialog(onDismissRequest={input=null;inputError=""},title={Text("릴스 링크 담기")},text={Column {OutlinedTextField(input!!,{input=it;inputError=""},label={Text("Instagram Reel 링크")},modifier=Modifier.testTag("share-url"));if(inputError.isNotEmpty())Text(inputError,color=MaterialTheme.colorScheme.error)}},confirmButton={TextButton(enabled=!busy,onClick={val text=input!!;if(ReelUrl.fromShare(text)==null)inputError="올바른 개별 릴스 링크를 입력해 주세요." else runJob({input=null;notice="보관함에 저장했어요 ♡";LocalLibrary.classifyLater(store)}){store.save(text,person)}}){Text("저장")}},dismissButton={TextButton(onClick={input=null}){Text("취소")}})
 editing?.let {reel->ReelEditor(reel,profile,onDismiss={editing=null},onSave={cats,tags->runJob({editing=null}){store.edit(person,reel.url,cats,tags)}})}
 removing?.let {reel->AlertDialog(onDismissRequest={removing=null},title={Text("이 릴스를 삭제할까요?")},text={Text("보관함과 내 취향 피드에서 삭제해요.")},confirmButton={TextButton(onClick={runJob({removing=null;detail=null}){store.remove(person,reel.url)}}){Text("삭제")}},dismissButton={TextButton(onClick={removing=null}){Text("취소")}})}
 if(collectionEditor)CollectionEditor(profile,{collectionEditor=false},onAdd={name->runJob {store.addCollection(person,name)}},onRename={id,name->runJob {store.renameCollection(person,id,name)}},onRemove={id->runJob {store.removeCollection(person,id)}})
 BackHandler(input==null&&editing==null&&removing==null&&!collectionEditor&&(detail!=null||step>0||settings||page!="발견")) {when {detail!=null->detail=null;step==2->step=1;step==1->step=0;settings->settings=false;else->page="발견"}}
}

private fun savedDate(reel:LocalReel)=if(reel.knownSaveTime)SimpleDateFormat("yyyy.MM.dd HH:mm",Locale.KOREA).format(Date(reel.savedAt))+" 저장" else "이전 버전에서 가져옴 · 저장일 미상"
internal fun classificationLabel(reel:LocalReel)=when(reel.classification){Classification.PENDING->"저장 완료 · 분류 대기";Classification.RULES->"공유 문구의 규칙 기반 자동 분류";Classification.MANUAL->"직접 분류";Classification.LEGACY->"기존 저장 릴스 · 분류를 선택해 주세요";else->"미분류 · 공유 문구만으로 내용을 알 수 없어요"}
@Composable internal fun SharingChoices(selected:Sharing,enabled:Boolean=true,onSelect:(Sharing)->Unit) {
 Column {Sharing.entries.forEach {value->Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(enabled=enabled){onSelect(value)}.testTag("sharing-${value.name}"),verticalAlignment=Alignment.CenterVertically) {RadioButton(selected==value,onClick=null,enabled=enabled);Text(value.title,Modifier.padding(start=8.dp),fontSize=14.sp)}}}
}
@Composable private fun ReelPlaceholder(reel:LocalReel,modifier:Modifier=Modifier) {
 Box(modifier.background(baby.copy(alpha=.6f)),contentAlignment=Alignment.Center) {Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(6.dp)) {Glyph("reel",Modifier.size(30.dp),roseDeep);Text("Instagram Reel",fontSize=11.sp,color=ink);Text("썸네일 미제공",fontSize=10.sp,color=muted);Text(reel.url.substringAfter("/reel/").trimEnd('/'),fontSize=10.sp,color=muted,maxLines=1,overflow=TextOverflow.Ellipsis)}}
}
@Composable private fun LibraryGrid(profile:LocalProfile,modifier:Modifier,onOpen:(LocalReel)->Unit,onAdd:()->Unit,onInput:()->Unit) {
 var filter by remember(profile.collections) {mutableStateOf("all")};var query by rememberSaveable {mutableStateOf("")};var search by rememberSaveable {mutableStateOf(false)}
 val entries=profile.reels.filter {(filter=="all"||filter in it.categories)&&(query.isBlank()||it.url.contains(query,true)||it.caption.contains(query,true)||it.tags.any {t->(labels[t]?:t).contains(query,true)}||it.categories.any {id->profile.collections.any {it.id==id&&it.name.contains(query,true)}})}.sortedByDescending {it.savedAt}
 Column(modifier) {
  Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {Text("보관함",Modifier.weight(1f),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);IconButton(onClick={search=!search},modifier=Modifier.semantics{contentDescription="검색"}){Text("⌕",fontSize=28.sp)};IconButton(onClick=onAdd,modifier=Modifier.semantics{contentDescription="컬렉션 추가 및 편집"}){Text("+",fontSize=26.sp)}}
  if(search)OutlinedTextField(query,{query=it},label={Text("링크·태그·컬렉션 검색")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp))
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {(listOf(ReelCollection("all","전체"))+profile.collections).forEach {c->FilterChip(filter==c.id,{filter=c.id},label={Text(c.name)},modifier=Modifier.testTag("collection-${c.id}"))}}
  val used=profile.collections.filter {c->profile.reels.any {c.id in it.categories}}
  if(used.isNotEmpty())Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {used.forEach {c->OutlinedCard(onClick={filter=c.id},modifier=Modifier.width(112.dp)){Column(Modifier.padding(10.dp)){Glyph("bookmark",Modifier.size(22.dp),pink);Text(c.name,fontWeight=FontWeight.Bold,maxLines=1);Text("${profile.reels.count {c.id in it.categories}}개",fontSize=12.sp,color=muted)}}}}
  Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {Text("${entries.size}개 · 최근 저장순",fontSize=12.sp,color=muted);TextButton(onClick=onInput){Text("링크 담기")}}
  if(entries.isEmpty())Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center){Text(if(profile.reels.isEmpty())"아직 저장한 릴스가 없어요.\n인스타그램에서 마음에 드는 릴스를 공유해 보세요 ♡" else "조건에 맞는 릴스가 없어요.",color=muted)}
  else LazyVerticalGrid(columns=GridCells.Fixed(3),modifier=Modifier.weight(1f).testTag("library-grid"),horizontalArrangement=Arrangement.spacedBy(2.dp),verticalArrangement=Arrangement.spacedBy(2.dp),contentPadding=PaddingValues(bottom=8.dp)) {
   items(entries,key={it.url}) {reel->Box(Modifier.aspectRatio(.69f).clickable{onOpen(reel)}.testTag("reel-${reel.url.substringAfter("/reel/").trimEnd('/')}").semantics{contentDescription="저장한 릴스 ${reel.url}, ${reel.sharing.title}"}) {ReelPlaceholder(reel,Modifier.fillMaxSize());Text(if(reel.sharing==Sharing.DIRECT)"직접" else "비슷한",Modifier.align(Alignment.TopEnd).background(cream.copy(alpha=.9f),RoundedCornerShape(6.dp)).padding(4.dp),fontSize=10.sp,color=roseDeep)}}
  }
 }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ReelEditor(reel:LocalReel,profile:LocalProfile,onDismiss:()->Unit,onSave:(Set<String>,Set<String>)->Unit) {
 var cats by remember(reel.url){mutableStateOf(reel.categories)};var tags by remember(reel.url){mutableStateOf(reel.tags)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("카테고리·태그")},text={Column(Modifier.verticalScroll(rememberScrollState())) {FlowRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){profile.collections.forEach {c->FilterChip(c.id in cats,{cats=if(c.id in cats)cats-c.id else cats+c.id},label={Text(c.name)})}};Text("취향 태그",fontWeight=FontWeight.Bold);Tags(tagVocabulary,tags){tags=if(it in tags)tags-it else tags+it}}},confirmButton={TextButton(onClick={onSave(cats,tags)}){Text("저장")}},dismissButton={TextButton(onClick=onDismiss){Text("취소")}})
}
@Composable private fun CollectionEditor(profile:LocalProfile,onDismiss:()->Unit,onAdd:(String)->Unit,onRename:(String,String)->Unit,onRemove:(String)->Unit) {
 var chosen by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")};var delete by remember{mutableStateOf<String?>(null)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("컬렉션 관리")},text={Column(Modifier.verticalScroll(rememberScrollState())) {OutlinedTextField(name,{name=it},label={Text(if(chosen==null)"새 컬렉션 이름" else "컬렉션 이름")});Row {TextButton(onClick={if(chosen==null)onAdd(name) else onRename(chosen!!,name);name="";chosen=null},enabled=name.isNotBlank()){Text(if(chosen==null)"추가" else "이름 저장")};if(chosen!=null)TextButton(onClick={chosen=null;name=""}){Text("취소")}};profile.collections.filter {it.id!="unclassified"}.forEach {c->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(c.name,Modifier.weight(1f));TextButton(onClick={chosen=c.id;name=c.name}){Text("수정")};TextButton(onClick={delete=c.id}){Text("삭제")}}}},confirmButton={TextButton(onClick=onDismiss){Text("닫기")}})
 delete?.let {id->AlertDialog(onDismissRequest={delete=null},title={Text("컬렉션을 삭제할까요?")},text={Text("릴스는 삭제하지 않아요. 다른 카테고리가 없으면 미분류로 옮겨요.")},confirmButton={TextButton(onClick={onRemove(id);delete=null;chosen=null;name=""}){Text("삭제")}},dismissButton={TextButton(onClick={delete=null}){Text("취소")}})}
}
