package mx.mobile.solution.nabia04.ui.ann_fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.include_loading.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.data.view_models.AnnViewModel
import mx.mobile.solution.nabia04.data.view_models.MainAppbarViewModel
import mx.mobile.solution.nabia04.databinding.ListFragmentBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.util.Event
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.GlideApp
import mx.mobile.solution.nabia04.utilities.Resource
import mx.mobile.solution.nabia04.utilities.Status
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class FragmentGeneralNot : BaseFragment<ListFragmentBinding>() {

    private lateinit var adapter: MyListAdapter
    private val TAG: String = "FragmentGeneralNot"

    private val mainAppbarViewModel by activityViewModels<MainAppbarViewModel>()

    override fun getLayoutRes(): Int = R.layout.list_fragment

    private val viewModel by activityViewModels<AnnViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = MyListAdapter()
        vb!!.recyclerView.adapter = adapter
        setupObserver()
    }

    private fun setupObserver() {

        viewModel.fetchAnn().observe(
            viewLifecycleOwner,
        ) { users: Resource<List<EntityAnnouncement>> ->
            when (users.status) {
                Status.SUCCESS -> {
                    Log.i("TAG", "FRAGMENT GENERAL: LOADING STATUS: SUCCESS...")
                    showLoading(false)
                    showError(false, null)
                    users.data?.let { renderList(it) }
                }
                Status.LOADING -> {
                    Log.i("TAG", "FRAGMENT GENERAL: LOADING STATUS: LOADING...")
                    showLoading(true)
                    showError(false, null)
                }
                Status.ERROR -> {
                    showLoading(false)
                    showError(true, users.message)
                    Log.i("TAG", "FRAGMENT GENERAL: LOADING STATUS: ERROR...")
                    //progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), users.message, Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    private fun showError(isError: Boolean, errorMessage: String?) {
        if (isError) {
            ivError.visibility = View.VISIBLE
            tvErrorMessage.visibility = View.VISIBLE
            tvErrorMessage.text = errorMessage
        } else {
            ivError.visibility = View.GONE
            tvErrorMessage.visibility = View.GONE
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            pbLoading.visibility = View.VISIBLE
            tvLoadingMessage.visibility = View.VISIBLE
        } else {
            pbLoading.visibility = View.GONE
            tvLoadingMessage.visibility = View.GONE
        }
    }

    private fun renderList(list: List<EntityAnnouncement>) {
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
                adapter.submitList(announcements)
            }
        }.execute()
    }

    inner class MyListAdapter() : ListAdapter<EntityAnnouncement,
            MyListAdapter.MyViewHolder>(DiffCallback) {
        private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

        inner class MyViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
            val topic: TextView = itemView.findViewById(R.id.heading)
            val time: TextView = itemView.findViewById(R.id.time)
            val annPicture: ImageView = itemView.findViewById(R.id.ann_picture)

            private var ann: EntityAnnouncement? = null


            /* Bind flower name and image. */
            fun bind(annItem: EntityAnnouncement, i: Int) {
                ann = annItem
                val topicc = annItem.heading
                val date = getDate(annItem.id)
                topic.text = topicc
                time.text = date
                if (annItem.isRead) {
                    topic.setTypeface(null)
                }

                val imagUri: String = annItem.imageUri ?: ""

                GlideApp.with(requireActivity())
                    .load(imagUri)
                    .signature(ObjectKey(annItem.id))
                    .placeholder(R.drawable.photo_galary)
                    .addListener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any,
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

                parent.setOnClickListener { view: View? ->
                    val bundle = bundleOf("folio" to annItem.id)
                    findNavController().navigate(R.id.action_gen_not_to_events_not, bundle)
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
            val flower = getItem(position)
            holder.bind(flower, position)

        }

        private fun getDate(l: Long): String {
            return fd.format(l)
        }

    }

    object DiffCallback : DiffUtil.ItemCallback<EntityAnnouncement>() {
        override fun areItemsTheSame(
            oldItem: EntityAnnouncement,
            newItem: EntityAnnouncement
        ): Boolean {
            val araTheSame = oldItem.id == newItem.id
            return araTheSame
        }

        override fun areContentsTheSame(
            oldItem: EntityAnnouncement,
            newItem: EntityAnnouncement
        ): Boolean {
            val araTheSame = oldItem.heading == newItem.heading
            return araTheSame
        }
    }

    override fun onResume() {
        super.onResume()
        // Set this navController as ViewModel's navController
        mainAppbarViewModel.currentNavController.value = Event(findNavController())
    }
}
