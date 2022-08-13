package mx.mobile.solution.nabia04.ui.database_fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.view_models.DBViewModel
import mx.mobile.solution.nabia04.databinding.ListFragmentBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.adapters.DBcurrentListAdapter
import mx.mobile.solution.nabia04.utilities.Response
import mx.mobile.solution.nabia04.utilities.Status
import javax.inject.Inject

@AndroidEntryPoint
class FragmentCurrentMembers : BaseFragment<ListFragmentBinding>() {

    private val viewModel by activityViewModels<DBViewModel>()

    @Inject
    lateinit var adapter: DBcurrentListAdapter

    override fun getLayoutRes(): Int = R.layout.list_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        vb!!.recyclerView.adapter = adapter
        setupObserver()
    }

    private fun setupObserver() {

        viewModel.fetchUserDataList()
            .observe(viewLifecycleOwner) { users: Response<List<EntityUserData>> ->
                when (users.status) {
                    Status.SUCCESS -> {
                        users.data?.let { renderList(it.toMutableList()) }
                        vb?.pb?.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        vb?.pb?.visibility = View.VISIBLE
                    }
                    Status.ERROR -> {
                        vb?.pb?.visibility = View.GONE
                        Toast.makeText(requireContext(), users.message, Toast.LENGTH_LONG)
                            .show()
                        users.data?.let { renderList(it.toMutableList()) }
                    }
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

}
