package com.couplereels.app

import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

/** P0-7 tokens sampled from the supplied references; the darker rose is for readable text. */
internal val cream = Color(0xFFFEF6F6)
internal val baby = Color(0xFFF9DEE4)
internal val pink = Color(0xFFD08298)
internal val roseDeep = Color(0xFFB85373)
internal val ink = Color(0xFF362A2B)
internal val muted = Color(0xFF786C6E)
internal val blushBorder = Color(0xFFF2E2E5)
internal val labels = linkedMapOf("jewelry" to "주얼리", "fashion" to "패션", "perfume" to "향수", "flowers" to "꽃", "food" to "맛집", "travel" to "여행", "tech" to "전자기기", "hobby" to "취미", "silver" to "실버", "gold" to "골드", "minimal" to "심플", "bold" to "화려함", "necklace" to "목걸이", "gift" to "선물", "pendant" to "펜던트", "daily" to "데일리", "925" to "925 실버", "budget" to "가성비", "anniversary" to "기념일", "date" to "데이트")
internal val interests = labels.keys.take(8)
internal val styles = listOf("silver", "gold", "minimal", "bold", "daily", "budget")
internal val gifts = listOf("necklace", "flowers", "perfume", "anniversary", "gift")

@Composable internal fun CoupleTheme(content: @Composable () -> Unit) {
 MaterialTheme(colorScheme=lightColorScheme(primary=roseDeep,onPrimary=Color.White,primaryContainer=baby,onPrimaryContainer=ink,background=cream,onBackground=ink,surface=cream,onSurface=ink,secondaryContainer=baby,onSecondaryContainer=ink,outline=blushBorder),
 typography=Typography(headlineLarge=TextStyle(fontSize=30.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold),headlineSmall=TextStyle(fontSize=23.sp,lineHeight=30.sp,fontWeight=FontWeight.Bold),titleLarge=TextStyle(fontSize=20.sp,lineHeight=28.sp,fontWeight=FontWeight.Bold),titleMedium=TextStyle(fontSize=16.sp,lineHeight=22.sp,fontWeight=FontWeight.SemiBold),bodyLarge=TextStyle(fontSize=16.sp,lineHeight=24.sp),bodyMedium=TextStyle(fontSize=14.sp,lineHeight=21.sp),bodySmall=TextStyle(fontSize=12.sp,lineHeight=18.sp),labelLarge=TextStyle(fontSize=15.sp,fontWeight=FontWeight.SemiBold),labelMedium=TextStyle(fontSize=12.sp,lineHeight=18.sp)),
 shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(28.dp)),content=content)
}
@Composable internal fun Brand(modifier:Modifier=Modifier,light:Boolean=false,compact:Boolean=false) {
 Row(modifier,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
  Canvas(Modifier.size(if(compact) 31.dp else 38.dp,29.dp)) { val s=size.height/29f; drawCircle(Color(0xFFD58CA0),10*s,Offset(11*s,14*s),style=Stroke(2*s)); drawCircle(Color(0xFFDCA59D),9*s,Offset(24*s,15*s),style=Stroke(2*s)) }
  Text("CoupleReels",color=if(light) Color.White else ink,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=if(compact) 21.sp else 25.sp,maxLines=1)
 }
}
/** Native line icons on a 24-unit grid, never screenshots or font glyphs. */
private val iconPaths=mapOf(
 "heart" to "M12 20 C10 18 3 13 3 8 C3 3 9 2 12 6 C15 2 21 3 21 8 C21 13 14 18 12 20 Z",
 "home" to "M3 11 L12 3 L21 11 L19 11 L19 21 L14 21 L14 15 L10 15 L10 21 L5 21 L5 11 Z",
 "bookmark" to "M6 3 L18 3 L18 21 L12 17 L6 21 Z",
 "people" to "M9 11 A4 4 0 1 0 9 3 A4 4 0 1 0 9 11 M2 21 C2 12 16 12 16 21 M17 10 A3 3 0 1 0 17 4 M18 14 C22 14 23 17 23 20",
 "person" to "M12 11 A4 4 0 1 0 12 3 A4 4 0 1 0 12 11 M4 21 C4 11 20 11 20 21",
 "check" to "M4 12 L9 17 L20 6",
 "play" to "M8 5 L20 12 L8 19 Z",
 "info" to "M12 22 A10 10 0 1 0 12 2 A10 10 0 1 0 12 22 M12 10 L12 17 M12 7 L12 7.1",
 "close" to "M5 5 L19 19 M19 5 L5 19",
 "down" to "M8 3 L18 3 L18 14 L14 14 L13 21 C9 21 10 16 10 15 L4 15 Q2 15 3 12 L5 4 Z M21 3 L21 14",
 "bell" to "M5 17 L19 17 L17 14 L17 9 C17 2 7 2 7 9 L7 14 Z M10 20 Q12 23 14 20 M12 2 L12 3",
 "gift" to "M3 9 L21 9 L21 13 L3 13 Z M5 13 L5 22 L19 22 L19 13 M12 9 L12 22 M12 9 C2 9 5 -1 10 4 L12 9 C22 9 19 -1 14 4 Z",
 "lock" to "M5 10 L19 10 L19 22 L5 22 Z M8 10 L8 6 C8 0 16 0 16 6 L16 10 M12 15 L12 18",
 "edit" to "M4 16 L16 4 L20 8 L8 20 L3 21 Z M14 6 L18 10",
 "share" to "M4 21 C4 12 8 7 15 7 L15 2 L23 10 L15 17 L15 12 C9 12 6 16 4 21 Z",
 "external" to "M14 3 L21 3 L21 10 M21 3 L11 13 M10 5 L4 5 L4 21 L20 21 L20 15",
 "link" to "M9 15 L15 9 M8 10 L4 14 C0 18 6 24 10 20 L14 16 M10 8 L14 4 C18 0 24 6 20 10 L16 14",
 "code" to "M5 5 L5 5.1 M12 5 L12 5.1 M19 5 L19 5.1 M5 12 L5 12.1 M12 12 L12 12.1 M19 12 L19 12.1 M5 19 L5 19.1 M12 19 L12 19.1 M19 19 L19 19.1",
 "settings" to "M9 3 L15 3 L16 6 L20 7 L22 12 L20 17 L16 18 L15 21 L9 21 L8 18 L4 17 L2 12 L4 7 L8 6 Z M12 16 A4 4 0 1 0 12 8 A4 4 0 1 0 12 16",
 "chevron" to "M9 5 L16 12 L9 19",
 "jewelry" to "M3 3 C4 9 8 13 10 14 M21 3 C20 9 16 13 14 14 M12 21 A4 4 0 1 0 12 13 A4 4 0 1 0 12 21",
 "fashion" to "M9 5 C9 0 16 0 16 5 C16 8 12 8 12 11 L2 18 Q1 20 4 20 L20 20 Q23 20 22 18 L12 11",
 "perfume" to "M9 3 L13 3 L13 7 L9 7 Z M8 8 C1 13 4 21 7 22 L16 22 C21 18 20 12 15 8 Z M13 5 C21 5 23 8 21 11 C17 14 16 7 20 7",
 "flowers" to "M12 21 L12 12 M12 12 C2 13 4 1 8 5 C8 -1 15 -1 16 5 C22 1 22 13 12 12 M12 20 C7 21 3 17 2 14 C7 14 10 16 12 20 C17 20 21 17 22 14 C17 14 14 17 12 20",
 "food" to "M6 2 L6 9 C6 12 8 12 10 13 L10 22 L14 22 L14 13 C16 12 18 12 18 9 L18 2 M10 2 L10 9 M14 2 L14 9",
 "travel" to "M4 7 L20 7 L20 22 L4 22 Z M8 7 L8 3 L16 3 L16 7 M8 11 L8 18 M16 11 L16 18",
 "tech" to "M3 14 L3 11 C3 -1 21 -1 21 11 L21 14 M3 12 C-1 12 -1 22 4 22 L6 22 L6 12 Z M21 12 C25 12 25 22 20 22 L18 22 L18 12 Z",
 "hobby" to "M12 21 C-3 23 0 4 10 2 C20 -1 27 10 20 12 C15 12 18 22 12 21 Z M8 7 L8 7.1 M14 5 L14 5.1 M5 13 L5 13.1 M9 17 L9 17.1",
 "reel" to "M3 3 L21 3 L21 21 L3 21 Z M3 8 L21 8 M7 3 L11 8 M14 3 L18 8 M10 11 L16 15 L10 19 Z"
)
@Composable internal fun Glyph(kind:String,modifier:Modifier=Modifier.size(24.dp),color:Color=ink,filled:Boolean=false) {
 Canvas(modifier) { val p=androidx.core.graphics.PathParser.createPathFromPathData(iconPaths[kind] ?: iconPaths.getValue("heart"))!!.asComposePath(); scale(size.width/24f,size.height/24f,pivot=Offset.Zero) { drawPath(p,color,style=if(filled) Fill else Stroke(1.65f,cap=StrokeCap.Round,join=StrokeJoin.Round)) } }
}
@Composable internal fun HeartDecoration(modifier:Modifier=Modifier) {
 Canvas(modifier) {
  val w=size.width; val h=size.height
  val wave=Path().apply { moveTo(0f,h*.76f); cubicTo(w*.35f,h*.48f,w*.45f,h*1.1f,w,h*.34f); lineTo(w,0f); cubicTo(w*.5f,-h*.1f,w*.6f,h*.7f,0f,h*.76f); close() }
  drawPath(wave,Brush.linearGradient(listOf(baby.copy(alpha=.15f),Color(0xFFFFDDE6))))
  val curve=Path().apply { moveTo(0f,h*.8f); cubicTo(w*.5f,h*.4f,w*.55f,h*1.25f,w,h*.52f) }
  drawPath(curve,pink.copy(alpha=.36f),style=Stroke(1.dp.toPx()))
  val heart=androidx.core.graphics.PathParser.createPathFromPathData(iconPaths.getValue("heart"))!!.asComposePath()
  translate(w*.68f,h*.23f) { rotate(-16f,Offset.Zero) { scale(w*.0025f,w*.0025f,Offset.Zero) { drawPath(heart,pink.copy(alpha=.45f),style=Stroke(1.3f)) } } }
 }
}
@Composable internal fun Heading(title:String,subtitle:String) {
 Box(Modifier.fillMaxWidth()) { HeartDecoration(Modifier.align(Alignment.TopEnd).width(200.dp).height(85.dp)); Column(Modifier.padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) { Text(title,style=MaterialTheme.typography.headlineLarge); Text(subtitle,color=muted,style=MaterialTheme.typography.bodyMedium) } }
}
@Composable internal fun SoftCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Column(modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color.White.copy(alpha=.8f),baby.copy(alpha=.5f))),RoundedCornerShape(20.dp)).border(1.dp,Color.White,RoundedCornerShape(20.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
}
@Composable internal fun Primary(label:String,enabled:Boolean=true,modifier:Modifier=Modifier,onClick:()->Unit) {
 Button(onClick=onClick,enabled=enabled,shape=RoundedCornerShape(15.dp),contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent,contentColor=Color.White,disabledContainerColor=Color.Transparent),modifier=modifier.fillMaxWidth().heightIn(min=50.dp).background(if(enabled) Brush.linearGradient(listOf(pink,roseDeep)) else Brush.linearGradient(listOf(blushBorder,blushBorder)),RoundedCornerShape(15.dp))) { Text(label,fontSize=17.sp,fontWeight=FontWeight.Bold) }
}
@Composable internal fun PrivacyNote(text:String="원본 릴스는 상대에게 직접 공개되지 않아요") {
 Row(Modifier.fillMaxWidth().background(baby.copy(alpha=.75f),RoundedCornerShape(22.dp)).padding(horizontal=12.dp,vertical=9.dp),horizontalArrangement=Arrangement.spacedBy(8.dp,Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically) { Glyph("lock",Modifier.size(18.dp),muted); Text(text,style=MaterialTheme.typography.bodySmall,color=ink) }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun Tags(values:Collection<String>,selected:Set<String>,enabled:Boolean=true,onToggle:(String)->Unit) {
 FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(0.dp)) { values.forEach { tag ->
  FilterChip(selected=tag in selected,enabled=enabled,onClick={onToggle(tag)},label={Text(labels[tag] ?: tag,fontSize=14.sp)},trailingIcon=if(tag in selected) { { Glyph("check",Modifier.size(16.dp),Color.White) } } else null,shape=RoundedCornerShape(25.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=roseDeep,selectedLabelColor=Color.White),border=FilterChipDefaults.filterChipBorder(enabled,tag in selected,borderColor=blushBorder,selectedBorderColor=Color.Transparent))
 } }
}
@Composable internal fun MenuRow(symbol:String,title:String,onClick:()->Unit) {
 val icon=when(symbol){"⚙"->"settings";else->symbol}
 Row(Modifier.fillMaxWidth().clickable(onClick=onClick).heightIn(min=56.dp).padding(5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) { Box(Modifier.size(38.dp).background(baby,CircleShape),contentAlignment=Alignment.Center) { Glyph(icon,Modifier.size(22.dp),roseDeep) }; Text(title,Modifier.weight(1f),fontWeight=FontWeight.SemiBold); Glyph("chevron",Modifier.size(18.dp),muted) }
}
@Composable internal fun CoupleNavigation(page:String,onPage:(String)->Unit) {
 Row(Modifier.fillMaxWidth().background(cream,RoundedCornerShape(topStart=20.dp,topEnd=20.dp)).navigationBarsPadding().padding(top=5.dp,bottom=3.dp)) {
  listOf("발견" to "home","취향" to "heart","보관함" to "bookmark","우리" to "people").forEach { (title,icon) ->
   val active=page==title
   Column(Modifier.weight(1f).selectable(active,role=Role.Tab,onClick={onPage(title)}).testTag("tab-$title").padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) { Glyph(icon,Modifier.size(25.dp),if(active) pink else ink,active && icon!="people"); Text(title,color=if(active) roseDeep else ink,fontSize=12.sp,fontWeight=if(active) FontWeight.Bold else FontWeight.Normal) }
  }
 }
}

