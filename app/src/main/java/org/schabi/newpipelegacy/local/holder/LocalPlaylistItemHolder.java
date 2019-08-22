package org.schabi.newpipelegacy.local.holder;

import android.view.View;
import android.view.ViewGroup;

import org.schabi.newpipelegacy.database.LocalItem;
import org.schabi.newpipelegacy.database.playlist.PlaylistMetadataEntry;
import org.schabi.newpipelegacy.local.LocalItemBuilder;
import org.schabi.newpipelegacy.local.history.HistoryRecordManager;
import org.schabi.newpipelegacy.util.ImageDisplayConstants;

import java.text.DateFormat;

public class LocalPlaylistItemHolder extends PlaylistItemHolder {

    public LocalPlaylistItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
        super(infoItemBuilder, parent);
    }

    LocalPlaylistItemHolder(LocalItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem, HistoryRecordManager historyRecordManager, final DateFormat dateFormat) {
        if (!(localItem instanceof PlaylistMetadataEntry)) return;
        final PlaylistMetadataEntry item = (PlaylistMetadataEntry) localItem;

        itemTitleView.setText(item.name);
        itemStreamCountView.setText(String.valueOf(item.streamCount));
        itemUploaderView.setVisibility(View.INVISIBLE);

        itemBuilder.displayImage(item.thumbnailUrl, itemThumbnailView,
                ImageDisplayConstants.DISPLAY_PLAYLIST_OPTIONS);

        super.updateFromItem(localItem, historyRecordManager, dateFormat);
    }
}
