package mx.mobile.solution.nabia04.ui.activities

import android.os.Build
import android.os.Bundle
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import mx.mobile.solution.nabia04.databinding.ActivityAboutNabia04Binding

class ActivityNabia04 : AppCompatActivity() {

    private lateinit var binding: ActivityAboutNabia04Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityAboutNabia04Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            binding.webView.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        }
        binding.webView.settings.builtInZoomControls = true

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            // Hide the zoom controls for HONEYCOMB+
            binding.webView.settings.displayZoomControls = false
        }

        binding.webView.loadUrl("file:///android_asset/about_nabia04.html")
    }

}