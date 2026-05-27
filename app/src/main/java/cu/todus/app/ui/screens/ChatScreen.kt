package cu.todus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    jid: String,
    messages: List<Message> = emptyList(),
    isTyping: Boolean = false,
    onBack: () -> Unit,
    onUserProfile: (String) -> Unit,
    onSendMessage: (String, String?) -> Unit,
    onSendTyping: () -> Unit,
    onEditMessage: (String, String) -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var lastTypingSent by remember { mutableLongStateOf(0L) }

    val contactName = jid.substringBefore("@")
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

    fun formatTime(ts: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(horizontal = 12.dp, vertical = 10.dp).height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextWhite, modifier = Modifier.size(20.dp))
            }
            Row(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onUserProfile(jid) }.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = getAvatarColor(contactName)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(contactName.first().uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text(contactName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (isTyping) "Escribiendo..." else "En línea",
                        color = if (isTyping) Red else Online,
                        fontSize = 12.sp,
                        fontStyle = if (isTyping) FontStyle.Italic else FontStyle.Normal
                    )
                }
            }
        }

        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(messages) { msg ->
                val isMine = msg.from == "me"

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 300.dp).clickable {
                            if (isMine) { selectedMessage = msg; showContextMenu = true }
                        },
                        shape = RoundedCornerShape(18.dp, 18.dp, if (isMine) 4.dp else 18.dp, if (isMine) 18.dp else 4.dp),
                        color = if (isMine) Surface else Red
                    ) {
                        Column(modifier = Modifier.padding(10.dp, 14.dp)) {
                            Text(msg.body, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                            Spacer(Modifier.height(3.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                Text(formatTime(msg.time), color = if (isMine) TextMuted else Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                if (isMine) {
                                    Spacer(Modifier.width(4.dp))
                                    when (msg.status) {
                                        "sent" -> Icon(Icons.Outlined.Check, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                        "delivered" -> Icon(Icons.Outlined.DoneAll, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                        "seen" -> Icon(Icons.Outlined.DoneAll, null, tint = CheckBlue, modifier = Modifier.size(12.dp))
                                        else -> Icon(Icons.Outlined.Schedule, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (replyTo != null) {
            Row(modifier = Modifier.fillMaxWidth().background(Surface).padding(8.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(36.dp).background(Red, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(contactName, color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(replyTo!!.body, color = TextMuted, fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = { replyTo = null }) {
                    Icon(Icons.Outlined.Close, "Cerrar", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (editingMessage != null) {
            Row(modifier = Modifier.fillMaxWidth().background(Surface).padding(8.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Editando mensaje", color = TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { editingMessage = null }) {
                    Icon(Icons.Outlined.Close, "Cerrar", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(8.dp, 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    val now = System.currentTimeMillis()
                    if (now - lastTypingSent > 3000) {
                        onSendTyping()
                        lastTypingSent = now
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensaje...", color = Color(0xFF5A5A5A)) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, cursorColor = Red),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            Surface(modifier = Modifier.size(42.dp).clickable {
                if (inputText.isNotBlank()) {
                    if (editingMessage != null) {
                        onEditMessage(editingMessage!!.id, inputText)
                        editingMessage = null
                    } else {
                        onSendMessage(inputText, replyTo?.id)
                        replyTo = null
                    }
                    inputText = ""
                }
            }, shape = CircleShape, color = Red) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Send, "Enviar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showContextMenu && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = null,
            text = {
                Column {
                    TextButton(onClick = { editingMessage = selectedMessage; inputText = selectedMessage!!.body; showContextMenu = false }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Edit, null, tint = TextWhite, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(10.dp)); Text("Editar", color = TextWhite, fontSize = 14.sp)
                    }
                    TextButton(onClick = { replyTo = selectedMessage; showContextMenu = false }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Reply, null, tint = TextWhite, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(10.dp)); Text("Responder", color = TextWhite, fontSize = 14.sp)
                    }
                    TextButton(onClick = { showContextMenu = false; showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Delete, null, tint = Red, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(10.dp)); Text("Eliminar", color = Red, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {},
            containerColor = Surface,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showDeleteDialog && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar mensaje", color = TextWhite) },
            text = {
                Column {
                    TextButton(onClick = { onDeleteMessage(selectedMessage!!.id, false); showDeleteDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Para mí", color = TextWhite, fontSize = 14.sp)
                    }
                    TextButton(onClick = { onDeleteMessage(selectedMessage!!.id, true); showDeleteDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Para todos", color = Red, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = TextMuted) } },
            containerColor = Surface,
            shape = RoundedCornerShape(14.dp)
        )
    }
}
