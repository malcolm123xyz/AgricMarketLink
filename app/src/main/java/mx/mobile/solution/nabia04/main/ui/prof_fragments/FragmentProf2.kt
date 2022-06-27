package mx.mobile.solution.nabia04.main.ui.prof_fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentProf2Binding
import mx.mobile.solution.nabia04.main.ui.BaseDataBindingFragment


class FragmentProf2 : BaseDataBindingFragment<FragmentProf2Binding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_prof2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb!!.btnNextPage.setOnClickListener {
            findNavController().navigate(R.id.action_proffragment2_to_proffragment3)
        }
    }

}
