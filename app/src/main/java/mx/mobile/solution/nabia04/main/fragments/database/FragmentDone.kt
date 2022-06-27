package mx.mobile.solution.nabia04.main.fragments.database

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import kotlinx.android.synthetic.main.activity_send_ann.*
import kotlinx.android.synthetic.main.fragment_done.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.activities.ActivityUpdateUserData.Companion.selectedFolio
import mx.mobile.solution.nabia04.activities.ActivityUpdateUserData.Companion.userFolio
import mx.mobile.solution.nabia04.activities.DatabaseUpdateViewModel
import mx.mobile.solution.nabia04.databinding.FragmentDoneBinding
import mx.mobile.solution.nabia04.main.MainActivity.Companion.sharedP
import mx.mobile.solution.nabia04.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseObject
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseResponse
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ExecutionException

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentDone.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentDone : BaseFragment<FragmentDoneBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_done
    override fun getCallBack(): OnBackPressedCallback = callback
    private var endpoint: MainEndpoint? = null
    private var file: File? = null
    private val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE: Int = 3485
    private var userData: DatabaseObject? = null
    private var updateMode: DatabaseUpdateViewModel? = null
    private var contentLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateMode = ViewModelProvider(requireActivity()).get(DatabaseUpdateViewModel::class.java)
        endpoint = getEndpointObject()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerPicturePickerActivityResults()

        updateMode?.getValue()?.observe(viewLifecycleOwner) { updateModel: DatabaseObject? ->
            userData = updateModel

            val imageUri = userData?.imageUri ?: ""
            val imageId = userData?.imageId ?: ""
            val runnable = Runnable {
                Looper.prepare()
                try {
                    file = GlideApp.with(requireContext()).downloadOnly()
                        .diskCacheStrategy(DiskCacheStrategy.DATA).load(imageUri).submit().get()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

            Thread(runnable).start()

            GlideApp.with(requireContext())
                .load(imageUri)
                .placeholder(R.drawable.use_icon)
                .apply(RequestOptions.circleCropTransform())
                .signature(ObjectKey(imageId))
                .into(profile_pic)
        }

        backButtonDone.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_done_move_back)
        }
        done.setOnClickListener {
            send(userData!!)
        }

        fab_change_picture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE)
            } else {
                contentLauncher!!.launch("image/*")
            }
        }
    }

    private fun send(data: DatabaseObject) {

        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage("You are about to update the Database. Do you want to continue?")
            .setPositiveButton("YES") { dialog, id ->
                dialog.dismiss()
                showPassWordDialog()
            }.setNegativeButton("NO") { dialog, id -> dialog.dismiss() }.show()
    }

    private fun showPassWordDialog() {
        val linf = LayoutInflater.from(requireContext())
        val v = linf.inflate(R.layout.request_passwrd, null)
        val passEdit = v.findViewById<EditText>(R.id.password_edit)
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("Enter Password")
            .setView(v)
            .setCancelable(false)
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, _: Int ->
                val p = passEdit.text.toString()
                if (p == sharedP.getString(SessionManager.PASSWORK, "")) {
                    dialog.dismiss()
                    sendPicture()
                } else {
                    showAlertDialog("ERROR", "Wrong password")
                }
            }.setNegativeButton("CANCEL") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private fun sendPicture() {
        if (file != null) {

            val pDialog = MyAlertDialog(requireContext(),"UPDATING USER DATA", "Uploading picture... Please wait")
            val id = System.currentTimeMillis().toString()
            MediaManager.get().upload(file?.absolutePath)
                .option("resource_type", "auto")
                .unsigned("my_preset")
                .option("public_id", "Nabia04/database/$id")
                .option("cloud_name", "callmanager")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        pDialog.show()
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        pDialog.dismiss()
                        val url = resultData["secure_url"] as String?
                        userData?.imageUri = url
                        doUpdate(userFolio)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        pDialog.dismiss()
                        showAlertDialog("FAILED", "Failed to upload picture. Please try again")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        } else {
            doUpdate(userFolio)
        }
    }

    private fun doUpdate(id: String){
        object: BackgroundTasks(){
            val pDialog = MyAlertDialog(requireContext(),"UPDATING USER DATA","Sending Data... Please wait")
            var response: DatabaseResponse? = null
            override fun onPreExecute() {
                pDialog.show()
            }

            override fun doInBackground() {
                val token = sharedP?.getString(SessionManager.LOGIN_TOKEN, "")
                try {
                    response = if (selectedFolio.isEmpty()) {
                        endpoint?.addNewMember(userData)?.execute()
                    } else {
                        endpoint?.insertDataModel(userData)?.execute()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun onPostExecute() {
                pDialog.dismiss()
                if (response?.returnCode == Cons.OK) {
                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setTitle("SUCCESS")
                        .setCancelable(false)
                        .setMessage("Task was successful")
                        .setPositiveButton(
                            "OK"
                        ) { dialog, id -> dialog.dismiss()
                        requireActivity().finish()}.show()
                } else {
                    showAlertDialog("ERROR", "An error occurred. Please try again")
                }
            }

        }.execute()
    }

    private fun registerPicturePickerActivityResults() {
        contentLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            val dir = File(requireContext().cacheDir.toString() + "/annPicture")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            file = File(dir, "upload_img.jpg")
            var inputStream: InputStream? = null
            try {
                inputStream = requireContext().contentResolver.openInputStream(uri!!)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            profile_pic.visibility = View.VISIBLE
            Utils.resizeImageFile(inputStream, file, 900, 750)
            GlideApp.with(requireContext())
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .into(profile_pic)
        }
    }

    val callback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING").setMessage("Do you want to cancel the update?")
                .setPositiveButton("YES") { dialog: DialogInterface, id: Int ->
                    dialog.dismiss()
                    requireActivity().finish()
                }.setNegativeButton("NO") { dialog: DialogInterface, id: Int ->
                    dialog.dismiss() }.show()

        }
    }

    private fun getEndpointObject(): MainEndpoint? {
        if (endpoint == null) {
            val builder = MainEndpoint.Builder(
                AndroidHttp.newCompatibleTransport(),
                AndroidJsonFactory(),
                null
            ).setRootUrl(Cons.ROOT_URL)
            endpoint = builder.build()
        }
        return endpoint
    }

    private fun showAlertDialog(t: String, s: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(t)
            .setMessage(s)
            .setPositiveButton(
                "OK"
            ) { dialog, id -> dialog.dismiss() }.show()
    }
}