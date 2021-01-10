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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipelegacy.report.ErrorActivity;
import org.schabi.newpipelegacy.report.ErrorInfo;
import org.schabi.newpipelegacy.report.UserAction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class CheckForNewAppVersion {
    private CheckForNewAppVersion() { }

    private static final boolean DEBUG = MainActivity.DEBUG;
    private static final String TAG = CheckForNewAppVersion.class.getSimpleName();

    private static final String GITHUB_APK_SHA1
            = "C7:A4:55:67:6C:34:97:42:10:CE:3B:02:8F:D4:11:E5:10:FB:01:17";
    private static final String NEWPIPE_API_URL = "https://newpipe.net/api/data.json";
    private static final String FLAVOR = "github_legacy";

    /**
     * Method to get the APK's SHA1 key. See https://stackoverflow.com/questions/9293019/#22506133.
     *
     * @param application The application
     * @return String with the APK's SHA1 fingerprint in hexadecimal
     */
    @NonNull
    private static String getCertificateSHA1Fingerprint(@NonNull final Application application) {
        final PackageInfo packageInfo;
        try {
            packageInfo = application.getPackageManager().getPackageInfo(
                    application.getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (final PackageManager.NameNotFoundException e) {
            ErrorActivity.reportError(application, e, null, null,
                    ErrorInfo.make(UserAction.SOMETHING_ELSE, "none",
                            "Could not find package info", R.string.app_ui_crash));
            return "";
        }

        final X509Certificate c;
        try {
            final Signature[] signatures = packageInfo.signatures;
            final byte[] cert = signatures[0].toByteArray();
            final InputStream input = new ByteArrayInputStream(cert);
            final CertificateFactory cf = CertificateFactory.getInstance("X509");
            c = (X509Certificate) cf.generateCertificate(input);
        } catch (final CertificateException e) {
            ErrorActivity.reportError(application, e, null, null,
                    ErrorInfo.make(UserAction.SOMETHING_ELSE, "none",
                            "Certificate error", R.string.app_ui_crash));
            return "";
        }

        try {
            final MessageDigest md = MessageDigest.getInstance("SHA1");
            final byte[] publicKey = md.digest(c.getEncoded());
            return byte2HexFormatted(publicKey);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            ErrorActivity.reportError(application, e, null, null,
                    ErrorInfo.make(UserAction.SOMETHING_ELSE, "none",
                            "Could not retrieve SHA1 key", R.string.app_ui_crash));
            return "";
        }
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

    /**
     * Method to compare the current and latest available app version.
     * If a newer version is available, we show the update notification.
     *
     * @param application    The application
     * @param versionName    Name of new version
     * @param apkLocationUrl Url with the new apk
     * @param versionCode    Code of new version
     */
    private static void compareAppVersionAndShowNotification(@NonNull final Application application,
                                                             final String versionName,
                                                             final String apkLocationUrl,
                                                             final int versionCode) {
        final int notificationId = 2000;

        if (BuildConfig.VERSION_CODE < versionCode) {
            // A pending intent to open the apk location url in the browser.
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkLocationUrl));
            final PendingIntent pendingIntent
                    = PendingIntent.getActivity(application, 0, intent, 0);

            final String channelId = application
                    .getString(R.string.app_update_notification_channel_id);
            final NotificationCompat.Builder notificationBuilder
                    = new NotificationCompat.Builder(application, channelId)
                    .setSmallIcon(R.drawable.ic_newpipe_update)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle(application
                            .getString(R.string.app_update_notification_content_title))
                    .setContentText(application
                            .getString(R.string.app_update_notification_content_text)
                            + " " + versionName);

            final NotificationManagerCompat notificationManager
                    = NotificationManagerCompat.from(application);
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    private static boolean isConnected(@NonNull final App app) {
        final ConnectivityManager connectivityManager =
                ContextCompat.getSystemService(app, ConnectivityManager.class);
        return connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null
                && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public static boolean isGithubApk(@NonNull final App app) {
        return getCertificateSHA1Fingerprint(app).equals(GITHUB_APK_SHA1);
    }

    @Nullable
    public static Disposable checkNewVersion(@NonNull final App app) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);

        // Check if user has enabled/disabled update checking
        // and if the current apk is a github one or not.
        if (!prefs.getBoolean(app.getString(R.string.update_app_key), true) || !isGithubApk(app)) {
            return null;
        }

        return Maybe
                .fromCallable(() -> {
                    if (!isConnected(app)) {
                        return null;
                    }

                    // Make a network request to get latest NewPipe data.
                    return DownloaderImpl.getInstance().get(NEWPIPE_API_URL).responseBody();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            // Parse the json from the response.
                            try {
                                final JsonObject githubStableObject = JsonParser.object()
                                        .from(response).getObject("flavors").getObject(FLAVOR)
                                        .getObject("stable");

                                final String versionName = githubStableObject
                                        .getString("version");
                                final int versionCode = githubStableObject
                                        .getInt("version_code");
                                final String apkLocationUrl = githubStableObject
                                        .getString("apk");

                                compareAppVersionAndShowNotification(app, versionName,
                                        apkLocationUrl, versionCode);
                            } catch (final JsonParserException e) {
                                // connectivity problems, do not alarm user and fail silently
                                if (DEBUG) {
                                    Log.w(TAG, "Could not get NewPipe API: invalid json", e);
                                }
                            }
                        },
                        e -> {
                            // connectivity problems, do not alarm user and fail silently
                            if (DEBUG) {
                                Log.w(TAG, "Could not get NewPipe API: network problem", e);
                            }
                        });
    }
}
