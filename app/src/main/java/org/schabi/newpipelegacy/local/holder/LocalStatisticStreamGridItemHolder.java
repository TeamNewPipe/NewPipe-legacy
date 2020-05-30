package org.schabi.newpipelegacy.local.holder;

import android.view.ViewGroup;

import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.local.LocalItemBuilder;

public class LocalStatisticStreamGridItemHolder extends LocalStatisticStreamItemHolder {
    public LocalStatisticStreamGridItemHolder(final LocalItemBuilder infoItemBuilder,
                                              final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_stream_grid_item, parent);
    }
}
