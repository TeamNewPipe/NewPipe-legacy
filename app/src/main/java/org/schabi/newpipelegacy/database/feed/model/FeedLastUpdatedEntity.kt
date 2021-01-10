package org.schabi.newpipelegacy.database.feed.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.schabi.newpipelegacy.database.feed.model.FeedLastUpdatedEntity.Companion.FEED_LAST_UPDATED_TABLE
import org.schabi.newpipelegacy.database.feed.model.FeedLastUpdatedEntity.Companion.SUBSCRIPTION_ID
import org.schabi.newpipelegacy.database.subscription.SubscriptionEntity
import java.time.OffsetDateTime

@Entity(
    tableName = FEED_LAST_UPDATED_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionEntity::class,
            parentColumns = [SubscriptionEntity.SUBSCRIPTION_UID],
            childColumns = [SUBSCRIPTION_ID],
            onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE, deferred = true
        )
    ]
)
data class FeedLastUpdatedEntity(
    @PrimaryKey
    @ColumnInfo(name = SUBSCRIPTION_ID)
    var subscriptionId: Long,

    @ColumnInfo(name = LAST_UPDATED)
    var lastUpdated: OffsetDateTime? = null
) {
    companion object {
        const val FEED_LAST_UPDATED_TABLE = "feed_last_updated"

        const val SUBSCRIPTION_ID = "subscription_id"
        const val LAST_UPDATED = "last_updated"
    }
}
