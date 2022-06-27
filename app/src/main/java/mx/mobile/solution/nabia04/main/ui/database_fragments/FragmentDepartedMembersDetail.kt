package mx.mobile.solution.nabia04.main.ui.database_fragments

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.preference.PreferenceManager
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentDepartedMembersDetailBinding
import mx.mobile.solution.nabia04.main.data.MainDataBase
import mx.mobile.solution.nabia04.main.data.dao.UserDataDao
import mx.mobile.solution.nabia04.main.data.entities.EntityUserData
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.GlideApp
import mx.mobile.solution.nabia04.utilities.SessionManager
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class FragmentDepartedMembersDetail : BaseFragment<FragmentDepartedMembersDetailBinding>() {

    private var userData: EntityUserData? = null
    private var endpoint: MainEndpoint? = null
    private var clearance: String? = null
    private var userFolio: String? = null
    private var shared: SharedPreferences? = null
    private var selectedFolio: String? = null

    override fun getLayoutRes(): Int = R.layout.fragment_departed_members_detail
    override fun getCallBack(): OnBackPressedCallback? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shared = PreferenceManager.getDefaultSharedPreferences(requireContext())
        endpoint = getEndpointObject()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedFolio = arguments?.getString("folio")
        userFolio = shared?.getString(SessionManager.FOLIO_NUMBER, "")
        clearance = shared?.getString(Cons.CLEARANCE, "NONE")

        if (clearance == Cons.PRO || clearance == Cons.PRESIDENT ||
            clearance == Cons.VICE_PRESIDENT || userFolio == "13786") {
            editBiography.visibility = View.VISIBLE
            editBiography.setOnClickListener { showBioEditor()}
        }

        addTribute.setOnClickListener { addTribute()}

        getUserData(selectedFolio)

    }

    private fun addTribute(){
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

    private fun sendTribute(tribute: String){
        object: BackgroundTasks(){
            private var response: Response? = Response()
            override fun onPreExecute() {
                Toast.makeText(requireContext(), "Sending Tribute...", Toast.LENGTH_SHORT).show()
            }
            override fun doInBackground() {
                val name = shared?.getString(SessionManager.USER_FULL_NAME, "").toString()
                response = try {
                    var tributes: MutableList<Map<String, String>> = ArrayList()
                    if(userData?.tribute != null){
                        tributes = convertMap (userData!!.tribute)
                    }
                    val tributeMap: MutableMap<String, String> = HashMap()
                    tributeMap["tribute"] = tribute
                    tributeMap["from"] = name
                    tributes.add(tributeMap)
                    val strTribute = convertToString(tributes)
                    endpoint?.addTribute(selectedFolio, strTribute)?.execute()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    if (ex is SocketTimeoutException || ex is SSLHandshakeException || ex is UnknownHostException) {
                        Response().setResponse("Cause: NO INTERNET CONNECTION").setReturnCode(0)
                    } else {
                        Response().setResponse("UNKNOWN ERROR").setReturnCode(0)
                    }
                }
            }
            override fun onPostExecute() {
                if(response?.returnCode == 1){
                    Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
                }else {
                    showAlertDialog("ERROR", response!!.response)
                }
            }
        }.execute()
    }

    private fun convertToString (obj: MutableList<Map<String, String>>): String {
        val gson = Gson()
        val type = object : TypeToken<MutableList<Map<String, String>>>() {}.type
        return gson.toJson(obj, type)
    }

    fun convertMap(tributes: String?): MutableList<Map<String, String>> {
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

    private fun setBiography(biography: String){
        object: BackgroundTasks(){
            private var response: Response? = null
            override fun onPreExecute() {
                Toast.makeText(requireContext(), "Sending biography...", Toast.LENGTH_SHORT).show()
            }
            override fun doInBackground() {
                response = try {
                    endpoint?.setBiography(biography, selectedFolio)?.execute()
                } catch (ex: IOException) {
                    if (ex is SocketTimeoutException || ex is SSLHandshakeException || ex is UnknownHostException) {
                        Response().setResponse("Cause: NO INTERNET CONNECTION").setReturnCode(0)
                    } else {
                        Response().setResponse("UNKNOWN ERROR").setReturnCode(0)
                    }
                }
            }
            override fun onPostExecute() {
                if(response?.returnCode == 1){
                    Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
                }else {
                    showAlertDialog("ERROR", response!!.response)
                }
            }
        }.execute()
    }

    private fun getUserData(folio: String?){
        Log.i("DatabaseDetail", "getUserData, foliw = $folio")
        object: BackgroundTasks() {
            override fun onPreExecute() {
            }

            override fun doInBackground() {
                val dao: UserDataDao = MainDataBase.getDatabase(requireContext()).userDataDao()
                userData = dao.getUser(folio)
            }

            override fun onPostExecute() {
                userData?.let { showDetails(it) }
            }
        }.execute()
    }

    private fun showDetails(data: EntityUserData){
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
                val s = "By: "+tribute["from"].toString();
                view.findViewById<TextView>(R.id.sender).text = s
                tribute_holder.addView(view)
            }
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
