package cu.todus.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
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
fun EditProfileScreen(
    alias: String = "",
    bio: String = "",
    onBack: () -> Unit,
    onSaved: (String, String) -> Unit
) {
    var editAlias by remember { mutableStateOf(alias) }
    var editBio by remember { mutableStateOf(bio) }

    val avatarColors = listOf(Color(0xFFE056FD), Color(0xFF2ECC71), Color(0xFF3498DB), Color(0xFFE67E22), Color(0xFFE74C3C), Color(0xFF1ABC9C), Color(0xFF9B59B6), Color(0xFFF1C40F))
    fun getAvatarColor(name: String): Color { if (name.isEmpty()) return avatarColors[0]; val h = name.fold(0) { acc, c -> acc * 31 + c.code }; return avatarColors[Math.abs(h) % avatarColors.size] }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(horizontal = 16.dp, vertical = 12.dp).height(60.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextWhite, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(8.dp)); Text("Editar perfil", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { onSaved(editAlias, editBio) }) { Text("Guardar", color = Red, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(40.dp))
            Box(modifier = Modifier.size(120.dp)) {
                Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = getAvatarColor(editAlias)) {
                    Box(contentAlignment = Alignment.Center) { Text(editAlias.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold) }
                }
                Surface(modifier = Modifier.size(32.dp).align(Alignment.BottomEnd).clickable { imagePicker.launch("image/*") }, shape = CircleShape, color = Red) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CameraAlt, "Cambiar foto", tint = Color.White, modifier = Modifier.size(14.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Cambiar foto de perfil", color = Red, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { imagePicker.launch("image/*") })
            Spacer(Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp)) {
                Text("NOMBRE", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                OutlinedTextField(value = editAlias, onValueChange = { editAlias = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Tu nombre", color = Color(0xFF5A5A5A)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, cursorColor = Red),
                    shape = RoundedCornerShape(14.dp), singleLine = true)
            }
            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp)) {
                Text("DESCRIPCIÓN", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                OutlinedTextField(value = editBio, onValueChange = { editBio = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Añade una descripción...", color = Color(0xFF5A5A5A)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, cursorColor = Red),
                    shape = RoundedCornerShape(14.dp), minLines = 2)
            }
        }
    }
}
