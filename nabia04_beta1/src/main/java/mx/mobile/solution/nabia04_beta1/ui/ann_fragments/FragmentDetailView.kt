package mx.mobile.solution.nabia04_beta1.ui.ann_fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.util.Linkify
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.entities.EntityAnnouncement
import mx.mobile.solution.nabia04_beta1.data.view_models.AnnViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentAnnDetailsBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04_beta1.utilities.Const
import mx.mobile.solution.nabia04_beta1.utilities.GlideApp
import mx.mobile.solution.nabia04_beta1.utilities.MyAlertDialog
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class FragmentDetailView : Fragment() {

    private var _binding: FragmentAnnDetailsBinding? = null

    private val binding get() = _binding!!

    private val viewModel by activityViewModels<AnnViewModel>()

    private lateinit var announcement: EntityAnnouncement
    private var annId: Long = 0
    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAnnDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        annId = arguments?.get("folio") as Long

        lifecycleScope.launch {
            announcement = viewModel.getAnn(annId)!!
            showAnnouncement()
        }
        binding.fabShare?.setOnClickListener { v: View? -> doShare() }
    }

    private fun showAnnouncement() {
        binding.heading?.text = announcement.heading
        binding.annBody?.text = announcement.message
        binding.date?.text = getDate(announcement.id)
        Linkify.addLinks(binding.annBody, Linkify.PHONE_NUMBERS or Linkify.WEB_URLS)

        loadImage(announcement.imageUri)

        val eventDate = announcement.eventDate
        if (announcement.annType > 0) {
            val strEventDate = fd.format(eventDate)
            binding.eventDate?.text = String.format("DATE: %s", strEventDate)
            binding.eventVenue?.text = java.lang.String.format("VENUE: %s", announcement.venue)
            binding.eventDate?.visibility = View.VISIBLE
            binding.eventVenue?.visibility = View.VISIBLE
        }
        lifecycleScope.launch {
            viewModel.setAnnAsRead(announcement)
        }
    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_notice_board_detail, menu)

            val serverDeleteItem = menu.findItem(R.id.delete_from_server)

            if (clearance == Const.POS_PRO || clearance == Const.POS_PRESIDENT ||
                clearance == Const.POS_VICE_PRESIDENT || userFolioNumber == "13786"
            ) {
                serverDeleteItem.isVisible = true
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.copy -> {
                    copy()
                    return true
                }
                R.id.delete -> {
                    delete(false, "Do you really want to delete this Announcement?")
                    return true
                }
                R.id.delete_from_server -> {
                    delete(
                        true,
                        "Do you really want to delete this Announcement from the server? This cannot be undone"
                    )
                    return true
                }
                else -> return true
            }
        }
    }

    private fun doDelete(fromServer: Boolean) {

        val pb = MyAlertDialog(requireContext(), "DELETE", "Deleting...", false)
        lifecycleScope.launch {
            pb.show()
            if (!fromServer) {
                val n = viewModel.delete(announcement)
                pb.dismiss()
                if (n > 0) {
                    findNavController().navigate(R.id.action_detail_fragment_to_view_pager)
                    return@launch
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to delete. Try again",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val retCode = viewModel.deleteFrmServer(announcement.id)
                pb.dismiss()
                if (retCode == 1) {
                    viewModel.delete(announcement)
                    findNavController().navigate(R.id.action_detail_fragment_to_view_pager)
                    return@launch
                }
                Toast.makeText(requireContext(), "Failed to delete. Try again", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun delete(fromServer: Boolean, msg: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage(msg)
            .setPositiveButton("YES") { dialog: DialogInterface, id: Int ->
                dialog.dismiss()
                doDelete(fromServer)
            }.setNegativeButton(
                "NO"
            ) { dialog: DialogInterface, id: Int -> dialog.dismiss() }.show()
    }


    @SuppressLint("NewApi")
    private fun copy() {
        val clipboard =
            getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager
        val msg = """ANNOUNCEMENT!!!${announcement.heading} ${announcement.message} 
            
            Download the Nabia04 social App at: Http://nabia04.app.com"""
        val clip = ClipData.newPlainText("announcement", msg)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun doShare() {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.type = "text/plain"
        sendIntent.putExtra(
            Intent.EXTRA_TEXT, """ANNOUNCEMENT!!!${announcement.heading} ${announcement.message} 
                    Download the Nabia04 social App at: Http://nabia04.app.com"""
        )
        startActivity(sendIntent)
    }

    private fun loadImage(link: String) {
        GlideApp.with(requireContext()).load(link)
            .addListener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any, target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.i("AnnDetails", "onLoadFailed")
                    binding.annImage?.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any, target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.i("AnnDetails", "onResourceReady")
                    binding.annImage?.visibility = View.VISIBLE
                    return false
                }
            }).into(binding.annImage)
    }

    private fun getDate(l: Long): String? {
        return fd.format(Date(l))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
