package cu.todus.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cu.todus.app.data.local.SessionManager
import cu.todus.app.data.remote.AuthRepository
import cu.todus.app.data.remote.ConnectionState
import cu.todus.app.data.remote.XmppManager
import cu.todus.app.ui.screens.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val authRepository = AuthRepository()
    val xmppManager = XmppManager()

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

    init {
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                if (loggedIn) {
                    _phone.value = sessionManager.phone.first()
                    _alias.value = sessionManager.alias.first()
                    connectXmpp(_phone.value, sessionManager.jwt.first())
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
            if (_activeChatJid.value == msg.chatJid) {
                _currentMessages.value = _currentMessages.value + Message(msg.id, msg.from, msg.body, msg.time)
            }
        }
        xmppManager.onTypingReceived = { sender ->
            viewModelScope.launch {
                if (_activeChatJid.value?.contains(sender) == true) {
                    _activeChatTyping.value = true; delay(3000); _activeChatTyping.value = false
                }
            }
        }
        xmppManager.onProfileReceived = { alias, photo, bio, todusId ->
            _alias.value = alias; _photoUrl.value = photo; _bio.value = bio; _todusId.value = todusId
        }
        xmppManager.onContactsReceived = { list ->
            _contacts.value = list.map { ContactItem("$it@im.todus.cu", it, "") }
        }
        xmppManager.onContactFound = { phone, alias ->
            _searchResult.value = ContactItem("$phone@im.todus.cu", alias, phone)
        }
    }

    fun login(phone: String, uuid: String) {
        viewModelScope.launch {
            _connectionState.value = "connecting"; _phone.value = phone
            val jwt = authRepository.getToken(phone, uuid)
            if (jwt != null) {
                sessionManager.saveSession(phone, uuid, jwt)
                _isLoggedIn.value = true
                connectXmpp(phone, jwt)
            } else _connectionState.value = "waiting"
        }
    }

    fun logout() {
        viewModelScope.launch {
            xmppManager.disconnect(); sessionManager.clearAll()
            _isLoggedIn.value = false; _chats.value = emptyList(); _contacts.value = emptyList(); _currentMessages.value = emptyList()
        }
    }

    fun openChat(jid: String) {
        _activeChatJid.value = jid; _activeChatTyping.value = false
        _activeChatName.value = jid.substringBefore("@")
    }

    fun closeChat() { _activeChatJid.value = null; _currentMessages.value = emptyList() }

    fun sendMessage(body: String, replyTo: String? = null) {
        _activeChatJid.value?.let { jid ->
            val mid = xmppManager.sendMessage(jid, body, replyTo)
            _currentMessages.value = _currentMessages.value + Message(mid, _phone.value, body, System.currentTimeMillis())
        }
    }

    fun sendTyping() { _activeChatJid.value?.let { xmppManager.sendTyping(it) } }

    fun editMessage(originalId: String, newBody: String) {
        _activeChatJid.value?.let { xmppManager.editMessage(it, originalId, newBody) }
        _currentMessages.value = _currentMessages.value.map { if (it.id == originalId) it.copy(body = newBody) else it }
    }

    fun deleteMessage(id: String, forEveryone: Boolean) {
        if (forEveryone) _activeChatJid.value?.let { xmppManager.deleteMessage(it, id) }
        _currentMessages.value = _currentMessages.value.filter { it.id != id }
    }

    fun searchByTodusId(todusId: String) { xmppManager.searchByTodusId(todusId) }
    fun clearSearch() { _searchResult.value = null; _searchNotFound.value = false }
}
