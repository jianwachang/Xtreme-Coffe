@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)

package com.extremecoffee.app.ui.screens

import android.Manifest
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.R
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.locationFlow
import com.extremecoffee.app.ui.CoffeeScaffold
import com.extremecoffee.app.ui.goFresh
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.extremecoffee.app.ui.decodeAvatar
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.first
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun RadarScreen(nav: NavController) {
    val context = LocalContext.current
    val perm = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val events by CoffeeRepository.openFriendshipEvents().collectAsState(initial = emptyList())

    var radiusKm by remember { mutableStateOf(5f) }
    var view by remember { mutableStateOf("locale") }
    var myLat by remember { mutableStateOf<Double?>(null) }
    var myLng by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(perm.status.isGranted) {
        if (perm.status.isGranted) {
            runCatching {
                val (la, lo) = locationFlow(context).first()
                myLat = la; myLng = lo
            }
        } else perm.launchPermissionRequest()
    }

    val centerLat = myLat ?: 44.4072
    val centerLng = myLng ?: 8.9340

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), zoomForRadius(radiusKm))
    }
    val cameraNaz = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(42.5, 12.5), 5f)  // Italia
    }
    LaunchedEffect(centerLat, centerLng, radiusKm) {
        runCatching {
            camera.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), zoomForRadius(radiusKm))
            )
        }
    }

    val sweep = rememberInfiniteTransition(label = "sweep").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)),
        label = "angle"
    )

    CoffeeScaffold(stringResource(R.string.rad_screen_title), nav, "radar") { mod ->
        Column(
            mod.fillMaxSize().background(Color(0xFF050B12)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.rad_title), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black, color = Color(0xFF00E5FF))
            Text(
                if (view == "locale") stringResource(R.string.rad_open_local, events.size, fmtKm(radiusKm))
                else stringResource(R.string.rad_open_italy, events.size),
                color = Color(0xFF7BD8E8)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = view == "locale", onClick = { view = "locale" },
                    label = { Text(stringResource(R.string.rad_local)) })
                FilterChip(selected = view == "nazionale", onClick = { view = "nazionale" },
                    label = { Text(stringResource(R.string.rad_national)) })
            }
            Spacer(Modifier.height(12.dp))
            if (view == "locale") {

            // Mappa Google circolare con il radar che scandaglia sopra
            BoxWithConstraints(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
            ) {
                val density = LocalDensity.current
                val sizePx = with(density) { maxWidth.toPx() }
                val rPx = sizePx / 2f
                val markerDp = 30.dp
                val halfPx = with(density) { markerDp.toPx() / 2f }

                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = camera,
                    properties = MapProperties(isMyLocationEnabled = perm.status.isGranted),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false, scrollGesturesEnabled = false,
                        zoomGesturesEnabled = false, rotationGesturesEnabled = false,
                        tiltGesturesEnabled = false, compassEnabled = false,
                        myLocationButtonEnabled = false, mapToolbarEnabled = false
                    )
                ) {}

                // Strato radar (semi-trasparente: la mappa resta visibile)
                Canvas(Modifier.matchParentSize()) {
                    val c = center
                    val r = size.minDimension / 2
                    for (i in 1..4) drawCircle(Color(0x3300E5FF), r * i / 4, c, style = Stroke(2f))
                    drawLine(Color(0x3300E5FF), Offset(c.x - r, c.y), Offset(c.x + r, c.y), 2f)
                    drawLine(Color(0x3300E5FF), Offset(c.x, c.y - r), Offset(c.x, c.y + r), 2f)
                    rotate(sweep.value, c) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0f to Color.Transparent,
                                0.10f to Color(0x4400E5FF),
                                0.12f to Color.Transparent, center = c
                            ),
                            startAngle = 0f, sweepAngle = 50f, useCenter = true,
                            topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2)
                        )
                        drawLine(Color(0xAA00E5FF), c, Offset(c.x + r, c.y), 3f, cap = StrokeCap.Round)
                    }
                    drawCircle(Color(0xFF00E5FF), 7f, c)
                }

                // Marker col LOGO: posizionati con le coordinate reali, pulsano al passaggio del raggio
                val proj = camera.projection
                val beam = sweep.value
                if (proj != null) {
                    events.forEach { e ->
                        val pt = runCatching { proj.toScreenLocation(LatLng(e.barLat, e.barLng)) }.getOrNull()
                        if (pt != null) {
                            val dx = pt.x - rPx
                            val dy = pt.y - rPx
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist <= rPx * 0.93f) {
                                var ang = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (ang < 0f) ang += 360f
                                var diff = abs(beam - ang) % 360f
                                if (diff > 180f) diff = 360f - diff
                                val window = 26f
                                val f = (1f - diff / window).coerceIn(0f, 1f)
                                val scale = 1f + 0.18f * f * f   // pulsazione leggera al passaggio
                                Box(
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .offset { IntOffset((pt.x - halfPx).toInt(), (pt.y - halfPx).toInt()) }
                                        .size(markerDp)
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .clickable { nav.goFresh("invite/${e.id}") }
                                ) {
                                    val avatar = remember(e.launcherPhoto) { decodeAvatar(e.launcherPhoto) }
                                    if (avatar != null) {
                                        Image(
                                            avatar.asImageBitmap(),
                                            contentDescription = e.launcherName,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                .border(2.dp, Color(0xFFF08730), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(R.drawable.ic_coffee_marker),
                                            contentDescription = e.launcherName,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.rad_radius, fmtKm(radiusKm)),
                color = Color.White, fontWeight = FontWeight.Bold)
            Slider(
                value = radiusKm, onValueChange = { radiusKm = it },
                valueRange = 0.1f..20f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF)
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.rad_km_min), color = Color(0xFF7BD8E8), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.rad_km_max), color = Color(0xFF7BD8E8), style = MaterialTheme.typography.bodySmall)
            }
            } else {
                // ---- VISTA NAZIONALE: mappa dell'Italia con tutti gli Extreme Coffee in amicizia ----
                Box(Modifier.fillMaxWidth().height(460.dp).clip(MaterialTheme.shapes.large)) {
                    GoogleMap(
                        modifier = Modifier.matchParentSize(),
                        cameraPositionState = cameraNaz,
                        properties = MapProperties(isMyLocationEnabled = perm.status.isGranted),
                        uiSettings = MapUiSettings(zoomControlsEnabled = true, mapToolbarEnabled = false)
                    ) {
                        events.forEach { e ->
                            MarkerComposable(
                                e.id, e.launcherPhoto,
                                state = rememberMarkerState(key = e.id, position = LatLng(e.barLat, e.barLng)),
                                title = e.launcherName,
                                snippet = stringResource(R.string.rad_open_item, e.barName, e.minutes),
                                onClick = { nav.goFresh("invite/${e.id}"); true }
                            ) {
                                val avatar = remember(e.launcherPhoto) { decodeAvatar(e.launcherPhoto) }
                                if (avatar != null) {
                                    Image(
                                        avatar.asImageBitmap(),
                                        contentDescription = e.launcherName,
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                            .border(2.dp, Color(0xFFF08730), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.ic_coffee_marker),
                                        contentDescription = e.launcherName,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.rad_tap),
                    color = Color(0xFF7BD8E8), style = MaterialTheme.typography.bodySmall)
            }

            if (events.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.rad_none),
                    color = Color(0xFF7BD8E8), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun fmtKm(r: Float): String =
    if (r < 1f) String.format(Locale.ITALY, "%.1f", r) else r.roundToInt().toString()

private fun zoomForRadius(radiusKm: Float): Float =
    (14.0 - ln(radiusKm.toDouble().coerceAtLeast(0.1)) / ln(2.0)).toFloat()
