package org.schabi.newpipelegacy.local.holder;

import android.view.ViewGroup;

import org.schabi.newpipelegacy.database.LocalItem;
import org.schabi.newpipelegacy.database.playlist.model.PlaylistRemoteEntity;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipelegacy.local.LocalItemBuilder;
import org.schabi.newpipelegacy.util.ImageDisplayConstants;
import org.schabi.newpipelegacy.util.Localization;

import java.text.DateFormat;

public class RemotePlaylistItemHolder extends PlaylistItemHolder {
    public RemotePlaylistItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
        super(infoItemBuilder, parent);
    }

    RemotePlaylistItemHolder(LocalItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
        if (!(localItem instanceof PlaylistRemoteEntity)) return;
        final PlaylistRemoteEntity item = (PlaylistRemoteEntity) localItem;

        itemTitleView.setText(item.getName());
        itemStreamCountView.setText(String.valueOf(item.getStreamCount()));
        itemUploaderView.setText(Localization.concatenateStrings(item.getUploader(),
                NewPipe.getNameOfService(item.getServiceId())));

        itemBuilder.displayImage(item.getThumbnailUrl(), itemThumbnailView,
                ImageDisplayConstants.DISPLAY_PLAYLIST_OPTIONS);

        super.updateFromItem(localItem, dateFormat);
    }
}
