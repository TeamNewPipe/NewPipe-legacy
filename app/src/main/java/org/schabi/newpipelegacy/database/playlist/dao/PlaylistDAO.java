package org.schabi.newpipelegacy.database.playlist.dao;

import androidx.room.Dao;
import androidx.room.Query;

import org.schabi.newpipelegacy.database.BasicDAO;
import org.schabi.newpipelegacy.database.playlist.model.PlaylistEntity;

import java.util.List;

import io.reactivex.Flowable;

import static org.schabi.newpipelegacy.database.playlist.model.PlaylistEntity.PLAYLIST_ID;
import static org.schabi.newpipelegacy.database.playlist.model.PlaylistEntity.PLAYLIST_TABLE;

@Dao
public abstract class PlaylistDAO implements BasicDAO<PlaylistEntity> {
    @Override
    @Query("SELECT * FROM " + PLAYLIST_TABLE)
    public abstract Flowable<List<PlaylistEntity>> getAll();

    @Override
    @Query("DELETE FROM " + PLAYLIST_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<PlaylistEntity>> listByService(final int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("SELECT * FROM " + PLAYLIST_TABLE + " WHERE " + PLAYLIST_ID + " = :playlistId")
    public abstract Flowable<List<PlaylistEntity>> getPlaylist(long playlistId);

    @Query("DELETE FROM " + PLAYLIST_TABLE + " WHERE " + PLAYLIST_ID + " = :playlistId")
    public abstract int deletePlaylist(long playlistId);

    @Query("SELECT COUNT(*) FROM " + PLAYLIST_TABLE)
    public abstract Flowable<Long> getCount();
}
