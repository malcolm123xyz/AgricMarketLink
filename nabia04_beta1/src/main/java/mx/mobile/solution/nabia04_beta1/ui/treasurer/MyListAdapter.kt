package mx.mobile.solution.nabia04_beta1.ui.treasurer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.utilities.GlideApp
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class MyListAdapter @Inject constructor(private val context: Context) :
    ListAdapter<Map<String, String>, MyListAdapter.MyViewHolder>(ContDiffCallback()) {
    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    inner class MyViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {

        val name: TextView = itemView.findViewById(R.id.name)
        private val totalPayment: TextView = itemView.findViewById<TextView?>(R.id.payment)
        val date: TextView = itemView.findViewById<TextView?>(R.id.date)
        val userIcon: ImageView = itemView.findViewById<android.widget.ImageView?>(R.id.userIcon)

        fun bind(map: Map<String, String>, i: Int) {
            name.text = map["name"]
            totalPayment.text = map["payment"]
            date.text = map["date"]

            GlideApp.with(context)
                .load(map["imageUri"])
                .placeholder(R.drawable.use_icon)
                .optionalCircleCrop()
                .into(userIcon)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_contribution, parent, false)
        return MyViewHolder(view)
    }

    /* Gets current flower and uses it to bind view. */
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val flower = getItem(position)
        holder.bind(flower, position)

    }

    private fun getDate(l: Long): String {
        return fd.format(l)
    }

    private class ContDiffCallback : DiffUtil.ItemCallback<Map<String, String>>() {
        override fun areItemsTheSame(
            oldItem: Map<String, String>,
            newItem: Map<String, String>
        ): Boolean {
            return oldItem["name"] == newItem["name"]
        }

        override fun areContentsTheSame(
            oldItem: Map<String, String>,
            newItem: Map<String, String>
        ): Boolean {
            return oldItem["payment"] == newItem["payment"]
        }
    }

}