package org.schabi.newpipelegacy.about;

import android.content.Context;
import android.util.Base64;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.util.ThemeHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.schabi.newpipelegacy.util.Localization.assureCorrectAppLanguage;

public final class LicenseFragmentHelper {
    private LicenseFragmentHelper() { }

    /**
     * @param context the context to use
     * @param license the license
     * @return String which contains a HTML formatted license page
     * styled according to the context's theme
     */
    @SuppressWarnings("CharsetObjectCanBeUsed")
    private static String getFormattedLicense(@NonNull final Context context,
                                              @NonNull final License license) {
        final StringBuilder licenseContent = new StringBuilder();
        final String webViewData;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                context.getAssets().open(license.getFilename()), "utf-8"))) {
            String str;
            while ((str = in.readLine()) != null) {
                licenseContent.append(str);
            }

            // split the HTML file and insert the stylesheet into the HEAD of the file
            webViewData = licenseContent.toString().replace("</head>",
                    "<style>" + getLicenseStylesheet(context) + "</style></head>");
        } catch (final IOException e) {
            throw new IllegalArgumentException(
                    "Could not get license file: " + license.getFilename(), e);
        }
        return webViewData;
    }

    /**
     * @param context the Android context
     * @return String which is a CSS stylesheet according to the context's theme
     */
    private static String getLicenseStylesheet(@NonNull final Context context) {
        final boolean isLightTheme = ThemeHelper.isLightThemeSelected(context);
        return "body{padding:12px 15px;margin:0;"
                + "background:#" + getHexRGBColor(context, isLightTheme
                ? R.color.light_license_background_color
                : R.color.dark_license_background_color) + ";"
                + "color:#" + getHexRGBColor(context, isLightTheme
                ? R.color.light_license_text_color
                : R.color.dark_license_text_color) + "}"
                + "a[href]{color:#" + getHexRGBColor(context, isLightTheme
                ? R.color.light_youtube_primary_color
                : R.color.dark_youtube_primary_color) + "}"
                + "pre{white-space:pre-wrap}";
    }

    /**
     * Cast R.color to a hexadecimal color value.
     *
     * @param context the context to use
     * @param color   the color number from R.color
     * @return a six characters long String with hexadecimal RGB values
     */
    private static String getHexRGBColor(@NonNull final Context context, final int color) {
        return context.getResources().getString(color).substring(3);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    static Disposable showLicense(@Nullable final Context context, @NonNull final License license) {
        if (context == null) {
            return Disposable.empty();
        }

        return Observable.fromCallable(() -> getFormattedLicense(context, license))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(formattedLicense -> {
                    final String webViewData = Base64.encodeToString(formattedLicense
                            .getBytes("utf-8"), Base64.NO_PADDING);
                    final WebView webView = new WebView(context);
                    webView.loadData(webViewData, "text/html; charset=UTF-8", "base64");

                    final AlertDialog.Builder alert = new AlertDialog.Builder(context);
                    alert.setTitle(license.getName());
                    alert.setView(webView);
                    assureCorrectAppLanguage(context);
                    alert.setNegativeButton(context.getString(R.string.finish),
                            (dialog, which) -> dialog.dismiss());
                    alert.show();
                });
    }
}
