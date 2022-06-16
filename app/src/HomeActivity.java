package mx.mobile.solution.nabia04.core;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.eu.fragmentstatemanager.StateManager;
import com.eu.fragmentstatemanager.StateManagerBuilder;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import mx.mobile.solution.nabia04.R;
import mx.mobile.solution.nabia04.core.old_package.home.N04_database.DatabaseViewModel;
import mx.mobile.solution.nabia04.core.old_package.home.N04_database.FragmentCurrentMembers;
import mx.mobile.solution.nabia04.core.old_package.home.events_gallary.EventsGallaryFragment;
import mx.mobile.solution.nabia04.core.old_package.home.noticeboard.AnnFragmentHolder;
import mx.mobile.solution.nabia04.core.old_package.home.noticeboard.AnnViewModel;
import mx.mobile.solution.nabia04.core.old_package.home.noticeboard.FragmentGeneral;
import mx.mobile.solution.nabia04.core.old_package.home.profession.ProfessionFragment;
import mx.mobile.solution.nabia04.core.old_package.home.welfare.WelfareFragment;

public class HomeActivity extends AppCompatActivity {

    public static AnnViewModel annViewModel;
    public static DatabaseViewModel databaseViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        annViewModel = new ViewModelProvider(this).get(AnnViewModel.class);
        databaseViewModel = new ViewModelProvider(this).get(DatabaseViewModel.class);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home_ann, R.id.navigation_home_database,
                R.id.navigation_home_welfare, R.id.navigation_home_prefession, R.id.navigation_home_events_gallery)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_home);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        //getSupportActionBar().setElevation(0);


    }

}