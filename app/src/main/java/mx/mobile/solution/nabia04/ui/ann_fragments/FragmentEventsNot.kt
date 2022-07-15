package mx.mobile.solution.nabia04.ui.ann_fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.include_loading.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.data.view_models.AnnViewModel
import mx.mobile.solution.nabia04.data.view_models.MainAppbarViewModel
import mx.mobile.solution.nabia04.databinding.ListFragmentBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.adapters.AnnListAdapter
import mx.mobile.solution.nabia04.util.Event
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.Resource
import mx.mobile.solution.nabia04.utilities.Status
import javax.inject.Inject

/**
 * This fragment is added to main graph via [NoticeBoardHostFragment]'s  [NavHostFragment]
 */
@AndroidEntryPoint
class FragmentEventsNot : BaseFragment<ListFragmentBinding>() {

    override fun getLayoutRes(): Int = R.layout.list_fragment

    @Inject
    lateinit var adapter: AnnListAdapter

    private val mainAppbarViewModel by activityViewModels<MainAppbarViewModel>()
    private val viewModel by activityViewModels<AnnViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        vb!!.recyclerView.adapter = adapter

        setupObserver()
    }

    private fun setupObserver() {

        viewModel.fetchAnn().observe(
            viewLifecycleOwner,
        ) { users: Resource<List<EntityAnnouncement>> ->
            when (users.status) {
                Status.SUCCESS -> {
                    Log.i("TAG", "FRAGMENT EVENT: LOADING STATUS: SUCCESS...")
                    showLoading(false)
                    showError(false, null)
                    users.data?.let { renderList(it) }
                }
                Status.LOADING -> {
                    Log.i("TAG", "FRAGMENT EVENT: LOADING STATUS: LOADING...")
                    showLoading(true)
                    showError(false, null)
                }
                Status.ERROR -> {
                    showLoading(false)
                    showError(true, users.message)
                    Log.i("TAG", "FRAGMENT EVENT: LOADING STATUS: ERROR...")
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
                    if (event.type != 0) {
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

    override fun onResume() {
        super.onResume()
        // Set this navController as ViewModel's navController
        mainAppbarViewModel.currentNavController.value = Event(findNavController())
    }

}