package com.epfl.esl.musicplayer

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/*
    StayConnected module to manage user connection state using ROOM database.
    It allows to save, fetch and delete the connected user information locally.

    Only one user can be saved at a time. The saved user can sign off, which deletes
    the user information from the local database.
*/

// ROOM database (select tables + DAO)
@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

// ROOM table called "users"
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 0,   // Unique ID (always will be 0 as we only have one saved user at a time)
    val username: String,          // Username
    val userKey: String            // Firebase profile ID
)

// ROOM DAO (Data Access Object) (Messenger for our ROOM database)
@Dao
interface UserDao {

    // Add user
    @Insert
    suspend fun insertUser(user: User)

    // Fetch user
    @Query("SELECT * FROM users WHERE id = 0")
    suspend fun getUser(): User?

    // Delete user
    @Query("DELETE FROM users WHERE id = 0")
    suspend fun deleteUser()
}

// ROOM database singleton
object DatabaseProvider {
    private var db: AppDatabase? = null

    // Get database instance if exists, else create it
    fun getDatabase(context: Context): AppDatabase {
        if (db == null) {
            db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "connection_db"
            ).build()
        }
        return db!!
    }
}