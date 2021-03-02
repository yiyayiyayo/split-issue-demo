package cn.yiyayiyayo.aab.demo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.*
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.android.synthetic.main.activity_main.*

const val PROVIDER_CLASS = "cn.yiyayiyayo.aab.demo.feature.TextFeatureImpl\$Provider"
private const val CONFIRMATION_REQUEST_CODE = 1

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    /** Listener used to handle changes in state for install requests. */
    private val listener = SplitInstallStateUpdatedListener { state ->
        val names = state.moduleNames().joinToString(" - ")
        when (state.status()) {
            /*
             This may occur when attempting to download a sufficiently large module.

             In order to see this, the application has to be uploaded to the Play Store.
             Then features can be requested until the confirmation path is triggered.
          */
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> manager.startConfirmationDialogForResult(state, this, CONFIRMATION_REQUEST_CODE)
            //  In order to see this, the application has to be uploaded to the Play Store.
            SplitInstallSessionStatus.DOWNLOADING -> showLoading(state, getString(R.string.downloading, names))
            SplitInstallSessionStatus.INSTALLING -> showLoading(state, getString(R.string.installing, names))
            SplitInstallSessionStatus.INSTALLED -> onFeatureInstalled()
            SplitInstallSessionStatus.FAILED -> Toast.makeText(this, getString(R.string.error_for_module, state.errorCode(), state.moduleNames()), Toast.LENGTH_LONG).show()
        }
    }
    private lateinit var manager: SplitInstallManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        manager = SplitInstallManagerFactory.create(this)

        install_feature.setOnClickListener { installFeature() }
        feature_get_text.setOnClickListener { featureGetText() }
        split_compat_install.setOnClickListener { splitCompatInstall() }
        new_web_view.setOnClickListener { newWebView() }
        updateUI()
    }

    private fun updateUI() {
        if (manager.installedModules.contains("feature")) {
            install_feature.isEnabled = false
            install_feature.text = getString(R.string.already_installed)

            feature_get_text.isEnabled = true
        } else {
            install_feature.isEnabled = true
            install_feature.text = getString(R.string.install_feature)

            feature_get_text.isEnabled = false
        }
        printAssetPaths(applicationContext)
    }

    override fun onResume() {
        manager.registerListener(listener)
        super.onResume()
    }

    override fun onPause() {
        manager.unregisterListener(listener)
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CONFIRMATION_REQUEST_CODE -> if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, R.string.user_cancelled, Toast.LENGTH_LONG).show()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private var dialog: ProgressDialog? = null
    private fun showLoading(state: SplitInstallSessionState, message: String) {
        val dlg = dialog ?: ProgressDialog(this).apply {
            setOnDismissListener { dialog = null }
            dialog = this
        }
        dlg.max = state.totalBytesToDownload().toInt()
        dlg.progress = state.bytesDownloaded().toInt()
        dlg.setMessage(message)
        if (!dlg.isShowing) {
            dlg.show()
        }
    }

    private fun onFeatureInstalled() {
        dialog?.dismiss()
        updateUI()
    }

    private fun installFeature() {
        manager.startInstall(
            SplitInstallRequest.newBuilder()
                .addModule("feature")
                .build()
        )
    }

    private fun featureGetText() {
        val dependencies = object : TextFeature.Dependencies {
            override fun getContext(): Context = application
        }
        val provider = Class.forName(PROVIDER_CLASS).kotlin.objectInstance as TextFeature.Provider

        val feature = provider.get(dependencies)
        val text = try {
            feature.getText()
        } catch (e: Resources.NotFoundException) {
            e.stackTraceToString()
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.text_title))
            .setMessage(text)
            .show()
    }

    private fun splitCompatInstall() {
        SplitCompat.install(applicationContext)
        printAssetPaths(applicationContext)
    }

    private fun newWebView() {
        WebView(this)
        printAssetPaths(applicationContext)
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun printAssetPaths(context: Context) {
        kotlin.runCatching {
            val assetManager = context.assets
            val method = AssetManager::class.java.getDeclaredMethod("getApkAssets").apply {
                isAccessible = true
            }
            val mApkAssets = method.invoke(assetManager) as Array<*>
            var idx = 0
            asset_paths_label.text = getString(R.string.asset_paths_label, context.javaClass.simpleName, mApkAssets.size.toString())
            asset_paths.text = mApkAssets.joinToString("\n\n") {
                "${idx++}: ${it.toString().substringAfter("path=").substringBefore("}")}"
            }
        }.onFailure {
            asset_paths.text = getString(R.string.asset_paths_error, it.stackTraceToString())
        }
    }
}