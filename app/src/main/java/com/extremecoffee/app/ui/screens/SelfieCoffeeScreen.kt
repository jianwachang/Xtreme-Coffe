@file:OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)

package com.extremecoffee.app.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.extremecoffee.app.R
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.ui.CoffeeScaffold
import com.extremecoffee.app.ui.SelfieShare
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@Composable
fun SelfieCoffeeScreen(nav: NavController, eventId: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val event by CoffeeRepository.eventFlow(eventId).collectAsState(initial = null)

    val camPermission = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(Unit) { if (!camPermission.status.isGranted) camPermission.launchPermissionRequest() }

    val imageCapture = remember { ImageCapture.Builder().build() }
    var captured by remember { mutableStateOf<Bitmap?>(null) }

    CoffeeScaffold("Selfie Coffee", nav, "selfie/$eventId") { mod ->
        Box(mod.fillMaxSize()) {
            val shot = captured
            when {
                // ---------- REVIEW: foto pronta + condivisione ----------
                shot != null -> {
                    Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = shot.asImageBitmap(),
                            contentDescription = "Selfie Coffee",
                            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val ok = SelfieShare.shareInstagramStory(context, shot)
                                if (!ok) {
                                    Toast.makeText(context,
                                        "Instagram non trovato: uso la condivisione di sistema",
                                        Toast.LENGTH_SHORT).show()
                                    SelfieShare.shareSystem(context, shot)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Storia Instagram", fontWeight = FontWeight.Bold) }

                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { SelfieShare.shareSystem(context, shot) },
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) { Text("Condividi") }
                            OutlinedButton(
                                onClick = {
                                    val ok = SelfieShare.saveToGallery(context, shot)
                                    Toast.makeText(context,
                                        if (ok) "Salvato in galleria" else "Salvataggio non riuscito",
                                        Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) { Text("Salva") }
                        }
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { captured = null }) { Text("Rifai lo scatto") }
                    }
                }

                // ---------- CAPTURE: anteprima fotocamera + cornice ----------
                camPermission.status.isGranted -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val pv = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                            val future = ProcessCameraProvider.getInstance(ctx)
                            future.addListener({
                                runCatching {
                                    val provider = future.get()
                                    val preview = Preview.Builder().build()
                                        .also { it.setSurfaceProvider(pv.surfaceProvider) }
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture
                                    )
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            pv
                        }
                    )

                    // anteprima della cornice (banda + logo + testo) in basso
                    Surface(
                        color = Color(0xCC141414),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(72.dp)
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(painterResource(R.drawable.ic_coffee_marker), null, Modifier.size(48.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("EXTREME COFFEE", color = Color.White, fontWeight = FontWeight.Black)
                                Text("\u2615 ${event?.barName ?: "Ci siamo!"}",
                                    color = Color(0xFFE8772E), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // pulsante scatto
                    Box(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val dir = File(context.cacheDir, "images").apply { mkdirs() }
                                val file = File(dir, "selfie_${System.currentTimeMillis()}.jpg")
                                val meta = ImageCapture.Metadata().apply { isReversedHorizontal = true }
                                val opts = ImageCapture.OutputFileOptions.Builder(file).setMetadata(meta).build()
                                imageCapture.takePicture(
                                    opts, ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(o: ImageCapture.OutputFileResults) {
                                            captured = SelfieShare.frameFromFile(context, file, event)
                                        }
                                        override fun onError(e: ImageCaptureException) {
                                            Toast.makeText(context, "Scatto non riuscito", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(64.dp)
                        ) { Text("\uD83D\uDCF8 Scatta il Selfie Coffee", fontWeight = FontWeight.Bold) }
                    }
                }

                // ---------- permesso negato ----------
                else -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Serve il permesso fotocamera per il Selfie Coffee.",
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { camPermission.launchPermissionRequest() }) {
                            Text("Consenti fotocamera")
                        }
                    }
                }
            }
        }
    }
}
