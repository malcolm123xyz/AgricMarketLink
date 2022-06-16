package mx.mobile.solution.nabia04.main.fragment.welfare

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentWelfareSummaryBinding
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.numMonths
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.numMonthsOwed
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.totalAmount
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.userTotalAmount
import mx.mobile.solution.nabia04.room_database.view_models.ExcelHelperViewModel
import kotlin.math.roundToLong

class FragmentWelfareSummary : BaseDataBindingFragment<FragmentWelfareSummaryBinding>() {

    private lateinit var excelHelperViewModel: ExcelHelperViewModel

    override fun getLayoutRes(): Int = R.layout.fragment_welfare_summary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        excelHelperViewModel =
            ViewModelProvider(requireActivity()).get(ExcelHelperViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        excelHelperViewModel.value.observe(viewLifecycleOwner) { excelhelper: ExcelHelper ->
            vb?.numMonths?.text = numMonths.toString()
            vb?.numMonthsOwed?.text = numMonthsOwed.toString()
            vb?.totalAmountTv?.text = "Ghc $userTotalAmount"

            val strPecentage = ((userTotalAmount / totalAmount) * 100).roundToLong()
            vb?.percentageContribution?.text = "${strPecentage}%"

            vb?.ranking?.text = excelhelper.getRank(userTotalAmount).toString()
        }

    }

}
