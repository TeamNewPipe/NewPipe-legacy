package org.schabi.newpipelegacy;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipelegacy.report.ErrorActivity;
import org.schabi.newpipelegacy.report.UserAction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * AsyncTask to check if there is a newer version of the NewPipe github apk available or not.
 * If there is a newer version we show a notification, informing the user. On tapping
 * the notification, the user will be directed to the download link.
 */
public class CheckForNewAppVersionTask extends AsyncTask<Void, Void, String> {
    private static final boolean DEBUG = MainActivity.DEBUG;
    private static final String TAG = CheckForNewAppVersionTask.class.getSimpleName();

    private static final Application APP = App.getApp();
    private static final String GITHUB_APK_SHA1
            = "C7:A4:55:67:6C:34:97:42:10:CE:3B:02:8F:D4:11:E5:10:FB:01:17";
    private static final String NEWPIPE_API_URL = "https://newpipe.schabi.org/api/data.json";
    private static final String FLAVOR = "github_legacy";

    /**
     * Method to get the apk's SHA1 key. See https://stackoverflow.com/questions/9293019/#22506133.
     *
     * @return String with the apk's SHA1 fingeprint in hexadecimal
     */
    private static String getCertificateSHA1Fingerprint() {
        final PackageManager pm = APP.getPackageManager();
        final String packageName = APP.getPackageName();
        final int flags = PackageManager.GET_SIGNATURES;
        PackageInfo packageInfo = null;

        try {
            packageInfo = pm.getPackageInfo(packageName, flags);
        } catch (final PackageManager.NameNotFoundException e) {
            ErrorActivity.reportError(APP, e, null, null,
                    ErrorActivity.ErrorInfo.make(UserAction.SOMETHING_ELSE, "none",
                            "Could not find package info", R.string.app_ui_crash));
        }

        final Signature[] signatures = packageInfo.signatures;
        final byte[] cert = signatures[0].toByteArray();
        final InputStream input = new ByteArrayInputStream(cert);

        X509Certificate c = null;

        try {
            final CertificateFactory cf = CertificateFactory.getInstance("X509");
            c = (X509Certificate) cf.generateCertificate(input);
        } catch (final CertificateException e) {
            ErrorActivity.reportError(APP, e, null, null,
                    ErrorActivity.ErrorInfo.make(UserAction.SOMETHING_ELSE, "none",
                            "Certificate error", R.string.app_ui_crash));
        }

        String hexString = null;

        try {
            final MessageDigest md = MessageDigest.getInstance("SHA1");
            final byte[] publicKey = md.digest(c.getEncoded());
            hexString = byte2HexFormatted(publicKey);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            ErrorActivity.reportError(APP, e, null, null,
                    ErrorActivity.ErrorInfo.make(UserAction.SOMETHING_ELSE, "none",
                            "Could not retrieve SHA1 key", R.string.app_ui_crash));
        }

        return hexString;
    }

    private static String byte2HexFormatted(final byte[] arr) {
        final StringBuilder str = new StringBuilder(arr.length * 2);

        for (int i = 0; i < arr.length; i++) {
            String h = Integer.toHexString(arr[i]);
            final int l = h.length();
            if (l == 1) {
                h = "0" + h;
            }
            if (l > 2) {
                h = h.substring(l - 2, l);
            }
            str.append(h.toUpperCase());
            if (i < (arr.length - 1)) {
                str.append(':');
            }
        }
        return str.toString();
    }

    public static boolean isGithubApk() {
        return getCertificateSHA1Fingerprint().equals(GITHUB_APK_SHA1);
    }

    @Override
    protected void onPreExecute() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(APP);

        // Check if user has enabled/disabled update checking
        // and if the current apk is a github one or not.
        if (!prefs.getBoolean(APP.getString(R.string.update_app_key), true) || !isGithubApk()) {
            this.cancel(true);
        }
    }

    @Override
    protected String doInBackground(final Void... voids) {
        if (isCancelled() || !isConnected()) {
            return null;
        }

        // Make a network request to get latest NewPipe data.
        try {
            return DownloaderImpl.getInstance().get(NEWPIPE_API_URL).responseBody();
        } catch (IOException | ReCaptchaException e) {
            // connectivity problems, do not alarm user and fail silently
            if (DEBUG) {
                Log.w(TAG, Log.getStackTraceString(e));
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(final String response) {
        // Parse the json from the response.
        if (response != null) {

            try {
                final JsonObject githubStableObject = JsonParser.object().from(response)
                        .getObject("flavors").getObject(FLAVOR).getObject("stable");

                final String versionName = githubStableObject.getString("version");
                final int versionCode = githubStableObject.getInt("version_code");
                final String apkLocationUrl = githubStableObject.getString("apk");

                compareAppVersionAndShowNotification(versionName, apkLocationUrl, versionCode);

            } catch (final JsonParserException e) {
                // connectivity problems, do not alarm user and fail silently
                if (DEBUG) {
                    Log.w(TAG, Log.getStackTraceString(e));
                }
            }
        }
    }

    /**
     * Method to compare the current and latest available app version.
     * If a newer version is available, we show the update notification.
     *
     * @param versionName    Name of new version
     * @param apkLocationUrl Url with the new apk
     * @param versionCode    Code of new version
     */
    private void compareAppVersionAndShowNotification(final String versionName,
                                                      final String apkLocationUrl,
                                                      final int versionCode) {
        final int notificationId = 2000;

        if (BuildConfig.VERSION_CODE < versionCode) {

            // A pending intent to open the apk location url in the browser.
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkLocationUrl));
            final PendingIntent pendingIntent
                    = PendingIntent.getActivity(APP, 0, intent, 0);

            final NotificationCompat.Builder notificationBuilder = new NotificationCompat
                    .Builder(APP, APP.getString(R.string.app_update_notification_channel_id))
                    .setSmallIcon(R.drawable.ic_newpipe_update)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(APP.getString(R.string.app_update_notification_content_title))
                    .setContentText(APP.getString(R.string.app_update_notification_content_text)
                            + " " + versionName);

            final NotificationManagerCompat notificationManager
                    = NotificationManagerCompat.from(APP);
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    private boolean isConnected() {
        final ConnectivityManager cm = ContextCompat.getSystemService(APP,
                ConnectivityManager.class);
        return cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnected();
    }
}
