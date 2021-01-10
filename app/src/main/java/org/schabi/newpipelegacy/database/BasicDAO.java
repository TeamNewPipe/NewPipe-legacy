package org.schabi.newpipelegacy.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

import java.util.Collection;
import java.util.List;

import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface BasicDAO<Entity> {
    /* Inserts */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Entity entity);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertAll(Entity... entities);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertAll(Collection<Entity> entities);

    /* Searches */
    Flowable<List<Entity>> getAll();

    Flowable<List<Entity>> listByService(int serviceId);

    /* Deletes */
    @Delete
    void delete(Entity entity);

    @Delete
    int delete(Collection<Entity> entities);

    int deleteAll();

    /* Updates */
    @Update
    int update(Entity entity);

    @Update
    void update(Collection<Entity> entities);
}
