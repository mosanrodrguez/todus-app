package cu.todus.app.data.local

import androidx.room.*

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val jid: String,
    val alias: String,
    val photoUrl: String,
    val bio: String,
    val online: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val jid: String,
    val alias: String,
    val photoUrl: String,
    val lastMsg: String,
    val lastTime: Long,
    val unread: Int = 0,
    val typing: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatJid: String,
    val from: String,
    val body: String,
    val time: Long,
    val status: String = "sent"
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastTime DESC")
    suspend fun getAllChats(): List<ChatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("UPDATE chats SET unread = 0 WHERE jid = :jid")
    suspend fun markRead(jid: String)

    @Query("UPDATE chats SET typing = :typing WHERE jid = :jid")
    suspend fun setTyping(jid: String, typing: Boolean)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatJid = :jid ORDER BY time ASC")
    suspend fun getMessages(jid: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("UPDATE messages SET body = :body WHERE id = :id")
    suspend fun updateMessage(id: String, body: String)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY alias ASC")
    suspend fun getAllContacts(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}

@Database(
    entities = [ContactEntity::class, ChatEntity::class, MessageEntity::class],
    version = 1
)
abstract class TodusDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
}
