package at.pardus.android.webview.gm.util;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import java.util.UUID;

import at.pardus.android.webview.gm.R;
import at.pardus.android.webview.gm.model.Script;
import at.pardus.android.webview.gm.model.ScriptRequire;
import at.pardus.android.webview.gm.util.ResourceHelper;
import at.pardus.android.webview.gm.util.ScriptInfo;

public class ScriptJsCode {
  protected static final boolean useES6 = (Build.VERSION.SDK_INT >= 21);  // use ES5 in Android <= 4.4 because WebView is outdated and cannot be updated

  private static final String GLOBAL_JS_OBJECT = "unsafeWindow.wrappedJSObject";

  private static String GM_API_LEGACY_MISSING = "";
  private static String GM_API_LEGACY         = "";
  private static String GM_API_V4_POLYFILL    = "";

  private static String JS_CLOSURE_1 = "";  // from: start of closure.     => to: start of GM API.
  private static String JS_CLOSURE_2 = "";  // from: end of GM API.        => to: start of userscript.
  private static String JS_CLOSURE_3 = "";  // from: end of userscript.    => to: start of dynamic input.
  private static String JS_CLOSURE_4 = "";  // from: end of dynamic input. => to: end of closure.

  public static void initStaticResources(Context context) {
    if (TextUtils.isEmpty(GM_API_LEGACY_MISSING)) {
      try {
        GM_API_LEGACY_MISSING = ResourceHelper.getRawStringResource(context, R.raw.gm_api_legacy_missing);
      }
      catch(Exception e) {}
    }
    if (TextUtils.isEmpty(GM_API_LEGACY)) {
      try {
        GM_API_LEGACY = ResourceHelper.getRawStringResource(context, R.raw.gm_api_legacy);
      }
      catch(Exception e) {}
    }
    if (TextUtils.isEmpty(GM_API_V4_POLYFILL)) {
      try {
        if (useES6)
          GM_API_V4_POLYFILL = ResourceHelper.getRawStringResource(context, R.raw.gm_api_v4_polyfill);
      }
      catch(Exception e) {}
    }

    if (TextUtils.isEmpty(JS_CLOSURE_1)) {
      try {
        JS_CLOSURE_1 = ResourceHelper.getRawStringResource(context, R.raw.js_closure_1);
      }
      catch(Exception e) {}
    }
    if (TextUtils.isEmpty(JS_CLOSURE_2)) {
      try {
        if (useES6)
          JS_CLOSURE_2 = ResourceHelper.getRawStringResource(context, R.raw.js_closure_2_es6);
        else
          JS_CLOSURE_2 = ResourceHelper.getRawStringResource(context, R.raw.js_closure_2_es5);
      }
      catch(Exception e) {}
    }
    if (TextUtils.isEmpty(JS_CLOSURE_3)) {
      try {
        JS_CLOSURE_3 = ResourceHelper.getRawStringResource(context, R.raw.js_closure_3);
      }
      catch(Exception e) {}
    }
    if (TextUtils.isEmpty(JS_CLOSURE_4)) {
      try {
        JS_CLOSURE_4 = ResourceHelper.getRawStringResource(context, R.raw.js_closure_4);
      }
      catch(Exception e) {}
    }

    ScriptInfo.initStaticResources(context);
  }

  public ScriptJsCode() {
  }

  public String getJsCode(Script script, boolean pageFinished, String jsBeforeScript, String jsAfterScript, String jsBridgeName, String secret) {
    boolean runNow = (
        (!pageFinished && Script.RUNATSTART.equals(script.getRunAt()))
     || (pageFinished && (script.getRunAt() == null || Script.RUNATEND.equals(script.getRunAt())))
    );

    return (!runNow)
      ? ""
      : script.useJsClosure()
        ? getJsCodeWithClosure(script, jsBeforeScript, jsAfterScript, jsBridgeName, secret)
        : getJsCodeNoClosure  (script, jsBeforeScript, jsAfterScript);
  }

  private String getJsCodeNoClosure(Script script, String jsBeforeScript, String jsAfterScript) {
    return getJsUserscript(script, jsBeforeScript, jsAfterScript);
  }

  private String getJsCodeWithClosure(Script script, String jsBeforeScript, String jsAfterScript, String jsBridgeName, String secret) {
    StringBuilder sb = new StringBuilder(4 * 1024);
    sb.append(JS_CLOSURE_1);
    sb.append(getJsApi(script, jsBridgeName, secret));
    sb.append(JS_CLOSURE_2);
    sb.append(getJsUserscript(script, jsBeforeScript, jsAfterScript));
    sb.append(JS_CLOSURE_3);
    sb.append(script.useJsSandbox() ? "true" : "false");
    sb.append(JS_CLOSURE_4);
    return sb.toString();
  }

  protected String getJsApi(Script script, String jsBridgeName, String secret) {
    StringBuilder sb;

    // defaultSignature
    sb = new StringBuilder(1 * 1024);
    sb.append("\"");
    sb.append(script.getName().replace("\"", "\\\""));
    sb.append("\", \"");
    sb.append(script.getNamespace().replace("\"", "\\\""));
    sb.append("\", \"");
    sb.append(secret);
    sb.append("\"");
    String defaultSignature = sb.toString();
    sb = null;

    // callbackPrefix
    sb = new StringBuilder(1 * 1024);
    sb.append("GM_");
    sb.append(script.getName());
    sb.append(script.getNamespace());
    sb.append(UUID.randomUUID().toString());
    String callbackPrefix = sb.toString().replaceAll("[^0-9a-zA-Z_]", "");
    sb = null;

    // jsApi
    sb = new StringBuilder(4 * 1024);
    sb.append(GLOBAL_JS_OBJECT + " = " + GLOBAL_JS_OBJECT + " || {};\n");
    sb.append(GM_API_LEGACY_MISSING);
    sb.append(GM_API_LEGACY);

    sb.append(GLOBAL_JS_OBJECT + ".");
    sb.append("_GM_formatXmlHttpResponse = _GM_formatXmlHttpResponse;\n");

    // -------------------------
    // Greasemonkey API (legacy)
    // -------------------------

    sb.append("var GM_info = ");
    sb.append(ScriptInfo.toJSONString(script));
    sb.append(";");
    sb.append("\n");

    sb.append("var GM_listValues = function() { return ");
    sb.append(jsBridgeName);
    sb.append(".listValues(");
    sb.append(defaultSignature);
    sb.append(").split(\",\"); };");
    sb.append("\n");

    sb.append("var GM_getValue = function(name, defaultValue) { ");
    sb.append("if (defaultValue === undefined) {defaultValue = null;} ");
    sb.append("defaultValue = JSON.stringify(defaultValue); ");
    sb.append("return JSON.parse(");
    sb.append(jsBridgeName);
    sb.append(".getValue(");
    sb.append(defaultSignature);
    sb.append(", name, defaultValue)");
    sb.append("); };");
    sb.append("\n");

    sb.append("var GM_setValue = function(name, value) { ");
    sb.append("if (value === undefined) {value = null;} ");
    sb.append("value = JSON.stringify(value); ");
    sb.append(jsBridgeName);
    sb.append(".setValue(");
    sb.append(defaultSignature);
    sb.append(", name, value); };");
    sb.append("\n");

    sb.append("var GM_deleteValue = function(name) { ");
    sb.append(jsBridgeName);
    sb.append(".deleteValue(");
    sb.append(defaultSignature);
    sb.append(", name); };");
    sb.append("\n");

    sb.append("var GM_log = function(message) { ");
    sb.append(jsBridgeName);
    sb.append(".log(");
    sb.append(defaultSignature);
    sb.append(", message); };");
    sb.append("\n");

    sb.append("var GM_getResourceURL = function(resourceName) { return ");
    sb.append(jsBridgeName);
    sb.append(".getResourceURL(");
    sb.append(defaultSignature);
    sb.append(", resourceName); };");
    sb.append("\n");

    sb.append("var GM_getResourceText = function(resourceName) { return ");
    sb.append(jsBridgeName);
    sb.append(".getResourceText(");
    sb.append(defaultSignature);
    sb.append(", resourceName); };");
    sb.append("\n");

    sb.append("var GM_xmlhttpRequest = function(details) { \n");
    // onabort
    sb.append("if (details.onabort) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_onAbortCallback = details.onabort;\n");
    sb.append("details.onabort = '");
    sb.append(callbackPrefix);
    sb.append("GM_onAbortCallback'; }\n");
    // onerror
    sb.append("if (details.onerror) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_onErrorCallback = details.onerror;\n");
    sb.append("details.onerror = '");
    sb.append(callbackPrefix);
    sb.append("GM_onErrorCallback'; }\n");
    // onload
    sb.append("if (details.onload) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_onLoadCallback = details.onload;\n");
    sb.append("details.onload = '");
    sb.append(callbackPrefix);
    sb.append("GM_onLoadCallback'; }\n");
    // onprogress
    sb.append("if (details.onprogress) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_onProgressCallback = details.onprogress;\n");
    sb.append("details.onprogress = '");
    sb.append(callbackPrefix);
    sb.append("GM_onProgressCallback'; }\n");
    // onreadystatechange
    sb.append("if (details.onreadystatechange) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_onReadyStateChange = details.onreadystatechange;\n");
    sb.append("details.onreadystatechange = '");
    sb.append(callbackPrefix);
    sb.append("GM_onReadyStateChange'; }\n");
    // ontimeout
    sb.append("if (details.ontimeout) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_onTimeoutCallback = details.ontimeout;\n");
    sb.append("details.ontimeout = '");
    sb.append(callbackPrefix);
    sb.append("GM_onTimeoutCallback'; }\n");
    // upload
    sb.append("if (details.upload) {\n");
    // upload.onabort
    sb.append("if (details.upload.onabort) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_uploadOnAbortCallback = details.upload.onabort;\n");
    sb.append("details.upload.onabort = '");
    sb.append(callbackPrefix);
    sb.append("GM_uploadOnAbortCallback'; }\n");
    // upload.onerror
    sb.append("if (details.upload.onerror) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_uploadOnErrorCallback = details.upload.onerror;\n");
    sb.append("details.upload.onerror = '");
    sb.append(callbackPrefix);
    sb.append("GM_uploadOnErrorCallback'; }\n");
    // upload.onload
    sb.append("if (details.upload.onload) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_uploadOnLoadCallback = details.upload.onload;\n");
    sb.append("details.upload.onload = '");
    sb.append(callbackPrefix);
    sb.append("GM_uploadOnLoadCallback'; }\n");
    // upload.onprogress
    sb.append("if (details.upload.onprogress) { " + GLOBAL_JS_OBJECT + ".");
    sb.append(callbackPrefix);
    sb.append("GM_uploadOnProgressCallback = details.upload.onprogress;\n");
    sb.append("details.upload.onprogress = '");
    sb.append(callbackPrefix);
    sb.append("GM_uploadOnProgressCallback'; }\n");
    // upload
    sb.append("}\n");
    // response value: WebViewXmlHttpResponse.toJSONString()
    sb.append("var response = JSON.parse(");
    sb.append(jsBridgeName);
    sb.append(".xmlHttpRequest(");
    sb.append(defaultSignature);
    sb.append(", JSON.stringify(details)));");
    // formatted response value
    sb.append("return _GM_formatXmlHttpResponse(details, response);");
    sb.append("};");
    sb.append("\n");

    sb.append("var GM_cookie = {};");
    sb.append("\n");

    sb.append("GM_cookie.list = function(details, callback) {");
    sb.append(  "if (typeof details === 'function') {callback = details; details = {};}");
    sb.append(  "if (!details || (typeof details !== 'object')) details = {};");
    sb.append(  "if (typeof callback !== 'function') return;");
    sb.append(  "var url, cookies;");
    sb.append(  "url = details.url ? details.url : details.domain ? ('https://' + details.domain) : unsafeWindow.location.href;");
    sb.append(  "cookies = JSON.parse(");
    sb.append(    jsBridgeName);
    sb.append(    ".listCookies(");
    sb.append(      defaultSignature);
    sb.append(      ", url");
    sb.append(    ")");
    sb.append(  ");");
    sb.append(  "if (details.name) {");
    sb.append(    "cookies = cookies.filter(function(cookie){return (cookie.name === details.name);});"); // note: cookie names ARE case-sensitive
    sb.append(  "}");
    sb.append(  "if (details.decode) {");
    sb.append(    "for (var i=0; i < cookies.length; i++) {");
    sb.append(      "cookies[i].value = unsafeWindow.decodeURIComponent(cookies[i].value);");
    sb.append(    "}");
    sb.append(  "}");
    sb.append(  "callback(cookies, null);");
    sb.append("};");
    sb.append("\n");

    sb.append("GM_cookie.set = function(details, callback) {");
    sb.append(  "var url, value, maxAge;");
    sb.append(  "if (details && (typeof details === 'object') && details.name && details.value) {");
    sb.append(    "url = details.url ? details.url : details.domain ? ('https://' + details.domain) : unsafeWindow.location.href;");
    sb.append(    "value = details.encode ? unsafeWindow.encodeURIComponent(details.value) : details.value;");
    sb.append(    "if (details.expirationDate && (typeof details.expirationDate === 'number')) {");
    sb.append(      "maxAge = (new Date(details.expirationDate * 1000)) - (new Date());");
    sb.append(      "maxAge = Math.floor(maxAge / 1000);");
    sb.append(      "if (maxAge < 0) maxAge = 0;");
    sb.append(    "}");
    sb.append(    "else {");
    sb.append(      "maxAge = -1;");
    sb.append(    "}");
    sb.append(    jsBridgeName);
    sb.append(    ".setCookie(");
    sb.append(      defaultSignature);
    sb.append(      ", url, details.name, value, !!details.secure, !!details.httpOnly, maxAge");
    sb.append(    ");");
    sb.append(  "}");
    sb.append(  "if (typeof callback === 'function') {");
    sb.append(    "callback();");
    sb.append(  "}");
    sb.append("};");
    sb.append("\n");

    sb.append("GM_cookie.delete = function(details, callback) {");
    sb.append(  "var url;");
    sb.append(  "if (details && (typeof details === 'object') && details.name) {");
    sb.append(    "url = details.url ? details.url : details.domain ? ('https://' + details.domain) : unsafeWindow.location.href;");
    sb.append(    jsBridgeName);
    sb.append(    ".deleteCookie(");
    sb.append(      defaultSignature);
    sb.append(      ", url, details.name");
    sb.append(    ");");
    sb.append(  "}");
    sb.append(  "if (typeof callback === 'function') {");
    sb.append(    "callback();");
    sb.append(  "}");
    sb.append("};");
    sb.append("\n");

    sb.append(GM_API_V4_POLYFILL);

    return sb.toString();
  }

  protected String getJsUserscript(Script script, String jsBeforeScript, String jsAfterScript) {
    StringBuilder sb = new StringBuilder(4 * 1024);

    // Get @require'd scripts to inject for this script.
    ScriptRequire[] requires = script.getRequires();
    if (requires != null) {
      for (ScriptRequire currentRequire : requires) {
        sb.append(currentRequire.getContent());
        sb.append("\n");
      }
    }

    if (!TextUtils.isEmpty(jsBeforeScript))
      sb.append(jsBeforeScript);

    sb.append(script.getContent());

    if (!TextUtils.isEmpty(jsAfterScript))
      sb.append(jsAfterScript);

    return sb.toString();
  }

}
