package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.ServiceOilConsumption
import com.example.model.DailyService
import com.example.model.Equipment
import com.example.model.OilTypes
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.IndustrialAccent
import com.example.ui.theme.IndustrialPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusWarning
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyServiceScreen(
    equipment: Equipment,
    currentUser: UserProfile,
    equipmentRepository: EquipmentRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val backlogDates = remember { mutableStateListOf<String>() }
    var currentBacklogIndex by remember { mutableStateOf(0) }
    var isLoadingBacklog by remember { mutableStateOf(true) }

    fun refreshBacklog() {
        coroutineScope.launch {
            isLoadingBacklog = true
            val dates = equipmentRepository.getDailyServiceBacklog(equipment.equipmentId, daysToCheck = 7)
            backlogDates.clear()
            backlogDates.addAll(dates)
            currentBacklogIndex = 0
            isLoadingBacklog = false
        }
    }

    LaunchedEffect(equipment.equipmentId) {
        refreshBacklog()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "سرویس روزانه — ${equipment.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ثبت چک‌لیست و بررسی فنی",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IndustrialPrimary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { paddingValues ->
        if (currentUser.roleEnum != UserRole.TECHNICAL_MANAGER) {
            BoxContent(paddingValues) {
                Text(
                    text = "شما اجازه دسترسی به این بخش را ندارید. تنها مسئول فنی مجاز به ثبت سرویس روزانه است.",
                    color = StatusDanger,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            return@Scaffold
        }

        if (isLoadingBacklog) {
            BoxContent(paddingValues) {
                CircularProgressIndicator(color = IndustrialPrimary)
            }
            return@Scaffold
        }

        val targetDate = if (backlogDates.isNotEmpty() && currentBacklogIndex < backlogDates.size) {
            backlogDates[currentBacklogIndex]
        } else {
            JalaliDateHelper.getTodayIsoDate()
        }

        DailyServiceForm(
            modifier = Modifier.padding(paddingValues),
            equipment = equipment,
            currentUser = currentUser,
            serviceDate = targetDate,
            totalBacklogCount = backlogDates.size,
            currentBacklogNumber = currentBacklogIndex + 1,
            isCompletedForNow = backlogDates.isEmpty(),
            equipmentRepository = equipmentRepository,
            onSubmitted = {
                if (currentBacklogIndex + 1 < backlogDates.size) {
                    currentBacklogIndex++
                } else {
                    refreshBacklog()
                }
            }
        )
    }
}

@Composable
private fun BoxContent(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun DailyServiceForm(
    modifier: Modifier = Modifier,
    equipment: Equipment,
    currentUser: UserProfile,
    serviceDate: String,
    totalBacklogCount: Int,
    currentBacklogNumber: Int,
    isCompletedForNow: Boolean,
    equipmentRepository: EquipmentRepository,
    onSubmitted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var checkEngineOil by remember(serviceDate) { mutableStateOf(false) }
    var engineOilServiceOpen by remember(serviceDate) { mutableStateOf(false) }
    var engineOilLitersText by remember(serviceDate) { mutableStateOf("") }
    var engineOilNotes by remember(serviceDate) { mutableStateOf("") }

    var checkGearboxOil by remember(serviceDate) { mutableStateOf(false) }
    var gearboxOilServiceOpen by remember(serviceDate) { mutableStateOf(false) }
    var gearboxOilLitersText by remember(serviceDate) { mutableStateOf("") }
    var gearboxOilNotes by remember(serviceDate) { mutableStateOf("") }

    var checkHydraulicOil by remember(serviceDate) { mutableStateOf(false) }
    var hydraulicOilServiceOpen by remember(serviceDate) { mutableStateOf(false) }
    var hydraulicOilLitersText by remember(serviceDate) { mutableStateOf("") }
    var hydraulicOilNotes by remember(serviceDate) { mutableStateOf("") }

    var checkOilLeaks by remember(serviceDate) { mutableStateOf(false) }
    var oilLeaksNotes by remember(serviceDate) { mutableStateOf("") }

    var checkRadiatorWater by remember(serviceDate) { mutableStateOf(false) }

    var checkHoses by remember(serviceDate) { mutableStateOf(false) }
    var hosesNotes by remember(serviceDate) { mutableStateOf("") }

    var checkAirCleaning by remember(serviceDate) { mutableStateOf(false) }
    var checkAirFilter by remember(serviceDate) { mutableStateOf(false) }
    var checkGreasing by remember(serviceDate) { mutableStateOf(false) }
    var checkWashing by remember(serviceDate) { mutableStateOf(false) }
    var checkCabinCleaning by remember(serviceDate) { mutableStateOf(false) }

    var errorMessage by remember(serviceDate) { mutableStateOf<String?>(null) }
    var isSubmitting by remember(serviceDate) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Backlog Status Banner
        if (totalBacklogCount > 0) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "عقب‌افتادگی سرویس",
                        tint = StatusWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "سرویس روزانه این وسیله $totalBacklogCount روز عقب افتاده است.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )
                        Text(
                            text = "در حال تکمیل فرم روز $currentBacklogNumber از $totalBacklogCount (${JalaliDateHelper.formatIsoToJalali(serviceDate)})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "به‌روز",
                        tint = IndustrialPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "سرویس برای تاریخ: ${JalaliDateHelper.formatIsoToJalali(serviceDate)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "چک‌لیست اقلام سرویس روزانه:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Items with possible service / oil consumption
        if (equipment.hasEngineOil) {
            ServiceItemCard(
                title = "چک روغن موتور",
                checked = checkEngineOil,
                onCheckedChange = { checkEngineOil = it },
                hasServiceButton = true,
                isServiceOpen = engineOilServiceOpen,
                onToggleService = { engineOilServiceOpen = !engineOilServiceOpen },
                serviceOilType = OilTypes.ENGINE_OIL_NAME,
                serviceLiters = engineOilLitersText,
                onLitersChange = { engineOilLitersText = it },
                serviceNotes = engineOilNotes,
                onNotesChange = { engineOilNotes = it }
            )

            ServiceItemCard(
                title = "چک روغن گیربکس (تأمین از روغن موتور 20W-50)",
                checked = checkGearboxOil,
                onCheckedChange = { checkGearboxOil = it },
                hasServiceButton = true,
                isServiceOpen = gearboxOilServiceOpen,
                onToggleService = { gearboxOilServiceOpen = !gearboxOilServiceOpen },
                serviceOilType = OilTypes.ENGINE_OIL_NAME,
                serviceLiters = gearboxOilLitersText,
                onLitersChange = { gearboxOilLitersText = it },
                serviceNotes = gearboxOilNotes,
                onNotesChange = { gearboxOilNotes = it }
            )
        }

        ServiceItemCard(
            title = "چک روغن هیدرولیک",
            checked = checkHydraulicOil,
            onCheckedChange = { checkHydraulicOil = it },
            hasServiceButton = true,
            isServiceOpen = hydraulicOilServiceOpen,
            onToggleService = { hydraulicOilServiceOpen = !hydraulicOilServiceOpen },
            serviceOilType = OilTypes.HYDRAULIC_OIL_NAME,
            serviceLiters = hydraulicOilLitersText,
            onLitersChange = { hydraulicOilLitersText = it },
            serviceNotes = hydraulicOilNotes,
            onNotesChange = { hydraulicOilNotes = it }
        )

        // Leaks with optional notes
        SimpleCheckWithNoteCard(
            title = "چک روغن‌ریزی و اتصالات",
            checked = checkOilLeaks,
            onCheckedChange = { checkOilLeaks = it },
            notes = oilLeaksNotes,
            onNotesChange = { oilLeaksNotes = it },
            placeholder = "مثلاً: اتصال شیلنگ سمت راست بررسی شد، نشتی مشاهده نشد."
        )

        if (equipment.hasRadiator) {
            SimpleCheckboxCard(
                title = "چک آب رادیاتور",
                checked = checkRadiatorWater,
                onCheckedChange = { checkRadiatorWater = it }
            )
        }

        // Hoses with optional notes
        SimpleCheckWithNoteCard(
            title = "چک شلنگ‌ها و اتصالات",
            checked = checkHoses,
            onCheckedChange = { checkHoses = it },
            notes = hosesNotes,
            onNotesChange = { hosesNotes = it },
            placeholder = "مثلاً: شلنگ هیدرولیک سمت راست بررسی شد."
        )

        // General service items
        SimpleCheckboxCard(
            title = "بادگیری کامل",
            checked = checkAirCleaning,
            onCheckedChange = { checkAirCleaning = it }
        )

        SimpleCheckboxCard(
            title = "بادگیری هواکش",
            checked = checkAirFilter,
            onCheckedChange = { checkAirFilter = it }
        )

        if (equipment.hasGrease) {
            SimpleCheckboxCard(
                title = "گریسکاری کامل",
                checked = checkGreasing,
                onCheckedChange = { checkGreasing = it }
            )
        }

        SimpleCheckboxCard(
            title = "شستشوی دستگاه",
            checked = checkWashing,
            onCheckedChange = { checkWashing = it }
        )

        SimpleCheckboxCard(
            title = "تمیز کردن کابین",
            checked = checkCabinCleaning,
            onCheckedChange = { checkCabinCleaning = it }
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = StatusDanger,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                // Collect oil consumptions if any service is filled
                val consumptions = mutableListOf<ServiceOilConsumption>()

                if (engineOilServiceOpen && engineOilLitersText.isNotBlank()) {
                    val l = engineOilLitersText.toDoubleOrNull()
                    if (l == null || l <= 0) {
                        errorMessage = "مقدار لیتر سرریز روغن موتور معتبر نیست."
                        return@Button
                    }
                    consumptions.add(
                        ServiceOilConsumption(
                            oilType = OilTypes.ENGINE_OIL_20W50,
                            amountLiters = l,
                            operationType = "service_engine",
                            reason = "سرریز روغن موتور ${equipment.name}",
                            notes = engineOilNotes.trim()
                        )
                    )
                }

                if (gearboxOilServiceOpen && gearboxOilLitersText.isNotBlank()) {
                    val l = gearboxOilLitersText.toDoubleOrNull()
                    if (l == null || l <= 0) {
                        errorMessage = "مقدار لیتر سرریز روغن گیربکس معتبر نیست."
                        return@Button
                    }
                    consumptions.add(
                        ServiceOilConsumption(
                            oilType = OilTypes.ENGINE_OIL_20W50,
                            amountLiters = l,
                            operationType = "service_gearbox",
                            reason = "سرریز روغن گیربکس ${equipment.name}",
                            notes = gearboxOilNotes.trim()
                        )
                    )
                }

                if (hydraulicOilServiceOpen && hydraulicOilLitersText.isNotBlank()) {
                    val l = hydraulicOilLitersText.toDoubleOrNull()
                    if (l == null || l <= 0) {
                        errorMessage = "مقدار لیتر سرریز روغن هیدرولیک معتبر نیست."
                        return@Button
                    }
                    consumptions.add(
                        ServiceOilConsumption(
                            oilType = OilTypes.HYDRAULIC_OIL_1068,
                            amountLiters = l,
                            operationType = "service_hydraulic",
                            reason = "سرریز روغن هیدرولیک ${equipment.name}",
                            notes = hydraulicOilNotes.trim()
                        )
                    )
                }

                val service = DailyService(
                    recordId = "${equipment.equipmentId}_$serviceDate",
                    equipmentId = equipment.equipmentId,
                    date = serviceDate,
                    checkEngineOil = checkEngineOil,
                    checkGearboxOil = checkGearboxOil,
                    checkHydraulicOil = checkHydraulicOil,
                    checkOilLeaks = checkOilLeaks,
                    oilLeaksNotes = oilLeaksNotes.trim(),
                    checkRadiatorWater = checkRadiatorWater,
                    checkHoses = checkHoses,
                    hosesNotes = hosesNotes.trim(),
                    checkAirCleaning = checkAirCleaning,
                    checkAirFilter = checkAirFilter,
                    checkGreasing = checkGreasing,
                    checkWashing = checkWashing,
                    checkCabinCleaning = checkCabinCleaning,
                    userId = currentUser.userId,
                    displayNameSnapshot = currentUser.displayName
                )

                isSubmitting = true
                errorMessage = null
                coroutineScope.launch {
                    val res = equipmentRepository.recordDailyService(
                        user = currentUser,
                        service = service,
                        oilConsumptions = consumptions
                    )
                    isSubmitting = false
                    res.fold(
                        onSuccess = { onSubmitted() },
                        onFailure = { err -> errorMessage = err.localizedMessage }
                    )
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_daily_service_button"),
            colors = ButtonDefaults.buttonColors(containerColor = IndustrialPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (totalBacklogCount > 1 && currentBacklogNumber < totalBacklogCount)
                        "تأیید و رفتن به روز بعدی ($serviceDate)"
                    else
                        "تأیید و ثبت نهایی سرویس روزانه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SimpleCheckboxCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SimpleCheckWithNoteCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    placeholder: String
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = onCheckedChange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            if (checked) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
                    label = { Text("توضیحات اختیاری") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ServiceItemCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hasServiceButton: Boolean,
    isServiceOpen: Boolean,
    onToggleService: () -> Unit,
    serviceOilType: String,
    serviceLiters: String,
    onLitersChange: (String) -> Unit,
    serviceNotes: String,
    onNotesChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = onCheckedChange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }

                if (hasServiceButton) {
                    OutlinedButton(
                        onClick = onToggleService,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (isServiceOpen) "لغو سرویس" else "سرویس / سرریز")
                    }
                }
            }

            if (isServiceOpen) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "تأمین خودکار از: $serviceOilType",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = serviceLiters,
                            onValueChange = onLitersChange,
                            label = { Text("مقدار مصرف / سرریز (لیتر)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = serviceNotes,
                            onValueChange = onNotesChange,
                            label = { Text("توضیحات سرویس") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
