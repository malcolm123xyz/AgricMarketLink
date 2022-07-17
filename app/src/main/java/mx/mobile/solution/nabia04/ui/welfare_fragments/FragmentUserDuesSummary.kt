package mx.mobile.solution.nabia04.ui.welfare_fragments

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_welfare_summary.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.dao.DBdao
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.databinding.FragmentWelfareSummaryBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import mx.mobile.solution.nabia04.utilities.GlideApp
import javax.inject.Inject

@AndroidEntryPoint
class FragmentUserDuesSummary : BaseFragment<FragmentWelfareSummaryBinding>() {

    @Inject
    lateinit var excelHelper: ExcelHelper

    @Inject
    lateinit var dao: DBdao

    private lateinit var adapter: ArrayAdapter<String>

    override fun getLayoutRes(): Int = R.layout.fragment_welfare_summary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            excelHelper.createExcel()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, excelHelper.names)
        vb?.userDuesSpinner?.adapter = adapter
        vb?.userDuesSpinner?.onItemSelectedListener = OnSpinnerItemClick()

        vb?.userDuesSpinner?.setSelection(
            (vb?.userDuesSpinner?.adapter as ArrayAdapter<String>).getPosition(
                excelHelper.getName(userFolioNumber)
            )
        )
    }

    private suspend fun CreateExcel() {
        withContext(Dispatchers.Default) {
            excelHelper.createExcel()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            container.visibility = View.GONE
            shimmer.visibility = View.VISIBLE
        } else {
            container.visibility = View.VISIBLE
            shimmer.visibility = View.GONE
        }
    }

    private fun refreshViews(i: Int) {
        lifecycleScope.launch {
            showLoading(true)

            val userTotal = excelHelper.getUserTotal(i)
            val numOfMonthsPaid = userTotal / 5
            val percentagePaged = ((numOfMonthsPaid / excelHelper.totalNumMonths) * 100).toInt()

            vb?.totalAmountTv?.text = "Ghc $userTotal"
            vb?.numMonths?.text = numOfMonthsPaid.toString()
            vb?.percentageContribution?.text = "${percentagePaged}%"
            vb?.numMonthsOwed?.text = (excelHelper.totalNumMonths - numOfMonthsPaid).toString()
            vb?.rank?.text = excelHelper.getRank(i).toString()

            if (percentagePaged <= 29) {
                vb?.headerHolder?.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.bad_standing
                    )
                )
                vb?.goodStandingTv?.text = "Not in Good Standing"
            } else if (percentagePaged in 30..69) {
                vb?.headerHolder?.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.average_standing
                    )
                )
                vb?.goodStandingTv?.text = "Average Good Standing"
            } else {
                vb?.headerHolder?.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.good_standing
                    )
                )
                vb?.goodStandingTv?.text = "Good Standing"
            }
            val folio = excelHelper.members[i].folio
            val user = loadImage(folio)
            val uri = user?.imageUri ?: ""
            val id = user?.imageId ?: ""
            GlideApp.with(requireContext())
                .load(uri)
                .placeholder(R.drawable.listitem_image_holder)
                .apply(RequestOptions.circleCropTransform())
                .signature(ObjectKey(id))
                .into(vb!!.image)
            showLoading(false)
        }
    }

    inner class OnSpinnerItemClick() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
            if (i > 0) {
                refreshViews(i)
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {}
    }

    private suspend fun loadImage(folio: String): EntityUserData? {
        return withContext(Dispatchers.IO) {
            dao.userData(folio)
        }
    }
}
