package mx.mobile.solution.nabia04.room_database;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import mx.mobile.solution.nabia04.room_database.entities.EntityAnnouncement;
import mx.mobile.solution.nabia04.room_database.entities.EntityUserData;
import mx.mobile.solution.nabia04.room_database.entities.EntityYearlyDues;


@Database(entities = {EntityAnnouncement.class, EntityUserData.class, EntityYearlyDues.class}, version = 42, exportSchema = false)
@TypeConverters({DataConverter.class})
public abstract class MainDataBase extends RoomDatabase {

    public abstract UserDataDao userDataDao();

    public abstract AnnDao annDao();

    public abstract DuesDetailDao duesDetailsDao();

    private static volatile MainDataBase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static MainDataBase getDatabase(final Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                        MainDataBase.class, "main_database").fallbackToDestructiveMigration()
                .build();
    }
}
