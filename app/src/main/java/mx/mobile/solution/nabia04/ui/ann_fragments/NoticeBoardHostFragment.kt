package mx.mobile.solution.nabia04.ui.ann_fragments

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.view_models.AnnViewModel
import mx.mobile.solution.nabia04.databinding.FragmentViewpagerContainerBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.ActivitySendAnnouncement
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.ui.adapters.NoticeBoardChildFragmentStateAdapter
import mx.mobile.solution.nabia04.utilities.Const
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import javax.inject.Inject


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
@AndroidEntryPoint
class NoticeBoardHostFragment : BaseFragment<FragmentViewpagerContainerBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_viewpager_container

    private val viewModel by activityViewModels<AnnViewModel>()

    @Inject
    lateinit var excelHelper: ExcelHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        // ViewPager2
        val viewPager = vb!!.viewPager

        /*
            Set Adapter for ViewPager inside this fragment using this Fragment,
            more specifically childFragmentManager as param
         */
        viewPager.adapter = NoticeBoardChildFragmentStateAdapter(childFragmentManager, lifecycle)

        // TabLayout
        val tabLayout = vb!!.tabLayout

        // Bind tabs and viewpager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "General"
                1 -> tab.text = "Events"
                2 -> tab.text = "BirthDays"
            }
        }.attach()

        if (clearance == Const.POS_PRO || clearance == Const.POS_PRESIDENT
            || clearance == Const.POS_VICE_PRESIDENT || userFolioNumber == "13786"
        ) {
            vb?.fabSendAnn?.visibility = View.VISIBLE
            vb?.fabSendAnn?.setOnClickListener {
                val i = Intent(activity, ActivitySendAnnouncement::class.java)
                startActivity(i)
            }
        }

    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.activity_announcements, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val id = menuItem.itemId
            return when (id) {
                R.id.refresh -> {
                    viewModel.refreshDB()
                    true
                }
                else -> true
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