package mx.mobile.solution.nabia04_beta1.ui.database_fragments

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_database_detail.classTV
import kotlinx.android.synthetic.main.fragment_database_detail.courseTV
import kotlinx.android.synthetic.main.fragment_database_detail.dis_residence_tv
import kotlinx.android.synthetic.main.fragment_database_detail.folio_number_Tv
import kotlinx.android.synthetic.main.fragment_database_detail.fullNameTv_Nickname
import kotlinx.android.synthetic.main.fragment_database_detail.hometownTV
import kotlinx.android.synthetic.main.fragment_database_detail.houseTV
import kotlinx.android.synthetic.main.fragment_database_detail.imageView
import kotlinx.android.synthetic.main.fragment_database_detail.position1TV
import kotlinx.android.synthetic.main.fragment_database_detail.region_residence
import kotlinx.android.synthetic.main.fragment_departed_members_detail.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.data.view_models.DBViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentDepartedMembersDetailBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04_beta1.utilities.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

@AndroidEntryPoint
class FragmentDepartedMembersDetail : Fragment() {

    private var userData: EntityUserData? = null
    private lateinit var selectedFolio: String

    @Inject
    lateinit var shared: SharedPreferences

    val viewModel by activityViewModels<DBViewModel>()

    private var _binding: FragmentDepartedMembersDetailBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDepartedMembersDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        selectedFolio = arguments?.getString("folio") ?: ""

        if (clearance == Const.POS_PRO || clearance == Const.POS_PRESIDENT ||
            clearance == Const.POS_VICE_PRESIDENT || userFolioNumber == "13786"
        ) {
            editBiography.visibility = View.VISIBLE
            editBiography.setOnClickListener { showBioEditor() }
        }

        addTribute.setOnClickListener { addTribute() }

        lifecycleScope.launch {
            userData = viewModel.getUser(selectedFolio)
            userData?.let { showDetails(it) }
        }

    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.details_menu1, menu)

            val redoMenu = menu.findItem(R.id.redo)
            if (clearance == Const.POS_PRO || userFolioNumber == "13786") {
                redoMenu.isVisible = true
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val id = menuItem.itemId
            if (id == R.id.redo) {
                showSetDeceasedDial()
                return true
            }
            return true
        }
    }

    private fun showSetDeceasedDial() {
        if (clearance != Const.POS_PRO && userFolioNumber != "13786") {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("LIE LIE!!!")
                .setMessage("Masa only the PRO can do this ooo. Hahahahahah")
                .show()
            return
        }

        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("Living status")
            .setMessage("You about to set living status of this person as 'Alive'. Do you want to continue?")
            .setPositiveButton("YES") { d, id ->
                d.dismiss()
                setDeceased(selectedFolio)
            }.setNegativeButton("NO") { d, id ->
                d.dismiss()
            }.show()
    }


    private fun setDeceased(folio: String) {
        val pDialog = MyAlertDialog(requireContext(), "ALERT", "Setting deceased status...", false)
        pDialog.show()
        lifecycleScope.launch {
            val retValue = viewModel.setDeceaseStatus(folio, "date", 0)
            pDialog.dismiss()
            when (retValue.status) {
                Status.SUCCESS ->
                    Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                Status.ERROR -> {
                    showAlertDialog("ERROR", retValue.message.toString())
                }
                else -> {}
            }
            this.cancel()
        }
    }

    private fun setBiography(biography: String) {
        val pDialog =
            MyAlertDialog(requireContext(), "BIOGRAPHY", "Sending Biography... Please wait", false)
        pDialog.show()
        lifecycleScope.launch {
            val retValue = viewModel.setBiography(biography, selectedFolio)
            pDialog.dismiss()
            when (retValue.status) {
                Status.SUCCESS ->
                    Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                Status.ERROR -> {
                    showAlertDialog("ERROR", retValue.message.toString())
                }
                else -> {}
            }
            this.cancel()
        }
    }

    private fun addTribute() {
        val v = layoutInflater.inflate(R.layout.tribute_edit, null)
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("ADD TRIBUTE")
        dialog.setView(v)
        val editText = v.findViewById<EditText>(R.id.editBiography)
        dialog.setPositiveButton("ADD") { d, id ->
            d.dismiss()
            val s = editText.text.toString()
            if (s.isEmpty()) {
                Toast.makeText(requireContext(), "Tribute is empty", Toast.LENGTH_SHORT).show()
            } else {
                sendTribute(s)
            }
        }
        dialog.setNegativeButton("CANCEL") { d, id -> d.dismiss() }
        dialog.show()
    }

    private fun sendTribute(tribute: String) {

        val pDialog = MyAlertDialog(requireContext(), "TRIBUTE", "Sending Tribute...", false)
        pDialog.show()
        lifecycleScope.launch {
            val name = shared.getString(SessionManager.USER_FULL_NAME, "").toString()
            try {
                var tributes: MutableList<Map<String, String>> = ArrayList()
                val newTribute = userData?.tributes ?: ""
                if (newTribute.isNotEmpty()) {
                    tributes = convertMap(userData!!.tributes)
                }
                val tributeMap: MutableMap<String, String> = HashMap()
                tributeMap["tribute"] = tribute
                tributeMap["from"] = name
                tributes.add(tributeMap)
                val strTribute = convertToString(tributes)
                val response = viewModel.sendTribute(selectedFolio, strTribute)
                pDialog.dismiss()
                when (response.status) {
                    Status.SUCCESS ->
                        Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                    Status.ERROR -> {
                        showAlertDialog("ERROR", response.message.toString())
                    }
                    else -> {}
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
                if (ex is SocketTimeoutException || ex is SSLHandshakeException || ex is UnknownHostException) {
                    showAlertDialog("Error", "Cause: NO INTERNET CONNECTION")
                } else {
                    showAlertDialog("Error", "\"UNKNOWN ERROR\"")
                }
            }
        }

    }

    private fun convertToString(obj: MutableList<Map<String, String>>): String {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Map<String, String>>>() {}.type
        return gson.toJson(obj, type)
    }

    private fun convertMap(tributes: String?): MutableList<Map<String, String>> {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Map<String, String>>>() {}.type
        return gson.fromJson(tributes, type)
    }

    private fun showBioEditor() {
        val v = layoutInflater.inflate(R.layout.biography_edit, null)
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("EDIT BIOGRAPHY")
        dialog.setView(v)
        val editText = v.findViewById<EditText>(R.id.editBiography)
        editText.setText(biography.text.toString())
        dialog.setPositiveButton("SEND") { d, id ->
            d.dismiss()
            val s = editText.text.toString()
            if (s.isEmpty()) {
                Toast.makeText(requireContext(), "Biography is empty", Toast.LENGTH_SHORT).show()
            } else {
                setBiography(s)
            }
        }
        dialog.setNegativeButton("CANCEL") { d, id -> d.dismiss() }
        dialog.show()
    }

    private fun showDetails(data: EntityUserData) {
        val nickName = " (" + data.nickName + ")"
        val name: String = data.fullName + nickName
        fullNameTv_Nickname.text = name
        folio_number_Tv.text = data.folioNumber

        hometownTV.text = data.homeTown
        dis_residence_tv.text = data.districtOfResidence
        region_residence.text = data.regionOfResidence

        classTV.text = data.className
        courseTV.text = data.courseStudied
        houseTV.text = data.house
        position1TV.text = data.positionHeld

        val imageUri: String = data.imageUri
        val imageId: String = data.imageId

        GlideApp.with(requireContext())
            .load(imageUri)
            .placeholder(R.drawable.listitem_image_holder)
            .signature(ObjectKey(imageId))
            .apply(RequestOptions.circleCropTransform())
            .into(imageView)

        val s = "Died on: " + data.dateDeparted
        departed_on.text = s

        biography.text = data.biography

        if (data.tributes.isNotEmpty()) {
            val tbs = convertMap(data.tributes)
            for (tribute in tbs) {
                val view = layoutInflater.inflate(R.layout.tribute, null)
                view.findViewById<TextView>(R.id.tribute).text = tribute["tribute"].toString()
                view.findViewById<TextView>(R.id.sender).text = "By: " + tribute["from"].toString()
                tribute_holder.addView(view)
            }
        }
    }

    private fun showAlertDialog(t: String, s: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(t)
            .setMessage(s)
            .setPositiveButton(
                "OK"
            ) { dialog, id -> dialog.dismiss() }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
