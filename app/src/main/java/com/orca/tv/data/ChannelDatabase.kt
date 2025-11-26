package com.orca.tv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room 数据库
 */
@Database(entities = [Channel::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ChannelDatabase : RoomDatabase() {
    
    abstract fun channelDao(): ChannelDao
    
    companion object {
        @Volatile
        private var INSTANCE: ChannelDatabase? = null
        
        fun getDatabase(context: Context): ChannelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChannelDatabase::class.java,
                    "orca_tv_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * DAO (Data Access Object)
 */
@androidx.room.Dao
interface ChannelDao {
    
    @androidx.room.Query("SELECT * FROM channels ORDER BY category, id")
    suspend fun getAllChannels(): List<Channel>
    
    @androidx.room.Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY lastPlayedTime DESC")
    suspend fun getFavoriteChannels(): List<Channel>
    
    @androidx.room.Query("SELECT * FROM channels WHERE category = :category ORDER BY id")
    suspend fun getChannelsByCategory(category: String): List<Channel>
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<Channel>)
    
    @androidx.room.Update
    suspend fun update(channel: Channel)
    
    @androidx.room.Query("DELETE FROM channels")
    suspend fun deleteAll()
}
