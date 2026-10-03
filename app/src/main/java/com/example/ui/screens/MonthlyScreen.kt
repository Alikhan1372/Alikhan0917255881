package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EquipmentData
import com.example.data.EquipmentRepository
import com.example.data.JalaliDateHelper
import com.example.data.PdfExportHelper
import com.example.model.MonthlyReport
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.IndustrialAccent
import com.example.ui.theme.IndustrialPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import kotlinx.coroutines.launch

@Composable
fun MonthlyScreen(
    currentUser: UserProfile,
    reports: List<MonthlyReport>,
    equipmentRepository: EquipmentRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: ماه جاری, 1: سوابق ماه قبل

    val currentJalali = JalaliDateHelper.getTodayJalali()
    val currentMonthKey = currentJalali.monthKey
    val currentMonthTitle = "${currentJalali.monthName} ${currentJalali.year}"

    var showCloseMonthDialog by remember { mutableStateOf(false) }
    var closeMonthError by remember { mutableStateOf<String?>(null) }
    var isClosingMonth by remember { mutableStateOf(false) }

    val isTechManager = currentUser.roleEnum == UserRole.TECHNICAL_MANAGER

    val pastMonthReports = reports.filter { it.month != currentMonthKey }
    val currentMonthReports = reports.filter { it.month == currentMonthKey }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Tab Row: ماه جاری vs سوابق ماه قبل
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = IndustrialPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("سوابق ماه جاری ($currentMonthTitle)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("سوابق ماه‌های قبل", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // ماه جاری
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    CurrentMonthOverviewCard(
                        monthTitle = currentMonthTitle,
                        isTechManager = isTechManager,
                        onCloseMonthClick = {
                            closeMonthError = null
                            showCloseMonthDialog = true
                        }
                    )
                }

                item {
                    Text(
                        text = "وضعیت گزارش تجمیعی ماه جاری:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (currentMonthReports.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ماه جاری هنوز باز است. عملیات و ساعت کاری روزانه در حال ثبت هستند. در پایان ماه، مسئول فنی با زدن دکمه «بستن و جمع‌بندی ماه» سوابق قطعی را ثبت و گزارش نهایی را تولید می‌نماید.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(currentMonthReports, key = { it.reportId }) { report ->
                        ReportCard(
                            report = report,
                            onExportPdf = {
                                val res = PdfExportHelper.generateAndShareMonthlyReportPdf(context, report)
                                if (res.isFailure) {
                                    Toast.makeText(context, "خطا در ساخت PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(70.dp)) }
            }
        } else {
            // سوابق ماه قبل
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "آرشیو گزارش‌های بسته شده و خروجی PDF (Immutable Snapshots)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (pastMonthReports.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "هنوز ماهی بسته نشده است. پس از پایان اولین ماه و جمع‌بندی، گزارش‌های آرشیوشده اینجا نمایش داده می‌شوند.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(pastMonthReports, key = { it.reportId }) { report ->
                        ReportCard(
                            report = report,
                            onExportPdf = {
                                val res = PdfExportHelper.generateAndShareMonthlyReportPdf(context, report)
                                if (res.isFailure) {
                                    Toast.makeText(context, "خطا در ساخت PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(70.dp)) }
            }
        }
    }

    // Close Month Dialog
    if (showCloseMonthDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClosingMonth) showCloseMonthDialog = false },
            title = {
                Text(
                    text = "بستن و جمع‌بندی ماه ($currentMonthTitle)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "با بستن ماه، سیستم عدم عقب‌افتادگی سرویس‌های روزانه را بررسی کرده و در صورت تکمیل بودن، گزارش نهایی غیرقابل تغییر (Snapshot) برای هر وسیله ایجاد و به آرشیو منتقل می‌گردد.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "توجه: موجودی جاری انبار روغن با بستن ماه دست‌نخورده باقی می‌ماند.",
                        style = MaterialTheme.typography.labelSmall,
                        color = IndustrialAccent,
                        fontWeight = FontWeight.Bold
                    )
                    if (closeMonthError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = closeMonthError!!,
                            color = StatusDanger,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isClosingMonth = true
                        closeMonthError = null
                        coroutineScope.launch {
                            val res = equipmentRepository.closeMonth(currentUser, currentMonthKey)
                            isClosingMonth = false
                            res.fold(
                                onSuccess = {
                                    showCloseMonthDialog = false
                                    Toast.makeText(context, "ماه با موفقیت جمع‌بندی شد.", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    closeMonthError = err.localizedMessage
                                }
                            )
                        }
                    },
                    enabled = !isClosingMonth,
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialPrimary)
                ) {
                    if (isClosingMonth) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text("تأیید و جمع‌بندی نهایی")
                    }
                }
            },
            dismissButton = {
                if (!isClosingMonth) {
                    TextButton(onClick = { showCloseMonthDialog = false }) {
                        Text("انصراف")
                    }
                }
            }
        )
    }
}

@Composable
fun CurrentMonthOverviewCard(
    monthTitle: String,
    isTechManager: Boolean,
    onCloseMonthClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "دوره ماهانه فعال",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IndustrialAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = IndustrialPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            if (isTechManager) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onCloseMonthClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("close_month_button")
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بستن و جمع‌بندی ماه", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ReportCard(
    report: MonthlyReport,
    onExportPdf: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = report.equipmentName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "دوره: ${report.month} | بسته شده توسط: ${report.closedByDisplayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                OutlinedButton(
                    onClick = onExportPdf,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("export_pdf_button_${report.reportId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = StatusDanger,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("خروجی PDF", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "کارکرد: ${report.totalWorkHours} ساعت",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "سرویس‌ها: ${report.dailyServicesCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "تعویض روغن: ${report.oilChangesCount} بار",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
