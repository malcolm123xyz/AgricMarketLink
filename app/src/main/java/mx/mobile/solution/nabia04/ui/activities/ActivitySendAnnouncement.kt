package mx.mobile.solution.nabia04.ui.activities

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.api.client.http.javanet.NetHttpTransport
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_send_ann.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.alarm.MyAlarmManager.CallBack
import mx.mobile.solution.nabia04.databinding.ActivitySendAnnBinding
import mx.mobile.solution.nabia04.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Announcement
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.AnnouncementResponse
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class ActivitySendAnnouncement : AppCompatActivity() {
    private var file: File? = null
    private var annType = 0
    private var annData: Announcement? = null
    private var eventDate: Long = 0
    private val format = SimpleDateFormat("EEE, d MMM yyyy, hh:mm")
    private var priority = 0
    private var contentLauncher: ActivityResultLauncher<String>? = null
    private lateinit var vbinding: ActivitySendAnnBinding

    @Inject
    lateinit var sharedP: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vbinding = DataBindingUtil.setContentView(this, R.layout.activity_send_ann)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        endpoint = endpointObject
        fabSendAnn.setOnClickListener { doSend() }
        fabAddImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this@ActivitySendAnnouncement,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@ActivitySendAnnouncement,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE
                )
            } else {
                contentLauncher!!.launch("image/*")
            }
        }
        eventTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                annType = i
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        registerPicturePickerActivityResults()
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
        if (v.getId() == R.id.priority) {
            priority = if (checked) {
                1
            } else {
                0
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
        } else if (eventCheckbox.isChecked && annType < 1) {
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
            if (annType > 0) {
                annData!!.type = annType
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
                if (p == sharedP.getString(SessionManager.PASSWORK, "")) {
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

            val alert = MyAlertDialog(this, "NEW ANNOUNCEMENT",
                "Uploading picture... Please wait")
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

        val alert = MyAlertDialog(this, "NEW ANNOUNCEMENT",
            "Sending Announcement... Please wait")

        object : BackgroundTasks() {
            var response: AnnouncementResponse? = null
            override fun onPreExecute() {
                alert.show()
            }

            override fun doInBackground() {
                try {
                    val accessToken = sharedP!!.getString(SessionManager.LOGIN_TOKEN, "")
                    response = endpoint!!.insertAnnouncement(accessToken, announcement).execute()
                    alert.dismiss()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    response = AnnouncementResponse()
                    response!!.returnCode = Cons.UNKNOWN_ERROR_CODE
                    response!!.response =
                        "An error occurred while sending the Announcement. Please try again"
                }
            }

            override fun onPostExecute() {
                alert.dismiss()
                if (response!!.returnCode != Cons.OK) {
                    showDialog("ERROR", response!!.response)
                }
            }
        }.execute()
    }

    fun pickEvent(v: View) {
        MyAlarmManager(this).showDateTimePicker(object : CallBack {
            override fun done(alarmTime: Long) {
                eventDate = alarmTime
                Log.i("MyAlarmManager", "Alarm time picked: "+format.format(eventDate))
                eventDateTv.text = format.format(eventDate)
            }
        })
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
        private var endpoint: MainEndpoint? = null
        val endpointObject: MainEndpoint?
            get() {
                if (endpoint == null) {
                    val builder =
                        MainEndpoint.Builder(NetHttpTransport(), AndroidJsonFactory(), null)
                            .setRootUrl(Cons.ROOT_URL)
                    endpoint = builder.build()
                }
                return endpoint
            }
    }
}