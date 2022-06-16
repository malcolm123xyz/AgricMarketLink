package mx.mobile.solution.nabia04.main.fragment.welfare

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.fragment_welfare_summary.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentWelfareSummaryBinding
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment
import mx.mobile.solution.nabia04.main.fragment.host_fragments.WelfareHostFragment.Companion.numMonths
import mx.mobile.solution.nabia04.main.fragment.host_fragments.WelfareHostFragment.Companion.userTotalAmount

class FragmentWelfareSummary : BaseDataBindingFragment<FragmentWelfareSummaryBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_welfare_summary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb?.totalAmount?.text = "Ghc $totalAmount"
        val percentage: Double = Math(totalAmount * userTotalAmount)
        vb?.percentageContribution?.text = "$percentage%"
        vb?.monthsDefaulted?.text = numMonths.toString()
        //vb.ranking.text =
    }

}
