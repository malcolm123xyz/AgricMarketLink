package mx.mobile.solution.nabia04.ui.database_fragments

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
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.ListFragmentBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.databaseViewModel
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.GlideApp

class FragmentCurrentMembers : BaseFragment<ListFragmentBinding>() {

    private var activityLauncher: ActivityResultLauncher<Intent>? = null
    private var adapter: ListAdapter? = null
    private var currentListHolder: List<mx.mobile.solution.nabia04.data.entities.EntityUserData?>? =
        null


    override fun getLayoutRes(): Int = R.layout.list_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ListAdapter(requireActivity())
        vb!!.recyclerView.adapter = adapter
        observeLiveData()
    }

    private fun observeLiveData() {
        databaseViewModel.data.observe(viewLifecycleOwner) { data: List<mx.mobile.solution.nabia04.data.entities.EntityUserData>? ->
            if (data != null) {
                setData(data)
            }
        }
    }

    private fun setData(list: List<mx.mobile.solution.nabia04.data.entities.EntityUserData>) {
        val data: MutableList<mx.mobile.solution.nabia04.data.entities.EntityUserData> = ArrayList()
        object : BackgroundTasks() {
            override fun onPreExecute() {}
            override fun doInBackground() {
                for (user in list) {
                    if (user.survivingStatus != 1) {
                        data.add(user)
                    }
                }
                data.sortWith { obj1: mx.mobile.solution.nabia04.data.entities.EntityUserData, obj2: mx.mobile.solution.nabia04.data.entities.EntityUserData ->
                    obj1.fullName.compareTo(obj2.fullName)
                }
            }

            override fun onPostExecute() {
                adapter?.upDateList(data)
            }
        }.execute()
    }

    private inner class ListAdapter(context: Context) :
        RecyclerView.Adapter<ListAdapter.MyViewHolder>() {

        private val fontBody: Typeface = Typeface.createFromAsset(context.assets, "text_body.ttf")
        private val fontHeading: Typeface = Typeface.createFromAsset(context.assets, "header_font.ttf")

        fun upDateList(data: List<mx.mobile.solution.nabia04.data.entities.EntityUserData?>) {
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
