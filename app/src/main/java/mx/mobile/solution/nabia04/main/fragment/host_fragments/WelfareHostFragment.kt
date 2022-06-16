package mx.mobile.solution.nabia04.main.fragment.host_fragments

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.adapter.WelfareHostStateAdapter
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.viewmodel.MainAppbarViewModel
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.activities.ActivityTreasurer
import mx.mobile.solution.nabia04.databinding.FragmentWelfareHostBinding
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper
import mx.mobile.solution.nabia04.room_database.repositories.AnnDataRepository
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.SessionManager


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
    private var sharedP: SharedPreferences? = null

    override fun getLayoutRes(): Int = R.layout.fragment_welfare_host

    companion object {
        lateinit var  excelHelper: ExcelHelper
        var totalAmount = 0.00
        var userTotalAmount = 0.00
        var numMonths = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        sharedP = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val folioNumber = sharedP?.getString(SessionManager.FOLIO_NUMBER, "")
        excelHelper = ExcelHelper.getInstance(requireContext())!!
        repository = AnnDataRepository.getInstance(requireActivity())
        totalAmount = excelHelper.getOverallTotal()
        userTotalAmount = excelHelper.getUserTotal(folioNumber!!)
        numMonths = excelHelper.getUserNumMonths(folioNumber)
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

        val clearance: String? = sharedP!!.getString(Cons.CLEARANCE, "")
        val folio: String? = sharedP!!.getString(SessionManager.FOLIO_NUMBER, "13786")
        if (clearance == Cons.TREASURER || folio == "13786") {
            vb?.treasurerFab?.visibility = View.VISIBLE
            vb?.treasurerFab?.setOnClickListener {
                val i = Intent(activity, ActivityTreasurer::class.java)
                startActivity(i)
                //ExcelHelper(requireContext()).readSheet(2)
            }
        }

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