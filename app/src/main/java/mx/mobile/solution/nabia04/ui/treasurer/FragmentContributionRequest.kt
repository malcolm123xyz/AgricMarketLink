package mx.mobile.solution.nabia04.ui.treasurer

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.repositories.DBRepository
import mx.mobile.solution.nabia04.data.view_models.NetworkViewModel
import mx.mobile.solution.nabia04.databinding.FragmentContRequestBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ContributionData
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentContributionRequest : BaseFragment<
        FragmentContRequestBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_cont_request

    private val storagPermission: Int = 8574

    private var contentLauncher: ActivityResultLauncher<String>? = null

    private lateinit var contData: ContributionData
    private var usersList: MutableList<EntityUserData>? = null

    private val fd = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)

    @Inject
    lateinit var dbRepository: DBRepository

    @Inject
    lateinit var sharedP: SharedPreferences

    @Inject
    lateinit var endpoint: MainEndpoint

    private val networkViewModel by viewModels<NetworkViewModel>()

    private var newImageUri = ""

    private lateinit var userSpinner: Spinner
    private lateinit var reqTypeSpinner: Spinner
    private lateinit var btnDeadline: Button
    private lateinit var tvDeadline: TextView
    private lateinit var msgEdit: EditText
    private lateinit var momoNumEdit: EditText
    private lateinit var momoNameEdit: EditText
    private lateinit var profilePic: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerPicturePickerActivityResults()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contData = ContributionData()
        contData.id = Cons.CONTRIBUTION_BACKEND_ID
        contData.momoName = ""
        contData.momoNum = ""

        msgEdit = vb?.messageEdit!!
        msgEdit.setText("Contribution towards...")
        momoNameEdit = vb?.momoName!!
        momoNumEdit = vb?.phoneNumber!!

        userSpinner = vb?.nameSpinner!!
        reqTypeSpinner = vb?.reqTypeSpinner!!
        btnDeadline = vb?.btnDeadline!!
        tvDeadline = vb?.tvDeadline!!
        profilePic = vb?.profilePic!!

        setSinners()

        vb?.send?.setOnClickListener { checkDataBeforeSend() }
        vb?.btnDeadline?.setOnClickListener {
            MyAlarmManager(requireContext()).showDayMonthPicker(object : MyAlarmManager.CallBack {
                override fun done(alarmTime: Long) {
                    val date = fd.format(Date(alarmTime))
                    contData.deadline = date
                    tvDeadline.text = date
                }
            })
        }

        vb?.selImage?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), storagPermission
                )
            } else {
                contentLauncher!!.launch("image/*")
            }
        }
    }

    private fun setSinners() {
        userSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                val imageUri = usersList?.get(i)?.imageUri ?: ""
                val folio = usersList?.get(i)?.folioNumber ?: ""
                val name = usersList?.get(i)?.fullName ?: ""
                contData.name = name
                contData.folio = folio
                contData.imageUri = imageUri

                msgEdit.setText("Contribution towards ${name}'s...")

                if (i > 0) {
                    GlideApp.with(requireContext())
                        .load(imageUri)
                        .placeholder(R.drawable.use_icon)
                        .apply(RequestOptions.circleCropTransform())
                        .signature(ObjectKey(folio))
                        .into(profilePic)
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        reqTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                contData.type = adapterView?.selectedItem.toString()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }


        lifecycleScope.launch {
            usersList = dbRepository.fetchUserData().data?.toMutableList()
            if (usersList != null) {
                Log.i("TAG", "list size1 = ${usersList!!.size}")
                usersList!!.add(0, EntityUserData())
                Log.i("TAG", "list size2 = ${usersList!!.size}")
                val namesArray = Array(usersList!!.size ?: 0) { "" }
                for ((index, item) in usersList!!.withIndex()) {
                    namesArray[index] = item.fullName ?: "NOT A MEMBER"
                }
                userSpinner.adapter =
                    ArrayAdapter(requireContext(), R.layout.simple_spinner_item, namesArray)
            }
        }

    }


    private fun checkDataBeforeSend() {
        contData.message = msgEdit.text.toString()
        contData.momoName = momoNameEdit.text.toString()
        contData.momoNum = momoNumEdit.text.toString()

        if (contData.message.isEmpty()) {
            showDialog("ERROR", "Message cannot be empty")
        } else if (contData.deadline.isEmpty()) {
            showDialog("ERROR", "Deadline not set")
        } else if (reqTypeSpinner.selectedItemPosition < 1) {
            showDialog("ERROR", "Request type/purpose cannot be empty")
        } else if (contData.momoNum.isEmpty()) {
            showDialog("ERROR", "Momo number cannot be empty")
        } else if (contData.momoName.isEmpty()) {
            showDialog("ERROR", "Momo name cannot be empty")
        } else {

            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING")
                .setMessage("You are about to send a contribution request. Do you want to continue?")
                .setPositiveButton("YES") { dialog, id ->
                    dialog.dismiss()
                    showPassWordDialog(contData)
                }.setNegativeButton("NO") { dialog, id -> dialog.dismiss() }.show()
        }
    }

    private fun showPassWordDialog(contData: ContributionData) {
        val linf = LayoutInflater.from(requireContext())
        val v = linf.inflate(R.layout.request_passwrd, null)
        val passEdit = v.findViewById<EditText>(R.id.password_edit)
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("Enter Password")
            .setView(v)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                val p = passEdit.text.toString()
                if (p == sharedP.getString(SessionManager.PASSWORD, "")) {
                    dialog.dismiss()
                    send(contData, newImageUri)
                } else {
                    showDialog("ERROR", "Wrong password")
                }
            }.setNegativeButton("CANCEL") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private fun send(contData: ContributionData, imageUri: String) {
        val pDial = ProgressDialog(requireContext())
        pDial.setTitle("NEW CONTRIBUTION REQUEST")
        pDial.setMessage("Sending new contribution request")
        pDial.setCancelable(false)
        pDial.show()
        networkViewModel.getListenableData(contData, imageUri)
            .observe(viewLifecycleOwner) { resource: Resource<String> ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        pDial.dismiss()
                        Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_move_back)
                    }
                    Status.LOADING -> {
                        pDial.setMessage(resource.message)
                    }
                    Status.ERROR -> {
                        pDial.dismiss()
                        showDialog("ERROR", "An error has occurred: ${resource.message}")
                    }
                }
            }
    }

    private fun registerPicturePickerActivityResults() {
        contentLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            val dir = File(requireContext().cacheDir.toString() + "/annPicture")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "upload_img.jpg")
            var inputStream: InputStream? = null
            try {
                inputStream = requireContext().contentResolver.openInputStream(uri!!)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            Utils.resizeImageFile(inputStream, file, 900, 750)
            val selImgUri = file.absolutePath ?: ""
            if (selImgUri.isNotEmpty()) {
                newImageUri = file.absolutePath ?: ""
            }
            GlideApp.with(requireContext())
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .into(profilePic)
        }
    }

    private fun showDialog(t: String, s: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(t)
            .setMessage(s)
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }.show()
    }
}
