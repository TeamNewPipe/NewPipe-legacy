package org.schabi.newpipelegacy.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.filepicker.Utils;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.schabi.newpipelegacy.DownloaderImpl;
import org.schabi.newpipelegacy.NewPipeDatabase;
import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.ReCaptchaActivity;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipelegacy.report.ErrorActivity;
import org.schabi.newpipelegacy.report.UserAction;
import org.schabi.newpipelegacy.util.FilePickerActivityHelper;
import org.schabi.newpipelegacy.util.ZipHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.schabi.newpipelegacy.util.Localization.assureCorrectAppLanguage;

public class ContentSettingsFragment extends BasePreferenceFragment {
    private static final int REQUEST_IMPORT_PATH = 8945;
    private static final int REQUEST_EXPORT_PATH = 30945;

    private File databasesDir;
    private File newpipeDb;
    private File newpipeDbJournal;
    private File newpipeDbShm;
    private File newpipeDbWal;
    private File newpipeSettings;

    private String thumbnailLoadToggleKey;
    private String youtubeRestrictedModeEnabledKey;

    private Localization initialSelectedLocalization;
    private ContentCountry initialSelectedContentCountry;
    private String initialLanguage;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thumbnailLoadToggleKey = getString(R.string.download_thumbnail_key);
        youtubeRestrictedModeEnabledKey = getString(R.string.youtube_restricted_mode_enabled);

        initialSelectedLocalization = org.schabi.newpipelegacy.util.Localization
                .getPreferredLocalization(requireContext());
        initialSelectedContentCountry = org.schabi.newpipelegacy.util.Localization
                .getPreferredContentCountry(requireContext());
        initialLanguage = PreferenceManager
                .getDefaultSharedPreferences(requireContext()).getString("app_language_key", "en");

        final Preference clearCookiePref = findPreference(getString(R.string.clear_cookie_key));

        clearCookiePref.setOnPreferenceClickListener(preference -> {
            defaultPreferences.edit()
                    .putString(getString(R.string.recaptcha_cookies_key), "").apply();
            DownloaderImpl.getInstance().setCookie(ReCaptchaActivity.RECAPTCHA_COOKIES_KEY, "");
            Toast.makeText(getActivity(), R.string.recaptcha_cookies_cleared,
                    Toast.LENGTH_SHORT).show();
            clearCookiePref.setVisible(false);
            return true;
        });

        if (defaultPreferences.getString(getString(R.string.recaptcha_cookies_key), "").isEmpty()) {
            clearCookiePref.setVisible(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(final Preference preference) {
        if (preference.getKey().equals(thumbnailLoadToggleKey)) {
            final ImageLoader imageLoader = ImageLoader.getInstance();
            imageLoader.stop();
            imageLoader.clearDiskCache();
            imageLoader.clearMemoryCache();
            imageLoader.resume();
            Toast.makeText(preference.getContext(), R.string.thumbnail_cache_wipe_complete_notice,
                    Toast.LENGTH_SHORT).show();
        }

        if (preference.getKey().equals(youtubeRestrictedModeEnabledKey)) {
            final Context context = getContext();
            if (context != null) {
                DownloaderImpl.getInstance().updateYoutubeRestrictedModeCookies(context);
            } else {
                Log.w(TAG, "onPreferenceTreeClick: null context");
            }
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {

        final String homeDir = getActivity().getApplicationInfo().dataDir;
        databasesDir = new File(homeDir + "/databases");
        newpipeDb = new File(homeDir + "/databases/newpipe.db");
        newpipeDbJournal = new File(homeDir + "/databases/newpipe.db-journal");
        newpipeDbShm = new File(homeDir + "/databases/newpipe.db-shm");
        newpipeDbWal = new File(homeDir + "/databases/newpipe.db-wal");

        newpipeSettings = new File(homeDir + "/databases/newpipe.settings");
        newpipeSettings.delete();

        addPreferencesFromResource(R.xml.content_settings);

        final Preference importDataPreference = findPreference(getString(R.string.import_data));
        importDataPreference.setOnPreferenceClickListener((Preference p) -> {
            final Intent i = new Intent(getActivity(), FilePickerActivityHelper.class)
                    .putExtra(FilePickerActivityHelper.EXTRA_ALLOW_MULTIPLE, false)
                    .putExtra(FilePickerActivityHelper.EXTRA_ALLOW_CREATE_DIR, false)
                    .putExtra(FilePickerActivityHelper.EXTRA_MODE,
                            FilePickerActivityHelper.MODE_FILE);
            startActivityForResult(i, REQUEST_IMPORT_PATH);
            return true;
        });

        final Preference exportDataPreference = findPreference(getString(R.string.export_data));
        exportDataPreference.setOnPreferenceClickListener((Preference p) -> {
            final Intent i = new Intent(getActivity(), FilePickerActivityHelper.class)
                    .putExtra(FilePickerActivityHelper.EXTRA_ALLOW_MULTIPLE, false)
                    .putExtra(FilePickerActivityHelper.EXTRA_ALLOW_CREATE_DIR, true)
                    .putExtra(FilePickerActivityHelper.EXTRA_MODE,
                            FilePickerActivityHelper.MODE_DIR);
            startActivityForResult(i, REQUEST_EXPORT_PATH);
            return true;
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final Localization selectedLocalization = org.schabi.newpipelegacy.util.Localization
                .getPreferredLocalization(requireContext());
        final ContentCountry selectedContentCountry = org.schabi.newpipelegacy.util.Localization
                .getPreferredContentCountry(requireContext());
        final String selectedLanguage = PreferenceManager
                .getDefaultSharedPreferences(requireContext()).getString("app_language_key", "en");

        if (!selectedLocalization.equals(initialSelectedLocalization)
                || !selectedContentCountry.equals(initialSelectedContentCountry)
                || !selectedLanguage.equals(initialLanguage)) {
            Toast.makeText(requireContext(), R.string.localization_changes_requires_app_restart,
                    Toast.LENGTH_LONG).show();

            NewPipe.setupLocalization(selectedLocalization, selectedContentCountry);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
                                 @NonNull final Intent data) {
        assureCorrectAppLanguage(getContext());
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG) {
            Log.d(TAG, "onActivityResult() called with: "
                    + "requestCode = [" + requestCode + "], "
                    + "resultCode = [" + resultCode + "], "
                    + "data = [" + data + "]");
        }

        if ((requestCode == REQUEST_IMPORT_PATH || requestCode == REQUEST_EXPORT_PATH)
                && resultCode == Activity.RESULT_OK && data.getData() != null) {
            final String path = Utils.getFileForUri(data.getData()).getAbsolutePath();
            if (requestCode == REQUEST_EXPORT_PATH) {
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                exportDatabase(path + "/NewPipeData-" + sdf.format(new Date()) + ".zip");
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.override_current_data)
                        .setPositiveButton(getString(R.string.finish),
                                (DialogInterface d, int id) -> importDatabase(path))
                        .setNegativeButton(android.R.string.cancel,
                                (DialogInterface d, int id) -> d.cancel());
                builder.create().show();
            }
        }
    }

    private void exportDatabase(final String path) {
        try {
            //checkpoint before export
            NewPipeDatabase.checkpoint();

            final ZipOutputStream outZip = new ZipOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(path)));
            ZipHelper.addFileToZip(outZip, newpipeDb.getPath(), "newpipe.db");

            saveSharedPreferencesToFile(newpipeSettings);
            ZipHelper.addFileToZip(outZip, newpipeSettings.getPath(), "newpipe.settings");

            outZip.close();

            Toast.makeText(getContext(), R.string.export_complete_toast, Toast.LENGTH_SHORT)
                    .show();
        } catch (final Exception e) {
            onError(e);
        }
    }

    private void saveSharedPreferencesToFile(final File dst) {
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            final SharedPreferences pref
                    = PreferenceManager.getDefaultSharedPreferences(requireContext());
            output.writeObject(pref.getAll());

        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void importDatabase(final String filePath) {
        // check if file is supported
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(filePath);
        } catch (final IOException ioe) {
            Toast.makeText(getContext(), R.string.no_valid_zip_file, Toast.LENGTH_SHORT)
                    .show();
            return;
        } finally {
            try {
                zipFile.close();
            } catch (final Exception ignored) {
            }
        }

        try {
            if (!databasesDir.exists() && !databasesDir.mkdir()) {
                throw new Exception("Could not create databases dir");
            }

            final boolean isDbFileExtracted = ZipHelper.extractFileFromZip(filePath,
                    newpipeDb.getPath(), "newpipe.db");

            if (isDbFileExtracted) {
                newpipeDbJournal.delete();
                newpipeDbWal.delete();
                newpipeDbShm.delete();
            } else {
                Toast.makeText(getContext(), R.string.could_not_import_all_files, Toast.LENGTH_LONG)
                        .show();
            }

            //If settings file exist, ask if it should be imported.
            if (ZipHelper.extractFileFromZip(filePath, newpipeSettings.getPath(),
                    "newpipe.settings")) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle(R.string.import_settings);

                alert.setNegativeButton(android.R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                    // restart app to properly load db
                    System.exit(0);
                });
                alert.setPositiveButton(getString(R.string.finish), (dialog, which) -> {
                    dialog.dismiss();
                    loadSharedPreferences(newpipeSettings);
                    // restart app to properly load db
                    System.exit(0);
                });
                alert.show();
            } else {
                // restart app to properly load db
                System.exit(0);
            }
        } catch (final Exception e) {
            onError(e);
        }
    }

    private void loadSharedPreferences(final File src) {
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(src));
            final SharedPreferences.Editor prefEdit = PreferenceManager
                    .getDefaultSharedPreferences(requireContext()).edit();
            prefEdit.clear();
            final Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (final Map.Entry<String, ?> entry : entries.entrySet()) {
                final Object v = entry.getValue();
                final String key = entry.getKey();

                if (v instanceof Boolean) {
                    prefEdit.putBoolean(key, (Boolean) v);
                } else if (v instanceof Float) {
                    prefEdit.putFloat(key, (Float) v);
                } else if (v instanceof Integer) {
                    prefEdit.putInt(key, (Integer) v);
                } else if (v instanceof Long) {
                    prefEdit.putLong(key, (Long) v);
                } else if (v instanceof String) {
                    prefEdit.putString(key, (String) v);
                }
            }
            prefEdit.commit();
        } catch (final IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Error
    //////////////////////////////////////////////////////////////////////////*/

    protected void onError(final Throwable e) {
        final Activity activity = getActivity();
        ErrorActivity.reportError(activity, e,
                activity.getClass(),
                null,
                ErrorActivity.ErrorInfo.make(UserAction.UI_ERROR,
                        "none", "", R.string.app_ui_crash));
    }
}
