package org.schabi.newpipelegacy.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

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

import static org.schabi.newpipelegacy.database.Migrations.DB_VER_12_0;

@TypeConverters({Converters.class})
@Database(
        entities = {
                SubscriptionEntity.class, SearchHistoryEntry.class,
                StreamEntity.class, StreamHistoryEntity.class, StreamStateEntity.class,
                PlaylistEntity.class, PlaylistStreamEntity.class, PlaylistRemoteEntity.class
        },
        version = DB_VER_12_0,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "newpipe.db";

    public abstract SubscriptionDAO subscriptionDAO();

    public abstract SearchHistoryDAO searchHistoryDAO();

    public abstract StreamDAO streamDAO();

    public abstract StreamHistoryDAO streamHistoryDAO();

    public abstract StreamStateDAO streamStateDAO();

    public abstract PlaylistDAO playlistDAO();

    public abstract PlaylistStreamDAO playlistStreamDAO();

    public abstract PlaylistRemoteDAO playlistRemoteDAO();
}
