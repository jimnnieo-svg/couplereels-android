package com.couplereels.app

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/** An explicit, allowed web embed attempt, never a claimed playback success. */
@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable internal fun ReelPreview(url: String, modifier: Modifier, onExternal: () -> Unit, onCopy: () -> Unit, autoAttempt: Boolean = false) {
 var attempted by remember(url) { mutableStateOf(autoAttempt) }
 var status by remember(url) { mutableStateOf("Instagram의 공개 릴스를 열어볼 수 있어요.") }
 var failed by remember(url) { mutableStateOf(false) }
 var web by remember(url) { mutableStateOf<WebView?>(null) }
 val owner = LocalLifecycleOwner.current
 DisposableEffect(owner, url) {
  val observer = LifecycleEventObserver { _, event -> when (event) { Lifecycle.Event.ON_PAUSE -> web?.onPause(); Lifecycle.Event.ON_RESUME -> web?.onResume(); else -> Unit } }
  owner.lifecycle.addObserver(observer)
  onDispose { owner.lifecycle.removeObserver(observer) }
 }
 LaunchedEffect(attempted, url) { if (attempted) { delay(15000); if (!failed) status = "영상이 보이지 않으면 Instagram에서 열어주세요. 로그인이나 공개 범위 제한이 있을 수 있어요." } }
 Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
  if (attempted) {
   Text(status, style = MaterialTheme.typography.bodySmall)
   key(url) { AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { context -> WebView(context).apply {
    web = this
    settings.apply { javaScriptEnabled = true; domStorageEnabled = true; mediaPlaybackRequiresUserGesture = true; allowFileAccess = false; allowContentAccess = false; mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW; setSupportMultipleWindows(false); javaScriptCanOpenWindowsAutomatically = false; safeBrowsingEnabled = true }
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
    webChromeClient = object : WebChromeClient() { override fun onPermissionRequest(request: PermissionRequest) { request.deny() } }
    setDownloadListener { _, _, _, _, _ -> status = "다운로드는 지원하지 않아요." }
    webViewClient = object : WebViewClient() {
     override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = !ReelUrl.allowedNavigation(request.url.toString())
     override fun onPageFinished(view: WebView, loaded: String) { if (!failed) status = "영상이 보이지 않으면 아래에서 Instagram을 열어주세요." }
     override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) { if (request.isForMainFrame) { failed = true; status = "콘텐츠를 불러오지 못했어요. 인터넷 연결을 확인하거나 Instagram에서 열어주세요." } }
     override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) { if (request.isForMainFrame) { failed = true; status = "Instagram 콘텐츠 제한 (HTTP ${response.statusCode}). 아래에서 원본을 열어주세요." } }
     override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) { handler.cancel(); failed = true; status = "보안 연결 오류로 중단했어요." }
     override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean { web = null; view.destroy(); attempted = false; status = "재생 화면이 종료됐어요. 다시 시도해 주세요."; return true }
    }
    loadDataWithBaseURL("https://www.instagram.com/", """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head><body style="margin:0"><blockquote class="instagram-media" data-instgrm-permalink="$url" data-instgrm-version="14"><a href="$url">Instagram Reel</a></blockquote><script async src="https://www.instagram.com/embed.js"></script></body></html>""", "text/html", "UTF-8", null)
   } }, onRelease = { v -> web = null; v.stopLoading(); v.onPause(); v.removeAllViews(); v.destroy() }) }
  } else SoftCard(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
   Text("Instagram Reel", style = MaterialTheme.typography.headlineSmall)
   Text(status)
   Text(url, style = MaterialTheme.typography.bodySmall)
   Text("로그인·공개 범위·Instagram 정책에 따라 앱 안에서 열리지 않을 수 있어요.", style = MaterialTheme.typography.bodySmall)
   Primary("공식 임베드 열기") { failed = false; status = "콘텐츠를 불러오는 중…"; attempted = true }
  }
  FlowRow { TextButton(onClick = onExternal) { Text("Instagram에서 열기") }; TextButton(onClick = onCopy) { Text("링크 복사") }; if (attempted) TextButton(onClick = { attempted = false }) { Text("닫기") } }
 }
}
