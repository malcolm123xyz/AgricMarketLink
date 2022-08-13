package mx.mobile.solution.nabia04.ui.ann_fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.data.view_models.AnnViewModel
import mx.mobile.solution.nabia04.data.view_models.MainAppbarViewModel
import mx.mobile.solution.nabia04.databinding.ListFragmentBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.adapters.AnnListAdapter
import mx.mobile.solution.nabia04.util.Event
import mx.mobile.solution.nabia04.utilities.Response
import mx.mobile.solution.nabia04.utilities.Status
import javax.inject.Inject

@AndroidEntryPoint
class FragmentGeneralNot : BaseFragment<ListFragmentBinding>() {

    @Inject
    lateinit var adapter: AnnListAdapter

    private val mainAppbarViewModel by activityViewModels<MainAppbarViewModel>()

    private val viewModel by activityViewModels<AnnViewModel>()

    override fun getLayoutRes(): Int = R.layout.list_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        vb!!.recyclerView.adapter = adapter
        setupObserver()
    }

    private fun setupObserver() {

        viewModel.fetchAnn()
            .observe(viewLifecycleOwner) { users: Response<List<EntityAnnouncement>> ->
                when (users.status) {
                    Status.SUCCESS -> {
                        vb?.pb?.visibility = View.GONE
                        lifecycleScope.launch { users.data?.let { renderList(it) } }
                    }
                    Status.LOADING -> {
                        vb?.pb?.visibility = View.VISIBLE
                    }
                    Status.ERROR -> {
                        vb?.pb?.visibility = View.GONE
                        Toast.makeText(requireContext(), users.message, Toast.LENGTH_LONG).show()
                        lifecycleScope.launch { users.data?.let { renderList(it) } }
                    }
                }
            }
    }

    private fun renderList(list: List<EntityAnnouncement>) {
        val announcements: MutableList<EntityAnnouncement> = ArrayList()
        for (event in list) {
            if (event.annType == 0) {
                announcements.add(event)
            }
        }
        adapter.submitList(announcements)
    }

    override fun onResume() {
        super.onResume()
        // Set this navController as ViewModel's navController
        mainAppbarViewModel.currentNavController.value = Event(findNavController())
    }
}
