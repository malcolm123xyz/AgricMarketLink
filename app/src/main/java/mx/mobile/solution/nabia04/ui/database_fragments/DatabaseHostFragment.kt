@file:Suppress("KDocUnresolvedReference")

package mx.mobile.solution.nabia04.ui.database_fragments

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat.requireViewById
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.adapter.DatabaseChildFragmentStateAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.core.old_package.database.view_models.AppbarViewModel
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.view_models.DBViewModel
import mx.mobile.solution.nabia04.databinding.DatabaseViewpagerContainerBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.ui.adapters.DBcurrentListAdapter
import mx.mobile.solution.nabia04.util.Event
import mx.mobile.solution.nabia04.utilities.*
import java.util.concurrent.TimeUnit
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
class DatabaseHostFragment : BaseFragment<DatabaseViewpagerContainerBinding>(),
    SearchView.OnQueryTextListener {

    private val viewModel by activityViewModels<DBViewModel>()


    private val appbarViewModel by activityViewModels<AppbarViewModel>()

    @Inject
    lateinit var adapter: DBcurrentListAdapter

    private var birthdayDrawable: Drawable? = null
    private lateinit var navController: NavController
    private var thisUser: EntityUserData? = null
    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    private val hometownDistrict: MutableList<String> = ArrayList()
    private val hometownReg: MutableList<String> = ArrayList()
    private val employmentSector: MutableList<String> = ArrayList()
    private val specificOrg: MutableList<String> = ArrayList()
    private val workRegion: MutableList<String> = ArrayList()
    private val workDistrict: MutableList<String> = ArrayList()

    private var animationJob: Job? = null

    override fun getLayoutRes(): Int = R.layout.database_viewpager_container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner, Lifecycle.State.RESUMED
        )

        // ViewPager2
        vb?.viewpager?.adapter =
            DatabaseChildFragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        // TabLayout
        val tabLayout = vb!!.tabLayout
        // Bind tabs and viewpager
        TabLayoutMediator(tabLayout, vb!!.viewpager) { tab, position ->
            when (position) {
                0 -> tab.text = "Current members"
                1 -> tab.text = "Departed members"
            }
        }.attach()

        vb?.imageswitcher?.setFactory {
            val imageView = ImageView(activity)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.layoutParams = FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            imageView
        }
        val `in` = AnimationUtils.loadAnimation(activity, R.anim.image_in)
        val out = AnimationUtils.loadAnimation(activity, R.anim.image_out)
        vb?.imageswitcher?.inAnimation = `in`
        vb?.imageswitcher?.outAnimation = out

        vb?.fabAddUser?.setOnClickListener {
            val i = Intent(activity, ActivityUpdateUserData::class.java)
            i.putExtra("folio", "")
            startActivity(i)
        }

        if (clearance == Cons.PRO || clearance == Cons.PRESIDENT || clearance == Cons.VICE_PRESIDENT ||
            clearance == Cons.TREASURER ||
            userFolioNumber == "13786"
        ) {
            vb?.fabAddUser?.visibility = View.VISIBLE
        }

        navController = findNavController()

        listenOnBackPressed()

        lifecycleScope.launch {
            thisUser = viewModel.getUser(userFolioNumber)
            val list = viewModel.getList()
            if (list != null) {
                upDateStats(list)
                getFilterData(list)
                this.cancel()
                initiateJob(list)
            }
        }
    }

    private fun initiateJob(list: List<EntityUserData>) {
        animationJob?.cancel() // optional if you want to start afresh

        // Create a new Job and assign it to our variable
        animationJob = lifecycleScope.launch {
            while (isActive) {
                val users: MutableList<EntityUserData> = ArrayList()
                for (event in list) {
                    val daysLeft = getDaysLeft(event.birthDayAlarm)
                    if (daysLeft == "0") {
                        users.add(event)
                    }
                }
                if (users.size > 0) {
                    getBirthDayImage(users[0].imageUri)
                    animationTask(true, users[0].imageUri)
                } else {
                    animationTask(false, "")
                }
                delay(2 * 1000) // Task will be performed after the delay
            }
        }
    }


    private fun getDaysLeft(userBirthday: Long): String {
        val diff = userBirthday - System.currentTimeMillis()
        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        return days.toString()
    }

    private fun getBirthDayImage(uri: String) {
        GlideApp.with(requireContext()).load(uri)
            .addListener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any, target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    birthdayDrawable = null
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any, target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    birthdayDrawable = resource
                    return false
                }
            }).submit()
    }

    private fun animationTask(isBirthDay: Boolean, birthdayPersonName: String) {
        var animationCounter = 1

        when (animationCounter++) {
            1 -> vb?.imageswitcher?.setImageDrawable(
                getTxtDrawable(isBirthDay, animationCounter, birthdayPersonName, "HAPPY")
            )
            2 -> vb?.imageswitcher?.setImageDrawable(
                getTxtDrawable(isBirthDay, animationCounter, birthdayPersonName, "BIRTHDAY")
            )
            3 -> vb?.imageswitcher?.setImageDrawable(
                getTxtDrawable(isBirthDay, animationCounter, birthdayPersonName, "TO")
            )
            4 -> vb?.imageswitcher?.setImageDrawable(
                getTxtDrawable(isBirthDay, animationCounter, birthdayPersonName, "TO")
            )
        }
        animationCounter %= 5
    }

    override fun onResume() {
        super.onResume()
        println("🏠 ${this.javaClass.simpleName} #${this.hashCode()}  onResume()")
        callback.isEnabled = true

        // Set this navController as ViewModel's navController
        navController.let {
            appbarViewModel.currentNavController.value = Event(it)
        }
    }

    override fun onPause() {
        super.onPause()
        callback.isEnabled = false
        println("🏠 ${this.javaClass.simpleName} #${this.hashCode()}  onPause()")
    }

    private fun listenOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    val callback = object : OnBackPressedCallback(false) {

        override fun handleOnBackPressed() {
            println("🏠 ${this@DatabaseHostFragment.javaClass.simpleName} #${this@DatabaseHostFragment.hashCode()} handleOnBackPressed()")

            // Check if it's the root of nested fragments in this navhost
            if (navController.currentDestination?.id == navController.graph.startDestinationId) {

                isEnabled = false
                requireActivity().onBackPressed()
                isEnabled = true

            } else {
                navController.navigateUp()
            }

        }
    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.database, menu)

            val searchItem: MenuItem = menu.findItem(R.id.search)
            val searchView = searchItem.actionView as SearchView
            searchView.setOnQueryTextListener(this@DatabaseHostFragment)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.refresh -> {
                    viewModel.refreshDB()
                    return true
                }
                R.id.profile -> {
                    val bundle = bundleOf("folio" to userFolioNumber)
                    findNavController().navigate(
                        R.id.action_database_viewpager_to_current_members_detail,
                        bundle
                    )
                    return true
                }
                R.id.filter -> {
                    val menuItemView: View = requireViewById(requireActivity(), R.id.filter)
                    showFilterPopup(menuItemView)
                    return true
                }
                else -> {
                    return true
                }
            }
        }
    }

    private fun showFilterPopup(view: View) {

        val popupMenu = PopupMenu(requireContext(), view)

        popupMenu.menu.add(0, 1, 0, "All")
        popupMenu.menu.add(1, 2, 1, "My house members")
        popupMenu.menu.add(2, 3, 2, "My classmates")
        val distSubmenu = popupMenu.menu.addSubMenu(3, 4, 3, "District")
        val regSubmenu = popupMenu.menu.addSubMenu(4, 5, 4, "Region")
        val employmentSubmenu = popupMenu.menu.addSubMenu(5, 6, 5, "Employment status")
        val emplSectorSubmenu = popupMenu.menu.addSubMenu(6, 7, 6, "Employment sector")
        val specificOrgSubmenu = popupMenu.menu.addSubMenu(7, 8, 7, "Specific Org.")
        val regWorkSubmenu = popupMenu.menu.addSubMenu(8, 4, 9, "Region of work")
        val distWorkSubmenu = popupMenu.menu.addSubMenu(9, 10, 9, "District of Work")

        for (dist in workDistrict) {
            distSubmenu.add(3, Menu.NONE, Menu.NONE, dist)
        }

        for (dist in workRegion) {
            regSubmenu.add(4, Menu.NONE, Menu.NONE, dist)
        }

        employmentSubmenu.add(5, Menu.NONE, Menu.NONE, "Employed")
        employmentSubmenu.add(5, Menu.NONE, Menu.NONE, "Unemployed")

        for (dist in employmentSector) {
            emplSectorSubmenu.add(6, Menu.NONE, Menu.NONE, dist)
        }

        for (dist in specificOrg) {
            specificOrgSubmenu.add(7, Menu.NONE, Menu.NONE, dist)
        }

        for (dist in workRegion) {
            regWorkSubmenu.add(8, Menu.NONE, Menu.NONE, dist)
        }

        for (dist in workDistrict) {
            distWorkSubmenu.add(9, Menu.NONE, Menu.NONE, dist)
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    adapter.filter.filter("")
                }
                2 -> {
                    adapter.filter.filter(thisUser?.house ?: "")
                }
                3 -> {
                    adapter.filter.filter(thisUser?.className ?: "")
                }
                4 -> {}
                5 -> {}
                6 -> {}
                7 -> {}
                8 -> {}
                9 -> {}
                10 -> {}
                else -> {
                    adapter.filter.filter(item.title.toString())
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun getTxtDrawable(
        isBirthDay: Boolean,
        index: Int,
        birthdayPersonName: String,
        txt: String
    ): Drawable? {
        if (isBirthDay) {
            return if (index == 5) {
                birthdayDrawable ?: TextDrawable.Builder()
                    .setColor(generator.randomColor)
                    .setShape(TextDrawable.SHAPE_RECT)
                    .setText(birthdayPersonName)
                    .setFontSize(50)
                    .build()
            } else {
                TextDrawable.Builder()
                    .setColor(generator.randomColor)
                    .setShape(TextDrawable.SHAPE_RECT)
                    .setText(txt)
                    .setFontSize(50)
                    .build()
            }
        } else {
            return when (index) {
                1 -> AppCompatResources.getDrawable(requireContext(), R.drawable.login_background2)
                2 -> AppCompatResources.getDrawable(requireContext(), R.drawable.login_background3)
                3 -> AppCompatResources.getDrawable(requireContext(), R.drawable.login_background4)
                else -> AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.login_background2
                )
            }
        }
    }

    private fun upDateStats(filteredData: List<EntityUserData>) {
        val listSize = filteredData.size
        var maleCount = 0
        var femaleCount = 0
        var noneCount = 0

        for (i in 0 until listSize) {
            val person = filteredData[i]
            if (person.sex != null && person.sex == "Male") {
                maleCount++
            } else if (person.sex != null && person.sex == "Female") {
                femaleCount++
            } else {
                noneCount++
            }
        }
        val tt = maleCount + femaleCount + noneCount

        vb?.numMales2?.text = "Male(s): $maleCount"
        vb?.numFemales2?.text = "Female(s): $femaleCount"
        vb?.total2?.text = "$tt Cow(s) Found: "
        if (noneCount > 0) {
            vb?.notSpecified?.text = "$noneCount Cow(s) did not specify their Gender "
            vb?.notSpecified?.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {

        val viewPager2 = vb?.viewpager

        /*
            Without setting ViewPager2 Adapter it causes memory leak

            https://stackoverflow.com/questions/62851425/viewpager2-inside-a-fragment-leaks-after-replacing-the-fragment-its-in-by-navig
         */
        viewPager2?.let {
            it.adapter = null
        }

        super.onDestroyView()
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        adapter.filter.filter(query)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        adapter.filter.filter(newText)
        return true
    }

    private fun getFilterData(list: List<EntityUserData>) {
        for (user in list) {
            if (!user.districtOfResidence.equals("SELECT DISTRICT") &&
                !user.districtOfResidence.isNullOrEmpty() &&
                !hometownDistrict.contains(user.districtOfResidence)
            ) {
                hometownDistrict.add(user.districtOfResidence)
            }
            if (!user.regionOfResidence.equals("SELECT REGION") &&
                !user.regionOfResidence.isNullOrEmpty() &&
                !hometownReg.contains(user.regionOfResidence)
            ) {
                hometownReg.add(user.regionOfResidence)
            }
            if (!user.employmentSector.isNullOrEmpty() &&
                !employmentSector.contains(user.employmentSector)
            ) {
                employmentSector.add(user.employmentSector)
            }
            if (!user.specificOrg.isNullOrEmpty() && !specificOrg.contains(user.specificOrg)) {
                specificOrg.add(user.specificOrg)
            }
            if (!user.establishmentRegion.equals("SELECT REGION") &&
                !user.establishmentRegion.isNullOrEmpty() &&
                !workRegion.contains(user.establishmentRegion)
            ) {
                workRegion.add(user.establishmentRegion)
            }
            if (!user.districtOfResidence.equals("SELECT DISTRICT") &&
                !user.establishmentDist.isNullOrEmpty() && !workDistrict.contains(user.establishmentDist)
            ) {
                workDistrict.add(user.establishmentDist)
            }
        }

    }
}

