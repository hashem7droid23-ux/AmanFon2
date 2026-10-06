package com.example.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object UpdatePolicy {
    private const val ROOT = "/hashem7droid23-ux/AmanFon2/releases/download/"
    fun trustedDownload(url: String): Boolean = try {
        val uri = URI(url)
        uri.scheme == "https" && uri.host == "github.com" && uri.port == -1 &&
            uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            uri.rawPath == uri.path && uri.path.startsWith(ROOT) &&
            uri.path.removePrefix(ROOT).split('/').let { parts ->
                parts.size == 2 && parts.all { it.isNotBlank() && it != "." && it != ".." } && parts[1].endsWith(".apk")
            }
    } catch (_: Exception) { false }
    fun compatible(localCode: Int, remoteCode: Int, localChannel: String, remoteChannel: String,
        localSigner: String, remoteSigner: String, packageName: String): Boolean =
        remoteCode > localCode && localChannel == remoteChannel && packageName == "com.aistudio.lostphone.ymndx" &&
        localSigner.matches(Regex("[a-fA-F0-9]{64}")) && localSigner.equals(remoteSigner, true)
    fun verifiedAsset(url: String, sha256: String, bytes: Long, assetUrl: String, digest: String, assetBytes: Long): Boolean =
        trustedDownload(url) && sha256.matches(Regex("[a-fA-F0-9]{64}")) && bytes > 0 &&
            assetUrl == url && assetBytes == bytes && digest.equals("sha256:$sha256", true)
}

/** Foreground checks; GitHub receives no account or report data. */
object AppUpdater {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
    private var checking = false
    private var dialog: AlertDialog? = null
    private var progressDialog: AlertDialog? = null
    private var owner: WeakReference<ComponentActivity>? = null
    private var lastCheck = 0L
    private var shownCode = 0
    private val api = "https://api.github.com/repos/hashem7droid23-ux/AmanFon2/releases/tags/v1.0-apk"
    private val manifestUrl = "https://raw.githubusercontent.com/hashem7droid23-ux/AmanFon2/main/update.json"
    private data class Update(val code: Int, val name: String, val notes: String, val url: String,
        val sha256: String, val bytes: Long)
    private var downloadId: Long = -1L
    private var downloadReceiver: BroadcastReceiver? = null
    private var progressHandler: Handler? = null

    private fun json(url: String): JSONObject {
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .header("User-Agent", "AmanPhone-UpdateCheck").header("Cache-Control", "no-cache").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful)
            val body = response.body ?: error("Missing body")
            check(body.contentLength() <= 262144)
            val output = ByteArrayOutputStream()
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= 262144)
                    output.write(buffer, 0, count)
                }
            }
            return JSONObject(output.toString("UTF-8"))
        }
    }
    @Suppress("DEPRECATION")
    private fun signer(activity: ComponentActivity): String {
        val certificates = if (Build.VERSION.SDK_INT >= 28) {
            activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners
        } else activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_SIGNATURES).signatures
        val certificate = certificates?.singleOrNull() ?: return ""
        return MessageDigest.getInstance("SHA-256").digest(certificate.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    private fun fetch(activity: ComponentActivity): Update? {
        val root = json(manifestUrl)
        if (root.optInt("schemaVersion") != 1) return null
        val channel = if (BuildConfig.DEBUG) "debug" else "production"
        val manifest = root.optJSONObject(channel) ?: return null
        if (!manifest.optBoolean("enabled", false)) return null
        val code = manifest.getInt("versionCode")
        if (!UpdatePolicy.compatible(BuildConfig.VERSION_CODE, code, channel, manifest.getString("channel"),
                signer(activity), manifest.getString("signerSha256"), manifest.getString("packageName"))) return null
        val url = manifest.getString("downloadUrl")
        if (!UpdatePolicy.trustedDownload(url)) return null
        val sha256 = manifest.getString("apkSha256")
        val bytes = manifest.getLong("apkBytes")
        val release = json(api)
        if (release.optBoolean("draft") || release.optBoolean("prerelease")) return null
        val assets = release.getJSONArray("assets")
        val matching = (0 until assets.length()).map { assets.getJSONObject(it) }.any { asset ->
            asset.optString("state") == "uploaded" && UpdatePolicy.verifiedAsset(url, sha256,
                bytes, asset.optString("browser_download_url"), asset.optString("digest"), asset.optLong("size"))
        }
        if (!matching) return null
        val name = manifest.getString("versionName").take(40)
        val notes = manifest.getString("releaseNotes").take(6000)
        if (name.isBlank() || notes.isBlank()) return null
        return Update(code, name, notes, url, sha256, bytes)
    }

    fun check(activity: ComponentActivity) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (checking || dialog?.isShowing == true || progressDialog?.isShowing == true ||
            (lastCheck != 0L && now - lastCheck < 15 * 60 * 1000L)) return
        checking = true
        activity.lifecycleScope.launch {
            try {
                val update = withContext(Dispatchers.IO) { fetch(activity) }
                lastCheck = android.os.SystemClock.elapsedRealtime()
                if (update == null || shownCode == update.code || activity.isFinishing || activity.isDestroyed ||
                    !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
                val preferences = activity.getSharedPreferences("app_update", 0)
                if (System.currentTimeMillis() < preferences.getLong("later_${update.code}", 0)) return@launch
                shownCode = update.code
                fun later() { preferences.edit().putLong("later_${update.code}", System.currentTimeMillis() + 24 * 60 * 60 * 1000L).apply() }
                owner = WeakReference(activity)
                val created = AlertDialog.Builder(activity).setTitle("إصدار جديد من أمان فون: ${update.name}")
                    .setMessage("الجديد في هذا الإصدار:\n\n${update.notes}\n\nسيتم تحميل التحديث داخل التطبيق، ثم يطلب منك الموافقة على التثبيت. لا تحذف التطبيق الحالي.")
                    .setPositiveButton("تحميل وتثبيت") { _, _ -> startInAppDownload(activity, update) }
                    .setNegativeButton("لاحقًا") { _, _ -> later() }
                    .setOnCancelListener { later() }.create()
                dialog = created
                created.setOnDismissListener { if (dialog === created) { dialog = null; owner = null } }
                created.show()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Failed checks never block the app. */ }
            finally { checking = false }
        }
    }

    /** Downloads the APK inside the app, verifies it, then opens the installer. */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun startInAppDownload(activity: ComponentActivity, update: Update) {
        val context = activity.applicationContext
        try {
            cancelDownload(context)
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "AmanPhone-${update.code}.apk")
            if (file.exists()) file.delete()

            val request = DownloadManager.Request(Uri.parse(update.url)).apply {
                setTitle("أمان فون ${update.name}")
                setDescription("جاري تحميل التحديث داخل التطبيق...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "updates/AmanPhone-${update.code}.apk")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val dm = context.getSystemService(DownloadManager::class.java) ?: error("No DownloadManager")
            downloadId = dm.enqueue(request)

            // Progress dialog
            val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100; isIndeterminate = false
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val label = TextView(activity).apply { text = "جاري تحميل التحديث... 0%" }
            val layout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 32, 48, 16)
                addView(label); addView(bar)
            }
            val pd = AlertDialog.Builder(activity).setTitle("تحميل التحديث ${update.name}")
                .setView(layout).setCancelable(false)
                .setNegativeButton("إلغاء") { _, _ -> cancelDownload(context); dismissProgress() }.create()
            progressDialog = pd
            pd.show()

            val handler = Handler(Looper.getMainLooper())
            progressHandler = handler
            val poll = object : Runnable {
                override fun run() {
                    if (progressDialog == null) return
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    dm.query(query).use { cursor ->
                        if (cursor.moveToFirst()) {
                            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                bar.progress = pct
                                label.text = "جاري تحميل التحديث... $pct%"
                            }
                        }
                    }
                    handler.postDelayed(this, 500)
                }
            }
            handler.post(poll)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != downloadId) return
                    dismissProgress()
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    dm.query(query).use { cursor ->
                        if (!cursor.moveToFirst()) { downloadFailed(activity, update); return }
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status != DownloadManager.STATUS_SUCCESSFUL) { downloadFailed(activity, update); return }
                    }
                    // Verify integrity before installing
                    if (file.length() != update.bytes || !sha256Of(file).equals(update.sha256, true)) {
                        file.delete()
                        Toast.makeText(activity, "فشل التحقق من ملف التحديث، سيتم فتح التحميل في المتصفح", Toast.LENGTH_LONG).show()
                        openInBrowser(activity, update.url)
                        return
                    }
                    installApk(activity, file)
                }
            }
            downloadReceiver = receiver
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
        } catch (_: Exception) {
            openInBrowser(activity, update.url)
        }
    }

    private fun downloadFailed(activity: ComponentActivity, update: Update) {
        cancelDownload(activity.applicationContext)
        Toast.makeText(activity, "تعذر تحميل التحديث داخل التطبيق، سيتم فتح المتصفح", Toast.LENGTH_LONG).show()
        openInBrowser(activity, update.url)
    }

    private fun openInBrowser(activity: ComponentActivity, url: String) {
        try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(activity, "تعذر فتح رابط التحميل", Toast.LENGTH_LONG).show() }
    }

    private fun installApk(activity: ComponentActivity, file: File) {
        try {
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.apkupdate", file)
            val install = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(install)
        } catch (_: Exception) {
            Toast.makeText(activity, "تعذر بدء التثبيت، افتح ملف APK من مجلد التحميل", Toast.LENGTH_LONG).show()
        }
    }

    private fun sha256Of(file: File): String = try {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) { "" }

    private fun dismissProgress() {
        progressHandler?.removeCallbacksAndMessages(null)
        progressHandler = null
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun cancelDownload(context: Context) {
        try {
            if (downloadId != -1L) {
                context.getSystemService(DownloadManager::class.java)?.remove(downloadId)
                downloadId = -1L
            }
        } catch (_: Exception) { }
        try {
            downloadReceiver?.let { context.unregisterReceiver(it) }
        } catch (_: Exception) { }
        downloadReceiver = null
    }

    fun release(activity: ComponentActivity) {
        if (owner?.get() === activity) {
            dialog?.dismiss(); dialog = null
            dismissProgress()
            cancelDownload(activity.applicationContext)
            owner = null
        }
    }
}
