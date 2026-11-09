package com.couplereels.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.http.SslError
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private var shared by mutableStateOf("")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receive(intent)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFF9EBBA), background = Color(0xFF270005), surface = Color(0xFF270005))) { Surface(Modifier.fillMaxSize()) { Probe(shared) } } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); receive(intent) }
    private fun receive(i: Intent) { if (i.action == Intent.ACTION_SEND && i.type == "text/plain") shared = ReelUrl.fromShare(i.getStringExtra(Intent.EXTRA_TEXT).orEmpty()).orEmpty() }

    @Composable private fun Probe(incoming: String) {
        val prefs = remember { getSharedPreferences("probe", MODE_PRIVATE) }
        var input by remember { mutableStateOf(prefs.getString("last", ReelUrl.candidates[0])!!) }
        var url by remember { mutableStateOf(input) }
        var embed by remember { mutableStateOf(false) }
        var reload by remember { mutableIntStateOf(0) }
        var status by remember { mutableStateOf("재생은 직접 확인해야 합니다. 페이지 로드 완료는 재생 성공이 아닙니다.") }
        var records by remember { mutableStateOf(prefs.getString("records", "[]")!!) }
        var showLog by remember { mutableStateOf(false) }
        var resultMenu by remember { mutableStateOf(false) }
        var note by remember { mutableStateOf("") }
        val events = remember { mutableStateListOf<String>() }
        fun event(value: String) { if (events.size >= 100) events.removeAt(0); events.add(value); status = value }
        fun open(value: String) {
            val valid = ReelUrl.normalize(value)
            if (valid == null) { status = "유효한 HTTPS Instagram 개별 Reel URL을 입력하세요."; return }
            input = valid; url = valid; reload++; prefs.edit().putString("last", valid).apply()
        }
        fun record(result: String) {
            val list = JSONArray(records)
            list.put(JSONObject().put("time", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())).put("url", url).put("mode", if (embed) "official-embed" else "webpage").put("result", result).put("note", note).put("device", android.os.Build.MODEL).put("android", android.os.Build.VERSION.RELEASE).put("webview", WebView.getCurrentWebViewPackage()?.versionName ?: "unknown").put("events", JSONArray(events.toList())))
            records = list.toString(2); prefs.edit().putString("records", records).commit(); status = "결과 저장: $result"; resultMenu = false
        }
        LaunchedEffect(incoming) { if (incoming.isNotEmpty()) open(incoming) }
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(12.dp)) {
            Text("CoupleReels · P0 재생 검증", style = MaterialTheme.typography.titleLarge)
            Text("일반 웹 / 공식 임베드 · 후보 10개", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Instagram Reel URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { open(input) }) { Text("열기/재시도") }
                FilterChip(selected = embed, onClick = { embed = !embed; reload++ }, label = { Text(if (embed) "공식 임베드" else "일반 웹") })
            }
            Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.heightIn(max = 55.dp).verticalScroll(rememberScrollState()))
            if (showLog) {
                Text(records, modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall)
            } else {
                key(url, embed, reload) { ReelWebView(url, embed, Modifier.weight(1f).fillMaxWidth(), ::event) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val index = ReelUrl.candidates.indexOf(url)
                Button(onClick = { open(ReelUrl.candidates[(if (index < 0) 0 else index + 9) % 10]) }) { Text("이전") }
                Text("${if (index < 0) "직접 입력" else "${index + 1}/10"}", Modifier.padding(top = 12.dp))
                Button(onClick = { open(ReelUrl.candidates[(index + 1).coerceAtLeast(0) % 10]) }) { Text("다음") }
                TextButton(onClick = { resultMenu = true }) { Text("결과 기록") }
            }
            Row {
                TextButton(onClick = { showLog = !showLog }) { Text(if (showLog) "플레이어" else "기록 보기") }
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_TEXT, records) }
                    startActivity(Intent.createChooser(send, "검증 기록 내보내기"))
                }) { Text("기록 내보내기") }
            }
        }
        if (resultMenu) AlertDialog(onDismissRequest = { resultMenu = false }, title = { Text("직접 관찰한 결과만 기록") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("재생/소리/로그인/전환 관찰 메모") })
                listOf("PASS: 영상 재생 확인", "FAIL: 로그인 요구", "FAIL: 영상 재생 실패", "FAIL: 다음 영상 전환", "FAIL: 외부 앱 이동", "BLOCKED: 네트워크", "미확인").forEach { label -> TextButton(onClick = { record(label) }) { Text(label) } }
            }
        }, confirmButton = { TextButton(onClick = { resultMenu = false }) { Text("닫기") } })
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ReelWebView(url: String, embed: Boolean, modifier: Modifier, event: (String) -> Unit) {
    val owner = LocalLifecycleOwner.current
    var web by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, value -> when (value) { Lifecycle.Event.ON_PAUSE -> web?.onPause(); Lifecycle.Event.ON_RESUME -> web?.onResume(); else -> Unit } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    AndroidView(modifier = modifier, factory = { context ->
        WebView(context).apply {
            web = this
            settings.apply {
                javaScriptEnabled = true; domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = true
                allowFileAccess = false; allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                setSupportMultipleWindows(false); javaScriptCanOpenWindowsAutomatically = false
                safeBrowsingEnabled = true
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) { request.deny(); event("카메라/마이크 등 웹 권한 요청 거부") }
                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean { event("새 창 요청 차단"); return false }
            }
            setDownloadListener { _, _, _, _, _ -> event("다운로드 차단") }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val link = request.url.toString()
                    if (!ReelUrl.allowedNavigation(link)) { event("외부 이동 차단: ${request.url.scheme} / ${request.url.host.orEmpty()}"); return true }
                    if (link.contains("/accounts/login")) event("Instagram 로그인 요구 감지 · 인증 우회 없음")
                    return false
                }
                override fun onPageFinished(view: WebView, loaded: String) { event("페이지 로드 완료 · 영상 재생은 직접 확인 필요") }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) { if (request.isForMainFrame) event("인터넷/페이지 오류 ${error.errorCode} · 연결 확인 후 재시도") }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) { if (request.isForMainFrame) event("HTTP ${response.statusCode} · 로그인/콘텐츠 제한 가능") }
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) { handler.cancel(); event("TLS 오류 · 보안 연결 차단") }
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean { view.destroy(); web = null; event("WebView 종료 · 열기/재시도로 복구하세요"); return true }
            }
            if (embed) {
                // Instagram's official blockquote + embed.js. No scraping or injected JS bridge.
                loadDataWithBaseURL("https://www.instagram.com/", """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head><body style="margin:0"><blockquote class="instagram-media" data-instgrm-permalink="$url" data-instgrm-version="14"><a href="$url">Instagram Reel</a></blockquote><script async src="https://www.instagram.com/embed.js"></script></body></html>""", "text/html", "UTF-8", null)
            } else loadUrl(url)
        }
    }, onRelease = { view -> web = null; view.stopLoading(); view.onPause(); view.removeAllViews(); view.destroy() })
}
