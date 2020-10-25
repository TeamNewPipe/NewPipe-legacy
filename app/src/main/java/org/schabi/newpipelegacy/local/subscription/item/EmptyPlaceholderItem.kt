package org.schabi.newpipelegacy.local.subscription.item

import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import org.schabi.newpipelegacy.R

class EmptyPlaceholderItem : Item() {
    override fun getLayout(): Int = R.layout.list_empty_view
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {}
    override fun getSpanSize(spanCount: Int, position: Int): Int = spanCount
}
