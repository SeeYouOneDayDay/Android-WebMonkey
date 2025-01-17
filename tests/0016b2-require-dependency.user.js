// ==UserScript==
// @name         test: @require
// @description  test: Promise.defer() with an external dependency.
// @namespace    WebViewWM
// @match        *://*/*
// @require      https://github.com/warren-bank/ES2015-wishlist/raw/master/Promise.defer/Promise.defer.js
// @run-at       document-start
// ==/UserScript==

var run_test = async function(val) {
  try {
    var defer = Promise.defer();

    setTimeout(function(){defer.resolve(val * 10);}, 1000)

    var result = await defer.promise;

    unsafeWindow.alert(`Promise.defer: ${val} * 10 = ${result}`);
  }
  catch(error) {
    unsafeWindow.alert(`Promise.defer error: ${error.message}`);
  }
};

var run_tests = async function() {
  await run_test(5);
  await run_test(15);
}

run_tests();
