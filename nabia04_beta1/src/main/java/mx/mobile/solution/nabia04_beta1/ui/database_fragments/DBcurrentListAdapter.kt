package mx.mobile.solution.nabia04_beta1.ui.database_fragments

import android.content.Context
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.utilities.GlideApp
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DBcurrentListAdapter @Inject constructor(private val context: Context) :
    ListAdapter<EntityUserData,
            DBcurrentListAdapter.MyViewHolder>(DiffCallbackCurrList), Filterable {

    private var list = mutableListOf<EntityUserData>()

    fun setData(l: MutableList<EntityUserData>) {
        list = l
        submitList(list)
    }

    inner class MyViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
        private val fullNameTxt: TextView = itemView.findViewById(R.id.name)
        private val folioTxt: TextView = itemView.findViewById(R.id.folio)
        private val contact1Txt: TextView = itemView.findViewById(R.id.contact)
        private val profileIcon: ImageView = itemView.findViewById(R.id.icon)

        private var user: EntityUserData? = null


        /* Bind flower name and image. */
        fun bind(userItem: EntityUserData, i: Int) {
            user = userItem
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
            contact1Txt.text = userItem.contact
            Linkify.addLinks(contact1Txt, Linkify.PHONE_NUMBERS)

            Log.i("TAG", "User name: $name, Uri: $imageUri")

            GlideApp.with(context)
                .load(imageUri)
                .placeholder(R.drawable.listitem_image_holder)
                .apply(RequestOptions.circleCropTransform())
                .signature(ObjectKey(imageId))
                .into(profileIcon)

            parent.setOnClickListener { view: View? ->
                val bundle = bundleOf("folio" to folio)
                parent.findNavController()
                    .navigate(R.id.action_database_viewpager_to_current_members_detail, bundle)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_database, parent, false)
        return MyViewHolder(view)
    }

    /* Gets current flower and uses it to bind view. */
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
                                        .contains(queryValue) ||
                                    item.folioNumber.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.house.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.className.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.courseStudied.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.homeTown.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.districtOfResidence.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.regionOfResidence.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.employmentSector.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.jobDescription.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.specificOrg.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.nameOfEstablishment.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.establishmentRegion.lowercase(Locale.getDefault())
                                        .contains(queryValue) ||
                                    item.establishmentDist.lowercase(Locale.getDefault())
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

object DiffCallbackCurrList : DiffUtil.ItemCallback<EntityUserData>() {
    override fun areItemsTheSame(
        oldItem: EntityUserData,
        newItem: EntityUserData
    ): Boolean {
        return oldItem.folioNumber == newItem.folioNumber
    }

    override fun areContentsTheSame(
        oldItem: EntityUserData,
        newItem: EntityUserData
    ): Boolean {
        return oldItem.imageUri == newItem.imageUri ||
                oldItem.survivingStatus == newItem.survivingStatus
    }
}