// ==UserScript==
// @name         test: GM_download (10 MB, ArrayBuffer)
// @namespace    WebViewWM
// @match        *://*/*
// @require      https://cdn.jsdelivr.net/npm/@warren-bank/browser-fetch-progress@latest/src/fetch-progress.js
// @grant        GM_download
// @run-at       document-end
// ==/UserScript==

// https://sabnzbd.org/
// https://github.com/sabnzbd/sabnzbd/blob/develop/tests/test_internetspeed.py
// https://github.com/sabnzbd/sabnzbd/blob/develop/sabnzbd/internetspeed.py
//   Access-Control-Allow-Origin: *

var progress_meter;

var progress_event_handler = function(event) {
  progress_meter.value = event.progress;
};

var init_dom = function(title) {
  while(document.body.childNodes.length) {
    document.body.removeChild(document.body.childNodes[0]);
  }

  add_notification(title);

  progress_meter = document.createElement('progress');
  progress_meter.max   = 1.0;
  progress_meter.value = 0.0;
  document.body.appendChild(progress_meter);
}

var add_notification = function(text) {
  var h4 = document.createElement('h4');
  h4.innerText = text;
  document.body.appendChild(h4);
}

var run_download_test = async function() {
  var url  = 'https://sabnzbd.org/tests/internetspeed/10MB.bin'
  var name = 'sabnzbd.bin'

  init_dom('downloading: ' + url.substring(0, 100))

  var buffer = await fetch(url)
    .then(window.fetchProgress(progress_event_handler))
    .then(res => res.arrayBuffer())

  add_notification('saving: ' + buffer.byteLength + ' bytes')

  GM_download(buffer, name)
}

run_download_test()
