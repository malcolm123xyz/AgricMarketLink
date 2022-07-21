package mx.mobile.solution.nabia04.ui.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.utilities.GlideApp
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ContListAdapter @Inject constructor(private val context: Context) :
    ListAdapter<Map<String, String>,
            ContListAdapter.MyViewHolder>(ContDiffCallback()) {
    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    inner class MyViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {

        val name = itemView.findViewById<TextView?>(R.id.name)
        val totalPayment = itemView.findViewById<TextView?>(R.id.payment)
        val date = itemView.findViewById<TextView?>(R.id.date)
        val userIcon = itemView.findViewById<android.widget.ImageView?>(R.id.userIcon)

        fun bind(map: Map<String, String>, i: Int) {
            Log.i("TAG", "Contributions: $map")
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
            .inflate(R.layout.contribution_list_item, parent, false)
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