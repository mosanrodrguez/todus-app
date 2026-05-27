package cu.todus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.app.ui.theme.*

@Composable
fun HomeScreen(
    onChatClick: (String) -> Unit = {},
    onMyProfile: () -> Unit = {},
    onContacts: () -> Unit = {},
    connectionState: String = "waiting",
    alias: String = "",
    chats: List<ChatItem> = emptyList()
) {
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

    @Composable
    fun StatusIcon(status: String) {
        when (status) {
            "pending" -> Icon(Icons.Outlined.Schedule, null, tint = TextMuted, modifier = Modifier.size(14.dp))
            "sent" -> Icon(Icons.Outlined.Check, null, tint = TextMuted, modifier = Modifier.size(14.dp))
            "delivered" -> Icon(Icons.Outlined.DoneAll, null, tint = TextMuted, modifier = Modifier.size(14.dp))
            "seen" -> Icon(Icons.Outlined.DoneAll, null, tint = Red, modifier = Modifier.size(14.dp))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(horizontal = 16.dp, vertical = 12.dp).height(60.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (connectionState) { "connecting" -> "Conectando..."; "connected" -> "toDus"; else -> "Esperando red..." },
                color = when (connectionState) { "connecting" -> Red; "connected" -> TextWhite; else -> TextMuted },
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold
            )
            Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onMyProfile() }.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(alias.ifEmpty { "Yo" }, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = getAvatarColor(alias)) {
                    Box(contentAlignment = Alignment.Center) { Text(alias.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        Box(modifier = Modifier.weight(1f)) {
            if (chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(when (connectionState) { "connecting" -> "Conectando..."; "connected" -> "Sin conversaciones"; else -> "Esperando red..." }, color = TextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn {
                    items(chats) { chat ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onChatClick(chat.jid) }.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = getAvatarColor(chat.alias)) {
                                Box(contentAlignment = Alignment.Center) { Text(chat.alias.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(chat.alias, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(chat.time, color = TextMuted, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(3.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        if (chat.status.isNotEmpty()) { StatusIcon(chat.status); Spacer(Modifier.width(2.dp)) }
                                        Text(if (chat.typing) "Escribiendo..." else chat.lastMsg, color = if (chat.typing) Red else TextMuted, fontSize = 13.sp, fontStyle = if (chat.typing) FontStyle.Italic else FontStyle.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (chat.unread > 0) {
                                        Surface(shape = RoundedCornerShape(10.dp), color = Red) {
                                            Text(if (chat.unread > 99) "99+" else chat.unread.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(20.dp), color = Surface, shadowElevation = 0.dp) {
                Text("Nueva conversación", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
            Spacer(Modifier.width(10.dp))
            Surface(modifier = Modifier.size(48.dp).clickable { onContacts() }, shape = RoundedCornerShape(16.dp), color = Red, shadowElevation = 0.dp) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Chat, "Chat", tint = Color.White, modifier = Modifier.size(22.dp)) }
            }
        }
    }
}
