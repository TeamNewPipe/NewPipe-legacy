package org.schabi.newpipelegacy.fragments.list;

import org.schabi.newpipelegacy.fragments.ViewContract;

public interface ListViewContract<I, N> extends ViewContract<I> {
    void showListFooter(boolean show);

    void handleNextItems(N result);
}
