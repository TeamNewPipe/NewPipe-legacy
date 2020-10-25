package org.schabi.newpipelegacy.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.schabi.newpipelegacy.R;

public final class ShareUtils {
    private ShareUtils() {
    }

    /**
     * Open the url with the system default browser.
     * <p>
     * If no browser is set as default, fallbacks to
     * {@link ShareUtils#openInDefaultApp(Context, String)}
     *
     * @param context the context to use
     * @param url     the url to browse
     */
    public static void openUrlInBrowser(final Context context, final String url) {
        final String defaultBrowserPackageName = getDefaultBrowserPackageName(context);

        if (defaultBrowserPackageName.equals("android")) {
            // no browser set as default
            openInDefaultApp(context, url);
        } else {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .setPackage(defaultBrowserPackageName)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * Open the url in the default app set to open this type of link.
     * <p>
     * If no app is set as default, it will open a chooser
     *
     * @param context the context to use
     * @param url     the url to browse
     */
    private static void openInDefaultApp(final Context context, final String url) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(Intent.createChooser(
                intent, context.getString(R.string.share_dialog_title))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /**
     * Get the default browser package name.
     * <p>
     * If no browser is set as default, it will return "android"
     *
     * @param context the context to use
     * @return the package name of the default browser, or "android" if there's no default
     */
    private static String getDefaultBrowserPackageName(final Context context) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    /**
     * Open the android share menu to share the current url.
     *
     * @param context the context to use
     * @param subject the url subject, typically the title
     * @param url     the url to share
     */
    public static void shareUrl(final Context context, final String subject, final String url) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(
                intent, context.getString(R.string.share_dialog_title))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /**
     * Copy the text to clipboard, and indicate to the user whether the operation was completed
     * successfully using a Toast.
     *
     * @param context the context to use
     * @param text    the text to copy
     */
    public static void copyToClipboard(final Context context, final String text) {
        final ClipboardManager clipboardManager =
                ContextCompat.getSystemService(context, ClipboardManager.class);

        if (clipboardManager == null) {
            Toast.makeText(context,
                    R.string.permission_denied,
                    Toast.LENGTH_LONG).show();
            return;
        }

        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text));
        Toast.makeText(context, R.string.msg_copied, Toast.LENGTH_SHORT)
                .show();
    }
}
