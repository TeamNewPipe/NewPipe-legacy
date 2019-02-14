package org.schabi.newpipelegacy;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;

import org.schabi.newpipelegacy.database.AppDatabase;

import static org.schabi.newpipelegacy.database.AppDatabase.DATABASE_NAME;
import static org.schabi.newpipelegacy.database.Migrations.MIGRATION_11_12;

public final class NewPipeDatabase {

    private static volatile AppDatabase databaseInstance;

    private NewPipeDatabase() {
        //no instance
    }

    private static AppDatabase getDatabase(Context context) {
        return Room
                .databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                .addMigrations(MIGRATION_11_12)
                .fallbackToDestructiveMigration()
                .build();
    }

    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        AppDatabase result = databaseInstance;
        if (result == null) {
            synchronized (NewPipeDatabase.class) {
                result = databaseInstance;
                if (result == null) {
                    databaseInstance = (result = getDatabase(context));
                }
            }
        }

        return result;
    }
}
