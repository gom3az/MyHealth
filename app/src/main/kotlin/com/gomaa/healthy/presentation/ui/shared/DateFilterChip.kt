package com.gomaa.healthy.presentation.ui.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gomaa.healthy.domain.model.DateRangeFilter
import com.gomaa.healthy.presentation.ui.theme.Dimensions
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterChip(
    selectedFilter: DateRangeFilter,
    onFilterChanged: (DateRangeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectingStartDate by remember { mutableStateOf(true) }
    var tempStartDate by remember { mutableStateOf<LocalDate?>(null) }

    Row(
        modifier = modifier
            .width(120.dp)
            .clickable { showDialog = true }) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = "Select date range",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = selectedFilter.displayName(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Date Range") },
            confirmButton = {},
            text = {
                DateRangeOptions(
                    selectedFilter = selectedFilter,
                    onFilterChanged = { filter ->
                        if (filter is DateRangeFilter.Custom) {
                            // Custom selected - show date picker
                            showDialog = false
                            selectingStartDate = true
                            tempStartDate = null
                            showDatePicker = true
                        } else {
                            onFilterChanged(filter)
                            showDialog = false
                        }
                    })
            })
    }

    if (showDatePicker) {
        CustomDatePickerDialog(
            selectingStartDate = selectingStartDate,
            initialDate = if (selectingStartDate) null else tempStartDate,
            onDateSelected = { selectedDate ->
                if (selectingStartDate) {
                    // First date selected - now select end date
                    tempStartDate = selectedDate
                    selectingStartDate = false
                } else {
                    // Second date selected - create custom filter
                    onFilterChanged(DateRangeFilter.Custom(tempStartDate!!, selectedDate))
                    showDatePicker = false
                    tempStartDate = null
                    selectingStartDate = true
                }
            },
            onDismiss = {
                showDatePicker = false
                tempStartDate = null
                selectingStartDate = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDatePickerDialog(
    selectingStartDate: Boolean,
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()
            ?.toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(selectedDate)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        Column {
            Text(
                text = if (selectingStartDate) "Select Start Date" else "Select End Date",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DateRangeOptions(
    selectedFilter: DateRangeFilter, onFilterChanged: (DateRangeFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .width(200.dp)
    ) {
        // Today
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFilterChanged(DateRangeFilter.Today) }
                .padding(vertical = Dimensions.spacingSmall),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedFilter == DateRangeFilter.Today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedFilter == DateRangeFilter.Today) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Divider()

        // Last 7 Days
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFilterChanged(DateRangeFilter.Last7Days) }
                .padding(vertical = Dimensions.spacingSmall),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = "7 Days",
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedFilter == DateRangeFilter.Last7Days) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedFilter == DateRangeFilter.Last7Days) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Divider()

        // Last 30 Days
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFilterChanged(DateRangeFilter.Last30Days) }
                .padding(vertical = Dimensions.spacingSmall),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = "30 Days",
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedFilter == DateRangeFilter.Last30Days) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedFilter == DateRangeFilter.Last30Days) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Divider()

        // All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFilterChanged(DateRangeFilter.All) }
                .padding(vertical = Dimensions.spacingSmall),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = "All",
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedFilter == DateRangeFilter.All) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedFilter == DateRangeFilter.All) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Divider()

        // Custom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onFilterChanged(
                        DateRangeFilter.Custom(
                            LocalDate.now(),
                            LocalDate.now()
                        )
                    )
                }
                .padding(vertical = Dimensions.spacingSmall),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = "Custom",
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedFilter is DateRangeFilter.Custom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedFilter is DateRangeFilter.Custom) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}