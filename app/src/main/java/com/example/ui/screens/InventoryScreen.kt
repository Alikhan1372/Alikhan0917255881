package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.OilBarrel
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EquipmentRepository
import com.example.data.JalaliDateHelper
import com.example.model.InventoryStock
import com.example.model.OilTransaction
import com.example.model.OilTypes
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.theme.IndustrialAccent
import com.example.ui.theme.IndustrialPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen(
    currentUser: UserProfile,
    inventoryList: List<InventoryStock>,
    transactions: List<OilTransaction>,
    equipmentRepository: EquipmentRepository
) {
    val canAddIncoming = currentUser.roleEnum == UserRole.REPRESENTATIVE || currentUser.roleEnum == UserRole.TECHNICAL_MANAGER
    val isTechManager = currentUser.roleEnum == UserRole.TECHNICAL_MANAGER

    var showIncomingDialog by remember { mutableStateOf(false) }
    var transactionToCorrect by remember { mutableStateOf<OilTransaction?>(null) }

    val engineOilStock = inventoryList.find { it.oilType == OilTypes.ENGINE_OIL_20W50 }?.currentAmountLiters ?: 0.0
    val hydOilStock = inventoryList.find { it.oilType == OilTypes.HYDRAULIC_OIL_1068 }?.currentAmountLiters ?: 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Stock Overview Cards
        item {
            Text(
                text = "موجودی انبار روغن",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StockCard(
                    title = OilTypes.ENGINE_OIL_NAME,
                    amount = engineOilStock,
                    modifier = Modifier.weight(1f)
                )
                StockCard(
                    title = OilTypes.HYDRAULIC_OIL_NAME,
                    amount = hydOilStock,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action: Record Incoming Oil
        if (canAddIncoming) {
            item {
                Button(
                    onClick = { showIncomingDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("record_incoming_oil_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت ورود روغن به انبار", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Transactions Ledger Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سوابق تراکنش‌های انبار",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${transactions.size} تراکنش",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "هنوز تراکنشی در انبار ثبت نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(transactions, key = { it.transactionId }) { tx ->
                TransactionCard(
                    tx = tx,
                    canEdit = isTechManager,
                    onEditClick = { transactionToCorrect = tx }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) }
    }

    if (showIncomingDialog) {
        IncomingOilDialog(
            currentUser = currentUser,
            equipmentRepository = equipmentRepository,
            onDismiss = { showIncomingDialog = false },
            onSuccess = { showIncomingDialog = false }
        )
    }

    transactionToCorrect?.let { tx ->
        CorrectionDialog(
            transaction = tx,
            currentUser = currentUser,
            equipmentRepository = equipmentRepository,
            onDismiss = { transactionToCorrect = null },
            onSuccess = { transactionToCorrect = null }
        )
    }
}

@Composable
fun StockCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$amount",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (amount < 20) StatusDanger else IndustrialAccent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "لیتر",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    tx: OilTransaction,
    canEdit: Boolean,
    onEditClick: () -> Unit
) {
    val isIn = tx.direction == "in"
    val dirColor = if (isIn) StatusSuccess else StatusDanger
    val dirIcon = if (isIn) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(dirColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = dirIcon,
                            contentDescription = null,
                            tint = dirColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tx.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${OilTypes.getDisplayName(tx.oilType)} • ${JalaliDateHelper.formatIsoToJalali(tx.date)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (isIn) "+" else "-"}${tx.amountLiters} لیتر",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = dirColor
                    )

                    if (canEdit) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "اصلاح",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (tx.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tx.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ثبت‌کننده: ${tx.displayNameSnapshot}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingOilDialog(
    currentUser: UserProfile,
    equipmentRepository: EquipmentRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val oilOptions = listOf(OilTypes.ENGINE_OIL_20W50, OilTypes.HYDRAULIC_OIL_1068)
    var selectedOil by remember { mutableStateOf(oilOptions[0]) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var isInitialStock by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ثبت ورود روغن به انبار",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Dropdown for Oil Type
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = it }
                ) {
                    OutlinedTextField(
                        value = OilTypes.getDisplayName(selectedOil),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع روغن") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        oilOptions.forEach { oil ->
                            DropdownMenuItem(
                                text = { Text(OilTypes.getDisplayName(oil)) },
                                onClick = {
                                    selectedOil = oil
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("مقدار ورودی (لیتر)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("incoming_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("شماره حواله / توضیحات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = StatusDanger, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "لطفاً مقدار معتبر (بیشتر از صفر) وارد فرمایید."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = equipmentRepository.recordIncomingOil(
                            user = currentUser,
                            oilType = selectedOil,
                            amountLiters = amount,
                            date = JalaliDateHelper.getTodayIsoDate(),
                            time = JalaliDateHelper.getCurrentTime(),
                            notes = notesText.trim(),
                            isInitial = isInitialStock
                        )
                        isSubmitting = false
                        result.fold(
                            onSuccess = { onSuccess() },
                            onFailure = { err -> errorMessage = err.localizedMessage }
                        )
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialPrimary),
                modifier = Modifier.testTag("submit_incoming_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("ثبت و افزایش موجودی")
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

@Composable
fun CorrectionDialog(
    transaction: OilTransaction,
    currentUser: UserProfile,
    equipmentRepository: EquipmentRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var newAmountText by remember { mutableStateOf(transaction.amountLiters.toString()) }
    var reasonText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اصلاح اطلاعات تراکنش انبار",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "طبق ضوابط، تراکنش حذف نمی‌شود؛ بلکه مقدار آن اصلاح شده و اثر مابه‌التفاوت روی موجودی انبار اعمال می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "تراکنش: ${transaction.reason} (مقدار فعلی: ${transaction.amountLiters} لیتر)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newAmountText,
                    onValueChange = {
                        newAmountText = it
                        errorMessage = null
                    },
                    label = { Text("مقدار جدید (لیتر)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("دلیل اصلاح (اجباری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = StatusDanger, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newAmount = newAmountText.toDoubleOrNull()
                    if (newAmount == null || newAmount <= 0) {
                        errorMessage = "مقدار جدید باید عددی و بزرگتر از صفر باشد."
                        return@Button
                    }
                    if (reasonText.trim().isEmpty()) {
                        errorMessage = "ذکر دلیل اصلاح اجباری است."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = equipmentRepository.correctOilTransaction(
                            user = currentUser,
                            transactionId = transaction.transactionId,
                            newAmountLiters = newAmount,
                            reasonNote = reasonText.trim()
                        )
                        isSubmitting = false
                        result.fold(
                            onSuccess = { onSuccess() },
                            onFailure = { err -> errorMessage = err.localizedMessage }
                        )
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("اعمال اصلاح")
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
