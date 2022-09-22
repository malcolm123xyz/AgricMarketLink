package mx.mobile.solution.nabia04_beta1.ui.treasurer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04_beta1.App.Companion.applicationContext
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.view_models.NetworkViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentTreasurerToolsBinding
import mx.mobile.solution.nabia04_beta1.utilities.ExcelHelper
import mx.mobile.solution.nabia04_beta1.utilities.MyAlertDialog
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DuesBackup
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class FragmentTreasurerTools : Fragment() {

    @Inject
    lateinit var excelHelper: ExcelHelper

    private val permissionRequestCode: Int = 586
    private val networkViewModel by viewModels<NetworkViewModel>()

    private var _binding: FragmentTreasurerToolsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTreasurerToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewDuesPayment?.setOnClickListener { findNavController().navigate(R.id.action_move_to_dues_payment_view) }
        binding.sendContRequest?.setOnClickListener { findNavController().navigate(R.id.action_move_to_cont_request) }
        binding.updateCont?.setOnClickListener {
            val bundle = bundleOf("fragment" to "FragmentTreasurerTools")
            findNavController().navigate(R.id.action_move_cont_update, bundle)
        }
        binding.manageBackups?.setOnClickListener { findNavController().navigate(R.id.action_move_manage_backups) }
        binding.uploadMasterExcelSheet?.setOnClickListener {
            if (checkPermission()) {
                sendExcelDocToCloud()
            } else {
                requestPermission()
            }
        }
    }

    private fun getExcelFile(): File? {
        val ourAppFileDirectory = File(Environment.getExternalStorageDirectory().absolutePath)
        ourAppFileDirectory.let {
            if (it.exists()) {
                return File(ourAppFileDirectory, "Nabiadues.xlsx")
            }
        }
        return null
    }

    private fun sendExcelDocToCloud() {

        val excelFile = getExcelFile()
        if (excelFile == null) {
            Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        val pDial = MyAlertDialog(requireContext(), "", "", false).show()

        val excelUri = excelFile.absolutePath
        val backup = DuesBackup()
        backup.totalAmount = "0.0"
        networkViewModel.publishExcel("dues/Nabiadues.xlsx", excelUri)
            .observe(viewLifecycleOwner) { response: Response<String> ->
                when (response.status) {
                    Status.SUCCESS -> {
                        pDial.dismiss()
                        Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                    }
                    Status.LOADING -> {
                        pDial.setMessage(response.data.toString())
                    }
                    Status.ERROR -> {
                        pDial.dismiss()
                        Toast.makeText(
                            requireContext(),
                            "An error has occurred: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {}
                }
            }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result =
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            val result1 =
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data =
                    Uri.parse(String.format("package:%s", applicationContext().packageName))
                startActivityForResult(intent, 2296)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, 2296)
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                permissionRequestCode
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2296) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.i("TAG", "Permission Granted")
                    sendExcelDocToCloud()
                } else {
                    Log.i("TAG", "Permission not Granted")
                    Toast.makeText(
                        requireContext(),
                        "Allow permission for storage access!",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            permissionRequestCode -> if (grantResults.isNotEmpty()) {
                val readExtStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val writeExtStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (readExtStorage && writeExtStorage) {
                    Log.i("TAG", "Permission Granted")
                    sendExcelDocToCloud()
                } else {
                    Log.i("TAG", "Permission not Granted")
                    Toast.makeText(
                        requireContext(),
                        "Allow permission for storage access!",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
