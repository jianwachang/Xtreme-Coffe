package com.extremecoffee.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.extremecoffee.app.Config
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.ui.CoffeeScaffold

@Composable
fun InviteFriendsScreen(nav: NavController) {
    val context = LocalContext.current
    val downloadUrl by CoffeeRepository.downloadUrl().collectAsState()

    CoffeeScaffold("Invita amici", nav, "inviteFriends") { mod ->
        Column(
            mod.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("\uD83D\uDCAC", fontSize = 56.sp)
            Text("Invita i tuoi amici!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Si aprirà WhatsApp con il messaggio pronto: scegli tu a chi mandarlo.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            putExtra(Intent.EXTRA_TEXT, Config.inviteMessage(Profile.name(context), downloadUrl))
                        }
                        context.startActivity(intent)
                    } catch (ex: Exception) {
                        Toast.makeText(context, "WhatsApp non è installato", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) { Text("Apri WhatsApp e invita", fontSize = 16.sp) }
        }
    }
}
