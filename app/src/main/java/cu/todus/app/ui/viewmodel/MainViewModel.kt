package cu.todus.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import cu.todus.app.data.local.*
import cu.todus.app.data.remote.AuthRepository
import cu.todus.app.data.remote.ConnectionState
import cu.todus.app.data.remote.XmppManager
import cu.todus.app.ui.screens.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val authRepository = AuthRepository()
    val xmppManager = XmppManager()

    private val db = Room.databaseBuilder(application, TodusDatabase::class.java, "todus.db").build()
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()
    private val contactDao = db.contactDao()

    private val _connectionState = MutableStateFlow("waiting")
    val connectionState: StateFlow<String> = _connectionState

    private val _alias = MutableStateFlow("")
    val alias: StateFlow<String> = _alias

    private val _todusId = MutableStateFlow("")
    val todusId: StateFlow<String> = _todusId

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio

    private val _photoUrl = MutableStateFlow("")
    val photoUrl: StateFlow<String> = _photoUrl

    private val _chats = MutableStateFlow<List<ChatItem>>(emptyList())
    val chats: StateFlow<List<ChatItem>> = _chats

    private val _currentMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentMessages: StateFlow<List<Message>> = _currentMessages

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts

    private val _activeChatJid = MutableStateFlow<String?>(null)
    val activeChatJid: StateFlow<String?> = _activeChatJid

    private val _activeChatName = MutableStateFlow("")
    val activeChatName: StateFlow<String> = _activeChatName

    private val _activeChatTyping = MutableStateFlow(false)
    val activeChatTyping: StateFlow<Boolean> = _activeChatTyping

    private val _searchResult = MutableStateFlow<ContactItem?>(null)
    val searchResult: StateFlow<ContactItem?> = _searchResult

    private val _searchNotFound = MutableStateFlow(false)
    val searchNotFound: StateFlow<Boolean> = _searchNotFound

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init { checkSession() }

    private fun checkSession() {
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                if (loggedIn) {
                    _phone.value = sessionManager.phone.first()
                    _alias.value = sessionManager.alias.first()
                    _todusId.value = sessionManager.todusId.first()
                    _photoUrl.value = sessionManager.photoUrl.first()
                    _bio.value = sessionManager.bio.first()
                    val jwt = sessionManager.jwt.first()
                    connectXmpp(_phone.value, jwt)
                    loadLocalData()
                }
            }
        }
    }

    private fun connectXmpp(phone: String, jwt: String) {
        viewModelScope.launch {
            xmppManager.connect(phone, jwt)
            xmppManager.state.collect { state ->
                _connectionState.value = when (state) {
                    ConnectionState.CONNECTED -> "connected"
                    ConnectionState.CONNECTING -> "connecting"
                    else -> "waiting"
                }
            }
        }
        xmppManager.onMessageReceived = { msg ->
            viewModelScope.launch {
                messageDao.insertMessage(MessageEntity(msg.id, msg.chatJid, msg.from, msg.body, msg.time))
                chatDao.insertChat(ChatEntity(msg.chatJid, getContactName(msg.from), "", msg.body, msg.time, if (_activeChatJid.value == msg.chatJid) 0 else 1, false))
                if (_activeChatJid.value == msg.chatJid) {
                    _currentMessages.value = _currentMessages.value + Message(msg.id, msg.from, msg.body, msg.time)
                }
                loadChats()
            }
        }
        xmppManager.onTypingReceived = { sender ->
            if (_activeChatJid.value?.contains(sender) == true) {
                _activeChatTyping.value = true
                kotlinx.coroutines.delay(3000)
                _activeChatTyping.value = false
            }
        }
        xmppManager.onProfileReceived = { alias, photo, bio, todusId ->
            viewModelScope.launch {
                _alias.value = alias; _photoUrl.value = photo; _bio.value = bio; _todusId.value = todusId
                sessionManager.updateProfile(alias, photo, bio, todusId)
            }
        }
        xmppManager.onContactsReceived = { list ->
            viewModelScope.launch {
                list.forEach { (phone, alias) -> contactDao.insertContact(ContactEntity("$phone@im.todus.cu", alias, "", "")) }
                loadContacts(); loadChats()
            }
        }
        xmppManager.onContactFound = { phone, alias -> _searchResult.value = ContactItem("$phone@im.todus.cu", alias, phone); _searchNotFound.value = false }
    }

    fun login(phone: String, uuid: String) {
        viewModelScope.launch {
            _connectionState.value = "connecting"
            _phone.value = phone
            val jwt = authRepository.getToken(phone, uuid)
            if (jwt != null) {
                sessionManager.saveSession(phone, uuid, jwt)
                _todusId.value = authRepository.getTodusIdFromJwt(jwt)
                _isLoggedIn.value = true
                connectXmpp(phone, jwt)
            } else _connectionState.value = "waiting"
        }
    }

    fun logout() {
        viewModelScope.launch {
            xmppManager.disconnect(); sessionManager.clearAll(); db.clearAllTables()
            _isLoggedIn.value = false; _chats.value = emptyList(); _contacts.value = emptyList(); _currentMessages.value = emptyList()
        }
    }

    private suspend fun loadLocalData() { loadChats(); loadContacts() }

    private suspend fun loadChats() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        _chats.value = chatDao.getAllChats().map { ChatItem(it.jid, it.alias, it.lastMsg, sdf.format(Date(it.lastTime)), it.unread, it.typing, it.photoUrl) }
    }

    private suspend fun loadContacts() {
        _contacts.value = contactDao.getAllContacts().map { ContactItem(it.jid, it.alias, it.bio, it.photoUrl) }
    }

    fun openChat(jid: String) {
        _activeChatJid.value = jid; _activeChatTyping.value = false
        viewModelScope.launch {
            chatDao.markRead(jid); loadChats()
            _currentMessages.value = messageDao.getMessages(jid).map { Message(it.id, it.from, it.body, it.time, it.status) }
            _activeChatName.value = contactDao.getAllContacts().find { it.jid == jid }?.alias ?: jid.substringBefore("@")
        }
    }

    fun closeChat() { _activeChatJid.value = null; _currentMessages.value = emptyList() }

    fun sendMessage(body: String, replyTo: String? = null) {
        val jid = _activeChatJid.value ?: return
        val mid = xmppManager.sendMessage(jid, body, replyTo)
        viewModelScope.launch {
            messageDao.insertMessage(MessageEntity(mid, jid, _phone.value, body, System.currentTimeMillis()))
            chatDao.insertChat(ChatEntity(jid, _activeChatName.value, "", body, System.currentTimeMillis(), 0, false))
            _currentMessages.value = _currentMessages.value + Message(mid, _phone.value, body, System.currentTimeMillis())
            loadChats()
        }
    }

    fun sendTyping() { _activeChatJid.value?.let { xmppManager.sendTyping(it) } }

    fun editMessage(originalId: String, newBody: String) {
        _activeChatJid.value?.let { xmppManager.editMessage(it, originalId, newBody) }
        viewModelScope.launch {
            messageDao.updateMessage(originalId, newBody)
            _currentMessages.value = _currentMessages.value.map { if (it.id == originalId) it.copy(body = newBody) else it }
        }
    }

    fun deleteMessage(id: String, forEveryone: Boolean) {
        if (forEveryone) _activeChatJid.value?.let { xmppManager.deleteMessage(it, id) }
        viewModelScope.launch {
            messageDao.deleteMessage(id)
            _currentMessages.value = _currentMessages.value.filter { it.id != id }
        }
    }

    fun searchByTodusId(todusId: String) { xmppManager.searchByTodusId(todusId) }
    fun clearSearch() { _searchResult.value = null; _searchNotFound.value = false }

    fun updateProfile(alias: String, bio: String) {
        viewModelScope.launch {
            _alias.value = alias; _bio.value = bio
            sessionManager.updateProfile(alias, _photoUrl.value, bio, _todusId.value)
            try {
                val jwt = sessionManager.jwt.first()
                val json = JSONObject().apply { put("alias", alias); put("description", bio); put("picture_url", _photoUrl.value); put("picture_thumbnail_url", "") }
                val req = Request.Builder().url("https://auth.todus.cu/v2/todus/users.me.json").post(json.toString().toRequestBody("application/json".toMediaType())).header("Authorization", jwt).build()
                OkHttpClient().newCall(req).execute()
            } catch (_: Exception) {}
        }
    }

    private fun getContactName(phone: String) = _contacts.value.find { it.jid.contains(phone) }?.alias ?: phone
}
