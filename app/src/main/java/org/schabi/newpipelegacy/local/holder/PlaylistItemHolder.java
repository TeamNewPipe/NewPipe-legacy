package org.schabi.newpipelegacy.local.holder;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.database.LocalItem;
import org.schabi.newpipelegacy.local.LocalItemBuilder;
import org.schabi.newpipelegacy.local.history.HistoryRecordManager;

import java.time.format.DateTimeFormatter;

public abstract class PlaylistItemHolder extends LocalItemHolder {
    public final ImageView itemThumbnailView;
    final TextView itemStreamCountView;
    public final TextView itemTitleView;
    public final TextView itemUploaderView;

    public PlaylistItemHolder(final LocalItemBuilder infoItemBuilder, final int layoutId,
                              final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
        itemTitleView = itemView.findViewById(R.id.itemTitleView);
        itemStreamCountView = itemView.findViewById(R.id.itemStreamCountView);
        itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
    }

    public PlaylistItemHolder(final LocalItemBuilder infoItemBuilder, final ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_playlist_mini_item, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem,
                               final HistoryRecordManager historyRecordManager,
                               final DateTimeFormatter dateTimeFormatter) {
        itemView.setOnClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().selected(localItem);
            }
        });

        itemView.setLongClickable(true);
        itemView.setOnLongClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().held(localItem);
            }
            return true;
        });
    }
}
