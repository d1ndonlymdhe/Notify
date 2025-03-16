@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.notify.ui.theme.NotifyTheme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.notify.DAO.DatabaseProvider
import com.example.notify.DAO.Task
import com.example.notify.DAO.TaskViewModel

class MainActivity : ComponentActivity() {
    private val db by lazy {
        DatabaseProvider.getDB(applicationContext)
    }
    private val viewModel by viewModels<TaskViewModel> {
        object : ViewModelProvider.Factory {
            fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return TaskViewModel(db.taskDao()) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val tasks by viewModel.tasks.collectAsState()
            Main(tasks, viewModel)
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

var x = 0;

fun getBuilder(text: String, context: Context): NotificationCompat.Builder {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Your Task")
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
fun Main(tasks: List<Task>, viewModel: TaskViewModel) {
    val context = LocalContext.current
    val db = DatabaseProvider.getDB(context)
    val taskDao = db.taskDao();
//
//    val tasks = remember {
//        mutableStateListOf(taskDao.getAll())
//    }

    LaunchedEffect(Unit) {
        createNotificationChannel(context)
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Camera Permission Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Camera Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    NotifyTheme {
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    title = { Text(text = "Mdhe Notify") }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    viewModel.upsertTask(Task("Title", "Hello There"))
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                LazyColumn(modifier = Modifier.padding(2.dp, 4.dp))
                {
                    tasks.forEachIndexed { idx, task ->
                        item {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                        viewModel.deleteTask(task)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.Settled -> {}
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Red)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    "DELETE TASK",
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .size(128.dp)
                                                )
                                            }
                                        }

                                        SwipeToDismissBoxValue.EndToStart -> {

                                        }
                                    }
                                },
                                enableDismissFromStartToEnd = true,
                                enableDismissFromEndToStart = false
                            ) {
                                TaskCard(task.title, { title ->
                                    viewModel.upsertTask(Task(title, task.description, task.uid))
                                }, task.description, { description ->
                                    viewModel.upsertTask(Task(task.title, description, task.uid))

                                })
                            }
                        }
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
fun TaskCard(
    title: String,
    setTitle: (String) -> Unit,
    description: String,
    setDescription: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp, 10.dp)
    ) {
        TextField(
            placeholder = { Text(text = "Title") },
            value = title,
            onValueChange = setTitle,
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(10.dp, 8.dp)
        )
        OutlinedTextField(
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


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    Main()
//}