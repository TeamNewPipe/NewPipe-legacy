package org.schabi.newpipelegacy;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Room;

import org.schabi.newpipelegacy.database.AppDatabase;

import static org.schabi.newpipelegacy.database.AppDatabase.DATABASE_NAME;
import static org.schabi.newpipelegacy.database.Migrations.MIGRATION_1_2;
import static org.schabi.newpipelegacy.database.Migrations.MIGRATION_2_3;

public final class NewPipeDatabase {
    private static volatile AppDatabase databaseInstance;

    private NewPipeDatabase() {
        //no instance
    }

    private static AppDatabase getDatabase(final Context context) {
        return Room
                .databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build();
    }

    @NonNull
    public static AppDatabase getInstance(@NonNull final Context context) {
        AppDatabase result = databaseInstance;
        if (result == null) {
            synchronized (NewPipeDatabase.class) {
                result = databaseInstance;
                if (result == null) {
                    databaseInstance = getDatabase(context);
                    result = databaseInstance;
                }
            }
        }

        return result;
    }

    public static void checkpoint() {
        if (databaseInstance == null) {
            throw new IllegalStateException("database is not initialized");
        }
        final Cursor c = databaseInstance.query("pragma wal_checkpoint(full)", null);
        if (c.moveToFirst() && c.getInt(0) == 1) {
            throw new RuntimeException("Checkpoint was blocked from completing");
        }
    }
}
