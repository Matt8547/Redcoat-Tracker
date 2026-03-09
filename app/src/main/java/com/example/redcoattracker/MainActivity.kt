package com.example.redcoattracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.redcoattracker.data.AppDatabase
import com.example.redcoattracker.data.Discipline
import com.example.redcoattracker.ui.theme.RedcoatTrackerTheme
import com.example.redcoattracker.viewmodel.DisciplineViewModel
import com.example.redcoattracker.viewmodel.DisciplineViewModelFactory
import com.example.redcoattracker.viewmodel.JtacStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Top-level constant for the date formatter to improve performance
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val presetDisciplines = listOf(
    "Type 1", "Type 2", "Type 3", "BOC", "BOT", "FW Cas", "RW Cas", "Hot", "VDL", "Laser", "Rem Obs", "LLTTP", "Night", "IR", "Evaluation date"
)

class MainActivity : ComponentActivity() {
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: DisciplineViewModel by viewModels {
        DisciplineViewModelFactory(db.disciplineDao())
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MediaPlayer.create(this, R.raw.startup_sound).apply {
            setOnCompletionListener { it.release() }
            start()
        }

        setContent {
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(10000) // 10 seconds
                showSplash = false
            }

            RedcoatTrackerTheme {
                if (showSplash) {
                    SplashScreen()
                } else {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        MainScreen(viewModel)
                    }
                }
            }
        }

        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.redcoat_splash),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .rotate(90f)
                .scale(1.8225f),
            contentScale = ContentScale.Fit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: DisciplineViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }

    val disciplineList by viewModel.allDisciplines.collectAsState()
    val jtacStatus by viewModel.jtacStatus.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showPrintDialog by remember { mutableStateOf(false) }
    var editingDiscipline by remember { mutableStateOf<Discipline?>(null) }
    var name by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var cs by remember { mutableStateOf(prefs.getString("user_cs", "") ?: "") }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.redcoat_splash),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.5f),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.training_currency)) },
                    actions = {
                        IconButton(onClick = { showPrintDialog = true }) {
                            Icon(Icons.Default.Print, contentDescription = "Print Report")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_discipline))
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(padding)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newName ->
                                name = newName
                                prefs.edit().putString("user_name", newName).apply()
                            },
                            label = { Text("Name") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cs,
                            onValueChange = { newCs ->
                                cs = newCs
                                prefs.edit().putString("user_cs", newCs).apply()
                            },
                            label = { Text("C/S") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(
                                color = when (jtacStatus) {
                                    JtacStatus.GREEN -> Color(0xFFC8E6C9)
                                    JtacStatus.YELLOW -> Color(0xFFFFECB3)
                                    JtacStatus.RED -> Color(0xFFFFCDD2)
                                })
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "JTAC Currency")
                    }
                    if (disciplineList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.no_disciplines))
                        }
                    } else {
                        LazyColumn {
                            items(disciplineList) { item ->
                                DisciplineCard(item) {
                                    editingDiscipline = item
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.app_version),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }

            if (showAddDialog) {
                AddDisciplineDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = { disciplineNames, date ->
                        disciplineNames.forEach { disciplineName ->
                            viewModel.insert(disciplineName, date)
                        }
                        showAddDialog = false
                    }
                )
            }

            if (showPrintDialog) {
                PrintReportDialog(
                    viewModel = viewModel,
                    userName = name,
                    userCs = cs,
                    onDismiss = { showPrintDialog = false }
                )
            }

            editingDiscipline?.let { discipline ->
                EditDisciplineDialog(
                    discipline = discipline,
                    onDismiss = { editingDiscipline = null },
                    onSave = { updatedDiscipline ->
                        viewModel.insert(updatedDiscipline.name, updatedDiscipline.completionDate)
                        editingDiscipline = null
                    }
                )
            }
        }
    }
}

@Composable
fun PrintReportDialog(
    viewModel: DisciplineViewModel,
    userName: String,
    userCs: String,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().minusMonths(6)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showStartDatePicker) {
        DatePickerModal(
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { startDate = it }
        )
    }

    if (showEndDatePicker) {
        DatePickerModal(
            initialDate = endDate,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { endDate = it }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Print Training Report") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Date Range:")
                Button(onClick = { showStartDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("From: ${startDate.format(dateFormatter)}")
                }
                Button(onClick = { showEndDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("To: ${endDate.format(dateFormatter)}")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                CoroutineScope(Dispatchers.Main).launch {
                    val list = viewModel.getDisciplinesInRange(startDate, endDate)
                    generateAndPrintReport(context, list, startDate, endDate, userName, userCs)
                    onDismiss()
                }
            }) { Text("Print") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(initialDate: LocalDate, onDismiss: () -> Unit, onDateSelected: (LocalDate) -> Unit) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

fun generateAndPrintReport(
    context: Context,
    disciplines: List<Discipline>,
    start: LocalDate,
    end: LocalDate,
    name: String,
    cs: String
) {
    val counts = disciplines.groupingBy { it.name }.eachCount()
    
    val html = StringBuilder().apply {
        append("<html><body>")
        append("<h1>Training Currency Report</h1>")
        append("<p><b>Name:</b> $name | <b>C/S:</b> $cs</p>")
        append("<p><b>Period:</b> ${start.format(dateFormatter)} to ${end.format(dateFormatter)}</p>")
        
        append("<h2>Summary</h2><ul>")
        presetDisciplines.forEach { disciplineName ->
            val count = counts[disciplineName] ?: 0
            append("<li>$disciplineName: $count</li>")
        }
        append("</ul>")

        append("<h2>Activity Log</h2>")
        append("<table border='1' width='100%' style='border-collapse: collapse;'>")
        append("<tr><th>Date</th><th>Discipline</th></tr>")
        disciplines.forEach {
            append("<tr><td>${it.completionDate.format(dateFormatter)}</td><td>${it.name}</td></tr>")
        }
        append("</table>")
        append("</body></html>")
    }.toString()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "RedcoatTracker_Report_${LocalDate.now()}"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
}

@Composable
fun DisciplineCard(discipline: Discipline, onLongPress: (Discipline) -> Unit) {
    val today = LocalDate.now()

    val (expiryDate, label) = if (discipline.name == "Evaluation date") {
        Pair(discipline.completionDate.plusMonths(18), "18 Month Expiry")
    } else {
        Pair(discipline.completionDate.plusMonths(6), "6 Month Expiry")
    }

    val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)

    val cardColor = when {
        daysUntilExpiry < 0 -> Color(0xFFFFCDD2)
        daysUntilExpiry <= 30 -> Color(0xFFFFECB3)
        else -> Color(0xFFC8E6C9)
    }
    val textColor = Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongPress(discipline) }
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = discipline.name, style = MaterialTheme.typography.headlineSmall, color = textColor)
            Spacer(modifier = Modifier.height(8.dp))

            if (discipline.name == "Evaluation date") {
                ExpiryRow(label, expiryDate, textColor)
            } else {
                val twelveMonthExpiry = discipline.completionDate.plusMonths(12)
                ExpiryRow(stringResource(R.string.six_month_expiry), expiryDate, textColor)
                ExpiryRow(stringResource(R.string.twelve_month_expiry), twelveMonthExpiry, textColor)
            }
        }
    }
}

@Composable
fun ExpiryRow(label: String, expiryDate: LocalDate, textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = textColor)
        Text(
            text = expiryDate.format(dateFormatter),
            color = textColor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDisciplineDialog(onDismiss: () -> Unit, onSave: (List<String>, LocalDate) -> Unit) {
    val selectedDisciplines = remember { mutableStateListOf<String>() }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerModal(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate = it }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_discipline)) },
        text = {
            Column {
                Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.date_format, selectedDate.format(dateFormatter)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(presetDisciplines) { discipline ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedDisciplines.contains(discipline)) {
                                        selectedDisciplines.remove(discipline)
                                    } else {
                                        selectedDisciplines.add(discipline)
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedDisciplines.contains(discipline),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        selectedDisciplines.add(discipline)
                                    } else {
                                        selectedDisciplines.remove(discipline)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(discipline)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (selectedDisciplines.isNotEmpty()) onSave(selectedDisciplines.toList(), selectedDate) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDisciplineDialog(discipline: Discipline, onDismiss: () -> Unit, onSave: (Discipline) -> Unit) {
    var selectedDate by remember { mutableStateOf(discipline.completionDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerModal(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate = it }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${discipline.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Current Date: ${discipline.completionDate.format(dateFormatter)}")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.date_format, selectedDate.format(dateFormatter)))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(discipline.copy(completionDate = selectedDate)) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
