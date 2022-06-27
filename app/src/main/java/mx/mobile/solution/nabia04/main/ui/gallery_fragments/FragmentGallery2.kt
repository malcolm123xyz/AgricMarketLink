package mx.mobile.solution.nabia04.main.ui.gallery_fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentGallery2Binding
import mx.mobile.solution.nabia04.main.ui.BaseDataBindingFragment

class FragmentGallery2 : BaseDataBindingFragment<FragmentGallery2Binding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_gallery2


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb!!.btnNextPage.setOnClickListener {
            findNavController().navigate(R.id.action_galleryfragment2_to_galleryfragment3)
        }

    }

}
