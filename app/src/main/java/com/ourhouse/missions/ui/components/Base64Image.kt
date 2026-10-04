package com.ourhouse.missions.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ourhouse.missions.ui.theme.AccentPrimary
import com.ourhouse.missions.util.ImageCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shows a photo stored as a base64 string (see [ImageCodec]); decodes off the main thread. */
@Composable
fun Base64Image(data: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, data) {
        value = withContext(Dispatchers.Default) { ImageCodec.decode(data)?.asImageBitmap() }
    }
    val image = bitmap
    if (image != null) {
        Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentPrimary)
        }
    }
}
