package com.example.notify.store

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Entity
data class Task(
    val title: String,
    val description: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM TASK")
    fun getAllTasks(): Flow<List<Task>>

    @Upsert
    suspend fun upsertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}


@Database(entities = [Task::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract val dao: TaskDao

    companion object {
        @Volatile
        private var database: AppDatabase? = null
        public fun getInstance(context: Context): AppDatabase {
            return database ?: synchronized(this) {
                database ?: buildDatabase(context).also { database = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "app_database").build()
        }
    }
}

class TaskViewModel(private val dao: TaskDao) : ViewModel() {
    val tasks =
        dao.getAllTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun addTask(task: Task) {
        viewModelScope.launch {
            dao.upsertTask(task)
        }
    }
    fun updateTask(task:Task) {
        viewModelScope.launch {
            dao.upsertTask(task)
        }
    }
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }
}