package mx.mobile.solution.nabia04.main.fragment.welfare

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentContributionsBinding
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment

class FragmentContribution : BaseDataBindingFragment<FragmentContributionsBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_contributions

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}
