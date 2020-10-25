package org.schabi.newpipelegacy.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.jakewharton.rxbinding2.view.RxView;

import org.schabi.newpipelegacy.BaseFragment;
import org.schabi.newpipelegacy.MainActivity;
import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.ReCaptchaActivity;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ContentNotSupportedException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipelegacy.report.ErrorActivity;
import org.schabi.newpipelegacy.report.UserAction;
import org.schabi.newpipelegacy.util.ExceptionUtils;
import org.schabi.newpipelegacy.util.InfoCache;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import icepick.State;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static org.schabi.newpipelegacy.util.AnimationUtils.animateView;

public abstract class BaseStateFragment<I> extends BaseFragment implements ViewContract<I> {
    @State
    protected AtomicBoolean wasLoading = new AtomicBoolean();
    protected AtomicBoolean isLoading = new AtomicBoolean();

    @Nullable
    private View emptyStateView;
    @Nullable
    private ProgressBar loadingProgressBar;

    protected View errorPanelRoot;
    private Button errorButtonRetry;
    private TextView errorTextView;

    @Override
    public void onViewCreated(final View rootView, final Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        doInitialLoadLogic();
    }

    @Override
    public void onPause() {
        super.onPause();
        wasLoading.set(isLoading.get());
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Init
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    protected void initViews(final View rootView, final Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        emptyStateView = rootView.findViewById(R.id.empty_state_view);
        loadingProgressBar = rootView.findViewById(R.id.loading_progress_bar);

        errorPanelRoot = rootView.findViewById(R.id.error_panel);
        errorButtonRetry = rootView.findViewById(R.id.error_button_retry);
        errorTextView = rootView.findViewById(R.id.error_message_view);
    }

    @Override
    protected void initListeners() {
        super.initListeners();
        RxView.clicks(errorButtonRetry)
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> onRetryButtonClicked());
    }

    protected void onRetryButtonClicked() {
        reloadContent();
    }

    public void reloadContent() {
        startLoading(true);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Load
    //////////////////////////////////////////////////////////////////////////*/

    protected void doInitialLoadLogic() {
        startLoading(true);
    }

    protected void startLoading(final boolean forceLoad) {
        if (DEBUG) {
            Log.d(TAG, "startLoading() called with: forceLoad = [" + forceLoad + "]");
        }
        showLoading();
        isLoading.set(true);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Contract
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void showLoading() {
        if (emptyStateView != null) {
            animateView(emptyStateView, false, 150);
        }
        if (loadingProgressBar != null) {
            animateView(loadingProgressBar, true, 400);
        }
        animateView(errorPanelRoot, false, 150);
    }

    @Override
    public void hideLoading() {
        if (emptyStateView != null) {
            animateView(emptyStateView, false, 150);
        }
        if (loadingProgressBar != null) {
            animateView(loadingProgressBar, false, 0);
        }
        animateView(errorPanelRoot, false, 150);
    }

    @Override
    public void showEmptyState() {
        isLoading.set(false);
        if (emptyStateView != null) {
            animateView(emptyStateView, true, 200);
        }
        if (loadingProgressBar != null) {
            animateView(loadingProgressBar, false, 0);
        }
        animateView(errorPanelRoot, false, 150);
    }

    @Override
    public void showError(final String message, final boolean showRetryButton) {
        if (DEBUG) {
            Log.d(TAG, "showError() called with: "
                    + "message = [" + message + "], showRetryButton = [" + showRetryButton + "]");
        }
        isLoading.set(false);
        InfoCache.getInstance().clearCache();
        hideLoading();

        errorTextView.setText(message);
        if (showRetryButton) {
            animateView(errorButtonRetry, true, 600);
        } else {
            animateView(errorButtonRetry, false, 0);
        }
        animateView(errorPanelRoot, true, 300);
    }

    @Override
    public void handleResult(final I result) {
        if (DEBUG) {
            Log.d(TAG, "handleResult() called with: result = [" + result + "]");
        }
        hideLoading();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Error handling
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Default implementation handles some general exceptions.
     *
     * @param exception The exception that should be handled
     * @return If the exception was handled
     */
    protected boolean onError(final Throwable exception) {
        if (DEBUG) {
            Log.d(TAG, "onError() called with: exception = [" + exception + "]");
        }
        isLoading.set(false);

        if (isDetached() || isRemoving()) {
            if (DEBUG) {
                Log.w(TAG, "onError() is detached or removing = [" + exception + "]");
            }
            return true;
        }

        if (ExceptionUtils.isInterruptedCaused(exception)) {
            if (DEBUG) {
                Log.w(TAG, "onError() isInterruptedCaused! = [" + exception + "]");
            }
            return true;
        }

        if (exception instanceof ReCaptchaException) {
            onReCaptchaException((ReCaptchaException) exception);
            return true;
        } else if (exception instanceof ContentNotAvailableException) {
            showError(getString(R.string.content_not_available), false);
            return true;
        } else if (ExceptionUtils.isNetworkRelated(exception)) {
            showError(getString(R.string.network_error), true);
            return true;
        } else if (exception instanceof ContentNotSupportedException) {
            showError(getString(R.string.content_not_supported), false);
            return true;
        }

        return false;
    }

    public void onReCaptchaException(final ReCaptchaException exception) {
        if (DEBUG) {
            Log.d(TAG, "onReCaptchaException() called");
        }
        Toast.makeText(activity, R.string.recaptcha_request_toast, Toast.LENGTH_LONG).show();
        // Starting ReCaptcha Challenge Activity
        final Intent intent = new Intent(activity, ReCaptchaActivity.class);
        intent.putExtra(ReCaptchaActivity.RECAPTCHA_URL_EXTRA, exception.getUrl());
        startActivityForResult(intent, ReCaptchaActivity.RECAPTCHA_REQUEST);

        showError(getString(R.string.recaptcha_request_toast), false);
    }

    public void onUnrecoverableError(final Throwable exception, final UserAction userAction,
                                     final String serviceName, final String request,
                                     @StringRes final int errorId) {
        onUnrecoverableError(Collections.singletonList(exception), userAction, serviceName,
                request, errorId);
    }

    public void onUnrecoverableError(final List<Throwable> exception, final UserAction userAction,
                                     final String serviceName, final String request,
                                     @StringRes final int errorId) {
        if (DEBUG) {
            Log.d(TAG, "onUnrecoverableError() called with: exception = [" + exception + "]");
        }

        ErrorActivity.reportError(getContext(), exception, MainActivity.class, null,
                ErrorActivity.ErrorInfo.make(userAction, serviceName == null ? "none" : serviceName,
                        request == null ? "none" : request, errorId));
    }

    public void showSnackBarError(final Throwable exception, final UserAction userAction,
                                  final String serviceName, final String request,
                                  @StringRes final int errorId) {
        showSnackBarError(Collections.singletonList(exception), userAction, serviceName, request,
                errorId);
    }

    /**
     * Show a SnackBar and only call
     * {@link ErrorActivity#reportError(Context, List, Class, View, ErrorActivity.ErrorInfo)}
     * IF we a find a valid view (otherwise the error screen appears).
     *
     * @param exception List of the exceptions to show
     * @param userAction The user action that caused the exception
     * @param serviceName The service where the exception happened
     * @param request The page that was requested
     * @param errorId The ID of the error
     */
    public void showSnackBarError(final List<Throwable> exception, final UserAction userAction,
                                  final String serviceName, final String request,
                                  @StringRes final int errorId) {
        if (DEBUG) {
            Log.d(TAG, "showSnackBarError() called with: "
                    + "exception = [" + exception + "], userAction = [" + userAction + "], "
                    + "request = [" + request + "], errorId = [" + errorId + "]");
        }
        View rootView = activity != null ? activity.findViewById(android.R.id.content) : null;
        if (rootView == null && getView() != null) {
            rootView = getView();
        }
        if (rootView == null) {
            return;
        }

        ErrorActivity.reportError(getContext(), exception, MainActivity.class, rootView,
                ErrorActivity.ErrorInfo.make(userAction, serviceName, request, errorId));
    }
}
