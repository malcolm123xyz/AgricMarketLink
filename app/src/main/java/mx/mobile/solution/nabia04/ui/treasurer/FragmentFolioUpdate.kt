package mx.mobile.solution.nabia04.ui.treasurer

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.api.client.http.javanet.NetHttpTransport
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentFolioUpdateBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.utilities.*
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.*
import java.io.File
import java.io.FileInputStream
import java.util.*

@AndroidEntryPoint
class FragmentFolioUpdate : BaseFragment<
        FragmentFolioUpdateBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_folio_update


    private lateinit var spinner: Spinner
    private lateinit var btnDeadline: Button
    private lateinit var msgEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        msgEdit = vb?.messageEdit!!
        spinner = vb?.nameSpinner!!
        btnDeadline = vb?.btnDeadline!!

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                if (i == 1) {
                    msgEdit.visibility = View.VISIBLE
                } else {
                    msgEdit.visibility = View.GONE
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        btnDeadline.setOnClickListener {
            if (spinner.selectedItemPosition == 1) {
                val s = msgEdit.text.toString()
                if (s.isEmpty()) {
                    Toast.makeText(requireContext(), "Invalide folio number", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    lifecycleScope.launch {
                        val alert =
                            MyAlertDialog(requireContext(), "", "Sending folio...", false).show()
                        val response = withContext(Dispatchers.IO) {
                            sendFolio(s)
                        }
                        alert.dismiss()
                        if (response.status == Status.SUCCESS.toString()) {
                            Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                        } else {
                            showDialog("FAILED", response.message)
                        }
                    }
                }
            } else if (spinner.selectedItemPosition == 2) {
                sendBulk()
            }
        }
    }

    private fun sendFolio(s: String): ResponseString {
        val loginData = LoginData()
        loginData.folioNumber = s
        return endpoint.uploadUser(loginData).execute()
    }

    val endpoint: MainEndpoint
        get() {
            val builder = MainEndpoint.Builder(
                NetHttpTransport(), AndroidJsonFactory(), null
            ).setRootUrl("https://gtuc123.appspot.com/_ah/api/")
            return builder.build()
        }

    private fun sendBulk() {
        val duesDir =
            File(Environment.getExternalStorageDirectory().absolutePath, "Nabia04_database")
        val file = File(duesDir, "database.xlsx")
        val workbook: Workbook
        try {
            val workbookStream = FileInputStream(file)
            workbook = WorkbookFactory.create(workbookStream)
            val sheet = workbook?.getSheetAt(0)
            val formatter = DataFormatter()
            val userList: MutableList<DatabaseObject> = ArrayList()
            if (sheet != null) {
                for (i in 1 until sheet.lastRowNum) {
                    val u = DatabaseObject()
                    //u.className = formatter.formatCellValue(sheet.getRow(i).getCell(13))
                    u.contact = formatter.formatCellValue(sheet.getRow(i).getCell(9))
                    //u.courseStudied = formatter.formatCellValue(sheet.getRow(i).getCell(14))
                    //u.districtOfResidence = formatter.formatCellValue(sheet.getRow(i).getCell(7))
                    u.email = formatter.formatCellValue(sheet.getRow(i).getCell(11))
                    u.folioNumber = formatter.formatCellValue(sheet.getRow(i).getCell(2))
                    u.homeTown = formatter.formatCellValue(sheet.getRow(i).getCell(6))
                    //u.house = formatter.formatCellValue(sheet.getRow(i).getCell(12))
                    u.nickName = formatter.formatCellValue(sheet.getRow(i).getCell(3))
                    u.jobDescription = formatter.formatCellValue(sheet.getRow(i).getCell(19))
                    u.specificOrg = formatter.formatCellValue(sheet.getRow(i).getCell(18))
                    //u.employmentStatus = formatter.formatCellValue(sheet.getRow(i).getCell(16))
                    u.employmentSector = formatter.formatCellValue(sheet.getRow(i).getCell(17))
                    u.nameOfEstablishment = formatter.formatCellValue(sheet.getRow(i).getCell(20))
                    //u.establishmentRegion = formatter.formatCellValue(sheet.getRow(i).getCell(22))
                    //u.establishmentDist = formatter.formatCellValue(sheet.getRow(i).getCell(21))
                    u.positionHeld = formatter.formatCellValue(sheet.getRow(i).getCell(15))
                    //u.regionOfResidence = formatter.formatCellValue(sheet.getRow(i).getCell(8))
                    //u.sex = formatter.formatCellValue(sheet.getRow(i).getCell(4))
                    u.fullName = formatter.formatCellValue(sheet.getRow(i).getCell(1))
                    u.survivingStatus = 0
                    userList.add(u)
                }
            }

            lifecycleScope.launch {
                val alert =
                    MyAlertDialog(requireContext(), "", "Sending bulk list...", false).show()
                val response = withContext(Dispatchers.IO) {
                    doSendBulk(userList)
                }
                alert.dismiss()
                if (response.status == Status.SUCCESS.toString()) {
                    Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                } else {
                    showDialog("FAILED", response.message)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doSendBulk(list: MutableList<DatabaseObject>): ResponseString {
        val usersListTP = UsersListTP()
        usersListTP.list = list
        return endpoint.uploadUserList(usersListTP).execute()
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
