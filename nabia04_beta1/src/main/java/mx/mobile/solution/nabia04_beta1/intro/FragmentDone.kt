package mx.mobile.solution.nabia04_beta1.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.databinding.FragmentDoneIntroBinding
import mx.mobile.solution.nabia04_beta1.intro.IntroActivity.Companion.skip

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class FragmentDone : Fragment() {

    private var _binding: FragmentDoneIntroBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDoneIntroBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        skip = false

        binding.signUp.setOnClickListener {
            findNavController().navigate(R.id.action_done)
        }
        binding.login.setOnClickListener {
            findNavController().navigate(R.id.action_move_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}