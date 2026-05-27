package cu.todus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    alias: String = "",
    todusId: String = "",
    phone: String = "",
    bio: String = "",
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    val avatarColors = listOf(Color(0xFFE056FD), Color(0xFF2ECC71), Color(0xFF3498DB), Color(0xFFE67E22), Color(0xFFE74C3C), Color(0xFF1ABC9C), Color(0xFF9B59B6), Color(0xFFF1C40F))
    fun getAvatarColor(name: String): Color { if (name.isEmpty()) return avatarColors[0]; val h = name.fold(0) { acc, c -> acc * 31 + c.code }; return avatarColors[Math.abs(h) % avatarColors.size] }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(horizontal = 16.dp, vertical = 12.dp).height(60.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextWhite, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(8.dp)); Text("Mi perfil", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onEdit) { Text("Editar", color = Red, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(50.dp))
            Surface(modifier = Modifier.size(130.dp), shape = CircleShape, color = getAvatarColor(alias)) {
                Box(contentAlignment = Alignment.Center) { Text(alias.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(16.dp))
            Text(alias.ifEmpty { "Usuario" }, color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (todusId.isNotEmpty()) { Spacer(Modifier.height(4.dp)); Text("@$todusId", color = TextSecondary, fontSize = 15.sp) }
            if (phone.isNotEmpty()) { Spacer(Modifier.height(4.dp)); Text("+$phone", color = TextSecondary, fontSize = 15.sp) }
            Spacer(Modifier.height(16.dp))
            Text(bio.ifEmpty { "Sin descripción" }, color = TextMuted, fontSize = 14.sp, modifier = Modifier.widthIn(max = 300.dp))
            Spacer(Modifier.weight(1f))

            Button(onClick = { showLogoutDialog = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Red), shape = RoundedCornerShape(14.dp)) {
                Text("Cerrar sesión", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(onDismissRequest = { showLogoutDialog = false }, title = { Text("¿Cerrar sesión?", color = TextWhite) }, text = { Text("Se borrarán todos los datos locales.", color = TextMuted) },
            confirmButton = { TextButton(onClick = { showLogoutDialog = false; onLogout() }) { Text("Cerrar", color = Red) } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar", color = TextMuted) } },
            containerColor = Surface, shape = RoundedCornerShape(14.dp))
    }
}
