package cu.todus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.app.ui.theme.*

data class ContactItem(
    val jid: String = "",
    val alias: String = "",
    val info: String = "",
    val photoUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<ContactItem> = emptyList(),
    searchResult: ContactItem? = null,
    searchNotFound: Boolean = false,
    onBack: () -> Unit,
    onContactClick: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val avatarColors = listOf(
        Color(0xFFE056FD), Color(0xFF2ECC71), Color(0xFF3498DB),
        Color(0xFFE67E22), Color(0xFFE74C3C), Color(0xFF1ABC9C),
        Color(0xFF9B59B6), Color(0xFFF1C40F)
    )

    fun getAvatarColor(name: String): Color {
        if (name.isEmpty()) return avatarColors[0]
        val h = name.fold(0) { acc, c -> acc * 31 + c.code }
        return avatarColors[Math.abs(h) % avatarColors.size]
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(horizontal = 16.dp, vertical = 12.dp).height(60.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextWhite, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(8.dp))
            Text("Contactos", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it; onClearSearch() },
            modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp),
            placeholder = { Text("Buscar...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, cursorColor = Red),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (searchQuery.startsWith("@")) {
                    onSearch(searchQuery.drop(1))
                }
            })
        )

        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        if (searchResult != null) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onContactClick(searchResult.jid) }.padding(14.dp, 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = getAvatarColor(searchResult.alias)) {
                    Box(contentAlignment = Alignment.Center) { Text(searchResult.alias.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
                Column { Text(searchResult.alias, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(searchResult.info, color = TextMuted, fontSize = 13.sp) }
            }
        } else if (searchNotFound) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Usuario no encontrado", color = TextMuted, fontSize = 14.sp) }
        } else {
            val filtered = if (searchQuery.startsWith("@")) contacts.filter { it.alias.lowercase().contains(searchQuery.drop(1).lowercase()) }
            else contacts.filter { it.alias.lowercase().contains(searchQuery.lowercase()) }

            LazyColumn {
                items(filtered) { contact ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onContactClick(contact.jid) }.padding(14.dp, 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(50.dp), shape = CircleShape, color = getAvatarColor(contact.alias)) {
                            Box(contentAlignment = Alignment.Center) { Text(contact.alias.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                        Column { Text(contact.alias, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); if (contact.info.isNotEmpty()) Text(contact.info, color = TextMuted, fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}
