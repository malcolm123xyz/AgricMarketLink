package mx.mobile.solution.nabia04.ui.database_fragments

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
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
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.view_models.DBViewModel
import mx.mobile.solution.nabia04.databinding.FragmentDepartedMembersDetailBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

@AndroidEntryPoint
class FragmentDepartedMembersDetail : BaseFragment<FragmentDepartedMembersDetailBinding>() {

    private var userData: EntityUserData? = null
    private lateinit var selectedFolio: String

    override fun getLayoutRes(): Int = R.layout.fragment_departed_members_detail

    @Inject
    lateinit var shared: SharedPreferences

    val viewModel by activityViewModels<DBViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedFolio = arguments?.getString("folio") ?: ""

        if (clearance == Cons.PRO || clearance == Cons.PRESIDENT ||
            clearance == Cons.VICE_PRESIDENT || userFolioNumber == "13786"
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.details_menu1, menu)
        val redoMenu = menu.findItem(R.id.redo)
        if (clearance == Cons.PRO || userFolioNumber == "13786") {
            redoMenu.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.redo) {
            showSetDeceasedDial()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSetDeceasedDial() {
        if (clearance != Cons.PRO && userFolioNumber != "13786") {
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
        val pDialog = MyAlertDialog(requireContext(), "WORKING", "Setting deceased status...")
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
        val pDialog = MyAlertDialog(requireContext(), "WORKING", "Sending Biography... Please wait")
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
            if(s.isEmpty()){
                Toast.makeText(requireContext(), "Tribute is empty", Toast.LENGTH_SHORT).show()
            }else {
                sendTribute(s)
            }
        }
        dialog.setNegativeButton("CANCEL") { d, id -> d.dismiss() }
        dialog.show()
    }

    private fun sendTribute(tribute: String) {

        val pDialog = MyAlertDialog(requireContext(), "WORKING", "Sending Tribute...")
        pDialog.show()
        lifecycleScope.launch {
            val name = shared.getString(SessionManager.USER_FULL_NAME, "").toString()
            try {
                var tributes: MutableList<Map<String, String>> = ArrayList()
                if (userData?.tribute != null) {
                    tributes = convertMap(userData!!.tribute)
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
                    Response().setResponse("Cause: NO INTERNET CONNECTION").returnCode = 0
                } else {
                    Response().setResponse("UNKNOWN ERROR").returnCode = 0
                }
            }
            this.cancel()
        }

    }

    private fun convertToString (obj: MutableList<Map<String, String>>): String {
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
            if(s.isEmpty()){
                Toast.makeText(requireContext(), "Biography is empty", Toast.LENGTH_SHORT).show()
            }else {
                Log.i("TAG", "BIOGRAPHY : $s")
                setBiography(s)
            }
        }
        dialog.setNegativeButton("CANCEL") { d, id -> d.dismiss() }
        dialog.show()
    }

    private fun showDetails(data: EntityUserData) {
        var nickName = ""
        if (data.nickName != null) {
            nickName = " (" + data.nickName.toString() + ")"
        }
        val name: String = data.fullName.toString() + nickName
        fullNameTv_Nickname.text = name
        folio_number_Tv.text = data.folioNumber

        if (data.homeTown != null) {
            hometownTV.text = data.homeTown
        }
        if (data.districtOfResidence != null) {
            dis_residence_tv.text = data.districtOfResidence
        }
        if (data.regionOfResidence != null) {
            region_residence.text = data.regionOfResidence
        }

        if (data.className != null) {
            classTV.text = data.className
        }
        if (data.courseStudied != null) {
            courseTV.text = data.courseStudied
        }
        if (data.house != null) {
            houseTV.text = data.house
        }
        if (data.positionHeld != null) {
            position1TV.text = data.positionHeld
        }

        val imageUri: String = data.imageUri ?: ""
        val imageId: String = data.imageId ?: ""

        GlideApp.with(requireContext())
            .load(imageUri)
            .placeholder(R.drawable.listitem_image_holder)
            .signature(ObjectKey(imageId))
            .apply(RequestOptions.circleCropTransform())
            .into(imageView)

        if (data.dateDeparted != null) {
            val s = "Died on: " + data.dateDeparted
            departed_on.text = s
        }

        Log.i("TAG", "BIOGRAPHY = "+data.biography)

        if (data.biography != null) {
            biography.text = data.biography
        }

        if(data.tribute != null){
            val tbs = convertMap(data.tribute)
            for (tribute in tbs) {
                val view = layoutInflater.inflate(R.layout.tribute, null)
                view.findViewById<TextView>(R.id.tribute).text = tribute["tribute"].toString()
                val s = "By: " + tribute["from"].toString()
                view.findViewById<TextView>(R.id.sender).text = s
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

}
