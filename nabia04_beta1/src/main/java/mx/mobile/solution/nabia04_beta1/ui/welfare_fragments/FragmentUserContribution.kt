package mx.mobile.solution.nabia04_beta1.ui.welfare_fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.databinding.FragmentContributionsBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.endpoint
import mx.mobile.solution.nabia04_beta1.ui.treasurer.MyListAdapter
import mx.mobile.solution.nabia04_beta1.utilities.GlideApp
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ContributionData
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

@AndroidEntryPoint
class FragmentUserContribution : Fragment() {

    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    @Inject
    lateinit var adapter: MyListAdapter

    private lateinit var imv: ImageView
    private lateinit var tv: TextView
    private lateinit var deadline: TextView
    private lateinit var type: TextView

    private lateinit var momoName: TextView
    private lateinit var momoNum: TextView

    private var _binding: FragmentContributionsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContributionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())

        imv = binding.icon!!
        tv = binding.msg!!
        deadline = binding.deadline!!
        type = binding.type!!
        momoName = binding.momoName!!
        momoNum = binding.momoNum!!

        lifecycleScope.launch {
            binding.progressBar?.visibility = View.VISIBLE
            val response = withContext(Dispatchers.IO) {
                getContributions()
            }
            if (response != null) {
                if (response.status == Status.SUCCESS) {
                    val contData = response.data
                    if (contData != null) {
                        binding.recyclerView.adapter = adapter
                        adapter.submitList(contData.contribution)
                        adapter.notifyDataSetChanged()
                        setImageDrawable(imv, contData.imageUri)
                        tv.text = contData.message
                        deadline.text = contData.deadline
                        type.text = contData.type
                        type.background = background()
                        momoNum.text = contData.momoNum
                        momoName.text = "MOMO NAME: ${contData.momoName}"
                        Linkify.addLinks(momoNum, Linkify.PHONE_NUMBERS)
                    }
                } else {
                    showDialog(
                        "ERROR",
                        "An error occurred while retrieving contribution list: ${response.message}"
                    )
                }

            }
            binding.progressBar?.visibility = View.GONE
        }
    }

    private fun background(): Drawable {
        return TextDrawable.Builder()
            .setColor(generator.randomColor)
            .setShape(TextDrawable.SHAPE_RECT)
            .setFontSize(50)
            .build()
    }

    private fun getContributions(): Response<ContributionData>? {
        return try {
            val response = endpoint.contributions.execute()
            Response.success(response.data)
        } catch (ex: IOException) {
            val erMsg = if (ex is SocketTimeoutException ||
                ex is SSLHandshakeException ||
                ex is UnknownHostException
            ) {
                "Cause: NO INTERNET CONNECTION"
            } else {
                ex.localizedMessage ?: ""
            }
            Response.error(erMsg, null)
        }
    }

    private fun setImageDrawable(img: ImageView, uri: String?) {
        if (uri != null) {
            GlideApp.with(requireContext())
                .load(uri)
                .signature(ObjectKey(uri))
                .apply(RequestOptions.circleCropTransform())
                .into(img)
        }
    }

    private fun showDialog(t: String, s: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(t)
            .setMessage(s)
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
