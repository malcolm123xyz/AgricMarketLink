package mx.mobile.solution.nabia04.authentication

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.api.client.http.javanet.NetHttpTransport
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentLoginAuthBinding
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.Cons.OK
import mx.mobile.solution.nabia04.utilities.MyAlertDialog
import mx.mobile.solution.nabia04.utilities.SessionManager
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.SignUpLoginResponse
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class LoginFragment : Fragment() {
    private var folioTextEmail: EditText? = null
    private var editTextPass: EditText? = null
    private var binding: FragmentLoginAuthBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginAuthBinding.inflate(inflater, container, false)
        val root: View = binding!!.root
        folioTextEmail = binding!!.folioEditView
        editTextPass = binding!!.passwordEditView
        binding!!.buttonLogIn.setOnClickListener(OnLogInClickListener())
        return root
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
            private val alert = MyAlertDialog(requireContext(), "LOGIN","Login in progress...")
            var response: SignUpLoginResponse? = null
            var exceptionThrown: Exception? = null
            override fun onPreExecute() {
                alert.show()
            }

            override fun doInBackground() {
                endpoint = endpointObject
                try {
                    response = endpoint?.login(folio, pass)?.execute()
                } catch (e: IOException) {
                    e.printStackTrace()
                    exceptionThrown = e
                }
            }

            override fun onPostExecute() {
                alert.dismiss()
                if (exceptionThrown != null) {
                    if (exceptionThrown is SocketTimeoutException) {
                        Log.i("NETWORK STATE: ", "Error is java.net.SocketTimeoutException")
                    }
                    if (exceptionThrown is UnknownHostException) {
                        Log.i("NETWORK STATE: ", "Error is java.net.UnknownHostException")
                    }
                    exceptionThrown!!.printStackTrace()
                    showDialog("Log in failed", exceptionThrown!!.localizedMessage as String)
                } else if (response != null) {
                    if (response?.returnCode == OK) {
                        val sessionManager = SessionManager(requireContext())
                        sessionManager.createLoginSession(response!!.loginData)
                        listener!!.onFinished()
                    } else {
                        showDialog("FAILED", response!!.response)
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

    interface Listener {
        fun onFinished()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            listener = context
        } else {
            throw RuntimeException(
                context.toString()
                        + " must implement OnFragmentInteractionListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        private var endpoint: MainEndpoint? = null
        private var listener: Listener? = null
        val endpointObject: MainEndpoint?
            get() {
                if (endpoint == null) {
                    val builder: MainEndpoint.Builder =
                        MainEndpoint.Builder(NetHttpTransport(),
                            AndroidJsonFactory(), null
                        )
                            .setRootUrl(Cons.ROOT_URL)
                    endpoint = builder.build()
                }
                return endpoint
            }
    }
}