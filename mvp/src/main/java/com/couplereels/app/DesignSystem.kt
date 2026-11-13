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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val burgundyBackground = Color(0xFF2C0D1B)
internal val burgundySurface = Color(0xFF511C32)
internal val goldAccent = Color(0xFFE6C788)
internal val ivoryText = Color(0xFFFFF4E2)
internal val muted = Color(0xFFD1BFB5)
internal val labels = linkedMapOf("jewelry" to "주얼리", "fashion" to "패션", "perfume" to "향수", "flowers" to "꽃", "food" to "맛집", "travel" to "여행", "tech" to "전자기기", "hobby" to "취미", "silver" to "실버", "gold" to "골드", "minimal" to "심플", "bold" to "화려함", "necklace" to "목걸이", "gift" to "선물", "pendant" to "펜던트", "daily" to "데일리", "925" to "925 실버", "budget" to "가성비", "anniversary" to "기념일", "date" to "데이트")
internal val interests = labels.keys.take(8)
internal val styles = listOf("silver", "gold", "minimal", "bold", "daily", "budget")
internal val gifts = listOf("necklace", "flowers", "perfume", "anniversary", "gift")

@Composable internal fun CoupleTheme(content: @Composable () -> Unit) {
 MaterialTheme(colorScheme = darkColorScheme(primary = ivoryText, onPrimary = burgundyBackground, primaryContainer = burgundySurface, onPrimaryContainer = ivoryText, background = burgundyBackground, onBackground = ivoryText, surface = burgundyBackground, onSurface = ivoryText, secondaryContainer = burgundySurface, onSecondaryContainer = ivoryText, outline = Color(0xFF926A52)), shapes = Shapes(small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(30.dp)), content = content)
}
@Composable internal fun Brand() { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("◯◯", color = goldAccent, style = MaterialTheme.typography.headlineMedium); Text("CoupleReels", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) } }
@Composable internal fun Heading(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text(subtitle, color = muted) } }
@Composable internal fun SoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
 Column(modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF481528), burgundySurface.copy(alpha = .65f))), RoundedCornerShape(26.dp)).border(1.dp, Color(0xFF805338), RoundedCornerShape(26.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable internal fun Primary(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) { Button(onClick = onClick, enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = goldAccent, contentColor = burgundyBackground), modifier = modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(label, fontWeight = FontWeight.Bold) } }
@Composable internal fun PrivacyNote() { Surface(color = burgundySurface, shape = RoundedCornerShape(18.dp)) { Text("태그와 원본 해시를 연인에게 직접 보내지 않아요.\n연인에게는 계산된 추천 후보 링크만 전달해요.", Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall) } }
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun Tags(values: Collection<String>, selected: Set<String>, enabled: Boolean = true, testTagPrefix: String = "tag", onToggle: (String) -> Unit) {
 FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { values.forEach { tag -> FilterChip(selected = tag in selected, enabled = enabled, onClick = { onToggle(tag) }, label = { Text(labels[tag] ?: tag) }, modifier = Modifier.heightIn(min = 48.dp).testTag("$testTagPrefix-$tag"), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = goldAccent, selectedLabelColor = burgundyBackground)) } }
}
@Composable internal fun MenuRow(symbol: String, title: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 60.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Text(symbol, style = MaterialTheme.typography.headlineSmall); Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("›") } }

@Composable internal fun DarkSheetSystemBars() {
 val view = androidx.compose.ui.platform.LocalView.current
 androidx.compose.runtime.SideEffect {
  val provider = (view.parent as? androidx.compose.ui.window.DialogWindowProvider) ?: (view as? androidx.compose.ui.window.DialogWindowProvider)
  provider?.window?.let { window ->
   androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
    isAppearanceLightStatusBars = false
    isAppearanceLightNavigationBars = false
   }
  }
 }
}
