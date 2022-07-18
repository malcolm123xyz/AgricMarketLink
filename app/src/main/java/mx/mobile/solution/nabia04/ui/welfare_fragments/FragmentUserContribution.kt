package mx.mobile.solution.nabia04.ui.welfare_fragments

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentContributionsBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.adapters.ContListAdapter
import mx.mobile.solution.nabia04.utilities.GlideApp
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResourceContributionData
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class FragmentUserContribution : BaseFragment<FragmentContributionsBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_contributions

    @Inject
    lateinit var endpoint: MainEndpoint

    @Inject
    lateinit var adapter: ContListAdapter

    private lateinit var imv: ImageView
    private lateinit var tv: TextView
    private lateinit var deadline: TextView
    private lateinit var type: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imv = vb?.icon!!
        tv = vb?.msg!!
        deadline = vb?.deadline!!
        type = vb?.type!!



        lifecycleScope.launch {
            val response = withContext(Dispatchers.IO) {
                getContributions()
            }
            if (response != null) {
                val contData = response.data
                adapter.submitList(contData.contribution)
                setImageDrawable(imv, contData.imageId)
                tv.text = contData.message
                deadline.text = contData.deadline
                type.text = contData.type
            }
        }

    }

    private fun getContributions(): ResourceContributionData? {
        try {
            return endpoint.contributions.execute()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        return null
    }

    private fun setImageDrawable(img: ImageView, uri: String?) {
        if (uri != null) {
            GlideApp.with(requireContext())
                .load(uri)
                .signature(ObjectKey(uri))
                .apply(RequestOptions.circleCropTransform())
                .into(img)
        }
    }
}
