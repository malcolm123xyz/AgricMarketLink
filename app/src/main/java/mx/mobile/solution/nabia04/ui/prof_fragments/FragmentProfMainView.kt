package mx.mobile.solution.nabia04.ui.prof_fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.os.Bundle
import android.text.util.Linkify
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityQuestion
import mx.mobile.solution.nabia04.data.repositories.DBRepository
import mx.mobile.solution.nabia04.data.view_models.ProfMainViewModel
import mx.mobile.solution.nabia04.databinding.FragmentProfMainBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.ui.activities.endpoint
import mx.mobile.solution.nabia04.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Question
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseString
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentProfMainView : BaseFragment<FragmentProfMainBinding>(),
    SearchView.OnQueryTextListener {

    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    override fun getLayoutRes(): Int = R.layout.fragment_prof_main

    lateinit var adapter: MyListAdapter

    @Inject
    lateinit var repository: DBRepository

    @Inject
    lateinit var sharedP: SharedPreferences

    private val viewModel by activityViewModels<ProfMainViewModel>()

    private var selectedArea = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        adapter = MyListAdapter()
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        vb!!.recyclerView.adapter = adapter

        val quest = viewModel.fetchQuestions().value?.data ?: ArrayList()

        adapter.setData(quest.toMutableList())

        setupObserver()

        vb?.fabAddUser?.setOnClickListener {
            showQuestionDial()
        }

    }

    private fun setupObserver() {

        viewModel.fetchQuestions()
            .observe(viewLifecycleOwner) { users: Response<List<EntityQuestion>> ->
                Log.i("TAG", "Data observed..........")
                when (users.status) {
                    Status.SUCCESS -> {
                        users.data?.let { adapter.submitList(it.toMutableList()) }
                        vb?.pb?.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        vb?.pb?.visibility = View.VISIBLE
                    }
                    Status.ERROR -> {
                        vb?.pb?.visibility = View.GONE
                        Toast.makeText(requireContext(), users.message, Toast.LENGTH_LONG).show()
                        if (users.data.isNullOrEmpty()) {
                            adapter.setData(ArrayList())
                        } else {
                            adapter.setData(users.data.toMutableList())
                        }
                    }
                }
            }
    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.questions_main_view, menu)

            val searchItem: MenuItem = menu.findItem(R.id.search)
            val searchView = searchItem.actionView as SearchView
            searchView.setOnQueryTextListener(this@FragmentProfMainView)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val id = menuItem.itemId
            return when (id) {
                R.id.refresh -> {
                    viewModel.refreshDB()
                    true
                }
                R.id.filter -> {
                    val menuItemView: View =
                        ActivityCompat.requireViewById(requireActivity(), R.id.filter)
                    showFilterPopup(menuItemView)
                    true
                }
                R.id.search -> {
                    viewModel.refreshDB()
                    true
                }

                else -> true
            }
        }
    }

    private fun showFilterPopup(view: View) {


        val arrayList = resources.getStringArray(R.array.employment_sector).toMutableList()
        arrayList.add(1, "All")
        val popupMenu = PopupMenu(requireContext(), view)

        for ((index, e) in arrayList.withIndex()) {
            popupMenu.menu.add(index, index, index, e)
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    adapter.filter.filter("")
                }
                else -> {
                    adapter.filter.filter(item.title.toString())
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun showQuestionDial() {
        val v: View = layoutInflater.inflate(R.layout.select_area_layout, null)
        val alert = AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setView(v)
            .show()
        val spinner = v.findViewById<Spinner>(R.id.select_spinner)
        val questionEdit = v.findViewById<TextView>(R.id.message)
        val sendButt = v.findViewById<Button>(R.id.send)
        spinner.onItemSelectedListener = OnSpinnerItemClick(questionEdit)
        sendButt.setOnClickListener {
            val txt = questionEdit.text.toString()
            if (selectedArea.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Selected Area cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (txt.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Question input cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                lifecycleScope.launch {
                    val dial =
                        MyAlertDialog(requireContext(), "", "Sending Question...", false).show()
                    val response = withContext(Dispatchers.IO) {
                        val user = repository.getUser(userFolioNumber)
                        val question = Question()
                        val id = System.currentTimeMillis()
                        question.id = id.toString()
                        question.area = selectedArea
                        question.folio = userFolioNumber
                        question.time = fd.format(Date(id))
                        question.question = txt
                        question.imageUrl = user.imageUri ?: ""
                        var name = "Anonymous"
                        if (!sharedP.getBoolean(Const.Q_FROM_ANANYMOUS, false)) {
                            name =
                                sharedP.getString(SessionManager.USER_FULL_NAME, "").toString()
                        }
                        question.from = name
                        sendQuestion(question)
                    }
                    dial.dismiss()
                    if (response.status == Status.SUCCESS.toString()) {
                        Toast.makeText(requireContext(), "SENT", Toast.LENGTH_SHORT).show()
                        alert.dismiss()
                        viewModel.fetchQuestions()
                    } else {
                        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                            .setTitle("Failed")
                            .setMessage("Sending failed: ${response.message}")
                    }
                }
            }
        }
    }

    private fun sendQuestion(question: Question): ResponseString {
        return try {
            val responseString = endpoint.insertQuestion(question).execute()
            if (responseString.status == Status.SUCCESS.toString()) {
                viewModel.repository.dao.insert(getEntity(question))
            }
            responseString
        } catch (e: IOException) {
            ResponseString().setStatus("ERROR").setMessage(e.localizedMessage)
        }
    }

    private inner class OnSpinnerItemClick(val questionEdit: TextView) :
        AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapter: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            selectedArea = adapter?.selectedItem.toString()
            questionEdit.isEnabled = p2 > 0
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }

    }

    private fun showPopupMenu(view: View, q: EntityQuestion) {

        val popupMenu = PopupMenu(requireContext(), view)

        popupMenu.menu.add(0, 1, 0, "Edit")
        popupMenu.menu.add(1, 2, 1, "Delete")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showEditDial(q)
                }
                2 -> {
                    deleteQuestion(q)
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun deleteQuestion(entityQuestion: EntityQuestion) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage("ARE YOU SURE YOU WANT TO DELETE THIS QUESTION?")
            .setPositiveButton("YES") { dialog, id ->
                dialog.dismiss()
                delete(getBackendQuestionObj(entityQuestion))
            }.setNegativeButton("NO") { dialog, id -> dialog.dismiss() }.show()
    }

    private fun delete(question: Question) {
        lifecycleScope.launch {
            val dial =
                MyAlertDialog(requireContext(), "", "Deleting question...", false).show()
            val response = withContext(Dispatchers.IO) {
                val response = doDelete(question)
                if (response.status == Status.SUCCESS.toString()) {
                    viewModel.repository.dao.delete(getEntity(question))
                }
                response
            }
            dial.dismiss()
            if (response.status == Status.SUCCESS.toString()) {
                Toast.makeText(requireContext(), "DELETED", Toast.LENGTH_SHORT).show()
                viewModel.fetchQuestions()
            } else {
                AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                    .setTitle("Failed")
                    .setMessage("Failed to delete: ${response.message}")
            }
        }
    }

    private fun doDelete(question: Question): ResponseString {
        return try {
            val responseString = endpoint.deleteQuestion(question).execute()
            responseString
        } catch (e: IOException) {
            ResponseString().setStatus("ERROR").setMessage(e.localizedMessage)
        }
    }


    private fun showEditDial(question: EntityQuestion) {
        val v: View = layoutInflater.inflate(R.layout.select_area_layout1, null)
        val alert = AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setView(v)
            .show()
        val questionEdit = v.findViewById<TextView>(R.id.message)
        questionEdit.text = question.question
        val sendButt = v.findViewById<Button>(R.id.send)
        sendButt.setOnClickListener {
            val txt = questionEdit.text.toString()
            if (txt.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Question input cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                lifecycleScope.launch {
                    val dial =
                        MyAlertDialog(requireContext(), "", "Sending Question...", false).show()
                    val response = withContext(Dispatchers.IO) {
                        question.question = txt
                        sendQuestion(getBackendQuestionObj(question))
                    }
                    dial.dismiss()
                    if (response.status == Status.SUCCESS.toString()) {
                        Toast.makeText(requireContext(), "SENT", Toast.LENGTH_SHORT).show()
                        alert.dismiss()
                        viewModel.fetchQuestions()
                    } else {
                        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                            .setTitle("Failed")
                            .setMessage("Sending failed: ${response.message}")
                    }
                }
            }
        }
    }

    inner class MyListAdapter : ListAdapter<EntityQuestion,
            MyListAdapter.MyViewHolder>(DiffCallback()),
        Filterable {

        private var list = mutableListOf<EntityQuestion>()

        fun setData(l: MutableList<EntityQuestion>) {
            list = l
            submitList(list)
        }

        inner class MyViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
            private val fromTV: TextView = itemView.findViewById(R.id.from)
            private val dateTV: TextView = itemView.findViewById(R.id.date)
            private val questionTV: TextView = itemView.findViewById(R.id.question)
            private val upVoteTV: TextView = itemView.findViewById(R.id.upVote)
            private val downVoteTV: TextView = itemView.findViewById(R.id.downVote)
            private val numReply: TextView = itemView.findViewById(R.id.msgIcon)
            private val profileIcon: ImageView = itemView.findViewById(R.id.icon)
            private val pb: ProgressBar = itemView.findViewById(R.id.progressBar)
            private val area: TextView = itemView.findViewById(R.id.area)
            private val menu: ImageView = itemView.findViewById(R.id.menu)
            private val copy: ImageView = itemView.findViewById(R.id.copy)


            fun bind(q: EntityQuestion, i: Int) {

                fromTV.text = q.from
                dateTV.text = q.time
                questionTV.text = q.question
                upVoteTV.text = q.upVote
                downVoteTV.text = q.downVote
                area.text = q.area

                Linkify.addLinks(questionTV, Linkify.PHONE_NUMBERS)

                GlideApp.with(requireContext())
                    .load(q.imageUrl)
                    .placeholder(R.drawable.listitem_image_holder)
                    .apply(RequestOptions.circleCropTransform())
                    .signature(ObjectKey(q.id))
                    .into(profileIcon)

                numReply.text = q.numReply

                if (q.folio == userFolioNumber) {
                    menu.visibility = View.VISIBLE
                    menu.setOnClickListener { showPopupMenu(it, q) }
                }

                parent.setOnClickListener { view: View? ->
                    val bundle = bundleOf("id" to q.id)
                    parent.findNavController().navigate(R.id.action_move_detail_view, bundle)
                }

                upVoteTV.setOnClickListener { view: View? ->
                    upVote(q, pb)
                }

                downVoteTV.setOnClickListener { view: View? ->
                    downVote(q, pb)
                }

                copy.setOnClickListener { view: View? ->
                    copy(q.question)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.question_list_item, parent, false)
            return MyViewHolder(view)
        }

        /* Gets current flower and uses it to bind view. */
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val flower = getItem(position)
            holder.bind(flower, position)

        }

        override fun getFilter(): Filter {
            return customFilter
        }

        private val customFilter = object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val queryValue = query.toString().lowercase(Locale.getDefault())
                Log.i("TAG", "Query str: $queryValue")
                val filteredList = mutableListOf<EntityQuestion>()
                if (queryValue.isEmpty()) {
                    filteredList.addAll(list)
                } else {
                    for (item in list) {
                        try {
                            val nameSearch =
                                item.area.lowercase(Locale.getDefault()).contains(queryValue) ||
                                        item.from.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        item.folio.lowercase(Locale.getDefault())
                                            .contains(queryValue) ||
                                        item.question.lowercase(Locale.getDefault())
                                            .contains(queryValue)
                            if (nameSearch) {
                                filteredList.add(item)
                            }
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                }
                val results = FilterResults()
                results.values = filteredList
                return results
            }

            override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
                submitList(filterResults?.values as MutableList<EntityQuestion>?)
            }

        }

        @SuppressLint("NewApi")
        private fun copy(str: String) {
            val clipboard =
                ContextCompat.getSystemService(
                    requireContext(),
                    ClipboardManager::class.java
                ) as ClipboardManager
            val clip = ClipData.newPlainText("question", str)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
        }

    }

    private data class VoteState(
        var id: String = "",
        var upVote: Boolean = false,
        var downVote: Boolean = false
    )


    private fun upVote(q: EntityQuestion, p: ProgressBar) {
        var voteState = getVoteStatusObj(sharedP.getString(q.id, ""))
        if (voteState != null && voteState.upVote) {
            Toast.makeText(requireContext(), "Already voted", Toast.LENGTH_SHORT).show()
            return
        } else {
            q.upVote = "${q.upVote.toInt() + 1}"
            lifecycleScope.launch {
                p.visibility = View.VISIBLE
                val response = withContext(Dispatchers.IO) {
                    sendQuestion(getBackendQuestionObj(q))
                }
                p.visibility = View.GONE
                if (response.status == Status.SUCCESS.toString()) {
                    if (voteState == null) {
                        voteState = VoteState(q.id)
                    }
                    voteState!!.upVote = true
                    sharedP.edit().putString(q.id, getVoteStatusObjToString(voteState)).apply()
                    viewModel.fetchQuestions()
                } else {
                    Toast.makeText(requireContext(), "Failed, try again", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun downVote(q: EntityQuestion, p: ProgressBar) {
        var voteState = getVoteStatusObj(sharedP.getString(q.id, ""))
        if (voteState != null && voteState.downVote) {
            Toast.makeText(requireContext(), "Already voted", Toast.LENGTH_SHORT).show()
            return
        } else {
            q.downVote = "${q.downVote.toInt() + 1}"
            lifecycleScope.launch {
                p.visibility = View.VISIBLE
                val response = withContext(Dispatchers.IO) {
                    sendQuestion(getBackendQuestionObj(q))
                }
                p.visibility = View.GONE
                if (response.status == Status.SUCCESS.toString()) {
                    if (voteState == null) {
                        voteState = VoteState(q.id)
                    }
                    voteState!!.downVote = true
                    sharedP.edit().putString(q.id, getVoteStatusObjToString(voteState)).apply()
                    viewModel.fetchQuestions()
                } else {
                    Toast.makeText(requireContext(), "Failed, try again", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }


    private class DiffCallback : DiffUtil.ItemCallback<EntityQuestion>() {
        override fun areItemsTheSame(
            oldItem: EntityQuestion,
            newItem: EntityQuestion
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: EntityQuestion,
            newItem: EntityQuestion
        ): Boolean {
            return oldItem.question == newItem.question &&
                    oldItem.upVote == newItem.upVote &&
                    oldItem.downVote == newItem.downVote
        }
    }

    private fun getBackendQuestionObj(q: EntityQuestion): Question {
        val u = Question()
        u.id = q.id
        u.folio = q.folio
        u.from = q.from ?: ""
        u.question = q.question ?: ""
        u.area = q.area ?: ""
        u.time = q.time ?: ""
        u.imageUrl = q.imageUrl ?: ""
        u.upVote = q.upVote ?: "0"
        u.downVote = q.downVote ?: "0"
        u.numReply = q.numReply ?: "0"
        u.replyList = q.replyList ?: ""
        u.visibility = q.visibility ?: true
        return u
    }

    private fun getEntity(q: Question): EntityQuestion {
        val u = EntityQuestion()
        u.id = q.id
        u.folio = q.folio
        u.from = q.from ?: ""
        u.question = q.question ?: ""
        u.area = q.area ?: ""
        u.time = q.time ?: ""
        u.imageUrl = q.imageUrl ?: ""
        u.upVote = q.upVote ?: "0"
        u.downVote = q.downVote ?: "0"
        u.numReply = q.numReply ?: "0"
        u.replyList = q.replyList ?: ""
        u.visibility = q.visibility ?: true
        return u
    }

    private fun getVoteStatusObj(strObj: String?): VoteState? {
        if (strObj.isNullOrEmpty()) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<VoteState>() {}.type
        return gson.fromJson(strObj, type)
    }

    private fun getVoteStatusObjToString(obj: VoteState?): String {
        if (obj == null) {
            return ""
        }
        val gson = Gson()
        val type = object : TypeToken<VoteState>() {}.type
        return gson.toJson(obj, type)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        adapter.filter.filter(query)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        adapter.filter.filter(newText)
        return true
    }

}
