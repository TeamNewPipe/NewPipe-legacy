package org.schabi.newpipelegacy.report;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

import com.google.android.material.snackbar.Snackbar;
import com.grack.nanojson.JsonWriter;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.schabi.newpipelegacy.ActivityCommunicator;
import org.schabi.newpipelegacy.BuildConfig;
import org.schabi.newpipelegacy.MainActivity;
import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.util.Localization;
import org.schabi.newpipelegacy.util.ShareUtils;
import org.schabi.newpipelegacy.util.ThemeHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import static org.schabi.newpipelegacy.util.Localization.assureCorrectAppLanguage;

/*
 * Created by Christian Schabesberger on 24.10.15.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * ErrorActivity.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class ErrorActivity extends AppCompatActivity {
    // LOG TAGS
    public static final String TAG = ErrorActivity.class.toString();
    // BUNDLE TAGS
    public static final String ERROR_INFO = "error_info";
    public static final String ERROR_LIST = "error_list";

    public static final String ERROR_EMAIL_ADDRESS = "crashreport@newpipe.schabi.org";
    public static final String ERROR_EMAIL_SUBJECT
            = "Exception in " + R.string.app_name + ' ' + BuildConfig.VERSION_NAME;

    public static final String ERROR_GITHUB_ISSUE_URL
            = "https://github.com/TeamNewPipe/NewPipe-legacy/issues";

    private String[] errorList;
    private ErrorInfo errorInfo;
    private Class returnActivity;
    private String currentTimeStamp;
    private EditText userCommentBox;

    public static void reportUiError(final AppCompatActivity activity, final Throwable el) {
        reportError(activity, el, activity.getClass(), null, ErrorInfo.make(UserAction.UI_ERROR,
                "none", "", R.string.app_ui_crash));
    }

    public static void reportError(final Context context, final List<Throwable> el,
                                   final Class returnActivity, final View rootView,
                                   final ErrorInfo errorInfo) {
        if (rootView != null) {
            Snackbar.make(rootView, R.string.error_snackbar_message, 3 * 1000)
                    .setActionTextColor(Color.YELLOW)
                    .setAction(context.getString(R.string.error_snackbar_action).toUpperCase(), v ->
                            startErrorActivity(returnActivity, context, errorInfo, el)).show();
        } else {
            startErrorActivity(returnActivity, context, errorInfo, el);
        }
    }

    private static void startErrorActivity(final Class returnActivity, final Context context,
                                           final ErrorInfo errorInfo, final List<Throwable> el) {
        final ActivityCommunicator ac = ActivityCommunicator.getCommunicator();
        ac.setReturnActivity(returnActivity);
        final Intent intent = new Intent(context, ErrorActivity.class);
        intent.putExtra(ERROR_INFO, errorInfo);
        intent.putExtra(ERROR_LIST, elToSl(el));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void reportError(final Context context, final Throwable e,
                                   final Class returnActivity, final View rootView,
                                   final ErrorInfo errorInfo) {
        List<Throwable> el = null;
        if (e != null) {
            el = new Vector<>();
            el.add(e);
        }
        reportError(context, el, returnActivity, rootView, errorInfo);
    }

    // async call
    public static void reportError(final Handler handler, final Context context,
                                   final Throwable e, final Class returnActivity,
                                   final View rootView, final ErrorInfo errorInfo) {

        List<Throwable> el = null;
        if (e != null) {
            el = new Vector<>();
            el.add(e);
        }
        reportError(handler, context, el, returnActivity, rootView, errorInfo);
    }

    // async call
    public static void reportError(final Handler handler, final Context context,
                                   final List<Throwable> el, final Class returnActivity,
                                   final View rootView, final ErrorInfo errorInfo) {
        handler.post(() -> reportError(context, el, returnActivity, rootView, errorInfo));
    }

    public static void reportError(final Context context, final CrashReportData report,
                                   final ErrorInfo errorInfo) {
        final String[] el = new String[]{report.getString(ReportField.STACK_TRACE)};

        final Intent intent = new Intent(context, ErrorActivity.class);
        intent.putExtra(ERROR_INFO, errorInfo);
        intent.putExtra(ERROR_LIST, el);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    // errorList to StringList
    private static String[] elToSl(final List<Throwable> stackTraces) {
        final String[] out = new String[stackTraces.size()];
        for (int i = 0; i < stackTraces.size(); i++) {
            out[i] = getStackTrace(stackTraces.get(i));
        }
        return out;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        assureCorrectAppLanguage(this);
        super.onCreate(savedInstanceState);
        ThemeHelper.setTheme(this);
        setContentView(R.layout.activity_error);

        final Intent intent = getIntent();

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.error_report_title);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        final Button reportEmailButton = findViewById(R.id.errorReportEmailButton);
        final Button copyButton = findViewById(R.id.errorReportCopyButton);
        final Button reportGithubButton = findViewById(R.id.errorReportGitHubButton);

        userCommentBox = findViewById(R.id.errorCommentBox);
        final TextView errorView = findViewById(R.id.errorView);
        final TextView infoView = findViewById(R.id.errorInfosView);
        final TextView errorMessageView = findViewById(R.id.errorMessageView);

        final ActivityCommunicator ac = ActivityCommunicator.getCommunicator();
        returnActivity = ac.getReturnActivity();
        errorInfo = intent.getParcelableExtra(ERROR_INFO);
        errorList = intent.getStringArrayExtra(ERROR_LIST);

        // important add guru meditation
        addGuruMeditation();
        currentTimeStamp = getCurrentTimeStamp();

        reportEmailButton.setOnClickListener((View v) ->
                openPrivacyPolicyDialog(this, "EMAIL"));

        copyButton.setOnClickListener((View v) -> {
            ShareUtils.copyToClipboard(this, buildMarkdown());
            Toast.makeText(this, R.string.msg_copied, Toast.LENGTH_SHORT).show();
        });

        reportGithubButton.setOnClickListener((View v) ->
                openPrivacyPolicyDialog(this, "GITHUB"));

        // normal bugreport
        buildInfo(errorInfo);
        if (errorInfo.message != 0) {
            errorMessageView.setText(errorInfo.message);
        } else {
            errorMessageView.setVisibility(View.GONE);
            findViewById(R.id.messageWhatHappenedView).setVisibility(View.GONE);
        }

        errorView.setText(formErrorText(errorList));

        // print stack trace once again for debugging:
        for (final String e : errorList) {
            Log.e(TAG, e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.error_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                goToReturnActivity();
                break;
            case R.id.menu_item_share_error:
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, buildJson());
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, getString(R.string.share_dialog_title)));
                break;
        }
        return false;
    }

    private void openPrivacyPolicyDialog(final Context context, final String action) {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.privacy_policy_title)
                .setMessage(R.string.start_accept_privacy_policy)
                .setCancelable(false)
                .setNeutralButton(R.string.read_privacy_policy, (dialog, which) ->
                        ShareUtils.openUrlInBrowser(context,
                                context.getString(R.string.privacy_policy_url)))
                .setPositiveButton(R.string.accept, (dialog, which) -> {
                    if (action.equals("EMAIL")) { // send on email
                        final Intent i = new Intent(Intent.ACTION_SENDTO)
                                .setData(Uri.parse("mailto:")) // only email apps should handle this
                                .putExtra(Intent.EXTRA_EMAIL, new String[]{ERROR_EMAIL_ADDRESS})
                                .putExtra(Intent.EXTRA_SUBJECT, ERROR_EMAIL_SUBJECT)
                                .putExtra(Intent.EXTRA_TEXT, buildJson());
                        if (i.resolveActivity(getPackageManager()) != null) {
                            startActivity(i);
                        }
                    } else if (action.equals("GITHUB")) { // open the NewPipe issue page on GitHub
                        ShareUtils.openUrlInBrowser(this, ERROR_GITHUB_ISSUE_URL);
                    }

                })
                .setNegativeButton(R.string.decline, (dialog, which) -> {
                    // do nothing
                })
                .show();
    }

    private String formErrorText(final String[] el) {
        final StringBuilder text = new StringBuilder();
        if (el != null) {
            for (final String e : el) {
                text.append("-------------------------------------\n").append(e);
            }
        }
        text.append("-------------------------------------");
        return text.toString();
    }

    /**
     * Get the checked activity.
     *
     * @param returnActivity the activity to return to
     * @return the casted return activity or null
     */
    @Nullable
    static Class<? extends Activity> getReturnActivity(final Class<?> returnActivity) {
        Class<? extends Activity> checkedReturnActivity = null;
        if (returnActivity != null) {
            if (Activity.class.isAssignableFrom(returnActivity)) {
                checkedReturnActivity = returnActivity.asSubclass(Activity.class);
            } else {
                checkedReturnActivity = MainActivity.class;
            }
        }
        return checkedReturnActivity;
    }

    private void goToReturnActivity() {
        final Class<? extends Activity> checkedReturnActivity = getReturnActivity(returnActivity);
        if (checkedReturnActivity == null) {
            super.onBackPressed();
        } else {
            final Intent intent = new Intent(this, checkedReturnActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            NavUtils.navigateUpTo(this, intent);
        }
    }

    private void buildInfo(final ErrorInfo info) {
        final TextView infoLabelView = findViewById(R.id.errorInfoLabelsView);
        final TextView infoView = findViewById(R.id.errorInfosView);
        String text = "";

        infoLabelView.setText(getString(R.string.info_labels).replace("\\n", "\n"));

        text += getUserActionString(info.userAction) + "\n"
                + info.request + "\n"
                + getContentLanguageString() + "\n"
                + getContentCountryString() + "\n"
                + getAppLanguage() + "\n"
                + info.serviceName + "\n"
                + currentTimeStamp + "\n"
                + getPackageName() + "\n"
                + BuildConfig.VERSION_NAME + "\n"
                + getOsString();

        infoView.setText(text);
    }

    private String buildJson() {
        try {
            return JsonWriter.string()
                    .object()
                    .value("user_action", getUserActionString(errorInfo.userAction))
                    .value("request", errorInfo.request)
                    .value("content_language", getContentLanguageString())
                    .value("content_country", getContentCountryString())
                    .value("app_language", getAppLanguage())
                    .value("service", errorInfo.serviceName)
                    .value("package", getPackageName())
                    .value("version", BuildConfig.VERSION_NAME)
                    .value("os", getOsString())
                    .value("time", currentTimeStamp)
                    .array("exceptions", Arrays.asList(errorList))
                    .value("user_comment", userCommentBox.getText().toString())
                    .end()
                    .done();
        } catch (final Throwable e) {
            Log.e(TAG, "Error while erroring: Could not build json");
            e.printStackTrace();
        }

        return "";
    }

    private String buildMarkdown() {
        try {
            final StringBuilder htmlErrorReport = new StringBuilder();

            final String userComment = userCommentBox.getText().toString();
            if (!userComment.isEmpty()) {
                htmlErrorReport.append(userComment).append("\n");
            }

            // basic error info
            htmlErrorReport
                    .append("## Exception")
                    .append("\n* __User Action:__ ")
                        .append(getUserActionString(errorInfo.userAction))
                    .append("\n* __Request:__ ").append(errorInfo.request)
                    .append("\n* __Content Country:__ ").append(getContentCountryString())
                    .append("\n* __Content Language:__ ").append(getContentLanguageString())
                    .append("\n* __App Language:__ ").append(getAppLanguage())
                    .append("\n* __Service:__ ").append(errorInfo.serviceName)
                    .append("\n* __Version:__ ").append(BuildConfig.VERSION_NAME)
                    .append("\n* __OS:__ ").append(getOsString()).append("\n");


            // Collapse all logs to a single paragraph when there are more than one
            // to keep the GitHub issue clean.
            if (errorList.length > 1) {
                htmlErrorReport
                        .append("<details><summary><b>Exceptions (")
                        .append(errorList.length)
                        .append(")</b></summary><p>\n");
            }

            // add the logs
            for (int i = 0; i < errorList.length; i++) {
                htmlErrorReport.append("<details><summary><b>Crash log ");
                if (errorList.length > 1) {
                    htmlErrorReport.append(i + 1);
                }
                htmlErrorReport.append("</b>")
                        .append("</summary><p>\n")
                        .append("\n```\n").append(errorList[i]).append("\n```\n")
                        .append("</details>\n");
            }

            // make sure to close everything
            if (errorList.length > 1) {
                htmlErrorReport.append("</p></details>\n");
            }
            htmlErrorReport.append("<hr>\n");
            return htmlErrorReport.toString();
        } catch (final Throwable e) {
            Log.e(TAG, "Error while erroring: Could not build markdown");
            e.printStackTrace();
            return "";
        }
    }

    private String getUserActionString(final UserAction userAction) {
        if (userAction == null) {
            return "Your description is in another castle.";
        } else {
            return userAction.getMessage();
        }
    }

    private String getContentCountryString() {
        return Localization.getPreferredContentCountry(this).getCountryCode();
    }

    private String getContentLanguageString() {
        return Localization.getPreferredLocalization(this).getLocalizationCode();
    }

    private String getAppLanguage() {
        return Localization.getAppLocale(getApplicationContext()).toString();
    }

    private String getOsString() {
        final String osBase = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? Build.VERSION.BASE_OS : "Android";
        return System.getProperty("os.name")
                + " " + (osBase.isEmpty() ? "Android" : osBase)
                + " " + Build.VERSION.RELEASE
                + " - " + Build.VERSION.SDK_INT;
    }

    private void addGuruMeditation() {
        //just an easter egg
        final TextView sorryView = findViewById(R.id.errorSorryView);
        String text = sorryView.getText().toString();
        text += "\n" + getString(R.string.guru_meditation);
        sorryView.setText(text);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        goToReturnActivity();
    }

    public String getCurrentTimeStamp() {
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date());
    }

    public static class ErrorInfo implements Parcelable {
        public static final Parcelable.Creator<ErrorInfo> CREATOR
                = new Parcelable.Creator<ErrorInfo>() {
            @Override
            public ErrorInfo createFromParcel(final Parcel source) {
                return new ErrorInfo(source);
            }

            @Override
            public ErrorInfo[] newArray(final int size) {
                return new ErrorInfo[size];
            }
        };

        final UserAction userAction;
        public final String request;
        final String serviceName;
        @StringRes
        public final int message;

        private ErrorInfo(final UserAction userAction, final String serviceName,
                          final String request, @StringRes final int message) {
            this.userAction = userAction;
            this.serviceName = serviceName;
            this.request = request;
            this.message = message;
        }

        protected ErrorInfo(final Parcel in) {
            this.userAction = UserAction.valueOf(in.readString());
            this.request = in.readString();
            this.serviceName = in.readString();
            this.message = in.readInt();
        }

        public static ErrorInfo make(final UserAction userAction, final String serviceName,
                                     final String request, @StringRes final int message) {
            return new ErrorInfo(userAction, serviceName, request, message);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeString(this.userAction.name());
            dest.writeString(this.request);
            dest.writeString(this.serviceName);
            dest.writeInt(this.message);
        }
    }
}
