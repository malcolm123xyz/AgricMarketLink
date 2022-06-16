package mx.mobile.solution.nabia04.main.fragment.notice_board

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.util.Linkify
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.api.client.http.javanet.NetHttpTransport
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentDetailBinding
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment
import mx.mobile.solution.nabia04.room_database.entities.EntityAnnouncement
import mx.mobile.solution.nabia04.room_database.repositories.AnnDataRepository
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.GlideApp
import mx.mobile.solution.nabia04.utilities.SessionManager
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ReturnObj
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class FragmentDetailView : BaseDataBindingFragment<FragmentDetailBinding>() {
    override fun getLayoutRes(): Int = R.layout.fragment_detail

    private var endpoint: MainEndpoint? = null
    private var announcement: EntityAnnouncement? = null
    private var repository: AnnDataRepository? = null
    private var annId: Long? = null

    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    private var clearance: String = ""
    private  var folio: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedP = PreferenceManager.getDefaultSharedPreferences(requireContext())
        repository = AnnDataRepository.getInstance(requireContext())
        clearance = sharedP.getString(Cons.CLEARANCE, "").toString()
        folio = sharedP.getString(SessionManager.FOLIO_NUMBER, "13786").toString()
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        annId = arguments?.get("folio") as Long
        getAnnouncement(annId!!)
        vb?.fabShare?.setOnClickListener { v: View? -> doShare() }
        setViewScrollListener()
    }

    private fun setViewScrollListener(){
        vb?.scrollView?.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                if (scrollY > 0 && vb?.fabShare?.isShown == true) {
                    vb?.fabShare?.hide();
                } else if (scrollY < 10) {
                    vb?.fabShare?.show();
                }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_notice_board_detail, menu)

        val serverDeleteItem = menu.findItem(R.id.delete_from_server)

        if (clearance == Cons.PRO || clearance == Cons.PRESIDENT ||
            clearance == Cons.VICE_PRESIDENT || folio == "13786") {
            serverDeleteItem.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.copy) {
            copy()
            return true
        } else if (id == R.id.delete) {
            delete(false, "Do you really want to delete this Announcement?")
            return true
        } else if (id == R.id.delete_from_server) {
            delete(
                true,
                "Do you really want to delete this Announcement from the server? This cannot be undone"
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun doDelete(fromServer: Boolean) {
        val pb = ProgressDialog(requireContext())
        pb.isIndeterminate = true
        pb.setTitle("DELETE")
        pb.setMessage("Deleting...")
        object : BackgroundTasks() {
            val returnObj = ReturnObj()
            override fun onPreExecute() {
                pb.show()
            }
            override fun doInBackground() {
                if (!fromServer) {
                    repository!!.delete(announcement!!)
                    returnObj.returnCode = 1
                } else {
                    endpoint = getEndpointObject()
                    try {
                        val rObj: ReturnObj = endpoint!!.deleteFromServer(announcement?.id).execute()
                        if (rObj.returnCode == 1) {
                            repository!!.delete(announcement!!)
                        }
                        returnObj.returnCode = rObj.returnCode
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                    }
                }
            }

            override fun onPostExecute() {
                pb.dismiss()
                if (returnObj.returnCode == 1) {
                    findNavController().navigate(R.id.action_detail_fragment_to_view_pager)
                } else {
                    Toast.makeText(requireContext(), "Failed to delete. Try again", Toast.LENGTH_SHORT).show()
                }
            }
        }.execute()
    }

    private fun delete(fromServer: Boolean, msg: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage(msg)
            .setPositiveButton(
                "YES"
            ) { dialog: DialogInterface, id: Int ->
                dialog.dismiss()
                doDelete(fromServer)
            }.setNegativeButton(
                "NO"
            ) { dialog: DialogInterface, id: Int -> dialog.dismiss() }.show()
    }


    @SuppressLint("NewApi")
    private fun copy() {
        val clipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager
        val msg = """ANNOUNCEMENT!!!${announcement!!.heading} ${announcement!!.message} 
            
            Download the Nabia04 social App at: Http://nabia04.app.com"""
                    val clip = ClipData.newPlainText("label", msg)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun doShare() {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.type = "text/plain"
        sendIntent.putExtra(
            Intent.EXTRA_TEXT, """ANNOUNCEMENT!!!${announcement!!.heading} ${announcement!!.message} 
                    Download the Nabia04 social App at: Http://nabia04.app.com""")
                    startActivity(sendIntent)
    }

    fun getEndpointObject(): MainEndpoint {
        if (endpoint == null) {
            val builder = MainEndpoint.Builder(NetHttpTransport(), AndroidJsonFactory(), null)
                .setRootUrl(Cons.ROOT_URL)
            endpoint = builder.build()
        }
        return endpoint!!
    }


    private fun getAnnouncement(id: Long) {
        object : BackgroundTasks() {
            override fun onPreExecute() {}
            override fun doInBackground() {
                announcement = repository!!.getAnn(id)
            }
            override fun onPostExecute() {
                showAnnouncement()
            }
        }.execute()
    }

    private fun showAnnouncement() {
        if (announcement != null) {
            vb?.heading?.text = announcement!!.heading
            vb?.annBody?.text = announcement!!.message
            vb?.date?.setText(getDate(announcement!!.id))
            Linkify.addLinks(vb!!.annBody, Linkify.PHONE_NUMBERS or Linkify.WEB_URLS)

            loadImage (announcement?.imageUri ?: "")

            val eventDate = announcement!!.eventDate
            if (announcement!!.type > 0) {
                val strEventDate = fd.format(eventDate)
                vb?.eventDate?.text = String.format("DATE: %s", strEventDate)
                vb?.eventVenue?.text = java.lang.String.format("VENUE: %s", announcement!!.venue)
                vb?.eventDate?.visibility = View.VISIBLE
                vb?.eventVenue?.visibility = View.VISIBLE
            }
            repository!!.setAnnAsRead(announcement!!)
        }
    }

    fun loadImage(link: String){
        GlideApp.with(requireContext()).load(link)
            .addListener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>,
                    isFirstResource: Boolean): Boolean {
                    Log.i("AnnDetails", "onLoadFailed")
                    vb?.annImage?.visibility = View.GONE
                    return false
                }
                override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.i("AnnDetails", "onResourceReady")
                    vb?.annImage?.visibility = View.VISIBLE
                    return false
                }
            }).into(vb!!.annImage)
    }

    private fun getDate(l: Long): String? {
        return fd.format(Date(l))
    }
}
