package org.schabi.newpipelegacy.player.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.util.MimeTypes;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.utils.Utils;
import org.schabi.newpipelegacy.player.playqueue.PlayQueue;
import org.schabi.newpipelegacy.player.playqueue.PlayQueueItem;
import org.schabi.newpipelegacy.player.playqueue.SinglePlayQueue;
import org.schabi.newpipelegacy.util.ListHelper;

import java.lang.annotation.Retention;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL;
import static com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;
import static com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static org.schabi.newpipelegacy.player.helper.PlayerHelper.AutoplayType.AUTOPLAY_TYPE_ALWAYS;
import static org.schabi.newpipelegacy.player.helper.PlayerHelper.AutoplayType.AUTOPLAY_TYPE_NEVER;
import static org.schabi.newpipelegacy.player.helper.PlayerHelper.AutoplayType.AUTOPLAY_TYPE_WIFI;
import static org.schabi.newpipelegacy.player.helper.PlayerHelper.MinimizeMode.MINIMIZE_ON_EXIT_MODE_BACKGROUND;
import static org.schabi.newpipelegacy.player.helper.PlayerHelper.MinimizeMode.MINIMIZE_ON_EXIT_MODE_NONE;
import static org.schabi.newpipelegacy.player.helper.PlayerHelper.MinimizeMode.MINIMIZE_ON_EXIT_MODE_POPUP;

public final class PlayerHelper {
    private static final StringBuilder STRING_BUILDER = new StringBuilder();
    private static final Formatter STRING_FORMATTER
            = new Formatter(STRING_BUILDER, Locale.getDefault());
    private static final NumberFormat SPEED_FORMATTER = new DecimalFormat("0.##x");
    private static final NumberFormat PITCH_FORMATTER = new DecimalFormat("##%");

    @Retention(SOURCE)
    @IntDef({AUTOPLAY_TYPE_ALWAYS, AUTOPLAY_TYPE_WIFI,
            AUTOPLAY_TYPE_NEVER})
    public @interface AutoplayType {
        int AUTOPLAY_TYPE_ALWAYS = 0;
        int AUTOPLAY_TYPE_WIFI = 1;
        int AUTOPLAY_TYPE_NEVER = 2;
    }

    private PlayerHelper() { }

    ////////////////////////////////////////////////////////////////////////////
    // Exposed helpers
    ////////////////////////////////////////////////////////////////////////////

    public static String getTimeString(final int milliSeconds) {
        final int seconds = (milliSeconds % 60000) / 1000;
        final int minutes = (milliSeconds % 3600000) / 60000;
        final int hours = (milliSeconds % 86400000) / 3600000;
        final int days = (milliSeconds % (86400000 * 7)) / 86400000;

        STRING_BUILDER.setLength(0);
        return days > 0
                ? STRING_FORMATTER.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds)
                .toString()
                : hours > 0
                ? STRING_FORMATTER.format("%d:%02d:%02d", hours, minutes, seconds).toString()
                : STRING_FORMATTER.format("%02d:%02d", minutes, seconds).toString();
    }

    public static String formatSpeed(final double speed) {
        return SPEED_FORMATTER.format(speed);
    }

    public static String formatPitch(final double pitch) {
        return PITCH_FORMATTER.format(pitch);
    }

    public static String subtitleMimeTypesOf(final MediaFormat format) {
        switch (format) {
            case VTT:
                return MimeTypes.TEXT_VTT;
            case TTML:
                return MimeTypes.APPLICATION_TTML;
            default:
                throw new IllegalArgumentException("Unrecognized mime type: " + format.name());
        }
    }

    @NonNull
    public static String captionLanguageOf(@NonNull final Context context,
                                           @NonNull final SubtitlesStream subtitles) {
        final String displayName = subtitles.getDisplayLanguageName();
        return displayName + (subtitles.isAutoGenerated()
                ? " (" + context.getString(R.string.caption_auto_generated) + ")" : "");
    }

    @NonNull
    public static String resizeTypeOf(@NonNull final Context context,
                                      @AspectRatioFrameLayout.ResizeMode final int resizeMode) {
        switch (resizeMode) {
            case RESIZE_MODE_FIT:
                return context.getResources().getString(R.string.resize_fit);
            case RESIZE_MODE_FILL:
                return context.getResources().getString(R.string.resize_fill);
            case RESIZE_MODE_ZOOM:
                return context.getResources().getString(R.string.resize_zoom);
            default:
                throw new IllegalArgumentException("Unrecognized resize mode: " + resizeMode);
        }
    }

    @NonNull
    public static String cacheKeyOf(@NonNull final StreamInfo info,
                                    @NonNull final VideoStream video) {
        return info.getUrl() + video.getResolution() + video.getFormat().getName();
    }

    @NonNull
    public static String cacheKeyOf(@NonNull final StreamInfo info,
                                    @NonNull final AudioStream audio) {
        return info.getUrl() + audio.getAverageBitrate() + audio.getFormat().getName();
    }

    /**
     * Given a {@link StreamInfo} and the existing queue items,
     * provide the {@link SinglePlayQueue} consisting of the next video for auto queueing.
     * <p>
     * This method detects and prevents cycles by naively checking
     * if a candidate next video's url already exists in the existing items.
     * </p>
     * <p>
     * The first item in {@link StreamInfo#getRelatedStreams()} is checked first.
     * If it is non-null and is not part of the existing items, it will be used as the next stream.
     * Otherwise, a random item with non-repeating url will be selected
     * from the {@link StreamInfo#getRelatedStreams()}.
     * </p>
     *
     * @param info          currently playing stream
     * @param existingItems existing items in the queue
     * @return {@link SinglePlayQueue} with the next stream to queue
     */
    @Nullable
    public static PlayQueue autoQueueOf(@NonNull final StreamInfo info,
                                        @NonNull final List<PlayQueueItem> existingItems) {
        final Set<String> urls = new HashSet<>(existingItems.size());
        for (final PlayQueueItem item : existingItems) {
            urls.add(item.getUrl());
        }

        final List<InfoItem> relatedItems = info.getRelatedStreams();
        if (Utils.isNullOrEmpty(relatedItems)) {
            return null;
        }

        if (relatedItems.get(0) != null && relatedItems.get(0) instanceof StreamInfoItem
                && !urls.contains(relatedItems.get(0).getUrl())) {
            return getAutoQueuedSinglePlayQueue((StreamInfoItem) relatedItems.get(0));
        }

        final List<StreamInfoItem> autoQueueItems = new ArrayList<>();
        for (final InfoItem item : relatedItems) {
            if (item instanceof StreamInfoItem && !urls.contains(item.getUrl())) {
                autoQueueItems.add((StreamInfoItem) item);
            }
        }

        Collections.shuffle(autoQueueItems);
        return autoQueueItems.isEmpty()
                ? null : getAutoQueuedSinglePlayQueue(autoQueueItems.get(0));
    }

    ////////////////////////////////////////////////////////////////////////////
    // Settings Resolution
    ////////////////////////////////////////////////////////////////////////////

    public static boolean isResumeAfterAudioFocusGain(@NonNull final Context context) {
        return isResumeAfterAudioFocusGain(context, false);
    }

    public static boolean isVolumeGestureEnabled(@NonNull final Context context) {
        return isVolumeGestureEnabled(context, true);
    }

    public static boolean isBrightnessGestureEnabled(@NonNull final Context context) {
        return isBrightnessGestureEnabled(context, true);
    }

    public static boolean isAutoQueueEnabled(@NonNull final Context context) {
        return isAutoQueueEnabled(context, false);
    }

    public static boolean isClearingQueueConfirmationRequired(@NonNull final Context context) {
        return getPreferences(context)
                .getBoolean(context.getString(R.string.clear_queue_confirmation_key), false);
    }

    @MinimizeMode
    public static int getMinimizeOnExitAction(@NonNull final Context context) {
        final String defaultAction = context.getString(R.string.minimize_on_exit_none_key);
        final String popupAction = context.getString(R.string.minimize_on_exit_popup_key);
        final String backgroundAction = context.getString(R.string.minimize_on_exit_background_key);

        final String action = getMinimizeOnExitAction(context, defaultAction);
        if (action.equals(popupAction)) {
            return MINIMIZE_ON_EXIT_MODE_POPUP;
        } else if (action.equals(backgroundAction)) {
            return MINIMIZE_ON_EXIT_MODE_BACKGROUND;
        } else {
            return MINIMIZE_ON_EXIT_MODE_NONE;
        }
    }

    @AutoplayType
    public static int getAutoplayType(@NonNull final Context context) {
        final String type = getAutoplayType(context, context.getString(R.string.autoplay_wifi_key));
        if (type.equals(context.getString(R.string.autoplay_always_key))) {
            return AUTOPLAY_TYPE_ALWAYS;
        } else if (type.equals(context.getString(R.string.autoplay_never_key))) {
            return AUTOPLAY_TYPE_NEVER;
        } else {
            return AUTOPLAY_TYPE_WIFI;
        }
    }

    public static boolean isAutoplayAllowedByUser(@NonNull final Context context) {
        switch (PlayerHelper.getAutoplayType(context)) {
            case PlayerHelper.AutoplayType.AUTOPLAY_TYPE_NEVER:
                return false;
            case PlayerHelper.AutoplayType.AUTOPLAY_TYPE_WIFI:
                return !ListHelper.isMeteredNetwork(context);
            case PlayerHelper.AutoplayType.AUTOPLAY_TYPE_ALWAYS:
            default:
                return true;
        }
    }

    @NonNull
    public static SeekParameters getSeekParameters(@NonNull final Context context) {
        return isUsingInexactSeek(context) ? SeekParameters.CLOSEST_SYNC : SeekParameters.EXACT;
    }

    public static long getPreferredCacheSize() {
        return 64 * 1024 * 1024L;
    }

    public static long getPreferredFileSize() {
        return 512 * 1024L;
    }

    /**
     * @return the number of milliseconds the player buffers for before starting playback
     */
    public static int getPlaybackStartBufferMs() {
        return 500;
    }

    /**
     * @return the minimum number of milliseconds the player always buffers to
     * after starting playback.
     */
    public static int getPlaybackMinimumBufferMs() {
        return 25000;
    }

    /**
     * @return the maximum/optimal number of milliseconds the player will buffer to once the buffer
     * hits the point of {@link #getPlaybackMinimumBufferMs()}.
     */
    public static int getPlaybackOptimalBufferMs() {
        return 60000;
    }

    public static TrackSelection.Factory getQualitySelector(@NonNull final Context context) {
        return new AdaptiveTrackSelection.Factory(
                1000,
                AdaptiveTrackSelection.DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
                AdaptiveTrackSelection.DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
                AdaptiveTrackSelection.DEFAULT_BANDWIDTH_FRACTION);
    }

    public static boolean isUsingDSP(@NonNull final Context context) {
        return true;
    }

    public static int getTossFlingVelocity(@NonNull final Context context) {
        return 2500;
    }

    @NonNull
    public static CaptionStyleCompat getCaptionStyle(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return CaptionStyleCompat.DEFAULT;
        }

        final CaptioningManager captioningManager = ContextCompat.getSystemService(context,
                CaptioningManager.class);
        if (captioningManager == null || !captioningManager.isEnabled()) {
            return CaptionStyleCompat.DEFAULT;
        }

        return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
    }

    /**
     * Get scaling for captions based on system font scaling.
     * <p>Options:</p>
     * <ul>
     *     <li>Very small: 0.25f</li>
     *     <li>Small: 0.5f</li>
     *     <li>Normal: 1.0f</li>
     *     <li>Large: 1.5f</li>
     *     <li>Very large: 2.0f</li>
     * </ul>
     *
     * @param context Android app context
     * @return caption scaling
     */
    public static float getCaptionScale(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return 1f;
        }

        final CaptioningManager captioningManager = ContextCompat.getSystemService(context,
                CaptioningManager.class);
        if (captioningManager == null || !captioningManager.isEnabled()) {
            return 1f;
        }

        return captioningManager.getFontScale();
    }

    public static float getScreenBrightness(@NonNull final Context context) {
        //a value of less than 0, the default, means to use the preferred screen brightness
        return getScreenBrightness(context, -1);
    }

    public static void setScreenBrightness(@NonNull final Context context,
                                           final float setScreenBrightness) {
        setScreenBrightness(context, setScreenBrightness, System.currentTimeMillis());
    }

    public static boolean globalScreenOrientationLocked(final Context context) {
        // 1: Screen orientation changes using accelerometer
        // 0: Screen orientation is locked
        return android.provider.Settings.System.getInt(
                context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 0;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private helpers
    ////////////////////////////////////////////////////////////////////////////

    @NonNull
    private static SharedPreferences getPreferences(@NonNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static boolean isResumeAfterAudioFocusGain(@NonNull final Context context,
                                                       final boolean b) {
        return getPreferences(context)
                .getBoolean(context.getString(R.string.resume_on_audio_focus_gain_key), b);
    }

    private static boolean isVolumeGestureEnabled(@NonNull final Context context,
                                                  final boolean b) {
        return getPreferences(context)
                .getBoolean(context.getString(R.string.volume_gesture_control_key), b);
    }

    private static boolean isBrightnessGestureEnabled(@NonNull final Context context,
                                                      final boolean b) {
        return getPreferences(context)
                .getBoolean(context.getString(R.string.brightness_gesture_control_key), b);
    }

    private static boolean isUsingInexactSeek(@NonNull final Context context) {
        return getPreferences(context)
                .getBoolean(context.getString(R.string.use_inexact_seek_key), false);
    }

    private static boolean isAutoQueueEnabled(@NonNull final Context context, final boolean b) {
        return getPreferences(context).getBoolean(context.getString(R.string.auto_queue_key), b);
    }

    private static void setScreenBrightness(@NonNull final Context context,
                                            final float screenBrightness, final long timestamp) {
        final SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putFloat(context.getString(R.string.screen_brightness_key), screenBrightness);
        editor.putLong(context.getString(R.string.screen_brightness_timestamp_key), timestamp);
        editor.apply();
    }

    private static float getScreenBrightness(@NonNull final Context context,
                                             final float screenBrightness) {
        final SharedPreferences sp = getPreferences(context);
        final long timestamp = sp
                .getLong(context.getString(R.string.screen_brightness_timestamp_key), 0);
        // Hypothesis: 4h covers a viewing block, e.g. evening.
        // External lightning conditions will change in the next
        // viewing block so we fall back to the default brightness
        if ((System.currentTimeMillis() - timestamp) > TimeUnit.HOURS.toMillis(4)) {
            return screenBrightness;
        } else {
            return sp
                    .getFloat(context.getString(R.string.screen_brightness_key), screenBrightness);
        }
    }

    private static String getMinimizeOnExitAction(@NonNull final Context context,
                                                  final String key) {
        return getPreferences(context)
                .getString(context.getString(R.string.minimize_on_exit_key), key);
    }

    private static String getAutoplayType(@NonNull final Context context,
                                                  final String key) {
        return getPreferences(context).getString(context.getString(R.string.autoplay_key),
                key);
    }

    private static SinglePlayQueue getAutoQueuedSinglePlayQueue(
            final StreamInfoItem streamInfoItem) {
        final SinglePlayQueue singlePlayQueue = new SinglePlayQueue(streamInfoItem);
        singlePlayQueue.getItem().setAutoQueued(true);
        return singlePlayQueue;
    }

    @Retention(SOURCE)
    @IntDef({MINIMIZE_ON_EXIT_MODE_NONE, MINIMIZE_ON_EXIT_MODE_BACKGROUND,
            MINIMIZE_ON_EXIT_MODE_POPUP})
    public @interface MinimizeMode {
        int MINIMIZE_ON_EXIT_MODE_NONE = 0;
        int MINIMIZE_ON_EXIT_MODE_BACKGROUND = 1;
        int MINIMIZE_ON_EXIT_MODE_POPUP = 2;
    }
}
