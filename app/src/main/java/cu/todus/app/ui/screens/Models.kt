package cu.todus.app.ui.screens

data class Message(
    val id: String,
    val from: String,
    val body: String,
    val time: Long,
    val status: String = "sent"
)

data class ChatItem(
    val jid: String = "",
    val alias: String = "",
    val lastMsg: String = "",
    val time: String = "",
    val unread: Int = 0,
    val typing: Boolean = false,
    val photoUrl: String = "",
    val status: String = ""
)

data class ContactItem(
    val jid: String = "",
    val alias: String = "",
    val info: String = "",
    val photoUrl: String = ""
)
