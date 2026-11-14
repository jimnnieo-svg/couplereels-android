package com.couplereels.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable internal fun DemoDiscovery(tags: Set<String>, modifier: Modifier = Modifier, onShare: () -> Unit, onEditTaste: () -> Unit) {
 val ranked = remember(tags) { DemoRecommendations.rank(tags) }
 val pager = rememberPagerState { ranked.size }
 LaunchedEffect(tags) { pager.scrollToPage(0) }
 Column(modifier.fillMaxWidth()) {
  Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Text("내 취향 추천", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
   Text("${pager.currentPage + 1} / ${ranked.size} · 위로 넘기기", style = MaterialTheme.typography.labelSmall, color = muted)
  }
  VerticalPager(state = pager, key = { ranked[it].content.id }, modifier = Modifier.weight(1f).testTag("discover-pager")) { index ->
   val item = ranked[index]
   Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Surface(color = Color(0xFF722F44), contentColor = cream, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().testTag("demo-card-${item.content.id}")) {
     Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF722F44), Color(0xFF40212F)))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text("데모 콘텐츠", color = pink, style = MaterialTheme.typography.labelLarge)
      Text(item.content.symbol, color = pink, style = MaterialTheme.typography.displayLarge)
      Text(item.content.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
      Text(item.content.description, style = MaterialTheme.typography.bodyMedium)
      Text("취향 추천을 설명하는 예시 · 실제 영상 아님", color = Color(0xFFF0DCC9), style = MaterialTheme.typography.labelSmall)
     }
    }
    Text(if (item.matchingTags.isEmpty()) "새로운 취향도 만나보세요" else "내 취향과 맞는 이유", fontWeight = FontWeight.Bold)
    Text(if (item.matchingTags.isEmpty()) "선택한 태그와 겹치는 항목이 없는 탐색 예시예요." else item.matchingTags.joinToString(" · ") { labels[it] ?: it }, modifier = Modifier.testTag("recommendation-reason"), color = muted)
    Primary("릴스 링크 담기", modifier = Modifier.testTag("discover-add-link"), onClick = onShare)
    TextButton(onClick = onEditTaste, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("내 취향 바꾸기") }
   }
  }
 }
}
