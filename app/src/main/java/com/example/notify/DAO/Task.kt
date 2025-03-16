package com.example.notify.DAO

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.ColumnInfo
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Entity
data class Task(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String,
    @PrimaryKey(autoGenerate = true) val uid: Int = 0
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM task")
    fun getAll(): Flow<List<Task>>

    @Upsert
    suspend fun upsertAll(vararg tasks: Task)

    @Delete
    suspend fun delete(user: Task)
}

@Database(entities = [Task::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}


object DatabaseProvider {
    @Volatile
    private var db: AppDatabase? = null;
    fun getDB(context: Context): AppDatabase {
        if (db == null) {
            db = Room.databaseBuilder(context, AppDatabase::class.java, "app-db").build()
        }
        return db as AppDatabase
    }
}

class TaskViewModel(
    private val dao: TaskDao
) : ViewModel() {
    val tasks =
        dao.getAll().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(),
            emptyList()
        )


    fun upsertTask(task: Task) {
        viewModelScope.launch {
            dao.upsertAll(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.delete(task)
        }
    }
}