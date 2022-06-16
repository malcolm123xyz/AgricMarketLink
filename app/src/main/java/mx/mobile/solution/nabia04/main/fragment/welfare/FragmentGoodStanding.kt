package mx.mobile.solution.nabia04.main.fragment.welfare

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentContHistoryBinding
import mx.mobile.solution.nabia04.databinding.FragmentGoodStandingBinding
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment

class FragmentGoodStanding : BaseDataBindingFragment<FragmentGoodStandingBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_good_standing

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

}
