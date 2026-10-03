package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EquipmentRepository
import com.example.data.JalaliDateHelper
import com.example.model.DailyService
import com.example.model.DailyWorkHour
import com.example.model.Equipment
import com.example.model.OilChange
import com.example.model.OilChangeStatus
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.IndustrialAccent
import com.example.ui.theme.IndustrialPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentDetailScreen(
    equipment: Equipment,
    currentUser: UserProfile,
    equipmentRepository: EquipmentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToDailyService: () -> Unit,
    onNavigateToOilChange: () -> Unit
) {
    var showWorkHoursDialog by remember { mutableStateOf(false) }
    var showHydraulicDialog by remember { mutableStateOf(false) }
    var oilStatus by remember { mutableStateOf<OilChangeStatus?>(null) }
    var oilStatusError by remember { mutableStateOf<String?>(null) }

    val workHoursList by equipmentRepository.observeWorkHoursForEquipment(equipment.equipmentId)
        .collectAsState(initial = emptyList())
    val servicesList by equipmentRepository.observeDailyServicesForEquipment(equipment.equipmentId)
        .collectAsState(initial = emptyList())
    val oilChangesList by equipmentRepository.observeOilChangesForEquipment(equipment.equipmentId)
        .collectAsState(initial = emptyList())

    val expandedDates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(equipment.equipmentId, workHoursList, oilChangesList) {
        if (equipment.hasEngineOil) {
            val res = equipmentRepository.calculateOilChangeStatus(equipment.equipmentId)
            res.fold(
                onSuccess = {
                    oilStatus = it
                    oilStatusError = null
                },
                onFailure = {
                    oilStatus = null
                    oilStatusError = it.localizedMessage ?: "خطای دریافت وضعیت تعویض روغن"
                }
            )
        }
    }

    val allDates = remember(workHoursList, servicesList, oilChangesList) {
        val set = mutableSetOf<String>()
        workHoursList.forEach { set.add(it.date) }
        servicesList.forEach { set.add(it.date) }
        oilChangesList.forEach { set.add(it.date) }
        set.sortedDescending()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = equipment.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (equipment.driverName != null) {
                            Text(
                                text = "${equipment.driverRole ?: "مسئول"}: ${equipment.driverName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IndustrialPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Oil Status Card (if applicable)
            if (equipment.hasEngineOil) {
                if (oilStatusError != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "وضعیت دوره تعویض روغن: $oilStatusError",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusWarning,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else if (oilStatus != null) {
                    item {
                        OilStatusCard(oilStatus = oilStatus!!)
                    }
                }
            }

            // Quick Actions Panel (Scoped according to roles and equipment)
            item {
                ActionButtonsPanel(
                    equipment = equipment,
                    currentUser = currentUser,
                    onOpenWorkHours = { showWorkHoursDialog = true },
                    onOpenHydraulicOverflow = { showHydraulicDialog = true },
                    onOpenDailyService = onNavigateToDailyService,
                    onOpenOilChange = onNavigateToOilChange
                )
            }

            // History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "سوابق",
                        tint = IndustrialPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سوابق روزانه و فعالیت‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (allDates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "هنوز سابقه‌ای برای این وسیله ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(allDates, key = { it }) { date ->
                    val isExpanded = expandedDates[date] ?: false
                    val dayWorkHour = workHoursList.find { it.date == date }
                    val dayService = servicesList.find { it.date == date }
                    val dayOilChanges = oilChangesList.filter { it.date == date }

                    DayHistoryCard(
                        date = date,
                        isExpanded = isExpanded,
                        workHour = dayWorkHour,
                        dailyService = dayService,
                        oilChanges = dayOilChanges,
                        onToggleExpand = { expandedDates[date] = !isExpanded }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }

    if (showWorkHoursDialog) {
        WorkHoursDialog(
            equipment = equipment,
            currentUser = currentUser,
            equipmentRepository = equipmentRepository,
            onDismiss = { showWorkHoursDialog = false },
            onSuccess = { showWorkHoursDialog = false }
        )
    }

    if (showHydraulicDialog) {
        HydraulicOverflowDialog(
            equipment = equipment,
            currentUser = currentUser,
            equipmentRepository = equipmentRepository,
            onDismiss = { showHydraulicDialog = false },
            onSuccess = { showHydraulicDialog = false }
        )
    }
}

@Composable
fun OilStatusCard(oilStatus: OilChangeStatus) {
    val statusColor = when {
        oilStatus.isDue -> StatusDanger
        oilStatus.remainingHours <= 15 -> StatusWarning
        else -> StatusSuccess
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.09f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وضعیت دوره تعویض روغن (۹۰ ساعته)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = if (oilStatus.isDue)
                        "موعد تعویض رسیده"
                    else
                        "${oilStatus.remainingHours.toInt()} ساعت باقیمانده",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "کارکرد این دوره: ${oilStatus.currentIntervalHours} ساعت | کل کارکرد تجمعی: ${oilStatus.accumulatedHours} ساعت",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (oilStatus.isDue && oilStatus.overdueHours > 0) {
                Text(
                    text = "${oilStatus.overdueHours} ساعت overdue (بیش از ۹۰ ساعت کارکرد)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = StatusDanger
                )
            }
        }
    }
}

@Composable
fun ActionButtonsPanel(
    equipment: Equipment,
    currentUser: UserProfile,
    onOpenWorkHours: () -> Unit,
    onOpenHydraulicOverflow: () -> Unit,
    onOpenDailyService: () -> Unit,
    onOpenOilChange: () -> Unit
) {
    val canLogWorkHours = when (currentUser.roleEnum) {
        UserRole.DRIVER -> currentUser.assignedEquipmentId == equipment.equipmentId
        UserRole.REPRESENTATIVE -> true
        UserRole.TECHNICAL_MANAGER -> true
    }

    val isTechManager = currentUser.roleEnum == UserRole.TECHNICAL_MANAGER

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Work hours button
            Button(
                onClick = onOpenWorkHours,
                enabled = canLogWorkHours,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("action_work_hours_button")
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ثبت ساعت کاری", style = MaterialTheme.typography.labelMedium)
            }

            // Hydraulic Overflow (For electric compressors)
            if (equipment.hasHydraulicOverflow) {
                Button(
                    onClick = onOpenHydraulicOverflow,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialAccent),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_hydraulic_overflow_button")
                ) {
                    Icon(Icons.Default.OilBarrel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سرریز روغن هیدرولیک", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Technical Manager specific buttons
        if (isTechManager) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDailyService,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_daily_service_button")
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سرویس روزانه", style = MaterialTheme.typography.labelMedium)
                }

                if (equipment.hasEngineOil) {
                    OutlinedButton(
                        onClick = onOpenOilChange,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_oil_change_button")
                    ) {
                        Icon(Icons.Default.OilBarrel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعویض روغن", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun DayHistoryCard(
    date: String,
    isExpanded: Boolean,
    workHour: DailyWorkHour?,
    dailyService: DailyService?,
    oilChanges: List<OilChange>,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = JalaliDateHelper.formatIsoToJalali(date),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (workHour != null) {
                            Text(
                                text = "کارکرد: ${workHour.hours} ساعت",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndustrialAccent
                            )
                        }
                        if (dailyService != null) {
                            Text(
                                text = "✓ سرویس روزانه",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSuccess
                            )
                        }
                        if (oilChanges.isNotEmpty()) {
                            Text(
                                text = "✓ تعویض روغن (${oilChanges.sumOf { it.oilAmountLiters }} لیتر)",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusDanger
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "بستن" else "باز کردن",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Work Hours detail
                        if (workHour != null) {
                            Text(
                                text = "ساعت کاری: ${workHour.hours} ساعت",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ثبت‌کننده ساعت کاری: ${workHour.displayNameSnapshot}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Daily Service detail
                        if (dailyService != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "چک‌لیست سرویس روزانه ثبت‌شده:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val checks = buildList {
                                if (dailyService.checkEngineOil) add("چک روغن موتور")
                                if (dailyService.checkGearboxOil) add("چک روغن گیربکس")
                                if (dailyService.checkHydraulicOil) add("چک روغن هیدرولیک")
                                if (dailyService.checkOilLeaks) add("چک روغن‌ریزی و اتصالات" + (if (dailyService.oilLeaksNotes.isNotEmpty()) " (${dailyService.oilLeaksNotes})" else ""))
                                if (dailyService.checkRadiatorWater) add("چک آب رادیاتور")
                                if (dailyService.checkHoses) add("چک شلنگ‌ها و اتصالات" + (if (dailyService.hosesNotes.isNotEmpty()) " (${dailyService.hosesNotes})" else ""))
                                if (dailyService.checkAirCleaning) add("بادگیری کامل")
                                if (dailyService.checkAirFilter) add("بادگیری هواکش")
                                if (dailyService.checkGreasing) add("گریسکاری")
                                if (dailyService.checkWashing) add("شستشو")
                                if (dailyService.checkCabinCleaning) add("تمیز کردن کابین")
                            }
                            checks.forEach { c ->
                                Text(text = "• $c", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "ثبت‌کننده سرویس: ${dailyService.displayNameSnapshot}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Oil Change detail
                        if (oilChanges.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            oilChanges.forEach { oc ->
                                Text(
                                    text = "عملیات تعویض روغن موتور: ${oc.oilAmountLiters} لیتر",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusDanger
                                )
                                Text(
                                    text = "فیلترها: گازوئیل: ${if (oc.dieselFilter) "تعویض شد" else "خیر"} | روغن: ${if (oc.oilFilter) "تعویض شد" else "خیر"} | آبگیر: ${if (oc.waterSeparatorFilter) "تعویض شد" else "خیر"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (oc.notes.isNotEmpty()) {
                                    Text(text = "توضیحات: ${oc.notes}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = "ثبت‌کننده: ${oc.displayNameSnapshot} (ساعت ${oc.time})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
