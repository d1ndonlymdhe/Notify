@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.notify.store.AppDatabase
import com.example.notify.store.Task
import com.example.notify.store.TaskViewModel
import com.example.notify.ui.theme.NotifyTheme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database by lazy {
            AppDatabase.getInstance(this)
        }
        val viewModel: TaskViewModel by viewModels<TaskViewModel>(
            factoryProducer = {
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TaskViewModel(database.dao) as T
                    }
                }
            }
        )
        enableEdgeToEdge()
        setContent {
            Main(viewModel)
        }
    }
}

const val CHANNEL_ID = "TEST_!"

private fun createNotificationChannel(context: Context) {
    val name = "Mdhe Notify"
    val descriptionText = "Will be used to send you timely notifications about your tasks"
    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
    }
    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

fun getBuilder(text: String, context: Context): NotificationCompat.Builder {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("Your Task")
        .setContentText(text)
    return builder
}

fun checkForNotificationPermission(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        TODO("VERSION.SDK_INT < TIRAMISU")
    }
}

@Composable
fun Main(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    LaunchedEffect(Unit) {
        createNotificationChannel(context)
    }
    val (showAddDialog, setShowAddDialog) = remember {
        mutableStateOf(false)
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Notification Permission Granted!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(context, "Notification Permission Denied!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    NotifyTheme {
        Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ), title = { Text(text = "Mdhe Notify") })
        }, floatingActionButton = {
            FloatingActionButton(onClick = {
                setShowAddDialog(true)
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                if (showAddDialog) {
                    AddCardDialog(
                        cancel = { setShowAddDialog(false) },
                        save = {
                            viewModel.addTask(it)
                            setShowAddDialog(false)
                        }
                    )
                }
                LazyColumn(modifier = Modifier.padding(2.dp, 4.dp)) {
                    itemsIndexed(tasks) { idx, task ->
                        TaskCard(task.title, { it ->
                            viewModel.updateTask(Task(it, task.description, task.id))
                        }, task.description, { it ->
                            viewModel.updateTask(Task(task.title, it, task.id))
                        }, {
                            viewModel.deleteTask(task)
                        })
                    }
                }
            }

        }
    }
}


@Preview(showBackground = true)
@Composable
fun TaskCardPreview() {
    Task("Testing", "Hello There")
}

@Composable
fun AddCardDialog(
    save: (task: Task) -> Unit,
    cancel: () -> Unit
) {
    val (title, setTitle) = remember {
        mutableStateOf("")
    }
    val (description, setDescription) = remember {
        mutableStateOf("")
    }
    Dialog(
        onDismissRequest = {
            cancel()
        }
    ) {
        Card(modifier = Modifier.padding(5.dp)) {
            Column(modifier = Modifier.padding(5.dp)) {
                TaskCard(title, setTitle, description, setDescription, {})
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = { cancel() }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = {
                        save(Task(title, description))
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
//    AlertDialog(onDismissRequest = {
//        cancel()
//    }, confirmButton = {
//        OutlinedButton(onClick = {
//            save(Task(title, description))
//        }) {
//            Text("Save")
//        }
//    }, dismissButton = {
//        OutlinedButton(onClick = {
//            cancel()
//        }) {
//            Text("Cancel")
//        }
//    }, title = { Text("Add a new task") }, text = {
//        TaskCard(title, setTitle, description, setDescription, {}, Modifier.padding(0.dp))
//    }, modifier = Modifier.padding(0.dp))
}

@Composable
fun TaskCard(
    title: String,
    setTitle: (String) -> Unit,
    description: String,
    setDescription: (String) -> Unit,
    delete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp, 10.dp)
            .then(modifier)
    ) {
        Row {
            TextField(
                placeholder = { Text(text = "Title") },
                value = title,
                onValueChange = setTitle,
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(10.dp, 8.dp)
            )
            IconButton(onClick = { delete() }) {
                Icon(Icons.Default.Delete, "Delete this task")
            }
        }
        OutlinedTextField(
            placeholder = {
                Text(text = "Description")
            },
            value = description,
            onValueChange = setDescription,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(10.dp, 4.dp)
                .fillMaxWidth(),
            maxLines = 5
        )

    }
}
