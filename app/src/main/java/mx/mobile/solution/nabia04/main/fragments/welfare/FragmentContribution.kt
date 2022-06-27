package mx.mobile.solution.nabia04.main.fragments.welfare

import android.os.Bundle
import android.view.View
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentContributionsBinding
import mx.mobile.solution.nabia04.main.fragments.BaseDataBindingFragment

class FragmentContribution : BaseDataBindingFragment<FragmentContributionsBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_contributions

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}
