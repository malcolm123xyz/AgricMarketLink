package mx.mobile.solution.nabia04.main.fragments.gallery

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentGallery3Binding
import mx.mobile.solution.nabia04.main.fragments.BaseDataBindingFragment

class FragmentGallery3 : BaseDataBindingFragment<FragmentGallery3Binding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_gallery3


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb!!.btnNextPage.setOnClickListener {
            findNavController().navigate(R.id.action_galleryfragment3_to_galleryfragment1)
        }

    }

}
