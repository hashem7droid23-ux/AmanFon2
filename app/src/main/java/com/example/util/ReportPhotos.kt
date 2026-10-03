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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Photos of the phone attached to a report (max 3).
 * Stored compressed on the device and in Firestore (report_photos/{imei}) so every user can see them,
 * without needing Firebase Storage.
 */
object ReportPhotos {
    private const val TAG = "ReportPhotos"
    const val MAX_PHOTOS = 3
    private const val MAX_SIDE = 1024
    private const val MAX_BYTES = 160_000

    private fun dir(ctx: Context) = File(ctx.filesDir, "report_photos").apply { mkdirs() }
    private fun key(imei: String) = imei.filter { it.isLetterOrDigit() }.ifBlank { "x" }
    private fun file(ctx: Context, imei: String, i: Int) = File(dir(ctx), "${key(imei)}_$i.jpg")

    private fun db(ctx: Context): FirebaseFirestore =
        FirebaseFirestore.getInstance(ctx.getString(R.string.firestore_database_id))

    private fun readOrientation(ctx: Context, uri: Uri): Int = try {
        ctx.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    /** Decodes a picked/captured image scaled down to [maxSide], with EXIF rotation applied. */
    fun decodeScaled(ctx: Context, uri: Uri, maxSide: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) null else {
            var sample = 1
            while (longest / (sample * 2) >= maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            raw?.let { bmp ->
                val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
                val m = Matrix()
                if (scale < 1f) m.postScale(scale, scale)
                when (readOrientation(ctx, uri)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
                }
                if (m.isIdentity) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "decode failed: ${e.message}")
        null
    }

    private fun compress(ctx: Context, uri: Uri): ByteArray? {
        val bmp = decodeScaled(ctx, uri, MAX_SIDE) ?: return null
        var quality = 80
        var out: ByteArray
        do {
            val bos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            out = bos.toByteArray()
            quality -= 10
        } while (out.size > MAX_BYTES && quality >= 30)
        return out
    }

    /** Compresses and saves the photos locally. Returns the compressed images (for upload). */
    suspend fun saveLocal(ctx: Context, imei: String, uris: List<String>): List<ByteArray> = withContext(Dispatchers.IO) {
        val images = uris.take(MAX_PHOTOS).mapNotNull { compress(ctx, Uri.parse(it)) }
        if (images.isNotEmpty()) {
            (0 until MAX_PHOTOS).forEach { file(ctx, imei, it).delete() }
            images.forEachIndexed { i, b -> file(ctx, imei, i).writeBytes(b) }
        }
        images
    }

    /** Publishes the photos so other users and shops see them. Needs a signed-in user. */
    suspend fun upload(ctx: Context, imei: String, images: List<ByteArray>): Boolean = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false
        if (images.isEmpty()) return@withContext false
        try {
            db(ctx).collection("report_photos").document(key(imei)).set(
                hashMapOf(
                    "userId" to uid,
                    "imei" to key(imei),
                    "photos" to images.map { Base64.encodeToString(it, Base64.NO_WRAP) },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "upload failed: ${e.message}")
            false
        }
    }

    /** Local copy first, otherwise downloads from Firestore and caches it. */
    suspend fun load(ctx: Context, imei: String): List<ByteArray> = withContext(Dispatchers.IO) {
        val local = (0 until MAX_PHOTOS).map { file(ctx, imei, it) }.filter { it.exists() }.map { it.readBytes() }
        if (local.isNotEmpty()) return@withContext local
        if (FirebaseAuth.getInstance().currentUser == null) return@withContext emptyList()
        try {
            val snap = db(ctx).collection("report_photos").document(key(imei)).get().await()
            val list = (snap.get("photos") as? List<*>)?.mapNotNull { s ->
                (s as? String)?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
            } ?: emptyList()
            list.take(MAX_PHOTOS).forEachIndexed { i, b -> file(ctx, imei, i).writeBytes(b) }
            list
        } catch (e: Exception) {
            Log.w(TAG, "load failed: ${e.message}")
            emptyList()
        }
    }

    fun deleteLocal(ctx: Context, imei: String) {
        (0 until MAX_PHOTOS).forEach { file(ctx, imei, it).delete() }
    }
}
