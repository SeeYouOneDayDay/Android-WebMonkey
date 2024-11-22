// ==UserScript==
// @name         test: @require
// @description  test: Vue.js with sandbox, and explicit injection of Vue object to both global and sandbox namespaces
// @namespace    WebViewWM
// @match        *://*/*
// @require      https://unpkg.com/vue@3.3.4/dist/vue.global.prod.js
// @run-at       document-end
// ==/UserScript==

// https://vuejs.org/guide/quick-start.html#using-vue-from-cdn

unsafeWindow.Vue = Vue;
window.Vue = Vue;

var clean_dom = function() {
  while(document.body.childNodes.length) {
    document.body.removeChild(document.body.childNodes[0]);
  }
}

var init_dom = function() {
  var div = document.createElement('div');

  div.setAttribute('id', 'app');
  div.innerText = '{{ message }}';
  document.body.appendChild(div);
}

var init_vue = function() {
  const { createApp, ref } = Vue;

  createApp({
    setup() {
      const message = ref('Hello, Vue.js')
      return {
        message
      }
    }
  }).mount('#app')
}

var run_test = function() {
  unsafeWindow.alert('typeof unsafeWindow.Vue = ' + (typeof unsafeWindow.Vue));
  unsafeWindow.alert('typeof window.Vue = ' + (typeof window.Vue));
  unsafeWindow.alert('typeof Vue = ' + (typeof Vue));

  clean_dom();
  init_dom();
  init_vue();
};

run_test();
