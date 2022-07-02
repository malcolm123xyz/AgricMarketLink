package mx.mobile.solution.nabia04.ui.dues_fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentWelfareSummaryBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.excelHelper
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.excelHelperIsInitialized
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import mx.mobile.solution.nabia04.utilities.ExcelHelper.Companion.namesList
import mx.mobile.solution.nabia04.utilities.GlideApp

@AndroidEntryPoint
class FragmentDuesSummary : BaseFragment<FragmentWelfareSummaryBinding>() {

    private lateinit var adapter: ArrayAdapter<String>
    private var dao: mx.mobile.solution.nabia04.data.dao.UserDataDao? = null

    override fun getLayoutRes(): Int = R.layout.fragment_welfare_summary

    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao =
            mx.mobile.solution.nabia04.data.MainDataBase.getDatabase(requireContext()).userDataDao()
        adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, namesList)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb?.userDuesSpinner?.adapter = adapter
        vb?.userDuesSpinner?.onItemSelectedListener = OnSpinnerItemClick()

        if (excelHelperIsInitialized()) {
            refreshViews()
        } else {
            object : ExcelHelper() {
                override fun onExcelReady(excel: ExcelHelper) {
                    excelHelper = excel
                    refreshViews()
                }
            }.create()
        }

    }

    private fun refreshViews() {
        val percentagePaged = excelHelper.getPercentagePayment()
        vb?.numMonths?.text = excelHelper.getNumMonthsPaid().toString()
        vb?.numMonthsOwed?.text = excelHelper.getNumMonthsOwed().toString()
        vb?.totalAmountTv?.text = "Ghc ${excelHelper.getUserTotalAmount()}"
        vb?.percentageContribution?.text = "${percentagePaged}%"
        vb?.rank?.text = excelHelper.getRank(excelHelper.getUserTotalAmount()).toString()

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

        adapter.notifyDataSetChanged()
    }

    inner class OnSpinnerItemClick() : AdapterView.OnItemSelectedListener {
        var isUserClick = false
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {

            if (isUserClick) {
                Log.i("TAG", "onItemSelected()......................." + i)
                val folio = excelHelper.reloadUserData(i - 1)
                refreshViews()
                loadImage(folio)
            }
            isUserClick = true
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {}
    }


    private fun loadImage(folio: String) {
        object : BackgroundTasks() {
            private var uri: String = ""
            private var id: String = ""
            override fun onPreExecute() {
            }

            override fun doInBackground() {
                val user = dao?.getUser(folio)
                uri = user?.imageUri ?: ""
                id = user?.imageId ?: ""
            }

            override fun onPostExecute() {
                GlideApp.with(requireContext())
                    .load(uri)
                    .placeholder(R.drawable.listitem_image_holder)
                    .apply(RequestOptions.circleCropTransform())
                    .signature(ObjectKey(id))
                    .into(vb!!.image)
            }

        }.execute()

    }

    private fun showProgress(show: Boolean) {
        if (show) {
            vb!!.shimmer.startShimmer()
            vb!!.shimmer.visibility = View.VISIBLE
            vb!!.container.visibility = View.INVISIBLE
        } else {
            vb!!.shimmer.stopShimmer()
            vb!!.shimmer.visibility = View.GONE
            vb!!.container.visibility = View.VISIBLE
        }
    }

}
