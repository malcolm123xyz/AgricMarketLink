package mx.mobile.solution.nabia04_beta1_beta1.ui.ann_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04_beta1.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04_beta1.data.view_models.AnnViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentListBinding
import mx.mobile.solution.nabia04_beta1.ui.ann_fragments.EventsAnnAdapter
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import javax.inject.Inject

/**
 * This fragment is added to main graph via [NoticeBoardHostFragment]'s  [NavHostFragment]
 */
@AndroidEntryPoint
class FragmentEventsNot : Fragment() {

    private var _binding: FragmentListBinding? = null

    private val binding get() = _binding!!

    @Inject
    lateinit var adapter: EventsAnnAdapter

    private val viewModel by activityViewModels<AnnViewModel>()

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
        binding.recyclerView.adapter = adapter

        setupObserver()
    }

    private fun setupObserver() {

        viewModel.fetchAnn().observe(
            viewLifecycleOwner,
        ) { users: Response<List<EntityAnnouncement>> ->
            when (users.status) {
                Status.SUCCESS -> {
                    lifecycleScope.launch { users.data?.let { renderList(it) } }
                    binding.pb?.visibility = View.GONE
                }
                Status.LOADING -> {
                    binding.pb?.visibility = View.VISIBLE
                }
                Status.ERROR -> {
                    binding.pb?.visibility = View.GONE
                    Toast.makeText(requireContext(), users.message, Toast.LENGTH_LONG).show()
                    lifecycleScope.launch { users.data?.let { renderList(it) } }
                }
                else -> {}
            }
        }
    }

    private fun renderList(list: List<EntityAnnouncement>) {
        val announcements: MutableList<EntityAnnouncement> = ArrayList()
        for (event in list) {
            if (event.annType == 1) {
                announcements.add(event)
            }
        }
        adapter.submitList(announcements)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
