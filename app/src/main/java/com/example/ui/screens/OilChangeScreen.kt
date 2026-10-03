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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.EquipmentRepository
import com.example.data.JalaliDateHelper
import com.example.model.Equipment
import com.example.model.OilChangeStatus
import com.example.model.OilTypes
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.IndustrialAccent
import com.example.ui.theme.IndustrialPrimary
import com.example.ui.theme.StatusDanger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OilChangeScreen(
    equipment: Equipment,
    currentUser: UserProfile,
    equipmentRepository: EquipmentRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var oilStatus by remember { mutableStateOf<OilChangeStatus?>(null) }
    var currentInventoryStock by remember { mutableDoubleStateOf(0.0) }
    var isLoadingData by remember { mutableStateOf(true) }

    // Form fields strictly in specified order:
    // 1. مقدار مصرف روغن موتور این دوره (عدد، واحد: لیتر)
    var oilAmountText by remember { mutableStateOf("") }
    // 2. فیلتر گازوئیل (checkbox)
    var dieselFilter by remember { mutableStateOf(false) }
    // 3. فیلتر روغن (checkbox)
    var oilFilter by remember { mutableStateOf(false) }
    // 4. فیلتر آبگیر (checkbox)
    var waterSeparatorFilter by remember { mutableStateOf(false) }
    // 5. توضیحات (متن آزاد)
    var notesText by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(equipment.equipmentId) {
        isLoadingData = true
        oilStatus = equipmentRepository.calculateOilChangeStatus(equipment.equipmentId).getOrNull()
        // Fetch current engine oil inventory
        equipmentRepository.observeInventory().collect { list ->
            val engineStock = list.find { it.oilType == OilTypes.ENGINE_OIL_20W50 }
            currentInventoryStock = engineStock?.currentAmountLiters ?: 0.0
            isLoadingData = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "تعویض روغن موتور — ${equipment.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ثبت تعویض روغن و فیلترها (دوره ۹۰ ساعته)",
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
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (currentUser.roleEnum != UserRole.TECHNICAL_MANAGER) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "شما اجازه دسترسی به این بخش را ندارید. فقط مسئول فنی می‌تواند تعویض روغن را ثبت کند.",
                    color = StatusDanger,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            return@Scaffold
        }

        if (isLoadingData) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = IndustrialPrimary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status and Baseline Overview
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "وضعیت دوره تعویض روغن (مبنای خودکار ۹۰ ساعته):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    oilStatus?.let { status ->
                        Text(
                            text = "مجموع ساعت کارکرد از آخرین تعویض: ${status.currentIntervalHours} ساعت",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (status.isDue)
                                "موعد تعویض رسیده است (${status.overdueHours} ساعت overdue)"
                            else
                                "${status.remainingHours} ساعت تا موعد تعویض باقیمانده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (status.isDue) StatusDanger else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "موجودی فعلی روغن موتور 20W-50 در انبار: $currentInventoryStock لیتر",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentInventoryStock < 10) StatusDanger else IndustrialAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "فرم ثبت تعویض روغن:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 1. مقدار مصرف روغن موتور این دوره (عدد، واحد: لیتر)
            OutlinedTextField(
                value = oilAmountText,
                onValueChange = {
                    oilAmountText = it
                    errorMessage = null
                },
                label = { Text("۱. مقدار مصرف روغن موتور این دوره (لیتر)") },
                placeholder = { Text("مثلاً: ۱۸") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("oil_amount_input")
            )

            // 2. فیلتر گازوئیل (checkbox)
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dieselFilter,
                        onCheckedChange = { dieselFilter = it },
                        modifier = Modifier.testTag("diesel_filter_checkbox")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "۲. تعویض فیلتر گازوئیل", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 3. فیلتر روغن (checkbox)
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = oilFilter,
                        onCheckedChange = { oilFilter = it },
                        modifier = Modifier.testTag("oil_filter_checkbox")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "۳. تعویض فیلتر روغن", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 4. فیلتر آبگیر (checkbox)
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = waterSeparatorFilter,
                        onCheckedChange = { waterSeparatorFilter = it },
                        modifier = Modifier.testTag("water_separator_checkbox")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "۴. تعویض فیلتر آبگیر", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 5. توضیحات (متن آزاد)
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("۵. توضیحات") },
                placeholder = { Text("توضیحات تکمیلی عملیات تعویض روغن") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("oil_change_notes_input")
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = StatusDanger,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. تأیید
            Button(
                onClick = {
                    val amount = oilAmountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "لطفاً مقدار مصرف روغن موتور را به صورت عددی و بیشتر از صفر وارد فرمایید."
                        return@Button
                    }

                    if (currentInventoryStock < amount) {
                        errorMessage = "موجودی روغن موتور 20W-50 کافی نیست. موجودی فعلی: $currentInventoryStock لیتر."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = equipmentRepository.recordOilChange(
                            user = currentUser,
                            equipmentId = equipment.equipmentId,
                            date = JalaliDateHelper.getTodayIsoDate(),
                            oilAmountLiters = amount,
                            dieselFilter = dieselFilter,
                            oilFilter = oilFilter,
                            waterSeparatorFilter = waterSeparatorFilter,
                            notes = notesText.trim()
                        )
                        isSubmitting = false
                        result.fold(
                            onSuccess = { onNavigateBack() },
                            onFailure = { err -> errorMessage = err.localizedMessage }
                        )
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_oil_change_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "۶. تأیید نهایی تعویض روغن و کسر از انبار",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
