package mx.mobile.solution.nabia04.ui.host_fragments

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat.requireViewById
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import kotlinx.coroutines.Runnable
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.repositories.DatabaseRepository
import mx.mobile.solution.nabia04.databinding.DatabaseViewpagerContainerBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.databaseViewModel
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.util.Event
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.GlideApp
import java.util.concurrent.TimeUnit


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
class DatabaseHostFragment : BaseFragment<DatabaseViewpagerContainerBinding>(),
    SearchView.OnQueryTextListener {

    private var birthdayDrawable: Drawable? = null
    private var imageSwitcherTask: BirthdayRunnableTask? = null
    private val TAG = "DatabaseViewPager"
    private lateinit var navController: NavController
    private var filterString = "none"
    private var filterType = 0
    private val activityTag = "DatabaseNavHost"
    private var repository: DatabaseRepository? = null
    private val appbarViewModel by activityViewModels<mx.mobile.solution.nabia04.core.old_package.database.view_models.AppbarViewModel>()
    private var selSpinnerItem: String? = null
    private var thisUser: mx.mobile.solution.nabia04.data.entities.EntityUserData? = null
    private var animationCounter = 1
    private val generator: ColorGenerator = ColorGenerator.MATERIAL
    private val labels = arrayOf(
        "ALL:", "Search results:", "Folio :", "My House members:", "My Classmates:",
        "MY Course mates:", "My Hometown:", "My District", "My Region:"
    )

    override fun getLayoutRes(): Int = R.layout.database_viewpager_container

    private val imageSwitcherHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        repository = DatabaseRepository.getInstance(requireActivity())
        Thread { thisUser = repository?.getUserData(userFolioNumber) }.start()
        println("${this.javaClass.simpleName} #${this.hashCode()}  onCreate()")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        if (clearance == Cons.PRO ||
            clearance == Cons.PRESIDENT ||
            clearance == Cons.VICE_PRESIDENT ||
            clearance == Cons.TREASURER ||
            userFolioNumber == "13786"
        ) {
            vb?.fabAddUser?.visibility = View.VISIBLE
        }

        repository?.refreshDatabase(false)

        navController = findNavController()

        observeDataChange()

        listenOnBackPressed()

        observeLiveData()

    }

    private fun observeLiveData() {
        databaseViewModel.data.observe(viewLifecycleOwner) { data: List<mx.mobile.solution.nabia04.data.entities.EntityUserData>? ->
            if (data != null) {
                setBirthDayPerson(data)
            }
        }
    }

    fun getDaysLeft(userBirthday: Long): String {
        val diff = userBirthday - System.currentTimeMillis()
        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        return days.toString()
    }

    private fun setBirthDayPerson(list: List<mx.mobile.solution.nabia04.data.entities.EntityUserData>) {
        val users: MutableList<mx.mobile.solution.nabia04.data.entities.EntityUserData> =
            ArrayList()
        object : BackgroundTasks() {
            override fun onPreExecute() {}
            override fun doInBackground() {
                for (event in list) {
                    val daysLeft = getDaysLeft(event.birthDayAlarm)
                    if (daysLeft == "0") {
                        users.add(event)
                    }
                }
            }
            override fun onPostExecute() {
                imageSwitcherTask = if(users.size > 0){
                    if(imageSwitcherTask != null){
                        imageSwitcherHandler.removeCallbacks(imageSwitcherTask!!)
                    }
                    getBirthDayImage(users[0].imageUri)
                    BirthdayRunnableTask(true, users[0].imageUri)
                }else{
                    BirthdayRunnableTask(false,"")
                }
                imageSwitcherHandler.post(imageSwitcherTask!!)
            }
        }.execute()
    }

    private fun getBirthDayImage(uri: String) {
        GlideApp.with(requireContext()).load(uri)
            .addListener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>,
                                          isFirstResource: Boolean): Boolean {
                    birthdayDrawable = null
                    return false
                }
                override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>,
                                             dataSource: DataSource,
                                             isFirstResource: Boolean
                ): Boolean {
                    birthdayDrawable = resource
                    return false
                }
            }).submit()
    }

    private inner class BirthdayRunnableTask(val isBirthDay: Boolean, val birthdayPersonName: String) : Runnable {
        var animationCounter = 1
        override fun run() {
            when (animationCounter++) {
                1 -> vb?.imageswitcher?.setImageDrawable(
                    getTxtDrawable(isBirthDay, animationCounter,birthdayPersonName,"HAPPY"))
                2 -> vb?.imageswitcher?.setImageDrawable(
                    getTxtDrawable(isBirthDay, animationCounter,birthdayPersonName,"BIRTHDAY"))
                3 -> vb?.imageswitcher?.setImageDrawable(
                    getTxtDrawable(isBirthDay, animationCounter,birthdayPersonName,"TO"))
                4 -> vb?.imageswitcher?.setImageDrawable(
                    getTxtDrawable(isBirthDay, animationCounter,birthdayPersonName,"TO"))
            }
            animationCounter %= 5
            if (animationCounter == 0) animationCounter = 1
            imageSwitcherHandler.postDelayed(this, 2000)
        }
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
        if (imageSwitcherTask != null) {
            imageSwitcherHandler.removeCallbacks(imageSwitcherTask!!)
        }
        println("🏠 ${this.javaClass.simpleName} #${this.hashCode()}  onPause()")
    }

    private fun listenOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    val callback = object : OnBackPressedCallback(false) {

        override fun handleOnBackPressed() {

            Toast.makeText(requireContext(), "🏠 handleOnBackPressed() ", Toast.LENGTH_SHORT)
                .show()

            println("🏠 ${this@DatabaseHostFragment.javaClass.simpleName} #${this@DatabaseHostFragment.hashCode()} handleOnBackPressed()")

            // Check if it's the root of nested fragments in this navhost
            if (navController.currentDestination?.id == navController.graph.startDestinationId) {

                Toast.makeText(requireContext(), "🏠 AT START DESTINATION ", Toast.LENGTH_SHORT)
                    .show()

                /*
                    Disable this callback because calls OnBackPressedDispatcher
                     gets invoked  calls this callback  gets stuck in a loop
                 */
                isEnabled = false
                requireActivity().onBackPressed()
                isEnabled = true

            } else {
                navController.navigateUp()
            }

        }
    }

    private fun observeDataChange() { databaseViewModel.data.observe(viewLifecycleOwner) { data: List<mx.mobile.solution.nabia04.data.entities.EntityUserData>? ->
        Log.i(activityTag, "DATA CHANGED, Updating statistics")
        if (data != null) {
            upDateDataBaseStatistics(data, filterType)
        }
    }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.database, menu)
        val searchItem: MenuItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(this)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                repository!!.refreshDatabase(true)
                super.onOptionsItemSelected(item)
            }
            R.id.profile -> {
                val bundle = bundleOf("folio" to userFolioNumber)
                findNavController().navigate(R.id.action_database_viewpager_to_current_members_detail, bundle)
                super.onOptionsItemSelected(item)
            }
            R.id.filter -> {
                val menuItemView:  View = requireViewById(requireActivity(),R.id.filter)
                showFilterPopup(menuItemView)
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
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

        for(dist in repository!!.workDistrict){
            Log.i("TAG", "DISTRICT: $dist")
            distSubmenu.add(3, Menu.NONE, Menu.NONE, dist)
        }

        for(dist in repository!!.workRegion){
            regSubmenu.add(4, Menu.NONE, Menu.NONE, dist)
        }

        employmentSubmenu.add(5, Menu.NONE, Menu.NONE, "Employed")
        employmentSubmenu.add(5, Menu.NONE, Menu.NONE, "Unemployed")

        for(dist in repository!!.employmentSector){
            emplSectorSubmenu.add(6, Menu.NONE, Menu.NONE, dist)
        }

        for(dist in repository!!.specificOrg){
            specificOrgSubmenu.add(7, Menu.NONE, Menu.NONE, dist)
        }

        for(dist in repository!!.workRegion){
            regWorkSubmenu.add(8, Menu.NONE, Menu.NONE, dist)
        }

        for(dist in repository!!.workDistrict){
            distWorkSubmenu.add(9, Menu.NONE, Menu.NONE, dist)
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when(item.itemId){
                1 -> {
                    filterString = ""
                    repository?.filter(filterString)
                }
                2 -> {
                    filterString = thisUser?.house ?: ""
                    repository?.filter(filterString)
                }
                3 -> {
                    filterString = thisUser?.className ?: ""
                    repository?.filter(filterString)
                }
                4->{}5->{}6->{}7->{}8->{}9->{}10->{}
                else ->{
                    Log.i("TAG", "Else")
                    filterString = item.title.toString()
                    repository?.filter(filterString)
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun getTxtDrawable(isBirthDay: Boolean, index: Int, birthdayPersonName: String, txt: String): Drawable? {
        if(isBirthDay) {
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
        }else{
            return when (index){
                1 -> AppCompatResources.getDrawable(requireContext(), R.drawable.login_background2)
                2 -> AppCompatResources.getDrawable(requireContext(), R.drawable.login_background3)
                3 -> AppCompatResources.getDrawable(requireContext(), R.drawable.login_background4)
                else -> AppCompatResources.getDrawable(requireContext(), R.drawable.login_background2)
            }
        }
    }

    private fun upDateDataBaseStatistics(
        filteredData: List<mx.mobile.solution.nabia04.data.entities.EntityUserData>,
        type: Int
    ) =
        object : BackgroundTasks() {
            var maleCount = 0
            var femaleCount = 0
            var noneCount = 0
            var strM = ""
            var strF = ""
            var strNot = ""
            var tt = 0
            var strT = ""
            override fun onPreExecute() {}
            override fun doInBackground() {
                val total = filteredData.size
                for (i in 0 until total) {
                    val person = filteredData[i]
                    if (person.sex != null && person.sex == "Male") {
                        maleCount++
                    } else if (person.sex != null && person.sex == "Female") {
                        femaleCount++
                    } else {
                        noneCount++
                    }
                }
                strM = "Male(s): $maleCount"
                strF = "Female(s): $femaleCount"
                strNot = "$noneCount Cow(s) did not specify their Gender "
                tt = maleCount + femaleCount + noneCount
                strT = "$tt Cow(s) Found: "
            }

            override fun onPostExecute() {
                vb?.numMales2?.text = strM
                vb?.numFemales2?.text = strF
                vb?.total2?.text = strT
                if (noneCount > 0) {
                    vb?.notSpecified?.text = strNot
                    vb?.notSpecified?.visibility = View.VISIBLE
                }

                val dispTxt: String = when (type) {
                    10 -> {
                        "Working in $selSpinnerItem:"
                    }
                    9 -> {
                        "Colleague $selSpinnerItem(s):"
                    }
                    else -> {
                        labels[type]
                    }
                }
                vb?.filterButton?.text = dispTxt
            }
        }.execute()

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
        repository?.filter(query)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        repository?.filter(newText)
        return true
    }
}

