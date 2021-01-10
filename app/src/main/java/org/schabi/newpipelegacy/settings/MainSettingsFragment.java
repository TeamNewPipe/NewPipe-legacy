package org.schabi.newpipelegacy.settings;

import android.os.Bundle;

import androidx.preference.Preference;

import org.schabi.newpipelegacy.App;
import org.schabi.newpipelegacy.CheckForNewAppVersion;
import org.schabi.newpipelegacy.MainActivity;
import org.schabi.newpipelegacy.R;

public class MainSettingsFragment extends BasePreferenceFragment {
    public static final boolean DEBUG = MainActivity.DEBUG;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.main_settings);

        if (!CheckForNewAppVersion.isGithubApk(App.getApp())) {
            final Preference update = findPreference(getString(R.string.update_pref_screen_key));
            getPreferenceScreen().removePreference(update);

            defaultPreferences.edit().putBoolean(getString(R.string.update_app_key), false).apply();
        }
    }
}
