package mx.mobile.solution.nabia04.ui.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_send_ann.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.alarm.MyAlarmManager.CallBack
import mx.mobile.solution.nabia04.databinding.ActivitySendAnnBinding
import mx.mobile.solution.nabia04.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Announcement
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class ActivitySendAnnouncement : AppCompatActivity() {
    private var file: File? = null
    private var annData: Announcement? = null
    private var eventDate: Long = 0
    private val format = SimpleDateFormat("EEE, d MMM yyyy, hh:mm")
    private var priority = 0
    private var contentLauncher: ActivityResultLauncher<String>? = null
    private lateinit var vbinding: ActivitySendAnnBinding

    private var permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Inject
    lateinit var sharedP: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vbinding = DataBindingUtil.setContentView(this, R.layout.activity_send_ann)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fabSendAnn.setOnClickListener {
            doSend()
            //sendNotificationTask()
        }
        changePicture.setOnClickListener {
            if (hasPermissions(this, permissions)) {
                contentLauncher!!.launch("image/*")
            } else {
                permReqLauncher.launch(permissions)
            }

        }
        eventDateTv.setOnClickListener {
            MyAlarmManager(this).showDateTimePicker(object : CallBack {
                override fun done(alarmTime: Long) {
                    eventDate = alarmTime
                    Log.i("MyAlarmManager", "Alarm time picked: " + format.format(eventDate))
                    eventDateTv.text = format.format(eventDate)
                }
            })
        }
        registerPicturePickerActivityResults()
    }

    private val permReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                contentLauncher!!.launch("image/*")
            }
        }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }


    private fun registerPicturePickerActivityResults() {
        contentLauncher = registerForActivityResult(
            GetContent()
        ) { uri: Uri? ->
            val dir = File(this@ActivitySendAnnouncement.cacheDir.toString() + "/annPicture")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            file = File(dir, "upload_img.jpg")
            var inputStream: InputStream? = null
            try {
                inputStream = contentResolver.openInputStream(uri!!)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            annImage.visibility = View.VISIBLE
            Utils.resizeImageFile(inputStream, file, 900, 750)
            GlideApp.with(applicationContext)
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .into(annImage)
        }
    }

    fun onEventCheck(v: View) {
        val checked = (v as CheckBox).isChecked
        if (v.getId() == R.id.eventCheckbox) {
            if (checked) {
                eventTypeSpinner!!.visibility = View.VISIBLE
                eventDateTv.visibility = View.VISIBLE
                venueHolder!!.visibility = View.VISIBLE
            } else {
                eventTypeSpinner!!.visibility = View.GONE
                eventDateTv!!.visibility = View.GONE
                venueHolder!!.visibility = View.GONE
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE) { // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                contentLauncher!!.launch("image/*")
            }
        }
    }

    private fun doSend() {
        val heading = heading!!.text.toString()
        val message = message.text.toString()
        val venue = venue.text.toString()
        if (eventCheckbox.isChecked && eventDate == 0L) {
            showDialog(
                "INVALID",
                "This Announcement is about an Event, you must set an event date and numDaysLeft."
            )
        } else if (eventCheckbox.isChecked && eventTypeSpinner.selectedItemPosition < 1) {
            showDialog(
                "INVALID",
                "This Announcement about an Event, you must set an event type other than General"
            )
        } else if (heading.isEmpty() || message.isEmpty()) {
            showDialog("INVALID", "Heading or Announcement")
        } else if (eventCheckbox.isChecked && venue.isEmpty()) {
            showDialog("INVALID", "Venue cannot be empty")
        } else {
            val timeStamp = System.currentTimeMillis()
            annData = Announcement()
            annData!!.heading = heading
            annData!!.message = message
            annData!!.venue = venue
            annData!!.id = timeStamp
            annData!!.annType = 0
            annData!!.eventType = 0
            if (eventTypeSpinner.selectedItemPosition > 0) {
                annData!!.annType = 1
                annData!!.eventType = eventTypeSpinner.selectedItemPosition
            }
            annData!!.priority = priority
            annData!!.eventDate = eventDate
            checkAndSend()
        }
    }

    private fun checkAndSend() {
        if (file == null) {
            AlertDialog.Builder(this@ActivitySendAnnouncement, R.style.AppCompatAlertDialogStyle)
                .setTitle("Warning")
                .setMessage("No related picture. Do you want to continue?")
                .setPositiveButton(
                    "YES"
                ) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    showPassWordDialog()
                }.setNegativeButton(
                    "NO"
                ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }.show()
        } else {
            showPassWordDialog()
        }
    }

    private fun showPassWordDialog() {
        val linf = LayoutInflater.from(this)
        val v = linf.inflate(R.layout.request_passwrd, null)
        val passEdit = v.findViewById<EditText>(R.id.password_edit)
        AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
            .setTitle("Enter Password")
            .setView(v)
            .setCancelable(false)
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, _: Int ->
                val p = passEdit.text.toString()
                if (p == sharedP.getString(SessionManager.PASSWORD, "")) {
                    dialog.dismiss()
                    sendPicture()
                } else {
                    showDialog("ERROR", "Wrong password")
                }
            }.setNegativeButton("CANCEL") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private fun sendPicture() {
        if (file != null) {

            val alert = MyAlertDialog(
                this, "ANNOUNCEMENT", "Uploading picture... Please wait",
                false
            )
            MediaManager.get().upload(file!!.absolutePath)
                .option("resource_type", "auto")
                .unsigned("my_preset")
                .option("public_id", "Nabia04/Notice_board/" + annData!!.id)
                .option("cloud_name", "callmanager")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        alert.show()
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        alert.dismiss()
                        val secureUrl = resultData["secure_url"] as String?
                        Log.i(TAG, "Image sent success, id = $secureUrl")
                        annData!!.imageUri = secureUrl
                        sendTask(annData)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        alert.dismiss()
                        showDialog(
                            "FAILED",
                            "Failed to upload picture. Make sure you have stable interner and try again"
                        )
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        } else {
            sendTask(annData)
        }
    }

    private fun sendTask(announcement: Announcement?) {

        val alert = MyAlertDialog(
            this, "ANNOUNCEMENT",
            "Sending Announcement... Please wait", false
        )

        object : BackgroundTasks() {
            var response = ""
            override fun onPreExecute() {
                alert.show()
            }

            override fun doInBackground() {
                try {
                    val res = endpoint.insertAnnouncement(announcement).execute()
                    if (res.status == Status.ERROR.toString()) {
                        response = res.message
                    }
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    response =
                        "An error occurred while sending the Announcement. Please try again"
                }
            }

            override fun onPostExecute() {
                alert.dismiss()
                if (response.isNotEmpty()) {
                    showDialog("ERROR", response)
                }
            }
        }.execute()
    }

    private fun showDialog(t: String, s: String) {
        AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
            .setTitle(t)
            .setMessage(s)
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }.show()
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 87
        private const val TAG = "ActivitySendAnn"
    }
}