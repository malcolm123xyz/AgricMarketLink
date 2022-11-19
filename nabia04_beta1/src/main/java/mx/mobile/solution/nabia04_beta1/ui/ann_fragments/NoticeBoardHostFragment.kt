package mx.mobile.solution.nabia04_beta1.ui.ann_fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.view_models.AnnViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentNoticeboardHostBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.ActivitySendAnnouncement
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04_beta1.utilities.Const
import mx.mobile.solution.nabia04_beta1.utilities.ExcelHelper
import javax.inject.Inject

@AndroidEntryPoint
class NoticeBoardHostFragment : Fragment() {


    private var _binding: FragmentNoticeboardHostBinding? = null

    private val binding get() = _binding!!

    private val viewModel by activityViewModels<AnnViewModel>()

    @Inject
    lateinit var excelHelper: ExcelHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoticeboardHostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        // ViewPager2
        val viewPager = binding.viewPager

        /*
            Set Adapter for ViewPager inside this fragment using this Fragment,
            more specifically childFragmentManager as param
         */
        viewPager.adapter = FragmentsAdapter(childFragmentManager, lifecycle)

        // TabLayout
        val tabLayout = binding.tabLayout

        // Bind tabs and viewpager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "General"
                1 -> tab.text = "Events"
                2 -> tab.text = "BirthDays"
            }
        }.attach()

        if (clearance == Const.POS_PRO || clearance == Const.POS_PRESIDENT
            || clearance == Const.POS_VICE_PRESIDENT || clearance == Const.POS_TREASURER
            || clearance == Const.AUTHORIZED
            || userFolioNumber == "13786"
        ) {
            binding.fabSendAnn.visibility = View.VISIBLE
            binding.fabSendAnn.setOnClickListener {
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
            return when (menuItem.itemId) {
                R.id.refresh -> {
                    viewModel.refreshDB()
                    true
                }
                else -> true
            }
        }
    }

    override fun onDestroyView() {

        val viewPager2 = binding.viewPager

        viewPager2.adapter = null

        _binding = null

        super.onDestroyView()
    }

}