package mx.mobile.solution.nabia04_beta1.intro

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.databinding.FragmentLoginAuthBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity
import mx.mobile.solution.nabia04_beta1.ui.activities.endpoint
import mx.mobile.solution.nabia04_beta1.utilities.BackgroundTasks
import mx.mobile.solution.nabia04_beta1.utilities.MyAlertDialog
import mx.mobile.solution.nabia04_beta1.utilities.SessionManager
import mx.mobile.solution.nabia04_beta1.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.LoginTP
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseLoginData
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class FragmentLogin : Fragment() {

    @Inject
    lateinit var session: SessionManager

    private var folioTextEmail: EditText? = null
    private var editTextPass: EditText? = null
    private var binding: FragmentLoginAuthBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginAuthBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        folioTextEmail = binding!!.folioEditView
        editTextPass = binding!!.passwordEditView
        binding!!.buttonLogIn.setOnClickListener(OnLogInClickListener())

        binding!!.signUp.setOnClickListener {
            findNavController().navigate(R.id.action_move_to_signup)
        }
    }

    private inner class OnLogInClickListener : View.OnClickListener {
        override fun onClick(arg0: View) {
            val folio = folioTextEmail!!.text.toString()
            val pass = editTextPass!!.text.toString()
            if (folio.isEmpty() || pass.isEmpty()) {
                showDialog("INVALID", "Invalid Password")
                return
            }
            login(folio, pass)
        }
    }

    private fun login(folio: String, pass: String) {
        object : BackgroundTasks() {
            private val alert =
                MyAlertDialog(requireContext(), "LOGIN", "Login in progress...", false)
            var response: ResponseLoginData? = null
            var exceptionThrown: Exception? = null

            override fun onPreExecute() {
                alert.show()
            }

            override fun doInBackground() {

                try {
                    val logInData = LoginTP().setFolio(folio).setPassword(pass)
                    response = endpoint.userLogin(logInData).execute()
                } catch (e: IOException) {
                    e.printStackTrace()
                    exceptionThrown = e
                }
            }

            override fun onPostExecute() {
                alert.dismiss()
                if (exceptionThrown != null) {
                    exceptionThrown!!.printStackTrace()
                    showDialog("Log in failed", exceptionThrown!!.localizedMessage as String)
                } else if (response != null) {
                    if (response?.status == Status.SUCCESS.toString()) {
                        session.createLoginSession(response!!.data)
                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        showDialog("FAILED", response!!.message)
                    }
                } else {
                    showDialog("FAILED", "Unknown Error. Please try again")
                }
            }
        }.execute()
    }

    private fun showDialog(t: String, s: String) {
        AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)
            .setMessage(s)
            .setTitle(t)
            .setPositiveButton(
                "OK"
            ) { dialog, _ -> dialog.dismiss() }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}