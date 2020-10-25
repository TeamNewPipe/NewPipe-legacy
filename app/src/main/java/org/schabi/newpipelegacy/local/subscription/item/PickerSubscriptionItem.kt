package org.schabi.newpipelegacy.local.subscription.item

import android.view.View
import androidx.core.view.isVisible
import com.nostra13.universalimageloader.core.ImageLoader
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import kotlinx.android.synthetic.main.picker_subscription_item.*
import kotlinx.android.synthetic.main.picker_subscription_item.view.*
import org.schabi.newpipelegacy.R
import org.schabi.newpipelegacy.database.subscription.SubscriptionEntity
import org.schabi.newpipelegacy.util.AnimationUtils
import org.schabi.newpipelegacy.util.AnimationUtils.animateView
import org.schabi.newpipelegacy.util.ImageDisplayConstants

data class PickerSubscriptionItem(
    val subscriptionEntity: SubscriptionEntity,
    var isSelected: Boolean = false
) : Item() {
    override fun getId(): Long = subscriptionEntity.uid
    override fun getLayout(): Int = R.layout.picker_subscription_item
    override fun getSpanSize(spanCount: Int, position: Int): Int = 1

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        ImageLoader.getInstance().displayImage(subscriptionEntity.avatarUrl,
                viewHolder.thumbnail_view, ImageDisplayConstants.DISPLAY_AVATAR_OPTIONS)

        viewHolder.title_view.text = subscriptionEntity.name
        viewHolder.selected_highlight.isVisible = isSelected
    }

    override fun unbind(viewHolder: GroupieViewHolder) {
        super.unbind(viewHolder)

        viewHolder.selected_highlight.animate().setListener(null).cancel()
        viewHolder.selected_highlight.visibility = View.GONE
        viewHolder.selected_highlight.alpha = 1F
    }

    fun updateSelected(containerView: View, isSelected: Boolean) {
        this.isSelected = isSelected
        animateView(containerView.selected_highlight,
                AnimationUtils.Type.LIGHT_SCALE_AND_ALPHA, isSelected, 150)
    }
}
