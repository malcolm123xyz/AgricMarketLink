package mx.mobile.solution.nabia04.ui.pro

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.view_models.DBViewModel
import mx.mobile.solution.nabia04.databinding.FragmentManageUsersBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData
import mx.mobile.solution.nabia04.ui.activities.endpoint
import mx.mobile.solution.nabia04.ui.database_fragments.DiffCallbackCurrList
import mx.mobile.solution.nabia04.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseLoginData
import java.text.SimpleDateFormat
import java.util.*

class FragmentManageUser : BaseFragment<FragmentManageUsersBinding>(),
    SearchView.OnQueryTextListener {

    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    override fun getLayoutRes(): Int = R.layout.fragment_manage_users

    private var deceasedDate: Long = 0

    lateinit var adapter: MyListAdapter

    private val viewModel by activityViewModels<DBViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner, Lifecycle.State.RESUMED
        )

        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = MyListAdapter(requireContext())
        vb!!.recyclerView.adapter = adapter
        setupObserver()
    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.manage_database, menu)

            val searchItem: MenuItem = menu.findItem(R.id.search)
            val searchView = searchItem.actionView as SearchView
            searchView.setOnQueryTextListener(this@FragmentManageUser)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                android.R.id.home -> {
                    findNavController().navigateUp()
                    true
                }
                R.id.refresh -> {
                    viewModel.refreshDB()
                    true
                }
                else -> {
                    true
                }
            }
        }
    }

    private fun setupObserver() {

        viewModel.refreshDB()
            .observe(viewLifecycleOwner) { users: Response<List<EntityUserData>> ->

                when (users.status) {
                    Status.SUCCESS -> {
                        users.data?.let {
                            adapter.setData(it.toMutableList())
                            adapter.notifyDataSetChanged()
                        }
                        vb?.pb?.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        vb?.pb?.visibility = View.VISIBLE
                    }
                    Status.ERROR -> {
                        vb?.pb?.visibility = View.GONE
                        Toast.makeText(requireContext(), users.message, Toast.LENGTH_LONG)
                            .show()
                        users.data?.let { adapter.setData(it.toMutableList()) }
                    }
                    else -> {}
                }
            }

    }

    private fun showPopupMenu(userItem: EntityUserData, view: View) {

        val popupMenu = PopupMenu(requireContext(), view)

        popupMenu.menu.add(0, 1, 0, "Edit")
        popupMenu.menu.add(1, 2, 1, "Set role")
        val submenu = popupMenu.menu.addSubMenu(2, Menu.NONE, 2, "Living/Dead")
        submenu.add(3, 4, 0, "Living")
        submenu.add(3, 5, 1, "Dead")
        popupMenu.menu.add(3, 6, 4, "Delete")
        popupMenu.menu.add(4, 7, 5, "Reset Password")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val intent = Intent(requireContext(), ActivityUpdateUserData::class.java)
                    intent.putExtra("folio", userItem.folioNumber)
                    startActivity(intent)
                }
                2 -> {
                    setRole(userItem)
                }
                3 -> {
                    setRole(userItem)
                }
                4 -> {
                    setLivingOrDead(4, userItem)
                }
                5 -> {
                    setLivingOrDead(5, userItem)
                }
                6 -> {
                    deleteUser(userItem)
                }
                7 -> {
                    resetPassword(userItem)
                }

            }
            true
        }
        popupMenu.show()
    }

    private fun resetPassword(userItem: EntityUserData) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage(
                "You are about to reset this user's password. \n\n" +
                        "Make sure this is a geniue request\n\n" +
                        "Do you want to Reset?"
            )
            .setCancelable(false)
            .setPositiveButton("Yes") { dialogInterface, i ->
                dialogInterface.dismiss()
                lifecycleScope.launch {
                    val pDialog =
                        MyAlertDialog(requireContext(), "", "Resetting password...", false)
                    pDialog.show()
                    val retValue = withContext(Dispatchers.IO) {
                        reset(userItem.folioNumber)
                    }
                    pDialog.dismiss()
                    if (retValue.status == Status.SUCCESS.toString()) {
                        Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                    } else {
                        showAlertDialog("ERROR", retValue.message.toString())
                    }
                }

            }.setNegativeButton(
                "No"
            ) { dialogInterface, i -> dialogInterface.dismiss() }.show()
    }

    private fun reset(folio: String): ResponseLoginData {
        return endpoint.resetPassword(folio).execute()
    }

    private fun deleteUser(userItem: EntityUserData) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("Warning")
            .setMessage("Are you sure you want to Delete this person from the database?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialogInterface, i ->
                dialogInterface.dismiss()
                deleteUser(userItem.folioNumber)
            }.setNegativeButton(
                "No"
            ) { dialogInterface, i -> dialogInterface.dismiss() }.show()
    }

    private fun deleteUser(folio: String) {
        val pDialog = MyAlertDialog(requireContext(), "DELETE", "Deleting...", false)
        pDialog.show()
        lifecycleScope.launch {
            val retValue = viewModel.deleteUser(folio)
            pDialog.dismiss()
            when (retValue.status) {
                Status.SUCCESS -> {
                    Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                    setupObserver()
                }
                Status.ERROR -> {
                    showAlertDialog("ERROR", retValue.message.toString())
                }
                else -> {}
            }
            this.cancel()
        }
    }

    private fun setLivingOrDead(i: Int, selUser: EntityUserData) {
        if (i == 5) {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING")
                .setMessage(
                    "You are about to set the living status of this colleague as Dead.\n\n" +
                            "Please kindly cross check and set the date the sad incidence occurred before continuing"
                )
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    dialog.dismiss()
                    MyAlarmManager(requireContext()).showDateTimePicker(object :
                        MyAlarmManager.CallBack {
                        override fun done(alarmTime: Long) {
                            val date = fd.format(Date(alarmTime))
                            setDeceased(selUser.folioNumber, date, 1)
                        }
                    })
                }
                .setNegativeButton("CANCEL") { dialog, id -> dialog.dismiss() }.show()

        } else {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING")
                .setMessage(
                    "You are about to set the living status of this colleague as Alive.\n\n" +
                            "DO YOU WANT TO CONTINUE"
                )
                .setCancelable(false)
                .setPositiveButton("YES") { dialog, id ->
                    dialog.dismiss()
                    setDeceased(selUser.folioNumber, "date", 0)
                }.setNegativeButton("NO") { dialog, id -> dialog.dismiss() }.show()
        }

    }

    private fun setDeceased(folio: String, date: String, status: Int) {
        val pDialog = MyAlertDialog(requireContext(), "ALERT", "Setting deceased status...", false)
        pDialog.show()
        lifecycleScope.launch {
            val retValue = viewModel.setDeceaseStatus(folio, date, status)
            pDialog.dismiss()
            when (retValue.status) {
                Status.SUCCESS -> {
                    Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                    setupObserver()
                }
                Status.ERROR -> {
                    showAlertDialog("ERROR", retValue.message.toString())
                }
                else -> {}
            }
            this.cancel()
        }
    }

    private fun setRole(userItem: EntityUserData) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING!!!").setMessage(getString(R.string.warning_1))
            .setPositiveButton("Continue") { dialog, id ->
                dialog.dismiss()
                val v: View = layoutInflater.inflate(R.layout.set_clearance, null)
                val alert =
                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setView(v)
                        .show()
                v.findViewById<View>(R.id.none)
                    .setOnClickListener(
                        OnClearanceItemClick(
                            userItem,
                            Const.POSITION_NONE,
                            alert
                        )
                    )
                v.findViewById<View>(R.id.president)
                    .setOnClickListener(
                        OnClearanceItemClick(
                            userItem,
                            Const.POSITION_PRESIDENT,
                            alert
                        )
                    )
                v.findViewById<View>(R.id.vice_president)
                    .setOnClickListener(
                        OnClearanceItemClick(
                            userItem,
                            Const.POSITION_VICE_PRES,
                            alert
                        )
                    )
                v.findViewById<View>(R.id.treasurer)
                    .setOnClickListener(
                        OnClearanceItemClick(
                            userItem,
                            Const.POSITION_TREASURER,
                            alert
                        )
                    )
                v.findViewById<View>(R.id.secretary)
                    .setOnClickListener(
                        OnClearanceItemClick(
                            userItem,
                            Const.POSITION_SEC,
                            alert
                        )
                    )
            }
            .setNegativeButton("Cancel") { dialog, id -> dialog.dismiss() }.show()
    }

    inner class OnClearanceItemClick internal constructor(
        var selUser: EntityUserData,
        var position: String,
        var alert: AlertDialog
    ) : View.OnClickListener {
        override fun onClick(view: View) {
            alert.dismiss()
            setClearance(selUser.folioNumber, position)
        }
    }

    private fun setClearance(folio: String, Clearance: String) {
        val pDialog =
            MyAlertDialog(requireContext(), "ALERT", "Assigning position... Please wait", false)
        pDialog.show()
        lifecycleScope.launch {
            val retValue = viewModel.setUserClearance(folio, Clearance)
            pDialog.dismiss()
            when (retValue.status) {
                Status.SUCCESS -> {
                    Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                    setupObserver()
                }
                Status.ERROR -> {
                    showAlertDialog("ERROR", retValue.message.toString())
                }
                else -> {}
            }
            this.cancel()
        }
    }

    inner class MyListAdapter(private val context: Context) :
        ListAdapter<EntityUserData, MyListAdapter.MyViewHolder>(
            DiffCallbackCurrList
        ), Filterable {

        private var list = mutableListOf<EntityUserData>()

        fun setData(l: MutableList<EntityUserData>) {
            list = l
            submitList(list)
        }

        inner class MyViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
            private val fullNameTxt: TextView = itemView.findViewById(R.id.name)
            private val folioTxt: TextView = itemView.findViewById(R.id.folio)
            private val profileIcon: ImageView = itemView.findViewById(R.id.icon)
            private val card: MaterialCardView = itemView.findViewById(R.id.card)
            val menu: ImageView = itemView.findViewById(R.id.actionMenu)

            /* Bind flower name and image. */
            fun bind(userItem: EntityUserData, i: Int) {
                val nickN = userItem.nickName
                var name = userItem.fullName
                if (nickN.isNotEmpty()) {
                    name = name + " (" + userItem.nickName + ")"
                }
                val folio = userItem.folioNumber
                val imageUri = userItem.imageUri
                val imageId = userItem.folioNumber
                fullNameTxt.text = name
                val f = ": " + userItem.folioNumber
                folioTxt.text = f
                menu.setOnClickListener { showPopupMenu(userItem, it) }

                if (userItem.survivingStatus == 1) {
                    card.setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red
                        )
                    )
                }
                GlideApp.with(context)
                    .load(imageUri)
                    .placeholder(R.drawable.listitem_image_holder)
                    .apply(RequestOptions.circleCropTransform())
                    .signature(ObjectKey(imageId))
                    .into(profileIcon)

                parent.setOnClickListener { view: View? ->
                    val bundle = bundleOf("folio" to folio)
                    parent.findNavController()
                        .navigate(R.id.action_move_detail_view, bundle)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_database_management, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val flower = getItem(position)
            holder.bind(flower, position)

        }

        override fun getFilter(): Filter {
            return customFilter
        }

        private val customFilter = object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val queryValue = query.toString().lowercase(Locale.getDefault())
                val filteredList = mutableListOf<EntityUserData>()
                if (queryValue.isEmpty()) {
                    filteredList.addAll(list)
                } else {
                    for (item in list) {
                        try {
                            val nameSearch =
                                item.fullName.lowercase(Locale.getDefault()).contains(queryValue) ||
                                        item.nickName.lowercase(Locale.getDefault())
                                            .contains(queryValue)
                            if (nameSearch) {
                                filteredList.add(item)
                            }
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                }
                val results = FilterResults()
                results.values = filteredList
                return results
            }

            override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
                submitList(filterResults?.values as MutableList<EntityUserData>?)
            }

        }

    }

    private fun showAlertDialog(t: String, s: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(t)
            .setMessage(s)
            .setPositiveButton(
                "OK"
            ) { dialog, id -> dialog.dismiss() }.show()
    }

    override fun onResume() {
        super.onResume()
        println("🏠 ${this.javaClass.simpleName} #${this.hashCode()}  onResume()")
    }

    override fun onPause() {
        super.onPause()
        println("🏠 ${this.javaClass.simpleName} #${this.hashCode()}  onPause()")
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        adapter.filter.filter(query)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        adapter.filter.filter(newText)
        return true
    }
}