package org.schabi.newpipelegacy.info_list.holder;

import android.view.ViewGroup;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.info_list.InfoItemBuilder;

public class PlaylistInfoItemHolder extends PlaylistMiniInfoItemHolder {

    public PlaylistInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_playlist_item, parent);
    }
}
