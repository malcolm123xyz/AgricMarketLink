package mx.mobile.solution.nabia04.main.fragment.welfare

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentContHistoryBinding
import mx.mobile.solution.nabia04.databinding.FragmentGoodStandingBinding
import mx.mobile.solution.nabia04.databinding.FragmentToolsBinding
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment

class FragmentTools : BaseDataBindingFragment<FragmentToolsBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_tools

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

}
