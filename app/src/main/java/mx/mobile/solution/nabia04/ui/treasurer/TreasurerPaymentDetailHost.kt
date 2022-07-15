package mx.mobile.solution.nabia04.ui.treasurer

import android.os.Bundle
import android.view.View
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentTreasurerPaymentDetailHostBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import javax.inject.Inject

@AndroidEntryPoint
class TreasurerPaymentDetailHost() :
    BaseFragment<FragmentTreasurerPaymentDetailHostBinding>() {

    @Inject
    lateinit var excelHelper: ExcelHelper

    override fun getLayoutRes(): Int = R.layout.fragment_treasurer_payment_detail_host

    companion object {
        var SORT: Int = 2
        lateinit var title: TextView
        lateinit var totalAmount: TextView
        lateinit var numContributers: TextView
        //var dataList: List<EntityDues>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = vb!!.title
        totalAmount = vb!!.totalCont
        numContributers = vb!!.numContributors

    }

}
