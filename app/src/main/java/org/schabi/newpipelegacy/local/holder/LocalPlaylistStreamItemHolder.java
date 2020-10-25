package org.schabi.newpipelegacy.local.holder;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.database.LocalItem;
import org.schabi.newpipelegacy.database.playlist.PlaylistStreamEntry;
import org.schabi.newpipelegacy.database.stream.model.StreamStateEntity;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipelegacy.local.LocalItemBuilder;
import org.schabi.newpipelegacy.local.history.HistoryRecordManager;
import org.schabi.newpipelegacy.util.AnimationUtils;
import org.schabi.newpipelegacy.util.ImageDisplayConstants;
import org.schabi.newpipelegacy.util.Localization;
import org.schabi.newpipelegacy.views.AnimatedProgressBar;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class LocalPlaylistStreamItemHolder extends LocalItemHolder {
    public final ImageView itemThumbnailView;
    public final TextView itemVideoTitleView;
    private final TextView itemAdditionalDetailsView;
    public final TextView itemDurationView;
    private final View itemHandleView;
    private final AnimatedProgressBar itemProgressView;

    LocalPlaylistStreamItemHolder(final LocalItemBuilder infoItemBuilder, final int layoutId,
                                  final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
        itemVideoTitleView = itemView.findViewById(R.id.itemVideoTitleView);
        itemAdditionalDetailsView = itemView.findViewById(R.id.itemAdditionalDetails);
        itemDurationView = itemView.findViewById(R.id.itemDurationView);
        itemHandleView = itemView.findViewById(R.id.itemHandle);
        itemProgressView = itemView.findViewById(R.id.itemProgressView);
    }

    public LocalPlaylistStreamItemHolder(final LocalItemBuilder infoItemBuilder,
                                         final ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_stream_playlist_item, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem,
                               final HistoryRecordManager historyRecordManager,
                               final DateFormat dateFormat) {
        if (!(localItem instanceof PlaylistStreamEntry)) {
            return;
        }
        final PlaylistStreamEntry item = (PlaylistStreamEntry) localItem;

        itemVideoTitleView.setText(item.getStreamEntity().getTitle());
        itemAdditionalDetailsView.setText(Localization
                .concatenateStrings(item.getStreamEntity().getUploader(),
                        NewPipe.getNameOfService(item.getStreamEntity().getServiceId())));

        if (item.getStreamEntity().getDuration() > 0) {
            itemDurationView.setText(Localization
                    .getDurationString(item.getStreamEntity().getDuration()));
            itemDurationView.setBackgroundColor(ContextCompat.getColor(itemBuilder.getContext(),
                    R.color.duration_background_color));
            itemDurationView.setVisibility(View.VISIBLE);

            final StreamStateEntity state = historyRecordManager
                    .loadLocalStreamStateBatch(new ArrayList<LocalItem>() {{
                add(localItem);
            }}).blockingGet().get(0);
            if (state != null) {
                itemProgressView.setVisibility(View.VISIBLE);
                itemProgressView.setMax((int) item.getStreamEntity().getDuration());
                itemProgressView.setProgress((int) TimeUnit.MILLISECONDS
                        .toSeconds(state.getProgressTime()));
            } else {
                itemProgressView.setVisibility(View.GONE);
            }
        } else {
            itemDurationView.setVisibility(View.GONE);
        }

        // Default thumbnail is shown on error, while loading and if the url is empty
        itemBuilder.displayImage(item.getStreamEntity().getThumbnailUrl(), itemThumbnailView,
                ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS);

        itemView.setOnClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().selected(item);
            }
        });

        itemView.setLongClickable(true);
        itemView.setOnLongClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().held(item);
            }
            return true;
        });

        itemHandleView.setOnTouchListener(getOnTouchListener(item));
    }

    @Override
    public void updateState(final LocalItem localItem,
                            final HistoryRecordManager historyRecordManager) {
        if (!(localItem instanceof PlaylistStreamEntry)) {
            return;
        }
        final PlaylistStreamEntry item = (PlaylistStreamEntry) localItem;

        final StreamStateEntity state = historyRecordManager
                .loadLocalStreamStateBatch(new ArrayList<LocalItem>() {{
            add(localItem);
        }}).blockingGet().get(0);
        if (state != null && item.getStreamEntity().getDuration() > 0) {
            itemProgressView.setMax((int) item.getStreamEntity().getDuration());
            if (itemProgressView.getVisibility() == View.VISIBLE) {
                itemProgressView.setProgressAnimated((int) TimeUnit.MILLISECONDS
                        .toSeconds(state.getProgressTime()));
            } else {
                itemProgressView.setProgress((int) TimeUnit.MILLISECONDS
                        .toSeconds(state.getProgressTime()));
                AnimationUtils.animateView(itemProgressView, true, 500);
            }
        } else if (itemProgressView.getVisibility() == View.VISIBLE) {
            AnimationUtils.animateView(itemProgressView, false, 500);
        }
    }

    private View.OnTouchListener getOnTouchListener(final PlaylistStreamEntry item) {
        return (view, motionEvent) -> {
            view.performClick();
            if (itemBuilder != null && itemBuilder.getOnItemSelectedListener() != null
                    && motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                itemBuilder.getOnItemSelectedListener().drag(item,
                        LocalPlaylistStreamItemHolder.this);
            }
            return false;
        };
    }
}
