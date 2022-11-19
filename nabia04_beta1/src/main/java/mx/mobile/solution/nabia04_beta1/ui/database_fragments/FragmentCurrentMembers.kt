package mx.mobile.solution.nabia04_beta1.ui.database_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.data.view_models.DBViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentListBinding
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import javax.inject.Inject

@AndroidEntryPoint
class FragmentCurrentMembers : Fragment() {

    private val viewModel by activityViewModels<DBViewModel>()

    @Inject
    lateinit var adapter: DBcurrentListAdapter

    private var _binding: FragmentListBinding? = null

    private val binding get() = _binding!!

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

        viewModel.fetchUserDataList()
            .observe(viewLifecycleOwner) { users: Response<List<EntityUserData>> ->
                when (users.status) {
                    Status.SUCCESS -> {
                        users.data?.let { renderList(it.toMutableList()) }
                        binding.pb.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.pb.visibility = View.VISIBLE
                    }
                    Status.ERROR -> {
                        binding.pb.visibility = View.GONE
                        Toast.makeText(requireContext(), users.message, Toast.LENGTH_LONG)
                            .show()
                        users.data?.let { renderList(it.toMutableList()) }
                    }
                    else -> {}
                }
            }

    }

    private fun renderList(list: List<EntityUserData>) {
        val data: MutableList<EntityUserData> = ArrayList()
        for (user in list) {
            if (user.survivingStatus != 1) {
                data.add(user)
            }
        }
        adapter.setData(data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
