package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.EquipmentRepository
import com.example.data.JalaliDateHelper
import com.example.model.Equipment
import com.example.model.UserProfile
import com.example.ui.theme.IndustrialPrimary
import com.example.ui.theme.StatusDanger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkHoursDialog(
    equipment: Equipment,
    currentUser: UserProfile,
    equipmentRepository: EquipmentRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val recentDays = remember { JalaliDateHelper.getRecentDays(7) }

    var selectedDate by remember { mutableStateOf(recentDays.first()) }
    var expandedDateDropdown by remember { mutableStateOf(false) }

    var hoursText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val shiftCheck = JalaliDateHelper.canSubmitWorkHourForDate(selectedDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "ثبت ساعت کارکرد روزانه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = equipment.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Shift Rule Notice
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "قانون شیفت",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "شیفت کاری: ۰۶:۰۰ الی ۱۸:۰۰. ثبت ساعت کارکرد روز جاری فقط پس از پایان شیفت (ساعت ۱۸:۰۰) مجاز است.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDateDropdown,
                    onExpandedChange = { expandedDateDropdown = it }
                ) {
                    OutlinedTextField(
                        value = JalaliDateHelper.formatIsoToJalali(selectedDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تاریخ کارکرد") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDateDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDateDropdown,
                        onDismissRequest = { expandedDateDropdown = false }
                    ) {
                        recentDays.forEach { dayIso ->
                            val isToday = dayIso == JalaliDateHelper.getTodayIsoDate()
                            val label = JalaliDateHelper.formatIsoToJalali(dayIso) + (if (isToday) " (امروز)" else "")
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedDate = dayIso
                                    expandedDateDropdown = false
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }

                if (!shiftCheck.first) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = shiftCheck.second ?: "",
                        color = StatusDanger,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = hoursText,
                    onValueChange = {
                        hoursText = it
                        errorMessage = null
                    },
                    label = { Text("مدت کارکرد واقعی (ساعت)") },
                    placeholder = { Text("مثلاً: ۸ یا ۸.۵") },
                    supportingText = { Text("حداکثر ۱۲ ساعت در روز مجاز است.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("work_hours_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = StatusDanger,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hours = hoursText.toDoubleOrNull()
                    if (hours == null) {
                        errorMessage = "لطفاً مدت ساعت کارکرد را به صورت عددی وارد فرمایید."
                        return@Button
                    }
                    if (hours < 0 || hours > 12) {
                        errorMessage = "ساعت کاری روزانه باید بین ۰ تا ۱۲ ساعت باشد."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = equipmentRepository.recordWorkHours(
                            user = currentUser,
                            equipmentId = equipment.equipmentId,
                            date = selectedDate,
                            hours = hours
                        )
                        isSubmitting = false
                        result.fold(
                            onSuccess = { onSuccess() },
                            onFailure = { err -> errorMessage = err.localizedMessage }
                        )
                    }
                },
                enabled = !isSubmitting && shiftCheck.first,
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialPrimary),
                modifier = Modifier.testTag("submit_work_hours_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("ثبت نهایی")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
