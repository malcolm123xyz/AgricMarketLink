package mx.mobile.solution.nabia04.ui.ann_fragments

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.repositories.DBRepository
import mx.mobile.solution.nabia04.data.view_models.MainAppbarViewModel
import mx.mobile.solution.nabia04.databinding.ListFragmentBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.util.Event
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.GlideApp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * This fragment is added to main graph via [NoticeBoardHostFragment]'s  [NavHostFragment]
 */
@AndroidEntryPoint
class FragmentBirthDay : BaseFragment<ListFragmentBinding>() {

    @Inject
    lateinit var repository: DBRepository

    override fun getLayoutRes(): Int = R.layout.list_fragment
    private var adapter: ListAdapter? = null
    private val mainAppbarViewModel by activityViewModels<MainAppbarViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ListAdapter()
        vb!!.recyclerView.adapter = adapter

        //repository?.refreshDatabase(false);

        listenForLoadingStatus()
        lifecycleScope.launch {
            observeLiveData()
        }
    }

    private suspend fun observeLiveData() {
        val data = repository.fetchUserData().data
        if (data != null) {
            setData(data)
        }

    }

    private fun listenForLoadingStatus() {
//        annloadingStatus.value.observe(
//        viewLifecycleOwner
//    ) { state: mx.mobile.solution.nabia04.data.view_models.State ->
//        showProgress(
//            state.isTrue
//        )
//    }
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


    private inner class ListAdapter() :
        RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
        private var data: List<EntityUserData>? = null
        private val fd = SimpleDateFormat(
            "EEE, d MMM yyyy", Locale.US
        )

        @SuppressLint("NotifyDataSetChanged")
        fun upDateList(recievedData: List<EntityUserData>?) {
            data = recievedData
            notifyDataSetChanged()
        }

        inner class MyViewHolder(val parent: View) :
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
                .inflate(R.layout.birthdays_list_item, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, i: Int) {
            holder.name.text = data?.get(i)?.fullName
            holder.dateOfBirth.text = String.format(
                "Birthday is on: %s",
                fd.format(Date(data!![i].birthDayAlarm))
            )
            val daysLeft = getDaysLeft(data!![i].birthDayAlarm);
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

        private fun getDate(l: Long): String {
            return fd.format(l)
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

    override fun onResume() {
        super.onResume()
        // Set this navController as ViewModel's navController
        mainAppbarViewModel.currentNavController.value = Event(findNavController())
    }

}
