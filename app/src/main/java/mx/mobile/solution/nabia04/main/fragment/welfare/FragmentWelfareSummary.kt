package mx.mobile.solution.nabia04.main.fragment.welfare

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentWelfareSummaryBinding
import mx.mobile.solution.nabia04.main.MainActivity.Companion.excelHelperViewModel
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.namesList
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.numMonthsOwed
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.totalMonths
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.userNumMonths
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.userTotalAmount
import mx.mobile.solution.nabia04.room_database.MainDataBase
import mx.mobile.solution.nabia04.room_database.UserDataDao
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.GlideApp
import kotlin.math.roundToLong


class FragmentWelfareSummary : BaseDataBindingFragment<FragmentWelfareSummaryBinding>() {

    private var dao: UserDataDao? = null
    private var excelhelper: ExcelHelper? = null

    override fun getLayoutRes(): Int = R.layout.fragment_welfare_summary

    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = MainDataBase.getDatabase(requireContext()).userDataDao()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, namesList)
        vb?.userDuesSpinner?.adapter = adapter
        vb?.userDuesSpinner?.onItemSelectedListener = OnSpinnerItemClick()

        excelHelperViewModel.value.observe(viewLifecycleOwner) { mExcelHelper: ExcelHelper ->
            Log.i("TAG", "Observed value recieved....................... ")
            excelhelper = mExcelHelper
            val p = ((userNumMonths / totalMonths) * 100).toInt()
            vb?.numMonths?.text = userNumMonths.toString()
            vb?.numMonthsOwed?.text = numMonthsOwed.toString()
            vb?.totalAmountTv?.text = "Ghc $userTotalAmount"
            val strPecentage = ((userNumMonths / totalMonths) * 100).roundToLong()
            vb?.percentageContribution?.text = "${strPecentage}%"
            vb?.rank?.text = excelhelper?.getRank(userTotalAmount).toString()

            if (p <= 29) {
                vb?.headerHolder?.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.bad_standing
                    )
                )
                vb?.goodStandingTv?.text = "Not in Good Standing"
            } else if (p in 30..69) {
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

        //adapter.notifyDataSetChanged()

            showProgress(false)
        }
    }

    inner class OnSpinnerItemClick() : AdapterView.OnItemSelectedListener {
        var isUserClick = false
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {

            if (isUserClick) {
                Log.i("TAG", "onItemSelected()......................." + i)
                val folio = excelhelper?.reloadUserData(i - 1)
                excelHelperViewModel.setValue(excelhelper)
                if (folio != null) {
                    loadImage(folio)
                }
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
