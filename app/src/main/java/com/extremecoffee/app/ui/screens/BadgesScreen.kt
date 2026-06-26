package com.extremecoffee.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.extremecoffee.app.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.data.Badge
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.MyStats
import com.extremecoffee.app.data.evaluateBadges
import com.extremecoffee.app.ui.CoffeeScaffold

@Composable
fun BadgesScreen(nav: NavController) {
    val context = LocalContext.current
    val stats by produceState<MyStats?>(initialValue = null) {
        value = CoffeeRepository.loadMyStats(context)
    }
    CoffeeScaffold(stringResource(R.string.bdg_title), nav, "badges") { mod ->
        Column(
            mod.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            val s = stats
            if (s == null) {
                Text(stringResource(R.string.bdg_calc), style = MaterialTheme.typography.bodyMedium)
            } else {
                val badges = evaluateBadges(s)
                val earned = badges.count { it.earned }
                Text(stringResource(R.string.bdg_count, earned, badges.size),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                badges.forEach { badge ->
                    BadgeRow(badge)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun BadgeRow(badge: Badge) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Text(badge.emoji,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.alpha(if (badge.earned) 1f else 0.3f))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(badge.titleRes),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.alpha(if (badge.earned) 1f else 0.6f))
                    if (badge.earned) {
                        Spacer(Modifier.width(6.dp))
                        Text("\u2713", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Text(stringResource(badge.descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!badge.earned) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { badge.progress.toFloat() / badge.target.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("${badge.progress}/${badge.target}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
