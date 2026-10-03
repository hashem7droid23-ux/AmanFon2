package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.ui.theme.BrandBg
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.YemenGold
import com.example.util.ReportPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Picker for up to 3 phone photos (camera or gallery). Photos are kept as content URIs until submit. */
@Composable
fun PhotoPickerSection(
    photos: List<String>,
    onPhotosChange: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    val remaining = ReportPhotos.MAX_PHOTOS - photos.size

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCamera
        if (ok && uri != null) onPhotosChange((photos + uri).take(ReportPhotos.MAX_PHOTOS))
        pendingCamera = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ReportPhotos.MAX_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) onPhotosChange((photos + uris.map { it.toString() }).distinct().take(ReportPhotos.MAX_PHOTOS))
    }

    fun openCamera() {
        try {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(dir, "phone_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
            pendingCamera = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح الكاميرا", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(shape = RoundedCornerShape(20.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("صور الجهاز (${photos.size}/${ReportPhotos.MAX_PHOTOS})", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            Text(
                "صورة للجهاز من الأمام والخلف تساعد المحلات تتعرف عليه بسرعة. تقدر تصوّر الكرتون أو الفاتورة كمان.",
                color = TextSecondaryLight, fontSize = 11.5.sp, lineHeight = 17.sp
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                photos.forEachIndexed { i, p ->
                    UriThumb(p, onRemove = { onPhotosChange(photos.filterIndexed { j, _ -> j != i }) })
                }
                if (remaining > 0) {
                    AddTile(Icons.Default.PhotoCamera, "تصوير", "add_photo_camera") { openCamera() }
                    AddTile(Icons.Default.PhotoLibrary, "من المعرض", "add_photo_gallery") {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTile(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    Column(
        Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandBg)
            .clickable { onClick() }
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = YemenGold, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UriThumb(uri: String, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bmp by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            ReportPhotos.decodeScaled(context, Uri.parse(uri), 300)?.asImageBitmap()
        }
    }
    Box(Modifier.size(92.dp).clip(RoundedCornerShape(16.dp)).background(BrandBg)) {
        bmp?.let {
            Image(it, contentDescription = "صورة الجهاز", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "حذف", tint = PureWhite, modifier = Modifier.size(14.dp))
        }
    }
}

/** Photos of a report (loaded from the device or the cloud). Shows nothing when there are none. */
@Composable
fun ReportPhotosGallery(imei: String) {
    val context = LocalContext.current
    val images by produceState<List<ImageBitmap>?>(initialValue = null, imei) {
        value = ReportPhotos.load(context, imei).mapNotNull { bytes ->
            withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }
    }
    var open by remember { mutableStateOf<ImageBitmap?>(null) }
    val list = images
    if (list.isNullOrEmpty()) return

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("report_photos_gallery"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        list.forEach { img ->
            Image(
                img,
                contentDescription = "صورة الجهاز",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 150.dp, height = 190.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { open = img }
            )
        }
    }

    open?.let { img ->
        Dialog(onDismissRequest = { open = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { open = null }) {
                Image(img, contentDescription = "صورة الجهاز", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                IconButton(onClick = { open = null }, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = PureWhite)
                }
            }
        }
    }
}
