package mx.mobile.solution.nabia04.ui.ann_fragments

import android.content.Context
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.utilities.GlideApp
import javax.inject.Inject

class GenAnnAdapter @Inject constructor(private val context: Context) :
    ListAdapter<EntityAnnouncement,
            GenAnnAdapter.MyViewHolder>(DiffCallback()) {

    inner class MyViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
        private val topic: TextView = itemView.findViewById(R.id.heading)
        val annPicture: ImageView = itemView.findViewById(R.id.ann_picture)

        fun bind(annItem: EntityAnnouncement) {
            val topic = annItem.heading
            this.topic.text = topic

            val imageUri: String = annItem.imageUri

            GlideApp.with(context)
                .load(imageUri)
                .signature(ObjectKey(annItem.id))
                .placeholder(R.drawable.photo_galary)
                .addListener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        annPicture.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        annPicture.visibility = View.VISIBLE
                        return false
                    }
                }).into(annPicture)

            parent.setOnClickListener {
                val bundle = bundleOf("folio" to annItem.id)
                parent.findNavController().navigate(R.id.action_gen_not_to_events_not, bundle)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_ann, parent, false)
        return MyViewHolder(view)
    }

    /* Gets current flower and uses it to bind view. */
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(getItem(position))

    }


    private class DiffCallback : DiffUtil.ItemCallback<EntityAnnouncement>() {
        override fun areItemsTheSame(
            oldItem: EntityAnnouncement,
            newItem: EntityAnnouncement
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: EntityAnnouncement,
            newItem: EntityAnnouncement
        ): Boolean {
            return oldItem.heading == newItem.heading
        }
    }

}
