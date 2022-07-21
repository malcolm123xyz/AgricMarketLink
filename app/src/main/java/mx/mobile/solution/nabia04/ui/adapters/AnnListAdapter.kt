package mx.mobile.solution.nabia04.ui.adapters

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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AnnListAdapter @Inject constructor(private val context: Context) :
    ListAdapter<EntityAnnouncement,
            AnnListAdapter.MyViewHolder>(DiffCallback()) {
    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    inner class MyViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
        private val topic: TextView = itemView.findViewById(R.id.heading)
        val time: TextView = itemView.findViewById(R.id.time)
        val annPicture: ImageView = itemView.findViewById(R.id.ann_picture)

        fun bind(annItem: EntityAnnouncement) {
            val topic = annItem.heading
            val date = getDate(annItem.id)
            this.topic.text = topic
            time.text = date
            if (annItem.isRead) {
                this.topic.typeface = null
            }

            val imageUri: String = annItem.imageUri ?: ""

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
            .inflate(R.layout.announcement_list_item, parent, false)
        return MyViewHolder(view)
    }

    /* Gets current flower and uses it to bind view. */
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(getItem(position))

    }

    private fun getDate(l: Long): String {
        return fd.format(l)
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
