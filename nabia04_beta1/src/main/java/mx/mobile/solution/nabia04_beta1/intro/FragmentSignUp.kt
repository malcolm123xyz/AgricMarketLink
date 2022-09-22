package mx.mobile.solution.nabia04_beta1.intro

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.databinding.FragmentRegisterAuthBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity
import mx.mobile.solution.nabia04_beta1.ui.activities.endpoint
import mx.mobile.solution.nabia04_beta1.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.LoginData
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseLoginData
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class FragmentSignUp : Fragment() {

    private var binding: FragmentRegisterAuthBinding? = null

    @Inject
    lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterAuthBinding.inflate(
            inflater,
            container,
            false
        )
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.buttonRegister
            .setOnClickListener {
                signUp(
                    binding!!.fullnameEditView.text.toString(),
                    binding!!.folioEditView.text.toString(),
                    binding!!.emailEditView.text.toString(),
                    binding!!.contactEditView.text.toString(),
                    binding!!.passwordEditView1.text.toString(),
                    binding!!.passwordEditView2.text.toString()
                )
            }

        binding!!.login.setOnClickListener {
            findNavController().navigate(R.id.action_move_to_login)
        }
    }

    private fun signUp(
        fullname: String,
        folio: String,
        email: String,
        contact: String,
        passW: String,
        repeatPassW: String
    ) {
        if (validate(fullname, folio, email, contact, passW, repeatPassW)) {
            // call sign up here
            val loginData = LoginData()
            loginData.folioNumber = folio
            loginData.emailAddress = email
            loginData.fullName = fullname
            loginData.contact = contact
            loginData.password = passW
            loginData.executivePosition = Const.POSITION_NONE
            signUp(loginData)
        }
    }

    private fun signUp(loginData: LoginData) {
        object : BackgroundTasks() {
            private val alert =
                MyAlertDialog(requireContext(), "SIGN UP", "Sign up in progress...", false)
            var exceptionThrown: Exception? = null
            var response: ResponseLoginData? = null
            override fun onPreExecute() {
                alert.show()
            }

            override fun doInBackground() {
                try {
                    response = endpoint.signUp(loginData).execute()
                } catch (e: IOException) {
                    e.printStackTrace()
                    exceptionThrown = e
                }
            }

            override fun onPostExecute() {
                alert.dismiss()
                if (exceptionThrown == null && response != null) {
                    if (response!!.status == Status.SUCCESS.toString()) {
                        session.createLoginSession(response!!.data)
                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        showDialog("FAILED", response!!.message)
                    }
                } else {
                    assert(exceptionThrown != null)
                    showDialog("ERROR", exceptionThrown!!.localizedMessage as String)
                }
            }
        }.execute()
    }

    private fun validate(
        fullName: String,
        folio: String,
        email: String,
        contact: String,
        passW: String,
        repeatPassW: String
    ): Boolean {
        var isValid = true
        if (fullName.isEmpty()) {
            showDialog("INVALID NAME", "Your full name cannot be empty")
            isValid = false
        } else if (folio.isEmpty()) {
            showDialog("INVALID FOLIO NUMBER", "Folio number cannot be empty")
            isValid = false
        } else if (folio.length > 5) {
            showDialog("INVALID FOLIO NUMBER", "Folio number cannot be more that 5 numbers")
            isValid = false
        } else if (email.isEmpty()) {
            showDialog("INVALID EMAIL", "Email address cannot be empty")
            isValid = false
        } else if (contact.length < 10) {
            showDialog("INVALID CONTACT", "Contact number cannot be less than 10 digits")
            isValid = false
        } else if (passW.length < 4) {
            showDialog("INVALID PASSWORD", "Password cannot be less than 4 characters")
            isValid = false
        } else if (repeatPassW != passW) {
            showDialog("PASSWORD ERROR", "Passwords does not match")
            isValid = false
        }
        return isValid
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