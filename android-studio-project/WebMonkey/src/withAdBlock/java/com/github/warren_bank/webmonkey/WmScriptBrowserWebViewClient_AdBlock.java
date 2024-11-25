package com.github.warren_bank.webmonkey;

import com.github.warren_bank.webmonkey.WmScriptBrowserWebViewClient_Base;
import com.github.warren_bank.webmonkey.settings.AdBlockSettingsUtils;
import com.github.warren_bank.webmonkey.settings.SettingsUtils;
import com.github.warren_bank.webmonkey.util.AdBlockListHelper;

import at.pardus.android.webview.gm.run.WebViewGm;
import at.pardus.android.webview.gm.store.ScriptStore;
import at.pardus.android.webview.gm.store.ui.ScriptBrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public class WmScriptBrowserWebViewClient_AdBlock extends WmScriptBrowserWebViewClient_Base {
  private boolean isEnabled;
  private boolean isPopulatingHosts;
  private boolean shouldRepopulateHosts;
  private TreeMap<String, Object> blockedHosts;
  private SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener;

  public WmScriptBrowserWebViewClient_AdBlock(Context context, WebViewGm webView) throws Exception {
    this(
      context,
      (ScriptBrowser.ScriptBrowserWebViewClientGm) webView.getWebViewClient()
    );
  }

  public WmScriptBrowserWebViewClient_AdBlock(Context context, ScriptBrowser.ScriptBrowserWebViewClientGm webViewClient) {
    this(
      context,
      webViewClient.getScriptStore(),
      webViewClient.getJsBridgeName(),
      webViewClient.getSecret(),
      webViewClient.getScriptBrowser()
    );
  }

  public WmScriptBrowserWebViewClient_AdBlock(Context context, ScriptStore scriptStore, String jsBridgeName, String secret, ScriptBrowser scriptBrowser) {
    super(context, scriptStore, jsBridgeName, secret, scriptBrowser);

    updateIsEnabled(context);
    updateBlockedHosts(context);
    addPreferenceChangeListener(context);
  }

  @Override
  public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
    return shouldBlockRequest(url);
  }

  @Override
  public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
    String url = request.getUrl().toString();
    return shouldBlockRequest(url);
  }

  private void updateIsEnabled(Context context) {
    isEnabled = AdBlockSettingsUtils.getEnableAdBlockPreference(context);
  }

  private void updateBlockedHosts(Context context) {
    if (isPopulatingHosts) {
      shouldRepopulateHosts = true;
      return;
    }

    if (isEnabled) {
      // don't perform networking on main thread
      new Thread(new Runnable(){
        @Override
        public void run(){
          isPopulatingHosts = true;
          populateBlockedHosts(context);
          isPopulatingHosts = false;
        }
      }).start();
    }
    else {
      blockedHosts = null;
    }
  }

  private void populateBlockedHosts(Context context) {
    try {
      InputStream is = AdBlockListHelper.open(context);
      if (is == null) throw new Exception();

      BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      String line;

      blockedHosts = new TreeMap<String, Object>();

      while (isEnabled && !shouldRepopulateHosts && ((line = br.readLine()) != null)) {
        line = line.toLowerCase().trim();

        if (!line.isEmpty() && !line.startsWith("#")) {
          blockedHosts.put(line, null);
        }
      }
    }
    catch (Exception e) {
      blockedHosts = null;
    }

    if (!isEnabled) {
      blockedHosts = null;
      return;
    }

    if (shouldRepopulateHosts) {
      shouldRepopulateHosts = false;
      populateBlockedHosts(context);
      return;
    }
  }

  private void addPreferenceChangeListener(Context context) {
    // https://stackoverflow.com/a/3104265
    prefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
      public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
        if (key.equals(AdBlockSettingsUtils.getEnableAdBlockPreferenceKey(context))) {
          updateIsEnabled(context);
          updateBlockedHosts(context);
          return;
        }
        if (key.equals(AdBlockSettingsUtils.getCustomAdBlockListUrlKey(context))) {
          AdBlockListHelper.delete(context);
          updateBlockedHosts(context);
          return;
        }
        if (key.equals(AdBlockSettingsUtils.getCustomAdBlockListUpdateIntervalDaysKey(context))) {
          updateBlockedHosts(context);
          return;
        }
      }
    };

    SharedPreferences prefs = SettingsUtils.getPrefs(context);
    prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);
  }

  private boolean isHostBlocked(String url) {
    if (!isEnabled || isPopulatingHosts || (blockedHosts == null)) return false;

    try {
      Uri uri = Uri.parse(url);
      String host = uri.getHost().toLowerCase().trim();

      List<String> domains = Arrays.asList(host.split("\\."));
      String domain;

      while (domains.size() > 1) {
        domain = TextUtils.join(".", domains);
        if (blockedHosts.containsKey(domain)) {
          return true;
        }
        domains = domains.subList(1, domains.size());
      }
    }
    catch(Exception e) {}
    return false;
  }

  private WebResourceResponse shouldBlockRequest(String url) {
    if (isHostBlocked(url)) {
      ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
      return new WebResourceResponse("text/plain", "utf-8", EMPTY);
    }
    else {
      return null;
    }
  }
}
