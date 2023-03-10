@file:Suppress("KDocUnresolvedReference")

package mx.mobile.solution.nabia04.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.startMils

/**
 * Base fragment with data binding and prints lifecycle events
 *
 * LifeCycle of Fragments
 *
 * * onAttach()
 * * onCreate()
 * * onCreateView() -> View is created or Fragment returned from back stack
 * * onViewCreated()
 * * onStart()
 * * onResume()
 * * onPause()
 * * onStop()
 * * onDestroyView() fragment sent to back stack / Back navigation -> onCreateView() is called
 * * onDestroy()
 * * onDetach()
 */
abstract class BaseFragment<ViewBinding : ViewDataBinding> : Fragment() {

    /**
     * * 🔥️ Data binding that is not null(or non-nullable) after [Fragment.onDestroyView]
     * causing leak canary to show data binding related **MEMORY LEAK**
     * for this fragment when used in [ViewPager2]
     *
     * * Even with null data binding [ViewPager2] still leaks with FragmentMaxLifecycleEnforcer
     * or it's false positive, not confirmed
     */
    var vb: ViewBinding? = null

    @LayoutRes
    abstract fun getLayoutRes(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("😀 ${this.javaClass.simpleName} #${this.hashCode()}  onCreate()")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        println("🤣 ${this.javaClass.simpleName} #${this.hashCode()} onCreateView()")
        // Inflate the layout for this fragment
        vb = DataBindingUtil.inflate(inflater, getLayoutRes(), container, false)
        vb!!.lifecycleOwner = viewLifecycleOwner

        return vb!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("🤩 ${this.javaClass.simpleName} #${this.hashCode()}  onViewCreated() view: $view")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        println("🥵 ${this.javaClass.simpleName} #${this.hashCode()}  onDestroyView()")

        /*
            🔥 Without nullifying dataBinding ViewPager2 gets data binding related MEMORY LEAKS
         */
        vb = null
    }

    override fun onDestroy() {
        super.onDestroy()
        println("🥶 ${this.javaClass.simpleName} #${this.hashCode()}  onDestroy()")
    }

    override fun onDetach() {
        super.onDetach()
        println("💀 BaseDataBindingFragment onDetach() $this")
    }

    override fun onResume() {
        super.onResume()

        val time = System.currentTimeMillis() - startMils

        println("🎃 ${this.javaClass.simpleName} #${this.hashCode()} onResume() in $time")
    }

    override fun onPause() {
        super.onPause()
        println("😱 ${this.javaClass.simpleName} #${this.hashCode()} onPause()")
    }

}