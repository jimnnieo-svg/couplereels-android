package com.couplereels.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val cream = Color(0xFFFFFAF6)
internal val baby = Color(0xFFFFE6EF)
internal val pink = Color(0xFFFF9CBB)
internal val ink = Color(0xFF302E33)
internal val muted = Color(0xFF68616A)
internal val labels = linkedMapOf("jewelry" to "주얼리", "fashion" to "패션", "perfume" to "향수", "flowers" to "꽃", "food" to "맛집", "travel" to "여행", "tech" to "전자기기", "hobby" to "취미", "silver" to "실버", "gold" to "골드", "minimal" to "심플", "bold" to "화려함", "necklace" to "목걸이", "gift" to "선물", "pendant" to "펜던트", "daily" to "데일리", "925" to "925 실버", "budget" to "가성비", "anniversary" to "기념일", "date" to "데이트")
internal val interests = labels.keys.take(8)
internal val styles = listOf("silver", "gold", "minimal", "bold", "daily", "budget")
internal val gifts = listOf("necklace", "flowers", "perfume", "anniversary", "gift")

@Composable internal fun CoupleTheme(content: @Composable () -> Unit) {
 MaterialTheme(colorScheme = lightColorScheme(primary = ink, onPrimary = cream, primaryContainer = baby, onPrimaryContainer = ink, background = cream, onBackground = ink, surface = cream, onSurface = ink, secondaryContainer = baby, onSecondaryContainer = ink, outline = Color(0xFFD8CAD0)), shapes = Shapes(small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(30.dp)), content = content)
}
@Composable internal fun Brand() { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("◯◯", color = Color(0xFFE57FA2), style = MaterialTheme.typography.headlineMedium); Text("CoupleReels", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) } }
@Composable internal fun Heading(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text(subtitle, color = muted) } }
@Composable internal fun SoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
 Column(modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color.White.copy(alpha = .82f), baby.copy(alpha = .65f))), RoundedCornerShape(26.dp)).border(1.dp, Color.White, RoundedCornerShape(26.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable internal fun Primary(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) { Button(onClick = onClick, enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = pink, contentColor = ink), modifier = modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(label, fontWeight = FontWeight.Bold) } }
@Composable internal fun PrivacyNote() { Surface(color = baby, shape = RoundedCornerShape(18.dp)) { Text("원본 링크를 상대에게 직접 공유하지 않아요.\n연결하면 추천에 필요한 취향 태그를 서로 공유해요.", Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall) } }
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun Tags(values: Collection<String>, selected: Set<String>, enabled: Boolean = true, onToggle: (String) -> Unit) {
 FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { values.forEach { tag -> FilterChip(selected = tag in selected, enabled = enabled, onClick = { onToggle(tag) }, label = { Text(labels[tag] ?: tag) }, modifier = Modifier.heightIn(min = 48.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pink, selectedLabelColor = ink)) } }
}
@Composable internal fun MenuRow(symbol: String, title: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 60.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Text(symbol, style = MaterialTheme.typography.headlineSmall); Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("›") } }
