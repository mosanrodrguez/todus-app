package cu.todus.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {

    companion object {
        val KEY_PHONE = stringPreferencesKey("phone")
        val KEY_UUID = stringPreferencesKey("uuid")
        val KEY_JWT = stringPreferencesKey("jwt")
        val KEY_ALIAS = stringPreferencesKey("alias")
        val KEY_TODUS_ID = stringPreferencesKey("todus_id")
        val KEY_PHOTO_URL = stringPreferencesKey("photo_url")
        val KEY_BIO = stringPreferencesKey("bio")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[KEY_JWT].isNullOrEmpty()
    }

    val phone: Flow<String> = context.dataStore.data.map { it[KEY_PHONE] ?: "" }
    val uuid: Flow<String> = context.dataStore.data.map { it[KEY_UUID] ?: "" }
    val jwt: Flow<String> = context.dataStore.data.map { it[KEY_JWT] ?: "" }
    val alias: Flow<String> = context.dataStore.data.map { it[KEY_ALIAS] ?: "" }
    val todusId: Flow<String> = context.dataStore.data.map { it[KEY_TODUS_ID] ?: "" }
    val photoUrl: Flow<String> = context.dataStore.data.map { it[KEY_PHOTO_URL] ?: "" }
    val bio: Flow<String> = context.dataStore.data.map { it[KEY_BIO] ?: "" }

    suspend fun saveSession(phone: String, uuid: String, jwt: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PHONE] = phone
            prefs[KEY_UUID] = uuid
            prefs[KEY_JWT] = jwt
        }
    }

    suspend fun updateJwt(jwt: String) {
        context.dataStore.edit { it[KEY_JWT] = jwt }
    }

    suspend fun updateProfile(alias: String, photoUrl: String, bio: String, todusId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ALIAS] = alias
            prefs[KEY_PHOTO_URL] = photoUrl
            prefs[KEY_BIO] = bio
            prefs[KEY_TODUS_ID] = todusId
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun getPhoneSync(): String {
        var result = ""
        context.dataStore.data.collect { prefs ->
            result = prefs[KEY_PHONE] ?: ""
            throw kotlinx.coroutines.CancellationException()
        }
        return result
    }

    suspend fun getUuidSync(): String {
        var result = ""
        context.dataStore.data.collect { prefs ->
            result = prefs[KEY_UUID] ?: ""
            throw kotlinx.coroutines.CancellationException()
        }
        return result
    }

    suspend fun getJwtSync(): String {
        var result = ""
        context.dataStore.data.collect { prefs ->
            result = prefs[KEY_JWT] ?: ""
            throw kotlinx.coroutines.CancellationException()
        }
        return result
    }
}
