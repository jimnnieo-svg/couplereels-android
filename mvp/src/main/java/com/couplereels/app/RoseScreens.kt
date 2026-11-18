package com.couplereels.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.couplereels.mvp.R

internal fun photoFor(tags:Set<String>):Int = when {
 "gold" in tags -> R.drawable.p07_gold
 "silver" in tags || "jewelry" in tags || "necklace" in tags -> R.drawable.p07_necklace
 "perfume" in tags -> R.drawable.p07_perfume
 "flowers" in tags -> R.drawable.p07_flowers
 "food" in tags || "budget" in tags -> R.drawable.p07_cafe
 "travel" in tags || "date" in tags -> R.drawable.p07_travel
 "bold" in tags -> R.drawable.p07_bold
 else -> R.drawable.p07_minimal
}
@Composable internal fun ReferencePhoto(resource:Int,modifier:Modifier=Modifier) { Image(painterResource(resource),contentDescription=null,modifier=modifier,contentScale=ContentScale.Crop) }
internal data class StyleSection(val title:String,val icon:String,val values:List<String>)
internal fun styleSections(tags:Set<String>):List<StyleSection> {
 val groups=mapOf("jewelry" to listOf("silver","gold","minimal","bold"),"fashion" to listOf("minimal","bold","daily"),"perfume" to listOf("minimal","bold","anniversary"),"flowers" to listOf("daily","anniversary"),"food" to listOf("budget","date"),"travel" to listOf("date","anniversary"),"tech" to listOf("daily","budget"),"hobby" to listOf("daily","bold"))
 return interests.filter { it in tags }.map { StyleSection("${labels[it]} 스타일",it,groups.getValue(it)) }.ifEmpty { listOf(StyleSection("선호 스타일","heart",styles)) }
}
internal fun matchesSavedFilter(tags:Set<String>,filter:String):Boolean = when(filter) {
 "all" -> true
 "jewelry" -> tags.any { it in setOf("jewelry","silver","gold","necklace","pendant","925") }
 "date" -> tags.any { it in setOf("date","food","travel","anniversary") }
 else -> filter in tags
}

@Composable internal fun SectionTitle(icon:String,title:String) { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) { Glyph(icon,Modifier.size(23.dp),pink); Text(title,style=MaterialTheme.typography.titleLarge) } }
@Composable internal fun ChoiceCheck(modifier:Modifier=Modifier) { Box(modifier.size(24.dp).background(pink,CircleShape),contentAlignment=Alignment.Center) { Glyph("check",Modifier.size(17.dp),Color.White) } }

@Composable internal fun InterestChoices(selected:Set<String>,onToggle:(String)->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { interests.chunked(2).forEach { row ->
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { row.forEach { tag ->
   val checked=tag in selected
   Box(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if(checked) baby else Color.White.copy(alpha=.25f)).border(1.dp,if(checked) Color(0xFFEFBACB) else blushBorder,RoundedCornerShape(18.dp)).toggleable(checked,role=Role.Checkbox,onValueChange={onToggle(tag)}).testTag("interest-$tag").padding(9.dp)) {
    Column(Modifier.fillMaxWidth().heightIn(min=76.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp,Alignment.CenterVertically)) { Glyph(tag,Modifier.size(36.dp)); Text(labels.getValue(tag),fontSize=16.sp,fontWeight=FontWeight.Bold) }
    if(checked) ChoiceCheck(Modifier.align(Alignment.TopEnd))
   }
  } }
 } }
}
@Composable internal fun StyleChoices(values:List<String>,selected:Set<String>,onToggle:(String)->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { values.chunked(2).forEach { row ->
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { row.forEach { tag ->
   val checked=tag in selected
   Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if(checked) baby else Color.White.copy(alpha=.3f)).border(1.dp,if(checked) Color(0xFFEFBACB) else blushBorder,RoundedCornerShape(18.dp)).toggleable(checked,role=Role.Checkbox,onValueChange={onToggle(tag)}).testTag("style-$tag").padding(5.dp),horizontalAlignment=Alignment.CenterHorizontally) {
    Box { ReferencePhoto(when(tag){"silver"->R.drawable.p07_silver;"gold"->R.drawable.p07_gold;"minimal"->R.drawable.p07_minimal;"bold"->R.drawable.p07_bold;else->photoFor(setOf(tag))},Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(13.dp))); if(checked) ChoiceCheck(Modifier.align(Alignment.TopEnd).padding(5.dp)) }
    Text(labels.getValue(tag),Modifier.padding(vertical=7.dp),fontSize=16.sp,fontWeight=FontWeight.Bold)
   }
  }; if(row.size==1) Spacer(Modifier.weight(1f)) }
 } }
}
@Composable internal fun OnboardingContent(step:Int,tags:Set<String>,busy:Boolean,onToggle:(String)->Unit,onNext:()->Unit,onSkip:()->Unit,onPrevious:()->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(9.dp)) { Text("취향 알아보기 $step/2",color=muted,fontWeight=FontWeight.SemiBold); LinearProgressIndicator(progress={step/2f},modifier=Modifier.width(245.dp).height(8.dp).clip(CircleShape),color=pink,trackColor=baby) }
  Heading(if(step==1) "어떤 걸 좋아하세요?" else "어떤 스타일이 좋아요?",if(step==1) "관심 있는 주제를 골라주세요" else "선택한 관심사의 세부 취향을 알려주세요")
  if(step==1) {
   Text("여러 개 선택할 수 있어요",Modifier.background(baby.copy(alpha=.55f),CircleShape).padding(horizontal=12.dp,vertical=5.dp),color=muted,style=MaterialTheme.typography.bodySmall)
   InterestChoices(tags,onToggle)
  } else {
   styleSections(tags).forEach { group -> SectionTitle(group.icon,group.title); StyleChoices(group.values,tags,onToggle) }
   SectionTitle("gift","선물로 받고 싶은 것 (선택)")
   Tags(gifts,tags,!busy,onToggle)
  }
  PrivacyNote("선택한 취향은 상대에게 그대로 공개되지 않아요")
  Primary(if(step==1) "다음" else "취향 저장하고 시작하기",!busy && tags.isNotEmpty(),Modifier.testTag("onboarding-next"),onNext)
  TextButton(enabled=!busy,onClick=onSkip,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("나중에 할게요",color=muted) }
  if(step==2) TextButton(onClick=onPrevious,modifier=Modifier.fillMaxWidth()) { Text("이전") }
 }
}

@Composable internal fun EmptyContent(title:String,subtitle:String,icon:String="bookmark") {
 SoftCard { Glyph(icon,Modifier.align(Alignment.CenterHorizontally).padding(12.dp).size(40.dp),pink); Text(title,fontWeight=FontWeight.SemiBold); Text(subtitle,color=muted,style=MaterialTheme.typography.bodyMedium) }
}

/** Illustrations reflect user-assigned tags. They are explicitly not fetched Reel thumbnails. */
@Composable internal fun ReelTile(item:SavedReel,onOpen:()->Unit,onRemove:(()->Unit)?=null,modifier:Modifier=Modifier) {
 Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha=.6f)).border(1.dp,blushBorder,RoundedCornerShape(16.dp))) {
  Box(Modifier.fillMaxWidth().height(158.dp).clickable(onClick=onOpen)) {
   ReferencePhoto(photoFor(item.tags),Modifier.fillMaxSize())
   Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(ink.copy(alpha=.67f)).padding(5.dp)) { Text("태그 예시 · 원본 썸네일 아님",color=Color.White,fontSize=10.sp,lineHeight=14.sp) }
   if(onRemove!=null) IconButton(onClick=onRemove,modifier=Modifier.align(Alignment.TopEnd).padding(3.dp).size(48.dp).semantics { contentDescription="보관함에서 삭제" }) { Box(Modifier.size(30.dp).background(cream,CircleShape),contentAlignment=Alignment.Center) { Glyph("bookmark",Modifier.size(20.dp),pink,true) } }
   else Box(Modifier.align(Alignment.TopEnd).padding(8.dp).background(ink.copy(alpha=.5f),RoundedCornerShape(8.dp)).padding(5.dp)) { Glyph("lock",Modifier.size(13.dp),Color.White) }
  }
  Row(Modifier.fillMaxWidth().clickable(onClick=onOpen).heightIn(min=48.dp).padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) { Text(item.tags.take(2).joinToString(" ") { labels[it] ?: it }.ifEmpty { "Instagram" }+" 릴스",Modifier.weight(1f),fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis); Glyph("chevron",Modifier.size(15.dp),muted) }
 }
}
@Composable internal fun ReelGrid(items:List<SavedReel>,onOpen:(SavedReel)->Unit,onRemove:((SavedReel)->Unit)?=null) {
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { items.chunked(2).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) { row.forEach { item -> ReelTile(item,{onOpen(item)},onRemove?.let { {it(item)} },Modifier.weight(1f)) }; if(row.size==1) Spacer(Modifier.weight(1f)) } } }
}
@Composable internal fun TasteContent(state:AppData,onEdit:()->Unit,onGifts:()->Unit,onOpen:(SavedReel)->Unit,onShare:()->Unit) {
 Heading("내 취향","좋아하는 것을 조금씩 알려주세요")
 SoftCard { SectionTitle("heart","내가 좋아하는 것"); if(state.myTaste.tags.isEmpty()) Text("아직 선택한 취향이 없어요",color=muted) else Tags(state.myTaste.tags,state.myTaste.tags) {onEdit()}; OutlinedButton(onClick=onEdit,modifier=Modifier.heightIn(min=48.dp)) { Text("취향 수정",color=ink) } }
 SoftCard { SectionTitle("gift","선물로 받고 싶은 것"); val chosen=state.myTaste.tags intersect gifts.toSet(); Row(verticalAlignment=Alignment.CenterVertically) { Box(Modifier.weight(1f)) { if(chosen.isEmpty()) Text("받고 싶은 선물을 골라주세요",color=muted) else Tags(chosen,chosen) {onGifts()} }; IconButton(onClick=onGifts,modifier=Modifier.semantics {contentDescription="선물 취향 수정"}) { Glyph("edit",color=muted) } } }
 SectionTitle("reel","내가 공유한 릴스")
 val shared=state.saved.filter {it.tags.isNotEmpty()}
 if(shared.isEmpty()) EmptyContent("공유한 릴스가 아직 없어요","좋아하는 릴스 링크를 담아보세요.","reel") else ReelGrid(shared,onOpen)
 PrivacyNote()
 Primary("릴스 공유하기",onClick=onShare)
}
@Composable internal fun CoupleUnlinked(demo:Boolean,onConnect:()->Unit,onInvite:()->Unit,onCode:()->Unit,onLater:()->Unit,onSettings:()->Unit) {
 Heading("우리","우리만의 취향 공간")
 SoftCard {
  Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp,Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically) {
   listOf(0,1).forEach { i -> if(i==1) Glyph("heart",Modifier.size(20.dp),pink,true); Box(Modifier.size(80.dp).background(cream.copy(alpha=.65f),CircleShape).border(2.dp,Color.White,CircleShape),contentAlignment=Alignment.Center) { Glyph("person",Modifier.size(38.dp),Color(0xFFB89C9E)) } }
  }
  Text("아직 연결된 연인이 없어요",Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.titleLarge)
  Text(if(demo) "체험모드에서는 실제 커플 연결을 지원하지 않아요." else "연결하면 서로의 취향이 추천 피드에 반영돼요.",color=muted,style=MaterialTheme.typography.bodyMedium)
  Primary("커플 연결하기",onClick=onConnect)
  TextButton(onClick=onLater,modifier=Modifier.fillMaxWidth()) { Text("나중에 연결할게요",color=muted) }
 }
 SoftCard { MenuRow("link","초대 코드 만들기",onInvite); HorizontalDivider(color=blushBorder); MenuRow("code","코드 입력하기",onCode) }
 PrivacyNote("내 취향과 원본 릴스는 직접 공개되지 않아요")
 SoftCard { MenuRow("settings","계정 설정",onSettings) }
}
@Composable internal fun ShareLinkSummary(url:String,onExternal:()->Unit) {
 Row(Modifier.fillMaxWidth().border(1.dp,blushBorder,RoundedCornerShape(16.dp)).background(Color.White.copy(alpha=.5f),RoundedCornerShape(16.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
  Box(Modifier.size(68.dp).background(baby,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) { Glyph("reel",Modifier.size(32.dp),roseDeep) }
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) { Text("Instagram Reel",fontWeight=FontWeight.Bold); Text(url,style=MaterialTheme.typography.bodySmall,color=muted,modifier=Modifier.testTag("share-sheet-url"),maxLines=2,overflow=TextOverflow.Ellipsis) }
  IconButton(onClick=onExternal,modifier=Modifier.semantics {contentDescription="Instagram 원본 열기"}) { Glyph("external",Modifier.size(21.dp)) }
 }
}
