package com.github.warren_bank.webmonkey.settings;

import com.github.warren_bank.webmonkey.R;

public class SettingsFragment extends SettingsFragment_Base {

  protected void addPreferences() {
    super.addPreferences();

    addPreferencesFromResource(R.xml.addblock_preferences);
  }

}
