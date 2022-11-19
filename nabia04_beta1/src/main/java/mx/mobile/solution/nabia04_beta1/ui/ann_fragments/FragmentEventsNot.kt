package mx.mobile.solution.nabia04_beta1.ui.ann_fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.include_loading.*
import mx.mobile.solution.nabia04_beta1.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04_beta1.data.view_models.AnnViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentListBinding
import mx.mobile.solution.nabia04_beta1.utilities.RateLimiter
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import java.util.concurrent.TimeUnit
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

        showData()
    }

    private fun showData() {
        Log.i("TAG", "showData()...")
        viewModel.fetchAnn()
            .observe(viewLifecycleOwner) { response: Response<List<EntityAnnouncement>> ->
                Log.i("TAG", "Data recieved in observer... Data: ${response}")
                when (response.status) {
                    Status.SUCCESS -> {
                        showLoading(false)
                        showError(false, "")
                        val announcements = response.data?.toMutableList()
                        if (announcements != null) {
                            announcements.sortWith { obj1: EntityAnnouncement, obj2: EntityAnnouncement ->
                                obj2.id.compareTo(obj1.id)
                            }
                            renderList(announcements.toList())
                        }
                    }
                    Status.LOADING -> {
                        showLoading(true)
                        showError(false, "")
                    }
                    Status.ERROR -> {
                        showLoading(false)

                        Toast.makeText(
                            requireContext(),
                            response.message,
                            Toast.LENGTH_SHORT
                        ).show()

                        if (adapter.currentList.isEmpty()) {
                            showError(true, response.message)
                        }
                    }
                    else -> {}
                }
            }

        Log.i("TAG", "Checking if should refresh announcements")
        if (shouldFetch()) {
            Log.i("TAG", "Yes, refresh")
            viewModel.refreshDB()
        } else {
            Log.i("TAG", "Don't refresh")
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

    private fun shouldFetch(): Boolean {
        if (RateLimiter.shouldFetch("Announcement", 1, TimeUnit.DAYS)) {
            return true
        }
        return false
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
