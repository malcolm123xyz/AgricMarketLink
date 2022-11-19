package mx.mobile.solution.nabia04_beta1.ui.ann_fragments

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.data.repositories.DBRepository
import mx.mobile.solution.nabia04_beta1.databinding.FragmentListBinding
import mx.mobile.solution.nabia04_beta1.utilities.BackgroundTasks
import mx.mobile.solution.nabia04_beta1.utilities.GlideApp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * This fragment is added to main graph via [NoticeBoardHostFragment]'s  [NavHostFragment]
 */
@AndroidEntryPoint
class FragmentBirthDay : Fragment() {

    @Inject
    lateinit var repository: DBRepository

    private var _binding: FragmentListBinding? = null

    private val binding get() = _binding!!

    private var adapter: ListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ListAdapter()
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            binding.pb.visibility = View.VISIBLE
            observeLiveData()
            binding.pb.visibility = View.GONE
        }
    }

    private suspend fun observeLiveData() {
        val data = repository.fetchUserData().data
        if (data != null) {
            setData(data)
        }

    }

    private fun setData(list: List<EntityUserData>) {
        val announcements: MutableList<EntityUserData> =
            ArrayList()
        object : BackgroundTasks() {
            private val CURRENT_TIME: Long = System.currentTimeMillis()
            private val HOURS_24: Long = 1000 * 60 * 60 * 24
            override fun onPreExecute() {}
            override fun doInBackground() {
                for (event in list) {
                    if (event.birthDayAlarm >= CURRENT_TIME - HOURS_24) {
                        announcements.add(event)
                    }
                }
                announcements.sortWith { obj1: EntityUserData, obj2: EntityUserData ->
                    obj1.birthDayAlarm.compareTo(obj2.birthDayAlarm)
                }
            }

            override fun onPostExecute() {
                adapter?.upDateList(announcements)
            }
        }.execute()
    }


    private inner class ListAdapter :
        RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
        private var data: List<EntityUserData>? = null
        private val fd = SimpleDateFormat(
            "EEE, d MMM yyyy", Locale.US
        )

        @SuppressLint("NotifyDataSetChanged")
        fun upDateList(receivedData: List<EntityUserData>?) {
            data = receivedData
            notifyDataSetChanged()
        }

        inner class MyViewHolder(parent: View) :
            RecyclerView.ViewHolder(parent) {
            val name: TextView = itemView.findViewById(R.id.name)
            val dateOfBirth: TextView = itemView.findViewById(R.id.dateOfBirth)
            val numDaysLeft: TextView = itemView.findViewById(R.id.numberDaysleft)
            val icon: ImageView = itemView.findViewById(R.id.icon)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_birthday, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, i: Int) {
            holder.name.text = data?.get(i)?.fullName
            holder.dateOfBirth.text = String.format(
                "Birthday is on: %s",
                fd.format(Date(data!![i].birthDayAlarm))
            )
            val daysLeft = getDaysLeft(data!![i].birthDayAlarm)
            holder.numDaysLeft.text = String.format("%s Day(s) more!", daysLeft)

            Log.i("BD", "Name: " + data?.get(i)?.fullName)

            GlideApp.with(activity!!.applicationContext)
                .load(data?.get(i)?.imageUri)
                .placeholder(R.drawable.use_icon)
                .apply(RequestOptions.circleCropTransform())
                .addListener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?, target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.icon.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?, model: Any?, target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.icon.visibility = View.VISIBLE
                        return false
                    }
                })
                .into(holder.icon)
        }

        override fun getItemCount(): Int {
            return if (data == null) {
                0
            } else data!!.size
        }
    }

    fun getDaysLeft(userBirthday: Long): String {
        val diff = userBirthday - System.currentTimeMillis()
        val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        return days.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
