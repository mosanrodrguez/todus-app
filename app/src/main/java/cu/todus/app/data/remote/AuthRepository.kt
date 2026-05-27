package cu.todus.app.data.remote

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AuthRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getToken(phone: String, uuid: String): String? = withContext(Dispatchers.IO) {
        try {
            val secret = uuid.replace("-", "").take(32)

            val body = ByteArray(2 + phone.length + 2 + secret.length).apply {
                this[0] = 0x0a.toByte()
                this[1] = phone.length.toByte()
                phone.encodeToByteArray().copyInto(this, 2)
                this[2 + phone.length] = 0x12.toByte()
                this[3 + phone.length] = secret.length.toByte()
                secret.encodeToByteArray().copyInto(this, 4 + phone.length)
            }

            val request = Request.Builder()
                .url("https://auth.todus.cu/v2/auth/token")
                .post(body.toRequestBody("application/x-protobuf".toMediaType()))
                .header("User-Agent", "ToDus 2.1.2 Auth")
                .header("Host", "auth.todus.cu")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val jwtRegex = Regex("[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")
            val jwt = jwtRegex.find(responseBody)?.value

            jwt
        } catch (e: Exception) {
            null
        }
    }

    fun getTodusIdFromJwt(jwt: String): String {
        return try {
            val parts = jwt.split(".")
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            JSONObject(payload).optString("toDusId", "")
        } catch (e: Exception) { "" }
    }
}
