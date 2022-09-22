package mx.mobile.solution.nabia04.ui.welfare_fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentContributionsBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.treasurer.MyListAdapter
import mx.mobile.solution.nabia04.utilities.GlideApp
import mx.mobile.solution.nabia04.utilities.Response
import mx.mobile.solution.nabia04.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ContributionData
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

@AndroidEntryPoint
class FragmentUserContribution : BaseFragment<FragmentContributionsBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_contributions

    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    @Inject
    lateinit var endpoint: MainEndpoint

    @Inject
    lateinit var adapter: MyListAdapter

    private lateinit var imv: ImageView
    private lateinit var tv: TextView
    private lateinit var deadline: TextView
    private lateinit var type: TextView

    private lateinit var momoName: TextView
    private lateinit var momoNum: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())

        imv = vb?.icon!!
        tv = vb?.msg!!
        deadline = vb?.deadline!!
        type = vb?.type!!
        momoName = vb?.momoName!!
        momoNum = vb?.momoNum!!

        lifecycleScope.launch {
            vb?.progressBar?.visibility = View.VISIBLE
            val response = withContext(Dispatchers.IO) {
                getContributions()
            }
            if (response != null) {
                if (response.status == Status.SUCCESS) {
                    val contData = response.data
                    if (contData != null) {
                        vb!!.recyclerView.adapter = adapter
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
            vb?.progressBar?.visibility = View.GONE
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
}
