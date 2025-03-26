package com.example.notify.store

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity
data class Task(
    val title: String,
    val description: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int,
)

@Dao
interface ContactDao {
    @Query("SELECT * FROM TASK")
    fun getAllContacts(): Flow<List<Task>>

    @Upsert
    suspend fun upsertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}


@Database(entities = [Task::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    companion object{
        @Volatile private var database: AppDatabase? = null
        public fun getInstance(context: Context): AppDatabase{
            return database ?: synchronized (this){
                database ?:buildDatabase(context).also{database = it}
            }
        }
        private fun buildDatabase(context: Context): AppDatabase{
            return Room.databaseBuilder(context, AppDatabase::class.java, "app_database").build()
        }
    }
}