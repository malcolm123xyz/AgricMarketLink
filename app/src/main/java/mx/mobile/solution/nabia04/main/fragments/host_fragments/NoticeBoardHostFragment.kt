package mx.mobile.solution.nabia04.main.fragments.host_fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
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
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.adapter.NoticeBoardChildFragmentStateAdapter
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.viewmodel.MainAppbarViewModel
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.activities.ActivitySendAnnouncement
import mx.mobile.solution.nabia04.databinding.FragmentViewpagerContainerBinding
import mx.mobile.solution.nabia04.main.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04.main.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.main.fragments.BaseDataBindingFragment
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
class NoticeBoardHostFragment : BaseDataBindingFragment<FragmentViewpagerContainerBinding>() {

    private var repository: AnnDataRepository? = null

    override fun getLayoutRes(): Int = R.layout.fragment_viewpager_container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        repository = AnnDataRepository.getInstance(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPager2
        val viewPager = vb!!.viewPager

        /*
            Set Adapter for ViewPager inside this fragment using this Fragment,
            more specifically childFragmentManager as param
         */
        viewPager.adapter = NoticeBoardChildFragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

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

        if (clearance == Cons.PRO || clearance == Cons.PRESIDENT
            || clearance == Cons.VICE_PRESIDENT || userFolioNumber == "13786"
        ) {
            vb?.fabSendAnn?.visibility = View.VISIBLE
            vb?.fabSendAnn?.setOnClickListener {
                val i = Intent(activity, ActivitySendAnnouncement::class.java)
                startActivity(i)
            }
        }

        repository!!.loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.activity_announcements, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                repository!!.reloadFromBackend()
                super.onOptionsItemSelected(item)
            }
            R.id.logout -> {
                AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                    .setTitle("WARNING!!!")
                    .setMessage("You are about to logout")
                    .setPositiveButton(
                        "Continue"
                    ) { dialog, id -> dialog.dismiss()
                    }
                    .setNegativeButton(
                        "Cancel"
                    ) { dialog, id -> dialog.dismiss() }.show()
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
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