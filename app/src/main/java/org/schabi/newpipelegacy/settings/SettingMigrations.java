package org.schabi.newpipelegacy.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.report.ErrorActivity;
import org.schabi.newpipelegacy.report.ErrorActivity.ErrorInfo;
import org.schabi.newpipelegacy.report.UserAction;

import static org.schabi.newpipelegacy.MainActivity.DEBUG;

public final class SettingMigrations {
    private static final String TAG = SettingMigrations.class.toString();
    /**
     * Version number for preferences. Must be incremented every time a migration is necessary.
     */
    public static final int VERSION = 2;
    private static SharedPreferences sp;

    public static final Migration MIGRATION_0_1 = new Migration(0, 1) {
        @Override
        public void migrate(final Context context) {
            // We changed the content of the dialog which opens when sharing a link to NewPipe
            // by removing the "open detail page" option.
            // Therefore, show the dialog once again to ensure users need to choose again and are
            // aware of the changed dialog.
            final SharedPreferences.Editor editor = sp.edit();
            editor.putString(context.getString(R.string.preferred_open_action_key),
                    context.getString(R.string.always_ask_open_action_key));
            editor.apply();
        }
    };

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        protected void migrate(final Context context) {
            // The new application workflow introduced in #2907 allows minimizing videos
            // while playing to do other stuff within the app.
            // For an even better workflow, we minimize a stream when switching the app to play in
            // background.
            // Therefore, set default value to background, if it has not been changed yet.
            final String minimizeOnExitKey = context.getString(R.string.minimize_on_exit_key);
            if (sp.getString(minimizeOnExitKey, "")
                    .equals(context.getString(R.string.minimize_on_exit_none_key))) {
                final SharedPreferences.Editor editor = sp.edit();
                editor.putString(minimizeOnExitKey,
                        context.getString(R.string.minimize_on_exit_background_key));
                editor.apply();
            }
        }
    };

    /**
     * List of all implemented migrations.
     * <p>
     * <b>Append new migrations to the end of the list</b> to keep it sorted ascending.
     * If not sorted correctly, migrations which depend on each other, may fail.
     */
    private static final Migration[] SETTING_MIGRATIONS = {
            MIGRATION_0_1,
            MIGRATION_1_2
    };


    public static void initMigrations(final Context context, final boolean isFirstRun) {
        // setup migrations and check if there is something to do
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        final String lastPrefVersionKey = context.getString(R.string.last_used_preferences_version);
        final int lastPrefVersion = sp.getInt(lastPrefVersionKey, 0);

        // no migration to run, already up to date
        if (isFirstRun) {
            sp.edit().putInt(lastPrefVersionKey, VERSION).apply();
            return;
        } else if (lastPrefVersion == VERSION) {
            return;
        }

        // run migrations
        int currentVersion = lastPrefVersion;
        for (final Migration currentMigration : SETTING_MIGRATIONS) {
            try {
                if (currentMigration.shouldMigrate(currentVersion)) {
                    if (DEBUG) {
                        Log.d(TAG, "Migrating preferences from version "
                                + currentVersion + " to " + currentMigration.newVersion);
                    }
                    currentMigration.migrate(context);
                    currentVersion = currentMigration.newVersion;
                }
            } catch (final Exception e) {
                // save the version with the last successful migration and report the error
                sp.edit().putInt(lastPrefVersionKey, currentVersion).apply();
                final ErrorInfo errorInfo = ErrorInfo.make(
                        UserAction.PREFERENCES_MIGRATION,
                        "none",
                        "Migrating preferences from version " + lastPrefVersion + " to "
                                + VERSION + ". "
                                + "Error at " + currentVersion  + " => " + ++currentVersion,
                        0
                );
                ErrorActivity.reportError(context, e, SettingMigrations.class, null, errorInfo);
                return;
            }
        }

        // store the current preferences version
        sp.edit().putInt(lastPrefVersionKey, currentVersion).apply();
    }

    private SettingMigrations() { }

    abstract static class Migration {
        public final int oldVersion;
        public final int newVersion;

        protected Migration(final int oldVersion, final int newVersion) {
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }

        /**
         * @param currentVersion current settings version
         * @return Returns whether this migration should be run.
         * A migration is necessary if the old version of this migration is lower than or equal to
         * the current settings version.
         */
        private boolean shouldMigrate(final int currentVersion) {
            return oldVersion >= currentVersion;
        }

        protected abstract void migrate(Context context);

    }

}
