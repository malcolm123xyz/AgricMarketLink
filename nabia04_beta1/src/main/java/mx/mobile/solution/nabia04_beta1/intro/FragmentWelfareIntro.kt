package mx.mobile.solution.nabia04_beta1.intro

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.databinding.FragmentSecondBinding
import mx.mobile.solution.nabia04_beta1.intro.IntroActivity.Companion.skip

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class FragmentWelfareIntro : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    private val hideHandler = Handler(Looper.myLooper()!!)

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_move_forward)
        }

        binding.progress.setImageDrawable(
            AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.progres_dot_4
            )
        )

        binding.intro.text = "WELFARE"

        binding.text.text = resources.getText(R.string.welfare_intro_text)

        binding.text.textSize = 20f
    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.intro_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {

                android.R.id.home -> {
                    findNavController().navigateUp()
                    true
                }
                R.id.skip -> {
                    skip = true
                    findNavController().navigate(R.id.action_move_forward)
                    true
                }
                else -> true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (skip) {
            hideHandler.postDelayed(skipRunnable, 500)
        }
    }

    private val skipRunnable = Runnable {
        val bundle = bundleOf("skip" to true)
        findNavController().navigate(R.id.action_move_forward, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}