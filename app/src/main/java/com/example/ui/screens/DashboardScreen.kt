package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EquipmentRepository
import com.example.model.Equipment
import com.example.model.EquipmentCategory
import com.example.model.InventoryStock
import com.example.model.OilChangeStatus
import com.example.model.UserProfile
import com.example.ui.components.WarningBanner
import com.example.ui.theme.IndustrialAccent
import com.example.ui.theme.IndustrialPrimary
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun DashboardScreen(
    currentUser: UserProfile,
    equipmentList: List<Equipment>,
    inventoryList: List<InventoryStock>,
    equipmentRepository: EquipmentRepository,
    onEquipmentClick: (Equipment) -> Unit
) {
    val oilStatuses = remember { mutableStateMapOf<String, OilChangeStatus>() }
    val oilStatusErrors = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(equipmentList) {
        equipmentList.forEach { equip ->
            if (equip.hasEngineOil) {
                val res = equipmentRepository.calculateOilChangeStatus(equip.equipmentId)
                res.fold(
                    onSuccess = { status ->
                        oilStatuses[equip.equipmentId] = status
                        oilStatusErrors.remove(equip.equipmentId)
                    },
                    onFailure = { err ->
                        oilStatuses.remove(equip.equipmentId)
                        oilStatusErrors[equip.equipmentId] = err.localizedMessage ?: "خطای دریافت وضعیت"
                    }
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Summary Dashboard (Uncluttered, High-Value)
        item {
            WorkshopStatusSection(
                inventoryList = inventoryList,
                oilStatuses = oilStatuses
            )
        }

        item {
            Text(
                text = "فهرست ماشین‌آلات و تجهیزات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(equipmentList, key = { it.equipmentId }) { equipment ->
            EquipmentCard(
                equipment = equipment,
                currentUser = currentUser,
                oilStatus = oilStatuses[equipment.equipmentId],
                oilStatusError = oilStatusErrors[equipment.equipmentId],
                onClick = { onEquipmentClick(equipment) }
            )
        }
    }
}

@Composable
fun WorkshopStatusSection(
    inventoryList: List<InventoryStock>,
    oilStatuses: Map<String, OilChangeStatus>
) {
    val lowStockOils = inventoryList.filter { it.currentAmountLiters < 20.0 && it.currentAmountLiters > 0.0 }
    val emptyStockOils = inventoryList.filter { it.currentAmountLiters <= 0.0 }
    val overdueEquipment = oilStatuses.filter { it.value.isDue }

    if (emptyStockOils.isNotEmpty() || lowStockOils.isNotEmpty() || overdueEquipment.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (overdueEquipment.isNotEmpty()) {
                WarningBanner(
                    title = "موعد تعویض روغن رسیده است",
                    message = "${overdueEquipment.size} دستگاه نیازمند تعویض روغن موتور هستند."
                )
            }
            if (emptyStockOils.isNotEmpty()) {
                WarningBanner(
                    title = "کسری موجودی روغن",
                    message = "موجودی ${emptyStockOils.joinToString(" و ") { it.oilTypeName }} به اتمام رسیده است."
                )
            } else if (lowStockOils.isNotEmpty()) {
                WarningBanner(
                    title = "کاهش سطح موجودی انبار",
                    message = "موجودی ${lowStockOils.joinToString(" و ") { it.oilTypeName }} کمتر از ۲۰ لیتر است."
                )
            }
        }
    } else {
        // Normal Workshop Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StatusSuccess.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "وضعیت عادی",
                    tint = StatusSuccess,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "وضعیت کارگاه: عادی و پایدار",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "موجودی انبار کافی بوده و تعویض روغن اضطراری وجود ندارد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EquipmentCard(
    equipment: Equipment,
    currentUser: UserProfile,
    oilStatus: OilChangeStatus?,
    oilStatusError: String? = null,
    onClick: () -> Unit
) {
    val isAssignedToUser = currentUser.assignedEquipmentId == equipment.equipmentId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("equipment_card_${equipment.equipmentId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAssignedToUser) {
                IndustrialAccent.copy(alpha = 0.06f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when (equipment.category) {
                        EquipmentCategory.GENERATOR.name -> Icons.Default.LocalGasStation
                        EquipmentCategory.COMPRESSOR_ELECTRIC.name -> Icons.Default.ElectricBolt
                        EquipmentCategory.COMPRESSOR_DIESEL.name -> Icons.Default.Speed
                        else -> Icons.Default.Engineering
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isAssignedToUser) IndustrialAccent else IndustrialPrimary,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = equipment.name,
                            tint = if (isAssignedToUser) Color.Black else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = equipment.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (equipment.driverName != null) {
                            Text(
                                text = "${equipment.driverRole ?: "مسئول"}: ${equipment.driverName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "مشاهده جزئیات",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (equipment.hasEngineOil) {
                    if (oilStatusError != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StatusWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "تعویض روغن: خطای بارگذاری وضعیت",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusWarning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (oilStatus != null) {
                        val statusText: String
                        val statusColor: Color
                        if (oilStatus.isDue) {
                            if (oilStatus.overdueHours > 0) {
                                statusText = "${oilStatus.overdueHours.toInt()} ساعت overdue"
                                statusColor = StatusDanger
                            } else {
                                statusText = "موعد تعویض روغن رسیده"
                                statusColor = StatusDanger
                            }
                        } else {
                            statusText = "${oilStatus.remainingHours.toInt()} ساعت باقیمانده"
                            statusColor = if (oilStatus.remainingHours <= 15) StatusWarning else StatusSuccess
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تعویض روغن: $statusText",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor
                                )
                            }
                        }
                    }
                } else if (equipment.hasHydraulicOverflow) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "سرریز هیدرولیک (بدون تعویض روغن موتور)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (isAssignedToUser) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = IndustrialAccent.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "وسیله اختصاصی شما",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndustrialAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
