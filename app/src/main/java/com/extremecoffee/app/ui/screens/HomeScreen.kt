@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.extremecoffee.app.ui.screens

import android.net.Uri
import android.widget.Toast
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.extremecoffee.app.R
import com.extremecoffee.app.ui.saveProfilePhoto
import com.extremecoffee.app.ui.makeAvatarBase64
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Phones
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.data.MyStats
import com.extremecoffee.app.ui.goFresh
import com.extremecoffee.app.ui.AppBottomBar
import com.extremecoffee.app.ui.decodeAvatar

private val HeroStart = Color(0xFFF3923F)
private val HeroEnd = Color(0xFFC85F1C)

@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val name = remember { Profile.name(context) }
    val nameFallback = stringResource(R.string.home_name_fallback)
    val myId = remember { Profile.id(context) }
    var photoPath by remember { mutableStateOf(Profile.photoPath(context)) }
    var photoVersion by remember { mutableStateOf(0) }
    val pickProfilePhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val saved = saveProfilePhoto(context, uri)
            if (saved != null) {
                Profile.setPhotoPath(context, saved)
                Profile.setPhoto64(context, makeAvatarBase64(saved))
                photoPath = saved; photoVersion++
                Toast.makeText(context, context.getString(R.string.account_toast_photo_ok), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.account_toast_photo_fail), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) { photoPath = Profile.photoPath(context); photoVersion++ }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val incoming by CoffeeRepository.incomingInvites(myId).collectAsState(initial = emptyList())
    val myActive by CoffeeRepository.myActiveEvent(myId).collectAsState(initial = null)
    val acceptedInvite by CoffeeRepository.myAcceptedActiveEvent(myId).collectAsState(initial = null)
    val myStats by produceState<MyStats?>(initialValue = null) { value = CoffeeRepository.loadMyStats(context) }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AppBottomBar(nav, "home") }
    ) { inner ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; delay(500); refreshing = false } },
            modifier = Modifier.fillMaxSize().padding(inner)
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 12.dp)
            ) {
                // ---- App bar ----
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_coffee_marker), contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Extreme Coffee", style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    val bmp = remember(photoPath, photoVersion) { if (photoPath != null) BitmapFactory.decodeFile(photoPath) else null }
                    Surface(onClick = { pickProfilePhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp)) {
                        if (bmp != null) {
                            Image(bmp.asImageBitmap(), contentDescription = stringResource(R.string.account_profile),
                                modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(name.trim().take(1).uppercase().ifBlank { "C" },
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                // ---- Hero ----
                val active = myActive
                val accepted = acceptedInvite
                val bigText = when {
                    active != null -> stringResource(R.string.home_title_inprogress)
                    accepted != null -> stringResource(R.string.home_title_awaited)
                    else -> stringResource(R.string.home_title_ready)
                }
                Box(
                    Modifier.fillMaxWidth()
                        .shadow(10.dp, MaterialTheme.shapes.extraLarge, clip = false)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Brush.linearGradient(listOf(HeroStart, HeroEnd)))
                ) {
                    Image(painterResource(R.drawable.ic_coffee_marker), contentDescription = null,
                        modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).size(80.dp))
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.home_greeting, name.trim().uppercase().ifBlank { nameFallback }),
                            style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFE6D2))
                        Spacer(Modifier.height(8.dp))
                        Text(bigText, style = MaterialTheme.typography.displaySmall, color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        when {
                            active != null -> {
                                Surface(onClick = { nav.goFresh("launched/${active.id}") },
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = Color.White, shadowElevation = 2.dp) {
                                    Text(stringResource(R.string.home_btn_viewmap),
                                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
                                        color = HeroEnd, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            accepted != null -> {
                                // Hai accettato un invito: lancio disabilitato finché non si conclude.
                                Surface(
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = Color.White.copy(alpha = 0.55f)
                                ) {
                                    Text(stringResource(R.string.home_btn_blocked),
                                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
                                        color = HeroEnd.copy(alpha = 0.75f),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1, softWrap = false)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.home_blocked_msg),
                                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFE6D2)
                                )
                            }
                            else -> {
                                Surface(onClick = { nav.goFresh("launch") },
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = Color.White, shadowElevation = 2.dp) {
                                    Text(stringResource(R.string.home_btn_launch),
                                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
                                        color = HeroEnd, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                // ---- Stat cards ----
                val s = myStats
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatCard(Modifier.weight(1f), stringResource(R.string.home_stat_streak), Icons.Filled.LocalFireDepartment,
                        (s?.streakWeeks ?: 0).toString(), stringResource(R.string.home_stat_weeks)) { nav.navigate("badges") }
                    StatCard(Modifier.weight(1f), stringResource(R.string.home_stat_coffees), Icons.Filled.Coffee,
                        (s?.total ?: 0).toString(), stringResource(R.string.home_stat_total)) { nav.navigate("badges") }
                }
                if (s?.atRisk == true) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.home_atrisk),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium)
                }

                // ---- Esplora (sopra la piega) ----
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.home_explore), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(10.dp))
                ExploreRow(Icons.Filled.TrackChanges, stringResource(R.string.home_radar_title), stringResource(R.string.home_radar_sub)) { nav.goFresh("radar") }
                Spacer(Modifier.height(10.dp))
                ExploreRow(Icons.Filled.GroupAdd, stringResource(R.string.home_invite_title), stringResource(R.string.home_invite_sub)) { nav.goFresh("inviteFriends") }
                Spacer(Modifier.height(10.dp))
                ExploreRow(Icons.Filled.Schedule, stringResource(R.string.home_recurring_title), stringResource(R.string.home_recurring_sub)) { nav.navigate("recurring") }

                // ---- Inviti in arrivo ----
                if (incoming.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    Text(stringResource(R.string.home_invites_for_you), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(10.dp))
                    incoming.forEach { e ->
                        Surface(onClick = { nav.goFresh("invite/${e.id}") }, shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                val inv = remember(e.launcherPhoto) { decodeAvatar(e.launcherPhoto) }
                                if (inv != null) {
                                    Image(inv.asImageBitmap(), contentDescription = e.launcherName,
                                        modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center) {
                                        Text("\u2615", fontSize = 20.sp)
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.home_invite_from, e.launcherName),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.home_invite_remaining, e.barName, (e.remainingMillis() / 60_000).toInt()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, icon: ImageVector, value: String, unit: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 3.dp, modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(unit, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 7.dp))
            }
        }
    }
}

@Composable
private fun ExploreRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
