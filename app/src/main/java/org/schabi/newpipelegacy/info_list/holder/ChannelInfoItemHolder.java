package org.schabi.newpipelegacy.info_list.holder;

import android.view.ViewGroup;
import android.widget.TextView;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipelegacy.info_list.InfoItemBuilder;
import org.schabi.newpipelegacy.local.history.HistoryRecordManager;
import org.schabi.newpipelegacy.util.Localization;

/*
 * Created by Christian Schabesberger on 12.02.17.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * ChannelInfoItemHolder .java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class ChannelInfoItemHolder extends ChannelMiniInfoItemHolder {
    private final TextView itemChannelDescriptionView;

    public ChannelInfoItemHolder(final InfoItemBuilder infoItemBuilder, final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_channel_item, parent);
        itemChannelDescriptionView = itemView.findViewById(R.id.itemChannelDescriptionView);
    }

    @Override
    public void updateFromItem(final InfoItem infoItem,
                               final HistoryRecordManager historyRecordManager) {
        super.updateFromItem(infoItem, historyRecordManager);

        if (!(infoItem instanceof ChannelInfoItem)) {
            return;
        }
        final ChannelInfoItem item = (ChannelInfoItem) infoItem;

        itemChannelDescriptionView.setText(item.getDescription());
    }

    @Override
    protected String getDetailLine(final ChannelInfoItem item) {
        String details = super.getDetailLine(item);

        if (item.getStreamCount() >= 0) {
            final String formattedVideoAmount = Localization.localizeStreamCount(
                    itemBuilder.getContext(), item.getStreamCount());

            if (!details.isEmpty()) {
                details += " • " + formattedVideoAmount;
            } else {
                details = formattedVideoAmount;
            }
        }
        return details;
    }
}
