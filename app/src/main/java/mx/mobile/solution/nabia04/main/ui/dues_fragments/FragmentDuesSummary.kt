package mx.mobile.solution.nabia04.main.ui.dues_fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentWelfareSummaryBinding
import mx.mobile.solution.nabia04.main.data.MainDataBase
import mx.mobile.solution.nabia04.main.data.dao.UserDataDao
import mx.mobile.solution.nabia04.main.ui.BaseDataBindingFragment
import mx.mobile.solution.nabia04.main.ui.activities.MainActivity.Companion.excelHelper
import mx.mobile.solution.nabia04.main.ui.activities.MainActivity.Companion.excelHelperIsInitialized
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.ExcelHelper.Companion.namesList
import mx.mobile.solution.nabia04.utilities.GlideApp


class FragmentDuesSummary : BaseDataBindingFragment<FragmentWelfareSummaryBinding>() {

    private lateinit var adapter: ArrayAdapter<String>
    private var dao: UserDataDao? = null

    override fun getLayoutRes(): Int = R.layout.fragment_welfare_summary

    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = MainDataBase.getDatabase(requireContext()).userDataDao()
        adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, namesList)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb?.userDuesSpinner?.adapter = adapter
        vb?.userDuesSpinner?.onItemSelectedListener = OnSpinnerItemClick()

        if (!excelHelperIsInitialized()) {
            GlobalScope.launch {
                suspend {
                    Log.d("coroutineScope", "#runs on ${Thread.currentThread().name}")
                    delay(5000)
                    withContext(Dispatchers.Main) {
                        Log.d("coroutineScope", "#runs on ${Thread.currentThread().name}")
                        loadData()
                    }
                }.invoke()
            }
        } else {
            loadData()
        }

    }

    private fun loadData() {

        Log.i("TAG", "Observed value recieved....................... ")
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
                loadData()
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
