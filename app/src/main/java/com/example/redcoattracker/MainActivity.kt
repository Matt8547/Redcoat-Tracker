package com.example.redcoattracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.redcoattracker.data.AppDatabase
import com.example.redcoattracker.data.Discipline
import com.example.redcoattracker.ui.theme.RedcoatTrackerTheme
import com.example.redcoattracker.viewmodel.DisciplineViewModel
import com.example.redcoattracker.viewmodel.DisciplineViewModelFactory
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Top-level constant for the date formatter to improve performance
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

class MainActivity : ComponentActivity() {
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: DisciplineViewModel by viewModels {
        DisciplineViewModelFactory(db.disciplineDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    }
}

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.redcoat_splash),
            contentDescription = null, // The image is decorative
            modifier = Modifier
                .fillMaxSize()
                .rotate(90f)
                .scale(1.8225f), // Reduce size by 10%
            contentScale = ContentScale.Fit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: DisciplineViewModel) {
    // State management for the list and dialog
    val disciplineList by viewModel.allDisciplines.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.training_currency)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_discipline))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) { // Use a Box to allow bottom alignment
            Column(modifier = Modifier.padding(padding)) {
                if (disciplineList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_disciplines))
                    }
                } else {
                    LazyColumn {
                        items(disciplineList) { item ->
                            DisciplineCard(item)
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

        if (showDialog) {
            AddDisciplineDialog(
                onDismiss = { showDialog = false },
                onSave = { name, date ->
                    viewModel.insert(name, date)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun DisciplineCard(discipline: Discipline) {
    val sixMonthExpiry = discipline.completionDate.plusMonths(6)
    val twelveMonthExpiry = discipline.completionDate.plusMonths(12)
    val today = LocalDate.now()

    val daysUntilExpiry = ChronoUnit.DAYS.between(today, sixMonthExpiry)

    val cardColor = when {
        daysUntilExpiry < 0 -> Color(0xFFFFCDD2) // Expired - Light Red
        daysUntilExpiry <= 30 -> Color(0xFFFFECB3) // Expires soon - Light Amber
        else -> Color(0xFFC8E6C9) // In date - Light Green
    }
    val textColor = Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = discipline.name, style = MaterialTheme.typography.headlineSmall, color = textColor)
            Spacer(modifier = Modifier.height(8.dp))

            ExpiryRow(stringResource(R.string.six_month_expiry), sixMonthExpiry, textColor)
            ExpiryRow(stringResource(R.string.twelve_month_expiry), twelveMonthExpiry, textColor)
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
fun AddDisciplineDialog(onDismiss: () -> Unit, onSave: (String, LocalDate) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_discipline)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.discipline_name)) })
                Button(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.date_format, selectedDate.format(dateFormatter)))
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, selectedDate) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
