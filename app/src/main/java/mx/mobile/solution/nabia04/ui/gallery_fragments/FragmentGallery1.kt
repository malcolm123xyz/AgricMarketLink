package mx.mobile.solution.nabia04.ui.gallery_fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentGallery1Binding
import mx.mobile.solution.nabia04.ui.BaseFragment

class FragmentGallery1 : BaseFragment<FragmentGallery1Binding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_gallery1


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb!!.btnNextPage.setOnClickListener {
            findNavController().navigate(R.id.action_galleryFragment1_to_galleryFragment2)
        }

    }

}
