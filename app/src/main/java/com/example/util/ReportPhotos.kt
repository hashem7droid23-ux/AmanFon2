package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object ReportPhotos {
    const val MAX_PHOTOS = 3
    private const val MAX_BYTES = 160000
    private fun dir(ctx: Context) = File(ctx.filesDir, "report_photos").apply { mkdirs() }
    private fun key(imei: String) = imei.filter { it.isLetterOrDigit() }.ifBlank { "x" }
    private fun file(ctx: Context, imei: String, i: Int) = File(dir(ctx), "${key(imei)}_$i.jpg")
    private fun pending(ctx: Context, imei: String) = File(dir(ctx), "${key(imei)}.pending")
    private fun db(ctx: Context) = FirebaseFirestore.getInstance(ctx.getString(R.string.firestore_database_id))
    private fun orientation(ctx: Context, uri: Uri): Int = try {
        ctx.contentResolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
    fun decodeScaled(ctx: Context, uri: Uri, maxSide: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) null else {
            var sample = 1
            while (longest / (sample * 2) >= maxSide) sample *= 2
            val raw = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            raw?.let { bmp ->
                val matrix = Matrix(); val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
                if (scale < 1f) matrix.postScale(scale, scale)
                when (orientation(ctx, uri)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
                }
                if (matrix.isIdentity) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true).also { bmp.recycle() }
            }
        }
    } catch (_: Exception) { null }
    private fun compress(ctx: Context, uri: Uri): ByteArray? {
        var bmp = decodeScaled(ctx, uri, 1024) ?: return null
        try {
            while (true) {
                for (quality in listOf(80, 70, 60, 50, 40, 30)) {
                    val bytes = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
                    if (bytes.size <= MAX_BYTES) return bytes
                }
                if (maxOf(bmp.width, bmp.height) <= 128) return null
                val smaller = Bitmap.createScaledBitmap(bmp, (bmp.width / 2).coerceAtLeast(1), (bmp.height / 2).coerceAtLeast(1), true)
                bmp.recycle(); bmp = smaller
            }
        } finally { bmp.recycle() }
    }
    suspend fun saveLocal(ctx: Context, imei: String, uris: List<String>): List<ByteArray> = withContext(Dispatchers.IO) {
        val images = uris.take(MAX_PHOTOS).map { compress(ctx, Uri.parse(it)) ?: throw IllegalStateException("تعذر ضغط إحدى الصور") }
        if (images.isNotEmpty()) {
            pending(ctx, imei).writeText(FirebaseAuth.getInstance().currentUser?.uid.orEmpty())
            images.forEachIndexed { i, bytes -> file(ctx, imei, i).writeBytes(bytes) }
            (images.size until MAX_PHOTOS).forEach { file(ctx, imei, it).delete() }
        }
        images
    }
    suspend fun upload(ctx: Context, imei: String, images: List<ByteArray>, reportId: String = ""): Boolean = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false
        if (reportId.isBlank() || images.size !in 1..MAX_PHOTOS || images.any { it.isEmpty() || it.size > MAX_BYTES }) return@withContext false
        try {
            val report = db(ctx).collection("reports").document(reportId).get(Source.SERVER).await()
            if (report.getString("userId") != uid || report.getString("imei1") != key(imei)) return@withContext false
            db(ctx).collection("report_photos").document(key(imei)).set(mapOf(
                "userId" to uid, "reportId" to reportId, "imei" to key(imei),
                "photos" to images.map { Base64.encodeToString(it, Base64.NO_WRAP) }, "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            pending(ctx, imei).delete()
            true
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Log.w("ReportPhotos", "Upload failed", e); false }
    }
    suspend fun load(ctx: Context, imei: String): List<ByteArray> = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val marker = pending(ctx, imei)
        val pendingOwner = if (marker.exists()) marker.readText() else null
        val cached = if (pendingOwner != null && pendingOwner != uid) emptyList() else
            (0 until MAX_PHOTOS).map { file(ctx, imei, it) }.filter { it.exists() }.map { it.readBytes() }
        // Unconfirmed uploads stay visible to their owner instead of being erased by a missing cloud document.
        if (pendingOwner != null && pendingOwner == uid && cached.isNotEmpty()) return@withContext cached
        if (uid == null) return@withContext cached
        try {
            val snap = db(ctx).collection("report_photos").document(key(imei)).get(Source.SERVER).await()
            if (!snap.exists()) { if (!marker.exists()) deleteLocal(ctx, imei); return@withContext emptyList() }
            val images = (snap.get("photos") as? List<*>)?.take(MAX_PHOTOS)?.mapNotNull {
                val value = it as? String ?: return@mapNotNull null
                if (value.length > 213336) return@mapNotNull null
                runCatching { Base64.decode(value, Base64.NO_WRAP).takeIf { bytes -> bytes.size <= MAX_BYTES } }.getOrNull()
            } ?: emptyList()
            // Do not overwrite another signed-in user's unsent photos stored on this device.
            if (!marker.exists()) {
                deleteLocal(ctx, imei)
                images.forEachIndexed { i, bytes -> file(ctx, imei, i).writeBytes(bytes) }
            }
            images
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { cached }
    }
    fun deleteLocal(ctx: Context, imei: String) {
        (0 until MAX_PHOTOS).forEach { file(ctx, imei, it).delete() }
        pending(ctx, imei).delete()
    }
}
