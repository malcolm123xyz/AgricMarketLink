package mx.mobile.solution.nabia04.main.fragment.host_fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.adapter.WelfareHostStateAdapter
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.viewmodel.MainAppbarViewModel
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.activities.ActivityTreasurer
import mx.mobile.solution.nabia04.databinding.FragmentWelfareHostBinding
import mx.mobile.solution.nabia04.main.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04.main.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment
import mx.mobile.solution.nabia04.room_database.repositories.AnnDataRepository
import mx.mobile.solution.nabia04.utilities.Cons


/**
 * Fragment that contains [ViewPager2] and [TabLayout]. If this fragments get replaced and [Fragment.onDestroyView]
 * is called there are things to be considered
 *
 * * [FragmentStateAdapter] that is not null after [Fragment.onDestroy] cause memory leak, so assign null to it
 *
 * * [TabLayoutMediator] cause memory leak if not detached after [Fragment.onDestroy] of this fragment is called.
 *
 * * Data-binding which is not null after [Fragment.onDestroy]  causes memory leak
 *
 * *[MainAppbarViewModel] that has a [NavController] that belong to a NavHostFragment that is to be destroyed
 * also causes memory leak.
 */
class WelfareHostFragment : BaseDataBindingFragment<FragmentWelfareHostBinding>() {


    private var repository: AnnDataRepository? = null

    override fun getLayoutRes(): Int = R.layout.fragment_welfare_host


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        repository = AnnDataRepository.getInstance(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPager2
        val viewPager = vb!!.viewPager

        viewPager.adapter = WelfareHostStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        // TabLayout
        val tabLayout = vb!!.tabLayout

        // Bind tabs and viewpager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Summary"
                1 -> tab.text = "Contributions"
                2 -> tab.text = "Contribution History"
            }
        }.attach()

        if (clearance == Cons.TREASURER || userFolioNumber == "13786") {
            vb?.treasurerFab?.visibility = View.VISIBLE
            vb?.treasurerFab?.setOnClickListener {
                Log.i("TAG", "FAB CLICKED")
                val i = Intent(activity, ActivityTreasurer::class.java)
                startActivity(i)
            }
        }
    }

   

    override fun onDestroyView() {

        val viewPager2 = vb?.viewPager

        /*
            Without setting ViewPager2 Adapter it causes memory leak

            https://stackoverflow.com/questions/62851425/viewpager2-inside-a-fragment-leaks-after-replacing-the-fragment-its-in-by-navig
         */
        viewPager2?.let {
            it.adapter = null
        }

        super.onDestroyView()
    }
}
