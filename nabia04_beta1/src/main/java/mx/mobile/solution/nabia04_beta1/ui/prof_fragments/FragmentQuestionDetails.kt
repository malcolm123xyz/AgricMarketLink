package mx.mobile.solution.nabia04_beta1.ui.prof_fragments

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
import androidx.core.content.ContextCompat
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
import kotlinx.android.synthetic.main.replay_layout_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.entities.EntityQuestion
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.data.entities.Reply
import mx.mobile.solution.nabia04_beta1.data.repositories.DBRepository
import mx.mobile.solution.nabia04_beta1.data.view_models.ProfMainViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentQuestDetailBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04_beta1.ui.activities.endpoint
import mx.mobile.solution.nabia04_beta1.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.Question
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseString
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentQuestionDetails : Fragment() {

    private var questionId = ""

    private lateinit var question: EntityQuestion

    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    private val viewModel by activityViewModels<ProfMainViewModel>()

    @Inject
    lateinit var userRepo: DBRepository

    @Inject
    lateinit var sharedP: SharedPreferences

    private var _binding: FragmentQuestDetailBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        binding.send.setOnClickListener {
            val reply = binding.message.text.toString()
            if (reply.isEmpty()) {
                Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT)
                    .show()
            } else {
                replySender(reply)
            }
        }

        questionId = arguments?.get("id") as String
        lifecycleScope.launch {
            showQuestion()
        }
    }

    private fun replySender(reply: String) {
        lifecycleScope.launch {
            binding.pb.visibility = View.VISIBLE
            binding.send.visibility = View.GONE
            val response = withContext(Dispatchers.IO) {
                val user = userRepo.getUser(userFolioNumber)
                sendReply(createReply(user, reply))
            }
            binding.pb.visibility = View.GONE
            binding.send.visibility = View.VISIBLE
            if (response.status == Status.SUCCESS.toString()) {
                showQuestion()
                binding.message.setText("")
            } else {
                AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                    .setTitle("Failed")
                    .setMessage("Sending failed: ${response.message}")
                    .show()
            }
        }
    }

    private fun sendReply(question: Question): ResponseString {
        return try {
            val responseString = endpoint.saveNewReply(question).execute()
            if (responseString.status == Status.SUCCESS.toString()) {
                viewModel.repository.dao.insert(getEntity(question))
            }
            responseString
        } catch (e: IOException) {
            ResponseString().setStatus("ERROR").setMessage(e.localizedMessage)
        }
    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.questions_refresh, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.refresh -> {
                    lifecycleScope.launch {
                        val dial =
                            MyAlertDialog(requireContext(), "", "Loading...", false).show()
                        viewModel.refreshDB()
                        question = viewModel.repository.getQuestion(questionId)
                        showQuestion()
                        dial.dismiss()
                    }
                    true
                }
                else -> true
            }
        }
    }

    private fun showPopupMenu(view: View, reply: Reply) {

        val popupMenu = PopupMenu(requireContext(), view)

        popupMenu.menu.add(0, 1, 0, "Edit")
        popupMenu.menu.add(1, 2, 1, "Delete")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showEditDial(reply.id)
                }
                2 -> {
                    deleteReply(reply)
                }
            }
            true
        }
        popupMenu.show()
    }


    private fun deleteReply(reply: Reply) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage("ARE YOU SURE YOU WANT TO DELETE THIS question")
            .setPositiveButton("YES") { dialog, id ->
                dialog.dismiss()
                delete(reply)
            }.setNegativeButton("NO") { dialog, id -> dialog.dismiss() }.show()
    }

    private fun delete(reply: Reply) {
        val replyList = getReplyList(question.replyList) ?: ArrayList()
        replyList.remove(reply)
        question.replyList = getStrReply(replyList)
        question.numReply = "${question.numReply.toInt() - 1}"

        lifecycleScope.launch {
            val dial =
                MyAlertDialog(requireContext(), "", "Deleting...", false).show()
            val response = withContext(Dispatchers.IO) {
                sendReply(getBackendObj(question))
            }
            dial.dismiss()
            if (response.status == Status.SUCCESS.toString()) {
                Toast.makeText(requireContext(), "DELETED", Toast.LENGTH_SHORT).show()
                question = viewModel.repository.getQuestion(questionId)
                showQuestion()
            } else {
                AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                    .setTitle("Failed")
                    .setMessage("Failed to delete: ${response.message}")
                    .show()
            }
        }
    }

    private fun showEditDial(id: String) {
        val replyList = getReplyList(question.replyList) ?: ArrayList()
        var replyIndex = 0
        for ((index, value) in replyList.withIndex()) {
            if (value.id == id) {
                replyIndex = index
                break
            }
        }

        Log.i("TAG", "Reply index = $replyIndex")

        val v: View = layoutInflater.inflate(R.layout.select_area_layout1, null)
        val alert = AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setView(v)
            .show()
        val replyEdit = v.findViewById<TextView>(R.id.message)
        replyEdit.text = replyList[replyIndex].reply
        val sendButt = v.findViewById<Button>(R.id.send)
        sendButt.setOnClickListener {
            val txt = replyEdit.text.toString()
            if (txt.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Reply input cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
            } else {

                lifecycleScope.launch {
                    val dial =
                        MyAlertDialog(requireContext(), "", "Sending...", false).show()
                    val response = withContext(Dispatchers.IO) {
                        replyList[replyIndex].reply = txt
                        question.replyList = getStrReply(replyList)
                        sendReply(getBackendObj(question))
                    }
                    dial.dismiss()
                    alert.dismiss()
                    if (response.status == Status.SUCCESS.toString()) {
                        Toast.makeText(requireContext(), "SENT", Toast.LENGTH_SHORT).show()
                        question = viewModel.repository.getQuestion(questionId)
                        showQuestion()
                    } else {
                        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                            .setTitle("Failed")
                            .setMessage("Sending failed: ${response.message}")
                            .show()
                    }
                }
            }
        }
    }

    private suspend fun showQuestion() {
        question = viewModel.repository.getQuestion(questionId)
        binding.from.text = question.from
        binding.date.text = question.time
        binding.question.text = question.question
        binding.upVote.text = question.upVote
        binding.downVote.text = question.downVote
        binding.area.text = question.area

        binding.question.let {
            if (it != null) {
                Linkify.addLinks(it, Linkify.ALL)
            }
        }

        binding.icon.let {
            GlideApp.with(requireContext())
                .load(question.imageUrl)
                .placeholder(R.drawable.listitem_image_holder)
                .apply(RequestOptions.circleCropTransform())
                .signature(ObjectKey(question.id))
                .into(it)
        }

        val replys = getReplyList(this.question.replyList)

        binding.questionViewItem.removeAllViews()

        if (!replys.isNullOrEmpty()) {
            for (reply in replys) {
                val v: View = layoutInflater.inflate(R.layout.replay_layout_item, null)
                val llParam = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                llParam.marginStart = 30
                llParam.topMargin = 5
                llParam.rightMargin = 5
                v.layoutParams = llParam

                v.from?.text = reply.from
                v.date?.text = reply.time
                v.question.text = reply.reply

                v.upVote.text = reply.upVote
                v.downVote.text = reply.downVote

                v.icon?.let {
                    GlideApp.with(requireContext())
                        .load(reply.imageUrl)
                        .placeholder(R.drawable.listitem_image_holder)
                        .apply(RequestOptions.circleCropTransform())
                        .signature(ObjectKey(reply.id))
                        .into(it)
                }

                if (question.folio == userFolioNumber) {
                    v.menu.visibility = View.VISIBLE
                    v.menu.setOnClickListener { showPopupMenu(it, reply) }
                }

                v.upVote.setOnClickListener { view: View? ->
                    upVote(reply, v.progressBar)
                }

                v.downVote.setOnClickListener { view: View? ->
                    downVote(reply, v.progressBar)
                }

                v.copy.setOnClickListener { view: View? ->
                    copy(reply.reply)
                }

                binding.questionViewItem.addView(v)
            }
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

    private data class VoteState(
        var id: String = "",
        var upVote: Boolean = false,
        var downVote: Boolean = false
    )


    private fun upVote(reply: Reply, p: ProgressBar) {
        var voteState = getVoteStatusObj(sharedP.getString(reply.id, ""))
        if (voteState != null && voteState.upVote) {
            Toast.makeText(requireContext(), "Already voted this item", Toast.LENGTH_SHORT).show()
            return
        } else {
            val replyList = getReplyList(question.replyList)
            val l = replyList?.remove(reply)
            reply.upVote = "${reply.upVote.toInt() + 1}"
            replyList?.add(reply)
            question.replyList = getStrReply(replyList)
            lifecycleScope.launch {
                p.visibility = View.VISIBLE
                val response = withContext(Dispatchers.IO) {
                    question.replyList = getStrReply(replyList)
                    sendReply(getBackendObj(question))
                }
                p.visibility = View.GONE
                if (response.status == Status.SUCCESS.toString()) {
                    if (voteState == null) {
                        voteState = VoteState(reply.id)
                    }
                    voteState!!.upVote = true
                    sharedP.edit().putString(reply.id, getVoteStatusObjToString(voteState)).apply()
                    viewModel.fetchQuestions()
                } else {
                    Toast.makeText(requireContext(), "Failed, try again", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downVote(reply: Reply, p: ProgressBar) {
        var voteState = getVoteStatusObj(sharedP.getString(reply.id, ""))
        if (voteState != null && voteState.downVote) {
            Toast.makeText(requireContext(), "Already voted", Toast.LENGTH_SHORT).show()
            return
        } else {
            val replyList = getReplyList(question.replyList)
            replyList?.remove(reply)
            reply.downVote = "${reply.downVote.toInt() + 1}"
            replyList?.add(reply)
            question.replyList = getStrReply(replyList)
            lifecycleScope.launch {
                p.visibility = View.VISIBLE
                val response = withContext(Dispatchers.IO) {
                    question.replyList = getStrReply(replyList)
                    sendReply(getBackendObj(question))
                }
                p.visibility = View.GONE
                if (response.status == Status.SUCCESS.toString()) {
                    if (voteState == null) {
                        voteState = VoteState(reply.id)
                    }
                    voteState!!.downVote = true
                    sharedP.edit().putString(reply.id, getVoteStatusObjToString(voteState)).apply()
                    viewModel.fetchQuestions()
                } else {
                    Toast.makeText(requireContext(), "Failed, try again", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }


    private fun getReplyList(reply: String?): MutableList<Reply>? {
        if (reply.isNullOrEmpty()) {
            return ArrayList()
        }
        val gson = Gson()
        val type = object : TypeToken<MutableList<Reply>>() {}.type
        return gson.fromJson(reply, type)
    }

    private fun getStrReply(payments: MutableList<Reply>?): String {
        if (payments == null) {
            return ""
        }
        val gson = Gson()
        val type = object : TypeToken<MutableList<Reply>>() {}.type
        return gson.toJson(payments, type)
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
        u.upVote = q.upVote ?: ""
        u.downVote = q.downVote ?: ""
        u.numReply = q.numReply ?: ""
        u.replyList = q.replyList ?: ""
        u.visibility = q.visibility ?: true
        return u
    }

    private fun createReply(user: EntityUserData?, msg: String): Question {
        val reply = Reply()
        val id = System.currentTimeMillis()
        reply.id = id.toString()
        reply.folio = userFolioNumber
        reply.questionId = question.id
        reply.time = fd.format(Date(id))
        reply.reply = msg
        reply.imageUrl = user?.imageUri ?: ""
        var name = "Anonymous"
        if (!sharedP.getBoolean(Const.Q_FROM_ANANYMOUS, false)) {
            name = sharedP.getString(SessionManager.USER_FULL_NAME, "").toString()
        }
        reply.from = name
        val replys = getReplyList(question.replyList)
        replys?.add(reply)
        val rp = getStrReply(replys)
        question.replyList = rp
        question.numReply = "${question.numReply.toInt() + 1}"
        return getBackendObj(question)
    }

    private fun getBackendObj(obj: EntityQuestion): Question {
        val u = Question()
        u.id = obj.id
        u.from = obj.from
        u.folio = obj.folio
        u.question = obj.question
        u.area = obj.area
        u.time = obj.time
        u.imageUrl = obj.imageUrl
        u.upVote = obj.upVote
        u.downVote = obj.downVote
        u.numReply = obj.numReply
        u.replyList = obj.replyList
        u.visibility = obj.visibility
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}






