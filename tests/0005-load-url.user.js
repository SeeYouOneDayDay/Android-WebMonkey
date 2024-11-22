// ==UserScript==
// @name         test: GM_loadUrl
// @namespace    WebViewWM
// @match        *://*/*
// @grant        GM_loadUrl
// @run-at       document-start
// ==/UserScript==

// ==========================================
// https://httpbin.org/#/operations/Request%20inspection/get_headers
// ==========================================

if (unsafeWindow.location.hostname !== "httpbin.org")
  GM_loadUrl(/* url= */ "https://httpbin.org/headers", /* headers: */ "Refer", "https://WebMonkey.com/", "User-Agent", "WebMonkey", "X-Requested-With", "WebMonkey");
