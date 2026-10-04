package com.example.util

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference

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
    private var owner: WeakReference<ComponentActivity>? = null
    private var lastCheck = 0L
    private var shownCode = 0
    private val api = "https://api.github.com/repos/hashem7droid23-ux/AmanFon2/releases/tags/v1.0-apk"
    private val manifestUrl = "https://raw.githubusercontent.com/hashem7droid23-ux/AmanFon2/main/update.json"
    private data class Update(val code: Int, val name: String, val notes: String, val url: String)
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
        val release = json(api)
        if (release.optBoolean("draft") || release.optBoolean("prerelease")) return null
        val assets = release.getJSONArray("assets")
        val matching = (0 until assets.length()).map { assets.getJSONObject(it) }.any { asset ->
            asset.optString("state") == "uploaded" && UpdatePolicy.verifiedAsset(url, manifest.getString("apkSha256"),
                manifest.getLong("apkBytes"), asset.optString("browser_download_url"), asset.optString("digest"), asset.optLong("size"))
        }
        if (!matching) return null
        val name = manifest.getString("versionName").take(40)
        val notes = manifest.getString("releaseNotes").take(6000)
        if (name.isBlank() || notes.isBlank()) return null
        return Update(code, name, notes, url)
    }
    fun check(activity: ComponentActivity) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (checking || dialog?.isShowing == true || (lastCheck != 0L && now - lastCheck < 15 * 60 * 1000L)) return
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
                    .setMessage("الجديد في هذا الإصدار:\n\n${update.notes}\n\nحمّل الملف ثم افتحه ووافق على التثبيت. لا تحذف التطبيق الحالي؛ يجب أن يكون التحديث بنفس التوقيع.")
                    .setPositiveButton("تحميل التحديث") { _, _ ->
                        try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.url))) }
                        catch (_: Exception) { Toast.makeText(activity, "تعذر فتح رابط التحميل، تأكد من وجود متصفح", Toast.LENGTH_LONG).show() }
                    }.setNegativeButton("لاحقًا") { _, _ -> later() }.setOnCancelListener { later() }.create()
                dialog = created
                created.setOnDismissListener { if (dialog === created) { dialog = null; owner = null } }
                created.show()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Failed checks never block the app. */ }
            finally { checking = false }
        }
    }
    fun release(activity: ComponentActivity) {
        if (owner?.get() === activity) { dialog?.dismiss(); dialog = null; owner = null }
    }
}
