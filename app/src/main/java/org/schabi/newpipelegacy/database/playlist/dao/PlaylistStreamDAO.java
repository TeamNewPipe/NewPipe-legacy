package org.schabi.newpipelegacy.database.playlist.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import org.schabi.newpipelegacy.database.BasicDAO;
import org.schabi.newpipelegacy.database.playlist.PlaylistMetadataEntry;
import org.schabi.newpipelegacy.database.playlist.PlaylistStreamEntry;
import org.schabi.newpipelegacy.database.playlist.model.PlaylistStreamEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;

import static org.schabi.newpipelegacy.database.playlist.PlaylistMetadataEntry.PLAYLIST_STREAM_COUNT;
import static org.schabi.newpipelegacy.database.playlist.model.PlaylistEntity.PLAYLIST_ID;
import static org.schabi.newpipelegacy.database.playlist.model.PlaylistEntity.PLAYLIST_NAME;
import static org.schabi.newpipelegacy.database.playlist.model.PlaylistEntity.PLAYLIST_TABLE;
import static org.schabi.newpipelegacy.database.playlist.model.PlaylistEntity.PLAYLIST_THUMBNAIL_URL;
import static org.schabi.newpipelegacy.database.playlist.model.PlaylistStreamEntity.JOIN_INDEX;
import static org.schabi.newpipelegacy.database.playlist.model.PlaylistStreamEntity.JOIN_PLAYLIST_ID;
import static org.schabi.newpipelegacy.database.playlist.model.PlaylistStreamEntity.JOIN_STREAM_ID;
import static org.schabi.newpipelegacy.database.playlist.model.PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE;
import static org.schabi.newpipelegacy.database.stream.model.StreamEntity.STREAM_ID;
import static org.schabi.newpipelegacy.database.stream.model.StreamEntity.STREAM_TABLE;
import static org.schabi.newpipelegacy.database.stream.model.StreamStateEntity.JOIN_STREAM_ID_ALIAS;
import static org.schabi.newpipelegacy.database.stream.model.StreamStateEntity.STREAM_PROGRESS_TIME;
import static org.schabi.newpipelegacy.database.stream.model.StreamStateEntity.STREAM_STATE_TABLE;

@Dao
public abstract class PlaylistStreamDAO implements BasicDAO<PlaylistStreamEntity> {
    @Override
    @Query("SELECT * FROM " + PLAYLIST_STREAM_JOIN_TABLE)
    public abstract Flowable<List<PlaylistStreamEntity>> getAll();

    @Override
    @Query("DELETE FROM " + PLAYLIST_STREAM_JOIN_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<PlaylistStreamEntity>> listByService(final int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("DELETE FROM " + PLAYLIST_STREAM_JOIN_TABLE
            + " WHERE " + JOIN_PLAYLIST_ID + " = :playlistId")
    public abstract void deleteBatch(long playlistId);

    @Query("SELECT COALESCE(MAX(" + JOIN_INDEX + "), -1)"
            + " FROM " + PLAYLIST_STREAM_JOIN_TABLE
            + " WHERE " + JOIN_PLAYLIST_ID + " = :playlistId")
    public abstract Flowable<Integer> getMaximumIndexOf(long playlistId);

    @Transaction
    @Query("SELECT * FROM " + STREAM_TABLE + " INNER JOIN "
            // get ids of streams of the given playlist
            + "(SELECT " + JOIN_STREAM_ID + "," + JOIN_INDEX
            + " FROM " + PLAYLIST_STREAM_JOIN_TABLE
            + " WHERE " + JOIN_PLAYLIST_ID + " = :playlistId)"

            // then merge with the stream metadata
            + " ON " + STREAM_ID + " = " + JOIN_STREAM_ID

            + " LEFT JOIN "
            + "(SELECT " + JOIN_STREAM_ID + " AS " + JOIN_STREAM_ID_ALIAS + ", "
            +  STREAM_PROGRESS_TIME
            + " FROM " + STREAM_STATE_TABLE + " )"
            + " ON " + STREAM_ID + " = " + JOIN_STREAM_ID_ALIAS

            + " ORDER BY " + JOIN_INDEX + " ASC")
    public abstract Flowable<List<PlaylistStreamEntry>> getOrderedStreamsOf(long playlistId);

    @Transaction
    @Query("SELECT " + PLAYLIST_ID + ", " + PLAYLIST_NAME + ", " + PLAYLIST_THUMBNAIL_URL + ", "
            + "COALESCE(COUNT(" + JOIN_PLAYLIST_ID + "), 0) AS " + PLAYLIST_STREAM_COUNT

            + " FROM " + PLAYLIST_TABLE
            + " LEFT JOIN " + PLAYLIST_STREAM_JOIN_TABLE
            + " ON " + PLAYLIST_ID + " = " + JOIN_PLAYLIST_ID
            + " GROUP BY " + JOIN_PLAYLIST_ID
            + " ORDER BY " + PLAYLIST_NAME + " COLLATE NOCASE ASC")
    public abstract Flowable<List<PlaylistMetadataEntry>> getPlaylistMetadata();
}
