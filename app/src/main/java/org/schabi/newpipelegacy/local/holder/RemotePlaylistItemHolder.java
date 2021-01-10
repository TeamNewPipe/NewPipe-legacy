package org.schabi.newpipelegacy.local.holder;

import android.text.TextUtils;
import android.view.ViewGroup;

import org.schabi.newpipelegacy.database.LocalItem;
import org.schabi.newpipelegacy.database.playlist.model.PlaylistRemoteEntity;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipelegacy.local.LocalItemBuilder;
import org.schabi.newpipelegacy.local.history.HistoryRecordManager;
import org.schabi.newpipelegacy.util.ImageDisplayConstants;
import org.schabi.newpipelegacy.util.Localization;

import java.time.format.DateTimeFormatter;

public class RemotePlaylistItemHolder extends PlaylistItemHolder {
    public RemotePlaylistItemHolder(final LocalItemBuilder infoItemBuilder,
                                    final ViewGroup parent) {
        super(infoItemBuilder, parent);
    }

    RemotePlaylistItemHolder(final LocalItemBuilder infoItemBuilder, final int layoutId,
                             final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem,
                               final HistoryRecordManager historyRecordManager,
                               final DateTimeFormatter dateTimeFormatter) {
        if (!(localItem instanceof PlaylistRemoteEntity)) {
            return;
        }
        final PlaylistRemoteEntity item = (PlaylistRemoteEntity) localItem;

        itemTitleView.setText(item.getName());
        itemStreamCountView.setText(Localization.localizeStreamCountMini(
                itemStreamCountView.getContext(), item.getStreamCount()));
        // Here is where the uploader name is set in the bookmarked playlists library
        if (!TextUtils.isEmpty(item.getUploader())) {
            itemUploaderView.setText(Localization.concatenateStrings(item.getUploader(),
                    NewPipe.getNameOfService(item.getServiceId())));
        } else {
            itemUploaderView.setText(NewPipe.getNameOfService(item.getServiceId()));
        }


        itemBuilder.displayImage(item.getThumbnailUrl(), itemThumbnailView,
                ImageDisplayConstants.DISPLAY_PLAYLIST_OPTIONS);

        super.updateFromItem(localItem, historyRecordManager, dateTimeFormatter);
    }
}
