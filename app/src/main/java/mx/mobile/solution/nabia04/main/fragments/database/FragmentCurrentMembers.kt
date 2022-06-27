package mx.mobile.solution.nabia04.main.fragments.database

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.ListFragmentBinding
import mx.mobile.solution.nabia04.main.MainActivity.Companion.databaseLoadingStatus
import mx.mobile.solution.nabia04.main.MainActivity.Companion.databaseViewModel
import mx.mobile.solution.nabia04.room_database.entities.EntityUserData
import mx.mobile.solution.nabia04.room_database.view_models.State
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.GlideApp

class FragmentCurrentMembers : BaseFragment<ListFragmentBinding>() {

    private var activityLauncher: ActivityResultLauncher<Intent>? = null
    private var adapter: ListAdapter? = null
    private var currentListHolder: List<EntityUserData?>? = null


    override fun getLayoutRes(): Int = R.layout.list_fragment
    override fun getCallBack(): OnBackPressedCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding!!.recyclerView.setHasFixedSize(true)
        dataBinding!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ListAdapter(requireActivity())
        dataBinding!!.recyclerView.adapter = adapter
        listenForLoadingStatus()
        observeLiveData()
    }

    private fun observeLiveData() {
        databaseViewModel.data.observe(viewLifecycleOwner) { data: List<EntityUserData>? ->
            if (data != null) {
                setData(data)
            }
        }
    }

    private fun setData(list: List<EntityUserData>) {
        val data: MutableList<EntityUserData> = ArrayList()
        object : BackgroundTasks() {
            override fun onPreExecute() {}
            override fun doInBackground() {
                for (user in list) {
                    if (user.survivingStatus != 1) {
                        data.add(user)
                    }
                }
                data.sortWith { obj1: EntityUserData, obj2: EntityUserData ->
                    obj1.fullName.compareTo(obj2.fullName)
                }
            }

            override fun onPostExecute() {
                adapter?.upDateList(data)
            }
        }.execute()
    }


    private fun listenForLoadingStatus() {
        databaseLoadingStatus.value.observe(viewLifecycleOwner) { state: State ->
            showProgress(
                state.isTrue
            )
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            dataBinding!!.shimmer.startShimmer()
            dataBinding!!.shimmer.visibility = View.VISIBLE
            dataBinding!!.recyclerView.visibility = View.INVISIBLE
        } else {
            dataBinding!!.shimmer.stopShimmer()
            dataBinding!!.shimmer.visibility = View.GONE
            dataBinding!!.recyclerView.visibility = View.VISIBLE
        }
    }

    private inner class ListAdapter(context: Context) :
        RecyclerView.Adapter<ListAdapter.MyViewHolder>() {

        private val fontBody: Typeface = Typeface.createFromAsset(context.assets, "text_body.ttf")
        private val fontHeading: Typeface = Typeface.createFromAsset(context.assets, "header_font.ttf")

        fun upDateList(data: List<EntityUserData?>) {
            currentListHolder = data
            notifyDataSetChanged()
        }

        inner class MyViewHolder(val parent: View) :
            RecyclerView.ViewHolder(parent) {
            val fullNameTxt: TextView = itemView.findViewById(R.id.name)
            val folioTxt: TextView = itemView.findViewById(R.id.folio)
            val contact1Txt: TextView = itemView.findViewById(R.id.contact)
            val profileIcon: ImageView = itemView.findViewById(R.id.icon)

        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.database_list_item, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, l: Int) {
            val parent = holder.parent
            val nickN: String = currentListHolder?.get(l)?.nickName ?: ""
            var name: String = currentListHolder?.get(l)?.fullName ?: ""
            if (nickN.isNotEmpty()) {
                name = name + " (" + currentListHolder?.get(l)?.nickName + ")"
            }
            val folio: String = currentListHolder?.get(l)?.folioNumber ?: ""
            val imageUri: String = currentListHolder?.get(l)?.imageUri ?: ""
            val imageId: String = currentListHolder?.get(l)?.imageId ?: ""
            holder.fullNameTxt.typeface = fontHeading
            holder.folioTxt.typeface = fontBody
            holder.contact1Txt.typeface = fontBody
            holder.fullNameTxt.text = name
            val f = ": " + currentListHolder?.get(l)?.folioNumber
            holder.folioTxt.text = f
            holder.contact1Txt.text = currentListHolder?.get(l)?.contact
            Linkify.addLinks(holder.contact1Txt, Linkify.PHONE_NUMBERS)

            GlideApp.with(requireContext())
                .load(imageUri)
                .placeholder(R.drawable.listitem_image_holder)
                .apply(RequestOptions.circleCropTransform())
                .signature(ObjectKey(imageId))
                .into(holder.profileIcon)

            holder.parent.setOnClickListener { view: View? ->
                val bundle = bundleOf("folio" to folio)
                findNavController().navigate(R.id.action_database_viewpager_to_current_members_detail, bundle)
            }
        }

        override fun getItemCount(): Int {
            return currentListHolder?.size ?: 0
        }
    }

}
