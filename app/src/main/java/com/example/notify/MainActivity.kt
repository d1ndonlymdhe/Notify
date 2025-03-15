@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.notify.ui.theme.NotifyTheme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Main()
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

data class Task(var title: String, var description: String)

@Composable
fun Main() {
    val context = LocalContext.current

    val (value, setValue) = remember {
        mutableStateOf("")
    }
//
    val tasks = remember {
        mutableStateListOf(Task("Title 1", "Description 1"), Task("Title 2", "Description 2"))
    }

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
                    tasks.add(Task("New Task", "New Description"))
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
                                        tasks.remove(task)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    when(dismissState.targetValue){
                                        SwipeToDismissBoxValue.Settled->{}
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            Text("DELETE")
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            Text("Delete")
                                        }
                                    }
                                },
                                enableDismissFromStartToEnd = true,
                            ) {
                                TaskCard(task.title, { it ->
                                    tasks[idx] = Task(it, task.description)
                                }, task.description, { it ->
                                    tasks[idx] = Task(task.title, it)
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


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Main()
}