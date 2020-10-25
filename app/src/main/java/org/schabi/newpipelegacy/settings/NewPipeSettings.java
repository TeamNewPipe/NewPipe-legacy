package org.schabi.newpipelegacy.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import org.schabi.newpipelegacy.R;

import java.io.File;
import java.util.Set;

/*
 * Created by k3b on 07.01.2016.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * NewPipeSettings.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Helper class for global settings.
 */
public final class NewPipeSettings {
    private NewPipeSettings() { }

    public static void initSettings(final Context context) {
        // check if there are entries in the prefs to determine whether this is the first app run
        Boolean isFirstRun = null;
        final Set<String> prefsKeys = PreferenceManager.getDefaultSharedPreferences(context)
                .getAll().keySet();
        for (final String key: prefsKeys) {
            // ACRA stores some info in the prefs during app initialization
            // which happens before this method is called. Therefore ignore ACRA-related keys.
            if (!key.toLowerCase().startsWith("acra")) {
                isFirstRun = false;
                break;
            }
        }
        if (isFirstRun == null) {
            isFirstRun = true;
        }

        PreferenceManager.setDefaultValues(context, R.xml.appearance_settings, true);
        PreferenceManager.setDefaultValues(context, R.xml.content_settings, true);
        PreferenceManager.setDefaultValues(context, R.xml.download_settings, true);
        PreferenceManager.setDefaultValues(context, R.xml.history_settings, true);
        PreferenceManager.setDefaultValues(context, R.xml.main_settings, true);
        PreferenceManager.setDefaultValues(context, R.xml.video_audio_settings, true);
        PreferenceManager.setDefaultValues(context, R.xml.debug_settings, true);

        getVideoDownloadFolder(context);
        getAudioDownloadFolder(context);

        SettingMigrations.initMigrations(context, isFirstRun);
    }

    private static void getVideoDownloadFolder(final Context context) {
        getDir(context, R.string.download_path_video_key, Environment.DIRECTORY_MOVIES);
    }

    private static void getAudioDownloadFolder(final Context context) {
        getDir(context, R.string.download_path_audio_key, Environment.DIRECTORY_MUSIC);
    }

    private static void getDir(final Context context, final int keyID,
                               final String defaultDirectoryName) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = context.getString(keyID);
        final String downloadPath = prefs.getString(key, null);
        if ((downloadPath != null) && (!downloadPath.isEmpty())) {
            return;
        }

        final SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString(key, getNewPipeChildFolderPathForDir(getDir(defaultDirectoryName)));
        spEditor.apply();
    }

    @NonNull
    public static File getDir(final String defaultDirectoryName) {
        return new File(Environment.getExternalStorageDirectory(), defaultDirectoryName);
    }

    private static String getNewPipeChildFolderPathForDir(final File dir) {
        return new File(dir, "NewPipe").toURI().toString();
    }

    public static boolean useStorageAccessFramework(final Context context) {
        final String key = context.getString(R.string.storage_use_saf);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getBoolean(key, false);
    }

}
