package org.schabi.newpipelegacy.local;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.schabi.newpipelegacy.database.LocalItem;
import org.schabi.newpipelegacy.database.stream.model.StreamStateEntity;
import org.schabi.newpipelegacy.local.history.HistoryRecordManager;
import org.schabi.newpipelegacy.local.holder.LocalItemHolder;
import org.schabi.newpipelegacy.local.holder.LocalPlaylistGridItemHolder;
import org.schabi.newpipelegacy.local.holder.LocalPlaylistItemHolder;
import org.schabi.newpipelegacy.local.holder.LocalPlaylistStreamGridItemHolder;
import org.schabi.newpipelegacy.local.holder.LocalPlaylistStreamItemHolder;
import org.schabi.newpipelegacy.local.holder.LocalStatisticStreamGridItemHolder;
import org.schabi.newpipelegacy.local.holder.LocalStatisticStreamItemHolder;
import org.schabi.newpipelegacy.local.holder.RemotePlaylistGridItemHolder;
import org.schabi.newpipelegacy.local.holder.RemotePlaylistItemHolder;
import org.schabi.newpipelegacy.util.FallbackViewHolder;
import org.schabi.newpipelegacy.util.Localization;
import org.schabi.newpipelegacy.util.OnClickGesture;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/*
 * Created by Christian Schabesberger on 01.08.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * InfoListAdapter.java is part of NewPipe.
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

public class LocalItemListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = LocalItemListAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int HEADER_TYPE = 0;
    private static final int FOOTER_TYPE = 1;

    private static final int STREAM_STATISTICS_HOLDER_TYPE = 0x1000;
    private static final int STREAM_PLAYLIST_HOLDER_TYPE = 0x1001;
    private static final int STREAM_STATISTICS_GRID_HOLDER_TYPE = 0x1002;
    private static final int STREAM_PLAYLIST_GRID_HOLDER_TYPE = 0x1004;
    private static final int LOCAL_PLAYLIST_HOLDER_TYPE = 0x2000;
    private static final int REMOTE_PLAYLIST_HOLDER_TYPE = 0x2001;
    private static final int LOCAL_PLAYLIST_GRID_HOLDER_TYPE = 0x2002;
    private static final int REMOTE_PLAYLIST_GRID_HOLDER_TYPE = 0x2004;

    private final LocalItemBuilder localItemBuilder;
    private final ArrayList<LocalItem> localItems;
    private final HistoryRecordManager recordManager;
    private final DateFormat dateFormat;

    private boolean showFooter = false;
    private boolean useGridVariant = false;
    private View header = null;
    private View footer = null;

    public LocalItemListAdapter(final Context context) {
        recordManager = new HistoryRecordManager(context);
        localItemBuilder = new LocalItemBuilder(context);
        localItems = new ArrayList<>();
        dateFormat = DateFormat.getDateInstance(DateFormat.SHORT,
                Localization.getPreferredLocale(context));
    }

    public void setSelectedListener(final OnClickGesture<LocalItem> listener) {
        localItemBuilder.setOnItemSelectedListener(listener);
    }

    public void unsetSelectedListener() {
        localItemBuilder.setOnItemSelectedListener(null);
    }

    public void addItems(@Nullable final List<? extends LocalItem> data) {
        if (data == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "addItems() before > localItems.size() = "
                    + localItems.size() + ", data.size() = " + data.size());
        }

        final int offsetStart = sizeConsideringHeader();
        localItems.addAll(data);

        if (DEBUG) {
            Log.d(TAG, "addItems() after > offsetStart = " + offsetStart + ", "
                    + "localItems.size() = " + localItems.size() + ", "
                    + "header = " + header + ", footer = " + footer + ", "
                    + "showFooter = " + showFooter);
        }
        notifyItemRangeInserted(offsetStart, data.size());

        if (footer != null && showFooter) {
            final int footerNow = sizeConsideringHeader();
            notifyItemMoved(offsetStart, footerNow);

            if (DEBUG) {
                Log.d(TAG, "addItems() footer from " + offsetStart
                        + " to " + footerNow);
            }
        }
    }

    public void removeItem(final LocalItem data) {
        final int index = localItems.indexOf(data);
        localItems.remove(index);
        notifyItemRemoved(index + (header != null ? 1 : 0));
    }

    public boolean swapItems(final int fromAdapterPosition, final int toAdapterPosition) {
        final int actualFrom = adapterOffsetWithoutHeader(fromAdapterPosition);
        final int actualTo = adapterOffsetWithoutHeader(toAdapterPosition);

        if (actualFrom < 0 || actualTo < 0) {
            return false;
        }
        if (actualFrom >= localItems.size() || actualTo >= localItems.size()) {
            return false;
        }

        localItems.add(actualTo, localItems.remove(actualFrom));
        notifyItemMoved(fromAdapterPosition, toAdapterPosition);
        return true;
    }

    public void clearStreamItemList() {
        if (localItems.isEmpty()) {
            return;
        }
        localItems.clear();
        notifyDataSetChanged();
    }

    public void setUseGridVariant(final boolean useGridVariant) {
        this.useGridVariant = useGridVariant;
    }

    public void setHeader(final View header) {
        final boolean changed = header != this.header;
        this.header = header;
        if (changed) {
            notifyDataSetChanged();
        }
    }

    public void setFooter(final View view) {
        this.footer = view;
    }

    public void showFooter(final boolean show) {
        if (DEBUG) {
            Log.d(TAG, "showFooter() called with: show = [" + show + "]");
        }
        if (show == showFooter) {
            return;
        }

        showFooter = show;
        if (show) {
            notifyItemInserted(sizeConsideringHeader());
        } else {
            notifyItemRemoved(sizeConsideringHeader());
        }
    }

    private int adapterOffsetWithoutHeader(final int offset) {
        return offset - (header != null ? 1 : 0);
    }

    private int sizeConsideringHeader() {
        return localItems.size() + (header != null ? 1 : 0);
    }

    public ArrayList<LocalItem> getItemsList() {
        return localItems;
    }

    @Override
    public int getItemCount() {
        int count = localItems.size();
        if (header != null) {
            count++;
        }
        if (footer != null && showFooter) {
            count++;
        }

        if (DEBUG) {
            Log.d(TAG, "getItemCount() called, count = " + count + ", "
                    + "localItems.size() = " + localItems.size() + ", "
                    + "header = " + header + ", footer = " + footer + ", "
                    + "showFooter = " + showFooter);
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if (DEBUG) {
            Log.d(TAG, "getItemViewType() called with: position = [" + position + "]");
        }

        if (header != null && position == 0) {
            return HEADER_TYPE;
        } else if (header != null) {
            position--;
        }
        if (footer != null && position == localItems.size() && showFooter) {
            return FOOTER_TYPE;
        }
        final LocalItem item = localItems.get(position);

        switch (item.getLocalItemType()) {
            case PLAYLIST_LOCAL_ITEM:
                return useGridVariant
                        ? LOCAL_PLAYLIST_GRID_HOLDER_TYPE : LOCAL_PLAYLIST_HOLDER_TYPE;
            case PLAYLIST_REMOTE_ITEM:
                return useGridVariant
                        ? REMOTE_PLAYLIST_GRID_HOLDER_TYPE : REMOTE_PLAYLIST_HOLDER_TYPE;

            case PLAYLIST_STREAM_ITEM:
                return useGridVariant
                        ? STREAM_PLAYLIST_GRID_HOLDER_TYPE : STREAM_PLAYLIST_HOLDER_TYPE;
            case STATISTIC_STREAM_ITEM:
                return useGridVariant
                        ? STREAM_STATISTICS_GRID_HOLDER_TYPE : STREAM_STATISTICS_HOLDER_TYPE;
            default:
                Log.e(TAG, "No holder type has been considered for item: ["
                        + item.getLocalItemType() + "]");
                return -1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                                      final int type) {
        if (DEBUG) {
            Log.d(TAG, "onCreateViewHolder() called with: "
                    + "parent = [" + parent + "], type = [" + type + "]");
        }
        switch (type) {
            case HEADER_TYPE:
                return new HeaderFooterHolder(header);
            case FOOTER_TYPE:
                return new HeaderFooterHolder(footer);
            case LOCAL_PLAYLIST_HOLDER_TYPE:
                return new LocalPlaylistItemHolder(localItemBuilder, parent);
            case LOCAL_PLAYLIST_GRID_HOLDER_TYPE:
                return new LocalPlaylistGridItemHolder(localItemBuilder, parent);
            case REMOTE_PLAYLIST_HOLDER_TYPE:
                return new RemotePlaylistItemHolder(localItemBuilder, parent);
            case REMOTE_PLAYLIST_GRID_HOLDER_TYPE:
                return new RemotePlaylistGridItemHolder(localItemBuilder, parent);
            case STREAM_PLAYLIST_HOLDER_TYPE:
                return new LocalPlaylistStreamItemHolder(localItemBuilder, parent);
            case STREAM_PLAYLIST_GRID_HOLDER_TYPE:
                return new LocalPlaylistStreamGridItemHolder(localItemBuilder, parent);
            case STREAM_STATISTICS_HOLDER_TYPE:
                return new LocalStatisticStreamItemHolder(localItemBuilder, parent);
            case STREAM_STATISTICS_GRID_HOLDER_TYPE:
                return new LocalStatisticStreamGridItemHolder(localItemBuilder, parent);
            default:
                Log.e(TAG, "No view type has been considered for holder: [" + type + "]");
                return new FallbackViewHolder(new View(parent.getContext()));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        if (DEBUG) {
            Log.d(TAG, "onBindViewHolder() called with: "
                    + "holder = [" + holder.getClass().getSimpleName() + "], "
                    + "position = [" + position + "]");
        }

        if (holder instanceof LocalItemHolder) {
            // If header isn't null, offset the items by -1
            if (header != null) {
                position--;
            }

            ((LocalItemHolder) holder)
                    .updateFromItem(localItems.get(position), recordManager, dateFormat);
        } else if (holder instanceof HeaderFooterHolder && position == 0 && header != null) {
            ((HeaderFooterHolder) holder).view = header;
        } else if (holder instanceof HeaderFooterHolder && position == sizeConsideringHeader()
                && footer != null && showFooter) {
            ((HeaderFooterHolder) holder).view = footer;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position,
                                 @NonNull final List<Object> payloads) {
        if (!payloads.isEmpty() && holder instanceof LocalItemHolder) {
            for (final Object payload : payloads) {
                if (payload instanceof StreamStateEntity) {
                    ((LocalItemHolder) holder).updateState(localItems
                            .get(header == null ? position : position - 1), recordManager);
                } else if (payload instanceof Boolean) {
                    ((LocalItemHolder) holder).updateState(localItems
                            .get(header == null ? position : position - 1), recordManager);
                }
            }
        } else {
            onBindViewHolder(holder, position);
        }
    }

    public GridLayoutManager.SpanSizeLookup getSpanSizeLookup(final int spanCount) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int position) {
                final int type = getItemViewType(position);
                return type == HEADER_TYPE || type == FOOTER_TYPE ? spanCount : 1;
            }
        };
    }
}
