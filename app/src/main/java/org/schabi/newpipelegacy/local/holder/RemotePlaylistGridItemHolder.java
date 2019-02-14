package org.schabi.newpipelegacy.local.holder;

import android.view.ViewGroup;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.local.LocalItemBuilder;

public class RemotePlaylistGridItemHolder extends RemotePlaylistItemHolder {

	public RemotePlaylistGridItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
		super(infoItemBuilder, R.layout.list_playlist_grid_item, parent);
	}
}
