package mx.mobile.solution.nabia04.main.fragment.notice_board

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.viewmodel.MainAppbarViewModel
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.ListFragmentBinding
import mx.mobile.solution.nabia04.main.MainActivity.Companion.annViewModel
import mx.mobile.solution.nabia04.main.MainActivity.Companion.annloadingStatus
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment
import mx.mobile.solution.nabia04.main.util.Event
import mx.mobile.solution.nabia04.room_database.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.room_database.view_models.LoadingStatus
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.GlideApp
import java.text.SimpleDateFormat
import java.util.*


class FragmentGeneralNot : BaseDataBindingFragment<ListFragmentBinding>() {

    private var activityLauncher: ActivityResultLauncher<Intent>? = null
    private val TAG: String = "FragmentGeneralNot"
    private var adapter: ListAdapter? = null

    private val mainAppbarViewModel by activityViewModels<MainAppbarViewModel>()

    override fun getLayoutRes(): Int = R.layout.list_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ListAdapter()
        vb!!.recyclerView.adapter = adapter
        listenForLoadingStatus()
        observeLiveData()
    }

    private fun observeLiveData() {
        annViewModel.data.observe(viewLifecycleOwner) { data: List<EntityAnnouncement>? ->
            if (data != null) {
                setData(data)
            }
        }
    }

    private fun listenForLoadingStatus() {
        annloadingStatus.value.observe(viewLifecycleOwner) { loadingStatus: LoadingStatus ->
            showProgress(
                loadingStatus.isLoading
            )
        }
    }

    private fun setData(list: List<EntityAnnouncement>) {
        val announcements: MutableList<EntityAnnouncement> = ArrayList()
        object : BackgroundTasks() {
            override fun onPreExecute() {}
            override fun doInBackground() {
                for (event in list) {
                    if (event.type == 0) {
                        announcements.add(event)
                    }
                }
                announcements.sortWith { obj1: EntityAnnouncement, obj2: EntityAnnouncement ->
                    obj2.id.compareTo(obj1.id)
                }
            }

            override fun onPostExecute() {
                adapter?.upDateList(announcements)
            }
        }.execute()
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            vb!!.shimmer.startShimmer()
            vb!!.shimmer.setVisibility(View.VISIBLE)
            vb!!.recyclerView.setVisibility(View.INVISIBLE)
        } else {
            vb!!.shimmer.stopShimmer()
            vb!!.shimmer.setVisibility(View.GONE)
            vb!!.recyclerView.visibility = View.VISIBLE
        }
    }

    private inner class ListAdapter() :
        RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
        private var data: List<EntityAnnouncement>? = null
        private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

        @SuppressLint("NotifyDataSetChanged")
        fun upDateList(receivedData: List<EntityAnnouncement>?) {
            data = receivedData
            notifyDataSetChanged()
        }

        inner class MyViewHolder(val parent: View) :
            RecyclerView.ViewHolder(parent) {
            val topic: TextView = itemView.findViewById(R.id.heading)
            val time: TextView = itemView.findViewById(R.id.time)
            val annPicture: ImageView = itemView.findViewById(R.id.ann_picture)

        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.announcement_list_item, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, i: Int) {
            val topic = data!![i].heading
            val date = getDate(data!![i].id)
            holder.topic.text = topic
            holder.time.text = date
            if (data!![i].isRead) {
                holder.topic.setTypeface(null)
            }

            val imagUri: String = data!![i].imageUri ?: ""

            GlideApp.with(requireContext())
                .load(imagUri)
                .signature(ObjectKey(data!![i].id))
                .placeholder(R.drawable.photo_galary)
                .addListener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean
                        ): Boolean {
                            holder.annPicture.visibility = View.GONE
                            return false }
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable?>?, dataSource: DataSource?,
                                                 isFirstResource: Boolean): Boolean {
                        holder.annPicture.visibility = View.VISIBLE
                        return false
                    }
                }).into(holder.annPicture)
            holder.parent.setOnClickListener { view: View? ->
                val bundle = bundleOf("folio" to data!![i].id)
                findNavController().navigate(R.id.action_gen_not_to_events_not, bundle)
            }
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

    override fun onResume() {
        super.onResume()
        // Set this navController as ViewModel's navController
        mainAppbarViewModel.currentNavController.value = Event(findNavController())
    }
}