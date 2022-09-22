package mx.mobile.solution.nabia04_beta1.data;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import mx.mobile.solution.nabia04_beta1.data.converters.DataConverter;
import mx.mobile.solution.nabia04_beta1.data.dao.AnnDao;
import mx.mobile.solution.nabia04_beta1.data.dao.DBdao;
import mx.mobile.solution.nabia04_beta1.data.dao.DuesBackupDao;
import mx.mobile.solution.nabia04_beta1.data.dao.ProfMainViewDao;
import mx.mobile.solution.nabia04_beta1.data.entities.EntityAnnouncement;
import mx.mobile.solution.nabia04_beta1.data.entities.EntityDuesBackup;
import mx.mobile.solution.nabia04_beta1.data.entities.EntityQuestion;
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData;


@Database(entities = {EntityAnnouncement.class, EntityUserData.class, EntityDuesBackup.class,
        EntityQuestion.class},
        version = 83, exportSchema = false)
@TypeConverters({DataConverter.class})
public abstract class MainDataBase extends RoomDatabase {

    public abstract AnnDao annDao();

    public abstract DBdao dbDao();

    public abstract DuesBackupDao duesBackupDao();

    public abstract ProfMainViewDao profMainViewDao();

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
