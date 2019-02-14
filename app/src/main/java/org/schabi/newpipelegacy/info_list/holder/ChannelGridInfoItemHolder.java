package org.schabi.newpipelegacy.info_list.holder;

import android.view.ViewGroup;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.info_list.InfoItemBuilder;

public class ChannelGridInfoItemHolder extends ChannelMiniInfoItemHolder {

	public ChannelGridInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
		super(infoItemBuilder, R.layout.list_channel_grid_item, parent);
	}
}
