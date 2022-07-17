package mx.mobile.solution.nabia04.ui.database_fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.include_error.*
import kotlinx.android.synthetic.main.include_loading.*
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.view_models.DBViewModel
import mx.mobile.solution.nabia04.databinding.ListFragmentBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.adapters.DBcurrentListAdapter
import mx.mobile.solution.nabia04.utilities.Resource
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
            .observe(viewLifecycleOwner) { users: Resource<List<EntityUserData>> ->
                when (users.status) {
                    Status.SUCCESS -> {
                        showLoading(false)
                        showError(false, null)
                        users.data?.let { renderList(it) }
                    }
                    Status.LOADING -> {
                        showLoading(true)
                        showError(false, null)
                    }
                Status.ERROR -> {
                    showLoading(false)
                    showError(true, users.message)
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
            vb?.recyclerView?.visibility = View.GONE
            pbLoading.visibility = View.VISIBLE
            tvLoadingMessage.visibility = View.VISIBLE
        } else {
            vb?.recyclerView?.visibility = View.VISIBLE
            pbLoading.visibility = View.GONE
            tvLoadingMessage.visibility = View.GONE
        }
    }

    private fun renderList(list: List<EntityUserData>) {
        lifecycleScope.launch {
            val data: MutableList<EntityUserData> = ArrayList()
            for (user in list) {
                if (user.survivingStatus != 1) {
                    data.add(user)
                }
            }
            data.sortWith { obj1: EntityUserData, obj2: EntityUserData ->
                obj1.fullName.compareTo(obj2.fullName)
            }
            adapter.setData(data)
        }
    }

}
