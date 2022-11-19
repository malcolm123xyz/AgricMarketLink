package mx.mobile.solution.nabia04_beta1.ui.welfare_fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_welfare_summary.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.dao.DBdao
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.databinding.FragmentWelfareSummaryBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04_beta1.utilities.Const
import mx.mobile.solution.nabia04_beta1.utilities.ExcelHelper
import mx.mobile.solution.nabia04_beta1.utilities.GlideApp
import javax.inject.Inject


@AndroidEntryPoint
class FragmentUserDuesSummary : Fragment() {

    @Inject
    lateinit var excelHelper: ExcelHelper

    @Inject
    lateinit var dBdao: DBdao

    @Inject
    lateinit var sharedP: SharedPreferences

    private lateinit var adapter: ArrayAdapter<String>

    private var _binding: FragmentWelfareSummaryBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelfareSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoading(true)
        val selName: String = excelHelper.getName(userFolioNumber)
        val selPos = excelHelper.names.indexOf(selName)

        if (isTreasurer()) {
            binding.spinnerHolder.visibility = View.VISIBLE
            setUpSpinner(selPos)
        } else {
            binding.spinnerHolder.visibility = View.GONE
            refreshViews(selPos)
        }

        if (selName.isEmpty()) {
            status_txt.text = "Summary not available. You have not paid any dues"
            status_txt.setTextColor(R.color.pink)
        }

        showLoading(false)

    }

    private fun isTreasurer(): Boolean {
        val clearance = sharedP.getString(Const.CLEARANCE, "")
        return clearance == Const.POS_TREASURER || userFolioNumber == "13786"

    }

    private fun setUpSpinner(selPos: Int) {
        binding.userDuesSpinner.onItemSelectedListener = OnSpinnerItemClick()
        adapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item, excelHelper.names)
        binding.userDuesSpinner.adapter = adapter
        binding.userDuesSpinner.setSelection(selPos)
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
        if (i > -1) {
            lifecycleScope.launch {
                val userTotal = excelHelper.getUserTotal(i)
                val numOfMonthsPaid = userTotal / 5
                val percentagePaged = ((numOfMonthsPaid / excelHelper.totalNumMonths) * 100).toInt()

                binding.totalAmountTv.text = "Ghc $userTotal"
                binding.numMonths.text = numOfMonthsPaid.toString()
                binding.percentageContribution.text = "${percentagePaged}%"
                binding.numMonthsOwed.text =
                    (excelHelper.totalNumMonths - numOfMonthsPaid).toString()
                binding.rank.text = excelHelper.getRank(i).toString()

                if (percentagePaged <= 29) {
                    binding.headerHolder.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.bad_standing
                        )
                    )
                    binding.goodStandingTv.text = "Not in Good Standing"
                } else if (percentagePaged in 30..69) {
                    binding.headerHolder.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.average_standing
                        )
                    )
                    binding.goodStandingTv.text = "Average Good Standing"
                } else {
                    binding.headerHolder.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.good_standing
                        )
                    )
                    binding.goodStandingTv.text = "Good Standing"
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
                    .into(binding.image)
            }
        }
    }

    inner class OnSpinnerItemClick : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
            if (i > 0) {
                refreshViews(i)
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {}
    }

    private suspend fun loadImage(folio: String): EntityUserData? {
        return withContext(Dispatchers.IO) {
            dBdao.userData(folio)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
