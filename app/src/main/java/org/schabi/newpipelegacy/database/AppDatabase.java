package org.schabi.newpipelegacy.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import org.schabi.newpipelegacy.database.feed.dao.FeedDAO;
import org.schabi.newpipelegacy.database.feed.dao.FeedGroupDAO;
import org.schabi.newpipelegacy.database.feed.model.FeedEntity;
import org.schabi.newpipelegacy.database.feed.model.FeedGroupEntity;
import org.schabi.newpipelegacy.database.feed.model.FeedGroupSubscriptionEntity;
import org.schabi.newpipelegacy.database.feed.model.FeedLastUpdatedEntity;
import org.schabi.newpipelegacy.database.history.dao.SearchHistoryDAO;
import org.schabi.newpipelegacy.database.history.dao.StreamHistoryDAO;
import org.schabi.newpipelegacy.database.history.model.SearchHistoryEntry;
import org.schabi.newpipelegacy.database.history.model.StreamHistoryEntity;
import org.schabi.newpipelegacy.database.playlist.dao.PlaylistDAO;
import org.schabi.newpipelegacy.database.playlist.dao.PlaylistRemoteDAO;
import org.schabi.newpipelegacy.database.playlist.dao.PlaylistStreamDAO;
import org.schabi.newpipelegacy.database.playlist.model.PlaylistEntity;
import org.schabi.newpipelegacy.database.playlist.model.PlaylistRemoteEntity;
import org.schabi.newpipelegacy.database.playlist.model.PlaylistStreamEntity;
import org.schabi.newpipelegacy.database.stream.dao.StreamDAO;
import org.schabi.newpipelegacy.database.stream.dao.StreamStateDAO;
import org.schabi.newpipelegacy.database.stream.model.StreamEntity;
import org.schabi.newpipelegacy.database.stream.model.StreamStateEntity;
import org.schabi.newpipelegacy.database.subscription.SubscriptionDAO;
import org.schabi.newpipelegacy.database.subscription.SubscriptionEntity;

import static org.schabi.newpipelegacy.database.Migrations.DB_VER_3;

@TypeConverters({Converters.class})
@Database(
        entities = {
                SubscriptionEntity.class, SearchHistoryEntry.class,
                StreamEntity.class, StreamHistoryEntity.class, StreamStateEntity.class,
                PlaylistEntity.class, PlaylistStreamEntity.class, PlaylistRemoteEntity.class,
                FeedEntity.class, FeedGroupEntity.class, FeedGroupSubscriptionEntity.class,
                FeedLastUpdatedEntity.class
        },
        version = DB_VER_3
)
public abstract class AppDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "newpipe.db";

    public abstract SearchHistoryDAO searchHistoryDAO();

    public abstract StreamDAO streamDAO();

    public abstract StreamHistoryDAO streamHistoryDAO();

    public abstract StreamStateDAO streamStateDAO();

    public abstract PlaylistDAO playlistDAO();

    public abstract PlaylistStreamDAO playlistStreamDAO();

    public abstract PlaylistRemoteDAO playlistRemoteDAO();

    public abstract FeedDAO feedDAO();

    public abstract FeedGroupDAO feedGroupDAO();

    public abstract SubscriptionDAO subscriptionDAO();
}
