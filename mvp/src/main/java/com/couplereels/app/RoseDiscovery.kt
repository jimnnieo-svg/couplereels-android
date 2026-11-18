package com.couplereels.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.couplereels.mvp.R

@Composable internal fun DemoDiscovery(tags:Set<String>,preferences:DemoUiPreferences,modifier:Modifier=Modifier,onLike:(String)->Unit,onBookmark:(String)->Unit,onHide:(String)->Unit,onRestore:()->Unit,onInfo:(DemoContent)->Unit) {
 val ranked=remember(tags,preferences.hidden) {DemoRecommendations.rank(tags).filter {it.content.id !in preferences.hidden}}
 if(ranked.isEmpty()) { Column(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp,Alignment.CenterVertically)) { Heading("새로운 발견을 기다려요","관심 없음으로 숨긴 예시를 다시 볼 수 있어요."); Primary("숨긴 데모 다시 보기",onClick=onRestore) }; return }
 val pager=rememberPagerState {ranked.size}
 VerticalPager(state=pager,key={ranked[it].content.id},modifier=modifier.testTag("discover-pager")) { index ->
  val item=ranked[index]; val c=item.content
  Box(Modifier.fillMaxSize()) {
   ReferencePhoto(if(c.id=="jewelry") R.drawable.p07_discover else photoFor(c.tags),Modifier.fillMaxSize())
   Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ink.copy(alpha=.46f),Color.Transparent,Color.Transparent,ink.copy(alpha=.82f)))))
   Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal=17.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) { Brand(light=true,compact=true); Text("연인을 위한 발견",color=Color.White,fontSize=11.sp,modifier=Modifier.border(1.dp,Color.White.copy(alpha=.4f),CircleShape).background(Color.White.copy(alpha=.12f),CircleShape).padding(horizontal=9.dp,vertical=6.dp)) }
   // No play triangle: these supplied photos are not playable videos.
   IconButton(onClick={onInfo(c)},modifier=Modifier.align(Alignment.Center).size(60.dp).background(ink.copy(alpha=.3f),CircleShape).semantics {contentDescription="데모 콘텐츠 정보"}) { Glyph("info",Modifier.size(28.dp),Color.White) }
   Column(Modifier.align(Alignment.BottomEnd).padding(end=9.dp,bottom=108.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
    FeedAction("heart",if(c.id in preferences.liked) "좋아요 취소" else "좋아요",c.id in preferences.liked) {onLike(c.id)}
    FeedAction("bookmark",if(c.id in preferences.bookmarked) "데모 보관 취소" else "데모 보관",c.id in preferences.bookmarked) {onBookmark(c.id)}
    FeedAction("down","관심 없음",false) {onHide(c.id)}
   }
   Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start=20.dp,end=62.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Text("데모 콘텐츠 · 영상 미제공",color=Color.White,fontSize=12.sp,modifier=Modifier.background(ink.copy(alpha=.55f),CircleShape).padding(horizontal=10.dp,vertical=5.dp))
    Text("추천 ${index+1}/${ranked.size}",color=Color.White.copy(alpha=.9f),style=MaterialTheme.typography.bodySmall)
    Text(c.title,color=Color.White,style=MaterialTheme.typography.headlineSmall)
    Text(if(item.matchingTags.isEmpty()) "새롭게 발견하는 취향" else item.matchingTags.joinToString(" · ") {labels[it] ?: it}+" 취향 추천",color=Color.White.copy(alpha=.9f),style=MaterialTheme.typography.bodySmall)
   }
  }
 }
}
@Composable private fun FeedAction(icon:String,label:String,active:Boolean,onClick:()->Unit) { IconButton(onClick=onClick,modifier=Modifier.size(48.dp).semantics {contentDescription=label}) { Glyph(icon,Modifier.size(29.dp),if(active) baby else Color.White,active) } }
@Composable internal fun DemoSavedGrid(items:List<DemoContent>,onOpen:(DemoContent)->Unit,onRemove:(DemoContent)->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { items.chunked(2).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) { row.forEach { c -> Column(Modifier.weight(1f).background(Color.White.copy(alpha=.6f),RoundedCornerShape(16.dp))) {
  Box(Modifier.fillMaxWidth().height(158.dp).clickable {onOpen(c)}) { ReferencePhoto(photoFor(c.tags),Modifier.fillMaxSize()); Text("데모 콘텐츠",Modifier.align(Alignment.BottomStart).background(ink.copy(alpha=.65f)).padding(5.dp),color=Color.White,fontSize=11.sp); IconButton(onClick={onRemove(c)},modifier=Modifier.align(Alignment.TopEnd).semantics {contentDescription="데모 보관 취소"}) { Glyph("bookmark",color=cream,filled=true) } }
  Text(c.title,Modifier.fillMaxWidth().clickable {onOpen(c)}.padding(10.dp),fontWeight=FontWeight.SemiBold,fontSize=14.sp)
 } }; if(row.size==1) Spacer(Modifier.weight(1f)) } } }
}
