package cu.todus.app.ui.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cu.todus.app.data.local.*
import cu.todus.app.data.remote.AuthRepository
import cu.todus.app.data.remote.ConnectionState
import cu.todus.app.data.remote.XmppManager
import cu.todus.app.ui.screens.ChatItem
import cu.todus.app.ui.screens.ContactItem
import cu.todus.app.ui.screens.Message
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val authRepository = AuthRepository()
    private val xmppManager = XmppManager()

    private val db = Room.databaseBuilder(application, TodusDatabase::class.java, "todus.db").build()
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()
    private val contactDao = db.contactDao()

    // Estado de conexión
    private val _connectionState = MutableStateFlow("waiting")
    val connectionState: StateFlow<String> = _connectionState

    // Perfil propio
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

    // Chats
    private val _chats = MutableStateFlow<List<ChatItem>>(emptyList())
    val chats: StateFlow<List<ChatItem>> = _chats

    // Mensajes del chat activo
    private val _currentMessages = MutableStateFlow<List<Message>>(emptyList())
    val currentMessages: StateFlow<List<Message>> = _currentMessages

    // Contactos
    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts

    // Chat activo
    private val _activeChatJid = MutableStateFlow<String?>(null)
    val activeChatJid: StateFlow<String?> = _activeChatJid

    private val _activeChatName = MutableStateFlow("")
    val activeChatName: StateFlow<String> = _activeChatName

    private val _activeChatTyping = MutableStateFlow(false)
    val activeChatTyping: StateFlow<Boolean> = _activeChatTyping

    // Búsqueda
    private val _searchResult = MutableStateFlow<ContactItem?>(null)
    val searchResult: StateFlow<ContactItem?> = _searchResult

    private val _searchNotFound = MutableStateFlow(false)
    val searchNotFound: StateFlow<Boolean> = _searchNotFound

    // Sesión iniciada
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        checkSession()
        setupXmppCallbacks()
    }

    private fun checkSession() {
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                if (loggedIn) {
                    val phone = sessionManager.getPhoneSync()
                    val uuid = sessionManager.getUuidSync()
                    val jwt = sessionManager.getJwtSync()
                    _phone.value = phone
                    _alias.value = sessionManager.alias.first()
                    _todusId.value = sessionManager.todusId.first()
                    _photoUrl.value = sessionManager.photoUrl.first()
                    _bio.value = sessionManager.bio.first()
                    connectXmpp(phone, jwt)
                    loadLocalData()
                }
            }
        }
    }

    private fun setupXmppCallbacks() {
        xmppManager.onMessageReceived = { msg ->
            viewModelScope.launch {
                val chatJid = msg.chatJid
                val sender = msg.from
                val body = msg.body

                // Guardar mensaje en Room
                val messageEntity = MessageEntity(
                    id = msg.id,
                    chatJid = chatJid,
                    from = sender,
                    body = body,
                    time = msg.time,
                    status = "delivered"
                )
                messageDao.insertMessage(messageEntity)

                // Actualizar chat
                val contactName = getContactName(sender)
                val chatEntity = ChatEntity(
                    jid = chatJid,
                    alias = contactName,
                    photoUrl = "",
                    lastMsg = body,
                    lastTime = msg.time,
                    unread = if (_activeChatJid.value == chatJid) 0 else 1,
                    typing = false
                )
                chatDao.insertChat(chatEntity)

                // Si es el chat activo, actualizar mensajes
                if (_activeChatJid.value == chatJid) {
                    val current = _currentMessages.value.toMutableList()
                    current.add(Message(id = msg.id, from = sender, body = body, time = msg.time, status = "delivered"))
                    _currentMessages.value = current
                }

                loadChats()
            }
        }

        xmppManager.onTypingReceived = { sender ->
            if (_activeChatJid.value?.contains(sender) == true) {
                _activeChatTyping.value = true
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _activeChatTyping.value = false
                }
            }
        }

        xmppManager.onProfileReceived = { alias, photoUrl, bio, todusId ->
            viewModelScope.launch {
                _alias.value = alias
                _photoUrl.value = photoUrl
                _bio.value = bio
                _todusId.value = todusId
                sessionManager.updateProfile(alias, photoUrl, bio, todusId)
            }
        }

        xmppManager.onContactsReceived = { contactList ->
            viewModelScope.launch {
                contactList.forEach { (phone, alias) ->
                    val jid = "$phone@im.todus.cu"
                    contactDao.insertContact(ContactEntity(jid = jid, alias = alias, photoUrl = "", bio = ""))
                }
                loadContacts()
                loadChats()
            }
        }

        xmppManager.onContactFound = { phone, alias ->
            _searchResult.value = ContactItem(
                jid = "$phone@im.todus.cu",
                alias = alias,
                info = phone
            )
            _searchNotFound.value = false
        }
    }

    fun login(phone: String, uuid: String) {
        viewModelScope.launch {
            _connectionState.value = "connecting"
            _phone.value = phone

            val jwt = authRepository.getToken(phone, uuid)
            if (jwt != null) {
                val todusId = authRepository.getTodusIdFromJwt(jwt)
                sessionManager.saveSession(phone, uuid, jwt)
                _todusId.value = todusId
                _isLoggedIn.value = true
                connectXmpp(phone, jwt)
            } else {
                _connectionState.value = "waiting"
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
                    ConnectionState.WAITING_NETWORK -> "waiting"
                    ConnectionState.DISCONNECTED -> "waiting"
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            xmppManager.disconnect()
            sessionManager.clearAll()
            db.clearAllTables()
            _isLoggedIn.value = false
            _chats.value = emptyList()
            _contacts.value = emptyList()
            _currentMessages.value = emptyList()
        }
    }

    private suspend fun loadLocalData() {
        loadChats()
        loadContacts()
    }

    private suspend fun loadChats() {
        val chatEntities = chatDao.getAllChats()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        _chats.value = chatEntities.map { chat ->
            ChatItem(
                jid = chat.jid,
                alias = chat.alias,
                lastMsg = chat.lastMsg,
                time = sdf.format(Date(chat.lastTime)),
                unread = chat.unread,
                typing = chat.typing,
                photoUrl = chat.photoUrl,
                status = "delivered"
            )
        }
    }

    private suspend fun loadContacts() {
        val contactEntities = contactDao.getAllContacts()
        _contacts.value = contactEntities.map { contact ->
            ContactItem(
                jid = contact.jid,
                alias = contact.alias,
                info = contact.bio,
                photoUrl = contact.photoUrl
            )
        }
    }

    fun openChat(jid: String) {
        _activeChatJid.value = jid
        _activeChatTyping.value = false
        viewModelScope.launch {
            chatDao.markRead(jid)
            loadChats()

            val phone = _phone.value
            val messages = messageDao.getMessages(jid)
            _currentMessages.value = messages.map { msg ->
                Message(
                    id = msg.id,
                    from = msg.from,
                    body = msg.body,
                    time = msg.time,
                    status = msg.status
                )
            }

            val contact = contactDao.getAllContacts().find { it.jid == jid }
            _activeChatName.value = contact?.alias ?: jid.substringBefore("@")
        }
    }

    fun closeChat() {
        _activeChatJid.value = null
        _currentMessages.value = emptyList()
    }

    fun sendMessage(body: String, replyTo: String? = null) {
        val jid = _activeChatJid.value ?: return
        val mid = xmppManager.sendMessage(jid, body, replyTo)

        viewModelScope.launch {
            val msg = MessageEntity(
                id = mid,
                chatJid = jid,
                from = _phone.value,
                body = body,
                time = System.currentTimeMillis(),
                status = "sent"
            )
            messageDao.insertMessage(msg)

            chatDao.insertChat(ChatEntity(
                jid = jid,
                alias = _activeChatName.value,
                photoUrl = "",
                lastMsg = body,
                lastTime = System.currentTimeMillis(),
                unread = 0,
                typing = false
            ))

            val current = _currentMessages.value.toMutableList()
            current.add(Message(id = mid, from = _phone.value, body = body, time = System.currentTimeMillis(), status = "sent"))
            _currentMessages.value = current

            loadChats()
        }
    }

    fun sendTyping() {
        val jid = _activeChatJid.value ?: return
        xmppManager.sendTyping(jid)
    }

    fun editMessage(originalId: String, newBody: String) {
        val jid = _activeChatJid.value ?: return
        xmppManager.editMessage(jid, originalId, newBody)

        viewModelScope.launch {
            messageDao.updateMessage(originalId, newBody)
            val current = _currentMessages.value.map {
                if (it.id == originalId) it.copy(body = newBody) else it
            }
            _currentMessages.value = current
        }
    }

    fun deleteMessage(id: String, forEveryone: Boolean = false) {
        val jid = _activeChatJid.value ?: return
        if (forEveryone) {
            xmppManager.deleteMessage(jid, id)
        }

        viewModelScope.launch {
            messageDao.deleteMessage(id)
            _currentMessages.value = _currentMessages.value.filter { it.id != id }
        }
    }

    fun searchByTodusId(todusId: String) {
        xmppManager.searchByTodusId(todusId)
    }

    fun clearSearch() {
        _searchResult.value = null
        _searchNotFound.value = false
    }

    fun updateProfile(alias: String, bio: String) {
        viewModelScope.launch {
            _alias.value = alias
            _bio.value = bio
            sessionManager.updateProfile(alias, _photoUrl.value, bio, _todusId.value)

            // Enviar a servidor via REST
            try {
                val jwt = sessionManager.getJwtSync()
                val client = okhttp3.OkHttpClient()
                val json = org.json.JSONObject().apply {
                    put("alias", alias)
                    put("description", bio)
                    put("picture_url", _photoUrl.value)
                    put("picture_thumbnail_url", "")
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = okhttp3.Request.Builder()
                    .url("https://auth.todus.cu/v2/todus/users.me.json")
                    .post(body)
                    .header("Authorization", jwt)
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) {}
        }
    }

    fun addContactFromSearch() {
        val result = _searchResult.value ?: return
        viewModelScope.launch {
            contactDao.insertContact(ContactEntity(
                jid = result.jid,
                alias = result.alias,
                photoUrl = result.photoUrl,
                bio = result.info
            ))
            loadContacts()
            clearSearch()
        }
    }

    private fun getContactName(phone: String): String {
        val contacts = _contacts.value
        return contacts.find { it.jid.contains(phone) }?.alias ?: phone
    }
}
