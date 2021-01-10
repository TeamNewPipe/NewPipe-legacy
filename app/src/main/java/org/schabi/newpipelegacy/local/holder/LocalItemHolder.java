package org.schabi.newpipelegacy.local.holder;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.schabi.newpipelegacy.database.LocalItem;
import org.schabi.newpipelegacy.local.LocalItemBuilder;
import org.schabi.newpipelegacy.local.history.HistoryRecordManager;

import java.time.format.DateTimeFormatter;

/*
 * Created by Christian Schabesberger on 12.02.17.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * InfoItemHolder.java is part of NewPipe.
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

public abstract class LocalItemHolder extends RecyclerView.ViewHolder {
    protected final LocalItemBuilder itemBuilder;

    public LocalItemHolder(final LocalItemBuilder itemBuilder, final int layoutId,
                           final ViewGroup parent) {
        super(LayoutInflater.from(itemBuilder.getContext()).inflate(layoutId, parent, false));
        this.itemBuilder = itemBuilder;
    }

    public abstract void updateFromItem(LocalItem item, HistoryRecordManager historyRecordManager,
                                        DateTimeFormatter dateTimeFormatter);

    public void updateState(final LocalItem localItem,
                            final HistoryRecordManager historyRecordManager) { }
}
