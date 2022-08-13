package mx.mobile.solution.nabia04.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.utilities.GlideApp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DBdepartedListAdapter @Inject constructor(private val context: Context) :
    ListAdapter<EntityUserData,
            DBdepartedListAdapter.MyViewHolder>(DiffCallback1) {

    private var list = mutableListOf<EntityUserData>()

    fun setData(l: MutableList<EntityUserData>) {
        list = l
        submitList(list)
    }

    inner class MyViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
        val fullNameTxt: TextView = itemView.findViewById(R.id.name)
        val dateDepartedTXT: TextView = itemView.findViewById(R.id.contact)
        val profileIcon: ImageView = itemView.findViewById(R.id.icon)

        private var user: EntityUserData? = null

        fun bind(userItem: EntityUserData, i: Int) {
            user = userItem
            val nickN: String = userItem.nickName ?: ""
            var name: String = userItem.fullName ?: ""
            if (nickN.isNotEmpty()) {
                name = name + " (" + userItem.nickName + ")"
            }
            val folio: String = userItem.folioNumber ?: ""
            val imageUri: String = userItem.imageUri ?: ""
            val imageId: String = userItem.imageId ?: ""
            fullNameTxt.text = name
            dateDepartedTXT.text =
                String.format("Died on:  %s", userItem.dateDeparted)

            GlideApp.with(context)
                .load(imageUri)
                .placeholder(R.drawable.listitem_image_holder)
                .apply(RequestOptions.circleCropTransform())
                .signature(ObjectKey(imageId))
                .into(profileIcon)

            parent.setOnClickListener { view: View? ->
                val bundle = bundleOf("folio" to folio)
                parent.findNavController()
                    .navigate(R.id.action_database_viewpager_to_departed_members_detail, bundle)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.database_list_item_department, parent, false)
        return MyViewHolder(view)
    }

    /* Gets current flower and uses it to bind view. */
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val flower = getItem(position)
        holder.bind(flower, position)

    }
}

object DiffCallback1 : DiffUtil.ItemCallback<EntityUserData>() {
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
        return oldItem.imageUri == newItem.imageUri
    }
}