package mx.mobile.solution.nabia04.main.ui.prof_fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentProf1Binding
import mx.mobile.solution.nabia04.main.ui.BaseDataBindingFragment

class FragmentProf1 : BaseDataBindingFragment<FragmentProf1Binding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_prof1


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb!!.btnNextPage.setOnClickListener {
            findNavController().navigate(R.id.action_profFragment1_to_profFragment2)
        }

    }

}
