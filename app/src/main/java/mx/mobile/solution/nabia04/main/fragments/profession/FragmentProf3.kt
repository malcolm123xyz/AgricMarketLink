package mx.mobile.solution.nabia04.main.fragments.profession

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentProf3Binding
import mx.mobile.solution.nabia04.main.fragments.BaseDataBindingFragment

class FragmentProf3 : BaseDataBindingFragment<FragmentProf3Binding>() {
    override fun getLayoutRes(): Int = R.layout.fragment_prof3


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb!!.btnGoToStart.setOnClickListener {
            findNavController().navigate(R.id.action_proffragment3_to_proffragment1)
        }
    }
}
