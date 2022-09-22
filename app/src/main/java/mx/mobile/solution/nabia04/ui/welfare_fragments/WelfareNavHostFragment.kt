package mx.mobile.solution.nabia04.ui.welfare_fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentDuesSummaryBinding
import mx.mobile.solution.nabia04.ui.BaseFragment


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
class WelfareNavHostFragment : BaseFragment<FragmentDuesSummaryBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_dues_summary

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPager2
        val viewPager = vb!!.viewPager

        viewPager.adapter =
            FragmentAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        // TabLayout
        val tabLayout = vb!!.tabLayout

        // Bind tabs and viewpager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "My Dues"
                1 -> tab.text = "Silver Contribution"
            }
        }.attach()

//        if (clearance == Cons.TREASURER || userFolioNumber == "13786") {
//            vb?.treasurerFab?.visibility = View.VISIBLE
//            vb?.treasurerFab?.setOnClickListener {
//                val i = Intent(activity, ActivityTreasurerTools::class.java)
//                startActivity(i)
//            }
//        }
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