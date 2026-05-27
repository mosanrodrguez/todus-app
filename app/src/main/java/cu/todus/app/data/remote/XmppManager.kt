package cu.todus.app.data.remote

import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.net.Socket

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, WAITING_NETWORK
}

data class XmppMessage(
    val id: String,
    val from: String,
    val body: String,
    val time: Long,
    val chatJid: String
)

class XmppManager {

    private var socket: Socket? = null
    private var job: Job? = null
    private var readJob: Job? = null

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state

    private val _messages = MutableStateFlow<List<XmppMessage>>(emptyList())
    val messages: StateFlow<List<XmppMessage>> = _messages

    var onMessageReceived: ((XmppMessage) -> Unit)? = null
    var onTypingReceived: ((String) -> Unit)? = null
    var onProfileReceived: ((String, String, String, String) -> Unit)? = null
    var onContactsReceived: ((List<Pair<String, String>>) -> Unit)? = null
    var onContactFound: ((String, String) -> Unit)? = null

    private fun createTrustAllSslFactory(): SSLSocketFactory {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(c: Array<X509Certificate?>?, a: String?) {}
            override fun checkServerTrusted(c: Array<X509Certificate?>?, a: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
        return ctx.socketFactory
    }

    private fun randomId(length: Int = 8): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun send(data: String) {
        socket?.getOutputStream()?.write(data.toByteArray())
        socket?.getOutputStream()?.flush()
    }

    private fun readLine(): String {
        val baos = ByteArrayOutputStream()
        while (true) {
            val b = socket?.getInputStream()?.read() ?: return ""
            if (b == 0x3e.toInt()) {
                baos.write(b)
                val s = baos.toString("UTF-8")
                if (s.endsWith(">")) return s
            } else {
                baos.write(b)
            }
        }
    }

    suspend fun connect(phone: String, jwt: String) = withContext(Dispatchers.IO) {
        _state.value = ConnectionState.CONNECTING

        try {
            val sslFactory = createTrustAllSslFactory()
            socket = sslFactory.createSocket("ws.todus.cu", 5222)

            send("<?xml version=\"1.0\"?><stream:stream to=\"im.todus.cu\" xmlns=\"jc\" xmlns:stream=\"x1\" version=\"1.0\" xml:lang=\"en\">")
            readLine()

            val auth = Base64.encodeToString("\u0000$phone\u0000$jwt".toByteArray(), Base64.NO_WRAP)
            send("<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">$auth</auth>")
            val authResp = readLine()

            if (authResp.contains("not-authorized")) {
                _state.value = ConnectionState.WAITING_NETWORK
                return@withContext
            }

            send("<?xml version=\"1.0\"?><stream:stream to=\"im.todus.cu\" xmlns=\"jc\" xmlns:stream=\"x1\" version=\"1.0\" xml:lang=\"en\">")
            readLine()

            val bindId = randomId()
            send("<iq type=\"set\" id=\"$bindId\"><bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\"><resource>todus</resource></bind></iq>")
            readLine()

            send("<iq type=\"set\" id=\"s$bindId\"><session xmlns=\"urn:ietf:params:xml:ns:xmpp-session\"/></iq>")
            readLine()

            send("<presence/>")
            readLine()

            _state.value = ConnectionState.CONNECTED
            fetchProfile(phone)
            fetchContacts()
            startListening()

        } catch (e: Exception) {
            _state.value = ConnectionState.WAITING_NETWORK
        }
    }

    private fun fetchProfile(phone: String) {
        val id = randomId()
        send("<iq type=\"get\" id=\"$id\"><query xmlns=\"todus:users:getinfo\" users=\"$phone\"/></iq>")
    }

    private fun fetchContacts() {
        send("<iq type=\"get\" id=\"contacts1\"><query xmlns=\"todus:roster:list:2\"/></iq>")
    }

    fun searchByTodusId(todusId: String) {
        val id = randomId()
        send("<iq type=\"get\" id=\"search_$id\"><query xmlns=\"todus:users:gettodusid\" user=\"$todusId\"/></iq>")
    }

    fun sendMessage(to: String, body: String, replyTo: String? = null): String {
        val mid = randomId()
        val escapedBody = body
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        val replyXml = if (replyTo != null) {
            val stamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS+00:00", java.util.Locale.US).format(java.util.Date())
            "<forwarded xmlns=\"urn:xmpp:forward:0\"><delay xmlns=\"urn:xmpp:delay\" stamp=\"$stamp\"/><m xmlns=\"jc\" o=\"$to\" f=\"$to\" i=\"$replyTo\" t=\"c\"><b></b><k xmlns=\"x8\"/></m></forwarded>"
        } else ""

        send("<m to=\"$to\" t=\"c\" i=\"$mid\" xmlns=\"jc\"><k xmlns=\"x8\"/>$replyXml<b>$escapedBody</b></m>")
        return mid
    }

    fun sendTyping(to: String) {
        send("<m to=\"$to\" t=\"c\" i=\"${randomId()}\"><csp xmlns=\"uc1\"/></m>")
    }

    fun editMessage(to: String, originalId: String, newBody: String): String {
        val editId = randomId()
        val escapedBody = newBody
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        send("<m to=\"$to\" t=\"c\" i=\"$originalId\"><edited xmlns=\"edited:n\" i=\"$editId\" mi=\"$originalId\"/><k xmlns=\"x8\"/><b>$escapedBody</b></m>")
        return editId
    }

    fun deleteMessage(to: String, originalId: String): String {
        val delId = randomId()
        send("<m to=\"$to\" t=\"c\" i=\"$originalId\"><deleted xmlns=\"deleted:n\" i=\"$delId\" mi=\"$originalId\"/><k xmlns=\"x8\"/><b/></m>")
        return delId
    }

    private fun startListening() {
        readJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                while (isActive) {
                    val data = readLine()
                    if (data.isNotEmpty()) {
                        parseXml(data)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    _state.value = ConnectionState.WAITING_NETWORK
                }
            }
        }
    }

    private fun parseXml(xml: String) {
        val from = Regex("f='([^']+)'").find(xml)?.groupValues?.get(1) ?: ""
        val id = Regex("i='([^']+)'").find(xml)?.groupValues?.get(1) ?: ""
        val bodyMatch = Regex("<b>(.*?)</b>", RegexOption.DOT_MATCHES_ALL).find(xml)
        val body = bodyMatch?.groupValues?.get(1)
            ?.replace("&lt;", "<")
            ?.replace("&gt;", ">")
            ?.replace("&amp;", "&") ?: ""

        when {
            xml.contains("<csp") -> {
                val sender = from.split("/").lastOrNull()?.split("@")?.firstOrNull() ?: from
                onTypingReceived?.invoke(sender)
            }
            xml.contains("<rd") || xml.contains("<dd") -> {
                // Message status update
            }
            xml.contains("alias='") -> {
                val alias = Regex("alias='([^']+)'").find(xml)?.groupValues?.get(1) ?: ""
                val photo = Regex("pic_url='([^']+)'").find(xml)?.groupValues?.get(1) ?: ""
                val bio = Regex("description='([^']*)'").find(xml)?.groupValues?.get(1) ?: ""
                val todusId = Regex("todus_id='([^']+)'").find(xml)?.groupValues?.get(1) ?: ""
                if (alias.isNotEmpty()) {
                    onProfileReceived?.invoke(alias, photo, bio, todusId)
                }
            }
            xml.contains("<contact") -> {
                val contacts = Regex("phone='([^']+)'[^>]*alias='([^']+)'").findAll(xml)
                val list = contacts.map { match ->
                    val phone = match.groupValues[1]
                    val aliasB64 = match.groupValues[2]
                    val alias = try {
                        String(Base64.decode(aliasB64, Base64.URL_SAFE))
                    } catch (e: Exception) { phone }
                    phone to alias
                }.toList()
                if (list.isNotEmpty()) {
                    onContactsReceived?.invoke(list)
                }
            }
            xml.contains("username='") && xml.contains("search_") -> {
                val phone = Regex("username='([^']+)'").find(xml)?.groupValues?.get(1) ?: ""
                val alias = Regex("alias='([^']+)'").find(xml)?.groupValues?.get(1) ?: phone
                if (phone.isNotEmpty()) {
                    onContactFound?.invoke(phone, alias)
                }
            }
            body.isNotEmpty() && id.isNotEmpty() -> {
                val chatJid = if (from.contains("/")) from.substringBefore("/") else from
                val sender = from.split("/").lastOrNull()?.split("@")?.firstOrNull() ?: from.split("@").firstOrNull() ?: from
                val msg = XmppMessage(
                    id = id,
                    from = sender,
                    body = body,
                    time = System.currentTimeMillis(),
                    chatJid = chatJid
                )
                val current = _messages.value.toMutableList()
                current.add(msg)
                _messages.value = current
                onMessageReceived?.invoke(msg)
            }
        }
    }

    fun disconnect() {
        readJob?.cancel()
        job?.cancel()
        try { socket?.close() } catch (_: Exception) {}
        _state.value = ConnectionState.DISCONNECTED
    }
}
